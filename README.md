# Ken Rec

**Tired of your storage running full and clumsily trimming Kendama clips?**

Ken Rec is a small recorder for kendama practice, by Kendama Senses. Record as long as you like. When you stop,
you see only the last bit, trim it in seconds, and only that clip is kept.

## How it works

1. Press record and practise. After 45 minutes (you can pick 15, 30, 45 or 60 in Settings) the recording restarts by itself and the old one is thrown away, so the phone never fills up. In the last 90 seconds before that, a "Restart now" button appears on the screen.
2. Press stop. You see the last 90 seconds, with the last 30 already selected.
3. Drag the two handles, or use the - and + buttons, to set the start and end. Play it to check.
4. Press save. The clip goes to your gallery in the album "Ken Rec", and the long recording is deleted.

Nothing is uploaded anywhere. In Settings you can also choose the video quality: small files (720p), standard (1080p) or best (the phone's own quality), each with a rough size per hour.

**Status:** early. Android only, tested on one phone so far.

## Install (Android)

1. On your phone, open the [latest release](https://github.com/jordivanvelzen/ken-rec/releases/latest) and download the `.apk` file.
2. Open the file. Android asks once to allow installs from your browser or files app: allow it.
3. Android may say the app comes from an unknown developer. That is because it is not from Google Play. Tap "Install anyway".
4. To get updates automatically, add this repository to [Obtainium](https://obtainium.imranr.dev/), a free app that installs and updates apps straight from GitHub.

To check that the file is the real one, compare its signing certificate with this SHA-256 fingerprint
(`apksigner verify --print-certs ken-rec-*.apk`):

```
64:C9:0B:8F:82:93:50:62:EE:A3:22:BC:30:E8:50:25:7F:79:0F:69:05:0C:E8:A2:E4:01:A7:DD:AB:75:17:3C
```

Each release also has a `SHA256SUMS.txt` file with the checksum of the app file.

## Build

```
./gradlew assembleDebug   # the app is app/build/outputs/apk/debug/app-debug.apk
```

Install on a phone with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Contributing

See `CONTRIBUTING.md`.

## License

Apache License 2.0, see `LICENSE`. The Kendama Senses name, logo and background image are not covered by it, see `NOTICE`.
