# Ken Rec

**Tired of your storage running full and clumsily trimming Kendama clips?**

Ken Rec is a small recorder for kendama practice, by Kendama Senses. Record as long as you like. When you stop,
you see only the last bit, trim it in seconds, and only that clip is kept.

## How it works

1. Press record and practise. After 45 minutes the recording restarts by itself and the old one is thrown away, so the phone never fills up. In the last 90 seconds before that, a "Restart now" button appears on the screen.
2. Press stop. You see the last 90 seconds, with the last 30 already selected.
3. Drag the two handles, or use the - and + buttons, to set the start and end. Play it to check.
4. Press save. The clip goes to your gallery in the album "Ken Rec", and the long recording is deleted.

Nothing is uploaded anywhere.

**Status:** early. Android only, tested on one phone so far.

## Build

```
./gradlew assembleDebug   # the app is app/build/outputs/apk/debug/app-debug.apk
```

Install on a phone with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Contributing

See `CONTRIBUTING.md`.

## License

Apache License 2.0, see `LICENSE`. The Kendama Senses name, logo and background image are not covered by it, see `NOTICE`.
