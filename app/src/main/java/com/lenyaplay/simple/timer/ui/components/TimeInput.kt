package com.lenyaplay.simple.timer.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenyaplay.simple.timer.ui.theme.TimerForKotlinLearningTheme
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val PRESETS_IN_MINUTES = listOf(1, 3, 5, 10, 15)

fun parseDurationMs(
    hours: TextFieldState,
    minutes: TextFieldState,
    seconds: TextFieldState,
): Long {
    val h = hours.text.toString().toLongOrNull() ?: 0L
    val m = minutes.text.toString().toLongOrNull() ?: 0L
    val s = seconds.text.toString().toLongOrNull() ?: 0L
    return (h.hours + m.minutes + s.seconds).inWholeMilliseconds
}

private class TwoDigitRange(private val max: Int) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val text = asCharSequence().toString()
        if (text.isEmpty()) return

        val isValid = text.length <= 2 &&
                text.all { it.isDigit() } &&
                (text.toIntOrNull() ?: (max + 1)) <= max

        if (!isValid) revertAllChanges()
    }
}

@Composable
private fun TimePart(
    state: TextFieldState,
    label: String,
    max: Int,
    imeAction: ImeAction,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(
            state = state,
            textStyle = TextStyle(
                color = if (isFocused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                textAlign = TextAlign.Center,
            ),
            inputTransformation = TwoDigitRange(max),
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = imeAction,
            ),
            modifier = Modifier
                .width(72.dp)
                .onFocusChanged { isFocused = it.isFocused }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimeSeparator() {
    Text(
        text = ":",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
fun TimeInput(
    modifier: Modifier = Modifier,
    hours: TextFieldState,
    minutes: TextFieldState,
    seconds: TextFieldState,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimePart(
                state = hours,
                label = "ч",
                max = 99,
                imeAction = ImeAction.Next,
            )
            TimeSeparator()
            TimePart(
                state = minutes,
                label = "мин",
                max = 59,
                imeAction = ImeAction.Next,
            )
            TimeSeparator()
            TimePart(
                state = seconds,
                label = "с",
                max = 59,
                imeAction = ImeAction.Done,
            )
        }
    }
}

@Composable
fun TimePresets(
    modifier: Modifier = Modifier,
    hours: TextFieldState,
    minutes: TextFieldState,
    seconds: TextFieldState,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PRESETS_IN_MINUTES.forEach { preset ->
            val presetText = preset.toString().padStart(2, '0')
            FilterChip(
                selected = minutes.text.toString() == presetText &&
                        hours.text.toString() == "00" &&
                        seconds.text.toString() == "00",
                onClick = {
                    hours.setTextAndPlaceCursorAtEnd("00")
                    minutes.setTextAndPlaceCursorAtEnd(presetText)
                    seconds.setTextAndPlaceCursorAtEnd("00")
                },
                label = { Text(text = "$preset мин") },
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TimeInputPreview() {
    TimerForKotlinLearningTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            val hours = TextFieldState(initialText = "00")
            val minutes = TextFieldState(initialText = "05")
            val seconds = TextFieldState(initialText = "00")

            TimeInput(hours = hours, minutes = minutes, seconds = seconds)
            TimePresets(hours = hours, minutes = minutes, seconds = seconds)
        }
    }
}
