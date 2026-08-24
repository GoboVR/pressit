# PressIt

On-device file compressor for Android. Pick an image, video, or audio file, set a target size, and PressIt compresses it locally — nothing leaves the phone.

- Images: iterative JPEG quality/scale reduction (pure Android APIs)
- Video/Audio: local FFmpegKit transcode with bitrate search to hit the target size
- minSdk 26, Kotlin, no Compose

## Build
Push to GitHub — `.github/workflows/build-apk.yml` builds a debug APK on every push to `main` (or via "Run workflow"), uploaded as an artifact named `PressIt-debug-apk`.

## Note on FFmpegKit
The original `com.arthenica:ffmpeg-kit-full` was pulled from Maven Central in April 2025 when the project was retired. `app/build.gradle.kts` uses a community republish, `com.moizhassan.ffmpeg:ffmpeg-kit-16kb:6.1.1`, which ships the same `com.arthenica.ffmpegkit` Java package — no source changes needed. If that coordinate ever stops resolving, search Maven Central for other `ffmpeg-kit` republishes; the code only needs a working `com.arthenica.ffmpegkit.FFmpegKit`/`ReturnCode` on the classpath.
