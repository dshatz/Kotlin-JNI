package com.dshatz.kni.serialization

fun parseNativeStack(nativeLines: List<String>): Array<StackTraceElement> {
    return nativeLines.map { line ->
        val regex = """(.+)\.(.+)\((.+):(\d+)\)""".toRegex()
        val match = regex.find(line)

        if (match != null) {
            val (className, methodName, fileName, lineNumber) = match.destructured
            StackTraceElement(className, methodName, fileName, lineNumber.toInt())
        } else {
            // Fallback for lines that don't match (like raw addresses)
            StackTraceElement("Native", line, null, -1)
        }
    }.toTypedArray()
}