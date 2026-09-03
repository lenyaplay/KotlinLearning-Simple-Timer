package com.lenyaplay.simple.timer.ui.components

import android.content.res.Configuration
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.MutatePriority
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenyaplay.simple.timer.trace
import com.lenyaplay.simple.timer.ui.theme.TimerForKotlinLearningTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs

private const val SETTLE_TOLERANCE_PX = 1f

/** Сколько раз пытаться довернуть барабан, если доворот прерывают */
private const val SETTLE_ATTEMPTS = 3

/** Какую долю высоты элемента надо пройти, чтобы барабан переключился на соседний */
private const val BARRIER = 0.2f

/** Во сколько раз бросок летит дальше обычного списка */
private const val FLING_VELOCITY_MULTIPLIER = 2f

/** Длительность мягкой посадки на значение после броска */
private const val SETTLE_DURATION_MS = 300


@Composable
fun WheelPicker(
    value: Int,
    count: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    syncKey: Int = 0,
    contentDescription: String? = null,
    visibleItemCount: Int = 5,
    itemHeight: Dp = 48.dp,
) {
    // Список "бесконечный": индексы идут до Int.MAX_VALUE, а значение берется как остаток
    // от деления. Стартуем из середины, чтобы крутить можно было в обе стороны
    val startIndex = remember(count) {
        val middle = Int.MAX_VALUE / 2
        middle - (middle % count) + value
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val view = LocalView.current
    val subsystem = contentDescription ?: "Барабан"

    // Пока барабан сам доворачивает до заданного снаружи значения, промежуточные числа
    // наружу не сообщаются: иначе они затирают только что заданную цель
    var isSyncing by remember { mutableStateOf(false) }

    // Эффекты живут дольше одной композиции (ключи listState/count и syncKey меняются не
    // при каждой рекомпозиции), поэтому без rememberUpdatedState они звали бы устаревшую
    // лямбду, замкнутую в момент запуска эффекта
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    // Текущее значение уходит наружу сразу по мере прокрутки, а не после остановки:
    // иначе "Старт" во время докрутки взял бы предыдущее число
    LaunchedEffect(listState, count) {
        var isFirst = true
        snapshotFlow { listState.centeredValue(count) }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { centered ->
                trace(subsystem) { "report $centered" }
                if (!isSyncing) currentOnValueChange(centered)
                // Вибрация - отклик на действие пользователя, при появлении экрана ее быть
                // не должно
                if (isFirst) {
                    isFirst = false
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
    }

    // Куда встать после остановки, решает барьер: сдвинулся больше чем на BARRIER в
    // сторону соседнего элемента - переключаемся на него, меньше - возвращаемся назад.
    // Направление берется за весь жест целиком, а не по кадрам: при медленной прокрутке
    // за кадр набегает доля пикселя, и покадровое направление получалось случайным
    LaunchedEffect(listState, itemHeightPx) {
        var anchor: Pair<Int, Int>? = null
        snapshotFlow { listState.isScrollInProgress }
            .collect { inProgress ->
                if (inProgress) {
                    anchor = listState.scrollAnchor()
                    trace(subsystem) {
                        "scroll start value=${listState.centeredValue(count)} " +
                                "fraction=${listState.centerFraction(itemHeightPx)}"
                    }
                    return@collect
                }

                val start = anchor ?: return@collect
                val moved = listState.movedSince(start, itemHeightPx)

                // Прерванный доворот повторяется: прервать его может тот, кто сам ничего
                // не прокручивает (например stopScroll), и тогда события "прокрутка
                // закончилась" больше не придет - барабан так и останется между значениями
                repeat(SETTLE_ATTEMPTS) { attempt ->
                    val fraction = listState.centerFraction(itemHeightPx) ?: return@collect
                    val direction = if (moved >= 0) 1 else -1
                    val step = step(fraction, direction)
                    val distance = (step - fraction) * itemHeightPx

                    if (attempt == 0) {
                        trace(subsystem) {
                            "scroll end value=${listState.centeredValue(count)} moved=$moved " +
                                    "fraction=$fraction dir=$direction step=$step distance=$distance"
                        }
                        // Знак доворота против направления жеста - это ощущение пружины
                        if (abs(moved) > SETTLE_TOLERANCE_PX && distance * moved < 0) {
                            trace(subsystem) { "WARN pull-back distance=$distance moved=$moved" }
                        }
                    }

                    // Прокрутка идет в целых пикселях, точного нуля не бывает. Без допуска
                    // барабан бесконечно доворачивает сам себя и экран не простаивает
                    if (abs(distance) < SETTLE_TOLERANCE_PX) return@collect

                    val settleStart = SystemClock.uptimeMillis()
                    try {
                        listState.animateScrollBy(distance, SETTLE_ANIMATION)
                    } catch (interrupted: CancellationException) {
                        // Отмена прокрутки, а не всего обработчика: без ensureActive
                        // CancellationException убила бы корутину
                        currentCoroutineContext().ensureActive()
                        trace(subsystem) { "settle interrupted: ${interrupted.message}" }

                        // Если прервавший сам крутит барабан, он позовет нас снова, когда
                        // закончит. Если нет - выравниваться придется самим
                        if (listState.isScrollInProgress) return@collect
                        return@repeat
                    }

                    val left = listState.centerFraction(itemHeightPx)
                    trace(subsystem) {
                        "settle done in ${SystemClock.uptimeMillis() - settleStart}ms " +
                                "value=${listState.centeredValue(count)} fraction=$left"
                    }
                    if (left != null && abs(left * itemHeightPx) > SETTLE_TOLERANCE_PX) {
                        trace(subsystem) { "WARN misaligned fraction=$left" }
                    }
                    return@collect
                }

                trace(subsystem) { "WARN settle gave up after $SETTLE_ATTEMPTS attempts" }
            }
    }

    // Доворот выполняется только по явной команде снаружи (смена syncKey), а не по смене
    // value. Барабан непрерывно сообщает значения наружу, они возвращаются обратно с
    // задержкой, и устаревшее эхо неотличимо от команды - барабан начинал мотаться
    // между двумя значениями
    val targetValue by rememberUpdatedState(value)
    LaunchedEffect(syncKey) {
        if (syncKey == 0) return@LaunchedEffect

        val target = targetValue
        val current = listState.centeredValue(count)
        trace(subsystem) { "sync request key=$syncKey target=$target centered=$current" }
        if (current == null || current == target) {
            trace(subsystem) { "sync skipped: уже на месте" }
            return@LaunchedEffect
        }

        // Инерционная докрутка идет с приоритетом UserInput и отменила бы прокрутку
        // с обычным приоритетом, поэтому сначала останавливаем ее
        listState.stopScroll(MutatePriority.PreventUserInput)

        // Положение читается уже после остановки: за время инерции барабан уехал
        val fromValue = listState.centeredValue(count) ?: return@LaunchedEffect
        var delta = (target - fromValue).mod(count)
        if (delta > count / 2) delta -= count

        trace(subsystem) { "sync $fromValue -> $target delta=$delta" }
        isSyncing = true
        try {
            listState.animateScrollBy(
                value = delta * itemHeightPx + listState.centerError(),
                animationSpec = SETTLE_ANIMATION,
            )
        } finally {
            isSyncing = false
            currentOnValueChange(listState.centeredValue(count) ?: target)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberWheelFlingBehavior(),
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
                        val distance = listState.distanceFromCenter(index, itemHeightPx)
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
 * Куда переключиться относительно ближайшего элемента: +1, -1 или остаться на месте.
 * В отличие от округления к ближайшему, барьер считается в направлении движения -
 * достаточно немного зайти на соседний элемент, а не преодолевать половину высоты
 */
private fun step(fraction: Float, direction: Int): Int = when {
    direction >= 0 && fraction >= BARRIER -> 1
    direction < 0 && fraction <= -BARRIER -> -1
    else -> 0
}

/** Плавная посадка на значение: скорость гасится к концу, без пружинного подрагивания */
private val SETTLE_ANIMATION = tween<Float>(
    durationMillis = SETTLE_DURATION_MS,
    easing = FastOutSlowInEasing,
)

/**
 * Бросок с увеличенной инерцией. Кривая торможения остается платформенной - меняется
 * только начальная скорость, поэтому ощущение замедления к концу привычное
 */
@Composable
private fun rememberWheelFlingBehavior(): FlingBehavior {
    val delegate = ScrollableDefaults.flingBehavior()
    return remember(delegate) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float =
                with(delegate) {
                    performFling(initialVelocity * FLING_VELOCITY_MULTIPLIER)
                }
        }
    }
}

private fun LazyListState.scrollAnchor(): Pair<Int, Int> =
    firstVisibleItemIndex to firstVisibleItemScrollOffset

/**
 * Сколько пикселей прокручено с момента [anchor]. Считается по индексу и смещению, а не по
 * абсолютной позиции: разница индексов мала и в пиксели переводится без потери точности
 */
private fun LazyListState.movedSince(anchor: Pair<Int, Int>, itemHeightPx: Float): Float {
    val (anchorIndex, anchorOffset) = anchor
    return (firstVisibleItemIndex - anchorIndex) * itemHeightPx +
            (firstVisibleItemScrollOffset - anchorOffset)
}

private fun LazyListState.viewportCenter(): Float =
    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f

/**
 * Элемент, чей центр ближе всего к центру барабана. Считается по фактической геометрии:
 * `firstVisibleItemIndex` для этого не годится - его смысл зависит от contentPadding,
 * числа видимых элементов и близости к краям списка.
 */
private fun LazyListState.centeredItem(): LazyListItemInfo? {
    val center = viewportCenter()
    return layoutInfo.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }
}

private fun LazyListState.centeredValue(count: Int): Int? = centeredItem()?.index?.mod(count)

/**
 * Насколько ближайший к центру элемент смещен относительно центра, в долях своей высоты:
 * от -0.5 до 0.5. Абсолютный индекс здесь не участвует намеренно - он порядка миллиарда,
 * и во Float дробная часть такого числа теряется полностью
 */
private fun LazyListState.centerFraction(itemHeightPx: Float): Float? {
    val item = centeredItem() ?: return null
    return (viewportCenter() - (item.offset + item.size / 2f)) / itemHeightPx
}

/**
 * На сколько пикселей нужно прокрутить, чтобы ближайший к центру элемент встал ровно
 * по центру. Доворот всегда получается к ближайшему значению.
 */
private fun LazyListState.centerError(): Float {
    val item = centeredItem() ?: return 0f
    return item.offset + item.size / 2f - viewportCenter()
}

/**
 * Расстояние от центра барабана до элемента [index] в высотах элемента. Дробное, поэтому
 * наклон и затухание меняются плавно прямо во время прокрутки.
 */
private fun LazyListState.distanceFromCenter(index: Int, itemHeightPx: Float): Float {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return 0f
    return (item.offset + item.size / 2f - viewportCenter()) / itemHeightPx
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
