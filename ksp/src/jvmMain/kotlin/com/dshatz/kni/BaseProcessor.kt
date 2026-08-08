package com.dshatz.kni

import com.dshatz.kni.jniCall.visibilityKModifier
import com.dshatz.kni.kspfix.FunctionParent
import com.dshatz.kni.model.KSConstructor
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.model.PropInfo
import com.dshatz.kni.utils.capitalized
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

abstract class BaseProcessor {
    abstract val mapper: TypeMapper
    abstract val logger: KSPLogger
    abstract val registry: Registry

    protected fun List<KSValueParameter>.toTypeInfos(): List<ParamInfo> {
        return map {
            ParamInfo(it.name!!.asString(), mapper.mapType(it.type))
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
    fun KSFunctionDeclaration.functionLocation(): FunctionParent {
        return closestClassDeclaration()?.innerFunLocation()
            ?: topLevelFunLocation()
            ?: error("Could not derive classname")
    }

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
            ClassKind.OBJECT -> FunctionParent.Object(this.toClassName())
            else -> error("Unsupported ClassKind: ${classKind}")
        }
        return type
    }
}