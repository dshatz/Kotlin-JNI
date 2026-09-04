package com.dshatz.kni.processors

import com.dshatz.kni.BaseProcessor
import com.dshatz.kni.Registry
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.TypeMapper
import com.dshatz.kni.Types
import com.dshatz.kni.utils.decapitalized
import com.dshatz.kni.utils.returnType
import com.dshatz.kni.utils.safeQualifiedName
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName

class ConverterProcessor(
    override val mapper: TypeMapper,
    override val logger: KSPLogger,
    override val registry: Registry
) : BaseProcessor() {

    private fun FunSpec.Builder.buildGenericFunction(receiver: TypeName): FunSpec.Builder {
        val needsReified = receiver is ParameterizedTypeName && receiver.typeArguments.any { it is WildcardTypeName }
        return if (needsReified) {
            val typeParams = receiver.typeArguments
            val typeParamsWithBounds = typeParams.map {
                if (it is WildcardTypeName) {
                    it.outTypes
                } else {
                    listOf(it)
                }
            }
            val typeVariables = typeParamsWithBounds.mapIndexed { index, bounds ->
                TypeVariableName("T$index", bounds).copy(reified = true)
            }
            val receiverWithT: ParameterizedTypeName = receiver.copy(
                typeArguments = typeVariables
            )
            receiver(
                receiverWithT
            ).addTypeVariables(typeVariables)
                .addModifiers(KModifier.INLINE)
        } else {
            receiver(receiver)
        }
    }

    fun generateConverters(
        platform: Registry.Platform
    ): Collection<FileSpec> {
        val types =
            registry.allTypes +
                    registry.nativeInstances.values.map { it.typeInfo } +
                    registry.callbackSuspendAdapters.map {
                        logger.info("Adding suspend adapter ${it.typeInfo}")
                        it.typeInfo
                    } +
                    registry.jniCallSuspendAdapters.map {
                        logger.info("Adding suspend adapter ${it.typeInfo}")
                        it.typeInfo
                    } +
                    registry.nativeInstances.values.flatMap {
                        it.flowProps.map { it.callbackType }
                    }
        return types.groupBy { it.converterFile() }.mapValues { (fileCls, types) ->
            val file = FileSpec.builder(fileCls)

            val thiss = CodeBlock.of("this")
            types.forEach { type ->
               when (platform) {
                   Registry.Platform.COMMON -> {}
                   Registry.Platform.NATIVE -> {
                        val fromJni = FunSpec.builder("fromJni")
                            .receiver(type.jniType.nativeType)
                            .returns(type.commonKotlinType)
                            .addParameter("env", Types.Environment)
                            .addAnnotation(Types.Annotations.Optin.NativeOptIn)
                            .addCode("return %L", type.unpackCode(thiss.returnType(type.jniType.nativeType)).code)
                            .build()
                       file.addFunction(fromJni)

                       val toJni = FunSpec.builder("toJni")
                           .buildGenericFunction(type.commonKotlinType)
                           .returns(type.jniType.nativeType)
                           .addParameter("env", Types.Environment)
                           .addAnnotation(Types.Annotations.Optin.NativeOptIn)
                           .addCode("return %L", type.packCode(thiss.returnType(type.kotlinType)).code)
                           .build()
                       file.addFunction(toJni)
                   }
                   Registry.Platform.JVM -> {
                       val toJni = FunSpec.builder("toJni")
                           .receiver(type.commonKotlinType)
                           .returns(type.jniType.jvmType)
                           .addCode("return %L", type.packCodeJvm(thiss.returnType(type.kotlinType)).code)
                           .build()
                       file.addFunction(toJni)

                       val fromJni = FunSpec.builder("fromJni")
                           .receiver(type.jniType.jvmType)
                           .returns(type.commonKotlinType)
                           .addCode("return %L", type.unpackCodeJvm(thiss.returnType(type.jniType.jvmType)).code)
                           .build()
                       file.addFunction(fromJni)
                   }
               }
            }
            file.build()
        }.values
    }
}

private fun TypeInfo.converterPackage(): String {
    return "kni.generated.converters." + commonKotlinType.safeQualifiedName().split('.').joinToString(".") {
        it.decapitalized()
    }
}

private fun TypeInfo.converterFile(): ClassName {
    val name = "converters"
    return ClassName(converterPackage(), name)
}

fun TypeInfo.packMember(): TypedMember {
    val member = MemberName(converterPackage(), "toJni", isExtension = true)
    return TypedMember(
        memberName = member,
        params = CodeBlock.of("env"),
        type = jniType.nativeType
    )
}

fun TypeInfo.unpackMember(): TypedMember {
    val member = MemberName(converterPackage(), "fromJni", isExtension = true)
    return TypedMember(
        memberName = member,
        params = CodeBlock.of("env"),
        type = kotlinType
    )
}

fun TypeInfo.packMemberJvm(): TypedMember {
    val member = MemberName(converterPackage(), "toJni", isExtension = true)
    return TypedMember(
        memberName = member,
        params = CodeBlock.of(""),
        type = jniType.jvmType
    )
}

fun TypeInfo.unpackMemberJvm(): TypedMember {
    val member = MemberName(converterPackage(), "fromJni", isExtension = true)
    return TypedMember(
        memberName = member,
        params = CodeBlock.of(""),
        type = kotlinType
    )
}

data class TypedMember(
    val memberName: MemberName,
    val params: CodeBlock,
    val type: TypeName
) {

}