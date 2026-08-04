package com.dshatz.kni.model

import com.dshatz.kni.TypeInfo
import com.dshatz.kni.jniCall.toJniDescriptor
import com.dshatz.kni.kspfix.FunctionParent
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

data class KSJniCall(
    val name: String,
    val returnType: TypeInfo,
    val parameters: List<ParamInfo>,
    override val parent: FunctionParent,
    val declaration: KSFunctionDeclaration
): WithParent

data class KSCallbackFun(
    val name: String,
    val returnType: TypeInfo,
    val parameters: List<ParamInfo>,
    override val parent: FunctionParent
): WithParent {
    fun getSignature(): String {
        val parameterDescriptors = parameters.joinToString("") { parameter ->
            parameter.typeInfo.jniType.jvmType.toJniDescriptor()
        }

        val returnDescriptor = returnType.jniType.jvmType.toJniDescriptor()

        return "($parameterDescriptors)$returnDescriptor"
    }
}

