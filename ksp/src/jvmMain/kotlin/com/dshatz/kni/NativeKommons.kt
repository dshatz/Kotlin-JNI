package com.dshatz.kni

import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.callable.CallableProcessor
import com.dshatz.kni.callable.CallbackProcessor
import com.dshatz.kni.flow.FlowProcessor
import com.dshatz.kni.model.KSDefinedSerializer
import com.dshatz.kni.serialization.SerializerProcessor
import com.dshatz.kni.serialization.serializerClass
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ksp.originatingKSFiles
import com.squareup.kotlinpoet.ksp.toClassName
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
        private var genericSerializersGenerated = false

        private val mapper: TypeMapper = TypeMapper(registry, env.logger)

        val serializableProcessor = SerializerProcessor(registry, env.logger)
        val callbackProcessor = CallbackProcessor(mapper, env.logger, registry)
        private val callableProcessor = CallableProcessor(registry, env.logger, mapper)
        private val flowProcessor = FlowProcessor(registry, env.logger, mapper)


        fun processV1(resolver: Resolver): List<KSAnnotated> {
            val isJvm = env.platforms.singleOrNull()?.platformName == "JVM"
            val isCommon = env.platforms.size > 1
            env.logger.warn("Platforms: ${env.platforms.joinToString { it.platformName }}")

            val serializables = serializableProcessor.findSerializables(env, resolver).also {
                registry.serializers.putAll(it.associate { it.cls to KSDefinedSerializer(it.cls, it.cls.serializerClass()) })
            }
            val externalSerializers = serializableProcessor.collectDefinedSerializers(env, resolver)
            registry.serializers.putAll(externalSerializers)

            callbackProcessor.collectDeclarations(resolver)
            callableProcessor.collectCallables(resolver)
            flowProcessor.process()

            if (isCommon) {
                serializableProcessor.generateSerializers(serializables).forEach {
                    it.writeTo(codeGenerator, Dependencies(false))
                }
            }

            getNativeInstances(resolver).also {
                registry.nativeInstances.addAll(it.map { it.toClassName() })
            }

            if (isCommon && !genericSerializersGenerated) {
                serializableProcessor.generateGenericSerializers()
                    .writeTo(codeGenerator, Dependencies(false))
                genericSerializersGenerated = true
            }

            if (!generated) {
                if (isJvm) {
                    callbackProcessor.generateJvm().forEach {
                        it.writeTo(codeGenerator, Dependencies(false))
                    }
                    callableProcessor.generateJvm().forEach {
                        it.writeTo(codeGenerator, Dependencies(false))
                    }
                } else if (!isCommon) {
                    callableProcessor.generateNative().forEach {
                        it.writeTo(codeGenerator, Dependencies(false))
                    }
                    val callbacks = callbackProcessor.generateNative()
                    callbacks.forEach {
                        it.fileSpec.writeTo(codeGenerator, Dependencies(false))
                    }
                } else {
                    // common
                    flowProcessor.generateCommon().forEach {
                        it.writeTo(codeGenerator, Dependencies(false))
                    }
                }
                generated = true
            }
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