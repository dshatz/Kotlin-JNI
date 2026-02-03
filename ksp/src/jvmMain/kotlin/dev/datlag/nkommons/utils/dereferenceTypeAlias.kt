package dev.datlag.nkommons.utils

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeReference

fun KSTypeReference.dereferenceTypeAlias(): KSType {
    return (resolve().declaration as? KSTypeAlias)?.type?.resolve() ?: this.resolve()
}