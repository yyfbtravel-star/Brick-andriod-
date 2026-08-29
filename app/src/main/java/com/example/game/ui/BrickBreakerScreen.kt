package com.example.game.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.GameStatus
import com.example.game.model.GameUiState
import com.example.game.viewmodel.GameViewModel
import com.example.ui.theme.ImmersiveArenaBg
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersiveBorder
import com.example.ui.theme.ImmersiveBottomBar
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveTextPrimary

@Composable
fun BrickBreakerScreen(
    viewModel: GameViewModel,
    uiState: GameUiState,
    modifier: Modifier = Modifier
) {
    // 60fps Game Loop
    var previousTimeNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(uiState.status) {
        if (uiState.status == GameStatus.PLAYING) {
            previousTimeNanos = 0L
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    if (previousTimeNanos != 0L) {
                        val deltaSeconds = (frameTimeNanos - previousTimeNanos) / 1_000_000_000f
                        viewModel.updateGame(deltaSeconds)
                    }
                    previousTimeNanos = frameTimeNanos
                }
            }
        }
    }

    // Pulse animation for the "Tap to Launch" banner
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val launchBannerPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Immersive Top Header (Score & Bricks)
            ImmersiveTopHeader(uiState = uiState)

            // Play Arena Card Container matching Design HTML:
            // flex-1 relative mx-4 mb-4 bg-[#2B2930] rounded-3xl border border-[#49454F] shadow-2xl overflow-hidden
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                val arenaWidth = constraints.maxWidth.toFloat()
                val arenaHeight = constraints.maxHeight.toFloat()

                LaunchedEffect(arenaWidth, arenaHeight) {
                    if (arenaWidth > 0 && arenaHeight > 0) {
                        viewModel.initArena(arenaWidth, arenaHeight)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(ImmersiveArenaBg)
                        .border(BorderStroke(1.dp, ImmersiveBorder), RoundedCornerShape(28.dp))
                        .graphicsLayer {
                            translationX = uiState.screenShakeOffsetX
                            translationY = uiState.screenShakeOffsetY
                        }
                        .pointerInput(uiState.status) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                viewModel.onPaddleMoveTo(down.position.x)

                                if (uiState.status == GameStatus.READY) {
                                    viewModel.launchBall()
                                }

                                drag(down.id) { change ->
                                    change.consume()
                                    val dragDelta = change.position.x - change.previousPosition.x
                                    viewModel.onPaddleDrag(dragDelta)
                                }
                            }
                        }
                        .testTag("game_canvas")
                ) {
                    // Custom Canvas with Radial Gradient and Game Entities
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Ambient radial glow matching HTML:
                        // bg-[radial-gradient(circle_at_50%_50%,#D0BCFF_0%,transparent_70%)]
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ImmersivePrimary.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * 0.75f
                            )
                        )

                        drawArcadeGrid()
                        drawBricks(uiState)
                        drawBallTrails(uiState)
                        drawParticles(uiState)
                        drawPaddle(uiState)
                        drawBall(uiState)
                    }

                    // Ready Cue Overlay ("Tap or Drag to launch")
                    if (uiState.status == GameStatus.READY) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = ImmersiveBottomBar.copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, ImmersivePrimary),
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = launchBannerPulse
                                        scaleY = launchBannerPulse
                                    }
                                    .testTag("launch_cue")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Swipe,
                                        contentDescription = null,
                                        tint = ImmersivePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "DRAG OR TAP TO LAUNCH",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = ImmersiveTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Control Sheet with Touch Strip, Action Buttons, and Status
            ImmersiveBottomControls(
                uiState = uiState,
                onPaddleDrag = { delta -> viewModel.onPaddleDrag(delta) },
                onLaunch = { viewModel.launchBall() },
                onTogglePause = { viewModel.togglePause() },
                onRestart = { viewModel.restartGame() }
            )
        }

        // Win / Loss / Pause dialog
        GameOverOverlay(
            uiState = uiState,
            onResume = { viewModel.togglePause() },
            onRestart = { viewModel.restartGame() }
        )
    }
}

private fun DrawScope.drawArcadeGrid() {
    val step = 44.dp.toPx()
    val gridColor = ImmersivePrimary.copy(alpha = 0.035f)

    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += step
    }

    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += step
    }
}

