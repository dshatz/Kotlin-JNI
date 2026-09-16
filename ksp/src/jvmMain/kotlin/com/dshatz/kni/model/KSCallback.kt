package com.dshatz.kni.model

import com.dshatz.kni.Registry
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.Types
import com.dshatz.kni.utils.JvmContext
import com.dshatz.kni.utils.NativeContext
import com.dshatz.kni.utils.PlatformContext
import com.dshatz.kni.utils.ResolverContext
import com.dshatz.kni.utils.capitalized
import com.dshatz.kni.utils.jniClassName
import com.dshatz.kni.utils.withSuffix
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

data class KSCallback(
    val type: ClassName,
    val funs: List<KSCallbackFun>,
    val baseClass: ClassName?,
    val platform: Registry.Platform
) {
    val typeInfo get() = context(PlatformContext(platform)) { TypeInfo.callback(type, baseClass = baseClass) }
    val jvmAdapterName: ClassName
        get() {
            return type.withSuffix("_JvmAdapter")
        }

    val nativeImplClass = type.withSuffix("_Native")

    context(_: NativeContext, _: ResolverContext)
    fun generateNative(): FileSpec {
        val funs = funs.map { it.generateNative() }

        val methodIds = this.funs.map { f ->
            PropertySpec.builder("${f.name}ID", Types.JMethodID)
                .delegate(CodeBlock.of("lazyMethodId(%S, %S)", f.name, f.jvmSignature))
                .build()
        }

        val constructor = FunSpec.constructorBuilder()
            .addParameter(ParameterSpec("env", Types.Environment))
            .addParameter(ParameterSpec("instance", Types.JObject))
            .build()

        val bridgeClass = TypeSpec.classBuilder(nativeImplClass)
            .addFunctions(funs.toList())
            .addProperties(methodIds.toList())
            .superclass(Types.BaseCallback)
            .primaryConstructor(constructor)
            .addSuperclassConstructorParameter("%S", typeInfo.commonKotlinType.jniClassName())
            .addSuperclassConstructorParameter("%S", jvmAdapterName.jniClassName())
            .addSuperclassConstructorParameter("env")
            .addSuperclassConstructorParameter("instance")
            .addSuperinterface(baseClass ?: type)
            .build()
        val factory = FunSpec.builder("asNative${type.simpleName.capitalized()}")
            .receiver(Types.JObject)
            .addParameter("env", Types.Environment)
            .returns(nativeImplClass)
            .addCode(CodeBlock.of("return %T(env, this)", nativeImplClass))
            .build()

        val fileSpec = FileSpec.builder(nativeImplClass)
            .addType(bridgeClass)
            .addFunction(factory)
            .addImport("kotlinx.cinterop", "get")
            .addAnnotation(Types.Annotations.Optin.NativeOptIn)
            .build()
        return fileSpec
    }

    context(_: JvmContext, _: ResolverContext)
    fun generateJvmAdapter(): FileSpec {
        val file = jvmAdapterName
        val funs = funs.map { it.generateJvm() }
        return FileSpec.builder(file)
            .addType(
                TypeSpec.objectBuilder(file)
                    .addFunctions(funs).build()
            )
            .build()
    }
}