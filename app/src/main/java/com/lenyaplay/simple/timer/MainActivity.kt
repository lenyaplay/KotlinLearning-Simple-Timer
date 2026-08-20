package com.lenyaplay.simple.timer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lenyaplay.simple.timer.ui.components.TimerCounterContent
import com.lenyaplay.simple.timer.ui.theme.TimerForKotlinLearningTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.lenyaplay.simple.timer.ui.components.PauseTimerButton
import com.lenyaplay.simple.timer.ui.components.RunTimerButton
import com.lenyaplay.simple.timer.ui.components.StopTimerButton

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Без разрешения уведомление не покажется", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TimerForKotlinLearningTheme {
                TimerView(requestPermission = { requestNotificationPermissionIfNeeded() })
            }
        }
    }
}


@Composable
fun TimerView(vm: TimerInputState = viewModel(), requestPermission: () -> Unit) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    TimerViewContent(
        hours = vm.hours,
        minutes = vm.minutes,
        seconds = vm.seconds,
        onStart = {
            requestPermission()
            vm.onStartClick()
        },
        timerUiState = uiState,
    )
}


@Composable
fun TimerViewContent(
    hours: TextFieldState,
    minutes: TextFieldState,
    seconds: TextFieldState,
    onStart: () -> Unit,
    timerUiState: TimerUiState,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (timerUiState.state == TimerState.Running) {
                TimerCounterContent(
                    modifier = Modifier.align(Alignment.Center),
                    state = timerUiState
                )
                Row(modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)) {
                    PauseTimerButton(onClick = onStart)
                    Spacer(modifier = Modifier.width(16.dp))
                    StopTimerButton(onClick = onStart)
                }
            } else {
                TimeInput(
                    modifier = Modifier.align(Alignment.Center),
                    hours = hours,
                    minutes = minutes,
                    seconds = seconds,
                )
                RunTimerButton(
                    onClick = onStart,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .align(Alignment.BottomCenter),
                )

            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimerViewContentPreviewStarted() {
    TimerForKotlinLearningTheme {
        TimerViewContent(
            hours = TextFieldState(initialText = "1"),
            minutes = TextFieldState(initialText = "1"),
            seconds = TextFieldState(initialText = "12"),
            onStart = {},
            timerUiState = TimerUiState(
                remainingMs = 65000,
                totalMs = 65000,
                state = TimerState.Running
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimerViewContentPreviewWithNotStarted() {
    TimerForKotlinLearningTheme {
        TimerViewContent(
            hours = TextFieldState(initialText = "1"),
            minutes = TextFieldState(initialText = "1"),
            seconds = TextFieldState(initialText = "12"),
            onStart = {},
            timerUiState = TimerUiState(
                remainingMs = 65000,
                totalMs = 65000,
                state = TimerState.Finished
            )
        )
    }
}

