package com.example.desktop

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Talks to the same Firestore database as the Android app, over Firestore's REST API.
 *
 * The Firebase Android SDK is Android-only, so on the desktop we speak REST directly and
 * only need a JSON reader. REST has no snapshot listeners, so the console polls — see
 * [OpsConsoleState]. Documents are keyed by the numeric order id exactly as the phone
 * writes them (`orders/{orderId}`, `live_tracking/{orderId}`).
 */
class FirestoreRest(val config: DesktopConfig.Firebase) {

    private val base =
        "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents"

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    // ---------------- Reads ----------------

    suspend fun listOrders(): List<DesktopOrder> = withContext(Dispatchers.IO) {
        listAll("orders").mapNotNull { (docId, fields) -> toOrder(docId, fields) }
            .sortedByDescending { it.createdAt }
    }

    suspend fun listLivePositions(): List<RiderPosition> = withContext(Dispatchers.IO) {
        listAll("live_tracking").mapNotNull { (docId, fields) -> toRiderPosition(docId, fields) }
    }

    // ---------------- Writes ----------------

    /** Updates an order's status (and optionally rider / ETA), leaving other fields untouched. */
    suspend fun updateOrderStatus(
        orderId: Int,
        status: String,
        riderName: String? = null,
        riderEmail: String? = null,
        etaMinutes: Int? = null
    ): Unit = withContext(Dispatchers.IO) {
        val fields = JsonObject().apply {
            add("status", stringValue(status))
            add("updatedAt", integerValue(System.currentTimeMillis()))
            if (riderName != null) add("riderName", stringValue(riderName))
            if (riderEmail != null) add("riderEmail", stringValue(riderEmail))
            if (etaMinutes != null) add("etaMinutes", integerValue(etaMinutes.toLong()))
        }
        val body = JsonObject().apply { add("fields", fields) }

        val masks = buildList {
            add("status")
            add("updatedAt")
            if (riderName != null) add("riderName")
            if (riderEmail != null) add("riderEmail")
            if (etaMinutes != null) add("etaMinutes")
        }.joinToString("&") { "updateMask.fieldPaths=" + encode(it) }

        val url = "$base/orders/$orderId?key=${encode(config.apiKey)}&$masks"
        send(
            HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                .build()
        )
    }

    /** Removes a finished order's live-tracking document, mirroring the rider service. */
    suspend fun clearLiveTracking(orderId: Int): Unit = withContext(Dispatchers.IO) {
        val url = "$base/live_tracking/$orderId?key=${encode(config.apiKey)}"
        try {
            send(
                HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .DELETE()
                    .build()
            )
        } catch (e: IOException) {
            // A missing document is not an error worth surfacing here.
            if (e.message?.contains("404") != true) throw e
        }
    }

    // ---------------- Plumbing ----------------

