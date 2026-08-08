package com.dshatz.kni.jniCall

import com.dshatz.kni.Types
import com.dshatz.kni.model.ParamInfo
import com.dshatz.kni.needsIsNullParam
import com.dshatz.kni.utils.nonNullOrPlaceholder
import com.dshatz.kni.utils.nullSafeCall
import com.dshatz.kni.utils.returnType
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.CodeBlock

fun constructNativeArgs(
    args: List<ParamInfo>,
    innerCode: CodeBlock,
): CodeBlock {
    return CodeBlock.Companion.builder()
        .beginControlFlow("%M", Def.memScoped)
        .addStatement("val args = %M<%T>(%L)", Def.allocArray, Types.JValue, args.size + 1)
        .apply {
            addStatement("args[0].l = ref.%M()", Def.reinterpret)
            args.forEachIndexed { idx, arg ->
                val type = arg.typeInfo
                val argCode = CodeBlock.Companion.of("%N", arg.name).returnType(type.kotlinType).nonNullOrPlaceholder().copy(type = type.jniType.nativeType)
                val valueCode = type.packCode(argCode)
                val reinterpreted = if (type.jniType.jniField == "l") {
                    valueCode.nullSafeCall(
                        CodeBlock.Companion.of("%M()", Def.reinterpret).returnType(
                            ANY
                        ))
                } else valueCode
                addStatement("args[%L].%L = %L", idx + 1, type.jniType.jniField, reinterpreted.code)
            }
            args.filter { it.typeInfo.needsIsNullParam() }.mapIndexed { idx, arg ->
                val globalIdx = idx + args.size + 1
                addStatement("args[%L].z = (%N == null).%M()", globalIdx, arg.name, Types.Method.ToJBoolean)
            }
        }
        .add(innerCode)
        .endControlFlow()
        .build()
}