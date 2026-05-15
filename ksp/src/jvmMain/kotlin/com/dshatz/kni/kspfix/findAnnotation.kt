package com.dshatz.kni.kspfix

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName

// Workaround for https://github.com/google/ksp/issues/2356

@OptIn(KspExperimental::class)
inline fun <reified A : Annotation> KSAnnotated.findAnnotation(): KSAnnotation? {
    return this.annotations.firstOrNull {
        it.shortName.getShortName() == A::class.simpleName && it.annotationType.resolve().declaration.qualifiedName?.asString() == A::class.qualifiedName
    }
}

inline fun <reified T> KSAnnotation.getArgumentValueByName(name: String): T? {
    return this.arguments.firstOrNull {
        it.name?.asString() == name && it.value != null && it.value is T
    }?.value as? T
}

fun KSAnnotation.getClassArgument(name: String): ClassName? {
    return this.arguments.firstOrNull {
        it.name?.asString() == name && it.value != null && it.value is KSType
    }
        ?.let { it.value as? KSType }
        ?.declaration
        ?.let {
            ClassName(it.packageName.asString(), it.simpleName.asString())
        }
}

inline fun <reified A : Annotation, reified T> KSAnnotated.getAnnotationValue(name: String): T? {
    return findAnnotation<A>()?.getArgumentValueByName<T>(name)
}