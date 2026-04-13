import com.dshatz.kni.utils.*
import com.dshatz.kni.JNIEnvVar
import com.dshatz.kni.binding.jstring
import com.dshatz.kni.binding.jintArray
import com.dshatz.kni.binding.jbyteArray
import kotlinx.cinterop.CPointer

val jMessage: jstring = ...
val kotlinString: String? = jMessage.toKString(env)
require(jMessage == kotlinString?.toJString(env))

val jInts: jintArray = ...
val kInts = jInts.toKIntArray(env)
require(jInts == kInts?.toJIntArray(env))

val jBytes: jbyteArray = ...
val kBytes: ByteArray? = jBytes.toKByteArray(env)
require(jBytes == kBytes?.toJByteArray(env))