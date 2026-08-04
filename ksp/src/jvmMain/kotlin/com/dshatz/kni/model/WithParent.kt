package com.dshatz.kni.model

import com.dshatz.kni.kspfix.FunctionParent

interface WithParent {
    val parent: FunctionParent
}