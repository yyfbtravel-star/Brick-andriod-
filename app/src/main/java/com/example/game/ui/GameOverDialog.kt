package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.GameStatus
import com.example.game.model.GameUiState
import com.example.ui.theme.ImmersiveArenaBg
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

@Composable
fun GameOverOverlay(
    uiState: GameUiState,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.status != GameStatus.WON &&
        uiState.status != GameStatus.LOST &&
        uiState.status != GameStatus.PAUSED
    ) {
        return
    }

    val isWin = uiState.status == GameStatus.WON
    val isLoss = uiState.status == GameStatus.LOST
    val isPaused = uiState.status == GameStatus.PAUSED

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(initialScale = 0.88f)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ImmersiveBottomBar
                ),
                border = BorderStroke(1.dp, ImmersiveBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Status Icon
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isWin -> ImmersivePrimary.copy(alpha = 0.2f)
                                    isLoss -> ImmersiveTertiary.copy(alpha = 0.2f)
                                    else -> ImmersiveSecondary.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isWin -> Icons.Default.EmojiEvents
                                isLoss -> Icons.Default.HeartBroken
                                else -> Icons.Default.PauseCircle
                            },
                            contentDescription = when {
                                isWin -> "Win Trophy"
                                isLoss -> "Game Over"
                                else -> "Game Paused"
                            },
                            tint = when {
                                isWin -> ImmersivePrimary
                                isLoss -> ImmersiveTertiary
                                else -> ImmersiveSecondary
                            },
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = when {
                            isWin -> "VICTORY!"
                            isLoss -> "GAME OVER"
                            else -> "PAUSED"
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = when {
                            isWin -> ImmersivePrimary
                            isLoss -> ImmersiveTertiary
                            else -> ImmersiveTextPrimary
                        },
                        textAlign = TextAlign.Center
                    )

                    // Subtitle
                    Text(
                        text = when {
                            isWin -> "All bricks shattered! Exceptional precision!"
                            isLoss -> "The ball slipped past your paddle."
                            else -> "Game is paused. Ready when you are."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ImmersiveTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Score Card Box
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ImmersiveArenaBg,
                        border = BorderStroke(1.dp, ImmersiveBorder.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Final Score",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ImmersiveTextMuted
                                )
                                Text(
                                    text = String.format("%,d", uiState.score),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ImmersivePrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "High Score",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ImmersiveTextMuted
                                )
                                Text(
                                    text = String.format("%,d", uiState.highScore),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ImmersiveSecondary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Bricks Cleared",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ImmersiveTextMuted
                                )
                                Text(
                                    text = "${uiState.destroyedBricks} / ${uiState.totalBricks}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ImmersiveTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    if (isPaused) {
                        Button(
                            onClick = onResume,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("resume_button"),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ImmersivePrimaryContainer,
                                contentColor = Color(0xFFEADDFF)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "RESUME GAME",
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = onRestart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag(if (isWin) "play_again_button" else "restart_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isWin) ImmersivePrimary else ImmersivePrimaryContainer,
                            contentColor = if (isWin) Color(0xFF381E72) else Color(0xFFEADDFF)
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isWin) "PLAY AGAIN" else "RESTART GAME",
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

