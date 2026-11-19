# Baby – Voice Controlled Call Assistant (Android)

An Android Jetpack Compose application that lets users manage incoming calls hands-free using natural **Hindi**, **English**, and **Hinglish** voice commands.

## Features

- **Multilingual Voice Command Engine** (`VoiceCommandClassifier`):
  - **Answer**: `"Baby, call attend karo."`, `"Baby, call utha lo."`, `"Baby, answer the call."`
  - **Do Not Answer**: `"Baby, call attend mat karo."` (negation prioritized before answer)
  - **Reject**: `"Baby, call reject karo."`
  - **Speaker Control**: `"Baby, speaker on karo."`, `"Baby, speaker off karo."`
  - **Microphone Mute**: `"Baby, mute karo."`, `"Baby, mute hatao."`
  - Includes optional `"Baby"` wake-word requirement, noise confidence filtering, duplicate-command cooldown debouncing, and confirmation prompts (`"Haan / Naa"`) for low-confidence call-altering commands.
- **Native Android Integration**:
  - `SpeechRecognizer` for speech-to-text with live partial transcripts and RMS waveform levels.
  - `TextToSpeech` for Hindi/Hinglish/English spoken feedback (with anti-echo mic pausing).
  - `TelecomManager` (`acceptRingingCall()` on API 26+, `endCall()` on API 28+), `CallScreeningService` (API 29+), `TelephonyCallback` (API 31+) / `PhoneStateListener` (API 24–30), and `AudioManager` communication-device routing.
  - `CallAssistantForegroundService` (`foregroundServiceType="microphone"`) automatically activates `SpeechRecognizer` as soon as an incoming `RINGING` call state is detected (with continuous ring re-arming and Android 14+ notification fallback—no tap-to-speak needed).
- **UI & Screens**:
  - **Home Dashboard**: Master Voice Assistant toggle, Auto-Mic incoming call status, animated mic orb, command cheat-sheet, and recent activity.
  - **Test Lab**: Simulate incoming calls (with automatic mic start) and test all voice commands (via live mic, presets, or custom phrases) without placing cellular calls.
  - **History**: Local Room database log of recognized commands and honest execution outcomes (no audio recordings stored or uploaded).
  - **Permission Setup Wizard**: Step-by-step interactive onboarding explaining Microphone, Phone State, Call Control, Call Screening Role, Notifications, and OEM battery rules before requesting.
  - **Settings**: Language selector (`Hinglish`, `Hindi`, `English`), auto-start mic on incoming call & continuous ring listening toggles, wake-word toggle, confirmation toggle, noise sensitivity slider, battery optimization shortcut, and Android API capability report.

## Setup & Build Instructions (Local / GitHub)

### Requirements
- Android Studio Ladybug / Meerkat or newer (JDK 21+ / Android Gradle Plugin 9.x, Compile SDK 36, Min SDK 24)
- Android SDK Platform 36 & Build-Tools installed

### Build Debug APK via Gradle
1. Clone the repository from GitHub.
2. Open the project in Android Studio **or** run via terminal:
   ```bash
   ./gradlew assembleDebug
   ```
   *(In environments without the Gradle wrapper script, use `gradle assembleDebug`.)*
3. Run unit & Robolectric tests:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```
4. The APK is generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

### Device Setup Notes
- Install the APK on a physical Android device (Android 8.0+ recommended for `TelecomManager` call actions, Android 10+ for `CallScreeningService`).
- Open **Permission Setup Wizard** inside the app to grant Microphone, Phone State, Answer Phone Calls, Call Screening Role, and Notification permissions.
- On Android 14+ or Xiaomi/Oppo/Vivo/Samsung devices, keep the **Voice Assistant** toggle enabled from inside the app and allow unrestricted background battery usage so incoming call detection remains responsive.
