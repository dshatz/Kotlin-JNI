package dev.datlag.nkommons

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.datlag.nkommons.TypeMatcher.typeOf
import dev.datlag.nkommons.callable.NativeCallable
import dev.datlag.nkommons.callable.getNativeImplClass
import dev.datlag.nkommons.kspfix.getAnnotationValue

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

        private val callableRegistry: MutableSet<ClassName> = mutableSetOf()

        private val codeGenerator: CodeGenerator
            get() = env.codeGenerator

        override fun process(resolver: Resolver): List<KSAnnotated> {
            val bridges = getAnnotatedBridges(resolver).map { NativeCallable.generateNativeBridge(it, env.logger) }
            if (bridges.any { it == null }) {
                env.logger.error("Callable processing failed")
                return emptyList()
            }
            bridges.filterNotNull().forEach {
                it.fileSpec.writeTo(codeGenerator, it.deps)
                callableRegistry.add(it.cls)
            }
            getAnnotatedFunctions(resolver).forEach(::generateJNIMethod)

            return emptyList()
        }

        private fun getAnnotatedFunctions(resolver: Resolver): List<KSFunctionDeclaration> {
            val jniConnectAnnotated = resolver.getSymbolsWithAnnotation(JNIConnect::class.java.name).toList()

            return (jniConnectAnnotated).filterIsInstance<KSFunctionDeclaration>().distinct()
        }

        private fun getAnnotatedBridges(resolver: Resolver): List<KSClassDeclaration> {
            val nativeCallableAnnotated = resolver.getSymbolsWithAnnotation(CallableFromNative::class.java.name).toList()
            return nativeCallableAnnotated.filterIsInstance<KSClassDeclaration>().distinct()
        }

        @OptIn(KspExperimental::class)
        private fun generateJNIMethod(declaration: KSFunctionDeclaration) {
            val packageName = declaration.packageName.asString()
            val functionName = declaration.simpleName.asString()
            val kotlinReturnType = declaration.returnType?.toTypeName() ?: TypeMatcher.UnitOrVoid
            val source = listOfNotNull(
                declaration.containingFile,
                declaration.parentDeclaration?.containingFile
            )

            val expectedPackageName = declaration.getAnnotationValue<JNIConnect, String>("packageName")?.ifBlank { null } ?: packageName
            val expectedClassName = declaration.getAnnotationValue<JNIConnect, String>("className")?.ifBlank { null }
            val expectedFunctionName = declaration.getAnnotationValue<JNIConnect, String>("functionName")?.ifBlank { null } ?: functionName
            val jniReturnType = TypeMatcher.jniTypeFor(kotlinReturnType, forReturn = true) ?: run {
                env.logger.error("Unsupported return type $kotlinReturnType", declaration.returnType)
                return
            }
            val finalReturnType = jniReturnType ?: kotlinReturnType

            val jniCName = CNameUtils.jniFunctionCName(
                packageName = expectedPackageName,
                className = expectedClassName,
                functionName = expectedFunctionName
            )

            val params = declaration.parameters.mapIndexed { index, param ->
                val name = "p$index"
                val kotlinType = param.type.toTypeName()
                val jniType = TypeMatcher.jniTypeFor(kotlinType, forReturn = false) ?: run {
                    if (kotlinType in callableRegistry) {
                        TypeMatcher.JObject
                    } else {
                        env.logger.error("Unknown parameter type: $kotlinType", param)
                        return
                    }
                }
                val nullCheck = if (finalReturnType.isNullable) {
                    " ?: return null"
                } else {
                    "!!"
                }
                val (code, subMember) = when {
                    jniType typeOf TypeMatcher.JBoolean && kotlinType typeOf TypeMatcher.KBoolean -> {
                        "$name.%M()" to TypeMatcher.Method.ToKBoolean
                    }
                    jniType typeOf TypeMatcher.JBooleanArray && kotlinType typeOf TypeMatcher.KBooleanArray -> {
                        "$name.%M(env)$nullCheck" to TypeMatcher.Method.ToKBooleanArray
                    }
                    jniType typeOf TypeMatcher.JByteArray && kotlinType typeOf TypeMatcher.KByteArray -> {
                        "$name.%M(env)$nullCheck" to TypeMatcher.Method.ToKByteArray
                    }
                    jniType typeOf TypeMatcher.JChar && kotlinType typeOf TypeMatcher.KChar -> {
                        "$name.%M()" to TypeMatcher.Method.ToKChar
                    }
                    jniType typeOf TypeMatcher.JCharArray && kotlinType typeOf TypeMatcher.KCharArray -> {
                        "$name.%M(env)$nullCheck" to TypeMatcher.Method.ToKCharArray
                    }
                    jniType typeOf TypeMatcher.JDoubleArray && kotlinType typeOf TypeMatcher.KDoubleArray -> {
                        "$name.%M(env)$nullCheck" to TypeMatcher.Method.ToKDoubleArray
                    }
                    jniType typeOf TypeMatcher.JFloatArray && kotlinType typeOf TypeMatcher.KFloatArray -> {
                        "$name.%M(env)$nullCheck" to TypeMatcher.Method.ToKFloatArray
                    }
                    jniType typeOf TypeMatcher.JIntArray && kotlinType typeOf TypeMatcher.KIntArray -> {
                        "$name.%M(env)$nullCheck" to TypeMatcher.Method.ToKIntArray
                    }
                    jniType typeOf TypeMatcher.JLongArray && kotlinType typeOf TypeMatcher.KLongArray -> {
                        "$name.%M(env)$nullCheck" to TypeMatcher.Method.ToKLongArray
                    }
                    jniType typeOf TypeMatcher.JShortArray && kotlinType typeOf TypeMatcher.KShortArray -> {
                        "$name.%M(env)$nullCheck" to TypeMatcher.Method.ToKShortArray
                    }
                    jniType typeOf TypeMatcher.JString && kotlinType typeOf TypeMatcher.KString -> {
                        "$name.%M(env)$nullCheck" to TypeMatcher.Method.ToKString
                    }
                    jniType typeOf TypeMatcher.JObject && kotlinType typeOf TypeMatcher.KByteBuffer -> {
                        "$name.%M(env)$nullCheck" to TypeMatcher.Method.ToKDirectByteBuffer
                    }
                    jniType typeOf TypeMatcher.JObject && kotlinType in callableRegistry -> {
                        val qualified = kotlinType.getNativeImplClass().let {
                            it.packageName + "." + it.simpleName
                        }
                        "${qualified}(env, $name)" to null
                    }
                    else -> name to null
                }

                val spec = ParameterSpec.builder(name, jniType).build()

                ParamInfo(code, subMember, spec)
            }

            val (code, subMember) = when {
                jniReturnType typeOf TypeMatcher.JBoolean && kotlinReturnType typeOf TypeMatcher.KBoolean -> {
                    "return %M(${params.joinToString { it.code }}).%M()" to TypeMatcher.Method.ToJBoolean
                }
                jniReturnType typeOf TypeMatcher.JBooleanArray && kotlinReturnType typeOf TypeMatcher.KBooleanArray -> {
                    "return %M(${params.joinToString { it.code }}).%M(env)" to TypeMatcher.Method.ToJBooleanArray
                }
                jniReturnType typeOf TypeMatcher.JByteArray && kotlinReturnType typeOf TypeMatcher.KByteArray -> {
                    "return %M(${params.joinToString { it.code }}).%M(env)" to TypeMatcher.Method.ToJByteArray
                }
                jniReturnType typeOf TypeMatcher.JChar && kotlinReturnType typeOf TypeMatcher.KChar -> {
                    "return %M(${params.joinToString { it.code }}).%M()" to TypeMatcher.Method.ToJChar
                }
                jniReturnType typeOf TypeMatcher.JCharArray && kotlinReturnType typeOf TypeMatcher.KCharArray -> {
                    "return %M(${params.joinToString { it.code }}).%M(env)" to TypeMatcher.Method.ToJCharArray
                }
                jniReturnType typeOf TypeMatcher.JDoubleArray && kotlinReturnType typeOf TypeMatcher.KDoubleArray -> {
                    "return %M(${params.joinToString { it.code }}).%M(env)" to TypeMatcher.Method.ToJDoubleArray
                }
                jniReturnType typeOf TypeMatcher.JFloatArray && kotlinReturnType typeOf TypeMatcher.KFloatArray -> {
                    "return %M(${params.joinToString { it.code }}).%M(env)" to TypeMatcher.Method.ToJFloatArray
                }
                jniReturnType typeOf TypeMatcher.JIntArray && kotlinReturnType typeOf TypeMatcher.KIntArray -> {
                    "return %M(${params.joinToString { it.code }}).%M(env)" to TypeMatcher.Method.ToJIntArray
                }
                jniReturnType typeOf TypeMatcher.JLongArray && kotlinReturnType typeOf TypeMatcher.KLongArray -> {
                    "return %M(${params.joinToString { it.code }}).%M(env)" to TypeMatcher.Method.ToJLongArray
                }
                jniReturnType typeOf TypeMatcher.JShortArray && kotlinReturnType typeOf TypeMatcher.KShortArray -> {
                    "return %M(${params.joinToString { it.code }}).%M(env)" to TypeMatcher.Method.ToJShortArray
                }
                jniReturnType typeOf TypeMatcher.JString && kotlinReturnType typeOf TypeMatcher.KString -> {
                    "return %M(${params.joinToString { it.code }}).%M(env)" to TypeMatcher.Method.ToJString
                }
                else -> "return %M(${params.joinToString { it.code }})" to null
            }

            val method = FunSpec.builder("_${functionName}JNI")
                .addAnnotation(CNameUtils.NativeOptIn)
                .addAnnotation(CNameUtils.cnameFor(jniCName))
                .addParameter("env", TypeMatcher.Environment)
                .addParameter("clazz", TypeMatcher.JObject)
                .addParameters(params.map { it.spec })
                .returns(finalReturnType)
                .apply {
                    val originalMethod = MemberName(packageName, functionName)
                    val memberCalls = listOfNotNull(*params.mapNotNull { it.member }.toTypedArray(), subMember)

                    addComment("FORCING BODY")
                    addStatement(code, originalMethod, *memberCalls.toTypedArray())
                }
                .build()

            FileSpec.builder(packageName, "_${functionName}JNI")
                .addFunction(method)
                .build()
                .writeTo(codeGenerator, Dependencies(false, *source.toTypedArray()))
        }

        data class ParamInfo(
            val code: String,
            val member: MemberName?,
            val spec: ParameterSpec
        )
    }
}