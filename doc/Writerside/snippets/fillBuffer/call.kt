// commonMain
import com.dshatz.kni.buffers.allocateBuffer

val buffer = allocateBuffer(1024)
val filledHex = fillBuffer(buffer) // call native
val contents = buffer.readToByteArray(0, 1024) // read contents
// filledHex == contents.toHexString()