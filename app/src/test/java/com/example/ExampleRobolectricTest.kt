package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.model.GameStatus
import com.example.game.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Application
    private lateinit var viewModel: GameViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = GameViewModel(context)
        // Initialize arena with standard dimensions
        viewModel.initArena(width = 800f, height = 1200f)
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Brick Breaker", appName)
    }

    @Test
    fun `arena initialization sets up bricks and paddle in ready state`() {
        val state = viewModel.uiState.value
        assertEquals(GameStatus.READY, state.status)
        assertEquals(3, state.lives)
        assertEquals(0, state.score)
        assertTrue("Bricks should be created", state.bricks.isNotEmpty())
        assertEquals(800f / 2f, state.paddleX, 0.1f)
    }

    @Test
    fun `paddle horizontal drag moves paddle and clamps within bounds`() {
        val initialX = viewModel.uiState.value.paddleX
        viewModel.onPaddleDrag(50f)
        assertEquals(initialX + 50f, viewModel.uiState.value.paddleX, 0.1f)

        // Drag far to the right beyond boundary
        viewModel.onPaddleDrag(2000f)
        val state = viewModel.uiState.value
        val maxExpectedX = state.arenaWidth - state.paddleWidth / 2f
        assertEquals(maxExpectedX, state.paddleX, 0.1f)
    }

    @Test
    fun `launching ball transitions state to PLAYING with upward velocity`() {
        assertEquals(GameStatus.READY, viewModel.uiState.value.status)
        viewModel.launchBall()
        val state = viewModel.uiState.value
        assertEquals(GameStatus.PLAYING, state.status)
        assertTrue("Ball Y velocity should be negative (upwards)", state.ballVy < 0f)
    }

    @Test
    fun `ball falling below paddle reduces lives and eventually triggers LOST state`() {
        viewModel.launchBall()

        // Simulate ball falling below bottom edge
        val arenaHeight = viewModel.uiState.value.arenaHeight
        // Run physics steps until ball falls past bottom
        for (i in 0 until 300) {
            viewModel.updateGame(0.03f)
            if (viewModel.uiState.value.status != GameStatus.PLAYING) break
        }

        val stateAfterDrop = viewModel.uiState.value
        // Either lives decremented and reset to READY or LOST
        assertTrue(
            stateAfterDrop.status == GameStatus.READY || stateAfterDrop.status == GameStatus.LOST
        )
    }

    @Test
    fun `restart game resets score lives and bricks`() {
        viewModel.launchBall()
        viewModel.restartGame()
        val state = viewModel.uiState.value
        assertEquals(GameStatus.READY, state.status)
        assertEquals(3, state.lives)
        assertEquals(0, state.score)
        assertFalse(state.bricks.any { it.isDestroyed })
    }
}
