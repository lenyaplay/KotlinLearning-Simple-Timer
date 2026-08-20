package com.lenyaplay.simple.timer;

import android.content.SharedPreferences

internal class TimerSettings(prefs: SharedPreferences) {
    var startTimeUtcInMs: Long by prefs.long("startTimeInMs", -1L)
    var delayInMs: Long by prefs.long("delayInMs", -1L)
}
