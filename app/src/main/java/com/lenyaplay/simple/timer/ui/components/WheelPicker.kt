package com.lenyaplay.simple.timer.ui.components

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.lenyaplay.simple.timer.ui.theme.TimerForKotlinLearningTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.math.abs

@Composable
fun WheelPicker(
    value: Int,
    count: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    visibleItemCount: Int = 5,
    itemHeight: Dp = 48.dp,
) {
    // Список "бесконечный": индексы идут до Int.MAX_VALUE, а значение берется как остаток
    // от деления. Стартуем из середины и выравниваем так, чтобы по центру оказалось value
    val startIndex = remember(count) {
        val middle = Int.MAX_VALUE / 2
        middle - (middle % count) + value
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val view = LocalView.current

    LaunchedEffect(listState, count) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.isScrollInProgress }
            .filter { !it.second }
            .map { it.first.mod(count) }
            .distinctUntilChanged()
            .collect(onValueChange)
    }

    // Значение может поменяться снаружи (например кнопкой пресета) - доворачиваем барабан
    LaunchedEffect(value) {
        val currentIndex = listState.firstVisibleItemIndex
        if (currentIndex.mod(count) != value && !listState.isScrollInProgress) {
            var delta = (value - currentIndex.mod(count)).mod(count)
            if (delta > count / 2) delta -= count
            listState.animateScrollToItem(currentIndex + delta)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .drop(1)
            .collect { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = itemHeight * (visibleItemCount - 1) / 2),
        modifier = modifier
            .height(itemHeight * visibleItemCount)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            },
    ) {
        items(Int.MAX_VALUE) { index ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val distance = distanceFromCenter(
                            listState = listState,
                            index = index,
                            itemHeightPx = itemHeight.toPx(),
                        )
                        rotationX = distance * -30f
                        scaleX = 1f - abs(distance) * 0.1f
                        scaleY = scaleX
                        alpha = (1f - abs(distance) * 0.35f).coerceAtLeast(0f)
                        cameraDistance = 8 * density
                    },
            ) {
                Text(
                    text = index.mod(count).toString().padStart(2, '0'),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Расстояние от центра барабана до элемента [index] в высотах элемента. Дробное, поэтому
 * наклон и затухание меняются плавно прямо во время прокрутки.
 */
private fun distanceFromCenter(
    listState: androidx.compose.foundation.lazy.LazyListState,
    index: Int,
    itemHeightPx: Float,
): Float {
    val layoutInfo = listState.layoutInfo
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return 0f
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val itemCenter = item.offset + item.size / 2f
    return (itemCenter - viewportCenter) / itemHeightPx
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WheelPickerPreview() {
    TimerForKotlinLearningTheme {
        var value by remember { mutableIntStateOf(7) }

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            WheelPicker(
                value = value,
                count = 60,
                onValueChange = { value = it },
                modifier = Modifier.width(64.dp),
            )
            Text(
                text = "выбрано: $value",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
