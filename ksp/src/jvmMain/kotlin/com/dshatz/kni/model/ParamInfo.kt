package com.dshatz.kni.model

import com.dshatz.kni.TypeInfo
import com.dshatz.kni.utils.JvmContext
import com.dshatz.kni.utils.NativeContext
import com.dshatz.kni.utils.commonCode
import com.dshatz.kni.utils.jvmCode
import com.dshatz.kni.utils.nativeCode
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterSpec

data class ParamInfo(
    val name: String,
    val typeInfo: TypeInfo,
) {

    context(_: JvmContext)
    val refJvm get() = CodeBlock.of("%N", name).jvmCode(typeInfo)

    context(_: NativeContext)
    val refNative get() = CodeBlock.of("%N", name).nativeCode(typeInfo)
    val refCommon = CodeBlock.of("%N", name).commonCode(typeInfo)

    context(_: JvmContext)
    fun paramSpecJvm(): ParameterSpec {
        return ParameterSpec(name, typeInfo.jniType.jniType)
    }

    fun paramSpecKotlin(): ParameterSpec {
        return ParameterSpec(name, typeInfo.kotlinType)
    }

    context(_: NativeContext)
    fun paramSpecNative(): ParameterSpec {
        return ParameterSpec(name, typeInfo.jniType.jniType)
    }
}

