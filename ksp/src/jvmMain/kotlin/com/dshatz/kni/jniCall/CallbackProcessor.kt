package com.dshatz.kni.jniCall

import com.dshatz.kni.BaseProcessor
import com.dshatz.kni.Registry
import com.dshatz.kni.Registry.Platform
import com.dshatz.kni.TypeInfo
import com.dshatz.kni.TypeMapper
import com.dshatz.kni.Types
import com.dshatz.kni.Types.typeOf
import com.dshatz.kni.annotations.JniCallback
import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.model.KSCallback
import com.dshatz.kni.model.KSCallbackFun
import com.dshatz.kni.model.KSInstance
import com.dshatz.kni.model.KSJniCall
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.model.getSignature
import com.dshatz.kni.utils.JvmContext
import com.dshatz.kni.utils.NativeContext
import com.dshatz.kni.utils.PlatformContext
import com.dshatz.kni.utils.ProcessorContext
import com.dshatz.kni.utils.ResolverContext
import com.dshatz.kni.utils.SymContext
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class CallbackProcessor(
    override val registry: Registry,
    override val logger: KSPLogger,
    override val mapper: TypeMapper
): BaseProcessor() {

    private fun getDefinitions(
        resolver: Resolver
    ): List<KSClassDeclaration> {
        return resolver.getSymbolsWithAnnotation(JniCallback::class.java.name).toList()
            .filterIsInstance<KSClassDeclaration>().distinct()
            .filter { declaration ->
                if (declaration.getAllSuperTypes().none { it.toTypeName() typeOf Types.AutoCloseable }) {
                    logger.error("@JniCallback annotated interface should extend kotlin.AutoCloseable.", declaration)
                    return@filter false
                }
                true
            }
    }

    context(ctx: ProcessorContext)
    fun collectCallbackClasses() {
        val classes = getDefinitions(ctx.resolver).map { it.toClassName() }
        registry.callbackClasses.addAll(classes)
    }


    context(res: ResolverContext)
    private fun KSFunctionDeclaration.getJvmSignature(): String {
        return res.runAsJvm {
            val jvmParams = parameters.toTypeInfos(saveTypes = false)
            val jvmReturn = mapper.mapType(returnType!!, saveType = false)
            getSignature(jvmParams, jvmReturn)
        }
    }

    context(res: ResolverContext)
    private fun KSFunctionDeclaration.getJvmReturnType(): TypeInfo {
        return res.runAsJvm {
            context(SymContext(returnType!!)) {
                mapper.mapType(returnType!!, saveType = false)
            }
        }
    }

    context(res: ResolverContext)
    private fun KSFunctionDeclaration.getJvmParameters(): List<ParamInfo> {
        return res.runAsJvm {
            parameters.toTypeInfos(saveTypes = false)
        }
    }

    context(ctx: ResolverContext, p: PlatformContext)
    fun collectCallbacks() {
        val callbacks = getDefinitions(ctx.resolver)
            .associate { declaration ->
            val funDeclarations = declaration.declarations
                .filterIsInstance<KSFunctionDeclaration>()
                .filterNot { it.isConstructor() }
                .filterNot {
                    // Remove close function, if defined (overridden). Call to it is already implemented in BaseCallback.
                    it.simpleName.asString() == "close" && it.parameters.isEmpty() && it.returnType?.toTypeName() == UNIT
                }
                val callbackClass = declaration.toClassName()
                val callbackType = TypeInfo.callback(callbackClass)
                val funs = funDeclarations.map { f ->
                    val jvmSignature = f.getJvmSignature()
                    if (Modifier.SUSPEND in f.modifiers) {
                        KSCallbackFun.Suspend(
                            name = f.simpleName.asString(),
                            returnType = mapper.mapType(f.returnType!!),
                            parameters = f.parameters.toTypeInfos(),
                            parent = f.functionLocation() as FunctionParent.Class,
                            callbackType = callbackType,
                            platform = p.platform,
                            jvmReturnType = f.getJvmReturnType(),
                            jvmParameters = f.getJvmParameters()
                        )
                    } else {
                        KSCallbackFun.Blocking(
                            name = f.simpleName.asString(),
                            returnType = mapper.mapType(f.returnType!!),
                            parameters = f.parameters.toTypeInfos(),
                            parent = f.functionLocation() as FunctionParent.Class,
                            callbackType = callbackType,
                            platform = p.platform,
                            jvmSignature = jvmSignature
                        )
                    }

            }.toList()
            callbackClass to KSCallback(
                type = callbackClass,
                funs = funs,
                baseClass = callbackClass,
                platform = p.platform
            )
        }
        registry.callbacks.putAll(callbacks)
        registerSuspendAdapters()
    }

    context(_: NativeContext, _: ResolverContext)
    fun generateNative(): List<FileSpec> {
        return registry.callbacks.values.map { it.generateNative() }
    }

    fun generateSuspendAdapters(): List<FileSpec> {
        return registry.jniCallSuspendAdapters.map { a ->
            val callback = TypeSpec.interfaceBuilder(a.type)
                .addAnnotation(JniCallback::class)
                .addSuperinterface(Types.AutoCloseable)
                .apply {
                    a.baseClass?.let(::addSuperinterface)
                }
                .build()
            FileSpec.builder(a.type)
                .addType(callback)
                .build()
        }
    }

    context(_: JvmContext, _: ResolverContext)
    fun generateJvm(): List<FileSpec> {
        return registry.callbacks.values.map { it.generateJvmAdapter() }
    }

    fun generateBaseSuspendAdapters(): List<FileSpec> {
        val callbacks = registry.callbacks.values.flatMap(KSCallback::funs).filterIsInstance<KSCallbackFun.Suspend>()
            .map(KSCallbackFun.Suspend::generateSuspendAdapter)

        val instances = registry.nativeInstances.values.flatMap(KSInstance::funs).filterIsInstance<KSJniCall.Suspend>()
            .map(KSJniCall.Suspend::generateBaseSuspendAdapter)

        val calls = registry.jniCalls.filterIsInstance<KSJniCall.Suspend>()
            .map(KSJniCall.Suspend::generateBaseSuspendAdapter)
        return callbacks + instances + calls
    }

    context(ctx: PlatformContext)
    private fun registerSuspendAdapters() {
        val adapters = registry.callbacks.values.flatMap {
            it.funs.filterIsInstance<KSCallbackFun.Suspend>().map { f ->
                KSInstance(
                    f.suspendAdapterClass,
                    emptyList(),
                    listOf(
                        KSJniCall.Blocking(
                            f.onValueFun,
                            returnType = TypeInfo.Unit,
                            parameters = listOf(ParamInfo("value", f.returnType)),
                            parent = FunctionParent.Class(
                                f.suspendAdapterClass,
                                emptyList(),
                                KModifier.PRIVATE
                            ),
                            modifiers = setOf(KModifier.OVERRIDE),
                            nativeInstance = f.suspendAdapter,
                            platform = ctx.platform,
                            jvmSignature = getSignature(
                                listOf(ParamInfo("value", f.jvmReturnType)),
                                TypeInfo.Unit
                            )
                        ),
                        KSJniCall.Blocking(
                            f.onFailureFun,
                            returnType = TypeInfo.Unit,
                            parameters = listOf(
                                ParamInfo("message", TypeInfo.PlatformString),
                                ParamInfo("stackTrace", TypeInfo.PlatformString)
                            ),
                            parent = FunctionParent.Class(
                                f.suspendAdapterClass,
                                emptyList(),
                                KModifier.PRIVATE
                            ),
                            modifiers = setOf(KModifier.OVERRIDE),
                            nativeInstance = f.suspendAdapter,
                            platform = ctx.platform,
                            jvmSignature = f.jvmSignature
                        )
                    ),
                    flowProps = emptyList(),
                    superInterfaces = setOf(
                        Types.SuspendCallback.parameterizedBy(f.returnType.kotlinType)
                    ),
                    baseClass = f.baseSuspendAdapterClass,
                    platform = ctx.platform
                )
            }
        }
        registry.nativeInstances.putAll(adapters.associateBy { it.className })
        registry.callbackSuspendAdapters.addAll(adapters)
    }
}

