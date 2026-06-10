package com.omnimiko.common.log

import android.util.Log

/**
 * Thin logging facade so modules don't depend on android.util.Log directly and
 * so a future file/in-app log sink can be swapped in for debugging agent runs.
 */
object OmniLog {
    var enabled: Boolean = true

    fun d(tag: String, message: String) {
        if (enabled) Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        if (enabled) Log.i(tag, message)
    }

    fun w(tag: String, message: String, t: Throwable? = null) {
        if (enabled) Log.w(tag, message, t)
    }

    fun e(tag: String, message: String, t: Throwable? = null) {
        if (enabled) Log.e(tag, message, t)
    }
}
