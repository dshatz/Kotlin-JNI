package dev.datlag.nkommons.callable

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
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
import dev.datlag.nkommons.TypeMatcher
import dev.datlag.nkommons.TypeMatcher.Environment
import dev.datlag.nkommons.utils.dereferenceTypeAlias

object NativeCallable {

    data class CallableBridge(
        val fileSpec: FileSpec,
        val deps: Dependencies,
        val cls: ClassName
    )

    fun generateNativeBridge(cls: KSClassDeclaration, logger: KSPLogger): CallableBridge? {
        val implCls = cls.toClassName().getNativeImplClass()
        if (cls.classKind != ClassKind.INTERFACE) {
            logger.error("@CallableFromNative can only be applied to an interface.")
            return null
        }
        if (cls.superTypes.none { it.resolve().toClassName() == TypeMatcher.Disposable }) {
            logger.error("@CallableFromNative annotated interface should extend Disposable.", cls)
            return null
        }
        val funs = cls.declarations.filterIsInstance<KSFunctionDeclaration>().filterNot { it.isConstructor() }.map { f ->
            val originalReturnType = f.returnType?.resolve()?.toClassName() ?: error("Failed to resolve return type: ${f.returnType}")
            val returnType = f.returnType?.dereferenceTypeAlias()?.toClassName() ?: error("Failed to resolve return type: ${f.returnType}")
            val call = Def.callHelper(returnType)
            val returnConverter = Def.returnTypeConverters[returnType] ?: CodeBlock.of("")
            val nullCheck = if (returnType.isNullable) "" else CodeBlock.of("!!")
            val callCode = CodeBlock.builder().addStatement("env.%M(%N, %N, %N)%L$nullCheck", call, "ref", "methodId", "args", returnConverter).build()
            FunSpec.builder(f.simpleName.asString())
                .also {
                    f.parameters.forEach { param ->
                        it.addParameter(
                            ParameterSpec(param.name!!.asString(), param.type.resolve().toClassName())
                        )
                    }
                }
                .addModifiers(KModifier.OVERRIDE)
                .returns(f.returnType?.resolve()?.toClassName() ?: UNIT)
                .addCode(CodeBlock.builder()
                    .addStatement("val cls = jvmClass")
                    .addStatement("val methodId = env.%M(cls, %S, %S)!!", Def.GetMethodID, f.simpleName.asString(), f.getSignature())
                    .add("%L", buildArgs(f.parameters, callCode))
                    .build()
                )
                .build()
        }

        val constructor = FunSpec.constructorBuilder()
            .addParameter(ParameterSpec("env", Environment))
            .addParameter(ParameterSpec("instance", TypeMatcher.JObject))
            .build()

        val bridgeClass = TypeSpec.classBuilder(implCls)
            .addFunctions(funs.toList())
            .superclass(TypeMatcher.BaseCallback)
            .primaryConstructor(constructor)
            .addSuperclassConstructorParameter("env")
            .addSuperclassConstructorParameter("instance")
            .addSuperinterface(cls.toClassName())
            .addProperty(generateGetClass(cls))
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

private fun generateGetClass(cls: KSClassDeclaration): PropertySpec {
    val name = cls.qualifiedName!!.asString().replace('.', '/')
    return PropertySpec.builder("jvmClass", TypeMatcher.JObject)
        .getter(FunSpec.getterBuilder().addStatement("return %N.%M(%S)!!", "env", Def.FindClass, name).build())
        .build()
}

private fun optin(): AnnotationSpec {
    return AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
        .addMember("%T::class", ClassName("kotlinx.cinterop", "ExperimentalForeignApi"))
        .build()
}

internal object Def {
    val FindClass = MemberName("dev.datlag.nkommons.utils", "FindClass")
    val GetMethodID = MemberName("dev.datlag.nkommons.utils", "GetMethodID")
    val CallObjMethodA = MemberName("dev.datlag.nkommons.utils", "CallObjectMethodA")
    val CallVoidMethodA = MemberName("dev.datlag.nkommons.utils", "CallVoidMethodA")
    val memScoped = MemberName("kotlinx.cinterop", "memScoped")
    val allocArray = MemberName("kotlinx.cinterop", "allocArray")
    val reinterpret = MemberName("kotlinx.cinterop", "reinterpret")

    internal fun callHelper(type: ClassName): MemberName {
        return when (type) {
            TypeMatcher.KString, TypeMatcher.KByteArray -> {
                CallObjMethodA
            }
            UNIT -> CallVoidMethodA
            else -> MemberName("dev.datlag.nkommons.utils", "Call${type.simpleName}MethodA")
        }
    }

    val returnTypeConverters = mapOf(
        TypeMatcher.KString to CodeBlock.of("?.%M(env)", TypeMatcher.Method.ToKString),
        TypeMatcher.KByteArray to CodeBlock.of("?.%M(env)", TypeMatcher.Method.ToKByteArray)
    )
}
