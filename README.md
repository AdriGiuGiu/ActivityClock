# ActivityClock

ActivityClock is a beautiful, intuitive time-tracking Android application built with Jetpack Compose. It allows you to effortlessly track how much time you spend on different activities throughout your day, view detailed analytics, and look back through your history.

## Features

- **⏱️ Live Time Tracking:** Start and stop tracking with a single tap. A beautiful radial gradient UI shows your currently active session and elapsed time.
- **📊 Detailed Analytics:** View your time distribution through a Donut Chart and a Ranking Bar Chart. You can filter your stats by Today, Week, Month, or All-time.
- **📜 Session Timeline:** Look back at your recent activity history with an intuitive, color-coded timeline.
- **🎨 Custom Activities:** Create custom activity types and assign vibrant accent colors to personalize your tracking experience.

## Tech Stack

This project is built using modern Android development practices:
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose & Material 3
- **Architecture:** MVVM (Model-View-ViewModel)
- **Concurrency:** Kotlin Coroutines & Flow for asynchronous data streams
- **Database:** Local SQLite with optimized aggregation queries

## Installation

You can install the app directly on your Android device:
1. Download the [`ActivityClock.apk`](ActivityClock.apk) file from this repository.
2. Transfer it to your Android device (or open GitHub on your phone and download it).
3. Tap the file to install it. *(You may need to enable "Install from unknown sources" in your Android settings)*.

## Running the Project

1. Clone this repository.
2. Open the project in **Android Studio**.
3. Sync the project with Gradle files.
4. Run the app on an Android emulator or a physical device (`Shift + F10`).

## Database Optimizations
This app uses a highly optimized local SQLite database implementation:
- Aggregations (like computing the sum of overlapping durations for analytics) are performed directly using SQL scalar functions (`MAX`, `MIN`, `SUM`) rather than being evaluated in-memory.
- Proper database indexing (`start_time`, `end_time`, `activity_id`) ensures fast lookups even as the timeline grows.
- ViewModels fetch data concurrently to provide a seamless and fast UI experience upon startup or when switching tabs.
