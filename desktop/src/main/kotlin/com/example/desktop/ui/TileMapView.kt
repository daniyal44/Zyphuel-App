package com.example.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sin

/** A point drawn on the map. [bearing] (degrees) draws a heading arrow, for vehicles. */
data class MapMarker(
    val position: LatLng,
    val color: Color,
    val label: String,
    val bearing: Float? = null,
    val radiusPx: Float = 9f
)

private const val TILE = 256.0
private const val MIN_ZOOM = 3
private const val MAX_ZOOM = 17

/**
 * A real slippy map for the desktop console.
 *
 * Compose Desktop has no WebView, so instead of embedding Leaflet (as the Android app does)
 * this fetches OpenStreetMap raster tiles over HTTP and composites them in a Canvas, then
 * draws the route and markers on top. The viewport auto-fits every supplied point, so the
 * rider and the destination are always both visible without any manual panning.
 */
@Composable
fun TileMapView(
    markers: List<MapMarker>,
    modifier: Modifier = Modifier,
    route: List<LatLng> = emptyList()
) {
    val tiles = remember { mutableStateMapOf<String, ImageBitmap>() }
    val requested = remember { mutableSetOf<String>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var manualZoomOffset by remember { mutableStateOf(0) }
    var manualCenter by remember { mutableStateOf<LatLng?>(null) }

    val allPoints = remember(markers, route) { markers.map { it.position } + route }

    // Reset manual overrides when points change significantly (e.g. selected order changes)
    LaunchedEffect(markers.firstOrNull()?.position) {
        manualZoomOffset = 0
        manualCenter = null
    }

    val view = remember(allPoints, canvasSize, manualZoomOffset, manualCenter) {
        computeViewport(allPoints, canvasSize, manualZoomOffset, manualCenter)
    }

    val visibleTiles = remember(view, canvasSize) { view?.visibleTiles(canvasSize) ?: emptyList() }

    LaunchedEffect(visibleTiles) {
        visibleTiles.forEach { tile ->
            val key = tile.key
            if (!tiles.containsKey(key) && requested.add(key)) {
                launch(Dispatchers.IO) {
                    val bitmap = TileLoader.load(tile.zoom, tile.x, tile.y)
                    if (bitmap != null) tiles[key] = bitmap else requested.remove(key)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE8EEF4))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
        ) {
            val viewport = view ?: return@Canvas

            visibleTiles.forEach { tile ->
                tiles[tile.key]?.let { bitmap ->
                    drawImage(bitmap, topLeft = Offset(tile.screenX, tile.screenY))
                }
            }

            if (route.size >= 2) {
                val path = Path()
                route.forEachIndexed { index, point ->
                    val (x, y) = viewport.toScreen(point)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = Color(0xFF0284C7),
                    style = Stroke(
                        width = 5f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    )
                )
            }

            markers.forEach { marker ->
                val (x, y) = viewport.toScreen(marker.position)
                drawCircle(Color.White, radius = marker.radiusPx + 3f, center = Offset(x, y))
                drawCircle(marker.color, radius = marker.radiusPx, center = Offset(x, y))

                marker.bearing?.let { bearing ->
                    val radians = Math.toRadians(bearing.toDouble() - 90.0)
                    val tip = Offset(
                        x + (cos(radians) * (marker.radiusPx + 13f)).toFloat(),
                        y + (sin(radians) * (marker.radiusPx + 13f)).toFloat()
                    )
                    val leftAngle = radians + 2.5
                    val rightAngle = radians - 2.5
                    val arrow = Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(
                            x + (cos(leftAngle) * (marker.radiusPx + 3f)).toFloat(),
                            y + (sin(leftAngle) * (marker.radiusPx + 3f)).toFloat()
                        )
                        lineTo(
                            x + (cos(rightAngle) * (marker.radiusPx + 3f)).toFloat(),
                            y + (sin(rightAngle) * (marker.radiusPx + 3f)).toFloat()
                        )
                        close()
                    }
                    drawPath(arrow, marker.color)
                }
            }
        }

        // Floating interactive map controls (top-right)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Zoom in
            MapControlButton(
                icon = "➕",
                tag = "btn_map_zoom_in",
                onClick = {
                    if (manualZoomOffset < 5) manualZoomOffset++
                }
            )
            // Zoom out
            MapControlButton(
                icon = "➖",
                tag = "btn_map_zoom_out",
                onClick = {
                    if (manualZoomOffset > -5) manualZoomOffset--
                }
            )
            // Reset / Auto-fit
            MapControlButton(
                icon = "🎯",
                tag = "btn_map_recenter",
                onClick = {
                    manualZoomOffset = 0
                    manualCenter = null
                }
            )
            // Focus on Depot
            MapControlButton(
                icon = "🏢",
                tag = "btn_map_focus_depot",
                onClick = {
                    manualCenter = LatLng(com.example.desktop.DEPOT_LAT, com.example.desktop.DEPOT_LNG)
                }
            )
            // Focus on Rider if present
            val riderMarker = markers.firstOrNull { it.label.startsWith("Rider") }
            if (riderMarker != null) {
                MapControlButton(
                    icon = "🏍️",
                    tag = "btn_map_focus_rider",
                    onClick = {
                        manualCenter = riderMarker.position
                    }
                )
            }
        }

        // Legend: Canvas text needs a TextMeasurer, so labels live in real composables.
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            markers.forEach { marker ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0xE60F172A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(marker.color, RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = marker.label,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Text(
            text = "© OpenStreetMap",
            color = Color(0xFF475569),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color(0xCCFFFFFF), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun MapControlButton(
    icon: String,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE60F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, fontSize = 12.sp)
    }
}


// ---------------- Web Mercator plumbing ----------------

private data class VisibleTile(
    val zoom: Int,
    val x: Int,
    val y: Int,
    val screenX: Float,
    val screenY: Float
) {
    val key: String get() = "$zoom/$x/$y"
}

/** Zoom level plus the world-pixel origin of the viewport's top-left corner. */
private class Viewport(val zoom: Int, val originX: Double, val originY: Double) {

    fun toScreen(point: LatLng): Pair<Float, Float> {
        val (x, y) = worldPx(point.lat, point.lng, zoom)
        return (x - originX).toFloat() to (y - originY).toFloat()
    }

    fun visibleTiles(size: IntSize): List<VisibleTile> {
        if (size.width <= 0 || size.height <= 0) return emptyList()
        val maxIndex = (1 shl zoom) - 1
        val firstX = floor(originX / TILE).toInt()
        val lastX = floor((originX + size.width) / TILE).toInt()
        val firstY = floor(originY / TILE).toInt()
        val lastY = floor((originY + size.height) / TILE).toInt()

        val out = mutableListOf<VisibleTile>()
        for (tx in firstX..lastX) {
            for (ty in firstY..lastY) {
                if (tx < 0 || ty < 0 || tx > maxIndex || ty > maxIndex) continue
                out += VisibleTile(
                    zoom = zoom,
                    x = tx,
                    y = ty,
                    screenX = (tx * TILE - originX).toFloat(),
                    screenY = (ty * TILE - originY).toFloat()
                )
            }
        }
        return out
    }
}

private fun worldPx(lat: Double, lng: Double, zoom: Int): Pair<Double, Double> {
    val scale = TILE * (1 shl zoom)
    val x = (lng + 180.0) / 360.0 * scale
    val clampedSin = sin(Math.toRadians(lat)).coerceIn(-0.9999, 0.9999)
    val y = (0.5 - ln((1 + clampedSin) / (1 - clampedSin)) / (4 * PI)) * scale
    return x to y
}

/** Picks the highest zoom at which every point still fits, then centres on them (with manual offset/center support). */
private fun computeViewport(
    points: List<LatLng>,
    size: IntSize,
    zoomOffset: Int = 0,
    customCenter: LatLng? = null
): Viewport? {
    if (size.width <= 0 || size.height <= 0) return null
    val usable = points.filter { it.lat != 0.0 || it.lng != 0.0 }
    if (usable.isEmpty()) {
        val targetCenter = customCenter ?: LatLng(31.5204, 74.3587)
        val targetZoom = (12 + zoomOffset).coerceIn(MIN_ZOOM, MAX_ZOOM)
        return viewportCentredOn(targetCenter, targetZoom, size)
    }

    val minLat = usable.minOf { it.lat }
    val maxLat = usable.maxOf { it.lat }
    val minLng = usable.minOf { it.lng }
    val maxLng = usable.maxOf { it.lng }
    val centre = customCenter ?: LatLng((minLat + maxLat) / 2.0, (minLng + maxLng) / 2.0)

    if (usable.size == 1) {
        val targetZoom = (14 + zoomOffset).coerceIn(MIN_ZOOM, MAX_ZOOM)
        return viewportCentredOn(centre, targetZoom, size)
    }

    var baseZoom = MIN_ZOOM
    for (zoom in MAX_ZOOM downTo MIN_ZOOM) {
        val (x1, y1) = worldPx(maxLat, minLng, zoom)
        val (x2, y2) = worldPx(minLat, maxLng, zoom)
        val spanX = kotlin.math.abs(x2 - x1)
        val spanY = kotlin.math.abs(y2 - y1)
        if (spanX < size.width * 0.82 && spanY < size.height * 0.82) {
            baseZoom = zoom
            break
        }
    }
    val targetZoom = (baseZoom + zoomOffset).coerceIn(MIN_ZOOM, MAX_ZOOM)
    return viewportCentredOn(centre, targetZoom, size)
}

private fun viewportCentredOn(centre: LatLng, zoom: Int, size: IntSize): Viewport {
    val (cx, cy) = worldPx(centre.lat, centre.lng, zoom)
    return Viewport(zoom, cx - size.width / 2.0, cy - size.height / 2.0)
}

private object TileLoader {

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun load(zoom: Int, x: Int, y: Int): ImageBitmap? = try {
        val request = HttpRequest.newBuilder(URI.create("https://tile.openstreetmap.org/$zoom/$x/$y.png"))
            .timeout(Duration.ofSeconds(15))
            // OpenStreetMap's tile usage policy requires an identifying User-Agent.
            .header("User-Agent", "ZyphuelOpsConsole/1.0 (fuel delivery dispatch console)")
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() !in 200..299) {
            null
        } else {
            ImageIO.read(ByteArrayInputStream(response.body()))?.toComposeImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}
