package com.lenyaplay.simple.timer.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lenyaplay.simple.timer.TimerUiState
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun TimerCounter(modifier: Modifier = Modifier, state: TimerUiState) {
    val duration = state.remainingDurationMs.milliseconds

    val hours = duration.inWholeHours
    val minutes = duration.inWholeMinutes % 60
    val seconds = duration.inWholeSeconds % 60

    val textModifier = Modifier
        .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outline)
        .padding(2.dp)

    Row(modifier = modifier) {
        Text(
            text = hours.toString().padStart(2, '0'),
            modifier = textModifier
        )
        Text(
            text = minutes.toString().padStart(2, '0'),
            modifier = textModifier
        )
        Text(
            text = seconds.toString().padStart(2, '0'),
            modifier = textModifier
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimerCounterPreview() {
    TimerCounter(
        state = TimerUiState(
            remainingDurationMs = 65000, totalDurationMs = 65000
        )
    )
}