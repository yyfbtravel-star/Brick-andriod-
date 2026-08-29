package com.example.game.model

import androidx.compose.ui.graphics.Color

enum class GameStatus {
    READY,
    PLAYING,
    PAUSED,
    WON,
    LOST
}

data class Brick(
    val id: Int,
    val row: Int,
    val col: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val color: Color,
    val highlightColor: Color,
    val points: Int,
    val maxHits: Int = 1,
    val currentHits: Int = 0,
    val isDestroyed: Boolean = false
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val alpha: Float = 1f,
    val life: Float = 1f, // 1.0 down to 0.0
    val maxLife: Float = 1f
)

data class BallTrail(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float
)

data class ScorePopup(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val alpha: Float = 1f,
    val life: Float = 1f
)

data class GameUiState(
    val status: GameStatus = GameStatus.READY,
    val score: Int = 0,
    val highScore: Int = 0,
    val lives: Int = 3,
    val maxLives: Int = 3,
    val bricks: List<Brick> = emptyList(),
    val paddleX: Float = 0f,
    val paddleY: Float = 0f,
    val paddleWidth: Float = 160f,
    val paddleHeight: Float = 24f,
    val ballX: Float = 0f,
    val ballY: Float = 0f,
    val ballVx: Float = 0f,
    val ballVy: Float = 0f,
    val ballRadius: Float = 12f,
    val baseBallSpeed: Float = 620f,
    val currentBallSpeed: Float = 620f,
    val arenaWidth: Float = 0f,
    val arenaHeight: Float = 0f,
    val particles: List<Particle> = emptyList(),
    val ballTrails: List<BallTrail> = emptyList(),
    val scorePopups: List<ScorePopup> = emptyList(),
    val combo: Int = 0,
    val screenShakeRemaining: Float = 0f,
    val screenShakeOffsetX: Float = 0f,
    val screenShakeOffsetY: Float = 0f
) {
    val totalBricks: Int get() = bricks.size
    val remainingBricks: Int get() = bricks.count { !it.isDestroyed }
    val destroyedBricks: Int get() = totalBricks - remainingBricks
}
