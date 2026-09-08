package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZyphuelBlueDark
import com.example.ui.theme.ZyphuelBluePrimary
import kotlin.math.roundToInt

/**
 * Interactive spotlight / coach-mark guided tour.
 *
 * Unlike a plain text walkthrough, this dims the screen, cuts a highlight window around a REAL
 * on-screen element (measured live via [spotlightAnchor]) and floats a callout bubble next to it
 * with Step X of N + Skip / Back / Next. Steps may run a suspending [SpotlightStep.beforeShow]
 * side-effect first (e.g. open the navigation drawer) so the tour visibly "takes the user to"
 * each feature.
 */
data class SpotlightStep(
    /** Anchor key registered via [Modifier.spotlightAnchor]; null renders a centered informational callout. */
    val anchorKey: String?,
    val title: String,
    val subtitle: String? = null,
    val body: String,
    val tip: String? = null,
    val badge: String? = null,
    val icon: ImageVector? = null,
    /** Full dark scrim when true; a lighter scrim when false (e.g. to keep an opened drawer visible). */
    val dimBackground: Boolean = true,
    /** Suspending action run before this step is revealed (drawer open/close, scroll, …). */
    val beforeShow: (suspend () -> Unit)? = null
)

/** Hoisted state for a running spotlight tour. Create with [rememberSpotlightState]. */
class SpotlightState {
    /** Live bounds (in root/window coordinates) of every registered anchor. */
    val anchors = mutableStateMapOf<String, Rect>()

    var isActive by mutableStateOf(false)
        private set
    var index by mutableIntStateOf(0)
        private set
    var steps by mutableStateOf<List<SpotlightStep>>(emptyList())
        private set

    /** Set by the host's sequencing effect once [SpotlightStep.beforeShow] ran and the anchor is measured. */
    var revealReady by mutableStateOf(false)

    val current: SpotlightStep? get() = steps.getOrNull(index)
    val count: Int get() = steps.size
    val isLast: Boolean get() = steps.isNotEmpty() && index == steps.lastIndex

    fun start(newSteps: List<SpotlightStep>) {
        if (newSteps.isEmpty()) return
        steps = newSteps
        index = 0
        revealReady = false
        isActive = true
    }

    fun next() {
        if (index < steps.lastIndex) {
            index += 1
            revealReady = false
        }
    }

    fun back() {
        if (index > 0) {
            index -= 1
            revealReady = false
        }
    }

    fun jumpTo(targetIndex: Int) {
        if (targetIndex in steps.indices && targetIndex != index) {
            index = targetIndex
            revealReady = false
        }
    }

    fun stop() {
        isActive = false
        revealReady = false
        index = 0
        steps = emptyList()
    }
}

@Composable
fun rememberSpotlightState(): SpotlightState = remember { SpotlightState() }

/**
 * Registers this element's live bounds under [key] so a [SpotlightStep] with the matching
 * `anchorKey` can highlight it. Uses [boundsInRoot] so all anchors and the overlay share one
 * coordinate space regardless of the app bar / lazy list / drawer they live in.
 */
fun Modifier.spotlightAnchor(state: SpotlightState, key: String): Modifier =
    this.onGloballyPositioned { coords ->
        if (coords.isAttached) state.anchors[key] = coords.boundsInRoot()
    }

/**
 * Draws the spotlight scrim + highlight ring + callout for the active step. Renders nothing when
 * the tour is inactive. Must live inside the same root Box as the anchored elements so the
 * measured bounds line up.
 */
