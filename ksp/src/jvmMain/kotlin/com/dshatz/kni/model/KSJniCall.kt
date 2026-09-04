package com.dshatz.kni.model

import com.dshatz.kni.CNameUtils
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.Types
import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.needsIsNullParam
import com.dshatz.kni.processors.packMember
import com.dshatz.kni.utils.cnameFunBuilder
import com.dshatz.kni.utils.commonCode
import com.dshatz.kni.utils.defineCommon
import com.dshatz.kni.utils.nativeCode
import com.dshatz.kni.utils.nonNullOrPlaceholder
import com.dshatz.kni.utils.nullSafeCall
import com.dshatz.kni.utils.returnType
import com.dshatz.kni.utils.withSuffix
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.joinToCode

sealed class KSJniCall: WithParent {

    /**
     * Original function name.
     */
    abstract val name: String
    abstract val returnType: TypeInfo
    abstract val parameters: List<ParamInfo>

    abstract val jniParams: List<ParamInfo>
    abstract val jniReturn: TypeInfo

    abstract val modifiers: Set<KModifier>

    val externalFun: MemberName by lazy { parent.member("${name}External") }

    /**
     * What to call from user-defined function.
     */
    abstract val callToExternal: MemberName

    abstract val nativeInstance: TypeInfo.NativeInstance?

    data class Blocking(
        override val name: String,
        override val returnType: TypeInfo,
        override val parameters: List<ParamInfo>,
        override val parent: FunctionParent,
        override val modifiers: Set<KModifier>,
        override val nativeInstance: TypeInfo.NativeInstance?
    ): KSJniCall() {
        override val jniParams: List<ParamInfo> by lazy {
            parameters + listOfNotNull(nativeInstance?.let {
                ParamInfo("instance", it)
            })
        }
        override val jniReturn: TypeInfo = returnType
        override val callToExternal: MemberName = externalFun
    }

    data class Suspend(
        override val name: String,
        override val returnType: TypeInfo,
        override val parameters: List<ParamInfo>,
        override val parent: FunctionParent,
        private val additionalModifiers: Set<KModifier>,
        override val nativeInstance: TypeInfo.NativeInstance?
    ): KSJniCall() {
        val suspendCallbackClass: ClassName = parent.className.withSuffix("_${name}_SuspendCallback")
        val baseSuspendCallback = suspendCallbackClass.withSuffix("Base")
        val externalAsyncFun: MemberName = parent.member("${externalFun.simpleName}Async")
        override val callToExternal: MemberName = externalAsyncFun
        val onValueFun: String = "onSuccess"
        val onFailureFun: String = "onFailure"
        override val jniParams: List<ParamInfo> by lazy {
            parameters + ParamInfo(
                "callback",
                callbackType
            ) + listOfNotNull(nativeInstance?.let {
                ParamInfo("instance", it)
            })
        }
        override val jniReturn: TypeInfo = TypeInfo.Unit
        override val modifiers: Set<KModifier> = additionalModifiers + KModifier.SUSPEND

        val callbackType = TypeInfo.Callback(baseSuspendCallback)
        private val jvmSuspendAdapter: TypeName = if (returnType == TypeInfo.Unit) {
            Types.SuspendCallbackImpl0
        } else Types.SuspendCallbackImpl.parameterizedBy(returnType.kotlinType)

        fun generateBaseSuspendAdapter(): FileSpec {
            val cls = TypeSpec.interfaceBuilder(baseSuspendCallback)
                .addKdoc("Callback for calling suspend function [%T.%N].", parent.className, name)
                .addSuperinterface(Types.AutoCloseable)
                .addSuperinterface(Types.SuspendCallback.parameterizedBy(returnType.kotlinType))
                .build()
            return FileSpec.builder(baseSuspendCallback)
                .addType(cls)
                .build()
        }

        fun externalAsyncSpec(): FunSpec {
            val anonCallback = TypeSpec.anonymousClassBuilder()
                .addSuperinterface(baseSuspendCallback)
                .superclass(jvmSuspendAdapter)
                .addSuperclassConstructorParameter("it")
                .build()
            val params = jniParams.map {
                it.refJvm
            }.joinToCode(", ") { it.code }
            return FunSpec.builder(externalAsyncFun)
                .addModifiers(KModifier.SUSPEND)
                .addParameters(
                    jniParams
                        .filterNot { it.name == "callback" }
                        .map { it.paramSpecJvm() }
                )
                .returns(returnType.kotlinType)
                .addCode(
                    CodeBlock.builder()
                        .beginControlFlow("return %M", Types.Method.SuspendCancellableCoroutine)
                        .addStatement("val callback = %L", anonCallback)
                        .addStatement("%L(%L)", externalFun.simpleName, params)
                        .endControlFlow()
                        .build()
                ).build()
        }
    }

