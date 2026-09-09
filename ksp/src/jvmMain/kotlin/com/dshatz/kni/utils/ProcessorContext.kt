package com.dshatz.kni.utils

import com.dshatz.kni.Registry
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSNode

open class ProcessorContext(
    open val resolver: Resolver,
    open val platform: Registry.Platform
) {
    fun forDeclaration(decl: KSNode) = TypeMappingContext(this, decl)
}

data class TypeMappingContext(
    val processorContext: ProcessorContext,
    val decl: KSNode,
): ProcessorContext(processorContext.resolver, processorContext.platform)