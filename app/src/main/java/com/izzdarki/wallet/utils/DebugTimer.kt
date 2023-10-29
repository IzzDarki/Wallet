package com.izzdarki.wallet.utils

import android.util.Log
import izzdarki.wallet.BuildConfig

fun <T> withTiming(name: String, block: () -> T): T {
    val timer = DebugTimer(name)
    val result = block()
    timer.end()
    return result
}

open class DebugTimer(protected var name: String) {
    private var startNanos: Long = -1
    private var startMillis: Long = -1

    companion object {
        protected var count = 0
    }

    init {
        if (BuildConfig.DEBUG) {
            startNanos = System.nanoTime()
            startMillis = System.currentTimeMillis()
            count++
        }
    }

    @JvmOverloads
    fun end(text: String? = null) {
        if (BuildConfig.DEBUG) {
            // Check if nanos are ok
            val nanosPassed = System.nanoTime() - startNanos
            val millisPassed = System.currentTimeMillis() - startMillis
            val timeMessage = if (millisPassed >= Long.MAX_VALUE / 1000000) {
                // At this point, the nanos have overflowed
                "${millisPassed}ms"
            } else {
                "${nanosPassed.toDouble() / 1000000}ms"
            }
            Log.d(
                "timing",
                indentation + name + ": " + timeMessage + if (text != null && text != "") " ($text)" else ""
            )
            count--
        }
    }

    private val indentation: String
        get() {
            val indentation = StringBuilder()
            for (i in 0 until count) indentation.append("\t")
            return indentation.toString()
        }
}