package com.fancyball.fancyball

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

data class PegPosition(val index: Int, val row: Int, val col: Int, val x: Float, val y: Float)

data class SlotResult(
    val slot: Int,
    val multiplier: Float,
    val win: Int,
    val timestamp: Long = System.currentTimeMillis()
)

private data class SimBall(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float
)

data class PlinkoUiState(
    val balance: Int = 100,
    val bet: Int = 10,
    val ball: Offset = Offset.Unspecified,
    val ballColorIndex: Int = 0,
    val isPlaying: Boolean = false,
    val lastWin: Int = 0,
    val lastMultiplier: Float = 0f,
    val highlightedSlot: Int? = null,
    val pegHighlights: Set<Int> = emptySet(),
    val recentResults: List<SlotResult> = emptyList()
) {
    val canPlay: Boolean = !isPlaying && balance >= bet
    val canIncreaseBet: Boolean = !isPlaying && bet + 5 <= balance
    val canDecreaseBet: Boolean = !isPlaying && bet > 5
}

class PlinkoViewModel : ViewModel() {
    companion object {
        const val ROWS = 12
        const val COLS_BASE = 3
        const val BOARD_LEFT = 0.025f
        const val BOARD_RIGHT = 0.975f
        const val BOARD_TOP = 0.025f
        const val BOARD_BOTTOM = 0.92f
        const val BALL_RADIUS = 0.018f
        const val PEG_RADIUS = 0.0115f
        const val GRAVITY = 0.00058f
        const val BOUNCE_DAMPING = 0.58f
        const val WALL_DAMPING = 0.72f
        const val HORIZONTAL_SPREAD = 0.010f
        const val FRAME_DELAY_MS = 16L
        const val HIGHLIGHT_DURATION_MS = 260L
        const val KNOCK_SOUND_DEBOUNCE_MS = 50L
    }

    private val multipliers = listOf(8f, 3f, 1.5f, 0.7f, 0.3f, 0.3f, 0.7f, 1.5f, 3f, 8f)
    private val ballColorCount = 6
    val pegs: List<PegPosition> = generatePegs()

    private val _uiState = MutableStateFlow(PlinkoUiState())
    val uiState: StateFlow<PlinkoUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null
    private val activeHighlights = mutableMapOf<Int, Long>()
    private var lastKnockSoundTime = 0L
    private var nextBallColorIndex = 0

    var onKnockSound: (() -> Unit)? = null
    var onGetMoneySound: (() -> Unit)? = null
    var onSelectValueSound: (() -> Unit)? = null

    fun increaseBet() {
        _uiState.update { state ->
            state.copy(bet = (state.bet + 5).coerceAtMost(state.balance))
        }
        onSelectValueSound?.invoke()
    }

    fun decreaseBet() {
        _uiState.update { state ->
            state.copy(bet = (state.bet - 5).coerceAtLeast(5))
        }
        onSelectValueSound?.invoke()
    }

    fun setBet(amount: Int) {
        if (_uiState.value.isPlaying) return
        _uiState.update { state ->
            state.copy(bet = amount.coerceIn(5, state.balance.coerceAtLeast(5)))
        }
        onSelectValueSound?.invoke()
    }

    fun resetGame() {
        simulationJob?.cancel()
        activeHighlights.clear()
        _uiState.value = PlinkoUiState()
        onSelectValueSound?.invoke()
    }

