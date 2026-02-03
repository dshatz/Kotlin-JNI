import dev.datlag.nkommons.JNIEnvVar
import dev.datlag.nkommons.binding.jboolean
import dev.datlag.nkommons.binding.jcharArray
import dev.datlag.nkommons.binding.jdouble
import dev.datlag.nkommons.binding.jobject
import dev.datlag.nkommons.binding.jstring
import dev.datlag.nkommons.utils.toJString
import dev.datlag.nkommons.utils.toKBoolean
import dev.datlag.nkommons.utils.toKCharArray
import dev.datlag.nkommons.utils.toKString
import kotlin.OptIn
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("Java_your_package_name_YourClass_customFunction")
public fun _exampleJNI(
    env: CPointer<JNIEnvVar>,
    clazz: jobject,
    p0: jstring,
    p1: jboolean,
    p2: jcharArray,
    p3: jdouble,
): jstring? {
    return example(
        p0.toKString(env) ?: return null,
        p1.toKBoolean(),
        p2.toKCharArray(env) ?: return null,
        p3
    ).toJString(env)
}