package dev.datlag.nkommons.kspfix

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation

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

inline fun <reified A : Annotation, reified T> KSAnnotated.getAnnotationValue(name: String): T? {
    return findAnnotation<A>()?.getArgumentValueByName<T>(name)
}