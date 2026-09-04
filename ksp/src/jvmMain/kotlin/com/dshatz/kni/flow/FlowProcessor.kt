package com.dshatz.kni.flow

import com.dshatz.kni.BaseProcessor
import com.dshatz.kni.Registry
import com.dshatz.kni.TypeMapper
import com.dshatz.kni.model.KSCallback
import com.dshatz.kni.model.flow.KSFlowProp
import com.dshatz.kni.utils.withSuffix
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.FileSpec

class FlowProcessor(
    override val registry: Registry,
    override val logger: KSPLogger,
    override val mapper: TypeMapper
) : BaseProcessor() {

    fun process() {
        prepareFlowCallbacks()
    }

    val flowProps: Sequence<KSFlowProp> get() = registry.nativeInstances.asSequence()
        .flatMap { (_, instance) -> instance.flowProps }

    private fun prepareFlowCallbacks() {
        val callbacks = flowProps.map { flowProp ->
            KSCallback(
                type = flowProp.callbackClassName,
                funs = listOf(flowProp.onValueFun),
                baseClass = flowProp.baseCallbackClass
            )
        }.associateBy { it.type }
        registry.callbacks.putAll(callbacks)
    }

    fun generateCommon(): List<FileSpec> {
        return registry.nativeInstances.mapNotNull { (_, parentInstance) ->
            val props = parentInstance.flowProps
            val fileClass = parentInstance.className.withSuffix("_FlowCallbacks")
            if (props.isNotEmpty()) {
                FileSpec.builder(fileClass)
                    .addTypes(props.map(KSFlowProp::generateFlowCallbackCommon))
                    .build()
            } else null
        }
    }
}