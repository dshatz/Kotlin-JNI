package com.dshatz.kni

import com.dshatz.kni.jniCall.visibilityKModifier
import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.model.KSConstructor
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.utils.PlatformContext
import com.dshatz.kni.utils.ProcessorContext
import com.dshatz.kni.utils.ResolverContext
import com.dshatz.kni.utils.capitalized
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

abstract class BaseProcessor {
    abstract val mapper: TypeMapper
    abstract val logger: KSPLogger
    abstract val registry: Registry

    context(r: ResolverContext, p: PlatformContext)
    protected fun List<KSValueParameter>.toTypeInfos(saveTypes: Boolean = true): List<ParamInfo> {
        return map {
            val c = ProcessorContext(r.resolver, p.platform, r.moduleName).forDeclaration(it)
            ParamInfo(
                it.name!!.asString(),
                context(c) { mapper.mapType(it.type, saveType = saveTypes) }
            )
        }
    }

    @Suppress("DefaultLocale")
    private fun KSFunctionDeclaration.topLevelFunLocation(): FunctionParent? {
        val fileName = containingFile?.fileName
        return fileName?.replace(".kt", "")?.substringBeforeLast('.')?.let { cls ->
            val clsKt = cls.capitalized() + "Kt"
            val pkg = packageName.asString()
            val classname = ClassName(pkg, cls)
            FunctionParent.TopLevel(
                classNameKt = ClassName(pkg,clsKt),
                className = classname,
            )
        }
    }

    @OptIn(KspExperimental::class)
    context(ctx: ResolverContext, p: PlatformContext)
    fun KSFunctionDeclaration.functionLocation(): FunctionParent {
        return closestClassDeclaration()?.innerFunLocation()
            ?: topLevelFunLocation()
            ?: error("Could not derive classname")
    }

    context(ctx: ResolverContext, p: PlatformContext)
    fun KSClassDeclaration.innerFunLocation(): FunctionParent {
        val type = when (classKind) {
            ClassKind.CLASS, ClassKind.INTERFACE -> {
                val constructors = takeIf { it.classKind == ClassKind.CLASS }
                    ?.getConstructors()
                    ?.mapIndexed { idx, constructor ->
                        KSConstructor(
                            id = idx,
                            params = constructor.parameters.toTypeInfos(),
                            modifier = constructor.modifiers.visibilityKModifier
                        )
                    }?.toList().orEmpty()
                FunctionParent.Class(
                    className = this.toClassName(),
                    superTypes = superTypes.map { it.resolve().toTypeName() }.toList()
                )
            }
            ClassKind.OBJECT -> {
                val (superClass, superInterfaces) = splitSuperTypes()
                FunctionParent.Object(
                    className = this.toClassName(),
                    superClass = superClass?.toTypeName(),
                    superInterfaces = superInterfaces.map { it.toTypeName() }
                )
            }
            else -> error("Unsupported ClassKind: ${classKind}")
        }
        return type
    }

    /**
     * Splits declared supertypes into the superclass (if any, excluding Any) and interfaces.
     */
    protected fun KSClassDeclaration.splitSuperTypes(): Pair<KSTypeReference?, List<KSTypeReference>> {
        val (superClasses, superInterfaces) = superTypes
            .filter { it.toTypeName() != ANY }
            .partition { (it.resolve().declaration as? KSClassDeclaration)?.classKind == ClassKind.CLASS }
        return superClasses.singleOrNull() to superInterfaces
    }

    /**
     * Generated actuals call the superclass constructor without arguments, since expect declarations can't pass any.
     */
    protected fun KSClassDeclaration.checkSuperClassConstructor() {
        val superDecl = splitSuperTypes().first?.resolve()?.declaration as? KSClassDeclaration ?: return
        val hasNoArgConstructor = superDecl.getConstructors().any { c -> c.parameters.all { p -> p.hasDefault } }
        if (!hasNoArgConstructor) {
            logger.error("Superclass ${superDecl.toClassName()} of ${toClassName()} must have a no-arg constructor to be used with @JniCall.", this)
        }
    }
}
