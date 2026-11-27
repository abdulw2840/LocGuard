# LocGuard

An Android privacy and battery utility that automatically turns off Location (GPS) when you aren't actively using it.

Instead of leaving GPS running in the background after you close Maps or lock your phone, LocGuard watches the foreground app and shuts Location off after a short configurable delay—using **Root (KernelSU / Magisk)** or **Shizuku**.

## Editions (Downloads)

Both editions share the exact same background engine and features, with two different UI styles available in [**Releases**](https://github.com/abdulw2840/LocGuard/releases):

| Edition | Branch | Download | Style |
| :--- | :--- | :--- | :--- |
| **Karui Edition** | [`karui`](https://github.com/abdulw2840/LocGuard/tree/karui) | `LocGuard-v1.3.0-Karui.apk` | Sharp zero-radius retro UI with 25 built-in themes & UI scaling (inspired by [ronynn/karui](https://github.com/ronynn/karui)) |
| **Liquid Glass** | [`main`](https://github.com/abdulw2840/LocGuard/tree/main) | `LocGuard-v1.3.0-LiquidGlass.apk` | Translucent frosted-glass Material You UI |

## Features

- **App Whitelist:** Keep Location ON while using trusted apps (Maps, Ride-sharing, Camera, etc.).
- **Auto-Enable (Optional):** Automatically turn Location ON when opening a whitelisted app.
- **Auto-Off Delay:** Configurable countdown (5s to 10m, with quick presets) after leaving a whitelisted app.
- **Screen-Off Shutoff:** Separate grace timer when locking your screen so GPS doesn't stay on in your pocket.
- **Live Countdown Notification:** Shows remaining seconds with quick **Turn Off** and **+1m** actions.
- **Quick Settings Tile:** Toggle LocGuard directly from Android's Quick Settings panel.
- **Activity Logs:** Tracks every auto-shutoff, auto-enable, and privilege event locally.
- **Root & Shizuku Support:** Works with KernelSU / Magisk (`su`) or Shizuku, plus one-tap auto-grant for `PACKAGE_USAGE_STATS` and `WRITE_SECURE_SETTINGS`.

## Requirements

- Android 12+ (API 31+)
- **Root** (KernelSU / Magisk) **or** **Shizuku** running on the device

## Setup

1. Install your preferred APK from [**Releases**](https://github.com/abdulw2840/LocGuard/releases).
2. Open LocGuard and grant Root access (or authorize Shizuku).
3. Tap **Auto-Grant Permissions** inside the app to grant UsageStats and Secure Settings access.
4. Go to the **Apps** tab and whitelist the apps that need Location.

## Building from Source

```bash
git clone https://github.com/abdulw2840/LocGuard.git
cd LocGuard
# Optional: switch to the Karui UI branch
# git checkout karui

./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Credits

- Karui Edition UI & 25-theme palette inspired by [ronynn/karui](https://github.com/ronynn/karui).
- Built with Kotlin, Jetpack Compose, [libsu](https://github.com/topjohnwu/libsu), and [Shizuku](https://github.com/RikkaApps/Shizuku).
