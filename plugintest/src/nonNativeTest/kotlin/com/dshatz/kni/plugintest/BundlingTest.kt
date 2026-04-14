package com.dshatz.kni.plugintest

import com.dshatz.kni.load.BundledLibLoader
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe

val BundlingTest by testSuite {
    test("Load library") {
        BundledLibLoader.loadBundledLibrary("plugintest")
    }

    test("Call method") {
        receiveHelloFromNative() shouldBe "Hello from Native"
    }
}

val PrebuiltBundlingTest by testSuite {
    test("Load bundled library") {
        BundledLibLoader.loadBundledLibrary("gif")
    }
}