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
import com.dshatz.kni.model.KSConstructor
import com.dshatz.kni.model.KSInstance
import com.dshatz.kni.model.KSJniCall
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.model.PropInfo
import com.dshatz.kni.model.flow.KSFlowProp
import com.dshatz.kni.needsIsNullParam
import com.dshatz.kni.utils.nonNullOrPlaceholder
import com.dshatz.kni.utils.returnType
import com.dshatz.kni.utils.withSuffix
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlin.collections.orEmpty

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
        val instances = getAnnotatedJniCalls(resolver)
            .filter {
                it.parentDeclaration is KSClassDeclaration
                        && (it.parentDeclaration as KSClassDeclaration).classKind == ClassKind.CLASS
            }
            .groupBy { it.parentDeclaration!! }
            .map { (parentDeclaration, funs) ->

                val parent = (parentDeclaration as KSClassDeclaration).innerFunLocation()

                val constructors = parentDeclaration.getConstructors()
                    .mapIndexed { idx, constructor ->
                        KSConstructor(
                            id = idx,
                            params = constructor.parameters.toTypeInfos(),
                            modifier = constructor.modifiers.visibilityKModifier
                        )
                    }.toList()

                val flowProps = parentDeclaration.declarations.filterIsInstance<KSPropertyDeclaration>()
                    .map {
                        PropInfo(
                            it.simpleName.asString(),
                            type = it.type.toTypeName(),
                            isMutable = it.isMutable,
                            declaration = it
                        )
                    }
                    .filter {
                        (it.type as? ParameterizedTypeName)?.rawType == Types.NativeBackedFlow
                    }
                    .filter {
                        if (it.isMutable) logger.error("com.dshatz.kni.flows.NativeBackedFlow<T> cannot be a mutable property")
                        !it.isMutable
                    }.map {
                        context(it.declaration) {
                            val typeArg = (it.type as ParameterizedTypeName).typeArguments.first()
                            KSFlowProp(
                                name = it.name,
                                innerType = mapper.mapType(typeArg),
                                instanceClass = parent.className
                            )
                        }
                    }
                    .toList()

                KSInstance(
                    className = parent.className,
                    constructors = constructors,
                    funs = funs.map { it.createJniCall(parent) },
                    flowProps = flowProps,
                    superInterfaces = parentDeclaration.superTypes.map { it.toTypeName() }.toSet(),
                    modifiers = setOfNotNull(parentDeclaration.modifiers.actualModifier)
                )
            }
        registry.nativeInstances.putAll(instances.associateBy { it.className })
    }

    fun KSFunctionDeclaration.createJniCall(parent: FunctionParent): KSJniCall {
        val returnType = mapper.mapType(returnType!!)
        val params = parameters.toTypeInfos()
        val name = simpleName.asString()
        val actualModifier = if (parent is FunctionParent.TopLevel) {
            modifiers.actualModifier
        } else parentDeclaration?.modifiers?.actualModifier
        return if (Modifier.SUSPEND in modifiers) {
            KSJniCall.Suspend(
                name = name,
                returnType = returnType,
                parameters = params,
                additionalModifiers = setOfNotNull(
                    modifiers.visibilityKModifier,
                    modifiers.overrideKModifier,
                    actualModifier
                ),
                parent = parent,
                nativeInstance = (parent as? FunctionParent.Class)?.className?.let(TypeInfo::NativeInstance)
            )
        } else {
            KSJniCall.Blocking(
                name = simpleName.asString(),
                returnType = returnType,
                parameters = parameters.toTypeInfos(),
                parent = parent,
                modifiers = setOfNotNull(
                    modifiers.visibilityKModifier,
                    modifiers.overrideKModifier,
                    actualModifier
                ),
                nativeInstance = (parent as? FunctionParent.Class)?.className?.let(TypeInfo::NativeInstance)
            )
        }
    }

    fun collectJniCalls(
        resolver: Resolver
    ) {
        val funs = getAnnotatedJniCalls(resolver)
        val jniCalls = funs.groupBy {
            it.functionLocation()
        }.filter { it.key !is FunctionParent.Class }.flatMap { (parent, funs) ->
            funs.map { f ->
                f.createJniCall(parent)
            }
        }
        registry.jniCalls.addAll(jniCalls)
        registerSuspendAdapters()
    }

    private fun registerSuspendAdapters() {
        val calls = registry.jniCalls

        val instanceCalls = registry.nativeInstances.values
            .flatMap { it.funs }
        (calls + instanceCalls).filterIsInstance<KSJniCall.Suspend>().map { f ->
            val callbackClass = f.suspendCallbackClass
            val callback = KSCallback(
                type = callbackClass,
                funs = listOf(
                    KSCallbackFun.Blocking(
                        f.onValueFun,
                        returnType = TypeInfo.Unit,
                        parameters = if (f.returnType != TypeInfo.Unit) listOf(
                            ParamInfo("value", f.returnType)
                        ) else emptyList(),
                        parent = f.parent,
                        callbackClass = callbackClass
                        // ?
                    ),
                    KSCallbackFun.Blocking(
                        f.onFailureFun,
                        returnType = TypeInfo.Unit,
                        parameters = listOf(
                            ParamInfo("message", TypeInfo.STRING),
                            ParamInfo("stackTrace", TypeInfo.STRING)
                        ),
                        parent = f.parent,
                        callbackClass = callbackClass
                        // ?
                    )
                ),
                superType = if (f.returnType == TypeInfo.Unit)
                    Types.SuspendCallback0
                else Types.SuspendCallback.parameterizedBy(f.returnType.kotlinType)
            )
            registry.callbacks[callbackClass] = callback
            registry.jniCallSuspendAdapters.add(callback)
        }
    }

    fun generateNative(): List<FileSpec> {
        val instances = registry.nativeInstances.values.map { it.generateNative() }
        return registry.jniCalls.groupBy { it.parent }.map { (parent, functions) ->
            val fileClass = parent.classNameKt.withSuffix("_jniCalls")
            FileSpec.builder(fileClass)
                .addFunctions(functions.map(KSJniCall::generateCnameFunction))
                .build()
        } + instances
    }

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
        instance: KSInstance? = null
    ): List<FunSpec> {
        val funNames = functions.associateWith { f ->
            MemberName(f.parent.className, f.name)
        }

        return functions.flatMap { f ->
            val funSpec = FunSpec.builder(funNames[f]!!)
                .addModifiers(f.modifiers)
                .returns(f.returnType.kotlinType)
                .apply {
                    f.parameters.forEach { (name, typeInfo) ->
                        addParameter(ParameterSpec.builder(
                            name = name,
                            type = typeInfo.kotlinType
                        ).build())
                    }
                }
                .apply {
                    val paramPacking = f.parameters.map { p ->
                        p.refCommon.packToJvm().typed.nonNullOrPlaceholder()
                    }
                    val isNullParams = f.parameters.filter { it.typeInfo.needsIsNullParam() }.map {
                        CodeBlock.of("%N == null", it.name).returnType(Types.KBoolean)
                    }
                    val params = if (instance == null) {
                        paramPacking + isNullParams
                    } else {
                        paramPacking + isNullParams + CodeBlock.of("it").returnType(instance.typeInfo.jniType.nativeType)
                    }
                    val paramsCode = params.joinToCode(prefix = "\n", separator = ",\n", suffix = "\n") { it.code }
                    val callExternalCode = CodeBlock.of("%L(%L)", f.callToExternal.simpleName, paramsCode).returnType(f.returnType.jniType.jvmType)
                    val returnValue = if (f is KSJniCall.Suspend) {
                        callExternalCode // No need to convert, callback already converted.
                    } else {
                        f.returnType.unpackCodeJvm(callExternalCode)
                    }

                    if (instance != null) {
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
        // top level
        val fileSpecs = generateNonInstanceJvm()
        registry.nativeInstances.forEach { (_, instance) ->
            generateJvmInstance(instance, fileSpecs)
        }
        return fileSpecs.values.map { it.build() }
    }

    private fun generateNonInstanceJvm(): MutableMap<ClassName, FileSpec.Builder> {
        val fileSpecs = mutableMapOf<ClassName, FileSpec.Builder>()
        registry.jniCalls.groupBy { it.parent }.map { (parent, funs) ->
            val funSpecs = generateJvmFunctions(funs, null)
            val fileSpec = fileSpecs.getOrPut(parent.className) { FileSpec.builder(parent.className) }
            when (parent) {
                is FunctionParent.Class -> error("Expected non-class parent for jniCall, got $parent.")
                is FunctionParent.Object -> {
                    val obj = TypeSpec.objectBuilder(parent.className)
                        .addModifiers(KModifier.ACTUAL, (parent as FunctionParent.WithVisibility).modifier ?: KModifier.PUBLIC)
                        .addFunctions(funSpecs)
                        .build()
                    fileSpec.addType(obj)
                }
                is FunctionParent.TopLevel -> {
                    fileSpec.addFunctions(funSpecs)
                }
            }
        }
        return fileSpecs
    }

    private fun generateJvmInstance(instance: KSInstance, fileSpecs: MutableMap<ClassName, FileSpec.Builder>) {
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

            val specs = generateJvmFunctions(functions, instance)

            val flowProps = instance.flowProps
            val factory = FunSpec.builder("as${parent.className.simpleName}")
                .returns(parent.className)
                .receiver(Types.KLong)
                .addCode("return %T(this)", parent.className)
                .build()
            val factoryFileClass = parent.className.withSuffix("_converter")
            val factoryFileSpec = FileSpec.builder(factoryFileClass)
                .addFunction(factory)

            fileSpecs[factoryFileClass] = factoryFileSpec

            val fileSpec = fileSpecs.getOrPut(instance.className) { FileSpec.builder(instance.className) }

            val type = TypeSpec.classBuilder(instance.className)
                .addSuperinterfaces(instance.superInterfaces)
                .superclass(Types.NativeInstanceJvm)
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
            fileSpec.addType(type)
        }
    }

}


val Set<Modifier>.visibilityKModifier: KModifier
    get() = if (this.contains(Modifier.PRIVATE))
        KModifier.PRIVATE
    else if (this.contains(Modifier.PROTECTED))
        KModifier.PROTECTED
    else if (this.contains(Modifier.INTERNAL))
        KModifier.INTERNAL
    else KModifier.PUBLIC

val Set<Modifier>.suspendKModifier: KModifier? get() = if (Modifier.SUSPEND in this) KModifier.SUSPEND else null
val Set<Modifier>.overrideKModifier: KModifier? get() = if (Modifier.OVERRIDE in this) KModifier.OVERRIDE else null
val Set<Modifier>.actualModifier: KModifier? get() = if (Modifier.EXPECT in this) KModifier.ACTUAL else null