    private fun listAll(collection: String): List<Pair<String, JsonObject>> {
        val out = mutableListOf<Pair<String, JsonObject>>()
        var pageToken: String? = null
        var pages = 0
        do {
            val url = buildString {
                append(base).append('/').append(collection)
                append("?key=").append(encode(config.apiKey))
                append("&pageSize=300")
                if (pageToken != null) append("&pageToken=").append(encode(pageToken!!))
            }
            val json = JsonParser.parseString(
                send(
                    HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(25))
                        .header("Accept", "application/json")
                        .GET()
                        .build()
                )
            ).asJsonObject

            json.getAsJsonArray("documents")?.forEach { element ->
                val doc = element.asJsonObject
                val name = doc.get("name")?.asString ?: return@forEach
                val docId = name.substringAfterLast('/')
                val fields = doc.getAsJsonObject("fields") ?: JsonObject()
                out += docId to fields
            }

            pageToken = json.get("nextPageToken")?.takeIf { !it.isJsonNull }?.asString
            pages++
        } while (pageToken != null && pages < 20)
        return out
    }

    private fun send(request: HttpRequest): String {
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IOException(
                "Firestore ${request.method()} failed: HTTP ${response.statusCode()} - " +
                    response.body().take(500)
            )
        }
        return response.body()
    }

    private fun encode(raw: String): String = URLEncoder.encode(raw, StandardCharsets.UTF_8)

    private fun stringValue(value: String) = JsonObject().apply { addProperty("stringValue", value) }

    // Firestore transports 64-bit integers as strings.
    private fun integerValue(value: Long) =
        JsonObject().apply { addProperty("integerValue", value.toString()) }

    // ---------------- Field decoding ----------------
    //
    // Firestore wraps every field in its type, e.g. {"stringValue":"Petrol"} or
    // {"integerValue":"12"}. Numbers written as Int arrive as integerValue (a string),
    // Doubles as doubleValue, so each reader accepts both.

    private fun field(fields: JsonObject, name: String): JsonObject? =
        fields.get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun readString(fields: JsonObject, name: String): String? {
        val v = field(fields, name) ?: return null
        return when {
            v.has("stringValue") -> v.get("stringValue").asString
            v.has("integerValue") -> v.get("integerValue").asString
            v.has("doubleValue") -> v.get("doubleValue").asString
            v.has("booleanValue") -> v.get("booleanValue").asString
            else -> null
        }?.takeIf { it.isNotBlank() }
    }

    private fun readDouble(fields: JsonObject, name: String): Double? {
        val v = field(fields, name) ?: return null
        return when {
            v.has("doubleValue") -> v.get("doubleValue").asString.toDoubleOrNull()
            v.has("integerValue") -> v.get("integerValue").asString.toDoubleOrNull()
            v.has("stringValue") -> v.get("stringValue").asString.toDoubleOrNull()
            else -> null
        }
    }

    private fun readLong(fields: JsonObject, name: String): Long? =
        readDouble(fields, name)?.toLong()

    private fun readInt(fields: JsonObject, name: String): Int? =
        readDouble(fields, name)?.toInt()

    private fun toOrder(docId: String, fields: JsonObject): DesktopOrder? {
        val id = docId.toIntOrNull() ?: readInt(fields, "id") ?: return null
        return DesktopOrder(
            id = id,
            customerName = readString(fields, "customerName") ?: "Customer",
            customerPhone = readString(fields, "customerPhone") ?: "",
            customerEmail = readString(fields, "customerEmail") ?: "",
            serviceType = readString(fields, "serviceType") ?: "Petrol",
            quantity = readInt(fields, "quantity") ?: 1,
            totalPrice = readDouble(fields, "totalPrice") ?: 0.0,
            deliveryAddress = readString(fields, "deliveryAddress") ?: "Lahore",
            paymentMethod = readString(fields, "paymentMethod") ?: "Cash on Delivery",
            status = readString(fields, "status") ?: "Pending",
            riderName = readString(fields, "riderName"),
            riderEmail = readString(fields, "riderEmail"),
            createdAt = readLong(fields, "createdAt") ?: 0L,
            etaMinutes = readInt(fields, "etaMinutes") ?: 30,
            destLat = readDouble(fields, "destLat"),
            destLng = readDouble(fields, "destLng"),
            originLat = readDouble(fields, "originLat"),
            originLng = readDouble(fields, "originLng")
        )
    }

    private fun toRiderPosition(docId: String, fields: JsonObject): RiderPosition? {
        val lat = readDouble(fields, "lat") ?: return null
        val lng = readDouble(fields, "lng") ?: return null
        if (lat == 0.0 && lng == 0.0) return null
        val orderId = readInt(fields, "orderId") ?: docId.toIntOrNull() ?: return null
        return RiderPosition(
            orderId = orderId,
            riderEmail = readString(fields, "riderEmail") ?: "",
            lat = lat,
            lng = lng,
            bearing = (readDouble(fields, "bearing") ?: 0.0).toFloat(),
            speedKmh = (readDouble(fields, "speedKmh") ?: 0.0).toFloat(),
            status = readString(fields, "status") ?: "",
            updatedAt = readLong(fields, "updatedAt") ?: 0L
        )
    }
}
