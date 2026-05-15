import com.dshatz.kni.CNameUtils
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals

val cNameTesting = testSuite {
    test("ascii name not escaped") {
        val name = "helloWorld"
        val escaped = CNameUtils.escapePart(name)

        escaped shouldBe name
    }

    test("underscore escaped") {
        val name = "hello_world"
        val escaped = CNameUtils.escapePart(name)

        escaped shouldBe "hello_1world"
    }

    test("underscore escaped") {
        val name = "hello_world"
        val escaped = CNameUtils.escapePart(name)

        escaped shouldBe "hello_1world"
    }

    test("semicolon escaped") {
        val name = "hello;world"
        val escaped = CNameUtils.escapePart(name)

        escaped shouldBe "hello_2world"
    }
    
    test("bracket escaped") {
        val name = "hello[world]"
        val escaped = CNameUtils.escapePart(name)

        escaped shouldBe "hello_3world]"
    }

    test("unicode character escaped") {
        val name = "ヘlloWorld"
        val escaped = CNameUtils.escapePart(name)
        val expected = "_030d8lloWorld"

        escaped shouldBe "_030d8lloWorld"
    }

    test("ascii jni cname without class name") {
        val packageName = "test.example"
        val functionName = "helloWorld"
        val cname = CNameUtils.jniFunctionCName(
            packageName = packageName,
            className = null,
            functionName = functionName
        )

        cname shouldBe "Java_test_example_helloWorld"
    }

    test("ascii jni cname with class name") {
        val packageName = "test.example"
        val className = "JNI"
        val functionName = "helloWorld"
        val cname = CNameUtils.jniFunctionCName(
            packageName = packageName,
            className = className,
            functionName = functionName
        )

        cname shouldBe "Java_test_example_JNI_helloWorld"
    }
}