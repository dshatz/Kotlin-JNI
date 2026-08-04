package com.dshatz.kni.model

import com.dshatz.kni.TypeInfo
import com.dshatz.kni.utils.TypedCode
import com.dshatz.kni.utils.returnType
import com.squareup.kotlinpoet.CodeBlock

data class ParamInfo(
    val name: String,
    val typeInfo: TypeInfo,
) {
    fun paramCodeJvm(): TypedCode {
        return CodeBlock.of("%N", name).returnType(typeInfo.jniType.jvmType)
    }

    fun paramCodeNative(): TypedCode {
        return CodeBlock.of("%N", name).returnType(typeInfo.jniType.nativeType)
    }

    fun paramCodeKotlin(): TypedCode {
        return CodeBlock.of("%N", name).returnType(typeInfo.kotlinType)
    }
}

