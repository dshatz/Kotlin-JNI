package com.dshatz.kni.callable

import com.dshatz.kni.CNameUtils
import com.dshatz.kni.Registry
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.TypeMapper
import com.dshatz.kni.TypeMatcher
import com.dshatz.kni.kspfix.FunLocation
import com.dshatz.kni.kspfix.KSFun
import com.dshatz.kni.kspfix.withLocations
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.let

class CallableProcessor(
    private val registry: Registry,
    private val logger: KSPLogger
) {
    private val mapper: TypeMapper = TypeMapper(registry)

    private fun List<KSValueParameter>.toTypeInfos(): List<ParamInfo> {
        return map {
            ParamInfo(it.name!!.asString(), mapper.mapType(it.type))
        }
    }

    fun generateNativeFuns(
        funs: List<KSFunctionDeclaration>,
    ): List<FileSpec> {
        return funs.withLocations().groupBy { it.location.className }.map { (parent, functions) ->

            val funSpecs = functions.map { (f, location) ->
                val params = f.parameters.toTypeInfos()
                val returnType = mapper.mapType(f.returnType!!)

                generateCnameFunction(location.toMemberName(f.simpleName.asString()), location, params, returnType)
            }

            val cl = functions.firstOrNull()?.classDeclaration?.takeIf { it.classKind == ClassKind.CLASS }
            val constructor = cl?.let(::generateNativeConstructor)
            val dispose = cl?.let(::generateNativeDispose)

            logger.warn("Adding file ${parent.simpleName}: generateNativeFuns(${funs.joinToString { it.simpleName.asString() }})")
            FileSpec.builder(parent)
                .addFunctions(funSpecs)
                .apply {
                    constructor?.let(::addFunction)
                    dispose?.let(::addFunction)
                }
                .build()
        }
    }

    private fun generateNativeConstructor(
        cl: KSClassDeclaration
    ): FunSpec {
        val jniCName = CNameUtils.jniFunctionCName(
            packageName = cl.packageName.asString(),
            className = cl.simpleName.asString(),
            functionName = "initNative"
        )

        val params = cl.primaryConstructor?.parameters.orEmpty().toTypeInfos()
        val paramSpecs = params.map {
            ParameterSpec.builder(it.name, it.typeInfo.jniType.nativeType).build()
        }

        val paramConversion = params.map {
            it.typeInfo.unpackCode(CodeBlock.of("%N", it.name))
        }.joinToString()

        val returnTypeInfo = mapper.mapType(cl.toClassName())

        val initNativeCode = CodeBlock.builder()
            .addStatement("%T(%L)", returnTypeInfo.kotlinType, paramConversion)
            .build()

        val returnCode = CodeBlock.builder()
            .addStatement("return %L", returnTypeInfo.packCode(initNativeCode))
            .build()

        return cnameFunBuilder(
            MemberName(cl.toClassName(), "init"),
            jniCName
        )
            .returns(LONG)
            .addParameters(paramSpecs)
            .addCode(returnCode)
            .build()
    }

    private fun generateNativeDispose(
        cl: KSClassDeclaration
    ): FunSpec {
        val jniCname = CNameUtils.jniFunctionCName(
            packageName = cl.packageName.asString(),
            className = cl.simpleName.asString(),
            functionName = "disposeNative"
        )
        val instanceType = mapper.mapType(cl.asType(emptyList()))
        val unpack = instanceType.unpackCode(CodeBlock.of("%N", "instance"))
        return cnameFunBuilder(
            MemberName(cl.toClassName(), "disposeNative"),
            jniCname
        ).addParameter(
            ParameterSpec.builder("instance", instanceType.jniType.nativeType).build()
        )
            .addCode(
                CodeBlock.builder()
                    .addStatement("%L.close()", unpack)
                    .addStatement("%N.%M<%T>()", "instance", TypeMatcher.Method.ReleaseStableRef, cl.toClassName())
                    .build()
            )
            .build()
    }

    private fun cnameFunBuilder(
        callFun: MemberName,
        jniCname: String,
    ): FunSpec.Builder {
        val funName = "_${callFun.simpleName}JNI"
        return FunSpec.builder(funName)
            .addAnnotation(AnnotationSpec.builder(CNameUtils.CName).addMember(CodeBlock.of("%S", jniCname)).build())
            .addAnnotation(CNameUtils.NativeOptIn)
            .addParameter("env", TypeMatcher.Environment)
            .addParameter("clazz", TypeMatcher.JObject)
            .addKdoc("Calling user function $callFun")
    }

    private fun generateCnameFunction(
        callFun: MemberName,
        funLocation: FunLocation,
        params: List<ParamInfo>,
        returnType: TypeInfo
    ): FunSpec {

        val jniCName = CNameUtils.jniFunctionCName(
            packageName = callFun.packageName,
            className = funLocation.classNameKt.simpleName,
            functionName = callFun.simpleName + "External"
        )

        val builder = cnameFunBuilder(
            callFun,
            jniCName
        )

        val paramCodes = params.map {
            it.typeInfo.unpackCode(CodeBlock.of("%N", it.name))
        }
        val paramConversionCode = paramCodes.joinToString()
        val callUserCode = if (funLocation.locationType == FunLocation.LocationType.CLASS) {
            val instanceType = mapper.mapType(funLocation.className)
            val unpackInstance = instanceType.unpackCode(CodeBlock.of("%N", "instance"))
            CodeBlock.of("%L.%L(%L)", unpackInstance, callFun.simpleName, paramConversionCode)
        } else {
            CodeBlock.of("%M(%L)", callFun, paramConversionCode)
        }
        val packResult = returnType.packCode(callUserCode)
        val instanceParameters = if (funLocation.locationType == FunLocation.LocationType.CLASS) {
            val pointerParam = ParameterSpec.builder(
                "instance",
                mapper.mapType(funLocation.className).jniType.nativeType
            ).build()
            listOf(pointerParam)
        } else emptyList()

        val f = builder
            .returns(
                returnType.jniType.nativeType,
                kdoc = CodeBlock.of("${returnType.kotlinType} <- ${returnType.jniType.nativeType}")
            )
            .addParameters(params.map {
                ParameterSpec
                    .builder(it.name, it.typeInfo.jniType.nativeType)
                    .addKdoc("${it.typeInfo.kotlinType} -> ${it.typeInfo.jniType.nativeType} (via ${it.typeInfo::class.simpleName})")
                    .build()
            } + instanceParameters)
            .addCode(
                CodeBlock.builder()
                    .addStatement("// generated by Kotlin-JNI")
                    .addStatement("val env = env.%M()!!", TypeMatcher.Method.GetAndAttach)
                    .addStatement("return %L", packResult)
                    .build()
            )
            .build()
        return f
    }

    private fun generateJvmActualConstructor(cl: KSClassDeclaration): FunSpec {
        val typedParams = cl.primaryConstructor?.parameters?.toTypeInfos().orEmpty()
        val params = typedParams.map {
            ParameterSpec.builder(it.name, it.typeInfo.kotlinType).build()
        }
        val convertParamsCode = typedParams.map {
            it.typeInfo.packCodeJvm(CodeBlock.of("%N", it.name))
        }.joinToString()
        return FunSpec.constructorBuilder()
            .addParameters(params)
            .addModifiers(KModifier.ACTUAL)
            .addCode(CodeBlock.builder().addStatement("nativeInstance = initNative(%L)", convertParamsCode).build())
            .build()
    }

    private fun generateJvmExternalConstructor(cl: KSClassDeclaration): FunSpec {
        val params = cl.primaryConstructor?.parameters?.toTypeInfos().orEmpty().map {
            ParameterSpec.builder(it.name, it.typeInfo.jniType.jvmType).build()
        }
        val returnType = mapper.mapType(cl.asType(emptyList()))

        return FunSpec.builder("initNative")
            .addParameters(params)
            .returns(returnType.jniType.jvmType)
            .addModifiers(KModifier.EXTERNAL)
            .build()
    }

    private fun generateJvmExternalDispose(cl: KSClassDeclaration): FunSpec {
        val type = mapper.mapType(cl.toClassName())
        return FunSpec.builder("disposeNative")
            .addParameter(ParameterSpec.builder("instance", type.jniType.jvmType).defaultValue("nativeInstance").build())
            .addModifiers(KModifier.EXTERNAL)
            .build()
    }

    private fun generateJvmFunctions(
        functions: List<KSFun>,
        instanceParameter: TypeInfo? = null
    ): List<FunSpec> {
        val funNames = functions.associate { f ->
            f.f to MemberName(f.location.className, f.f.simpleName.asString())
        }

        return functions.flatMap { (f, loc) ->

            val externalMember = funNames[f]!!.let {
                MemberName(f.packageName.asString(), f.simpleName.asString() + "External")
            }
            val params = f.parameters.map {
                it.name!!.asString() to mapper.mapType(it.type)
            }
            val returnType = mapper.mapType(f.returnType!!)

            val funSpec = FunSpec.builder(funNames[f]!!)
                .addModifiers(KModifier.ACTUAL)
                .returns(returnType.kotlinType)
                .apply {
                    params.forEach { (name, typeInfo) ->
                        addParameter(ParameterSpec.builder(
                            name = name,
                            type = typeInfo.kotlinType
                        ).build())
                    }
                }
                .apply {
                    val paramPacking = params.map { (name, typeInfo) ->
                        typeInfo.packCodeJvm(CodeBlock.of("%N", name))
                    }
                    val paramsCode = paramPacking.joinToString()
                    val callExternalCode = CodeBlock.of("%M(%L)", externalMember, paramsCode)
                    addCode(
                        CodeBlock.builder()
                            .addStatement("return %L", returnType.unpackCodeJvm(callExternalCode))
                            .build()
                    )
                }.build()


            val externalSpec = FunSpec.builder(externalMember)
                .addModifiers(KModifier.EXTERNAL)
                .returns(returnType.jniType.jvmType)
                .apply {
                    params.forEach { (name, typeInfo) ->
                        addParameter(ParameterSpec.builder(
                            name = name,
                            type = typeInfo.jniType.jvmType,
                        ).build())
                    }
                    instanceParameter?.let {
                        addParameter(
                            ParameterSpec.builder(
                                name = "nativeInstance",
                                type = it.jniType.jvmType
                            )
                                .defaultValue("this.nativeInstance")
                                .build()
                        )
                    }
                }.build()
            listOf(funSpec, externalSpec)
        }
    }

    fun generateJvmActuals(
        funs: List<KSFunctionDeclaration>,
    ): List<FileSpec> {
        val withLocations = funs.withLocations()
        val constructors = withLocations.mapNotNull {
            it.classDeclaration?.let { cls ->
                 it.location.className to generateJvmActualConstructor(cls)
            }
        }.toMap()

        val externalConstructors = withLocations.mapNotNull {
            it.classDeclaration?.let { cls ->
                it.location.className to generateJvmExternalConstructor(cls)
            }
        }.toMap()

        val externalDispose = withLocations.mapNotNull {
            it.classDeclaration?.let { cls ->
                it.location.className to generateJvmExternalDispose(cls)
            }
        }.toMap()

        return funs.withLocations().groupBy { it.location.className }.map { (parent, functions) ->

            val byLocationType: Map<FunLocation.LocationType, List<KSFun>> = functions.groupBy { it.location.locationType }


            val fileSpec = FileSpec.builder(parent)
            byLocationType.forEach { (type, funs) ->
                when (type) {
                    FunLocation.LocationType.CLASS -> {
                        val nativeType = mapper.mapType(parent)
                        val specs = generateJvmFunctions(funs, nativeType)
                        val nativeProp = PropertySpec.builder("nativeInstance", nativeType.jniType.jvmType).build()
                        val constructor = constructors[parent]
                        val type = TypeSpec.classBuilder(parent)
                            .addModifiers(KModifier.ACTUAL)
                            .addSuperinterface(TypeMatcher.AutoCloseable)
                            .addProperty(nativeProp)
                            .apply { constructor?.let(::primaryConstructor) }
                            .apply {
                                externalDispose[parent]?.let { externalDispose ->
                                    addFunction(externalDispose)
                                    addFunction(
                                        FunSpec.builder("close")
                                            .addModifiers(KModifier.OVERRIDE, KModifier.ACTUAL)
                                            .addCode(CodeBlock.of("%L", "disposeNative()"))
                                            .build()
                                    )
                                }
                            }
                            .addFunctions(listOfNotNull(externalConstructors[parent]))
                            .addFunctions(specs)
                            .build()

                        fileSpec.addType(type)
                    }
                    FunLocation.LocationType.OBJECT -> {
                        val specs = generateJvmFunctions(funs)
                        val type = TypeSpec.objectBuilder(parent)
                            .addModifiers(KModifier.ACTUAL)
                            .addFunctions(specs)
                            .build()
                        fileSpec.addType(type)
                    }
                    FunLocation.LocationType.TOPLEVEL -> {
                        val specs = generateJvmFunctions(funs)
                        fileSpec.addFunctions(specs)
                    }
                }
            }
            fileSpec.build()
        }
    }

    fun List<CodeBlock>.joinToString(): CodeBlock {
        return CodeBlock.of(
            generateSequence { "%L" }.take(size).joinToString(", "),
            args = this.toTypedArray()
        )
    }

    data class ParamInfo(
        val name: String,
        val typeInfo: TypeInfo,
    )
}