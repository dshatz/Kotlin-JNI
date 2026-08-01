package com.dshatz.kni

import com.dshatz.kni.model.ParamInfo
import com.google.devtools.ksp.symbol.KSValueParameter

abstract class BaseProcessor {
    abstract val mapper: TypeMapper

    protected fun List<KSValueParameter>.toTypeInfos(): List<ParamInfo> {
        return map {
            ParamInfo(it.name!!.asString(), mapper.mapType(it.type))
        }
    }
}