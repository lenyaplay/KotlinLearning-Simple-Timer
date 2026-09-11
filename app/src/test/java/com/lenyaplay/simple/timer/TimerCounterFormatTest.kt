package com.lenyaplay.simple.timer

import com.lenyaplay.simple.timer.ui.components.formatRemaining
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerCounterFormatTest {

    /**
     * Остаток округляется вверх: заданное пользователем число должно держаться всю первую
     * секунду. С округлением вниз таймер на 15 секунд почти сразу показывал 14
     */
    @Test
    fun roundsUpSoStartValueIsVisible() {
        assertEquals("00:00:15", formatRemaining(15_000))
        assertEquals("00:00:15", formatRemaining(14_999))
        assertEquals("00:00:15", formatRemaining(14_001))
        assertEquals("00:00:14", formatRemaining(14_000))
    }

    /** Ноль появляется только когда время действительно вышло */
    @Test
    fun showsZeroOnlyWhenFinished() {
        assertEquals("00:00:01", formatRemaining(1))
        assertEquals("00:00:00", formatRemaining(0))
        assertEquals("00:00:00", formatRemaining(-500))
    }

    @Test
    fun formatsHoursAndMinutes() {
        assertEquals("01:01:05", formatRemaining(3_665_000))
        assertEquals("23:59:59", formatRemaining(86_399_000))
    }
}
