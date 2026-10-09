package com.example.util

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Manages official doorstep delivery operating hours for Zyphuel.
 *
 * Official Operating Schedule (Pakistan Standard Time, Asia/Karachi):
 * - Monday to Thursday: 08:00 AM – 08:00 PM (8:00 - 20:00)
 * - Friday:             08:00 AM – 01:00 PM (8:00 - 13:00)
 * - Saturday & Sunday:  10:00 AM – 06:00 PM (10:00 - 18:00)
 *
 * Outside these operating hours, orders cannot be placed and order actions are hidden in the UI.
 */
object DeliveryOperatingHoursManager {

    val PAKISTAN_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Karachi")

    const val SCHEDULE_MON_THU = "Mon–Thu: 08:00 AM – 08:00 PM"
    const val SCHEDULE_FRI = "Fri: 08:00 AM – 01:00 PM"
    const val SCHEDULE_WEEKEND = "Sat–Sun: 10:00 AM – 06:00 PM"

    data class DeliveryStatus(
        val isOpen: Boolean,
        val statusTitle: String,
        val statusSubtitle: String,
        val activeScheduleText: String
    )

    /**
     * Evaluates whether the doorstep delivery service is currently open.
     */
    fun isDeliveryOpen(calendar: Calendar = Calendar.getInstance(PAKISTAN_TIMEZONE)): Boolean {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val minutesOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        return when (dayOfWeek) {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY -> {
                // 08:00 AM (480 mins) to 08:00 PM (1200 mins)
                minutesOfDay in 480 until 1200
            }
            Calendar.FRIDAY -> {
                // 08:00 AM (480 mins) to 01:00 PM (780 mins)
                minutesOfDay in 480 until 780
            }
            Calendar.SATURDAY, Calendar.SUNDAY -> {
                // 10:00 AM (600 mins) to 06:00 PM (1080 mins)
                minutesOfDay in 600 until 1080
            }
            else -> false
        }
    }

    /**
     * Returns structured status details for UI banners and feedback.
     */
    fun getDeliveryStatus(calendar: Calendar = Calendar.getInstance(PAKISTAN_TIMEZONE)): DeliveryStatus {
        val open = isDeliveryOpen(calendar)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val activeSchedule = when (dayOfWeek) {
            Calendar.FRIDAY -> SCHEDULE_FRI
            Calendar.SATURDAY, Calendar.SUNDAY -> SCHEDULE_WEEKEND
            else -> SCHEDULE_MON_THU
        }

        val title = if (open) "Deliveries Open" else "Deliveries Closed"
        val subtitle = if (open) {
            "Accepting orders now ($activeSchedule)"
        } else {
            "Orders paused outside operating hours ($activeSchedule)"
        }

        return DeliveryStatus(
            isOpen = open,
            statusTitle = title,
            statusSubtitle = subtitle,
            activeScheduleText = activeSchedule
        )
    }

    /**
     * Returns comprehensive operating hours string.
     */
    fun getFullScheduleSummary(): String {
        return "$SCHEDULE_MON_THU • $SCHEDULE_FRI • $SCHEDULE_WEEKEND"
    }
}
