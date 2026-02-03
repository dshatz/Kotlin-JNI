package dev.datlag.nkommons

import dev.datlag.nkommons.binding.jobject
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed

@OptIn(ExperimentalForeignApi::class)

open class BaseCallback(
    val env: CPointer<JNIEnvVar>,
    instance: jobject
): Disposable {
    val ref: jobject = env.pointed.pointedCommon!!.NewGlobalRef!!.invoke(env, instance)!!

    override fun close() {
        env.pointed.pointedCommon!!.DeleteGlobalRef!!.invoke(env, ref)
    }
}