package com.lenyaplay.simple.timer.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lenyaplay.simple.timer.R
import com.lenyaplay.simple.timer.trace
import com.lenyaplay.simple.timer.data.TimerState
import com.lenyaplay.simple.timer.data.TimerUiState
import com.lenyaplay.simple.timer.ui.components.PauseTimerButton
import com.lenyaplay.simple.timer.ui.components.RunTimerButton
import com.lenyaplay.simple.timer.ui.components.StopTimerButton
import com.lenyaplay.simple.timer.ui.components.TimeInput
import com.lenyaplay.simple.timer.ui.components.TimePresets
import com.lenyaplay.simple.timer.ui.components.TimerCounter
import com.lenyaplay.simple.timer.ui.components.parseDurationMs
import com.lenyaplay.simple.timer.ui.theme.TimerForKotlinLearningTheme

@Composable
fun TimerView(
    vm: TimerViewModel = viewModel(factory = timerViewModelFactory(LocalContext.current)),
    notificationStepDone: Boolean,
    openOverlaySettings: () -> Unit,
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showOverlayDialog by rememberSaveable { mutableStateOf(false) }

    // Показываем экран завершения сами, не дожидаясь Alarm - у него минимальная задержка
    // доставки ~5 сек (MIN_FUTURITY системы), а тикер точен. Подписка активна только пока
    // экран реально на переднем плане: иначе можно запустить Activity из фона и потерять
    // подстраховку через Alarm, отменив его раньше, чем экран правда показался
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(vm, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.timerFinishedEvents.collect {
                context.startActivity(timerFinishedActivityIntent(context))
                vm.onTimerFinishedScreenShown()
            }
        }
    }

    LifecycleResumeEffect(notificationStepDone) {
        if (notificationStepDone &&
            !Settings.canDrawOverlays(context) &&
            !vm.overlayPermissionDeclined &&
            !vm.overlayAsked
        ) {
            vm.overlayAsked = true
            showOverlayDialog = true
        }
        onPauseOrDispose { }
    }

    if (showOverlayDialog) {
        OverlayPermissionDialog(
            onConfirm = {
                showOverlayDialog = false
                openOverlaySettings()
            },
            onDismiss = {
                showOverlayDialog = false
                vm.overlayPermissionDeclined = true
            },
        )
    }

    TimerViewContent(
        hours = vm.inputValues.hours,
        minutes = vm.inputValues.minutes,
        seconds = vm.inputValues.seconds,
        onHoursChange = { vm.inputValues = vm.inputValues.copy(hours = it) },
        onMinutesChange = { vm.inputValues = vm.inputValues.copy(minutes = it) },
        onSecondsChange = { vm.inputValues = vm.inputValues.copy(seconds = it) },
        onPresetClick = { vm.applyPreset(it) },
        syncKey = vm.presetVersion,
        onStart = {
            vm.onStartClick()
        },
        onPause = {
            vm.onPauseClick()
        },
        onStop = {
            vm.onStopClick()
        },
        onResume = {
            vm.onResumeClick()
        },
        timerUiState = uiState,
    )
}


@Composable
fun OverlayPermissionDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.overlay_permission_dialog_title)) },
        text = { Text(text = stringResource(R.string.overlay_permission_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.overlay_permission_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.overlay_permission_dismiss_button))
            }
        },
    )
}

private fun currentAppLanguage(context: Context): String =
    if (context.storedLanguage() == "ru") "ru" else "en"

private fun toggleAppLanguage(context: Context) {
    val next = if (currentAppLanguage(context) == "ru") "en" else "ru"
    trace("Язык") { "переключение ${currentAppLanguage(context)} -> $next" }
    context.setStoredLanguage(next)
    trace("Язык") { "после setStoredLanguage: ${context.storedLanguage()}" }
    val activity = context.findActivity()
    trace("Язык") { "activity для recreate: $activity" }
    activity?.recreate()
}

@OptIn(ExperimentalMaterial3Api::class)
// innerPadding намеренно не применяется - контент центрируется по всему экрану,
// а не по области под панелью, см. комментарий у Scaffold ниже
@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TimerViewContent(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    onPresetClick: (Int) -> Unit,
    syncKey: Int = 0,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onResume: () -> Unit,
    timerUiState: TimerUiState,
) {
    val context = LocalContext.current
    val languageSwitchDescription = stringResource(R.string.language_switch_content_description)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.top_bar_title)) },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(text = stringResource(R.string.language_code_en))
                        Switch(
                            checked = currentAppLanguage(context) == "ru",
                            onCheckedChange = { toggleAppLanguage(context) },
                            colors = SwitchDefaults.colors(
                                uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                uncheckedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedBorderColor = MaterialTheme.colorScheme.primary,
                                uncheckedIconColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.semantics {
                                contentDescription = languageSwitchDescription
                            },
                        )
                        Text(text = stringResource(R.string.language_code_ru))
                    }
                },
            )
        },
    ) {
        // Центрируем по всему экрану, а не по области под панелью: иначе центр
        // содержимого смещается вниз на половину высоты панели и это заметно на глаз
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (timerUiState.state == TimerState.Idle) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    TimeInput(
                        hours = hours,
                        minutes = minutes,
                        seconds = seconds,
                        onHoursChange = onHoursChange,
                        onMinutesChange = onMinutesChange,
                        onSecondsChange = onSecondsChange,
                        syncKey = syncKey,
                    )
                    TimePresets(onPresetClick = onPresetClick)
                }
                RunTimerButton(
                    onClick = onStart,
                    enabled = parseDurationMs(hours, minutes, seconds) > 0,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp),
                )
            } else {
                TimerCounter(
                    modifier = Modifier.align(Alignment.Center),
                    state = timerUiState
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                ) {
                    if (timerUiState.state == TimerState.Paused)
                        RunTimerButton(onClick = onResume)
                    else
                        PauseTimerButton(onClick = onPause)
                    Spacer(modifier = Modifier.width(16.dp))
                    StopTimerButton(onClick = onStop)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RunningTimerViewContentPreview() {
    TimerForKotlinLearningTheme {
        TimerViewContent(
            hours = 1,
            minutes = 1,
            seconds = 12,
            onHoursChange = {},
            onMinutesChange = {},
            onSecondsChange = {},
            onPresetClick = {},
            onStart = {},
            onStop = {},
            onPause = {},
            onResume = {},
            timerUiState = TimerUiState(
                remainingDurationMs = 65000,
                totalDurationMs = 65000,
                state = TimerState.Running
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun IdleTimerViewContentPreview() {
    TimerForKotlinLearningTheme {
        TimerViewContent(
            hours = 1,
            minutes = 1,
            seconds = 12,
            onHoursChange = {},
            onMinutesChange = {},
            onSecondsChange = {},
            onPresetClick = {},
            onStart = {},
            onStop = {},
            onPause = {},
            onResume = {},
            timerUiState = TimerUiState(
                remainingDurationMs = 65000,
                totalDurationMs = 65000,
                state = TimerState.Idle
            )
        )
    }
}
