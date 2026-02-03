import dev.datlag.nkommons.JNIConnect

@JNIConnect(
    packageName = "your.package.name",
    className = "YourClass",

    // optional, defaults to function name (example).
    functionName = "customFunction"
)
fun example(a: String, b: Boolean, c: CharArray, d: Double): String {
    return "$a, $b, $c, $d"
}