    fun dropBall() {
        val state = _uiState.value
        if (state.isPlaying || state.balance < state.bet) return

        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            val bet = _uiState.value.bet
            val ballColorIndex = nextBallColorIndex()
            val ball = SimBall(
                x = 0.5f + Random.nextFloat() * 0.024f - 0.012f,
                y = BOARD_TOP - BALL_RADIUS,
                vx = Random.nextFloat() * 0.004f - 0.002f,
                vy = 0.006f
            )

            _uiState.update {
                it.copy(
                    balance = it.balance - bet,
                    ball = Offset(ball.x, ball.y),
                    ballColorIndex = ballColorIndex,
                    isPlaying = true,
                    lastWin = 0,
                    lastMultiplier = 0f,
                    highlightedSlot = null,
                    pegHighlights = emptySet()
                )
            }

            var frames = 0
            while (ball.y <= BOARD_BOTTOM + BALL_RADIUS * 2f && frames < 900) {
                stepBall(ball)
                expireHighlights()
                _uiState.update {
                    it.copy(
                        ball = Offset(ball.x, ball.y),
                        pegHighlights = activeHighlights.keys.toSet()
                    )
                }
                frames++
                delay(FRAME_DELAY_MS)
            }

            val slot = resolveSlot(ball.x)
            val multiplier = multipliers[slot]
            val win = (bet * multiplier).toInt()
            val result = SlotResult(slot = slot, multiplier = multiplier, win = win)
            if (win > 0) {
                onGetMoneySound?.invoke()
            }

            activeHighlights.clear()
            _uiState.update {
                it.copy(
                    balance = it.balance + win,
                    isPlaying = false,
                    lastWin = win,
                    lastMultiplier = multiplier,
                    highlightedSlot = slot,
                    pegHighlights = emptySet(),
                    recentResults = (listOf(result) + it.recentResults).take(6)
                )
            }
        }
    }

    fun multipliers(): List<Float> = multipliers

    private fun nextBallColorIndex(): Int {
        val color = nextBallColorIndex % ballColorCount
        nextBallColorIndex++
        return color
    }

    private fun stepBall(ball: SimBall) {
        ball.vy += GRAVITY
        ball.x += ball.vx
        ball.y += ball.vy

        if (ball.x - BALL_RADIUS < BOARD_LEFT) {
            ball.x = BOARD_LEFT + BALL_RADIUS
            ball.vx = abs(ball.vx) * WALL_DAMPING
        }

        if (ball.x + BALL_RADIUS > BOARD_RIGHT) {
            ball.x = BOARD_RIGHT - BALL_RADIUS
            ball.vx = -abs(ball.vx) * WALL_DAMPING
        }

        for (peg in pegs) {
            val dx = ball.x - peg.x
            val dy = ball.y - peg.y
            val dist = sqrt(dx * dx + dy * dy)
            val minDist = BALL_RADIUS + PEG_RADIUS

            if (dist < minDist && dist > 0.0001f) {
                val nx = dx / dist
                val ny = dy / dist

                ball.x = peg.x + nx * minDist
                ball.y = peg.y + ny * minDist

                val dot = ball.vx * nx + ball.vy * ny
                ball.vx = (ball.vx - 2f * dot * nx) * BOUNCE_DAMPING
                ball.vy = (ball.vy - 2f * dot * ny) * BOUNCE_DAMPING

                ball.vx += Random.nextFloat() * HORIZONTAL_SPREAD * 2f - HORIZONTAL_SPREAD
                if (ball.vy < 0.0025f) ball.vy = 0.0025f

                activeHighlights[peg.index] = System.currentTimeMillis()
                val now = System.currentTimeMillis()
                if (now - lastKnockSoundTime > KNOCK_SOUND_DEBOUNCE_MS) {
                    onKnockSound?.invoke()
                    lastKnockSoundTime = now
                }
            }
        }
    }

    private fun resolveSlot(x: Float): Int {
        val boardWidth = BOARD_RIGHT - BOARD_LEFT
        val slotWidth = boardWidth / multipliers.size
        return ((x - BOARD_LEFT) / slotWidth).toInt().coerceIn(0, multipliers.lastIndex)
    }

    private fun expireHighlights() {
        val now = System.currentTimeMillis()
        activeHighlights.entries.removeAll { now - it.value > HIGHLIGHT_DURATION_MS }
    }

    private fun generatePegs(): List<PegPosition> {
        val result = mutableListOf<PegPosition>()
        val boardWidth = BOARD_RIGHT - BOARD_LEFT
        val boardHeight = BOARD_BOTTOM - BOARD_TOP
        var index = 0

        for (row in 0 until ROWS) {
            val pegsInRow = COLS_BASE + row
            val rowY = BOARD_TOP + (row + 1) * boardHeight / (ROWS + 2)
            val rowWidth = pegsInRow * boardWidth / (ROWS + COLS_BASE)
            val startX = 0.5f - rowWidth / 2f

            for (col in 0 until pegsInRow) {
                val pegX = startX + col * rowWidth / (pegsInRow - 1).coerceAtLeast(1)
                result += PegPosition(index = index++, row = row, col = col, x = pegX, y = rowY)
            }
        }

        return result
    }
}
