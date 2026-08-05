# Stretch Timer App 📱
A lightweight **Android** utility that helps you perform stretch intervals. You can configure the number of rounds, the length of each round, and an optional break between rounds. The app displays a countdown timer, updates the action‑bar title, and plays a notification sound when a round or break ends.

## Key Features
- ✅ **Configurable rounds** – set total rounds, round duration and break time.
- ⏱️ **Live countdown** with the current round number shown in the ActionBar.
- 🔔 **Reliable notifications** using a foreground service, `AlarmManager.setAlarmClock`, and `USAGE_ALARM` audio attributes.
- 📦 **State persistence** via `ViewModel` ensuring UI survives configuration changes.
- 📱 **Portrait‑only UI** for a stable experience.
- 🌐 **Multilingual support** – strings are externalized and translated for English, Spanish, Italian, German, French, Chinese, and Japanese.
- 🎨 **High‑contrast colors** meeting WCAG accessibility standards.
  
## Architecture
- **MainActivity** – hosts the Navigation component.
- **FirstFragment** – collects user input and observes timer data.
- **RunningFragment** – shows the active timer, pause state and remaining rounds.
- **TimerViewModel** – bridges UI and `TimerService`.
- **TimerService** – foreground service that drives the timer logic with `AlarmManager`.
- **Resources** – all UI strings are stored in `res/values/strings.xml` and locale‑specific folders.

## Localization
The app ships with the following language packs:
- 🇺🇸 English (default)
- 🇪🇸 Spanish (`values-es`)
- 🇮🇹 Italian (`values-it`)
- 🇩🇪 German (`values-de`)
- 🇫🇷 French (`values-fr`)
- 🇨🇳 Chinese (`values-zh`)
- 🇯🇵 Japanese (`values-ja`)

Add more translations by creating a new `values-xx` folder and providing matching string resources.

## Build & Run
1. Open the project in **Android Studio**.
2. Ensure the Android SDK (API 34 recommended) is installed.
3. Sync Gradle and run the app on an emulator or device.
4. Grant **Notification** permission on Android 13+ and allow the app to use `WAKE_LOCK` and `USE_EXACT_ALARM`.

## License
This project is licensed under the **MIT License**.
