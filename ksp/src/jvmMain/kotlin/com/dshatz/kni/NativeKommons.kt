package com.dshatz.kni

import com.dshatz.kni.Registry.Platform
import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.annotations.JniSerializable
import com.dshatz.kni.annotations.JniSerializerFor
import com.dshatz.kni.annotations.JniAdapter
import com.dshatz.kni.flow.FlowProcessor
import com.dshatz.kni.jniCall.CallbackProcessor
import com.dshatz.kni.jniCall.JniCallProcessor
import com.dshatz.kni.model.KSDefinedSerializer
import com.dshatz.kni.model.KSWrapper
import com.dshatz.kni.processors.ConverterProcessor
import com.dshatz.kni.serialization.SerializerProcessor
import com.dshatz.kni.serialization.serializerClass
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getFunctionDeclarationsByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
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

        private val mapper: TypeMapper = TypeMapper(registry, env.logger)
        val serializableProcessor = SerializerProcessor(registry, env.logger)
        val callbackProcessor = CallbackProcessor(registry, env.logger, mapper)
        private val jniCallProcessor = JniCallProcessor(registry, env.logger, mapper)
        private val flowProcessor = FlowProcessor(registry, env.logger, mapper)
        private val converterProcessor = ConverterProcessor(mapper, env.logger, registry)
        private val platformsProcessed: MutableSet<Platform> = mutableSetOf()

        private fun getCurrentPlatform(): Platform {
            return if (env.platforms.singleOrNull()?.platformName == "JVM") Platform.JVM
            else if (env.platforms.size > 1) Platform.COMMON
            else Platform.NATIVE
        }

        private fun doProcess(
            resolver: Resolver,
            originatingFiles: List<KSFile>,
            platform: Platform
        ): List<KSAnnotated> {

            fun FileSpec.write() {
                writeTo(codeGenerator, aggregating = true, originatingKSFiles = originatingFiles)
            }
            fun Collection<FileSpec>.write() {
                forEach {
                    it.writeTo(codeGenerator, aggregating = true, originatingKSFiles = originatingFiles)
                }
            }

            val serializables = serializableProcessor.processSerializables(resolver).also {
                registry.generatedSerializers.addAll(it)
                registry.serializers.putAll(it.associate { it.cls to KSDefinedSerializer(it.cls, it.cls.serializerClass()) })
            }

            jniCallProcessor.processJniAdapters(resolver, platform)

            serializableProcessor.collectDefinedSerializers(resolver)
            callbackProcessor.collectCallbackClasses(resolver)
            jniCallProcessor.collectNativeInstanceClasses(resolver)

            collectWrappers(resolver, registry)

            callbackProcessor.collectCallbacks(resolver)
            jniCallProcessor.collectNativeInstances(resolver)
            jniCallProcessor.collectJniCalls(resolver)
            flowProcessor.process()

            when (platform) {
                Platform.COMMON -> {
                    callbackProcessor.generateBaseSuspendAdapters().write()
                    flowProcessor.generateCommon().write()
                }
                else -> {
                    serializableProcessor.collectGenericSerializers()

                    serializableProcessor.generateSerializers(serializables).write()
                    serializableProcessor.generateGenericSerializers()?.write()
                    when (platform) {
                        Platform.NATIVE -> {
                            jniCallProcessor.generateNative().write()
                            callbackProcessor.generateNative().write()
                        }
                        Platform.JVM -> {
                            jniCallProcessor.generateJvm().write()
                            callbackProcessor.generateJvm().write()
                        }
                    }
                    converterProcessor.generateConverters(platform).write()
                }
            }

            return emptyList()
        }

        override fun process(resolver: Resolver): List<KSAnnotated> {
            val platform = getCurrentPlatform()
            // Force one processing round per platform.
            if (platform in platformsProcessed) return emptyList()
            else platformsProcessed += platform
            env.logger.info("Platform: $platform")

            val allSymbols = resolver.getSymbolsWithAnnotation(JniCall::class.java.name) +
                    resolver.getSymbolsWithAnnotation(JniCallback::class.java.name) +
                    resolver.getSymbolsWithAnnotation(JniSerializable::class.java.name) +
                    resolver.getSymbolsWithAnnotation(JniSerializerFor::class.java.name)
            val originatingFiles = allSymbols.mapNotNull { it.containingFile }.toList()
            try {
                return doProcess(resolver, originatingFiles, platform)
            } finally {
                val deps = Dependencies(true, sources = originatingFiles.toTypedArray())
                codeGenerator.createNewFile(
                    deps,
                    "com.dshatz.kni.debug",
                    "nativeInstances",
                    "txt"
                ).write(registry.nativeInstances.values.joinToString("\n").encodeToByteArray())

                codeGenerator.createNewFile(
                    deps,
                    "com.dshatz.kni.debug",
                    "callbacks",
                    "txt"
                ).write(registry.callbacks.values.joinToString("\n").encodeToByteArray())

                codeGenerator.createNewFile(
                    deps,
                    "com.dshatz.kni.debug",
                    "serializables",
                    "txt"
                ).write(registry.serializersToString().encodeToByteArray())

                codeGenerator.createNewFile(
                    deps,
                    "com.dshatz.kni.debug",
                    "platformWrappers",
                    "txt"
                ).write(registry.jniAdaptersToString().encodeToByteArray())
            }
        }

        @OptIn(KspExperimental::class)
        fun collectWrappers(resolver: Resolver, registry: Registry) {

            val wrappers = resolver.getSymbolsWithAnnotation(JniAdapter::class.java.name)
                .filterIsInstance<KSClassDeclaration>()
                .map { it.toClassName() }
            registry.jniAdapters.addAll(wrappers)

            /*resolver.getFunctionDeclarationsByName("com.dshatz.kni.generated.jniadapters._getAdapter", true)
                .forEach {
                    env.logger.warn("Found adapter ${it.simpleName.asString()}")
                    val rawType = it.extensionReceiver?.resolve()?.toClassName()!!
                    val inner = it.parameters.first().type.toTypeName()
                    val returnType = it.returnType!!.resolve()
                    env.logger.info("Found jni adapter marker for $rawType, adapted as $inner using ${returnType.toClassName()}")
                    registry.jniAdapterTypes[rawType] = KSWrapper(
                        returnType.toClassName(),
                        inner
                    )
                    registry.jniAdapters.add(rawType)
                }*/
        }
    }
}