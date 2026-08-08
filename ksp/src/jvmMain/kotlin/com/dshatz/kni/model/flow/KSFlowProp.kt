package com.dshatz.kni.model.flow

import com.dshatz.kni.CNameUtils.cname
import com.dshatz.kni.CNameUtils.cnameFunName
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.Types
import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.model.KSCallbackFun
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.model.WithClassParent
import com.dshatz.kni.utils.add
import com.dshatz.kni.utils.addCode
import com.dshatz.kni.utils.addReturn
import com.dshatz.kni.utils.capitalized
import com.dshatz.kni.utils.cnameFunBuilder
import com.dshatz.kni.utils.defineCommon
import com.dshatz.kni.utils.defineJvm
import com.dshatz.kni.utils.nativeCode
import com.dshatz.kni.utils.originatesFrom
import com.dshatz.kni.utils.withSuffix
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import jdk.javadoc.internal.doclets.toolkit.util.DocPath.parent

data class KSFlowProp(
    val name: String,
    val innerType: TypeInfo,
    val instanceClass: ClassName
) {
    val fullType: TypeName = Types.NativeBackedFlow.parameterizedBy(innerType.kotlinType)

    val initFunction: MemberName = MemberName(instanceClass, "${name}InitJNI")

    val callbackClass: FunctionParent.Class = FunctionParent.Class(
        className = instanceClass.withSuffix("_${name}_FlowCallback"),
        superTypes = listOf(Types.FlowCallback.parameterizedBy(innerType.kotlinType)),
    )

    val baseCallbackClass: ClassName = ClassName(
        callbackClass.className.packageName,
        "Base" + callbackClass.className.simpleName.capitalized()
    )

    val onValueFun: KSCallbackFun = KSCallbackFun.Blocking(
        name = "onValue",
        returnType = TypeInfo.Unit,
        parameters = listOf(ParamInfo("value", innerType)),
        parent = callbackClass,
        callbackClass = baseCallbackClass
    )

    private fun onValueFunBuilder() : FunSpec.Builder {
        return FunSpec.builder("onValue")
            .addParameter(ParameterSpec("value", innerType.kotlinType))
    }

    fun generateFlowCallbackJvm(): TypeSpec {
        val onValue = onValueFunBuilder()
            .addModifiers(KModifier.OVERRIDE)
                .addCode("%N.onValue(value)", name)
            .addAnnotation(Types.Annotations.Optin.KniInternalOptIn)
            .build()

        return TypeSpec.classBuilder(callbackClass.className)
            .addModifiers(KModifier.INNER)
            .addSuperinterface(baseCallbackClass)
            .addFunction(onValue)
            .addFunction(FunSpec.builder("close").addModifiers(KModifier.OVERRIDE).build())
            .addAnnotation(JniCallback::class)
            .build()
    }

    fun generateFlowCallbackCommon(): TypeSpec {
        return TypeSpec.interfaceBuilder(baseCallbackClass)
            .addSuperinterface(callbackClass.superTypes.first())
            .build()
    }

    fun generateGetValueFun(): FunSpec {
        return FunSpec.builder(initFunction)
            .returns(innerType.jniType.jvmType)
            .addParameter(
                ParameterSpec.builder("instance", Types.KLong).defaultValue("nativeInstancePtr")
                    .build()
            )
            .addParameter(
                ParameterSpec(
                    "callback",
                    Types.FlowCallback.parameterizedBy(innerType.kotlinType)
                )
            )
            .addModifiers(KModifier.EXTERNAL, KModifier.PRIVATE)
            .build()

    }

    fun generateFlowProp(): PropertySpec {
        val (initCall, defaultValue) = CodeBlock.builder()
            .defineJvm(
                name = "defaultValue",
                type = innerType,
                "%N(callback = %L())",
                initFunction.simpleName,
                callbackClass.className.simpleName
            )

        val returnCode = CodeBlock.builder().addStatement(
            "%T(%L)",
            Types.NativeBackedFlow,
            defaultValue.unpackCode().code
        ).build()

        return PropertySpec.builder(name, fullType)
            .delegate(
                CodeBlock.builder()
                    .beginControlFlow("lazy")
                    .add(initCall)
                    .add(returnCode)
                    .endControlFlow()
                    .build()
            )
            .addModifiers(KModifier.ACTUAL)
            .build()
    }

    fun generateNativeFlowInit(): FunSpec {
        val callbackType = TypeInfo.Callback(baseCallbackClass)
        val callback = CodeBlock.of("callback").nativeCode(callbackType)

        val (initCallback, callbackRef) = CodeBlock.builder()
            .defineCommon(
                name = "callback",
                type = callbackType,
                "%L",
                callback.unpackCode().code
            )

        val (initCode, defaultValue) = CodeBlock.builder()
            .defineCommon(
                "defaultValue",
                innerType,
                "instance.%M<%T>().%N.bindToJvm(%L)",
                Types.Method.valueFromStableRefPointer,
                instanceClass,
                name,
                callbackRef.code
            )

        val converted = defaultValue.packToNative()
        return cnameFunBuilder(
            funName = initFunction.cnameFunName(),
            cname = initFunction.cname()
        )
            .addParameter("instance", Types.JLong)
            .addParameter("callback", Types.JObject)
            .addCode(initCallback)
            .addCode(initCode)
            .addReturn(converted)
            .build()
    }
}