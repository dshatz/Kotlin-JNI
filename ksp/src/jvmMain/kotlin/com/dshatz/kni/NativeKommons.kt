package com.dshatz.kni

import com.dshatz.kni.Registry.Platform
import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.annotations.JniSerializable
import com.dshatz.kni.annotations.JniSerializerFor
import com.dshatz.kni.flow.FlowProcessor
import com.dshatz.kni.jniCall.CallbackProcessor
import com.dshatz.kni.jniCall.JniCallProcessor
import com.dshatz.kni.model.KSDefinedSerializer
import com.dshatz.kni.serialization.SerializerProcessor
import com.dshatz.kni.serialization.serializerClass
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FileSpec
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

        private val mapper: TypeMapper = TypeMapper(registry, env.logger)
        val serializableProcessor = SerializerProcessor(registry, env.logger)
        val callbackProcessor = CallbackProcessor(registry, env.logger, mapper)
        private val jniCallProcessor = JniCallProcessor(registry, env.logger, mapper)
        private val flowProcessor = FlowProcessor(registry, env.logger, mapper)
        private val platformsProcessed: MutableSet<Platform> = mutableSetOf()

        private fun getCurrentPlatform(): Platform {
            return if (env.platforms.singleOrNull()?.platformName == "JVM") Platform.JVM
            else if (env.platforms.size > 1) Platform.COMMON
            else Platform.NATIVE
        }

        override fun process(resolver: Resolver): List<KSAnnotated> {
            val allSymbols = resolver.getSymbolsWithAnnotation(JniCall::class.java.name) +
                    resolver.getSymbolsWithAnnotation(JniCallback::class.java.name) +
                    resolver.getSymbolsWithAnnotation(JniSerializable::class.java.name) +
                    resolver.getSymbolsWithAnnotation(JniSerializerFor::class.java.name)
            val originatingFiles = allSymbols.mapNotNull { it.containingFile }.toList()

            fun FileSpec.write() {
                writeTo(codeGenerator, aggregating = true, originatingKSFiles = originatingFiles)
            }
            fun Collection<FileSpec>.write() {
                forEach {
                    it.writeTo(codeGenerator, aggregating = true, originatingKSFiles = originatingFiles)
                }
            }

            val platform = getCurrentPlatform()

            // Force one processing round per platform.
            if (platform in platformsProcessed) return emptyList()
            else platformsProcessed += platform
            env.logger.info("Platform: $platform")

            val serializables = serializableProcessor.findSerializables(resolver).also {
                registry.generatedSerializers.addAll(it)
                registry.serializers.putAll(it.associate { it.cls to KSDefinedSerializer(it.cls, it.cls.serializerClass()) })
            }

            serializableProcessor.collectDefinedSerializers(resolver)
            jniCallProcessor.collectNativeInstances(resolver)
            callbackProcessor.collectDeclarations(resolver)
            jniCallProcessor.collectJniCalls(resolver)
            flowProcessor.process()
            serializableProcessor.collectGenericSerializers()

            when (platform) {
                Platform.COMMON -> {
                    callbackProcessor.generateSuspendAdapters().write()
                    serializableProcessor.generateSerializers(serializables).write()
                    serializableProcessor.generateGenericSerializers()?.write()
                    flowProcessor.generateCommon().write()
                }
                Platform.NATIVE -> {
                    jniCallProcessor.generateNative().write()
                    callbackProcessor.generateNative().write()
                }
                Platform.JVM -> {
                    callbackProcessor.generateJvm().write()
                    jniCallProcessor.generateJvm().write()
                }
            }

            return emptyList()
        }

    }
}