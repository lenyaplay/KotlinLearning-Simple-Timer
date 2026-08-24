package com.lenyaplay.simple.timer.ui.components


import MaterialIconsPause
import MaterialIconsPlayArrow
import MaterialIconsStop
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun RunTimerButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val background = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    }

    Box(
        modifier = modifier
            .size(76.dp)
            .background(background, CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = MaterialIconsPlayArrow,
            contentDescription = "Запустить таймер",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(52.dp)
        )
    }
}


@Composable
fun PauseTimerButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(76.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = MaterialIconsPause,
            contentDescription = "Приостановить таймер",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(52.dp)
        )
    }
}

@Composable
fun StopTimerButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(76.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = MaterialIconsStop,
            contentDescription = "Остановить таймер",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(52.dp)
        )
    }
}


@Preview(showBackground = true)
@Composable
fun RunTimerButtonPreview() {
    RunTimerButton(onClick = {})
}


@Preview(showBackground = true)
@Composable
fun PauseTimerButtonPreview() {
    PauseTimerButton(onClick = {})
}


@Preview(showBackground = true)
@Composable
fun StopTimerButtonPreview() {
    StopTimerButton(onClick = {})
}