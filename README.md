# PressIt

On-device file compressor for Android. Pick an image, video, or audio file, set a target size, and PressIt compresses it locally — nothing leaves the phone.

- Images: iterative JPEG quality/scale reduction (pure Android APIs)
- Video/Audio: local FFmpegKit transcode with bitrate search to hit the target size
- minSdk 26, Kotlin, no Compose

## Build
Push to GitHub — `.github/workflows/build-apk.yml` builds a debug APK on every push to `main` (or via "Run workflow"), uploaded as an artifact named `PressIt-debug-apk`.

## Note on FFmpegKit
`app/build.gradle.kts` depends on `com.arthenica:ffmpeg-kit-full:6.0-2`. That project was archived in 2025; if the artifact ever disappears from Maven Central, swap the dependency for the community-maintained `io.github.ffmpeg-kit` fork (same package/API).
