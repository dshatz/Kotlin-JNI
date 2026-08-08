package com.dshatz.kni.model

import com.dshatz.kni.TypeInfo
import com.dshatz.kni.Types
import com.dshatz.kni.jniCall.Def
import com.dshatz.kni.jniCall.constructNativeArgs
import com.dshatz.kni.jniCall.toJniDescriptor
import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.needsIsNullParam
import com.dshatz.kni.utils.addCode
import com.dshatz.kni.utils.addReturn
import com.dshatz.kni.utils.defineCommon
import com.dshatz.kni.utils.defineNative
import com.dshatz.kni.utils.returnType
import com.dshatz.kni.utils.withSuffix
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

sealed class KSCallbackFun: WithParent {
    abstract val name: String
    abstract val returnType: TypeInfo
    abstract val parameters: List<ParamInfo>
    abstract val callbackClass: ClassName

    open val modifiers: List<KModifier> = emptyList()

    abstract val jniReturn: TypeInfo
    abstract val jniParams: List<ParamInfo>

    data class Blocking(
        override val name: String,
        override val returnType: TypeInfo,
        override val parameters: List<ParamInfo>,
        override val callbackClass: ClassName,
        override val parent: FunctionParent
    ): KSCallbackFun() {
        override val jniReturn: TypeInfo = returnType
        override val jniParams: List<ParamInfo> = parameters
    }

    data class Suspend(
        override val name: String,
        override val returnType: TypeInfo,
        override val parameters: List<ParamInfo>,
        override val callbackClass: ClassName,
        override val parent: FunctionParent,
        override val modifiers: List<KModifier> = listOf(KModifier.SUSPEND)
    ): KSCallbackFun() {

        val onValueFun: String = "onSuccess"
        val onFailureFun: String = "onFailure"

        val suspendAdapterClass = callbackClass.withSuffix("_${name}_SuspendAdapter")
        val baseSuspendAdapterClass = callbackClass.withSuffix("_${name}_BaseSuspendAdapter")

        val suspendAdapter = TypeInfo.NativeInstance(suspendAdapterClass, baseSuspendAdapterClass)
        val suspendCallbackImpl = if (returnType == TypeInfo.Unit) Types.SuspendCallbackImpl0 else Types.SuspendCallbackImpl.parameterizedBy(returnType.kotlinType)

        fun generateSuspendAdapter(): FileSpec {
            val cls = TypeSpec.interfaceBuilder(baseSuspendAdapterClass)
                .addSuperinterface(Types.AutoCloseable)
                .addSuperinterface(Types.SuspendCallback.parameterizedBy(returnType.kotlinType))
                .build()
            return FileSpec.builder(baseSuspendAdapterClass)
                .addType(cls)
                .build()
        }

        override val jniReturn: TypeInfo = TypeInfo.Unit
        override val jniParams: List<ParamInfo> = parameters + ParamInfo("suspendCallback", suspendAdapter)
    }

    fun getSignature(): String {
        val parameterDescriptors = jniParams.joinToString("") { parameter ->
            parameter.typeInfo.jniType.jvmType.toJniDescriptor()
        }

        val returnDescriptor = jniReturn.jniType.jvmType.toJniDescriptor()

        return "($parameterDescriptors)$returnDescriptor"
    }

    fun generateNative(): FunSpec {
        val call = Def.callHelper(jniReturn)
        val (callCode, jniResultRef) = CodeBlock.builder()
            .defineNative(
                "jniCallResult",
                returnType,
                "env.%M(%N, %N, %N)",
                call,
                "adapterClassGlobal",
                name + "ID",
                "args",
            )

        val unpacked = jniResultRef.unpackCode()
        val returnConverted = CodeBlock.builder()
            .add(callCode.code)
            .add(unpacked.code)
            .build()

        val finalCode = if (this is Suspend) {
            val (callbackDef, callbackRef) = CodeBlock.builder()
                .defineCommon(
                    "suspendCallback",
                    suspendAdapter,
                    "%L",
                    TypeSpec.anonymousClassBuilder()
                        .addSuperinterface(baseSuspendAdapterClass)
                        .superclass(suspendCallbackImpl)
                        .addSuperclassConstructorParameter("it")
                        .build()
                )


            CodeBlock.builder()
                .beginControlFlow("%M", Types.Method.SuspendCancellableCoroutine)
                .add(callbackDef.code)
                .add("%L", constructNativeArgs(jniParams, callCode.code))
                .endControlFlow()
                .build()
        } else {
            CodeBlock.of("%L", constructNativeArgs(jniParams, returnConverted))
        }

        return FunSpec.builder(name)
            .addParameters(parameters.map { it.paramSpecKotlin() })
            .addKdoc(CodeBlock.of("@returns ${returnType.kotlinType} converted using $returnType"))
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(modifiers)
            .returns(returnType.kotlinType)
            .addCode(CodeBlock.builder()
                .beginControlFlow("return runIfOpen")
                .add(finalCode)
                .endControlFlow()
                .build()
            )
            .build()
    }

    fun generateJvm(): FunSpec {
        val fName = name
        val paramsSpecs = jniParams.map { it.paramSpecJvm() }
        val isNullParamSpecs = parameters.filter { it.typeInfo.needsIsNullParam() }.map {
            ParameterSpec("_${it.name}IsNull", Types.KBoolean)
        }
        val convertArgsCode = parameters.joinToCode(",\n") {
            val unpackCode = it.refJvm.unpackCode().code
            if (it.typeInfo.needsIsNullParam()) {
                CodeBlock.of("if (_%NIsNull) null else %L", it.name, unpackCode)
            } else unpackCode
        }
        val builder = FunSpec.builder(fName)
            .addParameter(ParameterSpec("instance", callbackClass))
            .addParameters(paramsSpecs)
            .addParameters(isNullParamSpecs)
            .addAnnotation(JvmStatic::class)
        val makeCall = CodeBlock.of("%N.%N(%L)", "instance", fName, convertArgsCode)
            .returnType(returnType.kotlinType)

        val (resultDef, resultRef) = CodeBlock.builder()
            .defineCommon(
                "jvmResult",
                returnType,
                "%L",
                makeCall.code
            )

        val code = if (this is Suspend) {
            CodeBlock.builder()
                .beginControlFlow("return %T(suspendCallback).%M", suspendAdapterClass, Types.Method.ExecuteSuspend)
                .add(resultDef.code)
                .add(resultRef.code)
                .endControlFlow()
                .build()
        } else {
            CodeBlock.builder()
                .add(resultDef.code)
                .add("return %L", resultRef.packToJvm().code)
                .build()
        }

        if (returnType.kotlinType != Types.UnitOrVoid) {
            builder
                .addCode(code)
                .addKdoc("return type is $returnType")
                .returns(jniReturn.jniType.jvmType)
        } else {
            builder.addStatement("%L", makeCall.code)
        }

        return builder.build()
    }
}