package com.dshatz.kni.model

import com.dshatz.kni.CNameUtils
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.Types
import com.dshatz.kni.model.flow.KSFlowProp
import com.dshatz.kni.utils.cnameFunBuilder
import com.dshatz.kni.utils.commonCode
import com.dshatz.kni.utils.nativeCode
import com.dshatz.kni.utils.withSuffix
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.joinToCode

data class KSInstance(
    val className: ClassName,
    val constructors: List<KSConstructor>,
    val funs: List<KSJniCall>,
    val flowProps: List<KSFlowProp>
) {
    val typeInfo = TypeInfo.NativeInstance(className)

    fun generateNative(): FileSpec {
        val funSpecs = funs.map(KSJniCall::generateCnameFunction)
        val constructors = generateNativeConstructors()
        val dispose = generateNativeDispose()

        val flowGetValueFuncs = flowProps.map { flowProp ->
            flowProp.generateNativeFlowInit()
        }

        val fileClassName = className.withSuffix("_jniCalls")
        return FileSpec.builder(fileClassName)
            .addFunctions(funSpecs)
            .addFunctions(flowGetValueFuncs)
            .addFunctions(constructors)
            .addFunction(dispose)
            .build()
    }

    private fun generateNativeConstructors(
    ): List<FunSpec> {
        val returnTypeInfo = typeInfo
        return constructors.map { constructor ->
            val jniCName = CNameUtils.jniFunctionCName(
                packageName = className.packageName,
                className = className.simpleName,
                functionName = "initNative${constructor.id}"
            )
            val paramSpecs = constructor.params.map(ParamInfo::paramSpecNative)
            val paramConversion = constructor.params.map {
                it.refNative.unpackCode()
            }.joinToCode { it.code }

            val initNativeCode = CodeBlock.builder()
                .addStatement("%T(%L)", returnTypeInfo.kotlinType, paramConversion)
                .build()
                .commonCode(returnTypeInfo)

            val returnCode = CodeBlock.builder()
                .addStatement("return %L", initNativeCode.packToNative().code)
                .build()

            cnameFunBuilder(
                MemberName(className, "init${constructor.id}"),
                jniCName
            )
                .returns(LONG)
                .addParameters(paramSpecs)
                .addCode(returnCode)
                .build()
        }
    }

    private fun generateNativeDispose(): FunSpec {
        val jniCname = CNameUtils.jniFunctionCName(
            packageName = className.packageName,
            className = className.simpleName,
            functionName = "disposeNative"
        )
        val instanceType = typeInfo
        val unpack = CodeBlock.of("%N", "instance").nativeCode(instanceType).unpackCode()
        return cnameFunBuilder(
            MemberName(className, "disposeNative"),
            jniCname
        ).addParameter(
            ParameterSpec("instance", instanceType.jniType.nativeType)
        )
            .addCode(
                CodeBlock.builder()
                    .addStatement("%L.close()", unpack.code)
                    .addStatement("%N.%M<%T>()", "instance", Types.Method.ReleaseStableRef, className)
                    .build()
            )
            .build()
    }
}