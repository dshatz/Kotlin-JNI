package com.dshatz.kni.serialization

import com.dshatz.kni.TypeMatcher
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

object SerializerProcessor {

    fun processSerializers(
        env: SymbolProcessorEnvironment,
        serializers: List<KSClassDeclaration>
    ): MutableMap<TypeName, ClassName> {
        val serializerRegistry: MutableMap<TypeName, ClassName> = mutableMapOf()
        serializers.forEach {
            env.logger.info("Processing serializer for ${it.simpleName.asString()}")
            if (it.classKind != ClassKind.OBJECT) {
                env.logger.error("@AddJniSerializer can only be applied to objects.", it)
            } else {
                val superclass = it.superTypes
                    .find { (it.resolve().toTypeName() as ParameterizedTypeName).rawType == TypeMatcher.JniSerializer }
                if (superclass == null) {
                    env.logger.error("@AddJniSerializer annotated object must extend JniSerializer.")
                } else {
                    // all good
                    val targetType = superclass.resolve().arguments.first().toTypeName()
                    serializerRegistry[targetType] = it.toClassName()
                }
            }
        }
        return serializerRegistry
    }

}