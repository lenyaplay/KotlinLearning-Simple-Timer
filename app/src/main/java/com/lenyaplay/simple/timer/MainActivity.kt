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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lenyaplay.simple.timer.ui.components.TimerCounterContent
import com.lenyaplay.simple.timer.ui.theme.TimerForKotlinLearningTheme
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Без разрешения уведомление не покажется", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun startTimerWithPermissionRequest(durationMillis: Long) {
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
                TimerView()
            }
        }
    }
}


@Composable
fun TimerView(vm: TimerInputState = viewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    TimerViewContent(
        hours = vm.hours,
        minutes = vm.minutes,
        seconds = vm.seconds,
        onStart = {
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
    Scaffold(modifier = Modifier.fillMaxSize(), floatingActionButton = {
        FloatingActionButton(onClick = { }) {
            Icon(Icons.Default.Add, contentDescription = "Добавить таймер")
        }
    }) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (timerUiState.isRunning)
                TimerCounterContent(state = timerUiState)
            else
                TimeInput(
                    modifier = Modifier.padding(innerPadding),
                    hours = hours,
                    minutes = minutes,
                    seconds = seconds,
                )
            Spacer(modifier = Modifier.height(16.dp))
            RunTimerButton(onStart = onStart)

        }
    }
}

@Composable
fun RunTimerButton(onStart: () -> Unit) {
    Button(onClick = onStart) {
        Text("Запустить таймер")
    }
}

@Preview(showBackground = false)
@Composable
fun RunTimerButtonPreview() {
    RunTimerButton(onStart = {})
}

@Preview(showBackground = true)
@Composable
fun TimerViewContentPreview() {
    TimerForKotlinLearningTheme {
        TimerViewContent(
            hours = TextFieldState(initialText = "1"),
            minutes = TextFieldState(initialText = "1"),
            seconds = TextFieldState(initialText = "12"),
            onStart = {},
            timerUiState = TimerUiState(remainingMs = 65000, totalMs = 65000, isRunning = false)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimerViewContentPreviewWithStated() {
    TimerForKotlinLearningTheme {
        TimerViewContent(
            hours = TextFieldState(initialText = "1"),
            minutes = TextFieldState(initialText = "1"),
            seconds = TextFieldState(initialText = "12"),
            onStart = {},
            timerUiState = TimerUiState(remainingMs = 65000, totalMs = 65000, isRunning = true)
        )
    }
}

