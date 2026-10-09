# 08. Post-Delivery Rating & Driver Feedback Documentation ⭐

> **Current App Version:** `2.6.4.0.0.26 (Build 54)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

## 📌 Category
**Customer Feedback & Quality Assurance**

## 🎯 Purpose & Overview
The Post-Delivery Rating system empowers customers to rate their doorstep fuel delivery experience on a 1-5 star scale, select quick compliment chips, and provide driver feedback notes upon order completion.

---

## ⚙️ Key Rating Functions & ViewModel Methods
* `submitOrderRating(orderId, rating, feedback)` (`MainViewModel.kt`): Validates rating (1-5) and feedback via `SecurityInputValidator`, applies rate limits, updates `OrderEntity` with rating & feedback, and logs audit record.
* <font color="#10b981"><b>`PostDeliveryRatingCard`</b></font> (`Screens.kt`): Interactive Jetpack Compose component embedded in live tracking and order history screens.

---

## 🎨 UI Components & Design Features
1. <font color="#10b981"><b>Interactive 1-5 Star Bar</b></font>: Spring-animated star buttons with dynamic scaling (`testTag`: `rating_star_1` to `rating_star_5`).
2. **Star Rating Labels**:
   * 1 Star: "Poor Service 😞"
   * 2 Stars: "Below Expectations 😐"
   * 3 Stars: "Fair Experience 🙂"
   * 4 Stars: "Great Delivery! 😊"
   * 5 Stars: "Exceptional Experience! 🌟"
3. <font color="#10b981"><b>Quick Compliment Chips (`FlowRow`, `FilterChip`)</b></font>:
   * `⏱️ On-Time Arrival`
   * `⛽ Pure Fuel Quality`
   * `👨‍✈️ Courteous Driver`
   * `🛡️ Safety Followed`
   * `💵 Exact Change`
4. **Driver Feedback Text Field (`rating_feedback_input`)**: Optional feedback text field up to 500 characters.
5. **Verified Review Display**: Converts rating card into a verified badge post-submission.

---

## 📁 Source Locations
* **UI Component**: `app/src/main/java/com/example/ui/Screens.kt` (`PostDeliveryRatingCard`)
* **State Management**: `app/src/main/java/com/example/ui/MainViewModel.kt` (`submitOrderRating`)
* **Security Validation**: `app/src/main/java/com/example/security/SecurityInputValidator.kt` (`validateRatingAndFeedback`)
* **Data Fields**: `app/src/main/java/com/example/data/Models.kt` (`OrderEntity.rating`, `OrderEntity.feedback`)

---

## 🔄 Rating Flow
1. **Completion**: Driver marks order `Completed`.
2. **Card Activation**: `PostDeliveryRatingCard` automatically displays on tracking card or order history.
3. **Rating Selection**: User selects 1-5 stars and taps compliment chips (e.g., "Pure Fuel Quality").
4. **Submission**: Taps `Submit Rating & Review` (`submit_rating_button`), persisting rating to Room DB and displaying verified badge.
