import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.binding.jboolean
import com.dshatz.kni.binding.jcharArray
import com.dshatz.kni.binding.jdouble
import com.dshatz.kni.binding.jobject
import com.dshatz.kni.binding.jstring
import com.dshatz.kni.utils.toJString
import com.dshatz.kni.utils.toKBoolean
import com.dshatz.kni.utils.toKCharArray
import com.dshatz.kni.utils.toKString
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