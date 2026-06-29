package com.dshatz.kni.callable

import com.dshatz.kni.TypeMapper
import com.dshatz.kni.Types
import com.dshatz.kni.Types.typeOf
import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.needsIsNullParam
import com.dshatz.kni.utils.dereferenceTypeAlias
import com.dshatz.kni.utils.nonNullOrPlaceholder
import com.dshatz.kni.utils.nullSafeCall
import com.dshatz.kni.utils.returnType
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
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
import com.squareup.kotlinpoet.ksp.toClassName

class CallbackProcessor(
    private val typeMapper: TypeMapper,
    private val logger: KSPLogger
) {

    data class CallableBridge(
        val fileSpec: FileSpec,
        val deps: Dependencies,
        val cls: ClassName
    )

    data class CallbackDeclaration(
        val declaration: KSClassDeclaration,
        val cls: ClassName
    ) {
        fun jvmAdapterName(): ClassName {
            return ClassName(cls.packageName, cls.simpleName + "Adapter")
        }
    }

    fun getCallbackDeclarations(declarations: List<KSClassDeclaration>): List<CallbackDeclaration> {
        return declarations.map { CallbackDeclaration(it, it.toClassName()) }
    }
    
    fun getAnnotatedCallbacks(resolver: Resolver): List<KSClassDeclaration> {
        return resolver.getSymbolsWithAnnotation(JniCallback::class.java.name).toList()
            .filterIsInstance<KSClassDeclaration>().distinct()
    }

    fun generateNativeCallback(decl: CallbackDeclaration): CallableBridge? {
        val declaration = decl.declaration
        val cls = decl.cls
        val implCls = cls.getNativeImplClass()
        if (declaration.classKind != ClassKind.INTERFACE) {
            logger.error("@JniCallback can only be applied to an interface.")
            return null
        }
        if (declaration.superTypes.none { it.resolve().toClassName() typeOf Types.AutoCloseable }) {
            logger.error("@JniCallback annotated interface should extend kotlin.AutoCloseable.", declaration)
            return null
        }

        fun FunSpec.Builder.addParams(params: List<KSValueParameter>): FunSpec.Builder {
            val typed = params.map { CallableProcessor.ParamInfo(
                it.name!!.asString(),
                typeMapper.mapType(it.type)
            ) }
            typed.forEach {
                addParameter(
                    ParameterSpec(it.name, it.typeInfo.kotlinType)
                )
            }
            return this
        }

        val funs = declaration.declarations.filterIsInstance<KSFunctionDeclaration>().filterNot { it.isConstructor() }.map { f ->
            val returnType = f.returnType?.dereferenceTypeAlias()?.toClassName() ?: error("Failed to resolve return type: ${f.returnType}")
            val call = Def.callHelper(returnType)
            if (Modifier.SUSPEND in f.modifiers) {
                logger.error("suspend functions are not supported in @JniCallback.", f)
            }
            val returnConverter = Def.returnTypeConverters[returnType.copy(nullable = false)] ?: CodeBlock.of("")
            val nullCheck = if (returnType.isNullable) "" else CodeBlock.of("!!")
            val callCode = CodeBlock.builder().addStatement(
                "env.%M(%N, %N, %N)%L$nullCheck",
                call,
                "adapterClassGlobal",
                f.simpleName.asString() + "ID",
                "args",
                returnConverter
            ).build()
            val params = f.parameters.map {
                context(it) {
                    CallableProcessor.ParamInfo(it.name!!.asString(), typeMapper.mapType(it.type))
                }
            }
            FunSpec.builder(f.simpleName.asString())
                .addParams(f.parameters)
                .addModifiers(KModifier.OVERRIDE)
                .returns(f.returnType?.resolve()?.toClassName() ?: UNIT)
                .addCode(CodeBlock.builder()
                    .add("%L", buildArgs(params, callCode, typeMapper))
                    .build()
                )
                .build()
        }

        val methodIds = declaration.declarations.filterIsInstance<KSFunctionDeclaration>().filterNot { it.isConstructor() }.map { f ->
            PropertySpec.builder("${f.simpleName.asString()}ID", Types.JMethodID)
                .delegate(CodeBlock.of("lazyMethodId(%S, %S)", f.simpleName.asString(), f.getSignature(typeMapper)))
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
            .addSuperclassConstructorParameter("%S", declaration.jniClassName())
            .addSuperclassConstructorParameter("env")
            .addSuperclassConstructorParameter("instance")
            .addSuperinterface(cls)
            .build()

        val deps = Dependencies(false, *listOfNotNull(
            declaration.containingFile,
            declaration.parentDeclaration?.containingFile
        ).toTypedArray())
        val fileSpec = FileSpec.builder(implCls)
            .addType(bridgeClass)
            .addImport("kotlinx.cinterop", "get")
            .addAnnotation(optin())
            .build()
        return CallableBridge(fileSpec, deps, cls)
    }

    private fun KSClassDeclaration.jniClassName(): String {
        return qualifiedName!!.asString().replace('.', '/')
    }

    fun generateJvmAdapter(decl: CallbackDeclaration): FileSpec {
        val file = decl.jvmAdapterName()
        val funs = decl.declaration.declarations.filterIsInstance<KSFunctionDeclaration>().map { f ->
            val fName = f.simpleName.asString()
            val params = f.parameters.map { param ->
                CallableProcessor.ParamInfo(param.name!!.asString(), typeMapper.mapType(param.type))
            }
            val paramsSpecs = params.map {
                ParameterSpec(it.name, it.typeInfo.jniType.jvmType)
            }
            val isNullParamSpecs = params.filter { it.typeInfo.needsIsNullParam() }.map {
                ParameterSpec("_${it.name}IsNull", Types.KBoolean)
            }
            val convertArgsCode = params.joinToCode(",\n") {
                val unpackCode = it.typeInfo.unpackCodeJvm(it.paramCodeJvm()).code
                if (it.typeInfo.needsIsNullParam()) {
                    CodeBlock.of("if (_%NIsNull) null else %L", it.name, unpackCode)
                } else unpackCode
            }
            val returnType = f.returnType?.let(typeMapper::mapType)
            val builder = FunSpec.builder(fName)
                .addParameter(ParameterSpec.builder("instance", decl.cls).build())
                .addParameters(paramsSpecs)
                .addParameters(isNullParamSpecs)
                .addAnnotation(JvmStatic::class)
            val code = CodeBlock.of("%N.%N(%L)", "instance", fName, convertArgsCode)

            if (returnType != null) {
                builder.addCode("return %L", code)
                    .returns(returnType.jniType.jvmType)
            } else builder.addCode(code)

            builder.build()
        }.toList()
        return FileSpec.builder(file)
            .addType(
                TypeSpec.objectBuilder(file)
                    .addFunctions(funs).build()
            )
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

private fun buildArgs(
    args: List<CallableProcessor.ParamInfo>,
    innerCode: CodeBlock,
    typeMapper: TypeMapper
): CodeBlock {
    return CodeBlock.builder()
        .beginControlFlow("return %M", Def.memScoped)
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

private fun optin(): AnnotationSpec {
    return AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
        .addMember("%T::class", ClassName("kotlinx.cinterop", "ExperimentalForeignApi"))
        .build()
}

internal object Def {
    val CallStaticObjMethodA = MemberName("com.dshatz.kni.utils", "CallStaticObjMethodA")
    val CallStaticVoidMethodA = MemberName("com.dshatz.kni.utils", "CallStaticVoidMethodA")
    val memScoped = MemberName("kotlinx.cinterop", "memScoped")
    val allocArray = MemberName("kotlinx.cinterop", "allocArray")
    val reinterpret = MemberName("kotlinx.cinterop", "reinterpret")

    internal fun callHelper(type: ClassName): MemberName {
        return when (type.copy(nullable = false)) {
            Types.KString, Types.KByteArray -> {
                CallStaticObjMethodA
            }
            UNIT -> CallStaticVoidMethodA
            else -> MemberName("com.dshatz.kni.utils", "CallStatic${type.simpleName}MethodA")
        }
    }

    val returnTypeConverters = mapOf(
        Types.KString to CodeBlock.of("?.%M(env)", Types.Method.ToKString),
        Types.KByteArray to CodeBlock.of("?.%M(env)", Types.Method.ToKByteArray)
    )
}
