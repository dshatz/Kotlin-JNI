package com.dshatz.kni.jniCall

import com.dshatz.kni.BaseProcessor
import com.dshatz.kni.Registry
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.TypeMapper
import com.dshatz.kni.Types
import com.dshatz.kni.Types.typeOf
import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.model.KSCallback
import com.dshatz.kni.model.KSCallbackFun
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.needsIsNullParam
import com.dshatz.kni.utils.add
import com.dshatz.kni.utils.addCode
import com.dshatz.kni.utils.addReturn
import com.dshatz.kni.utils.capitalized
import com.dshatz.kni.utils.define
import com.dshatz.kni.utils.jniClassName
import com.dshatz.kni.utils.nonNullOrPlaceholder
import com.dshatz.kni.utils.nullSafeCall
import com.dshatz.kni.utils.returnType
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class CallbackProcessor(
    override val registry: Registry,
    override val logger: KSPLogger,
    override val mapper: TypeMapper
): BaseProcessor() {

    data class CallableBridge(
        val fileSpec: FileSpec,
//        val deps: Dependencies,
        val cls: ClassName
    )

    fun collectDeclarations(
        resolver: Resolver
    ) {
        val clss = resolver.getSymbolsWithAnnotation(JniCallback::class.java.name).toList()
            .filterIsInstance<KSClassDeclaration>().distinct()

        val callbacks = clss
            .filter { declaration ->
                if (declaration.getAllSuperTypes().none { it.toTypeName() typeOf Types.AutoCloseable }) {
                    logger.error("@JniCallback annotated interface should extend kotlin.AutoCloseable.", declaration)
                    return@filter false
                }
                true
            }
            .associate { declaration ->
            val funDeclarations = declaration.declarations
                .filterIsInstance<KSFunctionDeclaration>()
                .filterNot { it.isConstructor() }
                .filterNot {
                    // Remove close function, if defined (overridden). Call to it is already implemented in BaseCallback.
                    it.simpleName.asString() == "close" && it.parameters.isEmpty() && it.returnType?.toTypeName() == UNIT
                }
            val funs = funDeclarations.map { f ->
                if (Modifier.SUSPEND in f.modifiers) {
                    logger.error("suspend functions are not supported in @JniCallback.", f)
                }
                KSCallbackFun(
                    name = f.simpleName.asString(),
                    returnType = mapper.mapType(f.returnType!!),
                    parameters = f.parameters.toTypeInfos(),
                    parent = f.functionLocation(),
                )
            }.toList()
            val callbackType = declaration.toClassName()
            callbackType to KSCallback(
                type = callbackType,
                funs = funs,
                dependency = declaration.containingFile!!
            )
        }
        registry.callbacks.putAll(callbacks)
    }

    fun generateNative(): List<FileSpec> {
        return registry.callbacks.values.map(::generateNativeCallback)
    }

    private fun generateNativeCallback(callback: KSCallback): FileSpec {
        val cls = callback.type
        val implCls = cls.getNativeImplClass()

        fun FunSpec.Builder.addParams(params: List<ParamInfo>): FunSpec.Builder {
            params.forEach {
                addParameter(
                    ParameterSpec(it.name, it.typeInfo.kotlinType)
                )
            }
            return this
        }

        val funs = callback.funs.map { f ->
            val call = Def.callHelper(f.returnType)
            val (callCode, jniResultRef) = CodeBlock.builder()
                .define(
                    "jniCallResult",
                    f.returnType,
                    "env.%M(%N, %N, %N)",
                    call,
                    "adapterClassGlobal",
                    f.name + "ID",
                    "args",
                )
            val unpacked = jniResultRef.unpackCode()
            val returnConverted = CodeBlock.builder()
                .add(callCode)
                .add(unpacked.code)
                .build()
            val params = f.parameters
            FunSpec.builder(f.name)
                .addParams(f.parameters)
                .addKdoc(CodeBlock.of("@returns ${f.returnType.kotlinType} converted using ${f.returnType}"))
                .addModifiers(KModifier.OVERRIDE)
                .returns(f.returnType.kotlinType)
                .addCode(CodeBlock.builder()
                    .beginControlFlow("return runIfOpen")
                    .add("%L", buildArgs(params, returnConverted))
                    .endControlFlow()
                    .build()
                )
                .build()
        }

        val methodIds = callback.funs.map { f ->
            PropertySpec.builder("${f.name}ID", Types.JMethodID)
                .delegate(CodeBlock.of("lazyMethodId(%S, %S)", f.name, f.getSignature()))
                .build()
        }

        val constructor = FunSpec.constructorBuilder()
            .addParameter(ParameterSpec("env", Types.Environment))
            .addParameter(ParameterSpec("instance", Types.JObject))
            .build()

        val bridgeClass = TypeSpec.classBuilder(implCls)
            .addFunctions(funs.toList())
            .addProperties(methodIds.toList())
            .superclass(Types.BaseCallback)
            .primaryConstructor(constructor)
            .addSuperclassConstructorParameter("%S", callback.type.jniClassName())
            .addSuperclassConstructorParameter("%S", callback.jvmAdapterName().jniClassName())
            .addSuperclassConstructorParameter("env")
            .addSuperclassConstructorParameter("instance")
            .addSuperinterface(cls)
            .build()
        val factory = FunSpec.builder("asNative${cls.simpleName.capitalized()}")
            .receiver(Types.JObject)
            .addParameter("env", Types.Environment)
            .returns(cls.getNativeImplClass())
            .addCode(CodeBlock.of("return %T(env, this)", cls.getNativeImplClass()))
            .build()

        val fileSpec = FileSpec.builder(implCls)
            .addType(bridgeClass)
            .addFunction(factory)
            .addImport("kotlinx.cinterop", "get")
            .addAnnotation(optin())
            .build()
        return fileSpec
    }

    fun generateJvm(): List<FileSpec> {
        return registry.callbacks.values.map(::generateJvmAdapter)
    }

    private fun generateJvmAdapter(callback: KSCallback): FileSpec {
        val file = callback.jvmAdapterName()
        val funs = callback.funs.map { f ->
            val fName = f.name
            val paramsSpecs = f.parameters.map {
                ParameterSpec(it.name, it.typeInfo.jniType.jvmType)
            }
            val isNullParamSpecs = f.parameters.filter { it.typeInfo.needsIsNullParam() }.map {
                ParameterSpec("_${it.name}IsNull", Types.KBoolean)
            }
            val convertArgsCode = f.parameters.joinToCode(",\n") {
                val unpackCode = it.typeInfo.unpackCodeJvm(it.paramCodeJvm()).code
                if (it.typeInfo.needsIsNullParam()) {
                    CodeBlock.of("if (_%NIsNull) null else %L", it.name, unpackCode)
                } else unpackCode
            }
            val builder = FunSpec.builder(fName)
                .addParameter(ParameterSpec("instance", callback.type))
                .addParameters(paramsSpecs)
                .addParameters(isNullParamSpecs)
                .addAnnotation(JvmStatic::class)
            val makeCall = CodeBlock.of("%N.%N(%L)", "instance", fName, convertArgsCode)
                .returnType(f.returnType.kotlinType)

            if (f.returnType.kotlinType != Types.UnitOrVoid) {
                val (getJvmResult, jvmResultRef) = CodeBlock.builder().define(
                    "jvmResult",
                    f.returnType,
                    "%L",
                    makeCall.code
                )
                builder
                    .addCode(getJvmResult)
                    .addReturn(jvmResultRef.packCodeJvm())
            } else {
                builder.addStatement("%L", makeCall.code)
            }

            builder.build()
        }.toList()
        return FileSpec.builder(file)
            .addType(
                TypeSpec.objectBuilder(file)
                    .addOriginatingKSFile(callback.dependency)
                    .addFunctions(funs).build()
            )
            .build()
    }

    private fun buildArgs(
        args: List<ParamInfo>,
        innerCode: CodeBlock,
    ): CodeBlock {
        return CodeBlock.builder()
            .beginControlFlow("%M", Def.memScoped)
            .addStatement("val args = %M<%T>(%L)", Def.allocArray, Types.JValue, args.size + 1)
            .apply {
                addStatement("args[0].l = ref.%M()", Def.reinterpret)
                args.forEachIndexed { idx, arg ->
                    val type = arg.typeInfo
                    val argCode = CodeBlock.of("%N", arg.name).returnType(type.kotlinType).nonNullOrPlaceholder().copy(type = type.jniType.nativeType)
                    val valueCode = type.packCode(argCode)
                    val reinterpreted = if (type.jniType.jniField == "l") {
                        valueCode.nullSafeCall(CodeBlock.of("%M()", Def.reinterpret).returnType(ANY))
                    } else valueCode
                    addStatement("args[%L].%L = %L", idx + 1, type.jniType.jniField, reinterpreted.code)
                }
                args.filter { it.typeInfo.needsIsNullParam() }.mapIndexed { idx, arg ->
                    val globalIdx = idx + args.size + 1
                    addStatement("args[%L].z = (%N == null).%M()", globalIdx, arg.name, Types.Method.ToJBoolean)
                }
            }
            .add(innerCode)
            .endControlFlow()
            .build()
    }
}

