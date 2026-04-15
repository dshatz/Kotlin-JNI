package com.dshatz.kni

import com.dshatz.kni.binding.jint
import platform.android.JNI_ABORT
import platform.android.JNI_COMMIT

actual val JNI_COMMIT: jint = JNI_COMMIT
actual val JNI_ABORT: jint = JNI_ABORT