package kni.test

import com.dshatz.kni.serialization.pack
import com.dshatz.kni.serialization.unpack
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe

val SerializersTest by testSuite {

    test("latin") {
        val obj = ColorfulObject("green", 100.0)

        val packed = ColorfulObjectSerializer.pack(obj)
        ColorfulObjectSerializer.unpack(packed) shouldBe obj
    }

    test("cyrillic") {
        val obj = ColorfulObject("зелений", 200.0)
        val packed = ColorfulObjectSerializer.pack(obj)
        ColorfulObjectSerializer.unpack(packed) shouldBe obj
    }
}