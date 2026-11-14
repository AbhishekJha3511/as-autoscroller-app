# 📱 Auto Scroller

### Hands-free scrolling for YouTube Shorts & Instagram Reels

Sit back. Tap ▶. Let your phone swipe for you.

![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-3DDC84?logo=android&logoColor=white)
![Expo](https://img.shields.io/badge/Expo-SDK%2052-000020?logo=expo&logoColor=white)
![Kotlin](https://img.shields.io/badge/Native-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Network](https://img.shields.io/badge/network-none-success)

</div>

---

## ✨ What it does

Auto Scroller performs real swipe gestures on your screen at a pace you choose, so Shorts, Reels (or any vertical/horizontal feed) advance on their own. A tiny floating controller sits on the edge of your screen. Tap ▶ to start, ❚❚ to pause.

## 🚀 Features

| Feature | Details |
|---|---|
| ⏱ **Delay** | 0 – 180 seconds between swipes (minimum 0.5 s gap so the video can settle) |
| 📏 **Area** | 10 – 100 % of the screen covered by each swipe (default 50 %) |
| ⚡ **Speed** | 0.5x – 3x finger velocity (default 1x) |
| 🧭 **Direction** | Down (next video), Up (previous), Left, Right |
| 🎛 **Edge controller** | Floating ▶/❚❚ pill on the screen edge, draggable up/down |
| 🙈 **Hide** | `»` collapses it to a small edge handle, tap the handle to bring it back |
| ❌ **Close** | `✕` removes the controller; bring it back from the app |
| 🎨 **Customize tab** | Button size, transparency, how much hangs off-screen, left/right edge |
| 🔋 **Smart behaviour** | Keeps the screen awake while scrolling, auto-pauses when the screen turns off |
| 🔒 **Private** | No accounts, no tracking, nothing sent anywhere |

Settings apply **live**, even while scrolling.

## 🔑 Permission

The app needs only **one** permission: the **Accessibility service** (this is what allows it to perform swipe gestures for you). The edge controller uses the same service, so there is no "draw over other apps" permission.

## 📦 Install & use

1. Install the APK.
2. Open **Auto Scroller** → tap **Grant permission** → **Accessibility → Auto Scroller → On**.
3. Open YouTube Shorts / Instagram Reels.
4. Tap the round **▶** on the screen edge. The first swipe happens after your chosen delay.
5. Tap **❚❚** to pause.

> **Direction tip:** *Down* = go to the **next** video (the finger swipes upward). *Up* = go to the **previous** video.

## 🛑 "Access denied" / toggle greyed out?

Android 13+ blocks accessibility for apps installed outside the Play Store (including EAS/sideloaded APKs). It's a one-time fix:

1. **Settings → Accessibility → Auto Scroller** → tap the toggle once (it will refuse), then go back.
2. **Settings → Apps → Auto Scroller** → tap **⋮** (top-right) → **Allow restricted settings** → confirm PIN/fingerprint.
3. Go back to **Accessibility → Auto Scroller** and switch it **On**.

Alternative: install from your computer with `adb install app.apk`.

## 🛠 Build it yourself

### Option A: Expo (this repo)

> Does **not** run in Expo Go because it contains custom native Android code.

**Requirements:** Node 18+, JDK 17, Android SDK (`ANDROID_HOME` set), phone with USB debugging.

```bash
npm install
npx expo install --fix        # aligns package versions with the Expo SDK
npx expo run:android          # prebuilds, compiles the Kotlin module, installs on your phone
```

Before publishing, set your own ID in `app.json`:

```json
"android": { "package": "com.yourname.autoscroller" }
```

### Option B: Cloud APK with EAS (no local SDK)

```bash
npm i -g eas-cli
eas login
eas build:configure
```

Add an APK profile to `eas.json`:

```json
{
  "build": {
    "preview": {
      "distribution": "internal",
      "android": { "buildType": "apk" }
    }
  }
}
```

```bash
eas build -p android --profile preview
```

Download the APK from the link EAS prints, install it, then follow the **"Access denied"** fix above.

### Option C: Pure native (Android Studio / VS Code)

The `AutoScroller/` native Kotlin project builds with Gradle:

```bash
gradle wrapper --gradle-version 8.7
echo "sdk.dir=/path/to/Android/sdk" > local.properties
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 🧠 How it works

```
React Native UI (App.tsx)
        │  setConfig / setCustomize / showController
        ▼
Expo native module (Kotlin)  ──►  SharedPreferences
        │                              ▲
        ▼                              │ live listener
AutoScrollService (AccessibilityService)
   ├─ dispatchGesture()  →  performs the swipes
   └─ TYPE_ACCESSIBILITY_OVERLAY  →  draws the edge controller
```

- Swipes are single-stroke gestures at a constant finger velocity, so they register as real flings at any area size.
- A session counter makes sure quick pause/play taps can never create two swipe loops.
- The loop waits for each gesture to finish, then waits your delay, so swipes never overlap.

## 📁 Project structure

```
autoscroller-expo/
├── App.tsx                      # UI: Scroll tab, Customize tab, GitHub footer
├── app.json                     # app name + Android package id
├── index.ts                     # entry point
└── modules/auto-scroll-controller/
    ├── index.ts                 # JS API for the native module
    ├── expo-module.config.json
    └── android/
        ├── build.gradle
        └── src/main/
            ├── AndroidManifest.xml          # registers the accessibility service
            ├── res/xml/accessibility_config.xml
            └── java/.../
                ├── AutoScrollService.kt     # swipe loop + edge controller
                ├── AutoScrollControllerModule.kt
                └── Prefs.kt
```

## 🩹 Troubleshooting

| Problem | Fix |
|---|---|
| Toggle greyed out / "Access denied" | Allow restricted settings (see above) |
| Not listed under Accessibility | The native module didn't make it into the build; rebuild with `npx expo run:android` or a fresh EAS build |
| No ▶ button after enabling | Open the app → **Customize** → **Show controller** (you may have pressed ✕) |
| Swipes feel too weak | Raise `BASE_PX_PER_SEC` at the top of `AutoScrollService.kt`, or increase Area/Speed |
| Skips two videos | Increase the Delay slightly |
| Stops after a while | Turn the service off and on once; check battery optimisation for the app |
| Kotlin changes not showing | Native code needs a rebuild (`npx expo run:android`); JS changes hot reload |

## ⚠️ Good to know

- Android only (iOS has no equivalent of an accessibility gesture service for third-party apps).
- Automating other apps may be against their terms of service. Use it responsibly and at your own risk.
- Accessibility services are heavily restricted on Google Play, so this project is intended for sideloading.

## 🤝 Contributing

Issues and pull requests are welcome. Ideas on the roadmap: auto-hide controller, swipe-count timer, randomized delay.

---

<div align="center">

**Made with ❤️ by [Abhishek Jha](https://github.com/AbhishekJha3511)**

⭐ If this helped you, drop a star on GitHub.

</div>