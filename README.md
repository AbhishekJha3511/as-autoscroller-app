# Auto Scroller (Expo + native Android module)

Needs: Node 18+, JDK 17, Android SDK (ANDROID_HOME set), phone with USB debugging.
Does NOT run in Expo Go (custom native code) – use a development build.

    npm install
    npx expo install --fix      # aligns package versions with the Expo SDK
    # edit android.package in app.json if you like
    npx expo run:android        # prebuilds, compiles Kotlin module, installs on phone

Then in the app: tap "Grant permission" -> Accessibility -> Auto Scroller -> On.
Android 13+ sideloaded: Settings > Apps > Auto Scroller > ⋮ > Allow restricted settings first.

Cloud build instead of local SDK:
    npm i -g eas-cli && eas build --profile development --platform android
(run `eas build:configure` first and set "developmentClient": true in the development profile)

Edit JS/UI -> `npm start` (fast refresh). Edit Kotlin in modules/auto-scroll-controller/android -> rebuild.

## Controller buttons
▶/❚❚ play-pause · » hide (collapses to a small edge handle, tap it to expand) · ✕ close
(remove it completely; bring it back with "Show controller" in the Customize tab).
Customize tab: button size, transparency, how much hangs off-screen, left/right edge.
