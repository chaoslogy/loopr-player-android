# Installing Loopr Player on Fire TV

The full step-by-step is at **<https://loopr.studio/install>**.

TL;DR (Method A, no PC):

1. Fire TV → Settings → My Fire TV → Developer options → **Apps from Unknown Sources = ON**.
2. Install the **Downloader** app from the Amazon Appstore.
3. In Downloader, enter URL: `loopr.studio/player.apk`
4. Click Install.
5. Open Loopr Player. Note the 6-character pairing code.
6. On your laptop, sign in to <https://app-staging.loopr.studio>, click **Pair a TV**, enter the code.

TL;DR (Method B, ADB):

```sh
curl -fsSL -o loopr-player-staging.apk https://loopr.studio/player.apk
adb connect <FIRE_TV_IP>:5555     # accept the dialog on the TV
adb install -r loopr-player-staging.apk
adb shell am start -n co.loopr.player/.MainActivity
```

## Where the APK comes from

GitHub Actions builds `app/build/outputs/apk/debug/app-debug.apk` on every push
to `main` and publishes it to a rolling release at
<https://github.com/chaoslogy/loopr-player-android/releases/tag/staging-latest>.
The marketing site has a `loopr.studio/player.apk` 302 redirect to the GitHub
release asset, which is what Downloader and the install page use.
