import dev.datlag.nkommons.utils.*
import dev.datlag.nkommons.JNIEnvVar
import dev.datlag.nkommons.binding.jstring
import dev.datlag.nkommons.binding.jintArray
import dev.datlag.nkommons.binding.jbyteArray
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