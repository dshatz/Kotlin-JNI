/*
package com.dshatz.kni

import com.dshatz.kni.model.KSCallback
import com.dshatz.kni.model.KSFun
import com.dshatz.kni.model.ParamInfo
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

object DeclarationAnalyzer {

    */
/*fun analyze(registry: Registry) {

    }*//*

}


data class AnalysisResult(
    val files: Set<AFile>
)


data class AFile(
    val target: FileTarget,
    val classes: Set<AClass>,
    val funs: Set<AFun>
) {
    enum class FileTarget {
        Common,
        Native,
        Jvm
    }
}

sealed class AClass {
    abstract val funs: Set<AFun>
    abstract val superType: TypeName

    data class NativeCallbackImpl(
        val type: TypeName,
    ): AClass() {
        override val superType: TypeName = Types.BaseCallback
    }
}

sealed class AFun {
    abstract val name: String
    abstract val params: List<ParamInfo>

    sealed class Resolved: AFun() {
        abstract val returnType: TypeName

        data class CallbackFun(
            override val name: String,
            override val params: List<ParamInfo>,
            override val returnType: TypeName,
        ): Resolved()
    }

    sealed class Produced {
        abstract val returnType: TypeInfo

        data class NativeCallbackFun(
            override val returnType: TypeInfo,
        ): Produced()
    }

}*/