internal object Def {
    val CallStaticObjMethodA = MemberName("com.dshatz.kni.utils", "CallStaticObjMethodA")
    val CallStaticObjMethodANullable = MemberName("com.dshatz.kni.utils", "CallStaticObjMethodANullable")
    val CallStaticVoidMethodA = MemberName("com.dshatz.kni.utils", "CallStaticVoidMethodA")

    val memScoped = MemberName("kotlinx.cinterop", "memScoped")
    val allocArray = MemberName("kotlinx.cinterop", "allocArray")
    val reinterpret = MemberName("kotlinx.cinterop", "reinterpret")

    context(_: NativeContext)
    internal fun callHelper(typeInfo: TypeInfo): MemberName {
        val type = typeInfo.jniType.jniType
        return when(type.copy(nullable = false)) {
            Types.JObject,
            Types.JByteArray,
            Types.JString -> if (type.isNullable) CallStaticObjMethodANullable else CallStaticObjMethodA
            Types.UnitOrVoid -> CallStaticVoidMethodA
            else -> {
                val clsName = (typeInfo.kotlinType as? ClassName)?.simpleName ?: error("Unable to map callback return to jni function: $type")
                MemberName("com.dshatz.kni.utils", "CallStatic${clsName}MethodA")
            }
        }
    }
}
