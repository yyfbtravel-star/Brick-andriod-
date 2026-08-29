package com.example.game.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.model.BallTrail
import com.example.game.model.Brick
import com.example.game.model.GameStatus
import com.example.game.model.GameUiState
import com.example.game.model.Particle
import com.example.game.model.ScorePopup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("brick_breaker_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        GameUiState(
            highScore = prefs.getInt(KEY_HIGH_SCORE, 0)
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var nextPopupId = 0L

    companion object {
        private const val KEY_HIGH_SCORE = "key_high_score"
        private const val BRICK_ROWS = 6
        private const val BRICK_COLS = 6
        private const val MAX_BOUNCE_ANGLE = 60f * (PI.toFloat() / 180f) // 60 degrees
        private const val TRAIL_INTERVAL_MS = 25L
    }

    private var lastTrailTime = 0L

    fun initArena(width: Float, height: Float) {
        if (width <= 0 || height <= 0) return

        val currentState = _uiState.value
        // Only re-layout if arena dimensions actually changed significantly or not initialized yet
        if (currentState.arenaWidth == width && currentState.arenaHeight == height && currentState.bricks.isNotEmpty()) {
            return
        }

        val paddleW = (width * 0.26f).coerceIn(120f, 220f)
        val paddleH = 22f
        val paddleY = height - 90f
        val paddleX = width / 2f

        val ballRadius = 11f
        val ballX = paddleX
        val ballY = paddleY - paddleH / 2f - ballRadius - 2f
        val speed = (height * 0.52f).coerceIn(580f, 850f)

        val bricks = createBricks(width, height)

        _uiState.update { current ->
            current.copy(
                arenaWidth = width,
                arenaHeight = height,
                paddleWidth = paddleW,
                paddleHeight = paddleH,
                paddleX = paddleX,
                paddleY = paddleY,
                ballRadius = ballRadius,
                baseBallSpeed = speed,
                currentBallSpeed = speed,
                ballX = ballX,
                ballY = ballY,
                ballVx = 0f,
                ballVy = 0f,
                bricks = bricks,
                status = if (current.status == GameStatus.PLAYING) current.status else GameStatus.READY
            )
        }
    }

    private fun createBricks(arenaWidth: Float, arenaHeight: Float): List<Brick> {
        val list = mutableListOf<Brick>()
        val horizontalMargin = arenaWidth * 0.05f
        val availableWidth = arenaWidth - (horizontalMargin * 2)
        val spacing = 8f
        val brickWidth = (availableWidth - (spacing * (BRICK_COLS - 1))) / BRICK_COLS
        val brickHeight = (arenaHeight * 0.032f).coerceIn(24f, 36f)
        val topMargin = (arenaHeight * 0.05f).coerceAtLeast(16f) // Inside the Immersive rounded card

        val rowColors = listOf(
            // Row 0: Immersive Primary Lavender
            Pair(Color(0xFFD0BCFF), Color(0xFFEADDFF)),
            // Row 1: Bright Lilac / Purple80 tint
            Pair(Color(0xFFC0A9F5), Color(0xFFDDD0F7)),
            // Row 2: Immersive Secondary Mauve / PurpleGrey80
            Pair(Color(0xFFCCC2DC), Color(0xFFE6E0E9)),
            // Row 3: Soft Slate Violet
            Pair(Color(0xFFBDB2CF), Color(0xFFDFD8EC)),
            // Row 4: Immersive Tertiary Soft Rose / Pink80
            Pair(Color(0xFFEFB8C8), Color(0xFFFFD8E4)),
            // Row 5: Dusty Rose / Pink40 tint
            Pair(Color(0xFFE5A8BA), Color(0xFFF8CCD7))
        )

        val rowPoints = listOf(60, 50, 40, 30, 20, 10)

        var id = 0
        for (r in 0 until BRICK_ROWS) {
            val (baseColor, highlightColor) = rowColors[r % rowColors.size]
            val points = rowPoints[r % rowPoints.size]
            val top = topMargin + r * (brickHeight + spacing)
            val bottom = top + brickHeight

            for (c in 0 until BRICK_COLS) {
                val left = horizontalMargin + c * (brickWidth + spacing)
                val right = left + brickWidth

                list.add(
                    Brick(
                        id = id++,
                        row = r,
                        col = c,
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        color = baseColor,
                        highlightColor = highlightColor,
                        points = points,
                        maxHits = 1,
                        currentHits = 0,
                        isDestroyed = false
                    )
                )
            }
        }
        return list
    }

    fun onPaddleDrag(dragDeltaX: Float) {
        _uiState.update { current ->
            val minX = current.paddleWidth / 2f
            val maxX = current.arenaWidth - current.paddleWidth / 2f
            val newPaddleX = (current.paddleX + dragDeltaX).coerceIn(minX, maxX)

            if (current.status == GameStatus.READY) {
                current.copy(
                    paddleX = newPaddleX,
                    ballX = newPaddleX
                )
            } else {
                current.copy(paddleX = newPaddleX)
            }
        }
    }

    fun onPaddleMoveTo(targetX: Float) {
        _uiState.update { current ->
            val minX = current.paddleWidth / 2f
            val maxX = current.arenaWidth - current.paddleWidth / 2f
            val newPaddleX = targetX.coerceIn(minX, maxX)

            if (current.status == GameStatus.READY) {
                current.copy(
                    paddleX = newPaddleX,
                    ballX = newPaddleX
                )
            } else {
                current.copy(paddleX = newPaddleX)
            }
        }
    }

    fun launchBall() {
        _uiState.update { current ->
            if (current.status != GameStatus.READY) return@update current

            // Launch slightly randomized angle (-30 to +30 degrees from straight up)
            val angleDeg = Random.nextFloat() * 40f - 20f
            val angleRad = angleDeg * (PI.toFloat() / 180f)
            val speed = current.baseBallSpeed
            val vx = speed * sin(angleRad)
            val vy = -speed * cos(angleRad)

            current.copy(
                status = GameStatus.PLAYING,
                ballVx = vx,
                ballVy = vy,
                combo = 0
            )
        }
    }

    fun togglePause() {
        _uiState.update { current ->
            when (current.status) {
                GameStatus.PLAYING -> current.copy(status = GameStatus.PAUSED)
                GameStatus.PAUSED -> current.copy(status = GameStatus.PLAYING)
                else -> current
            }
        }
    }

    fun restartGame() {
        val width = _uiState.value.arenaWidth
        val height = _uiState.value.arenaHeight

        val paddleW = (width * 0.26f).coerceIn(120f, 220f)
        val paddleH = 22f
        val paddleY = height - 90f
        val paddleX = width / 2f
        val ballRadius = 11f
        val ballX = paddleX
        val ballY = paddleY - paddleH / 2f - ballRadius - 2f
        val speed = (height * 0.52f).coerceIn(580f, 850f)
        val bricks = createBricks(width, height)

        _uiState.update { current ->
            current.copy(
                status = GameStatus.READY,
                score = 0,
                lives = current.maxLives,
                bricks = bricks,
                paddleX = paddleX,
                paddleY = paddleY,
                paddleWidth = paddleW,
                paddleHeight = paddleH,
                ballX = ballX,
                ballY = ballY,
                ballVx = 0f,
                ballVy = 0f,
                ballRadius = ballRadius,
                baseBallSpeed = speed,
                currentBallSpeed = speed,
                particles = emptyList(),
                ballTrails = emptyList(),
                scorePopups = emptyList(),
                combo = 0,
                screenShakeRemaining = 0f,
                screenShakeOffsetX = 0f,
                screenShakeOffsetY = 0f
            )
        }
    }

    fun updateGame(deltaSeconds: Float) {
        val state = _uiState.value
        if (state.status != GameStatus.PLAYING) {
            // Update particles and effects even if game ended
            if (state.particles.isNotEmpty() || state.scorePopups.isNotEmpty()) {
                updateEffectsOnly(deltaSeconds)
            }
            return
        }

        // Clamp delta time to avoid large jumps
        val clampedDt = deltaSeconds.coerceIn(0.001f, 0.033f)

        // Substepping: perform 2 sub-steps to guarantee precise physics & prevent clipping
        val steps = 2
        val subDt = clampedDt / steps

        var s = state
        for (i in 0 until steps) {
            s = stepPhysics(s, subDt)
            if (s.status != GameStatus.PLAYING) break
        }

        // Add ball trail
        val now = System.currentTimeMillis()
        val updatedTrails = s.ballTrails.mapNotNull {
            val newAlpha = it.alpha - (clampedDt * 2.5f)
            if (newAlpha > 0.05f) it.copy(alpha = newAlpha) else null
        }.toMutableList()

        if (now - lastTrailTime >= TRAIL_INTERVAL_MS && s.status == GameStatus.PLAYING) {
            lastTrailTime = now
            updatedTrails.add(
                BallTrail(
                    x = s.ballX,
                    y = s.ballY,
                    radius = s.ballRadius * 0.8f,
                    alpha = 0.5f
                )
            )
        }

        // Update particles
        val updatedParticles = s.particles.mapNotNull { p ->
            val nextLife = p.life - (clampedDt / p.maxLife)
            if (nextLife > 0f) {
                p.copy(
                    x = p.x + p.vx * clampedDt,
                    y = p.y + p.vy * clampedDt + 180f * clampedDt * clampedDt, // slight gravity
                    alpha = nextLife,
                    life = nextLife
                )
            } else null
        }

        // Update score popups
        val updatedPopups = s.scorePopups.mapNotNull { popup ->
            val nextLife = popup.life - clampedDt * 1.5f
            if (nextLife > 0f) {
                popup.copy(
                    y = popup.y - 35f * clampedDt,
                    alpha = nextLife,
                    life = nextLife
                )
            } else null
        }

        // Screen shake decay
        val newShakeRemaining = (s.screenShakeRemaining - clampedDt).coerceAtLeast(0f)
        val shakeX = if (newShakeRemaining > 0f) (Random.nextFloat() - 0.5f) * 10f * (newShakeRemaining / 0.15f) else 0f
        val shakeY = if (newShakeRemaining > 0f) (Random.nextFloat() - 0.5f) * 10f * (newShakeRemaining / 0.15f) else 0f

        _uiState.value = s.copy(
            ballTrails = updatedTrails,
            particles = updatedParticles,
            scorePopups = updatedPopups,
            screenShakeRemaining = newShakeRemaining,
            screenShakeOffsetX = shakeX,
            screenShakeOffsetY = shakeY
        )
    }

    private fun updateEffectsOnly(dt: Float) {
        _uiState.update { current ->
            val updatedParticles = current.particles.mapNotNull { p ->
                val nextLife = p.life - (dt / p.maxLife)
                if (nextLife > 0f) {
                    p.copy(
                        x = p.x + p.vx * dt,
                        y = p.y + p.vy * dt,
                        alpha = nextLife,
                        life = nextLife
                    )
                } else null
            }
            val updatedPopups = current.scorePopups.mapNotNull { popup ->
                val nextLife = popup.life - dt * 1.5f
                if (nextLife > 0f) {
                    popup.copy(
                        y = popup.y - 35f * dt,
                        alpha = nextLife,
                        life = nextLife
                    )
                } else null
            }
            current.copy(
                particles = updatedParticles,
                scorePopups = updatedPopups,
                screenShakeRemaining = 0f,
                screenShakeOffsetX = 0f,
                screenShakeOffsetY = 0f
            )
        }
    }

    private fun stepPhysics(state: GameUiState, dt: Float): GameUiState {
        var bx = state.ballX + state.ballVx * dt
        var by = state.ballY + state.ballVy * dt
        var bvx = state.ballVx
        var bvy = state.ballVy
        var speed = state.currentBallSpeed
        val radius = state.ballRadius
        val w = state.arenaWidth
        val h = state.arenaHeight

        val newParticles = state.particles.toMutableList()
        val newPopups = state.scorePopups.toMutableList()
        var scoreAdd = 0
        var combo = state.combo
        var screenShake = state.screenShakeRemaining

        // 1. Left and Right Wall Collision
        if (bx - radius <= 0f) {
            bx = radius
            bvx = abs(bvx)
            spawnWallParticles(bx, by, Color.White, newParticles)
        } else if (bx + radius >= w) {
            bx = w - radius
            bvx = -abs(bvx)
            spawnWallParticles(bx, by, Color.White, newParticles)
        }

        // 2. Top Wall Collision
        if (by - radius <= 0f) {
            by = radius
            bvy = abs(bvy)
            spawnWallParticles(bx, by, Color.White, newParticles)
        }

        // 3. Bottom Wall (Ball lost past paddle)
        if (by - radius >= h) {
            val remainingLives = state.lives - 1
            if (remainingLives <= 0) {
                // Game Over - Loss
                val finalScore = state.score
                val newHigh = if (finalScore > state.highScore) {
                    prefs.edit().putInt(KEY_HIGH_SCORE, finalScore).apply()
                    finalScore
                } else state.highScore

                return state.copy(
                    status = GameStatus.LOST,
                    lives = 0,
                    highScore = newHigh,
                    particles = newParticles,
                    scorePopups = newPopups
                )
            } else {
                // Reset ball on paddle
                val resetBallY = state.paddleY - state.paddleHeight / 2f - radius - 2f
                return state.copy(
                    status = GameStatus.READY,
                    lives = remainingLives,
                    ballX = state.paddleX,
                    ballY = resetBallY,
                    ballVx = 0f,
                    ballVy = 0f,
                    combo = 0,
                    particles = newParticles,
                    scorePopups = newPopups
                )
            }
        }

        // 4. Paddle Collision
        val paddleLeft = state.paddleX - state.paddleWidth / 2f
        val paddleRight = state.paddleX + state.paddleWidth / 2f
        val paddleTop = state.paddleY - state.paddleHeight / 2f
        val paddleBottom = state.paddleY + state.paddleHeight / 2f

        // Check if ball intersects paddle and is moving downward
        if (bvy > 0f &&
            bx + radius >= paddleLeft &&
            bx - radius <= paddleRight &&
            by + radius >= paddleTop &&
            by - radius <= paddleBottom
        ) {
            // Deflection angle mapping:
            // Calculate hit position from center (-1.0 to 1.0)
            val hitOffset = ((bx - state.paddleX) / (state.paddleWidth / 2f)).coerceIn(-1f, 1f)
            val deflectionAngle = hitOffset * MAX_BOUNCE_ANGLE

            // Slight speedup on paddle hit for progression (max 1100f)
            speed = (speed * 1.015f).coerceAtMost(1100f)

            bvx = speed * sin(deflectionAngle)
            bvy = -abs(speed * cos(deflectionAngle))
            by = paddleTop - radius // Prevent sticking into paddle

            // Paddle bounce particles
            spawnPaddleParticles(bx, paddleTop, newParticles)
            combo = 0 // reset combo on paddle hit
        }

        // 5. Brick Collision
        var brickHitOccurred = false
        val updatedBricks = state.bricks.map { brick ->
            if (brick.isDestroyed || brickHitOccurred) {
                brick
            } else {
                // AABB vs Circle intersection
                val closestX = bx.coerceIn(brick.left, brick.right)
                val closestY = by.coerceIn(brick.top, brick.bottom)
                val dx = bx - closestX
                val dy = by - closestY
                val distSq = dx * dx + dy * dy

                if (distSq <= radius * radius) {
                    brickHitOccurred = true

                    // Determine bounce normal
                    if (abs(dx) > abs(dy)) {
                        bvx = if (dx > 0) abs(bvx) else -abs(bvx)
                    } else {
                        bvy = if (dy > 0) abs(bvy) else -abs(bvy)
                    }

                    val pointsEarned = brick.points * (1 + combo / 4)
                    scoreAdd += pointsEarned
                    combo += 1
                    screenShake = 0.12f

                    // Spawn brick explosion particles
                    spawnBrickParticles(brick.centerX, brick.centerY, brick.color, newParticles)

                    // Add score popup
                    newPopups.add(
                        ScorePopup(
                            id = nextPopupId++,
                            text = "+$pointsEarned",
                            x = brick.centerX,
                            y = brick.top,
                            color = brick.highlightColor
                        )
                    )

                    brick.copy(isDestroyed = true)
                } else {
                    brick
                }
            }
        }

        val totalScore = state.score + scoreAdd
        val allDestroyed = updatedBricks.all { it.isDestroyed }

        val newStatus = if (allDestroyed) GameStatus.WON else GameStatus.PLAYING
        val finalHighScore = if (totalScore > state.highScore) {
            prefs.edit().putInt(KEY_HIGH_SCORE, totalScore).apply()
            totalScore
        } else state.highScore

        return state.copy(
            status = newStatus,
            score = totalScore,
            highScore = finalHighScore,
            bricks = updatedBricks,
            ballX = bx,
            ballY = by,
            ballVx = bvx,
            ballVy = bvy,
            currentBallSpeed = speed,
            combo = combo,
            particles = newParticles,
            scorePopups = newPopups,
            screenShakeRemaining = screenShake
        )
    }

    private fun spawnBrickParticles(x: Float, y: Float, color: Color, list: MutableList<Particle>) {
        val count = 12
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val pSpeed = Random.nextFloat() * 220f + 60f
            list.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * pSpeed,
                    vy = sin(angle) * pSpeed,
                    color = color,
                    size = Random.nextFloat() * 5f + 3f,
                    life = 1f,
                    maxLife = Random.nextFloat() * 0.4f + 0.3f
                )
            )
        }
    }

    private fun spawnPaddleParticles(x: Float, y: Float, list: MutableList<Particle>) {
        for (i in 0 until 7) {
            val angle = -Random.nextFloat() * PI.toFloat() // upwards
            val pSpeed = Random.nextFloat() * 120f + 40f
            list.add(
                Particle(
                    x = x + (Random.nextFloat() - 0.5f) * 18f,
                    y = y,
                    vx = cos(angle) * pSpeed,
                    vy = sin(angle) * pSpeed,
                    color = Color(0xFFD0BCFF),
                    size = Random.nextFloat() * 4f + 2f,
                    life = 1f,
                    maxLife = 0.25f
                )
            )
        }
    }

    private fun spawnWallParticles(x: Float, y: Float, color: Color, list: MutableList<Particle>) {
        for (i in 0 until 4) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val pSpeed = Random.nextFloat() * 80f + 20f
            list.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * pSpeed,
                    vy = sin(angle) * pSpeed,
                    color = color,
                    size = 3f,
                    life = 1f,
                    maxLife = 0.2f
                )
            )
        }
    }
}
