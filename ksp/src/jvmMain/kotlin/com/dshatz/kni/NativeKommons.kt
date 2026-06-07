package com.dshatz.kni

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.callable.CallableProcessor
import com.dshatz.kni.callable.CallbackProcessor
import com.dshatz.kni.serialization.SerializerProcessor
import com.dshatz.kni.serialization.serializerClass
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.jvm.java

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


        private var generated = false

        private val mapper: TypeMapper = TypeMapper(registry, env.logger)

        val serializableProcessor = SerializerProcessor(registry, env.logger)
        val callbackProcessor = CallbackProcessor(mapper, env.logger)
        private val generator = CallableProcessor(registry, env.logger, mapper)

        override fun process(resolver: Resolver): List<KSAnnotated> {
            val isJvm = env.platforms.singleOrNull()?.platformName == "JVM"
            val isCommon = env.platforms.size > 1

            env.logger.warn("Platforms: ${env.platforms.joinToString { it.platformName }}")

            val serializables = serializableProcessor.findSerializables(env, resolver).also {
                registry.serializers.putAll(it.mapValues {
                    it.key.serializerClass()
                })
            }

            val externalSerializers = serializableProcessor.findSerializers(resolver, env)
            registry.serializers.putAll(externalSerializers)

            if (isCommon) {
                serializableProcessor.generateSerializers(serializables).forEach {
                    it.writeTo(codeGenerator, Dependencies(false))
                }
            }

            val callbacks = callbackProcessor.getAnnotatedCallbacks(resolver)
            val callables = getAnnotatedCallables(resolver)

            val callbackDeclarations = callbackProcessor.getCallbackDeclarations(callbacks)
            registry.callbacks.addAll(callbackDeclarations.map { it.cls })

            generator.analyzeDeclarations(callables).also {
                registry.declarations.addAll(it)
            }

            if (isCommon && !generated) {
                serializableProcessor.generateGenericSerializers()
                    .writeTo(codeGenerator, Dependencies(false))
            }

            getNativeInstances(resolver).also {
                registry.nativeInstances.addAll(it.map { it.toClassName() })
            }

            if (!isJvm && !isCommon) {
                val bridges = callbackDeclarations.map { callbackProcessor.generateNativeCallback(it) }
                // native
                if (bridges.any { it == null }) {
                    env.logger.error("Callback processing failed")
                    error("Callback processing failed")
                    return emptyList()
                }

                bridges.filterNotNull().forEach {
                    it.fileSpec.writeTo(codeGenerator, it.deps)
                }
            }

            val sources = callables.mapNotNull { it.containingFile }.distinct().toTypedArray()
            if (!generated) {
                if (isJvm) {
                    callbackDeclarations.map {
                        callbackProcessor.generateJvmAdapter(it)
                    }.forEach {
                        it.writeTo(codeGenerator, Dependencies(false, sources = sources))
                    }
                    generator.generateJvmActuals().forEach {
                        it.writeTo(codeGenerator, Dependencies(false, sources = sources))
                    }
                } else if (!isCommon) {
                    generator.generateNativeFuns().forEach {
                        it.writeTo(codeGenerator, Dependencies(false, sources = sources))
                    }
                }
                generated = true
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

        private fun getNativeInstances(resolver: Resolver): List<KSClassDeclaration> {
            val jniCallFunctions = resolver.getSymbolsWithAnnotation(JniCall::class.java.name).toList()
            return jniCallFunctions.filterIsInstance<KSFunctionDeclaration>().mapNotNull { it.closestClassDeclaration()?.takeIf { it.classKind == ClassKind.CLASS } }
        }
    }
}