internal fun TypeName.getNativeImplClass(): ClassName {
    val cls = this as ClassName
    return ClassName(
        packageName = cls.packageName,
        "_" + cls.simpleName + "NativeImpl"
    )
}

private fun optin(): AnnotationSpec {
    return AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
        .addMember("%T::class", ClassName("kotlinx.cinterop", "ExperimentalForeignApi"))
        .build()
}

internal object Def {
    val CallStaticObjMethodA = MemberName("com.dshatz.kni.utils", "CallStaticObjMethodA")
    val CallStaticObjMethodANullable = MemberName("com.dshatz.kni.utils", "CallStaticObjMethodANullable")
    val CallStaticVoidMethodA = MemberName("com.dshatz.kni.utils", "CallStaticVoidMethodA")

    val memScoped = MemberName("kotlinx.cinterop", "memScoped")
    val allocArray = MemberName("kotlinx.cinterop", "allocArray")
    val reinterpret = MemberName("kotlinx.cinterop", "reinterpret")

    internal fun callHelper(typeInfo: TypeInfo): MemberName {
        val type = typeInfo.jniType.nativeType
        return when(type.copy(nullable = false)) {
            Types.JObject,
            Types.JByteArray,
            Types.JString -> if (type.isNullable) CallStaticObjMethodANullable else CallStaticObjMethodA
            Types.UnitOrVoid -> CallStaticVoidMethodA
            else -> {
                val clsName = (typeInfo.kotlinType as? ClassName)?.simpleName ?: error("Unable to map callback return to jni function: $type")
                MemberName("com.dshatz.kni.utils", "CallStatic${clsName}MethodA")
            }
        }
    }
}
