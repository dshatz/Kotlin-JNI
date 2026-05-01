package com.dshatz.kni

import com.dshatz.kni.annotations.AddJniSerializer
import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.callable.CallableProcessor
import com.dshatz.kni.callable.NativeCallable
import com.dshatz.kni.serialization.SerializerProcessor
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class NativeKommons : SymbolProcessorProvider {
    var called: Boolean = false
        private set


    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        called = true
        return NativeKommonsProcessor(environment)
    }

    class NativeKommonsProcessor(
        private val env: SymbolProcessorEnvironment,
    ) : SymbolProcessor {

        private val registry = Registry()

        private val codeGenerator: CodeGenerator
            get() = env.codeGenerator

        private val generator = CallableProcessor(registry, env.logger)

        override fun process(resolver: Resolver): List<KSAnnotated> {
            val isJvm = env.platforms.any { it.platformName == "JVM" }

            SerializerProcessor.processSerializers(env, getAnnotatedSerializers(resolver)).also {
                registry.serializers.putAll(it)
            }

            getNativeInstances(resolver).also {
                registry.nativeInstances.addAll(it.map { it.toClassName() })
            }

            if (!isJvm) {
                val bridges = getAnnotatedBridges(resolver).map { NativeCallable.generateNativeBridge(it, env.logger) }
                if (bridges.any { it == null }) {
                    env.logger.error("Callable processing failed")
                    return emptyList()
                }

                bridges.filterNotNull().forEach {
                    it.fileSpec.writeTo(codeGenerator, it.deps)
                    registry.callables.add(it.cls)
                }
            }


            if (isJvm) {
                val funs = getAnnotatedCallables(resolver)
                generator.generateJvmActuals(funs).forEach {
                    it.writeTo(codeGenerator, Dependencies(false, sources = funs.mapNotNull { it.containingFile }.toTypedArray()))
                }
                return emptyList()
            } else {
                val funs = getAnnotatedCallables(resolver)
                generator.generateNativeFuns(funs).forEach {
                    it.writeTo(codeGenerator, Dependencies(false, sources = funs.mapNotNull { it.containingFile }.toTypedArray()))
                }
            }
            return emptyList()
        }

        @OptIn(KspExperimental::class)
        private fun getAnnotatedCallables(resolver: Resolver): List<KSFunctionDeclaration> {
            val allowedClassKinds = setOf(ClassKind.OBJECT, ClassKind.CLASS)
            return resolver.getSymbolsWithAnnotation(JniCall::class.java.name).toList()
                .filterIsInstance<KSFunctionDeclaration>()
                .filter {
                    val parentClass = it.parent as? KSClassDeclaration
                    if (parentClass == null) {
                        // top level function
                        true
                    } else {
                        if (parentClass.classKind !in allowedClassKinds) {
                            env.logger.error("@JniCall is only supported inside classes/objects or on top-level functions.", it)
                            false
                        } else if (parentClass.classKind == ClassKind.CLASS && parentClass.superTypes.none { it.toTypeName() == TypeMatcher.AutoCloseable }) {
                            env.logger.error("Classes with @JniCall methods must implement ${TypeMatcher.AutoCloseable.canonicalName}", it)
                            false
                        } else true
                    }
                }
                .distinctBy { it.qualifiedName?.asString() }
        }

        private fun getAnnotatedBridges(resolver: Resolver): List<KSClassDeclaration> {
            val nativeCallableAnnotated = resolver.getSymbolsWithAnnotation(JniCallback::class.java.name).toList()
            return nativeCallableAnnotated.filterIsInstance<KSClassDeclaration>().distinct()
        }

        private fun getNativeInstances(resolver: Resolver): List<KSClassDeclaration> {
            val jniCallFunctions = resolver.getSymbolsWithAnnotation(JniCall::class.java.name).toList()
            return jniCallFunctions.filterIsInstance<KSFunctionDeclaration>().mapNotNull { it.closestClassDeclaration()?.takeIf { it.classKind == ClassKind.CLASS } }
        }

        private fun getAnnotatedSerializers(resolver: Resolver): List<KSClassDeclaration> {
            val serializers = resolver.getSymbolsWithAnnotation(AddJniSerializer::class.java.name).toList()
            return serializers.filterIsInstance<KSClassDeclaration>()
        }

        data class ParamInfo(
            val code: CodeBlock,
            val spec: ParameterSpec
        )
    }
}