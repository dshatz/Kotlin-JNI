package com.dshatz.kni.model.flow

import com.dshatz.kni.TypeInfo
import com.dshatz.kni.Types
import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.model.KSCallbackFun
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.model.WithClassParent
import com.dshatz.kni.utils.add
import com.dshatz.kni.utils.capitalized
import com.dshatz.kni.utils.define
import com.dshatz.kni.utils.originatesFrom
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

data class KSFlowProp(
    val name: String,
    val innerType: TypeInfo,
    override val parent: FunctionParent.Class
): WithClassParent {
    val fullType: TypeName = Types.NativeBackedFlow.parameterizedBy(innerType.kotlinType)

    val initFunction: MemberName = MemberName(parent.className, "${name}InitJNI")

    val callbackClass: FunctionParent.Class = FunctionParent.Class(
        declaration = parent.declaration,
        className = ClassName(parent.className.packageName, "${name.capitalized()}FlowCallback"),
        constructors = emptyList(),
        superTypes = listOf(Types.FlowCallback.parameterizedBy(innerType.kotlinType)),
        props = emptyList()
    )

    val baseCallbackClass: ClassName = ClassName(
        callbackClass.className.packageName,
        "Base" + callbackClass.className.simpleName.capitalized()
    )

    val onValueFun: KSCallbackFun = KSCallbackFun(
        name = "onValue",
        returnType = TypeInfo.Unit,
        parameters = listOf(ParamInfo("value", innerType)),
        parent = callbackClass
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
            .originatesFrom(parent.declaration)
            .build()
    }

    fun generateGetValueFun(): FunSpec {
        return FunSpec.builder(initFunction)
            .returns(innerType.jniType.jvmType)
            .addParameter(
                ParameterSpec.builder("instance", Types.KLong).defaultValue("nativeInstance")
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
            .define(
                name = "defaultValue",
                type = innerType,
                "%N(callback = %L())",
                initFunction.simpleName,
                callbackClass.className.simpleName
            )

        val defaultValueConverted = defaultValue.unpackCodeJvm()
        val returnCode = CodeBlock.builder().addStatement("%T(%L)", Types.NativeBackedFlow, defaultValueConverted.code).build()

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
}