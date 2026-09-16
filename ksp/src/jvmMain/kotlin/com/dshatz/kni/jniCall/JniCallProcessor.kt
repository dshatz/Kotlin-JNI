package com.dshatz.kni.jniCall

import com.dshatz.kni.BaseProcessor
import com.dshatz.kni.Registry
import com.dshatz.kni.Registry.Platform
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.TypeMapper
import com.dshatz.kni.Types
import com.dshatz.kni.Types.typeOf
import com.dshatz.kni.annotations.JniCall
import com.dshatz.kni.annotations.JniAdapter
import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.kspfix.findAnnotation
import com.dshatz.kni.kspfix.getClassArgument
import com.dshatz.kni.model.KSCallback
import com.dshatz.kni.model.KSCallbackFun
import com.dshatz.kni.model.KSConstructor
import com.dshatz.kni.model.KSInstance
import com.dshatz.kni.model.KSJniCall
import com.dshatz.kni.model.KSWrapper
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.model.PropInfo
import com.dshatz.kni.model.flow.KSFlowProp
import com.dshatz.kni.model.getSignature
import com.dshatz.kni.utils.SymbolContext
import com.dshatz.kni.utils.JvmContext
import com.dshatz.kni.utils.NativeContext
import com.dshatz.kni.utils.PlatformContext
import com.dshatz.kni.utils.ProcessorContext
import com.dshatz.kni.utils.ResolverContext
import com.dshatz.kni.utils.SymContext
import com.dshatz.kni.utils.withSuffix
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
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
        return resolver.getSymbolsWithAnnotation(JniCall::class.java.name)
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
            .toList()

    }

    context(ctx: ProcessorContext)
    fun processJniAdapters() {
        val types = ctx.resolver.getSymbolsWithAnnotation(JniAdapter::class.java.name)
            .filterIsInstance<KSClassDeclaration>()
            .filterNot { it.isExpect }
            .associate {
                val adapterCls = it.findAnnotation<JniAdapter>()!!.getClassArgument("adapter")!!
                val declaration = ctx.resolver.getClassDeclarationByName(adapterCls.canonicalName)
                val expectedSupertype = when (ctx.platform) {
                    Platform.COMMON -> error("Platform wrappers called from common code")
                    Platform.NATIVE -> Types.NativeJniAdapter
                    Platform.JVM -> Types.JvmJniAdapter
                }

                val superType = declaration?.superTypes?.singleOrNull { it.toTypeName() typeOf expectedSupertype } ?: run {
                    logger.error("Expected PlatformWrapper to extend $expectedSupertype for platform $ctx.platform, found: ${declaration?.superTypes?.joinToString { it.toTypeName().toString() }}", declaration)
                    error("Failed to process platform wrappers")
                }
                val actualType = (superType.toTypeName() as ParameterizedTypeName).typeArguments[1]
                logger.info("Type ${it.toClassName()} will be passed as $actualType for $ctx.platform")

                val typeInfo = TypeInfo.JniAdapter(
                    it.toClassName(),
                    context(ctx.forDeclaration(superType).copy(forAdapter = true)) { mapper.mapType(actualType) },
                    adapterClassName = adapterCls
                )
                it.toClassName() to KSWrapper(
                    adapterCls = adapterCls,
                    inner = actualType,
                    type = typeInfo
                )
            }
        registry.jniAdapterTypes.putAll(types)
    }

    context(ctx: ProcessorContext)
    fun collectNativeInstanceClasses() {
        val instanceClasses = getAnnotatedJniCalls(ctx.resolver)
            .map { it.parentDeclaration }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.CLASS }
            .distinct()
            .map {
                it.toClassName()
            }
        registry.nativeInstanceClasses.addAll(instanceClasses)
    }

    context(decl: SymbolContext, res: ResolverContext)
    private fun TypeName.jvmType(): TypeInfo {
        return ProcessorContext(res.resolver, Platform.JVM, res.moduleName).runAsJvm {
            mapper.mapType(this, saveType = false)
        }
    }

    context(ctx: ResolverContext, p: PlatformContext)
    fun collectNativeInstances() {
        val instances = getAnnotatedJniCalls(ctx.resolver)
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
                        val ctx = SymContext(it.declaration)
                        context(ctx) {
                            val typeArg = (it.type as ParameterizedTypeName).typeArguments.first()
                            val jvmTypeString = typeArg.jvmType()
                            KSFlowProp(
                                name = it.name,
                                innerType = mapper.mapType(typeArg),
                                instanceClass = parent.className,
                                platform = p.platform,
                                jvmInnerType = jvmTypeString
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
                    modifiers = setOfNotNull(parentDeclaration.modifiers.actualModifier),
                    platform = p.platform
                )
            }
        registry.nativeInstances.putAll(instances.associateBy { it.className })
    }

    context(res: ResolverContext)
    private fun KSFunctionDeclaration.jvmSignature(): String {
        return res.runAsJvm {
            val params = parameters.toTypeInfos(saveTypes = false)
            val returnType = mapper.mapType(returnType!!, saveType = false)
            getSignature(params, returnType)
        }
    }

    context(res: ResolverContext)
    private fun KSFunctionDeclaration.jvmReturnType(): TypeInfo {
        return res.runAsJvm {
            mapper.mapType(returnType!!, saveType = false)
        }
    }

    context(ctx: PlatformContext, res: ResolverContext)
    fun KSFunctionDeclaration.createJniCall(parent: FunctionParent): KSJniCall {
        val returnType = context(SymContext(returnType!!)) {
            mapper.mapType(returnType!!)
        }
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
                nativeInstance = (parent as? FunctionParent.Class)?.className?.let {
                    TypeInfo.nativeInstance(it)
                },
                platform = ctx.platform,
                jvmSignature = jvmSignature(),
                jvmReturnType = jvmReturnType()
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
                nativeInstance = (parent as? FunctionParent.Class)?.className?.let {
                    TypeInfo.nativeInstance(it)
                },
                platform = ctx.platform,
                jvmSignature = jvmSignature()
            )
        }
    }

    context(ctx: ProcessorContext)
    fun collectJniCalls(
    ) {
        val funs = getAnnotatedJniCalls(ctx.resolver)
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

    context(ctx: PlatformContext)
    private fun registerSuspendAdapters() {
        val calls = registry.jniCalls

        val instanceCalls = registry.nativeInstances.values
            .flatMap { it.funs }
        (calls + instanceCalls).filterIsInstance<KSJniCall.Suspend>().forEach { f ->
            val callback = KSCallback(
                type = f.suspendCallbackClass,
                funs = listOf(
                    KSCallbackFun.Blocking(
                        f.onSuccessFun,
                        returnType = TypeInfo.Unit,
                        parameters = f.onSuccessParams,
                        parent = f.parent,
                        callbackType = f.callbackType,
                        platform = ctx.platform,
                        jvmSignature = f.onSuccessSignature
                    ),
                    KSCallbackFun.Blocking(
                        f.onFailureFun,
                        returnType = TypeInfo.Unit,
                        parameters = listOf(
                            ParamInfo("message", TypeInfo.PlatformString),
                            ParamInfo("stackTrace", TypeInfo.PlatformString)
                        ),
                        parent = f.parent,
                        callbackType = f.callbackType,
                        platform = ctx.platform,
                        jvmSignature = getSignature(
                            listOf(
                                ParamInfo("message", context(JvmContext()) { TypeInfo.PlatformString }),
                                ParamInfo("stackTrace", context(JvmContext()) { TypeInfo.PlatformString })
                            ),
                            TypeInfo.Unit
                        )
                    )
                ),
                baseClass = f.baseSuspendCallback,
                platform = ctx.platform
            )
            registry.callbacks[f.baseSuspendCallback] = callback
            registry.jniCallSuspendAdapters.add(callback)
        }
    }

    context(_: NativeContext, c: ResolverContext)
    fun generateNative(): List<FileSpec> {
        val instances = registry.nativeInstances.values.map { it.generateNative() }
        return registry.jniCalls.groupBy { it.parent }.map { (parent, functions) ->
            val fileClass = parent.classNameKt.withSuffix("_jniCalls")
            FileSpec.builder(fileClass)
                .addFunctions(functions.map { it.generateCnameFunction() })
                .build()
        } + instances
    }

    context(_: JvmContext, _: ResolverContext)
    fun generateJvm(): List<FileSpec> {
        // top level
        val fileSpecs = generateNonInstanceJvm()
        registry.nativeInstances.forEach { (_, instance) ->
            instance.generateJvmInstance(instance, fileSpecs)
        }
        return fileSpecs.values.map { it.build() }
    }

    context(_: JvmContext, _: ResolverContext)
    private fun generateNonInstanceJvm(): MutableMap<ClassName, FileSpec.Builder> {
        val fileSpecs = mutableMapOf<ClassName, FileSpec.Builder>()
        registry.jniCalls.groupBy { it.parent }.map { (parent, funs) ->
            val funSpecs = funs.flatMap { it.generateJvmFunctions(null) }
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