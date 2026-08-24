package com.lenyaplay.simple.timer.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenyaplay.simple.timer.TICK_INTERVAL_MS
import com.lenyaplay.simple.timer.TimerState
import com.lenyaplay.simple.timer.TimerUiState
import com.lenyaplay.simple.timer.ui.theme.TimerForKotlinLearningTheme
import kotlin.math.ceil

private val RING_MAX_SIZE = 320.dp
private val RING_WIDTH = 14.dp

/** Зазор между дугой и цифрами */
private val TEXT_INSET = 12.dp

/** Размер, на котором строка измеряется перед подгонкой */
private val PROBE_FONT_SIZE = 100.sp
private val MAX_FONT_SIZE = 56.sp

/**
 * Остаток округляется вверх: иначе заданное пользователем число исчезает с экрана сразу
 * после старта, а ноль показывается за секунду до сигнала
 */
fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = ceil(remainingMs.coerceAtLeast(0) / 1000.0).toLong()
    return "%02d:%02d:%02d".format(
        totalSeconds / 3600,
        (totalSeconds % 3600) / 60,
        totalSeconds % 60,
    )
}

@Composable
fun TimerCounter(modifier: Modifier = Modifier, state: TimerUiState) {
    val isPaused = state.state == TimerState.Paused

    val progress = if (state.totalDurationMs > 0) {
        state.remainingDurationMs.coerceAtLeast(0).toFloat() / state.totalDurationMs
    } else {
        0f
    }
    // Линейная анимация длиной ровно в один тик: новая цель приходит в момент, когда
    // предыдущая анимация закончилась, поэтому скорость дуги не меняется. Кривая с
    // замедлением или другая длительность дают серию рывков
    val animatedProgress = remember { Animatable(progress) }
    LaunchedEffect(progress, isPaused) {
        if (!isPaused) {
            animatedProgress.animateTo(
                targetValue = progress,
                animationSpec = tween(TICK_INTERVAL_MS, easing = LinearEasing),
            )
        }
        // На паузе ничего не делаем: смена ключа сама отменит текущий animateTo, и дуга
        // замрет там, где была - без snapTo, который дергал ее к цели рывком
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val arcColor = if (isPaused) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.primary
    }

    val textStyle = MaterialTheme.typography.displayLarge.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
    )
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val ringSize = minOf(maxWidth, maxHeight).coerceAtMost(RING_MAX_SIZE)

        // Размер подбирается измерением, а не коэффициентом "ширина символа ~ 0.6 em":
        // такие коэффициенты врут при смене шрифта и системного масштаба текста
        val fontSize: TextUnit = remember(ringSize, textStyle, density) {
            val probe = measurer.measure(
                "00:00:00",
                textStyle.copy(fontSize = PROBE_FONT_SIZE),
            )
            val available = with(density) {
                (ringSize - RING_WIDTH * 2 - TEXT_INSET * 2).toPx()
            }
            val scaled = PROBE_FONT_SIZE.value * available / probe.size.width
            scaled.coerceAtMost(MAX_FONT_SIZE.value).sp
        }

        if (state.totalDurationMs > 0) {
            Canvas(
                modifier = Modifier
                    .size(ringSize)
                    .testTag("ring")
            ) {
                val stroke = Stroke(width = RING_WIDTH.toPx(), cap = StrokeCap.Round)
                val inset = RING_WIDTH.toPx() / 2
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress.value * 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
            }
        }

        Text(
            text = formatRemaining(state.remainingDurationMs),
            style = textStyle,
            fontSize = fontSize,
            color = if (isPaused) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            modifier = Modifier.testTag("counter"),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TimerCounterRunningPreview() {
    TimerForKotlinLearningTheme {
        TimerCounter(
            state = TimerUiState(
                remainingDurationMs = 65_000,
                totalDurationMs = 90_000,
                state = TimerState.Running,
            )
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TimerCounterPausedPreview() {
    TimerForKotlinLearningTheme {
        TimerCounter(
            state = TimerUiState(
                remainingDurationMs = 65_000,
                totalDurationMs = 90_000,
                state = TimerState.Paused,
            )
        )
    }
}
