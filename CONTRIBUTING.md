# Contributing to Ken Rec

Thanks for helping. Ken Rec is a small app, so the rules are small too.

## Setup

1. Clone the repository.
2. Open the project in Android Studio, or build from a terminal: `./gradlew assembleDebug`.
3. The app needs a real phone to test recording. The emulator camera is not good enough for this.
4. A recording restarts after 45 minutes unless Settings says otherwise. To test that without waiting, build with a short limit:
   `./gradlew assembleDebug -PmaxRecordingMs=150000` (150 seconds; the restart button appears for the last 90).

## Releasing (maintainers)

1. Raise `versionCode` and `versionName` in `app/build.gradle.kts` and commit.
2. Push a tag: `git tag v0.1.1 && git push origin v0.1.1`. The `release` workflow builds the signed app and
   publishes it on the Releases page with a checksum file. Run the workflow by hand to build without publishing.

The signing key is not in the repository. It is stored as repository secrets and the maintainer keeps a backup.
Never replace it: phones that installed an app signed with another key cannot update it.

## Making a change

- One change per pull request. Small is good.
- Commit messages: a short summary line in the style `fix: ...`, `feat: ...`, `docs: ...`, `chore: ...`.
- Say in the pull request what you tested and on which phone and Android version.
- Do not commit keys, passwords, signing files or `local.properties`.
- Your name and email end up in the public history. GitHub offers a private no-reply address if you prefer
  (GitHub settings, Emails, "Keep my email addresses private").

## Looks and wording

The app follows the Kendama Senses look: dark teal canvas, cream text, mint pill buttons, Poppins type.
Colours and shapes live in `app/src/main/kotlin/com/kendamasenses/kenrec/ui/Theme.kt`. Reuse them, do not add new ones.
Plain words in the app, no jargon.

## Licence

By contributing you agree that your contribution is licensed under the Apache License 2.0 (section 5 of the licence).

## Conduct

Be kind and be specific. See `CODE_OF_CONDUCT.md`.
