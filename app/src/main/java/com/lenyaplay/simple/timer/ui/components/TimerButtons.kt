package com.lenyaplay.simple.timer.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

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