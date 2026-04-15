import com.dshatz.kni.CNameUtils
import kotlin.test.Test
import kotlin.test.assertEquals

class CNameTesting {

    @Test
    fun `ascii name not escaped`() {
        val name = "helloWorld"
        val escaped = CNameUtils.escapePart(name)

        assertEquals(name, escaped)
    }

    @Test
    fun `underscore escaped`() {
        val name = "hello_world"
        val escaped = CNameUtils.escapePart(name)
        val expected = "hello_1world"

        assertEquals(expected, escaped)
    }

    @Test
    fun `semicolon escaped`() {
        val name = "hello;world"
        val escaped = CNameUtils.escapePart(name)
        val expected = "hello_2world"

        assertEquals(expected, escaped)
    }

    @Test
    fun `bracket escaped`() {
        val name = "hello[world]"
        val escaped = CNameUtils.escapePart(name)
        val expected = "hello_3world]"

        assertEquals(expected, escaped)
    }

    @Test
    fun `unicode character escaped`() {
        val name = "ヘlloWorld"
        val escaped = CNameUtils.escapePart(name)
        val expected = "_030d8lloWorld"

        assertEquals(expected, escaped)
    }

    @Test
    fun `ascii jni cname without class name`() {
        val packageName = "test.example"
        val functionName = "helloWorld"
        val cname = CNameUtils.jniFunctionCName(
            packageName = packageName,
            className = null,
            functionName = functionName
        )
        val expected = "Java_test_example_helloWorld"

        assertEquals(expected, cname)
    }

    @Test
    fun `ascii jni cname with class name`() {
        val packageName = "test.example"
        val className = "JNI"
        val functionName = "helloWorld"
        val cname = CNameUtils.jniFunctionCName(
            packageName = packageName,
            className = className,
            functionName = functionName
        )
        val expected = "Java_test_example_JNI_helloWorld"

        assertEquals(expected, cname)
    }
}