package com.dshatz.kni.flow

import com.dshatz.kni.BaseProcessor
import com.dshatz.kni.Registry
import com.dshatz.kni.Registry.Platform
import com.dshatz.kni.TypeMapper
import com.dshatz.kni.Types
import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.model.KSCallback
import com.dshatz.kni.model.flow.KSFlowProp
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName

class FlowProcessor(
    override val registry: Registry,
    override val logger: KSPLogger,
    override val mapper: TypeMapper
) : BaseProcessor() {

    fun process() {
        collectFlows()
        prepareFlowCallbacks()
    }

    private fun collectFlows() {
        val flowFields = registry.jniCalls
            .map { it.parent }
            .filterIsInstance<FunctionParent.Class>()
            .groupBy { it }
            .mapValues { (classLoc, _) ->
                classLoc.props.filter {
                    (it.type as? ParameterizedTypeName)?.rawType == Types.NativeBackedFlow
                }.filter {
                    if (it.isMutable) logger.error("com.dshatz.kni.flows.NativeBackedFlow<T> cannot be a mutable property")
                    !it.isMutable
                }.map {
                    context(it.declaration) {
                        val typeArg = (it.type as ParameterizedTypeName).typeArguments.first()
                        KSFlowProp(
                            name = it.name,
                            innerType = mapper.mapType(typeArg),
                            parent = classLoc
                        )
                    }
                }
            }
        registry.flowFields.putAll(flowFields)
    }

    private fun prepareFlowCallbacks() {
        registry.flowFields.map { (parentClass, fields) ->
            val flowCallbacks = fields.map { flowProp ->
                KSCallback(
                    type = flowProp.baseCallbackClass,
                    funs = listOf(flowProp.onValueFun),
                    dependency = parentClass.declaration.containingFile!!
                )
            }.associateBy { it.type }
            registry.callbacks.putAll(flowCallbacks)
        }
    }

    fun generateCommon(): List<FileSpec> {
        return registry.flowFields.keys.map { parent ->
            val fields = registry.flowFields[parent].orEmpty()
            val fileClass = ClassName(
                parent.className.packageName,
                "${parent.className.simpleName}FlowCallbacks"
            )
            FileSpec.builder(fileClass)
                .addTypes(fields.map(KSFlowProp::generateFlowCallbackCommon))
                .build()
        }
    }
}