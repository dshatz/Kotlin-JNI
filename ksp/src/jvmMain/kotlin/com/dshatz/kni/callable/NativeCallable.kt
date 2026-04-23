package com.dshatz.kni.callable

import com.dshatz.kni.TypeMatcher
import com.dshatz.kni.TypeMatcher.typeOf
import com.dshatz.kni.utils.dereferenceTypeAlias
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
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
import com.squareup.kotlinpoet.ksp.toClassName

object NativeCallable {

    data class CallableBridge(
        val fileSpec: FileSpec,
        val deps: Dependencies,
        val cls: ClassName
    )

    fun generateNativeBridge(cls: KSClassDeclaration, logger: KSPLogger): CallableBridge? {
        val implCls = cls.toClassName().getNativeImplClass()
        if (cls.classKind != ClassKind.INTERFACE) {
            logger.error("@JniCallback can only be applied to an interface.")
            return null
        }
        if (cls.superTypes.none { it.resolve().toClassName() typeOf TypeMatcher.AutoCloseable }) {
            logger.error("@JniCallback annotated interface should extend kotlin.AutoCloseable.", cls)
            return null
        }

        fun FunSpec.Builder.addParams(params: List<KSValueParameter>): FunSpec.Builder {
            params.forEach { param ->
                addParameter(
                    ParameterSpec(param.name!!.asString(), param.type.resolve().toClassName())
                )
            }
            return this
        }

        val funs = cls.declarations.filterIsInstance<KSFunctionDeclaration>().filterNot { it.isConstructor() }.map { f ->
            val originalReturnType = f.returnType?.resolve()?.toClassName() ?: error("Failed to resolve return type: ${f.returnType}")
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
                "ref",
                f.simpleName.asString() + "ID",
                "args",
                returnConverter
            ).build()
            FunSpec.builder(f.simpleName.asString())
                .addParams(f.parameters)
                .addModifiers(KModifier.OVERRIDE)
                .returns(f.returnType?.resolve()?.toClassName() ?: UNIT)
                .addCode(CodeBlock.builder()
                    .add("%L", buildArgs(f.parameters, callCode))
                    .build()
                )
                .build()
        }

        val methodIds = cls.declarations.filterIsInstance<KSFunctionDeclaration>().filterNot { it.isConstructor() }.map { f ->
            PropertySpec.builder("${f.simpleName.asString()}ID", TypeMatcher.JMethodID)
                .delegate(CodeBlock.of("lazyMethodId(%S, %S)", f.simpleName.asString(), f.getSignature()))
                .build()
        }

        val constructor = FunSpec.constructorBuilder()
            .addParameter(ParameterSpec("env", TypeMatcher.Environment))
            .addParameter(ParameterSpec("instance", TypeMatcher.JObject))
            .build()

        val bridgeClass = TypeSpec.classBuilder(implCls)
            .addFunctions(funs.toList())
            .addProperties(methodIds.toList())
            .superclass(TypeMatcher.BaseCallback)
            .primaryConstructor(constructor)
            .addSuperclassConstructorParameter("%S", cls.jniClassName())
            .addSuperclassConstructorParameter("env")
            .addSuperclassConstructorParameter("instance")
            .addSuperinterface(cls.toClassName())
            .build()

        val deps = Dependencies(false, *listOfNotNull(
            cls.containingFile,
            cls.parentDeclaration?.containingFile
        ).toTypedArray())
        val fileSpec = FileSpec.builder(implCls)
            .addType(bridgeClass)
            .addImport("kotlinx.cinterop", "get")
            .addAnnotation(optin())
            .build()
        return CallableBridge(fileSpec, deps, cls.toClassName())
    }

    private fun KSClassDeclaration.jniClassName(): String {
        return qualifiedName!!.asString().replace('.', '/')
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
    args: List<KSValueParameter>,
    innerCode: CodeBlock
): CodeBlock {
    return CodeBlock.builder()
        .beginControlFlow("return %M", Def.memScoped)
        .addStatement("val args = %M<%T>(%L)", Def.allocArray, TypeMatcher.JValue, args.size)
        .apply {
            args.forEachIndexed { idx, arg ->
                val type = arg.type.dereferenceTypeAlias()
                val (jniField, converter) = type.toJValueField()
                addStatement("args[%L].%L = %N%L", idx, jniField, arg.name!!.asString(), converter)
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
    val FindClass = MemberName("com.dshatz.kni.utils", "FindClass")
    val GetMethodID = MemberName("com.dshatz.kni.utils", "GetMethodID")
    val CallObjMethodA = MemberName("com.dshatz.kni.utils", "CallObjectMethodA")
    val CallVoidMethodA = MemberName("com.dshatz.kni.utils", "CallVoidMethodA")
    val memScoped = MemberName("kotlinx.cinterop", "memScoped")
    val allocArray = MemberName("kotlinx.cinterop", "allocArray")
    val reinterpret = MemberName("kotlinx.cinterop", "reinterpret")

    internal fun callHelper(type: ClassName): MemberName {
        return when (type.copy(nullable = false)) {
            TypeMatcher.KString, TypeMatcher.KByteArray -> {
                CallObjMethodA
            }
            UNIT -> CallVoidMethodA
            else -> MemberName("com.dshatz.kni.utils", "Call${type.simpleName}MethodA")
        }
    }

    val returnTypeConverters = mapOf(
        TypeMatcher.KString to CodeBlock.of("?.%M(env)", TypeMatcher.Method.ToKString),
        TypeMatcher.KByteArray to CodeBlock.of("?.%M(env)", TypeMatcher.Method.ToKByteArray)
    )
}
