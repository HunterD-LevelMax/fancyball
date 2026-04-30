package com.fancyball.fancyball

import android.content.res.Configuration
import android.os.Bundle
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fancyball.fancyball.ui.theme.FancyBallTheme
import java.util.Locale
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private val viewModel: PlinkoViewModel by viewModels()
    private var soundManager: FancySoundManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundManager = FancySoundManager(applicationContext)
        setContent {
            FancyBallTheme(dynamicColor = false) {
                val colors = fancyPalette()
                Surface(color = colors.bgDeep) {
                    PlinkoGame(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        soundManager?.startMusic()
    }

    override fun onPause() {
        super.onPause()
        soundManager?.pauseMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager?.release()
        soundManager = null
    }

    fun playKnockSound() {
        soundManager?.playKnock()
    }

    fun playGetMoneySound() {
        soundManager?.playGetMoney()
    }

    fun playSelectValueSound() {
        soundManager?.playSelectValue()
    }
}

@Composable
fun PlinkoGame(viewModel: PlinkoViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val colors = fancyPalette()

    DisposableEffect(viewModel) {
        val activity = context as? MainActivity
        viewModel.onKnockSound = { activity?.playKnockSound() }
        viewModel.onGetMoneySound = { activity?.playGetMoneySound() }
        viewModel.onSelectValueSound = { activity?.playSelectValueSound() }

        onDispose {
            viewModel.onKnockSound = null
            viewModel.onGetMoneySound = null
            viewModel.onSelectValueSound = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.bgStart, colors.bgMid, colors.bgEnd)
                )
            )
    ) {
        BackgroundGlow(colors)

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlinkoBoard(
                    state = state,
                    multipliers = viewModel.multipliers(),
                    pegs = viewModel.pegs,
                    colors = colors,
                    fillContainer = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                Column(
                    modifier = Modifier
                        .width(310.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Header(state, colors, compact = true)

                    Spacer(Modifier.height(10.dp))

                    ControlPanel(
                        state = state,
                        colors = colors,
                        compact = true,
                        onDecrease = viewModel::decreaseBet,
                        onIncrease = viewModel::increaseBet,
                        onSetBet = viewModel::setBet,
                        onReset = viewModel::resetGame,
                        onDrop = viewModel::dropBall
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Header(state, colors)

                Spacer(Modifier.height(10.dp))

                PlinkoBoard(
                    state = state,
                    multipliers = viewModel.multipliers(),
                    pegs = viewModel.pegs,
                    colors = colors,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                )

                Spacer(Modifier.height(14.dp))

                ControlPanel(
                    state = state,
                    colors = colors,
                    onDecrease = viewModel::decreaseBet,
                    onIncrease = viewModel::increaseBet,
                    onSetBet = viewModel::setBet,
                    onReset = viewModel::resetGame,
                    onDrop = viewModel::dropBall
                )
            }
        }
    }
}

@Composable
private fun Header(state: PlinkoUiState, colors: FancyPalette, compact: Boolean = false) {
    val baseModifier = Modifier
        .fillMaxWidth()
        .widthIn(max = if (compact) 340.dp else 620.dp)
        .shadow(18.dp, RoundedCornerShape(22.dp), clip = false)
        .clip(RoundedCornerShape(22.dp))
        .background(
            Brush.horizontalGradient(
                listOf(colors.panelStart, colors.panelMid, colors.panelEnd)
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(colors.gold.copy(alpha = 0.33f), colors.cyan.copy(alpha = 0.20f), colors.purple.copy(alpha = 0.33f))
            ),
            shape = RoundedCornerShape(22.dp)
        )
        .padding(horizontal = if (compact) 12.dp else 14.dp, vertical = if (compact) 10.dp else 12.dp)

    if (compact) {
        Row(
            modifier = baseModifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderMetric(
                label = stringResource(R.string.balance_label),
                value = stringResource(R.string.money_amount, state.balance),
                accent = colors.gold,
                colors = colors,
                compact = true,
                modifier = Modifier.weight(1f)
            )

            HeaderDivider(colors, compact = true)

            HeaderMetric(
                label = stringResource(R.string.bet_label),
                value = stringResource(R.string.money_amount, state.bet),
                accent = colors.cyan,
                colors = colors,
                compact = true,
                modifier = Modifier.weight(0.82f)
            )

            ResultBadge(state, colors, compact = true)
        }
        return
    }

    Row(
        modifier = Modifier
            .then(baseModifier),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderMetric(
            label = stringResource(R.string.balance_label),
            value = stringResource(R.string.money_amount, state.balance),
            accent = colors.gold,
            colors = colors,
            modifier = Modifier.weight(1f)
        )

        HeaderDivider(colors)

        HeaderMetric(
            label = stringResource(R.string.bet_label),
            value = stringResource(R.string.money_amount, state.bet),
            accent = colors.cyan,
            colors = colors,
            modifier = Modifier.weight(1f)
        )

        ResultBadge(state, colors)
    }
}

@Composable
private fun ResultBadge(state: PlinkoUiState, colors: FancyPalette, compact: Boolean = false) {
    val alpha by animateFloatAsState(
        targetValue = if (state.lastWin > 0) 1f else 0.65f,
        animationSpec = tween(300),
        label = "resultAlpha"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (state.lastWin > 0) colors.gold else colors.white12)
            .border(1.dp, if (state.lastWin > 0) colors.goldLight else colors.white20, RoundedCornerShape(100.dp))
            .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 6.dp else 8.dp)
    ) {
        Text(
            text = if (state.lastWin > 0) stringResource(R.string.plus_money_amount, state.lastWin) else stringResource(R.string.ready_status),
            color = if (state.lastWin > 0) colors.textDark else colors.white.copy(alpha = alpha),
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 12.sp else 13.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun HeaderMetric(label: String, value: String, accent: Color, colors: FancyPalette, modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            color = colors.textMuted,
            fontSize = if (compact) 9.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = accent,
            fontSize = if (compact) 19.sp else 22.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun HeaderDivider(colors: FancyPalette, compact: Boolean = false) {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = if (compact) 30.dp else 36.dp)
            .background(colors.white.copy(alpha = 0.12f))
    )
}

@Composable
private fun PlinkoBoard(
    state: PlinkoUiState,
    multipliers: List<Float>,
    pegs: List<PegPosition>,
    colors: FancyPalette,
    fillContainer: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "boardPulse").animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val boardModifier = if (fillContainer) {
        modifier.fillMaxSize()
    } else {
        modifier.aspectRatio(if (multipliers.size > 8) 0.72f else 0.78f)
    }

    Box(
        modifier = boardModifier
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surface)
            .border(1.dp, colors.white20, RoundedCornerShape(28.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val boardWidth = size.width
            val boardHeight = size.height
            val slotCount = multipliers.size
            val slotHeight = boardHeight * 0.11f
            val pinRadius = min(boardWidth, boardHeight) * 0.012f
            val playLeft = PlinkoViewModel.BOARD_LEFT * boardWidth
            val playRight = PlinkoViewModel.BOARD_RIGHT * boardWidth
            val playTop = PlinkoViewModel.BOARD_TOP * boardHeight
            val playBottom = PlinkoViewModel.BOARD_BOTTOM * boardHeight

            drawRect(
                brush = Brush.linearGradient(
                    listOf(colors.boardStart, colors.boardMid1, colors.boardMid2, colors.boardEnd),
                    start = Offset(0f, 0f),
                    end = Offset(boardWidth, boardHeight)
                )
            )

            drawRoundRect(
                brush = Brush.radialGradient(
                    listOf(colors.white.copy(alpha = 0.10f), colors.transparent),
                    center = Offset(boardWidth * 0.5f, boardHeight * 0.42f),
                    radius = boardWidth * 0.58f
                ),
                topLeft = Offset(boardWidth * 0.06f, boardHeight * 0.05f),
                size = Size(boardWidth * 0.88f, boardHeight * 0.82f),
                cornerRadius = CornerRadius(56f, 56f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors.cyanLight.copy(alpha = 0.53f), colors.cyanLight.copy(alpha = 0.13f), colors.transparent),
                    center = Offset(boardWidth * 0.18f, boardHeight * 0.14f),
                    radius = boardWidth * 0.54f
                ),
                radius = boardWidth * 0.54f,
                center = Offset(boardWidth * 0.18f, boardHeight * 0.14f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors.gold.copy(alpha = 0.47f), colors.pink.copy(alpha = 0.10f), colors.transparent),
                    center = Offset(boardWidth * 0.84f, boardHeight * 0.52f),
                    radius = boardWidth * 0.48f
                ),
                radius = boardWidth * 0.48f,
                center = Offset(boardWidth * 0.84f, boardHeight * 0.52f)
            )

            val ribbon = Path().apply {
                moveTo(0f, boardHeight * 0.22f)
                cubicTo(boardWidth * 0.3f, boardHeight * 0.08f, boardWidth * 0.58f, boardHeight * 0.36f, boardWidth, boardHeight * 0.18f)
                lineTo(boardWidth, boardHeight * 0.34f)
                cubicTo(boardWidth * 0.62f, boardHeight * 0.48f, boardWidth * 0.28f, boardHeight * 0.22f, 0f, boardHeight * 0.38f)
                close()
            }
            drawPath(
                path = ribbon,
                brush = Brush.horizontalGradient(
                    listOf(colors.transparent, colors.cyanLight.copy(alpha = 0.13f), colors.gold.copy(alpha = 0.16f), colors.transparent)
                )
            )

            pegs.forEach { peg ->
                val x = peg.x * boardWidth
                val y = peg.y * boardHeight
                val highlighted = peg.index in state.pegHighlights
                val glowScale = if (highlighted) 6.2f else 3.8f
                val glowAlpha = if (highlighted) 0.52f else 0.18f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.cyan.copy(alpha = glowAlpha),
                            colors.transparent
                        ),
                        center = Offset(x, y),
                        radius = pinRadius * glowScale
                    ),
                    radius = pinRadius * glowScale,
                    center = Offset(x, y)
                )

                if (highlighted) {
                    drawCircle(colors.white.copy(alpha = 0.58f), pinRadius * 2.3f, Offset(x, y))
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(colors.white, colors.goldLight, colors.purple),
                        center = Offset(x - pinRadius * 0.3f, y - pinRadius * 0.35f),
                        radius = pinRadius * 1.25f
                    ),
                    radius = pinRadius * 1.15f,
                    center = Offset(x, y)
                )
            }

            val slotWidth = (playRight - playLeft) / slotCount
            multipliers.forEachIndexed { index, multiplier ->
                val left = playLeft + index * slotWidth + 3f
                val slotSize = Size(slotWidth - 8f, slotHeight - 8f)
                val top = playBottom - slotSize.height * 0.45f
                val highlighted = state.highlightedSlot == index
                val slotColor = slotAccentColor(multiplier, highlighted, colors)
                drawRoundRect(
                    color = slotColor.copy(alpha = if (highlighted) 0.42f else 0.2f),
                    topLeft = Offset(left, top),
                    size = slotSize,
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawRoundRect(
                    color = slotColor.copy(alpha = 0.9f),
                    topLeft = Offset(left, top),
                    size = slotSize,
                    cornerRadius = CornerRadius(18f, 18f),
                    style = Stroke(width = if (highlighted) 4f else 2f)
                )

                drawIntoCanvas { canvas ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (highlighted) colors.surface.toArgb() else colors.white.toArgb()
                        textAlign = Paint.Align.CENTER
                        textSize = (slotWidth * 0.24f).coerceIn(16f, 30f)
                        isFakeBoldText = true
                    }
                    val centerX = left + slotSize.width / 2f
                    val centerY = top + slotSize.height / 2f - (paint.ascent() + paint.descent()) / 2f
                    canvas.nativeCanvas.drawText(multiplierText(multiplier), centerX, centerY, paint)
                }
            }

            val hasBall = state.ball != Offset.Unspecified
            val ball = if (hasBall) {
                Offset(state.ball.x * boardWidth, state.ball.y * boardHeight)
            } else {
                Offset(boardWidth / 2f, PlinkoViewModel.BOARD_TOP * boardHeight)
            }
            if (state.isPlaying || hasBall) {
                val ballColor = colors.ballColors[state.ballColorIndex % colors.ballColors.size]
                drawCircle(ballColor.copy(alpha = 0.48f), 38f * pulse.value, ball)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(colors.white, ballColor, darken(ballColor)),
                        center = Offset(ball.x - 7f, ball.y - 9f),
                        radius = 28f
                    ),
                    radius = min(boardWidth, boardHeight) * 0.018f,
                    center = ball
                )
                drawCircle(colors.white.copy(alpha = 0.8f), 4f, Offset(ball.x - 5f, ball.y - 6f))
            }
        }

    }
}

