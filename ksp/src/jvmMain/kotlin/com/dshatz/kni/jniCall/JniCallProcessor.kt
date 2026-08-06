package com.dshatz.kni.jniCall

import com.dshatz.kni.BaseProcessor
import com.dshatz.kni.CNameUtils
import com.dshatz.kni.CNameUtils.cname
import com.dshatz.kni.CNameUtils.cnameFunName
import com.dshatz.kni.Registry
import com.dshatz.kni.Registry.Platform
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.TypeMapper
import com.dshatz.kni.Types
import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.model.KSJniCall
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.model.flow.KSFlowProp
import com.dshatz.kni.needsIsNullParam
import com.dshatz.kni.utils.addCode
import com.dshatz.kni.utils.addReturn
import com.dshatz.kni.utils.addStatement
import com.dshatz.kni.utils.define
import com.dshatz.kni.utils.joinToCode
import com.dshatz.kni.utils.nonNullOrPlaceholder
import com.dshatz.kni.utils.originatesFrom
import com.dshatz.kni.utils.returnType
import com.dshatz.kni.utils.withSuffix
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import jdk.javadoc.internal.doclets.toolkit.util.DocPath.parent

class JniCallProcessor(
    override val registry: Registry,
    override val logger: KSPLogger,
    override val mapper: TypeMapper
): BaseProcessor() {
    @OptIn(KspExperimental::class)
    private fun getAnnotatedJniCalls(resolver: Resolver): List<KSFunctionDeclaration> {
        val allowedClassKinds = setOf(ClassKind.OBJECT, ClassKind.CLASS)
        return resolver.getSymbolsWithAnnotation(JniCall::class.java.name).toList()
            .filterIsInstance<KSFunctionDeclaration>()
            .filter {
                val parentClass = it.parent as? KSClassDeclaration
                if (parentClass == null) {
                    // top level function
                    true
                } else {
                    if (parentClass.classKind !in allowedClassKinds) {
                        logger.error("@JniCall is only supported inside classes/objects or on top-level functions.", it)
                        false
                    } else if (parentClass.classKind == ClassKind.CLASS && parentClass.getAllSuperTypes().none { it.toTypeName() == Types.AutoCloseable }) {
                        logger.error("Classes with @JniCall methods must implement ${Types.AutoCloseable.canonicalName}", it)
                        false
                    } else true
                }
            }
            .filter { it.isExpect } // only take from common. common should act as the source of truth.
            .distinctBy { it.qualifiedName?.asString() }
    }

    /**
     * Find all **classes** with at least one [JniCall] function.
     * Saves found classNames to registry so [TypeMapper] knows that these should be treated accordingly.
     */
    fun collectNativeInstances(
        resolver: Resolver
    ) {
        val funDecls = getAnnotatedJniCalls(resolver)
            .map { it.parentDeclaration }
            .filterIsInstance<KSClassDeclaration>()
        registry.nativeInstances.addAll(funDecls.map { it.toClassName() })
    }

    fun collectJniCalls(
        resolver: Resolver
    ) {
        val funs = getAnnotatedJniCalls(resolver)
        val jniCalls = funs.groupBy {
            it.functionLocation()
        }.flatMap { (parent, funs) ->
            funs.map { f ->
                if (parent.declaration.containingFile == null) error("$parent has no containingFile.")
                KSJniCall(
                    name = f.simpleName.asString(),
                    returnType = mapper.mapType(f.returnType!!),
                    parameters = f.parameters.toTypeInfos(),
                    parent = parent,
                    declaration = f
                )
            }
        }
        registry.jniCalls.addAll(jniCalls)
    }

    fun generateNative(): List<FileSpec> {
        return registry.jniCalls.groupBy { it.parent }.map { (parent, functions) ->
            logger.info("JniCall: ${parent.className}: ${parent.declaration.containingFile?.filePath}")
            val funSpecs = functions.map { f ->
                context(f.declaration) {
                    generateCnameFunction(
                        f.parent.toMemberName(f.name),
                        f.parent,
                        f.parameters,
                        f.returnType
                    ).toBuilder().originatesFrom(f.declaration).build()
                }
            }

            val nativeInstance = parent as? FunctionParent.Class

            val constructors = nativeInstance?.let(::generateNativeConstructors)
            val dispose = nativeInstance?.let(::generateNativeDispose)

            val funParent = functions.first().parent
            val flows = registry.flowFields[funParent].orEmpty()
            val flowGetValueFuncs = flows.map { flowProp ->
                generateNativeFlowInit(funParent, flowProp)
            }
            val fileClassName = parent.classNameKt.withSuffix("_jniCalls")
            FileSpec.builder(fileClassName).apply {
                addFunctions(funSpecs)
                .addFunctions(flowGetValueFuncs)
                .apply {
                    constructors?.let(::addFunctions)
                    dispose?.let(::addFunction)
                }
            }.build()
        }
    }

    private fun generateNativeFlowInit(
        funParent: FunctionParent,
        flowProp: KSFlowProp,
    ): FunSpec {
        context(funParent) {
            val callbackType = TypeInfo.Callback(flowProp.baseCallbackClass)
            val callback = CodeBlock.of("callback").returnType(callbackType)
            val unpackCallback = callback.unpackCode()

            val (initCallback, callbackRef) = CodeBlock.builder()
                .define(
                    "callback",
                    callbackType,
                    "%L",
                    unpackCallback.code
                )

            val (initCode, defaultValue) = CodeBlock.builder()
                .define(
                    "defaultValue",
                    flowProp.innerType,
                    "instance.%M<%T>().%N.bindToJvm(%L)",
                    Types.Method.valueFromStableRefPointer,
                    funParent.className,
                    flowProp.name,
                    callbackRef.code
                )

            val converted = defaultValue.packCode()
            return cnameFunBuilder(
                funName = flowProp.initFunction.cnameFunName(),
                cname = flowProp.initFunction.cname()
            )
                .addParameter("instance", Types.JLong)
                .addParameter("callback", Types.JObject)
                .addCode(initCallback)
                .addCode(initCode)
                .addReturn(converted)
                .build()
        }
    }

    private fun generateNativeConstructors(
        cl: FunctionParent.Class
    ): List<FunSpec> {
        val returnTypeInfo = context(cl.declaration) {
            mapper.mapType(cl.className)
        }
        return cl.constructors.map { constructor ->
            val jniCName = CNameUtils.jniFunctionCName(
                packageName = cl.className.packageName,
                className = cl.className.simpleName,
                functionName = "initNative${constructor.id}"
            )
            val paramSpecs = constructor.params.map {
                ParameterSpec.builder(it.name, it.typeInfo.jniType.nativeType).build()
            }
            val paramConversion = constructor.params.map {
                it.typeInfo.unpackCode(it.paramCodeNative())
            }.joinToCode()

            val initNativeCode = CodeBlock.builder()
                .addStatement("%T(%L)", returnTypeInfo.kotlinType, paramConversion)
                .build()
                .returnType(returnTypeInfo.kotlinType)
            val returnCode = CodeBlock.builder()
                .addStatement("return %L", returnTypeInfo.packCode(initNativeCode).code)
                .build()

            cnameFunBuilder(
                MemberName(cl.className, "init${constructor.id}"),
                jniCName
            )
                .returns(LONG)
                .addParameters(paramSpecs)
                .addCode(returnCode)
                .build()
        }
    }

    private fun generateNativeDispose(
        cl: FunctionParent.Class
    ): FunSpec = context(cl.declaration) {
        val jniCname = CNameUtils.jniFunctionCName(
            packageName = cl.className.packageName,
            className = cl.className.simpleName,
            functionName = "disposeNative"
        )
        val instanceType = mapper.mapType(cl.className)
        val unpack = instanceType.unpackCode(CodeBlock.of("%N", "instance").returnType(instanceType.kotlinType))
        return cnameFunBuilder(
            MemberName(cl.className, "disposeNative"),
            jniCname
        ).addParameter(
            ParameterSpec.builder("instance", instanceType.jniType.nativeType).build()
        )
            .addCode(
                CodeBlock.builder()
                    .addStatement("%L.close()", unpack.code)
                    .addStatement("%N.%M<%T>()", "instance", Types.Method.ReleaseStableRef, cl.className)
                    .build()
            )
            .build()
    }

    private fun cnameFunBuilder(
        callFun: MemberName,
        jniCname: String,
    ): FunSpec.Builder {
        return cnameFunBuilder(
            funName = "_${callFun.enclosingClassName?.simpleName.orEmpty()}_${callFun.simpleName}JNI",
            cname = jniCname
        ).addKdoc("Calling user function [$callFun]")
    }

    private fun cnameFunBuilder(
        funName: String,
        cname: String
    ): FunSpec.Builder {
        return FunSpec.builder(funName)
            .addAnnotation(AnnotationSpec.builder(Types.Annotations.CName).addMember(CodeBlock.of("%S", cname)).build())
            .addAnnotation(Types.Annotations.Optin.NativeOptIn)
            .addParameter("env", Types.Environment)
            .addParameter("clazz", Types.JObject)
    }

    context(decl: KSFunctionDeclaration)
    private fun generateCnameFunction(
        callFun: MemberName,
        funLocation: FunctionParent,
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
            val paramCode = it.paramCodeNative()
            val nullified = if (it.typeInfo.needsIsNullParam()) {
                CodeBlock.of("(if (%N.%M()) null else %L)", "_${it.name}IsNull", Types.Method.ToKBoolean, paramCode.code)
                    .returnType(it.typeInfo.kotlinType)
            } else paramCode
            it.name to it.typeInfo.unpackCode(nullified)
        }
        val paramAssignment = paramCodes.map { (name, code) ->
            CodeBlock.builder().addStatement("val %N: %T = %L", name, code.type, code.code).build()
        }.joinToString("")
        val paramConversionCode = params.joinToString(", ") { it.name }
        val callUserCode = if (funLocation is FunctionParent.Class) {
            val instanceType = mapper.mapType(funLocation.className)
            val unpackInstance = instanceType.unpackCode(CodeBlock.of("%N", "instance").returnType(Types.KLong))
            CodeBlock.of("%L.%L(%L)", unpackInstance.code, callFun.simpleName, paramConversionCode)
        } else {
            CodeBlock.of("%M(%L)", callFun, paramConversionCode)
        }
        val packResult = returnType.packCode(callUserCode.returnType(returnType.kotlinType))
        val instanceParameters = if (funLocation is FunctionParent.Class) {
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
            .addParameters(
                params
                    .filter {
                        it.typeInfo.needsIsNullParam()
                    }
                    .map {
                        ParameterSpec.builder("_${it.name}IsNull", Types.JBoolean).build()
                    }
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement("// generated by Kotlin-JNI")
                    .addStatement("val env = env.%M()!!", Types.Method.GetAndAttach)
                    .add(paramAssignment)
                    .addStatement("val result: %T = %L", packResult.type, packResult.code)
                    .addStatement("return result")
                    .build()
            )
            .build()
        return f
    }

    private fun generateJvmActualConstructors(cl: FunctionParent.Class): List<FunSpec> {
        return cl.constructors.map { constructor ->
            val params = constructor.params.map {
                ParameterSpec.builder(it.name, it.typeInfo.kotlinType).build()
            }
            val convertParamsCode = constructor.params.map {
                it.typeInfo.packCodeJvm(it.paramCodeKotlin())
            }.joinToCode()
            FunSpec.constructorBuilder()
                .addParameters(params)
                .addModifiers(KModifier.ACTUAL, cl.declaration.primaryConstructor?.modifiers?.visibilityKModifier ?: KModifier.PUBLIC)
                .addCode(CodeBlock.builder().addStatement("nativeInstance = initNative%L(%L)", constructor.id, convertParamsCode).build())
                .build()
        }
    }

    private fun generateJvmExternalConstructors(cl: FunctionParent.Class): List<FunSpec> = context(cl.declaration) {
        cl.constructors.map { constructor ->
            val params = constructor.params.map {
                ParameterSpec.builder(it.name, it.typeInfo.jniType.jvmType).build()
            }
            val returnType = mapper.mapType(cl.className)

            FunSpec.builder("initNative${constructor.id}")
                .addParameters(params)
                .returns(returnType.jniType.jvmType)
                .addModifiers(KModifier.EXTERNAL, KModifier.PRIVATE)
                .build()
        }
    }

    private fun generateJvmExternalDispose(cl: FunctionParent.Class): FunSpec = context(cl.declaration) {
        val type = mapper.mapType(cl.className)
        return FunSpec.builder("disposeNative")
            .addParameter(ParameterSpec("instance", type.jniType.jvmType))
            .addModifiers(KModifier.EXTERNAL, KModifier.OVERRIDE)
            .build()
    }

    private fun generateJvmFunctions(
        functions: List<KSJniCall>,
        instanceParameter: TypeInfo? = null
    ): List<FunSpec> {
        val funNames = functions.associateWith { f ->
            MemberName(f.parent.className, f.name)
        }

        return functions.flatMap { f ->
            val externalMember = MemberName(funNames[f]!!.packageName, f.name + "External")

            val funSpec = FunSpec.builder(funNames[f]!!)
                .addModifiers(KModifier.ACTUAL, f.declaration.modifiers.visibilityKModifier)
                .apply {
                    if (Modifier.OVERRIDE in f.declaration.modifiers) addModifiers(KModifier.OVERRIDE)
                }
                .returns(f.returnType.kotlinType)
                .apply {
                    f.parameters.forEach { (name, typeInfo) ->
                        addParameter(ParameterSpec.builder(
                            name = name,
                            type = typeInfo.kotlinType
                        ).build())
                    }
                }
                .originatesFrom(f.declaration)
                .apply {
                    val paramPacking = f.parameters.map { p ->
                        val paramCode = p.paramCodeKotlin()
                        p.typeInfo.packCodeJvm(paramCode).nonNullOrPlaceholder()
                    }
                    val isNullParams = f.parameters.filter { it.typeInfo.needsIsNullParam() }.map {
                        CodeBlock.of("%N == null", it.name).returnType(Types.KBoolean)
                    }
                    val params = if (instanceParameter == null) {
                        paramPacking + isNullParams
                    } else {
                        paramPacking + isNullParams + CodeBlock.of("it").returnType(instanceParameter.jniType.nativeType)
                    }
                    val paramsCode = params.joinToCode(",\n")
                    val callExternalCode = CodeBlock.of("%M(%L)", externalMember, paramsCode).returnType(f.returnType.jniType.jvmType)
                    val returnValue = f.returnType.unpackCodeJvm(callExternalCode)
                    if (instanceParameter != null) {
                        val withValidInstanceBlock = CodeBlock.builder()
                            .beginControlFlow("return withValidInstance")
                            .add(returnValue.code)
                            .endControlFlow()
                            .build()
                        addCode(withValidInstanceBlock)
                    } else addCode("return %L", returnValue.code)

                }.build()


            val externalSpec = FunSpec.builder(externalMember)
                .addModifiers(KModifier.EXTERNAL, KModifier.PRIVATE)
                .apply {
                    f.parameters.forEach {
                        addKdoc("@param ${it.name} [${it.typeInfo.kotlinType}] converted to `${it.typeInfo.jniType.nativeType}`.\n")
                    }
                    if (f.returnType.kotlinType != UNIT) {
                        addKdoc("@return Representing `${f.returnType.describe()}`. Converted from `${f.returnType.jniType.nativeType}` to `${f.returnType.jniType.jvmType}`.\n")
                    }
                }
                .returns(f.returnType.jniType.jvmType)
                .addParameters(
                    f.parameters.map { (name, typeInfo) ->
                        ParameterSpec.builder(
                            name = name,
                            type = typeInfo.jniType.jvmType,
                        ).build()
                    }
                )
                .apply {
                    instanceParameter?.let {
                        addParameter(
                            ParameterSpec(
                                name = "nativeInstance",
                                type = it.jniType.jvmType
                            )
                        )
                    }
                }
                .addParameters(f.parameters.filter {
                    it.typeInfo.needsIsNullParam()
                }.map {
                    ParameterSpec("_${it.name}IsNull", Types.KBoolean)
                })
                .build()
            listOf(funSpec, externalSpec)
        }
    }


    fun generateJvm(): List<FileSpec> {
        return generateJvmActuals(registry.jniCalls)
    }

    private fun generateJvmActuals(funs: Collection<KSJniCall>): List<FileSpec> {
        val fileSpecs = mutableMapOf<ClassName, FileSpec.Builder>()
        funs.groupBy { it.parent }.forEach { (parent, functions) ->
            val instance = parent as? FunctionParent.Class
            val constructors = instance?.let(::generateJvmActualConstructors)
            val constructorFromPointer = FunSpec.constructorBuilder()
                .addParameter(ParameterSpec("nativeInstancePtr", Types.KLong))
                .callSuperConstructor()
                .addCode(CodeBlock.of("nativeInstance = nativeInstancePtr"))
                .build()
            val externalConstructors = instance?.let(::generateJvmExternalConstructors)
            val externalDispose = instance?.let(::generateJvmExternalDispose)
            val nativeInstanceType = instance?.let {
                context(it.declaration) {
                    mapper.mapType(it.className)
                }
            }

            val fileSpec = fileSpecs.getOrPut(parent.className) { FileSpec.builder(parent.className) }
            val specs = generateJvmFunctions(functions, nativeInstanceType)
            when {
                parent is FunctionParent.Class -> {
                    val flowProps = registry.flowFields[parent].orEmpty()
                    val factory = FunSpec.builder("as${parent.className.simpleName}")
                        .returns(parent.className)
                        .receiver(Types.KLong)
                        .addCode("return %T(this)", parent.className)
                        .build()
                    val factoryFileClass = parent.className.withSuffix("_converter")
                    val factoryFileSpec = FileSpec.builder(factoryFileClass)
                        .addFunction(factory)
                    fileSpecs[factoryFileClass] = factoryFileSpec

                    val type = TypeSpec.classBuilder(parent.className)
                        .addModifiers(KModifier.ACTUAL, parent.declaration.modifiers.visibilityKModifier)
                        .addSuperinterfaces(parent.superTypes)
                        .superclass(Types.NativeInstanceJvm)
                        .addFunction(constructorFromPointer)
                        .apply { constructors?.let(::addFunctions) }
                        .apply {
                            externalDispose?.let(::addFunction)
                        }
                        .addFunctions(flowProps.map(KSFlowProp::generateGetValueFun))
                        .addProperties(flowProps.map(KSFlowProp::generateFlowProp))
                        .addTypes(flowProps.map(KSFlowProp::generateFlowCallbackJvm))
                        .addFunctions(externalConstructors.orEmpty())
                        .addFunctions(specs)
                        .build()
                    fileSpec.addType(type)

                }
                parent is FunctionParent.Object -> {
                    val obj = TypeSpec.objectBuilder(parent.className)
                        .addModifiers(KModifier.ACTUAL, parent.declaration.modifiers.visibilityKModifier)
                        .addFunctions(specs)
                        .build()
                    fileSpec.addType(obj)
                }
                parent is FunctionParent.TopLevel -> {
                    fileSpec.addFunctions(specs)
                }
            }
        }
        return fileSpecs.values.map { it.build() }
    }

}

fun List<CodeBlock>.joinToString(separator: String = ", "): CodeBlock {
    return CodeBlock.of(
        generateSequence { "%L" }.take(size).joinToString(separator),
        args = this.toTypedArray()
    )
}


val Set<Modifier>.visibilityKModifier: KModifier
    get() = if (this.contains(Modifier.PRIVATE))
        KModifier.PRIVATE
    else if (this.contains(Modifier.PROTECTED))
        KModifier.PROTECTED
    else if (this.contains(Modifier.INTERNAL))
        KModifier.INTERNAL
    else KModifier.PUBLIC