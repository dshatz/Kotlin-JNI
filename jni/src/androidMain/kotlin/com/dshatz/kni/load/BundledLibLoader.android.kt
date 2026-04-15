package com.dshatz.kni.load

actual object BundledLibLoader {
    actual fun loadBundledLibrary(name: String) {
        System.loadLibrary(name)
    }
}