@Composable
private fun ControlPanel(
    state: PlinkoUiState,
    colors: FancyPalette,
    compact: Boolean = false,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onSetBet: (Int) -> Unit,
    onReset: () -> Unit,
    onDrop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = if (compact) 340.dp else 620.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BetButton(stringResource(R.string.bet_minus_five), state.canDecreaseBet, onDecrease, colors, compact)
            FancyActionButton(
                text = if (state.isPlaying) stringResource(R.string.dropping_status) else stringResource(R.string.drop_ball_button),
                enabled = state.canPlay,
                colors = colors,
                compact = compact,
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 46.dp else 56.dp),
                onClick = onDrop
            )
            BetButton(stringResource(R.string.bet_plus_five), state.canIncreaseBet, onIncrease, colors, compact)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(5, 10, 25, 50).forEach { amount ->
                QuickBetChip(
                    amount = amount,
                    selected = state.bet == amount,
                    enabled = !state.isPlaying && amount <= state.balance,
                    onClick = { onSetBet(amount) },
                    colors = colors,
                    compact = compact,
                    modifier = Modifier.weight(1f)
                )
            }

            FancyUtilityButton(
                text = stringResource(R.string.reset_button),
                enabled = !state.isPlaying,
                colors = colors,
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 36.dp else 42.dp),
                compact = compact,
                onClick = onReset
            )
        }

        AnimatedVisibility(visible = state.lastMultiplier > 0f || state.highlightedSlot != null) {
            val message = if (state.lastWin > 0) {
                stringResource(R.string.win_result_format, multiplierText(state.lastMultiplier), state.lastWin)
            } else {
                stringResource(R.string.no_win_result)
            }
            Text(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = if (state.lastWin > 0) colors.gold else colors.textSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 12.sp else 14.sp,
                maxLines = 1
            )
        }

        if (state.recentResults.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.recentResults.forEach { result ->
                    val accent = slotAccentColor(result.multiplier, highlighted = false, colors)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 24.dp else 28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(accent.copy(alpha = 0.16f))
                            .border(1.dp, accent.copy(alpha = 0.46f), RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = multiplierText(result.multiplier),
                            color = accent,
                            fontSize = if (compact) 9.sp else 10.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickBetChip(
    amount: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    colors: FancyPalette,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val accent = if (selected) colors.purple else colors.cyan
    Box(
        modifier = modifier
            .height(if (compact) 36.dp else 42.dp)
            .shadow(if (selected) 12.dp else 6.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (selected) {
                        listOf(colors.purpleLight, colors.purpleMid, colors.purpleDark)
                    } else {
                        listOf(colors.buttonDisabledMid, colors.surfaceAlt, colors.surfaceAlt2)
                    }
                )
            )
            .border(1.dp, accent.copy(alpha = if (enabled) 0.75f else 0.22f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(10.dp)
                .background(colors.white.copy(alpha = if (enabled) 0.13f else 0.04f))
        )
        Text(
            stringResource(R.string.money_amount, amount),
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.Black,
            color = if (enabled) colors.white else colors.textDisabledAlt,
            maxLines = 1
        )
    }
}

@Composable
private fun BetButton(text: String, enabled: Boolean, onClick: () -> Unit, colors: FancyPalette, compact: Boolean = false) {
    FancyUtilityButton(
        text = text,
        enabled = enabled,
        colors = colors,
        modifier = Modifier.size(width = if (compact) 58.dp else 76.dp, height = if (compact) 46.dp else 56.dp),
        compact = compact,
        onClick = onClick
    )
}

@Composable
private fun FancyActionButton(
    text: String,
    enabled: Boolean,
    colors: FancyPalette,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .shadow(if (enabled) 20.dp else 6.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (enabled) {
                        listOf(colors.goldBright, colors.goldButton, colors.goldDark)
                    } else {
                        listOf(colors.buttonDisabledTop, colors.buttonDisabledMid, colors.buttonDisabledBottom)
                    }
                )
            )
            .border(1.dp, if (enabled) colors.goldLight else colors.borderDisabled, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(if (compact) 13.dp else 16.dp)
                .background(colors.white.copy(alpha = if (enabled) 0.26f else 0.05f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(6.dp)
                .background(colors.black.copy(alpha = if (enabled) 0.18f else 0.28f))
        )
        Text(
            text = text,
            color = if (enabled) colors.textGoldDark else colors.textSecondary.copy(alpha = 0.75f),
            fontSize = if (compact) 13.sp else 16.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun FancyUtilityButton(
    text: String,
    enabled: Boolean,
    colors: FancyPalette,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 14.dp else 18.dp)
    Box(
        modifier = modifier
            .shadow(if (enabled) 10.dp else 3.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (enabled) {
                        listOf(colors.buttonUtilityTop, colors.buttonUtilityMid, colors.buttonUtilityBottom)
                    } else {
                        listOf(colors.buttonDimTop, colors.buttonDimMid, colors.buttonDimBottom)
                    }
                )
            )
            .border(1.dp, if (enabled) colors.cyan.copy(alpha = 0.40f) else colors.textDisabled.copy(alpha = 0.13f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(if (compact) 10.dp else 13.dp)
                .background(colors.white.copy(alpha = if (enabled) 0.12f else 0.04f))
        )
        Text(
            text = text,
            color = if (enabled) colors.white else colors.textDisabled,
            fontSize = if (compact) 12.sp else 18.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun BackgroundGlow(colors: FancyPalette) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(listOf(colors.cyan.copy(alpha = 0.20f), colors.transparent)),
            radius = size.width * 0.55f,
            center = Offset(size.width * 0.1f, size.height * 0.12f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(colors.gold.copy(alpha = 0.20f), colors.transparent)),
            radius = size.width * 0.5f,
            center = Offset(size.width * 0.95f, size.height * 0.82f)
        )
    }
}

private fun multiplierText(multiplier: Float): String {
    return if (multiplier % 1f == 0f) {
        "${multiplier.toInt()}x"
    } else {
        String.format(Locale.US, "%.1fx", multiplier)
    }
}

private fun slotAccentColor(multiplier: Float, highlighted: Boolean, colors: FancyPalette): Color {
    return when {
        highlighted -> colors.gold
        multiplier >= 8f -> colors.pink
        multiplier >= 3f -> colors.purple
        multiplier >= 1.5f -> colors.cyan
        multiplier >= 0.7f -> colors.teal
        else -> colors.slotLow
    }
}

private fun darken(color: Color): Color {
    return Color(
        red = color.red * 0.62f,
        green = color.green * 0.62f,
        blue = color.blue * 0.62f,
        alpha = color.alpha
    )
}

private data class FancyPalette(
    val white: Color,
    val black: Color,
    val transparent: Color,
    val bgDeep: Color,
    val bgStart: Color,
    val bgMid: Color,
    val bgEnd: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceAlt2: Color,
    val panelStart: Color,
    val panelMid: Color,
    val panelEnd: Color,
    val boardStart: Color,
    val boardMid1: Color,
    val boardMid2: Color,
    val boardEnd: Color,
    val gold: Color,
    val goldLight: Color,
    val goldBright: Color,
    val goldButton: Color,
    val goldDark: Color,
    val cyan: Color,
    val cyanLight: Color,
    val pink: Color,
    val teal: Color,
    val purple: Color,
    val purpleLight: Color,
    val purpleMid: Color,
    val purpleDark: Color,
    val orange: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val textDisabledAlt: Color,
    val textDark: Color,
    val textGoldDark: Color,
    val buttonUtilityTop: Color,
    val buttonUtilityMid: Color,
    val buttonUtilityBottom: Color,
    val buttonDisabledTop: Color,
    val buttonDisabledMid: Color,
    val buttonDisabledBottom: Color,
    val buttonDimTop: Color,
    val buttonDimMid: Color,
    val buttonDimBottom: Color,
    val borderDisabled: Color,
    val slotLow: Color,
    val white12: Color,
    val white20: Color,
    val ballColors: List<Color>
)

@Composable
private fun fancyPalette(): FancyPalette {
    val white = colorResource(R.color.white)
    val black = colorResource(R.color.black)
    val gold = colorResource(R.color.fb_gold)
    val cyan = colorResource(R.color.fb_cyan)
    val pink = colorResource(R.color.fb_pink)
    val teal = colorResource(R.color.fb_teal)
    val purple = colorResource(R.color.fb_purple)
    val orange = colorResource(R.color.fb_orange)

    return FancyPalette(
        white = white,
        black = black,
        transparent = colorResource(R.color.transparent),
        bgDeep = colorResource(R.color.fb_bg_deep),
        bgStart = colorResource(R.color.fb_bg_start),
        bgMid = colorResource(R.color.fb_bg_mid),
        bgEnd = colorResource(R.color.fb_bg_end),
        surface = colorResource(R.color.fb_surface),
        surfaceAlt = colorResource(R.color.fb_surface_alt),
        surfaceAlt2 = colorResource(R.color.fb_surface_alt_2),
        panelStart = colorResource(R.color.fb_panel_start),
        panelMid = colorResource(R.color.fb_panel_mid),
        panelEnd = colorResource(R.color.fb_panel_end),
        boardStart = colorResource(R.color.fb_board_start),
        boardMid1 = colorResource(R.color.fb_board_mid_1),
        boardMid2 = colorResource(R.color.fb_board_mid_2),
        boardEnd = colorResource(R.color.fb_board_end),
        gold = gold,
        goldLight = colorResource(R.color.fb_gold_light),
        goldBright = colorResource(R.color.fb_gold_bright),
        goldButton = colorResource(R.color.fb_gold_button),
        goldDark = colorResource(R.color.fb_gold_dark),
        cyan = cyan,
        cyanLight = colorResource(R.color.fb_cyan_light),
        pink = pink,
        teal = teal,
        purple = purple,
        purpleLight = colorResource(R.color.fb_purple_light),
        purpleMid = colorResource(R.color.fb_purple_mid),
        purpleDark = colorResource(R.color.fb_purple_dark),
        orange = orange,
        textSecondary = colorResource(R.color.fb_text_secondary),
        textMuted = colorResource(R.color.fb_text_muted),
        textDisabled = colorResource(R.color.fb_text_disabled),
        textDisabledAlt = colorResource(R.color.fb_text_disabled_alt),
        textDark = colorResource(R.color.fb_text_dark),
        textGoldDark = colorResource(R.color.fb_text_gold_dark),
        buttonUtilityTop = colorResource(R.color.fb_button_utility_top),
        buttonUtilityMid = colorResource(R.color.fb_button_utility_mid),
        buttonUtilityBottom = colorResource(R.color.fb_button_utility_bottom),
        buttonDisabledTop = colorResource(R.color.fb_button_disabled_top),
        buttonDisabledMid = colorResource(R.color.fb_button_disabled_mid),
        buttonDisabledBottom = colorResource(R.color.fb_button_disabled_bottom),
        buttonDimTop = colorResource(R.color.fb_button_dim_top),
        buttonDimMid = colorResource(R.color.fb_button_dim_mid),
        buttonDimBottom = colorResource(R.color.fb_button_dim_bottom),
        borderDisabled = colorResource(R.color.fb_border_disabled),
        slotLow = colorResource(R.color.fb_slot_low),
        white12 = colorResource(R.color.fb_white_12),
        white20 = colorResource(R.color.fb_white_20),
        ballColors = listOf(gold, cyan, pink, teal, purple, orange)
    )
}
