package com.lenyaplay.simple.timer.data

interface AlarmScheduler {
    fun schedule(afterMs: Long)
    fun cancel()
}
