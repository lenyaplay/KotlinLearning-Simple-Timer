package com.lenyaplay.simple.timer

import android.os.SystemClock
import android.util.Log

const val TRACE_TAG = "SimpleTimer"

/** Единственный переключатель трассировки. Работает в любой сборке, включая release */
const val TRACE_ENABLED = false

/**
 * Событие подсистемы [subsystem] в лог. Сообщение передается лямбдой: при выключенной
 * трассировке строка не собирается, а константа позволяет компилятору выбросить и сам
 * вызов, и весь код внутри него
 */
inline fun trace(subsystem: String, message: () -> String) {
    if (!TRACE_ENABLED) return
    Log.i(TRACE_TAG, "[${SystemClock.uptimeMillis()}] $subsystem: ${message()}")
}
