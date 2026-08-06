package kni.test

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import kni.test.serializable.ColorEnum
import kni.test.serializable.ColorEnum_Serializer_generated
import kni.test.serializable.ColorfulObject
import kni.test.serializable.ColorfulObject_Serializer_generated
import kni.test.serializable.Inner
import kni.test.serializable.PolymorphicFruit
import kni.test.serializable.PolymorphicFruit_Serializer_generated
import kni.test.serializable.Simple
import kni.test.serializable.Simple_Serializer_generated
import kotlin.random.Random

val SerializersTest by testSuite {

    test("latin") {
        val obj = ColorfulObject("green", 100.0, 1..2)

        val packed = ColorfulObject_Serializer_generated.pack(obj)
        ColorfulObject_Serializer_generated.unpackFrom(packed) shouldBe obj
    }

    test("cyrillic") {
        val obj = ColorfulObject("зелений", 200.0, 99..999)
        val packed = ColorfulObject_Serializer_generated.pack(obj)
        ColorfulObject_Serializer_generated.unpackFrom(packed) shouldBe obj
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
            objs = listOf(ColorfulObject("black", 2.0, 10..20))
        )
        val packed = Simple_Serializer_generated.pack(obj)
        Simple_Serializer_generated.unpackFrom(packed) shouldBe obj
    }

    test("polymorphic") {
        PolymorphicFruit_Serializer_generated.apply {
            val orange = PolymorphicFruit.Orange(true)
            val orangePacked = pack(orange)
            unpackFrom(orangePacked) shouldBe orange

            val apple = PolymorphicFruit.Apple(1000)
            val applePacked = pack(apple)
            unpackFrom(applePacked) shouldBe apple

            val watermelon = PolymorphicFruit.Watermelon(Random.nextBytes(10), LongArray(10),
                CharArray(10))
            val watermelonPacked = pack(watermelon)
            unpackFrom(watermelonPacked) shouldBe watermelon
        }
    }

    test("enum") {
        ColorEnum_Serializer_generated.apply {
            val green = ColorEnum.GREEN
            val packed = pack(green)
            unpackFrom(packed) shouldBe green
        }
    }
}