private fun DrawScope.drawBricks(state: GameUiState) {
    state.bricks.forEach { brick ->
        if (!brick.isDestroyed) {
            val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            val topLeft = Offset(brick.left, brick.top)
            val brickSize = Size(brick.width, brick.height)

            // Outer Soft Glow matching Design HTML: shadow-[0_0_10px_rgba(...,0.4)]
            drawRoundRect(
                color = brick.color.copy(alpha = 0.35f),
                topLeft = Offset(brick.left - 2f, brick.top - 2f),
                size = Size(brick.width + 4f, brick.height + 4f),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Brick Body Gradient
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        brick.highlightColor,
                        brick.color
                    ),
                    startY = brick.top,
                    endY = brick.bottom
                ),
                topLeft = topLeft,
                size = brickSize,
                cornerRadius = cornerRadius
            )

            // Top Sheen Line
            drawLine(
                color = Color(0x66FFFFFF),
                start = Offset(brick.left + 3f, brick.top + 2f),
                end = Offset(brick.right - 3f, brick.top + 2f),
                strokeWidth = 1.5f
            )

            // Brick Subtle Border
            drawRoundRect(
                color = Color(0x33000000),
                topLeft = topLeft,
                size = brickSize,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1f)
            )
        }
    }
}

private fun DrawScope.drawBallTrails(state: GameUiState) {
    state.ballTrails.forEach { trail ->
        drawCircle(
            color = ImmersivePrimary.copy(alpha = trail.alpha * 0.35f),
            radius = trail.radius,
            center = Offset(trail.x, trail.y)
        )
    }
}

private fun DrawScope.drawParticles(state: GameUiState) {
    state.particles.forEach { p ->
        drawCircle(
            color = p.color.copy(alpha = p.alpha),
            radius = p.size * p.alpha,
            center = Offset(p.x, p.y)
        )
    }
}

private fun DrawScope.drawPaddle(state: GameUiState) {
    if (state.paddleWidth <= 0f) return

    val left = state.paddleX - state.paddleWidth / 2f
    val top = state.paddleY - state.paddleHeight / 2f
    val corner = CornerRadius(state.paddleHeight / 2f, state.paddleHeight / 2f)
    val paddleSize = Size(state.paddleWidth, state.paddleHeight)

    // Outer Soft Glow matching Design HTML: shadow-[0_4px_12px_rgba(0,0,0,0.5)]
    drawRoundRect(
        color = ImmersivePrimary.copy(alpha = 0.25f),
        topLeft = Offset(left - 4f, top - 2f),
        size = Size(state.paddleWidth + 8f, state.paddleHeight + 4f),
        cornerRadius = CornerRadius((state.paddleHeight + 4f) / 2f, (state.paddleHeight + 4f) / 2f)
    )

    // Paddle Body: #D0BCFF with vertical highlight gradient
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFEADDFF),
                Color(0xFFD0BCFF),
                Color(0xFFB6A0E3)
            ),
            startY = top,
            endY = top + state.paddleHeight
        ),
        topLeft = Offset(left, top),
        size = paddleSize,
        cornerRadius = corner
    )

    // Top Rim Highlight
    drawRoundRect(
        color = Color(0x99FFFFFF),
        topLeft = Offset(left + 6f, top + 1.5f),
        size = Size(state.paddleWidth - 12f, 2f),
        cornerRadius = CornerRadius(1f, 1f)
    )

    // Bottom Border Line matching Design HTML: border-b-2 border-white/20
    drawLine(
        color = Color(0x33000000),
        start = Offset(left + 4f, top + state.paddleHeight - 1f),
        end = Offset(left + state.paddleWidth - 4f, top + state.paddleHeight - 1f),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawBall(state: GameUiState) {
    if (state.ballRadius <= 0f) return

    val center = Offset(state.ballX, state.ballY)
    val r = state.ballRadius

    // Outer Soft Glow matching Design HTML: shadow-[0_0_15px_rgba(255,255,255,0.8)]
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.5f),
                Color.White.copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = center,
            radius = r * 2.2f
        ),
        radius = r * 2.2f,
        center = center
    )

    // Ball Core (White sphere)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color(0xFFF8F5FF),
                Color(0xFFEADDFF)
            ),
            center = Offset(state.ballX - r * 0.25f, state.ballY - r * 0.25f),
            radius = r
        ),
        radius = r,
        center = center
    )

    // Ball Specular Glint
    drawCircle(
        color = Color.White,
        radius = r * 0.3f,
        center = Offset(state.ballX - r * 0.35f, state.ballY - r * 0.35f)
    )
}
