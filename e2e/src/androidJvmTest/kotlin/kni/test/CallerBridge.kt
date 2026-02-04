package kni.test

import java.nio.ByteBuffer


class CallerBridge {
    external fun init(caller: JvmCaller)
    external fun dispose()
    external fun askJvmForANumber(): Int
    external fun askJvmToFillBuffer(buffer: ByteBuffer): String
    external fun sendTypeAlias(alias: TestAlias): TestAlias
}