    fun generateCnameFunction(): FunSpec {
        val callFun = parent.member(name)
        val jniCName = CNameUtils.jniFunctionCName(
            packageName = callFun.packageName,
            className = parent.classNameKt.simpleName,
            functionName = callFun.simpleName + "External"
        )

        val builder = cnameFunBuilder(
            callFun,
            jniCName
        )

        val paramCodes = jniParams.map {
            val paramCode = it.refNative
            val nullified = if (it.typeInfo.needsIsNullParam()) {
                CodeBlock.of("(if (%N.%M()) null else %L)", "_${it.name}IsNull", Types.Method.ToKBoolean, paramCode.code)
                    .nativeCode(it.typeInfo, nullable = true)
            } else paramCode
            it.name to nullified.unpackCode()
        }
        val paramDefinitions = paramCodes.map { (name, code) ->
            CodeBlock.builder().defineCommon(
                name,
                code.type,
                "%L",
                code.code
            )
        }
        val paramConversionCode = parameters.joinToCode(", ") { CodeBlock.of(it.name) }
        val callUserCode = nativeInstance?.let { instanceType ->
            val instanceParam = CodeBlock.of("%N", "instance").nativeCode(instanceType)
            CodeBlock.of("%L.%L(%L)", instanceParam.code, callFun.simpleName, paramConversionCode)
        } ?: CodeBlock.of("%M(%L)", callFun, paramConversionCode)

        val finalCode = if (this is Suspend) {
            val callback = CodeBlock.of("callback").commonCode(callbackType)
            CodeBlock.builder()
                .beginControlFlow("%L.%M", callback.code, Types.Method.ExecuteSuspend)
                .addStatement("%L", callUserCode)
                .endControlFlow()
                .build()
        } else {
            val packResult = callUserCode.returnType(returnType.kotlinType).nullSafeCall(
                returnType.packMember()
            )
            /*val packResult = returnType.packCode(callUserCode.returnType(returnType.kotlinType))*/
            CodeBlock.of("return %L", packResult.code)
        }

        val f = builder
            .returns(
                jniReturn.jniType.nativeType
            )
            .addParameters(jniParams.map {
                ParameterSpec
                    .builder(it.name, it.typeInfo.jniType.nativeType)
                    .addKdoc("${it.typeInfo.kotlinType} -> ${it.typeInfo.jniType.nativeType} (via ${it.typeInfo::class.simpleName})")
                    .build()
            })
            .addParameters(
                parameters
                    .filter {
                        it.typeInfo.needsIsNullParam()
                    }
                    .map {
                        ParameterSpec("_${it.name}IsNull", Types.JBoolean)
                    }
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement("// generated by Kotlin-JNI")
                    .addStatement("val env = env.%M()!!", Types.Method.GetAndAttach)
                    .add(paramDefinitions.joinToCode("") { it.definition.code })
                    .add("%L", finalCode)
                    .build()
            )
            .build()
        return f
    }

    internal fun generateJvmFunctions(
        instance: KSInstance? = null
    ): List<FunSpec> {
        val f = this
        val fName = MemberName(this.parent.className, f.name)
        val funSpec = FunSpec.builder(fName)
            .addModifiers(f.modifiers)
            .returns(f.returnType.kotlinType)
            .apply {
                f.parameters.forEach { (name, typeInfo) ->
                    addParameter(ParameterSpec.builder(
                        name = name,
                        type = typeInfo.kotlinType
                    ).build())
                }
            }
            .apply {
                val paramPacking = f.parameters.map { p ->
                    p.refCommon.packToJvm().typed.nonNullOrPlaceholder()
                }
                val isNullParams = f.parameters.filter { it.typeInfo.needsIsNullParam() }.map {
                    CodeBlock.of("%N == null", it.name).returnType(Types.KBoolean)
                }
                val params = if (instance == null) {
                    paramPacking + isNullParams
                } else {
                    paramPacking + isNullParams + CodeBlock.of("it").returnType(instance.typeInfo.jniType.nativeType)
                }
                val paramsCode = params.joinToCode(prefix = "\n", separator = ",\n", suffix = "\n") { it.code }
                val callExternalCode = CodeBlock.of("%L(%L)", f.callToExternal.simpleName, paramsCode).returnType(f.returnType.jniType.jvmType)
                val returnValue = if (f is KSJniCall.Suspend) {
                    callExternalCode // No need to convert, callback already converted.
                } else {
                    f.returnType.unpackCodeJvm(callExternalCode)
                }

                if (instance != null) {
                    val method = if (f is KSJniCall.Suspend) "withValidInstanceSuspend" else "withValidInstance"
                    val withValidInstanceBlock = CodeBlock.builder()
                        .beginControlFlow("return %L", method)
                        .add(returnValue.code)
                        .endControlFlow()
                        .build()
                    addCode(withValidInstanceBlock)
                } else addCode("return %L", returnValue.code)

            }.build()

        val externalAsyncSpec = (f as? KSJniCall.Suspend)?.externalAsyncSpec()


        val externalSpec = FunSpec.builder(f.externalFun)
            .addModifiers(KModifier.EXTERNAL, KModifier.PRIVATE)
            .apply {
                f.jniParams.forEach {
                    addKdoc("@param ${it.name} [${it.typeInfo.kotlinType}] converted to `${it.typeInfo.jniType.nativeType}`.\n")
                }
                if (f.jniReturn.kotlinType != UNIT) {
                    addKdoc("@return Representing `${f.returnType.describe()}`. Converted from `${f.returnType.jniType.nativeType}` to `${f.returnType.jniType.jvmType}`.\n")
                }
            }
            .returns(f.jniReturn.jniType.jvmType)
            .addParameters(
                f.jniParams.map { (name, typeInfo) ->
                    ParameterSpec.builder(
                        name = name,
                        type = typeInfo.jniType.jvmType,
                    ).build()
                }
            )
            .addParameters(f.parameters.filter {
                it.typeInfo.needsIsNullParam()
            }.map {
                ParameterSpec("_${it.name}IsNull", Types.KBoolean)
            })
            .build()
        return listOfNotNull(funSpec, externalSpec, externalAsyncSpec)
    }
}

