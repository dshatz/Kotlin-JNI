package dev.datlag.nkommons

import dev.datlag.nkommons.binding.jobject
import dev.datlag.nkommons.utils.CallObjectMethodA
import dev.datlag.nkommons.utils.FindClass
import dev.datlag.nkommons.utils.GetMethodID
import dev.datlag.nkommons.utils.toKString
import kotlin.OptIn
import kotlin.String
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.`get`
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped

public class _JvmCallbackNativeImpl(
    env: CPointer<JNIEnvVar>,
    instance: jobject,
) : BaseCallback(env, "dev/datlag/nkommons/JvmCallback", instance),
    JvmCallback {

    override fun sayHello(): String {
        val cls = jvmClass
        val methodId = env.GetMethodID(cls, "sayHello", "()Ljava/lang/String;")!!
        return memScoped {
            val args = allocArray<jvalue>(0)
            env.CallObjectMethodA(ref, methodId, args)?.toKString(env)!!
        }
    }
}
