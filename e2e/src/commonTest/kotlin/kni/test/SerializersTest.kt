package kni.test

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import kni.test.serializable.Inner
import kni.test.serializable.PolymorphicFruit
import kni.test.serializable.PolymorphicFruitSerializer_generated
import kni.test.serializable.Simple
import kni.test.serializable.SimpleSerializer_generated
import kni.test.serializable.ValueClass
import kni.test.serializable.ValueClassSerializer_generated
import kotlin.random.Random

val SerializersTest by testSuite {

    test("latin") {
        val obj = ColorfulObject("green", 100.0, 1..2)

        val packed = ColorfulObjectSerializer_generated.pack(obj)
        ColorfulObjectSerializer_generated.unpackFrom(packed) shouldBe obj
    }

    test("cyrillic") {
        val obj = ColorfulObject("зелений", 200.0, 99..999)
        val packed = ColorfulObjectSerializer_generated.pack(obj)
        ColorfulObjectSerializer_generated.unpackFrom(packed) shouldBe obj
    }

    test("serialize deserialize") {
        val obj = Simple(
            0x20.toByte(),
            short = 10.toShort(),
            int = 10,
            long = 100,
            float = -200f,
            double = 100.0,
            "Hello",
            generateSequence { Random.nextInt().toString() }.take(3).toList(),
            generateSequence { Random.nextInt().toString() }.take(3).toSet(),
            mapOf("one" to listOf(1, 11, 21), "two" to listOf(2, 12, 22)),
            alias = 100,
            inner = Inner(0xAA.toByte()),
            result = Result.success("Success"),
            resultOfCustom = Result.success(Inner(0xff.toByte())),
            path = kotlinx.io.files.Path("/home/kni/Documents")
        )
        val packed = SimpleSerializer_generated.pack(obj)
        SimpleSerializer_generated.unpackFrom(packed) shouldBe obj
    }

    test("polymorphic") {
        PolymorphicFruitSerializer_generated.apply {
            val orange = PolymorphicFruit.Orange(true)
            val orangePacked = pack(orange)
            unpackFrom(orangePacked) shouldBe orange

            val apple = PolymorphicFruit.Apple(1000)
            val applePacked = pack(apple)
            unpackFrom(applePacked) shouldBe apple
        }
    }

    test("value class") {
        val valueClass = ValueClass(99)
        val packed = ValueClassSerializer_generated.pack(valueClass)
        ValueClassSerializer_generated.unpackFrom(packed) shouldBe valueClass
    }
}