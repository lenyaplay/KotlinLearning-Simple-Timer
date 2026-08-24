package com.lenyaplay.simple.timer.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenyaplay.simple.timer.ui.theme.TimerForKotlinLearningTheme
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val PRESETS_IN_MINUTES = listOf(1, 3, 5, 10, 15)

private val ITEM_HEIGHT = 48.dp
private val LABEL_HEIGHT = 16.dp
private val PRESETS_MAX_WIDTH = 260.dp
private const val VISIBLE_ITEM_COUNT = 5

fun parseDurationMs(hours: Int, minutes: Int, seconds: Int): Long =
    (hours.hours + minutes.minutes + seconds.seconds).inWholeMilliseconds

@Composable
private fun TimeSeparator() {
    Text(
        text = ":",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
    )
}

/**
 * Барабан с подписью. Сверху добавляется отступ высотой с подпись, иначе колонка окажется
 * выше барабана и цифры уедут вверх относительно выделения центральной строки.
 */
@Composable
private fun LabeledWheel(
    value: Int,
    count: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    contentDescription: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(LABEL_HEIGHT))
        WheelPicker(
            value = value,
            count = count,
            onValueChange = onValueChange,
            contentDescription = contentDescription,
            itemHeight = ITEM_HEIGHT,
            visibleItemCount = VISIBLE_ITEM_COUNT,
            modifier = Modifier.width(64.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(LABEL_HEIGHT),
        )
    }
}

@Composable
fun TimeInput(
    modifier: Modifier = Modifier,
    hours: Int,
    minutes: Int,
    seconds: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Выделение центральной строки - по ширине блока барабанов.
        // matchParentSize задает жесткие размеры, поэтому высота задается уже внутри
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ITEM_HEIGHT),
            ) {}
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            LabeledWheel(
                value = hours,
                count = 24,
                onValueChange = onHoursChange,
                label = "ч",
                contentDescription = "Часы",
            )
            TimeSeparator()
            LabeledWheel(
                value = minutes,
                count = 60,
                onValueChange = onMinutesChange,
                label = "мин",
                contentDescription = "Минуты",
            )
            TimeSeparator()
            LabeledWheel(
                value = seconds,
                count = 60,
                onValueChange = onSecondsChange,
                label = "с",
                contentDescription = "Секунды",
            )
        }

        // Дальние элементы барабанов растворяются в фоне экрана
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to backgroundColor,
                        0.35f to backgroundColor.copy(alpha = 0f),
                        0.65f to backgroundColor.copy(alpha = 0f),
                        1f to backgroundColor,
                    )
                )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimePresets(
    modifier: Modifier = Modifier,
    hours: Int,
    minutes: Int,
    seconds: Int,
    onPresetClick: (minutes: Int) -> Unit,
) {
    FlowRow(
        modifier = modifier.widthIn(max = PRESETS_MAX_WIDTH),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PRESETS_IN_MINUTES.forEach { preset ->
            FilterChip(
                selected = hours == 0 && minutes == preset && seconds == 0,
                onClick = { onPresetClick(preset) },
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
        var hours by remember { mutableIntStateOf(0) }
        var minutes by remember { mutableIntStateOf(5) }
        var seconds by remember { mutableIntStateOf(0) }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            TimeInput(
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                onHoursChange = { hours = it },
                onMinutesChange = { minutes = it },
                onSecondsChange = { seconds = it },
            )
            TimePresets(
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                onPresetClick = {
                    hours = 0
                    minutes = it
                    seconds = 0
                },
            )
        }
    }
}
