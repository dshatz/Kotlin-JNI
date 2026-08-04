package com.dshatz.kni.model

import com.dshatz.kni.kspfix.FunctionParent

interface WithClassParent: WithParent {
    override val parent: FunctionParent.Class
}