package com.dshatz.kni.utils

import com.dshatz.kni.Registry.Platform
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSNode

open class PlatformContext(
    open val platform: Platform
)

interface SymbolContext {
    val decl: KSNode
    val forAdapter: Boolean
}

open class SymContext(override val decl: KSNode, override val forAdapter: Boolean = false): SymbolContext

interface ResolverContext{
    val resolver: Resolver
    val moduleName: String

    fun <R> runAsJvm(block: context(ProcessorContext) () -> R): R {
        return context(ProcessorContext(resolver, Platform.JVM, moduleName)) {
            block()
        }
    }
}

open class ProcessorContext(
    override val resolver: Resolver,
    override val platform: Platform,
    override val moduleName: String,
): PlatformContext(platform), ResolverContext {
    fun forDeclaration(decl: KSNode) = TypeMappingContext(this, decl)
}

class NativeContext: PlatformContext(Platform.NATIVE)

class JvmContext: PlatformContext(Platform.JVM)

data class TypeMappingContext(
    val processorContext: ProcessorContext,
    override val decl: KSNode,
    override val forAdapter: Boolean = false,
): ProcessorContext(processorContext.resolver, processorContext.platform, processorContext.moduleName), SymbolContext