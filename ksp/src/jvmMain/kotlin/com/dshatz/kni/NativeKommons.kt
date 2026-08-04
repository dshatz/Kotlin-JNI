package com.dshatz.kni

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
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.toClassName
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
        val callbackProcessor = CallbackProcessor(mapper, env.logger, registry)
        private val jniCallProcessor = JniCallProcessor(registry, env.logger, mapper)
        private val flowProcessor = FlowProcessor(registry, env.logger, mapper)


        fun processV1(resolver: Resolver): List<KSAnnotated> {
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

            val isJvm = env.platforms.singleOrNull()?.platformName == "JVM"
            val isCommon = env.platforms.size > 1
            val isNative = !isJvm && !isCommon
            env.logger.info("Platforms: ${env.platforms.joinToString { it.platformName }}")

            val serializables = serializableProcessor.findSerializables(resolver).also {
                registry.generatedSerializers.addAll(it)
                registry.serializers.putAll(it.associate { it.cls to KSDefinedSerializer(it.cls, it.cls.serializerClass()) })
            }
            val definedSerializers = serializableProcessor.collectDefinedSerializers(resolver)
            registry.serializers.putAll(definedSerializers)

            callbackProcessor.collectDeclarations(resolver)
            jniCallProcessor.collectJniCalls(resolver)
            flowProcessor.process()
            serializableProcessor.collectGenericSerializers()


            if (isCommon) {
                serializableProcessor.generateSerializers(serializables).write()
            }

            getNativeInstances(resolver).also {
                registry.nativeInstances.addAll(it.map { it.toClassName() })
            }

            if (isCommon) {
                serializableProcessor.generateGenericSerializers()?.write()
            }

            if (isJvm) {
                callbackProcessor.generateJvm().write()
                jniCallProcessor.generateJvm().write()
            } else if (isNative) {
                jniCallProcessor.generateNative().write()
                callbackProcessor.generateNative().map { it.fileSpec }.write()
            } else {
                // common
                flowProcessor.generateCommon().write()
            }
            registry.clear()
            return emptyList()
        }

        override fun process(resolver: Resolver): List<KSAnnotated> {
            return processV1(resolver)
        }

        private fun getNativeInstances(resolver: Resolver): List<KSClassDeclaration> {
            val jniCallFunctions = resolver.getSymbolsWithAnnotation(JniCall::class.java.name).toList()
            return jniCallFunctions.filterIsInstance<KSFunctionDeclaration>().mapNotNull { it.closestClassDeclaration()?.takeIf { it.classKind == ClassKind.CLASS } }
        }
    }
}