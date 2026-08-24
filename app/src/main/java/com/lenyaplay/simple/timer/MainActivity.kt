package com.lenyaplay.simple.timer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lenyaplay.simple.timer.ui.components.TimeInput
import com.lenyaplay.simple.timer.ui.components.TimePresets
import com.lenyaplay.simple.timer.ui.components.TimerCounter
import com.lenyaplay.simple.timer.ui.components.parseDurationMs
import com.lenyaplay.simple.timer.ui.theme.TimerForKotlinLearningTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.lenyaplay.simple.timer.ui.components.PauseTimerButton
import com.lenyaplay.simple.timer.ui.components.RunTimerButton
import com.lenyaplay.simple.timer.ui.components.StopTimerButton

class MainActivity : ComponentActivity() {
    private val notificationStepDone = mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Без разрешения уведомление не покажется", Toast.LENGTH_SHORT)
                .show()
        }
        notificationStepDone.value = true
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationStepDone.value = true
        }
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            TimerForKotlinLearningTheme {
                TimerView(
                    notificationStepDone = notificationStepDone.value,
                    openOverlaySettings = { openOverlaySettings() },
                )
            }
        }
    }
}


@Composable
fun TimerView(
    vm: TimerInputState = viewModel(),
    notificationStepDone: Boolean,
    openOverlaySettings: () -> Unit,
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showOverlayDialog by rememberSaveable { mutableStateOf(false) }

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
        hours = vm.hours,
        minutes = vm.minutes,
        seconds = vm.seconds,
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
        title = { Text(text = "Показывать таймер поверх приложений") },
        text = {
            Text(
                text = "Без этого разрешения, когда время выйдет и вы будете пользоваться " +
                        "телефоном, придёт только обычное уведомление. " +
                        "С ним таймер развернётся на весь экран."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = "Открыть настройки") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Не надо") }
        },
    )
}

@Composable
fun TimerViewContent(
    hours: TextFieldState,
    minutes: TextFieldState,
    seconds: TextFieldState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onResume: () -> Unit,
    timerUiState: TimerUiState,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
                    )
                    TimePresets(
                        hours = hours,
                        minutes = minutes,
                        seconds = seconds,
                    )
                }
                RunTimerButton(
                    onClick = onStart,
                    enabled = parseDurationMs(hours, minutes, seconds) > 0,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .align(Alignment.BottomCenter),
                )
            } else {
                TimerCounter(
                    modifier = Modifier.align(Alignment.Center),
                    state = timerUiState
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
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
            hours = TextFieldState(initialText = "1"),
            minutes = TextFieldState(initialText = "1"),
            seconds = TextFieldState(initialText = "12"),
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
            hours = TextFieldState(initialText = "1"),
            minutes = TextFieldState(initialText = "1"),
            seconds = TextFieldState(initialText = "12"),
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

