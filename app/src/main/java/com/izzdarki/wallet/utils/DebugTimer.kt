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
    private var startMillis: Long = -1

    companion object {
        protected var count = 0
    }

    init {
        if (BuildConfig.DEBUG) {
            startMillis = System.currentTimeMillis()
            count++
        }
    }

    @JvmOverloads
    fun end(text: String? = null) {
        if (BuildConfig.DEBUG) {
            val endMillis = System.currentTimeMillis()
            Log.d(
                "timing",
                indentation + name + ": " + (endMillis - startMillis).toInt() + "ms" + if (text != null && text != "") " ($text)" else ""
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