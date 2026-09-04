package com.dshatz.kni.model

import com.dshatz.kni.CNameUtils
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.Types
import com.dshatz.kni.model.flow.KSFlowProp
import com.dshatz.kni.needsIsNullParam
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
    val modifiers: Set<KModifier> = emptySet(),
    val baseClass: ClassName? = null // in case it is not expect/actual, this is the class available in all targets.
) {
    val typeInfo = TypeInfo.NativeInstance(className, baseClass)

    fun generateNative(): FileSpec {
        val funSpecs = funs.map(KSJniCall::generateCnameFunction)
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

    private fun generateNativeConstructors(
    ): List<FunSpec> {
        val returnTypeInfo = typeInfo
        return constructors.map { constructor ->
            val jniCName = CNameUtils.jniFunctionCName(
                packageName = className.packageName,
                className = className.simpleName,
                functionName = "initNative${constructor.id}"
            )
            val paramSpecs = constructor.params.map(ParamInfo::paramSpecNative)
            val paramConversion = constructor.params.map {
                it.refNative.unpackCode()
            }.joinToCode { it.code }

            val initNativeCode = CodeBlock.builder()
                .addStatement("%T(%L)", returnTypeInfo.kotlinType, paramConversion)
                .build()
                .commonCode(returnTypeInfo)

            val returnCode = CodeBlock.builder()
                .addStatement("return %L", initNativeCode.packToNative().code)
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
            ParameterSpec("instance", instanceType.jniType.nativeType)
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
                .addAnnotation(Types.Annotations.Optin.AtomicsOptIn)
                .addModifiers(KModifier.ACTUAL, cl.constructors.first().modifier)
                .addCode(CodeBlock.builder().addStatement("nativeInstance.store(initNative%L(%L))", constructor.id, convertParamsCode).build())
                .build()
        }
    }

    private fun generateJvmExternalConstructors(cl: KSInstance): List<FunSpec> {
        return cl.constructors.map { constructor ->
            val params = constructor.params.map {
                ParameterSpec.builder(it.name, it.typeInfo.jniType.jvmType).build()
            }
            FunSpec.builder("initNative${constructor.id}")
                .addParameters(params)
                .returns(cl.typeInfo.jniType.jvmType)
                .addModifiers(KModifier.EXTERNAL, KModifier.PRIVATE)
                .build()
        }
    }

    private fun generateJvmExternalDispose(cl: KSInstance): FunSpec {
        return FunSpec.builder("disposeNative")
            .addParameter(ParameterSpec("instance", cl.typeInfo.jniType.jvmType))
            .addModifiers(KModifier.EXTERNAL, KModifier.OVERRIDE)
            .build()
    }


    private fun generateJvmFunctions(
        functions: Iterable<KSJniCall>,
    ): List<FunSpec> {
        return functions.flatMap { f ->
            f.generateJvmFunctions(this)
        }
    }


    internal fun generateJvmInstance(instance: KSInstance, fileSpecs: MutableMap<ClassName, FileSpec.Builder>) {
        val funs = instance.funs
        funs.groupBy { it.parent }.forEach { (parent, functions) ->
            val constructors = instance.let(::generateJvmActualConstructors)
            val constructorFromPointer = FunSpec.constructorBuilder()
                .addAnnotation(Types.Annotations.Optin.AtomicsOptIn)
                .addParameter(ParameterSpec("nativeInstancePtr", Types.KLong))
                .callSuperConstructor()
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
                .addSuperinterfaces(instance.superInterfaces)
                .superclass(Types.NativeInstanceJvm)
                .apply {
                    instance.baseClass?.let(::addSuperinterface)
                }
                .addModifiers(instance.modifiers)
                .addFunction(constructorFromPointer)
                .addFunctions(constructors)
                .addFunction(externalDispose)
                .addFunctions(flowProps.map(KSFlowProp::generateGetValueFun))
                .addProperties(flowProps.map(KSFlowProp::generateFlowProp))
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
}