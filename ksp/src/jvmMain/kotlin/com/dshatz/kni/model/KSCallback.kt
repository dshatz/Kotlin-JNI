package com.dshatz.kni.model

import com.dshatz.kni.Types
import com.dshatz.kni.jniCall.Def
import com.dshatz.kni.jniCall.constructNativeArgs
import com.dshatz.kni.needsIsNullParam
import com.dshatz.kni.utils.addCode
import com.dshatz.kni.utils.addReturn
import com.dshatz.kni.utils.capitalized
import com.dshatz.kni.utils.defineCommon
import com.dshatz.kni.utils.defineNative
import com.dshatz.kni.utils.jniClassName
import com.dshatz.kni.utils.returnType
import com.dshatz.kni.utils.withSuffix
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile

data class KSCallback(
    val type: ClassName,
    val funs: List<KSCallbackFun>,
    val superType: TypeName?,
) {
    val jvmAdapterName: ClassName
        get() {
            return type.withSuffix("_JvmAdapter")
        }

    val nativeImplClass = type.withSuffix("_Native")

    fun generateNative(): FileSpec {
        val cls = type

        fun FunSpec.Builder.addParams(params: List<ParamInfo>): FunSpec.Builder {
            params.forEach {
                addParameter(it.paramSpecKotlin())
            }
            return this
        }

        val funs = funs.map(KSCallbackFun::generateNative)

        val methodIds = this.funs.map { f ->
            PropertySpec.builder("${f.name}ID", Types.JMethodID)
                .delegate(CodeBlock.of("lazyMethodId(%S, %S)", f.name, f.getSignature()))
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
            .apply {
                superType?.let(::addSuperinterface)
            }
            .addSuperclassConstructorParameter("%S", type.jniClassName())
            .addSuperclassConstructorParameter("%S", jvmAdapterName.jniClassName())
            .addSuperclassConstructorParameter("env")
            .addSuperclassConstructorParameter("instance")
            .addSuperinterface(cls)
            .build()
        val factory = FunSpec.builder("asNative${cls.simpleName.capitalized()}")
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

    fun generateJvmAdapter(): FileSpec {
        val file = jvmAdapterName
        val funs = funs.map(KSCallbackFun::generateJvm)
        return FileSpec.builder(file)
            .addType(
                TypeSpec.objectBuilder(file)
                    .addFunctions(funs).build()
            )
            .build()
    }
}