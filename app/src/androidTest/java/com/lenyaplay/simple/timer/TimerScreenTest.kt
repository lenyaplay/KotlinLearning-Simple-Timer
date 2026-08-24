package com.lenyaplay.simple.timer

import android.view.ViewConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.lenyaplay.simple.timer.ui.components.TimerCounter
import com.lenyaplay.simple.timer.ui.theme.TimerForKotlinLearningTheme
import androidx.compose.ui.semantics.getOrNull
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TimerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var hours by mutableIntStateOf(0)
    private var minutes by mutableIntStateOf(0)
    private var seconds by mutableIntStateOf(15)
    private var syncKey by mutableIntStateOf(0)

    /** Все значения, о которых барабан секунд сообщил наружу, по порядку */
    private val reportedSeconds = mutableListOf<Int>()

    private fun setScreen(h: Int = 0, m: Int = 0, s: Int = 15) {
        hours = h
        minutes = m
        seconds = s
        syncKey = 0
        reportedSeconds.clear()
        composeRule.setContent {
            TimerForKotlinLearningTheme {
                TimerViewContent(
                    hours = hours,
                    minutes = minutes,
                    seconds = seconds,
                    onHoursChange = { hours = it },
                    onMinutesChange = { minutes = it },
                    onSecondsChange = {
                        seconds = it
                        reportedSeconds += it
                    },
                    onPresetClick = {
                        hours = 0
                        minutes = it
                        seconds = 0
                        syncKey++
                    },
                    syncKey = syncKey,
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onResume = {},
                    timerUiState = TimerUiState(state = TimerState.Idle),
                )
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * Значение считается выбранным, только если текст с ним физически стоит по центру
     * своего барабана. Спрашивать у компонента, что он думает, бессмысленно - расходятся
     * как раз показанное и сообщенное
     */
    /**
     * Значение считается выбранным, только если оно физически ближе всех к центру своего
     * барабана. Спрашивать у компонента, что он думает, бессмысленно: расходятся как раз
     * показанное и сообщенное. Сравнивать координаты напрямую нельзя - положение текста
     * искажено 3D-трансформацией, поэтому ищем ближайший к центру
     */
    private fun assertWheelShows(label: String, value: Int) {
        val wheelCenter = composeRule.onNodeWithContentDescription(label)
            .fetchSemanticsNode().boundsInRoot.center.y

        val texts = composeRule.onAllNodes(hasAnyAncestor(hasContentDescription(label)), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                val text = node.config.getOrNull(SemanticsProperties.Text)
                    ?.firstOrNull()?.text ?: return@mapNotNull null
                text to node.boundsInRoot.center.y
            }

        val closest = texts.minByOrNull { abs(it.second - wheelCenter) }
        assertEquals(
            "Барабан \"$label\" показывает по центру не то значение",
            value.toString().padStart(2, '0'),
            closest?.first,
        )
    }

    private fun itemHeightPx(): Float = with(composeRule.density) { 48.dp.toPx() }

    /** Часть жеста съедается touch slop, до него прокрутка вообще не начинается */
    private fun touchSlopPx(): Float = ViewConfiguration
        .get(InstrumentationRegistry.getInstrumentation().targetContext)
        .scaledTouchSlop
        .toFloat()

    @Test
    fun startValueIsShown() {
        setScreen(s = 15)

        assertWheelShows("Секунды", 15)
    }

    @Test
    fun presetScrollsAllWheels() {
        setScreen()

        composeRule.onNodeWithText("10 мин").performClick()
        composeRule.waitForIdle()

        assertWheelShows("Часы", 0)
        assertWheelShows("Минуты", 10)
        assertWheelShows("Секунды", 0)
    }

    @Test
    fun presetDuringInertiaScrollsAllWheels() {
        setScreen()
        val itemHeight = itemHeightPx()

        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithContentDescription("Минуты").performTouchInput {
            swipeUp(startY = center.y + 2 * itemHeight, endY = center.y - 2 * itemHeight)
        }
        // Несколько кадров инерции - барабан еще крутится
        composeRule.mainClock.advanceTimeBy(100)

        composeRule.onNodeWithText("10 мин").performClick()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertWheelShows("Часы", 0)
        assertWheelShows("Минуты", 10)
        assertWheelShows("Секунды", 0)
    }

    @Test
    fun swipeChangesShownValue() {
        setScreen(s = 15)
        val itemHeight = itemHeightPx()

        composeRule.onNodeWithContentDescription("Секунды").performTouchInput {
            slowDrag(dy = 3 * itemHeight + touchSlopPx())
        }
        composeRule.waitForIdle()

        assertWheelShows("Секунды", 18)
    }

    @Test
    fun interruptedGestureSettles() {
        setScreen(s = 15)
        val itemHeight = itemHeightPx()

        composeRule.onNodeWithContentDescription("Секунды").performTouchInput {
            slowDrag(dy = 0.6f * itemHeight + touchSlopPx())
        }
        composeRule.waitForIdle()

        assertWheelShows("Секунды", 16)
    }

    /** Небольшого сдвига за барьер достаточно, чтобы переключиться на следующее значение */
    @Test
    fun dragPastBarrierCommitsToNextValue() {
        setScreen(s = 15)
        val itemHeight = itemHeightPx()

        composeRule.onNodeWithContentDescription("Секунды").performTouchInput {
            slowDrag(dy = 0.3f * itemHeight + touchSlopPx())
        }
        composeRule.waitForIdle()

        assertWheelShows("Секунды", 16)
    }

    /** Сдвиг, не преодолевший барьер, возвращается назад */
    @Test
    fun dragBelowBarrierReturns() {
        setScreen(s = 15)
        val itemHeight = itemHeightPx()

        composeRule.onNodeWithContentDescription("Секунды").performTouchInput {
            slowDrag(dy = 0.1f * itemHeight + touchSlopPx())
        }
        composeRule.waitForIdle()

        assertWheelShows("Секунды", 15)
    }

    /** Барьер работает симметрично в обратную сторону */
    @Test
    fun dragBackPastBarrierCommitsToPreviousValue() {
        setScreen(s = 15)
        val itemHeight = itemHeightPx()

        composeRule.onNodeWithContentDescription("Секунды").performTouchInput {
            slowDrag(dy = -(0.3f * itemHeight + touchSlopPx()))
        }
        composeRule.waitForIdle()

        assertWheelShows("Секунды", 14)
    }

    /** Очень медленная прокрутка: за кадр набегает доля пикселя, барабан все равно доворачивает */
    @Test
    fun slowDragCommitsToNextValue() {
        setScreen(s = 15)
        val itemHeight = itemHeightPx()

        composeRule.onNodeWithContentDescription("Секунды").performTouchInput {
            slowDrag(dy = 0.4f * itemHeight + touchSlopPx(), steps = 60, stepDelayMs = 16)
        }
        composeRule.waitForIdle()

        assertWheelShows("Секунды", 16)
    }

    /**
     * Палец отрывается за пределами барабана, и сразу после этого - клик по пресету,
     * пока барабан еще не докрутился
     */
    @Test
    fun presetRightAfterDragEndingOutsideWheelApplies() {
        setScreen()
        val itemHeight = itemHeightPx()

        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithContentDescription("Минуты").performTouchInput {
            down(center)
            advanceEventTime(32)
            moveTo(center + Offset(0f, -1.4f * itemHeight))
            advanceEventTime(32)
            // Уводим палец выше барабана и отпускаем уже за его пределами
            moveTo(Offset(center.x, -200f))
            advanceEventTime(32)
            up()
        }
        // Совсем немного времени - барабан еще в движении
        composeRule.mainClock.advanceTimeBy(32)

        composeRule.onNodeWithText("10 мин").performClick()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertWheelShows("Часы", 0)
        assertWheelShows("Минуты", 10)
        assertWheelShows("Секунды", 0)
    }

    /** Быстрые нажатия по всем пресетам подряд: побеждает последний */
    @Test
    fun rapidPresetClicksSelectLast() {
        setScreen()

        composeRule.mainClock.autoAdvance = false
        listOf("1 мин", "3 мин", "5 мин", "10 мин", "15 мин").forEach { chip ->
            composeRule.onNodeWithText(chip).performClick()
            composeRule.mainClock.advanceTimeBy(16)
        }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertWheelShows("Часы", 0)
        assertWheelShows("Минуты", 15)
        assertWheelShows("Секунды", 0)
        assertEquals(15, minutes)
    }

    /** Бросок должен проматывать заметно больше, чем прошел палец */
    @Test
    fun flingScrollsFurtherThanFinger() {
        setScreen(s = 0)
        val itemHeight = itemHeightPx()

        val node = composeRule.onNodeWithContentDescription("Секунды")
        val height = node.fetchSemanticsNode().size.height
        node.performTouchInput {
            swipeUp(startY = height - 1f, endY = 1f, durationMillis = 100)
        }
        composeRule.waitForIdle()

        // Палец прошел примерно высоту барабана - пять элементов. С инерцией должно
        // проматывать ощутимо дальше
        val travelled = (height / itemHeight).toInt()
        assertTrue(
            "Бросок промотал всего $seconds элементов, ожидалось больше $travelled",
            seconds > travelled,
        )
    }

    /**
     * Барабан не должен мотаться туда-обратно после броска: при прокрутке вверх значения
     * обязаны только расти. Разворот в обратную сторону - это и есть ощущение пружины
     */
    @Test
    fun flingDoesNotOscillate() {
        setScreen(s = 0)

        val node = composeRule.onNodeWithContentDescription("Секунды")
        val height = node.fetchSemanticsNode().size.height
        node.performTouchInput {
            swipeUp(startY = height - 1f, endY = 1f, durationMillis = 100)
        }
        composeRule.waitForIdle()

        val reversals = reportedSeconds.zipWithNext()
            .map { (previous, next) -> circularDelta(previous, next, 60) }
            .filter { it < 0 }

        assertTrue(
            "Значения разворачивались назад ${reversals.size} раз: $reportedSeconds",
            reversals.isEmpty(),
        )
    }

    /**
     * Пресет прерывает доворот барабана. После этого барабан обязан остаться рабочим:
     * прерванная прокрутка - это отмена одной анимации, а не всего механизма выравнивания
     */
    @Test
    fun wheelStillSettlesAfterPresetInterruptedSettle() {
        setScreen()
        val itemHeight = itemHeightPx()

        // Короткий сдвиг минут: сразу за ним начнется доворот
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithContentDescription("Минуты").performTouchInput {
            slowDrag(dy = 0.4f * itemHeight + touchSlopPx())
        }
        composeRule.mainClock.advanceTimeBy(16)

        // Прерываем доворот пресетом
        composeRule.onNodeWithText("10 мин").performClick()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertWheelShows("Минуты", 10)

        // Барабан должен продолжать выравниваться после прерывания
        composeRule.onNodeWithContentDescription("Минуты").performTouchInput {
            slowDrag(dy = 0.4f * itemHeight + touchSlopPx())
        }
        composeRule.waitForIdle()

        assertWheelShows("Минуты", 11)
    }

    /** Пресет - это действие, а не состояние: при ручной прокрутке чипы не подсвечиваются */
    @Test
    fun presetChipsDoNotReactToManualScrolling() {
        setScreen(m = 4)
        val itemHeight = itemHeightPx()

        composeRule.onNodeWithContentDescription("Минуты").performTouchInput {
            slowDrag(dy = itemHeight + touchSlopPx())
        }
        composeRule.waitForIdle()

        assertWheelShows("Минуты", 5)
        composeRule.onNodeWithText("5 мин")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
    }

    /** Цифры счетчика должны помещаться внутрь кольца, а не вылезать за дугу */
    @Test
    fun counterTextFitsInsideRing() {
        composeRule.setContent {
            TimerForKotlinLearningTheme {
                TimerCounter(
                    state = TimerUiState(
                        remainingDurationMs = 86_399_000,
                        totalDurationMs = 86_400_000,
                        state = TimerState.Running,
                    )
                )
            }
        }
        composeRule.waitForIdle()

        val ring = composeRule.onNodeWithTag("ring").fetchSemanticsNode().size
        val text = composeRule.onNodeWithTag("counter").fetchSemanticsNode().size
        val ringWidth = with(composeRule.density) { 14.dp.toPx() }

        assertTrue(
            "Текст шириной ${text.width} не помещается в кольцо шириной ${ring.width}",
            text.width <= ring.width - 2 * ringWidth,
        )
    }

    /**
     * Бросок должен доезжать до значения, на котором остановился снаппинг, и не
     * откатываться назад к предыдущему
     */
    @Test
    fun flingSettlesWithoutPullBack() {
        setScreen(s = 15)

        composeRule.onNodeWithContentDescription("Секунды").performTouchInput {
            swipeUp(startY = bottom - 1f, endY = top + 1f)
        }
        composeRule.waitForIdle()

        // Показанное на экране и то, что уйдет в таймер, обязаны совпадать
        assertWheelShows("Секунды", seconds)
    }
}

/**
 * Медленное перетаскивание без броска: проверяем итоговое положение барабана,
 * а не физику полета. dy > 0 - тянем вверх, значения растут
 */
private fun TouchInjectionScope.slowDrag(dy: Float, steps: Int = 10, stepDelayMs: Long = 32) {
    val from = center
    down(from)
    repeat(steps) { step ->
        advanceEventTime(stepDelayMs)
        moveTo(from + Offset(0f, -dy * (step + 1) / steps))
    }
    advanceEventTime(300)
    up()
}

/** Шаг между значениями с учетом зацикливания: с 59 на 0 это +1, а не -59 */
private fun circularDelta(previous: Int, next: Int, count: Int): Int =
    ((next - previous + count + count / 2) % count) - count / 2
