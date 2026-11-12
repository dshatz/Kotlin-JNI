package com.dshatz.kni.model

import com.dshatz.kni.CNameUtils
import com.dshatz.kni.Registry
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.Types
import com.dshatz.kni.model.flow.KSFlowProp
import com.dshatz.kni.needsIsNullParam
import com.dshatz.kni.utils.JvmContext
import com.dshatz.kni.utils.NativeContext
import com.dshatz.kni.utils.PlatformContext
import com.dshatz.kni.utils.ResolverContext
import com.dshatz.kni.utils.cnameFunBuilder
import com.dshatz.kni.utils.commonCode
import com.dshatz.kni.utils.nativeCode
import com.dshatz.kni.utils.nonNullOrPlaceholder
import com.dshatz.kni.utils.returnType
import com.dshatz.kni.utils.withSuffix
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.joinToCode
import kotlin.collections.orEmpty

data class KSInstance(
    val className: ClassName,
    val constructors: List<KSConstructor>,
    val funs: List<KSJniCall>,
    val flowProps: List<KSFlowProp>,
    val superInterfaces: Set<TypeName> = emptySet(),
    val superClass: TypeName? = null, // user-declared superclass, must have a no-arg constructor.
    val modifiers: Set<KModifier> = emptySet(),
    val baseClass: ClassName? = null, // in case it is not expect/actual, this is the class available in all targets.
    private val platform: Registry.Platform
) {
    val typeInfo = context(PlatformContext(platform)) { TypeInfo.nativeInstance(className, baseClass) }

    context(_: NativeContext, _: ResolverContext)
    fun generateNative(): FileSpec {
        val funSpecs = funs.map { it.generateCnameFunction() }
        val constructors = generateNativeConstructors()
        val dispose = generateNativeDispose()

        val flowGetValueFuncs = flowProps.map { flowProp ->
            flowProp.generateNativeFlowInit()
        }

        val fileClassName = className.withSuffix("_jniCalls")
        return FileSpec.builder(fileClassName)
            .addFunctions(funSpecs)
            .addFunctions(flowGetValueFuncs)
            .addFunctions(constructors)
            .addFunction(dispose)
            .build()
    }

    context(_: NativeContext, _: ResolverContext)
    private fun generateNativeConstructors(
    ): List<FunSpec> {
        val returnTypeInfo = typeInfo
        return constructors.map { constructor ->
            val jniCName = CNameUtils.jniFunctionCName(
                packageName = className.packageName,
                className = className.simpleName,
                functionName = "initNative${constructor.id}"
            )
            val paramSpecs = constructor.params.map { it.paramSpecNative() }
            // Buffers are only borrowed for the duration of the constructor, unless user code calls ByteBuffer.retain().
            val borrowedBuffers = constructor.params.filter { it.typeInfo is TypeInfo.ByteBuffer }
            val paramConversion = constructor.params.map {
                if (it in borrowedBuffers) CodeBlock.of("%N", it.name) else it.refNative.unpackCode().code
            }.joinToCode()

            val initNativeCode = CodeBlock.of("%T(%L)", returnTypeInfo.kotlinType, paramConversion)
                .commonCode(returnTypeInfo)

            val returnCode = CodeBlock.builder()
                .apply {
                    borrowedBuffers.forEach {
                        addStatement(
                            "val %N = %N%L%M(env)",
                            it.name,
                            it.name,
                            if (it.typeInfo.kotlinType.isNullable) "?." else ".",
                            Types.Method.ToKDirectByteBufferBorrowed
                        )
                    }
                    if (borrowedBuffers.isEmpty()) {
                        addStatement("return %L", initNativeCode.packToNative().code)
                    } else {
                        beginControlFlow(
                            "return %M(%L)",
                            Types.Method.WithBorrowedBuffers,
                            borrowedBuffers.joinToCode(", ") { CodeBlock.of("%N", it.name) }
                        )
                        addStatement("%L", initNativeCode.packToNative().code)
                        endControlFlow()
                    }
                }
                .build()

            cnameFunBuilder(
                MemberName(className, "init${constructor.id}"),
                jniCName
            )
                .returns(LONG)
                .addParameters(paramSpecs)
                .addCode(returnCode)
                .build()
        }
    }

    context(_: NativeContext, _: ResolverContext)
    private fun generateNativeDispose(): FunSpec {
        val jniCname = CNameUtils.jniFunctionCName(
            packageName = className.packageName,
            className = className.simpleName,
            functionName = "disposeNative"
        )
        val instanceType = typeInfo
        val unpack = CodeBlock.of("%N", "instance").nativeCode(instanceType).unpackCode()
        return cnameFunBuilder(
            MemberName(className, "disposeNative"),
            jniCname
        ).addParameter(
            ParameterSpec("instance", instanceType.jniType.jniType)
        )
            .addCode(
                CodeBlock.builder()
                    .addStatement("%L.close()", unpack.code)
                    .addStatement("%N.%M<%T>()", "instance", Types.Method.ReleaseStableRef, baseClass ?: className)
                    .build()
            )
            .build()
    }

    // JVM


    context(_: JvmContext, _: ResolverContext)
    private fun generateJvmActualConstructors(cl: KSInstance): List<FunSpec> {
        return cl.constructors.map { constructor ->
            val params = constructor.params.map {
                ParameterSpec.builder(it.name, it.typeInfo.kotlinType).build()
            }
            val convertParamsCode = constructor.params.map {
                it.refCommon.packToJvm().code
            }.joinToCode()
            FunSpec.constructorBuilder()
                .addParameters(params)
                .callThisConstructor(newNativeInstanceImpl)
                .addAnnotation(Types.Annotations.Optin.AtomicsOptIn)
                .addModifiers(KModifier.ACTUAL, cl.constructors.first().modifier)
                .addCode(CodeBlock.builder().addStatement("nativeInstance.store(initNative%L(%L))", constructor.id, convertParamsCode).build())
                .build()
        }
    }

    private fun generateJvmExternalConstructors(cl: KSInstance): List<FunSpec> {
        return cl.constructors.map { constructor ->
            val params = constructor.params.map {
                ParameterSpec.builder(it.name, it.typeInfo.jniType.jniType).build()
            }
            FunSpec.builder("initNative${constructor.id}")
                .addParameters(params)
                .returns(cl.typeInfo.jniType.jniType)
                .addModifiers(KModifier.EXTERNAL, KModifier.PRIVATE)
                .build()
        }
    }

    private val newNativeInstanceImpl: CodeBlock
        get() = CodeBlock.of("%T(Companion::disposeNative)", Types.NativeInstanceJvmImpl)

    private fun generateJvmExternalDispose(cl: KSInstance): FunSpec {
        return FunSpec.builder("disposeNative")
            .addParameter(ParameterSpec("instance", cl.typeInfo.jniType.jniType))
            .addAnnotation(JvmStatic::class)
            .addModifiers(KModifier.EXTERNAL, KModifier.PRIVATE)
            .build()
    }


    context(_: JvmContext, _: ResolverContext)
    private fun generateJvmFunctions(
        functions: Iterable<KSJniCall>,
    ): List<FunSpec> {
        return functions.flatMap { f ->
            f.generateJvmFunctions(this)
        }
    }


    context(_: JvmContext, _: ResolverContext)
    internal fun generateJvmInstance(instance: KSInstance, fileSpecs: MutableMap<ClassName, FileSpec.Builder>) {
        val funs = instance.funs
        funs.groupBy { it.parent }.forEach { (parent, functions) ->
            val constructors = generateJvmActualConstructors(instance)
            val constructorFromPointer = FunSpec.constructorBuilder()
                .addAnnotation(Types.Annotations.Optin.AtomicsOptIn)
                .addParameter(ParameterSpec("nativeInstancePtr", Types.KLong))
                .callThisConstructor(newNativeInstanceImpl)
                .addCode(CodeBlock.of("nativeInstance.store(nativeInstancePtr)"))
                .build()
            val externalConstructors = instance.let(::generateJvmExternalConstructors)
            val externalDispose = instance.let(::generateJvmExternalDispose)

            val specs = generateJvmFunctions(functions)

            val flowProps = instance.flowProps
            val factory = FunSpec.builder("as${instance.className.simpleName}")
                .returns(instance.className)
                .receiver(Types.KLong)
                .addCode("return %T(this)", instance.className)
                .build()
            val factoryFileClass = parent.className.withSuffix("_converter")
            val factoryFileSpec = FileSpec.builder(factoryFileClass)
                .addFunction(factory)

            fileSpecs[factoryFileClass] = factoryFileSpec

            val fileSpec = fileSpecs.getOrPut(instance.className) { FileSpec.builder(instance.className) }

            val type = TypeSpec.classBuilder(instance.className)
                .apply {
                    instance.superClass?.let(::superclass)
                }
                .addSuperinterfaces(instance.superInterfaces)
                // Delegation requires a primary constructor; all other constructors delegate to this one.
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter(NATIVE_INSTANCE_IMPL_PARAM, Types.NativeInstanceJvmImpl)
                        .build()
                )
                .addSuperinterface(Types.NativeInstanceJvm, CodeBlock.of("%N", NATIVE_INSTANCE_IMPL_PARAM))
                .apply {
                    instance.baseClass?.let(::addSuperinterface)
                }
                .addModifiers(instance.modifiers)
                .addFunction(constructorFromPointer)
                .addFunctions(constructors)
                .addType(TypeSpec.companionObjectBuilder().addFunction(externalDispose).build())
                .addFunctions(flowProps.map { it.generateGetValueFun() })
                .addProperties(flowProps.map { it.generateFlowProp() })
                .addTypes(flowProps.map(KSFlowProp::generateFlowCallbackJvm))
                .addFunctions(externalConstructors.orEmpty())
                .addFunctions(specs)
                .build()
            fileSpec
                .addType(type)
                .addFunction(generateJvmAsLong())
        }
    }




    private fun generateJvmAsLong(): FunSpec {
        return FunSpec.builder("asLong")
            .returns(LONG)
            .receiver(typeInfo.commonKotlinType)
            .addCode("return (this as %T).asLong()", Types.NativeInstanceJvm)
            .build()
    }

    private companion object {
        const val NATIVE_INSTANCE_IMPL_PARAM = "nativeInstanceImpl"
    }
}
