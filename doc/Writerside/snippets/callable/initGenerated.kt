import dev.datlag.nkommons.JNIEnvVar
import dev.datlag.nkommons.binding.jobject
import kotlin.OptIn
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("Java_com_example_MainKt_init")
public fun _initJNI(
    env: CPointer<JNIEnvVar>,
    clazz: jobject,
    p0: jobject,
) {
    // FORCING BODY
    return `init`(dev.datlag.nkommons._JvmCallbackNativeImpl(env, p0))
}
