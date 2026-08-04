# Stretch Timer App

## Overview

A simple Android app that helps you perform stretch intervals. It allows you to set the number of rounds, the duration of each round, and an optional intermediate break between rounds. The app displays a countdown timer and plays a notification sound when a round or break ends.

## Key Features
- Configurable total rounds, round time, and intermediate break time.
- Visual countdown with current round display.
- Audible notification using high-priority audio attributes (`USAGE_ALARM`).
- **Guaranteed Background Execution:** Uses a **Foreground Service** combined with **AlarmManager.setAlarmClock** and **WakeLock** to ensure the timer advances and sounds play even when the screen is off or the phone is locked.
- **State Persistence:** Uses `ViewModel` to persist user inputs and sync with the background service across configuration changes or app backgrounding.
- **Portrait Optimized:** The application is locked to portrait orientation for a stable and predictable user experience.
- Clean handling of resources (`MediaPlayer`, `WakeLock`, and Foreground Service lifecycle).

## Screenshots
![App Screenshot](front.png)

## Architecture
- **MainActivity** – Hosts the navigation component.
- **FirstFragment** – Handles the UI and observes timer data from the `TimerService` via the `TimerViewModel`.
- **TimerViewModel** – Manages the binding to the `TimerService` and preserves user input state.
- **TimerService** – A Foreground Service that handles the actual timer logic using `AlarmManager` for high-precision background execution.
- Layouts are defined using View Binding.

## Build & Run
1. Open the project in Android Studio.
2. Ensure you have the required Android SDK (API 34 recommended).
3. Sync Gradle, then run the app on an emulator or physical device.
4. The app requires **Notification permissions** (on Android 13+) and uses `WAKE_LOCK` and `USE_EXACT_ALARM` to function correctly in the background.

## Code Improvements Implemented
- **Advanced Background Strategy**: Integrated `AlarmManager.setAlarmClock` for the most reliable wakeups on modern Android versions, ensuring transitions happen while the device is in deep sleep.
- **Audio Reliability**: Configured `MediaPlayer` with `setWakeMode` and `USAGE_ALARM` to ensure notifications are heard even if the device is locked or CPU is throttled.
- **Portrait Lock**: Simplified the layout and locked the Manifest to portrait mode, removing unnecessary orientation-handling complexity.
- **ViewModel Sync**: Decoupled UI state from the background service, allowing the timer progress to remain visible and synced across lifecycle events.

## License
This project is licensed under the MIT License.
