package com.dshatz.kni.jniCall

import com.dshatz.kni.BaseProcessor
import com.dshatz.kni.Registry
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.TypeMapper
import com.dshatz.kni.Types
import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.model.KSCallback
import com.dshatz.kni.model.KSCallbackFun
import com.dshatz.kni.model.KSInstance
import com.dshatz.kni.model.KSJniCall
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.model.flow.KSFlowProp
import com.dshatz.kni.needsIsNullParam
import com.dshatz.kni.utils.joinToCode
import com.dshatz.kni.utils.nonNullOrPlaceholder
import com.dshatz.kni.utils.originatesFrom
import com.dshatz.kni.utils.returnType
import com.dshatz.kni.utils.withSuffix
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class JniCallProcessor(
    override val registry: Registry,
    override val logger: KSPLogger,
    override val mapper: TypeMapper
): BaseProcessor() {
    @OptIn(KspExperimental::class)
    private fun getAnnotatedJniCalls(resolver: Resolver): List<KSFunctionDeclaration> {
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
                        logger.error("@JniCall is only supported inside classes/objects or on top-level functions.", it)
                        false
                    } else if (parentClass.classKind == ClassKind.CLASS && parentClass.getAllSuperTypes().none { it.toTypeName() == Types.AutoCloseable }) {
                        logger.error("Classes with @JniCall methods must implement ${Types.AutoCloseable.canonicalName}", it)
                        false
                    } else true
                }
            }
            .filter { it.isExpect } // only take from common. common should act as the source of truth.
            .distinctBy { it.qualifiedName?.asString() }

    }

    /**
     * Find all **classes** with at least one [JniCall] function.
     * Saves found classNames to registry so [TypeMapper] knows that these should be treated accordingly.
     */
    fun collectNativeInstances(
        resolver: Resolver
    ) {
        val funDecls = getAnnotatedJniCalls(resolver)
            .map { it.parentDeclaration }
            .filterIsInstance<KSClassDeclaration>()
        registry.nativeInstances.addAll(funDecls.map { it.toClassName() })
    }

    fun collectJniCalls(
        resolver: Resolver
    ) {
        val funs = getAnnotatedJniCalls(resolver)
        val jniCalls = funs.groupBy {
            it.functionLocation()
        }.flatMap { (parent, funs) ->
            funs.map { f ->
                val returnType = mapper.mapType(f.returnType!!)
                val params = f.parameters.toTypeInfos()
                val name = f.simpleName.asString()
                if (Modifier.SUSPEND in f.modifiers) {
                    KSJniCall.Suspend(
                        name = name,
                        returnType = returnType,
                        parameters = params,
                        declaration = f,
                        parent = parent
                    )
                } else {
                    KSJniCall.Blocking(
                        name = f.simpleName.asString(),
                        returnType = mapper.mapType(f.returnType!!),
                        parameters = f.parameters.toTypeInfos(),
                        parent = parent,
                        declaration = f,
                    )
                }
            }
        }
        registry.jniCalls.addAll(jniCalls)
        registerSuspendAdapters()
    }

    private fun registerSuspendAdapters() {
        registry.jniCalls.filterIsInstance<KSJniCall.Suspend>().map { f ->
            val callbackCls = f.suspendCallbackClass
            val callback = KSCallback(
                type = callbackCls,
                funs = listOf(
                    KSCallbackFun(
                        f.onValueFun,
                        returnType = TypeInfo.Unit,
                        parameters = if (f.returnType != TypeInfo.Unit) listOf(
                            ParamInfo("value", f.returnType)
                        ) else emptyList(),
                        parent = f.parent // ?
                    ),
                    KSCallbackFun(
                        f.onFailureFun,
                        returnType = TypeInfo.Unit,
                        parameters = listOf(
                            ParamInfo("message", TypeInfo.STRING),
                            ParamInfo("stackTrace", TypeInfo.STRING)
                        ),
                        parent = f.parent // ?
                    )
                ),
                dependency = f.declaration.containingFile!!,
                superType = if (f.returnType == TypeInfo.Unit)
                    Types.SuspendCallback0
                else Types.SuspendCallback.parameterizedBy(f.returnType.kotlinType)
            )
            registry.callbacks[callbackCls] = callback
            registry.jniCallSuspendAdapters.add(callback)
        }
    }

    fun generateNative(): List<FileSpec> {
        return registry.jniCalls.groupBy { it.parent }.map { (parent, functions) ->
            if (parent is FunctionParent.Class) {
                val instance = KSInstance(
                    parent.className,
                    parent.constructors,
                    functions,
                    registry.flowFields[parent].orEmpty()
                )
                instance.generateNative()
            } else {
                val fileClass = parent.classNameKt.withSuffix("_jniCalls")
                FileSpec.builder(fileClass)
                    .addFunctions(functions.map(KSJniCall::generateCnameFunction))
                    .build()
            }
        }
    }



    private fun generateJvmActualConstructors(cl: FunctionParent.Class): List<FunSpec> {
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
                .addModifiers(KModifier.ACTUAL, cl.declaration.primaryConstructor?.modifiers?.visibilityKModifier ?: KModifier.PUBLIC)
                .addCode(CodeBlock.builder().addStatement("nativeInstance.store(initNative%L(%L))", constructor.id, convertParamsCode).build())
                .build()
        }
    }

    private fun generateJvmExternalConstructors(cl: FunctionParent.Class): List<FunSpec> = context(cl.declaration) {
        cl.constructors.map { constructor ->
            val params = constructor.params.map {
                ParameterSpec.builder(it.name, it.typeInfo.jniType.jvmType).build()
            }
            val returnType = mapper.mapType(cl.className)

            FunSpec.builder("initNative${constructor.id}")
                .addParameters(params)
                .returns(returnType.jniType.jvmType)
                .addModifiers(KModifier.EXTERNAL, KModifier.PRIVATE)
                .build()
        }
    }

    private fun generateJvmExternalDispose(cl: FunctionParent.Class): FunSpec = context(cl.declaration) {
        val type = mapper.mapType(cl.className)
        return FunSpec.builder("disposeNative")
            .addParameter(ParameterSpec("instance", type.jniType.jvmType))
            .addModifiers(KModifier.EXTERNAL, KModifier.OVERRIDE)
            .build()
    }

    private fun generateJvmFunctions(
        functions: List<KSJniCall>,
        instanceParameter: TypeInfo? = null
    ): List<FunSpec> {
        val funNames = functions.associateWith { f ->
            MemberName(f.parent.className, f.name)
        }

        return functions.flatMap { f ->
            val funSpec = FunSpec.builder(funNames[f]!!)
                .addModifiers(KModifier.ACTUAL, f.declaration.modifiers.visibilityKModifier)
                .apply {
                    if (Modifier.OVERRIDE in f.declaration.modifiers) addModifiers(KModifier.OVERRIDE)
                    if (Modifier.SUSPEND in f.declaration.modifiers) addModifiers(KModifier.SUSPEND)
                }
                .returns(f.returnType.kotlinType)
                .apply {
                    f.parameters.forEach { (name, typeInfo) ->
                        addParameter(ParameterSpec.builder(
                            name = name,
                            type = typeInfo.kotlinType
                        ).build())
                    }
                }
                .originatesFrom(f.declaration)
                .apply {
                    val paramPacking = f.parameters.map { p ->
                        p.refCommon.packToJvm().typed.nonNullOrPlaceholder()
                    }
                    val isNullParams = f.parameters.filter { it.typeInfo.needsIsNullParam() }.map {
                        CodeBlock.of("%N == null", it.name).returnType(Types.KBoolean)
                    }
                    val params = if (instanceParameter == null) {
                        paramPacking + isNullParams
                    } else {
                        paramPacking + isNullParams + CodeBlock.of("it").returnType(instanceParameter.jniType.nativeType)
                    }
                    val paramsCode = params.joinToCode(prefix = "\n", separator = ",\n", suffix = "\n") { it.code }
                    val callExternalCode = CodeBlock.of("%L(%L)", f.callToExternal.simpleName, paramsCode).returnType(f.returnType.jniType.jvmType)
                    val returnValue = if (f is KSJniCall.Suspend) {
                        callExternalCode // No need to convert, callback already converted.
                    } else {
                        f.returnType.unpackCodeJvm(callExternalCode)
                    }

                    if (instanceParameter != null) {
                        val method = if (f is KSJniCall.Suspend) "withValidInstanceSuspend" else "withValidInstance"
                        val withValidInstanceBlock = CodeBlock.builder()
                            .beginControlFlow("return %L", method)
                            .add(returnValue.code)
                            .endControlFlow()
                            .build()
                        addCode(withValidInstanceBlock)
                    } else addCode("return %L", returnValue.code)

                }.build()

            val externalAsyncSpec = (f as? KSJniCall.Suspend)?.externalAsyncSpec()


            val externalSpec = FunSpec.builder(f.externalFun)
                .addModifiers(KModifier.EXTERNAL, KModifier.PRIVATE)
                .apply {
                    f.jniParams.forEach {
                        addKdoc("@param ${it.name} [${it.typeInfo.kotlinType}] converted to `${it.typeInfo.jniType.nativeType}`.\n")
                    }
                    if (f.jniReturn.kotlinType != UNIT) {
                        addKdoc("@return Representing `${f.returnType.describe()}`. Converted from `${f.returnType.jniType.nativeType}` to `${f.returnType.jniType.jvmType}`.\n")
                    }
                }
                .returns(f.jniReturn.jniType.jvmType)
                .addParameters(
                    f.jniParams.map { (name, typeInfo) ->
                        ParameterSpec.builder(
                            name = name,
                            type = typeInfo.jniType.jvmType,
                        ).build()
                    }
                )
                .addParameters(f.parameters.filter {
                    it.typeInfo.needsIsNullParam()
                }.map {
                    ParameterSpec("_${it.name}IsNull", Types.KBoolean)
                })
                .build()
            listOfNotNull(funSpec, externalSpec, externalAsyncSpec)
        }
    }


    fun generateJvm(): List<FileSpec> {
        return generateJvmActuals(registry.jniCalls)
    }

    private fun generateJvmActuals(funs: Collection<KSJniCall>): List<FileSpec> {
        val fileSpecs = mutableMapOf<ClassName, FileSpec.Builder>()
        funs.groupBy { it.parent }.forEach { (parent, functions) ->
            val instance = parent as? FunctionParent.Class
            val constructors = instance?.let(::generateJvmActualConstructors)
            val constructorFromPointer = FunSpec.constructorBuilder()
                .addAnnotation(Types.Annotations.Optin.AtomicsOptIn)
                .addParameter(ParameterSpec("nativeInstancePtr", Types.KLong))
                .callSuperConstructor()
                .addCode(CodeBlock.of("nativeInstance.store(nativeInstancePtr)"))
                .build()
            val externalConstructors = instance?.let(::generateJvmExternalConstructors)
            val externalDispose = instance?.let(::generateJvmExternalDispose)
            val nativeInstanceType = instance?.let {
                context(it.declaration) {
                    mapper.mapType(it.className)
                }
            }

            val fileSpec = fileSpecs.getOrPut(parent.className) { FileSpec.builder(parent.className) }
            val specs = generateJvmFunctions(functions, nativeInstanceType)
            when {
                parent is FunctionParent.Class -> {
                    val flowProps = registry.flowFields[parent].orEmpty()
                    val factory = FunSpec.builder("as${parent.className.simpleName}")
                        .returns(parent.className)
                        .receiver(Types.KLong)
                        .addCode("return %T(this)", parent.className)
                        .build()
                    val factoryFileClass = parent.className.withSuffix("_converter")
                    val factoryFileSpec = FileSpec.builder(factoryFileClass)
                        .addFunction(factory)
                    fileSpecs[factoryFileClass] = factoryFileSpec

                    val type = TypeSpec.classBuilder(parent.className)
                        .addModifiers(KModifier.ACTUAL, parent.declaration.modifiers.visibilityKModifier)
                        .addSuperinterfaces(parent.superTypes)
                        .superclass(Types.NativeInstanceJvm)
                        .addFunction(constructorFromPointer)
                        .apply { constructors?.let(::addFunctions) }
                        .apply {
                            externalDispose?.let(::addFunction)
                        }
                        .addFunctions(flowProps.map(KSFlowProp::generateGetValueFun))
                        .addProperties(flowProps.map(KSFlowProp::generateFlowProp))
                        .addTypes(flowProps.map(KSFlowProp::generateFlowCallbackJvm))
                        .addFunctions(externalConstructors.orEmpty())
                        .addFunctions(specs)
                        .build()
                    fileSpec.addType(type)

                }
                parent is FunctionParent.Object -> {
                    val obj = TypeSpec.objectBuilder(parent.className)
                        .addModifiers(KModifier.ACTUAL, parent.declaration.modifiers.visibilityKModifier)
                        .addFunctions(specs)
                        .build()
                    fileSpec.addType(obj)
                }
                parent is FunctionParent.TopLevel -> {
                    fileSpec.addFunctions(specs)
                }
            }
        }
        return fileSpecs.values.map { it.build() }
    }

}

fun List<CodeBlock>.joinToString(separator: String = ", "): CodeBlock {
    return CodeBlock.of(
        generateSequence { "%L" }.take(size).joinToString(separator),
        args = this.toTypedArray()
    )
}


val Set<Modifier>.visibilityKModifier: KModifier
    get() = if (this.contains(Modifier.PRIVATE))
        KModifier.PRIVATE
    else if (this.contains(Modifier.PROTECTED))
        KModifier.PROTECTED
    else if (this.contains(Modifier.INTERNAL))
        KModifier.INTERNAL
    else KModifier.PUBLIC