package com.example.game.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.GameStatus
import com.example.game.model.GameUiState
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersiveBorder
import com.example.ui.theme.ImmersiveBottomBar
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersivePrimaryContainer
import com.example.ui.theme.ImmersiveSecondary
import com.example.ui.theme.ImmersiveTertiary
import com.example.ui.theme.ImmersiveTextMuted
import com.example.ui.theme.ImmersiveTextPrimary

/**
 * Top Immersive Header matching the Design HTML:
 * - Score label in #D0BCFF, uppercase, large tabular-nums font in #E6E1E5
 * - Level/Bricks indicator on right in #CCC2DC
 */
@Composable
fun ImmersiveTopHeader(
    uiState: GameUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag("score_display"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Score Section
        Column {
            Text(
                text = "SCORE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                ),
                color = ImmersivePrimary.copy(alpha = 0.8f)
            )
            AnimatedContent(
                targetState = uiState.score,
                transitionSpec = {
                    slideInVertically { height -> height } togetherWith
                            slideOutVertically { height -> -height }
                },
                label = "score_anim"
            ) { score ->
                Text(
                    text = String.format("%05d", score),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp
                    ),
                    color = ImmersiveTextPrimary
                )
            }
        }

        // Level / Bricks Section
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "BRICKS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                ),
                color = ImmersiveSecondary.copy(alpha = 0.8f)
            )
            Text(
                text = String.format("%02d", uiState.remainingBricks),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 22.sp
                ),
                color = if (uiState.remainingBricks <= 5) ImmersiveTertiary else ImmersiveTextPrimary
            )
        }
    }
}

/**
 * Bottom Control Bar / Sheet matching the Design HTML:
 * - Rounded top container (#211F26)
 * - Handle indicator
 * - Touch & Drag track with arrows
 * - Pause and Restart action buttons (#4F378B / #938F99)
 * - Status footer: Lives left (#EFB8C8 indicator) and High Score
 */
@Composable
fun ImmersiveBottomControls(
    uiState: GameUiState,
    onPaddleDrag: (Float) -> Unit,
    onLaunch: () -> Unit,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ImmersiveBottomBar,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        border = BorderStroke(1.dp, ImmersiveBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Drag Handle Indicator
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(ImmersiveBorder.copy(alpha = 0.6f))
            )

            // Touch Drag Track for Paddle Control
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ImmersiveBg)
                    .border(BorderStroke(1.dp, ImmersiveBorder), RoundedCornerShape(16.dp))
                    .pointerInput(uiState.status) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (uiState.status == GameStatus.READY) {
                                onLaunch()
                            }
                            drag(down.id) { change ->
                                change.consume()
                                val delta = change.position.x - change.previousPosition.x
                                onPaddleDrag(delta)
                            }
                        }
                    }
                    .testTag("paddle_drag_track"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "←",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DRAG TO MOVE PADDLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 11.sp
                        ),
                        color = ImmersivePrimary
                    )
                    Text(
                        text = "→",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume Pill Button
                Button(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pause_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersivePrimaryContainer,
                        contentColor = Color(0xFFEADDFF)
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.status == GameStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (uiState.status == GameStatus.PAUSED) "Resume" else "Pause",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.status == GameStatus.PAUSED) "Resume" else "Pause",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Restart Round Button
                IconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(BorderStroke(1.dp, ImmersiveOutline), CircleShape)
                        .testTag("restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = ImmersivePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Bottom Info Bar (Lives & High Score)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("lives_display"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lives Indicator with Immersive Rose dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (uiState.lives > 0) ImmersiveTertiary else Color.Gray)
                    )
                    Text(
                        text = "${uiState.lives} LIVES LEFT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = ImmersiveTextMuted
                    )
                }

                // High Score Indicator
                Text(
                    text = "HIGH: ${String.format("%,d", uiState.highScore)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = ImmersiveTextMuted
                )
            }
        }
    }
}

/**
 * Backward compatibility HUD component combining top and bottom bars if used together.
 */
@Composable
fun GameHud(
    uiState: GameUiState,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    ImmersiveTopHeader(
        uiState = uiState,
        modifier = modifier
    )
}

