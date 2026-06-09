package com.dshatz.kni.error

class JniUnavailableError: IllegalStateException("JNI environment is unavailable")