package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val body: String,
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

        val rawAnchor: Rect? = step.anchorKey?.let { state.anchors[it] }
        val hasCutout = state.revealReady && rawAnchor != null && !rawAnchor.isEmpty
        val target: Rect? = if (hasCutout && rawAnchor != null) {
            Rect(
                left = (rawAnchor.left - padPx).coerceAtLeast(0f),
                top = (rawAnchor.top - padPx).coerceAtLeast(0f),
                right = (rawAnchor.right + padPx).coerceAtMost(wPx),
                bottom = (rawAnchor.bottom + padPx).coerceAtMost(hPx)
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
        val calloutWidthDp = if (maxWidth - 32.dp <= 360.dp) maxWidth - 32.dp else 360.dp
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
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STEP ${state.index + 1} OF ${state.count}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ZyphuelBluePrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.testTag("spotlight_skip_btn"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Skip", style = MaterialTheme.typography.labelMedium.copy(color = Color.Gray))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.Top) {
                    if (step.icon != null) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(ZyphuelBluePrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = ZyphuelBluePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = ZyphuelBlueDark,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = step.body,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.DarkGray,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Progress dots
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(state.count) { i ->
                            Box(
                                modifier = Modifier
                                    .size(width = if (i == state.index) 18.dp else 7.dp, height = 7.dp)
                                    .background(
                                        if (i == state.index) ZyphuelBluePrimary else Color.LightGray,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.index > 0) {
                            OutlinedButton(
                                onClick = { state.back() },
                                modifier = Modifier.height(38.dp).testTag("spotlight_back_btn"),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ZyphuelBluePrimary)
                            ) {
                                Text("Back", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Button(
                            onClick = { if (state.isLast) onFinish() else state.next() },
                            modifier = Modifier.height(38.dp).testTag("spotlight_next_btn"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
                        ) {
                            Text(
                                text = if (state.isLast) "Done" else "Next",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}