@Composable
fun SpotlightOverlay(
    state: SpotlightState,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    if (!state.isActive) return
    val step = state.current ?: return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()

        val padPx = with(density) { 8.dp.toPx() }
        val gapPx = with(density) { 14.dp.toPx() }
        val marginPx = with(density) { 16.dp.toPx() }
        val cornerPx = with(density) { 16.dp.toPx() }
        val ringStrokePx = with(density) { 3.dp.toPx() }

        val anchor: Rect? = step.anchorKey?.let { state.anchors[it] }
        val target: Rect? = if (state.revealReady && anchor != null && !anchor.isEmpty) {
            Rect(
                left = (anchor.left - padPx).coerceAtLeast(0f),
                top = (anchor.top - padPx).coerceAtLeast(0f),
                right = (anchor.right + padPx).coerceAtMost(wPx),
                bottom = (anchor.bottom + padPx).coerceAtMost(hPx)
            )
        } else null

        val scrimColor = Color.Black.copy(alpha = if (step.dimBackground) 0.74f else 0.38f)

        val infinite = rememberInfiniteTransition(label = "spotlight_pulse")
        val ringAlpha by infinite.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(850, easing = LinearEasing), RepeatMode.Reverse),
            label = "spotlight_ring_alpha"
        )

        // Scrim + ring. The Canvas also swallows taps so the underlying UI can't be clicked through.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                }
        ) {
            if (target == null) {
                drawRect(color = scrimColor, size = size)
            } else {
                // Four rectangles framing the target — a reliable cutout without offscreen compositing.
                drawRect(scrimColor, topLeft = Offset(0f, 0f), size = Size(size.width, target.top))
                drawRect(scrimColor, topLeft = Offset(0f, target.bottom), size = Size(size.width, size.height - target.bottom))
                drawRect(scrimColor, topLeft = Offset(0f, target.top), size = Size(target.left, target.height))
                drawRect(scrimColor, topLeft = Offset(target.right, target.top), size = Size(size.width - target.right, target.height))
                drawRoundRect(
                    color = Color.White.copy(alpha = ringAlpha),
                    topLeft = target.topLeft,
                    size = target.size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = ringStrokePx)
                )
            }
        }

        // ---- Callout placement ----
        val calloutWidthDp = if (maxWidth - 32.dp <= 380.dp) maxWidth - 32.dp else 380.dp
        val calloutWidthPx = with(density) { calloutWidthDp.toPx() }
        var calloutHeightPx by remember { mutableIntStateOf(0) }

        val xRaw: Float
        val yRaw: Float
        if (target == null) {
            xRaw = (wPx - calloutWidthPx) / 2f
            yRaw = (hPx - calloutHeightPx) / 2f
        } else {
            xRaw = target.center.x - calloutWidthPx / 2f
            val fitsBelow = target.bottom + gapPx + calloutHeightPx + marginPx <= hPx
            yRaw = if (fitsBelow) target.bottom + gapPx else target.top - gapPx - calloutHeightPx
        }
        val x = xRaw.coerceIn(marginPx, (wPx - calloutWidthPx - marginPx).coerceAtLeast(marginPx))
        val y = yRaw.coerceIn(marginPx, (hPx - calloutHeightPx - marginPx).coerceAtLeast(marginPx))

        val animatedOffset by animateIntOffsetAsState(
            targetValue = IntOffset(x.roundToInt(), y.roundToInt()),
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
            label = "spotlight_callout_offset"
        )

        Card(
            modifier = Modifier
                .width(calloutWidthDp)
                .offset { animatedOffset }
                .onSizeChanged { calloutHeightPx = it.height }
                .testTag("spotlight_callout"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column {
                // Top Linear Animated Progress Bar
                val targetProgress = if (state.count > 0) (state.index + 1).toFloat() / state.count else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    label = "spotlight_progress"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = ZyphuelBluePrimary,
                    trackColor = Color(0xFFF1F5F9)
                )

                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    // Header Row: Category Badge + Progress Text + Skip Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ZyphuelBluePrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = step.badge ?: "STEP ${state.index + 1} OF ${state.count}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ZyphuelBluePrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.8.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "Step ${state.index + 1}/${state.count}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.testTag("spotlight_skip_btn"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Skip Tour ✕", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontWeight = FontWeight.SemiBold))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Animated Step Content (Icon + Title + Subtitle + Body + Pro Tip)
                    AnimatedContent(
                        targetState = state.index,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeIn(animationSpec = tween(220)))
                                    .togetherWith(
                                        slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> -width / 3 } + fadeOut(animationSpec = tween(180))
                                    )
                            } else {
                                (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> -width / 3 } + fadeIn(animationSpec = tween(220)))
                                    .togetherWith(
                                        slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeOut(animationSpec = tween(180))
                                    )
                            }
                        },
                        label = "spotlight_step_content_anim"
                    ) { currentIdx ->
                        val item = state.steps.getOrNull(currentIdx) ?: return@AnimatedContent
                        Column {
                            Row(verticalAlignment = Alignment.Top) {
                                if (item.icon != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .background(ZyphuelBluePrimary.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = ZyphuelBluePrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = ZyphuelBlueDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    if (!item.subtitle.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = ZyphuelBluePrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.body,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFF334155),
                                            lineHeight = 20.sp
                                        )
                                    )
                                }
                            }

                            if (!item.tip.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "💡",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = item.tip,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF475569),
                                                fontWeight = FontWeight.Normal,
                                                lineHeight = 16.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Bottom Navigation Bar: Clickable Jump Dots + Back Button + Next / Finish Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Interactive Step Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(state.count) { i ->
                                val isCurrent = (i == state.index)
                                Box(
                                    modifier = Modifier
                                        .height(7.dp)
                                        .width(if (isCurrent) 18.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) ZyphuelBluePrimary else Color(0xFFCBD5E1))
                                        .clickable { state.jumpTo(i) }
                                        .testTag("spotlight_step_dot_$i")
                                )
                            }
                        }

                        // Navigation Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.index > 0) {
                                OutlinedButton(
                                    onClick = { state.back() },
                                    modifier = Modifier.height(38.dp).testTag("spotlight_back_btn"),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZyphuelBluePrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Back", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                            Button(
                                onClick = { if (state.isLast) onFinish() else state.next() },
                                modifier = Modifier.height(38.dp).testTag("spotlight_next_btn"),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (state.isLast) "Start Exploring 🚀" else "Next ➔",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
