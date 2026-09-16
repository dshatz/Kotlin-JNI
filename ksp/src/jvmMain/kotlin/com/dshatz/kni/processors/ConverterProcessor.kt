package com.dshatz.kni.processors

import com.dshatz.kni.BaseProcessor
import com.dshatz.kni.Registry
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.TypeMapper
import com.dshatz.kni.Types
import com.dshatz.kni.annotations.TypeMarker
import com.dshatz.kni.model.KSCallbackFun
import com.dshatz.kni.model.KSJniCall
import com.dshatz.kni.utils.PlatformContext
import com.dshatz.kni.utils.ProcessorContext
import com.dshatz.kni.utils.ResolverContext
import com.dshatz.kni.utils.decapitalized
import com.dshatz.kni.utils.returnType
import com.dshatz.kni.utils.safeQualifiedName
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.sun.tools.javac.tree.TreeInfo.types

class ConverterProcessor(
    override val mapper: TypeMapper,
    override val logger: KSPLogger,
    override val registry: Registry
) : BaseProcessor() {

    /**
     * Given a generic receiver type with varied type arguments, moves those type arguments
     * to function reified type parameters and makes the function inline.
     *
     * In other cases, just applies the receiver as usual.
     */
    private fun FunSpec.Builder.buildGenericFunction(receiver: TypeName): FunSpec.Builder {
        val needsReified = receiver is ParameterizedTypeName && receiver.typeArguments.any { it is WildcardTypeName }
        return if (needsReified) {
            // for example, receiver = Array<out Callback>
            val typeParams = receiver.typeArguments // e.g. 'out Callback'
            val typeParamsWithBounds = typeParams.map {
                if (it is WildcardTypeName) {
                    it.outTypes // 'Callback' - variance removed.
                } else {
                    listOf(it)
                }
            }
            val typeVariables = typeParamsWithBounds.mapIndexed { index, bounds ->
                TypeVariableName("T$index", bounds).copy(reified = true)
                // reified T0: Callback
            }
            val receiverWithT: ParameterizedTypeName = receiver.copy(
                typeArguments = typeVariables
            ) // Array<T0>
            receiver(
                receiverWithT
            ).addTypeVariables(typeVariables)
                .addModifiers(KModifier.INLINE)
        } else {
            receiver(receiver)
        }
    }

    private fun KSJniCall.collectTypes(): List<TypeInfo> {
        return parameters.map { it.typeInfo.notNullable() } + returnType.notNullable()
    }

    private fun KSCallbackFun.collectTypes(): List<TypeInfo> {
        return parameters.map { it.typeInfo.notNullable() } + returnType.notNullable()
    }

    context(ctx: ProcessorContext)
    fun generateConverters(): Collection<FileSpec> {
        val types =
            registry.allTypes +
                    registry.nativeInstances.values.map { it.typeInfo } +
                    registry.callbackSuspendAdapters.map {
                        it.typeInfo
                    } +
                    registry.jniCallSuspendAdapters.map {
                        it.typeInfo
                    } +
                    registry.nativeInstances.values.flatMap {
                        it.flowProps.map { it.callbackType }
                    } +
                    registry.jniAdapterTypes.values.map {
                        it.type
                    } + TypeInfo.PlatformString
        return types.groupBy { it.converterFile() }.mapValues { (fileCls, types) ->
            val file = FileSpec.builder(fileCls)

            val thiss = CodeBlock.of("this")

            types.forEach { type ->
                if (type is TypeInfo.Convertible) {
                    type.aliasedImports.forEach { (cls, name) ->
                        file.addAliasedImport(cls, name)
                    }
                }
               when (ctx.platform) {
                   Registry.Platform.COMMON -> {}
                   Registry.Platform.NATIVE -> {
                        val fromJni = FunSpec.builder("fromJni")
                            .receiver(type.jniType.jniType)
                            .returns(type.commonKotlinType)
                            .addParameter("env", Types.Environment)
                            .addAnnotation(Types.Annotations.Optin.NativeOptIn)
                            .addCode("return %L", type.unpackCode(thiss.returnType(type.jniType.jniType)).code)
                            .addKdoc("$type")
                            .build()
                       file.addFunction(fromJni)

                       val toJni = FunSpec.builder("toJni")
                           .buildGenericFunction(type.commonKotlinType)
                           .returns(type.jniType.jniType)
                           .addParameter("env", Types.Environment)
                           .addAnnotation(Types.Annotations.Optin.NativeOptIn)
                           .addCode("return %L", type.packCode(thiss.returnType(type.kotlinType)).code)
                           .addKdoc("$type")
                           .build()
                       file.addFunction(toJni)
                   }
                   Registry.Platform.JVM -> {
                       val toJni = FunSpec.builder("toJni")
                           .receiver(type.commonKotlinType)
                           .returns(type.jniType.jniType)
                           .addCode("return %L", type.packCodeJvm(thiss.returnType(type.kotlinType)).code)
                           .addKdoc("$type")
                           .build()
                       file.addFunction(toJni)

                       val fromJni = FunSpec.builder("fromJni")
                           .receiver(type.jniType.jniType)
                           .returns(type.commonKotlinType)
                           .addCode("return %L", type.unpackCodeJvm(thiss.returnType(type.jniType.jniType)).code)
                           .addKdoc("$type")
                           .build()
                       file.addFunction(fromJni)
                   }
               }
            }
            file.build()
        }.values
    }
}

context(resolverContext: ResolverContext)
fun TypeInfo.converterPackage(): String {
    return commonKotlinType.converterPackage()
}

context(_: ResolverContext)
private fun TypeInfo.converterFile(): ClassName {
    val name = "converters"
    return ClassName(converterPackage(), name)
}


context(ctx: ResolverContext)
fun TypeName.converterPackage(): String {
    val moduleName = ctx.moduleName
    return "kni.${moduleName}.generated.converters." + safeQualifiedName().split('.').joinToString(".") {
        it.decapitalized()
    }
}

context(resolverContext: ResolverContext)
fun TypeInfo.packMember(): TypedMember {
    val member = MemberName(converterPackage(), "toJni", isExtension = true)
    return TypedMember(
        memberName = member,
        params = CodeBlock.of("env"),
        type = jniType.jniType
    )
}

context(resolverContext: ResolverContext)
fun TypeInfo.unpackMember(): TypedMember {
    val member = MemberName(converterPackage(), "fromJni", isExtension = true)
    return TypedMember(
        memberName = member,
        params = CodeBlock.of("env"),
        type = kotlinType
    )
}

context(resolverContext: ResolverContext)
fun TypeInfo.packMemberJvm(): TypedMember {
    val member = MemberName(converterPackage(), "toJni", isExtension = true)
    return TypedMember(
        memberName = member,
        params = CodeBlock.of(""),
        type = jniType.jniType
    )
}

context(resolverContext: ResolverContext)
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