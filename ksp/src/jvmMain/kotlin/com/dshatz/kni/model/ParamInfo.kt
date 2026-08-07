package com.dshatz.kni.model

import com.dshatz.kni.TypeInfo
import com.dshatz.kni.utils.commonCode
import com.dshatz.kni.utils.jvmCode
import com.dshatz.kni.utils.nativeCode
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterSpec

data class ParamInfo(
    val name: String,
    val typeInfo: TypeInfo,
) {

    val refJvm = CodeBlock.of("%N", name).jvmCode(typeInfo)
    val refNative = CodeBlock.of("%N", name).nativeCode(typeInfo)
    val refCommon = CodeBlock.of("%N", name).commonCode(typeInfo)

    fun paramSpecJvm(): ParameterSpec {
        return ParameterSpec(name, typeInfo.jniType.jvmType)
    }

    fun paramSpecKotlin(): ParameterSpec {
        return ParameterSpec(name, typeInfo.kotlinType)
    }

    fun paramSpecNative(): ParameterSpec {
        return ParameterSpec(name, typeInfo.jniType.nativeType)
    }
}

