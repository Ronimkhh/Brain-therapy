# TRAP THEORY: NOTHING IS WHAT IT SEEMS

TRAP THEORY is an offline, native Android troll puzzle-platformer. The Android game is written in Kotlin and rendered with Android Canvas on a `SurfaceView`; it does not use a WebView, game engine, native C++ code, or network service. A small, separate Vite project at the repository root is a developer-facing game landing page and is not packaged in the APK.

## Game

- 45 story levels across five worlds, plus three discoverable secret rooms.
- Story, Challenge, Time Attack, and Endless Trap modes.
- Fake exits and checkpoints, crumbling and invisible platforms, gravity shifts, teleport doors, deceptive buttons, moving hazards, hidden routes, wind, ice, darkness, and more level-specific twists.
- Three-star scores, run timer, deaths, checkpoints, respawns, best times, level unlocks, and automatic local saves.
- Canvas-drawn character animation, parallax scenery, particles, lighting, screen shake, camera follow, pause and completion screens.
- Offline synthesized background music and sound effects using Android `AudioTrack`, with separate master, music, and SFX levels.
- Multi-touch direction, jump, action, and pause controls. Drag controls to move them, pinch to resize them, and change opacity, sensitivity, and handedness. Settings persist on-device.
- Settings for Low/Medium/High/Ultra graphics, 30/60/120 FPS caps, particles, screen shake, lighting, vibration, fullscreen, and English/Spanish menu text.

## Project Layout

- `app/src/main/java/com/traptheory/game/MainActivity.kt` - native menu activity and menu audio lifecycle.
- `app/src/main/java/com/traptheory/game/GameActivity.kt` - landscape gameplay activity.
- `app/src/main/java/com/traptheory/game/GameSurfaceView.kt` - touch routing and `SurfaceHolder` lifecycle.
- `app/src/main/java/com/traptheory/game/GameLoop.kt` - fixed-step 120 Hz simulation and capped rendering thread.
- `app/src/main/java/com/traptheory/game/GameEngine.kt` - game state, progression, checkpoints, score, and mode logic.
- `app/src/main/java/com/traptheory/game/GameRenderer.kt` - Canvas drawing, effects, HUD, touch controls, and overlays.
- `app/src/main/java/com/traptheory/game/PhysicsEngine.kt` and `CollisionSystem.kt` - movement, platform resolution, and collision events.
- `app/src/main/java/com/traptheory/game/LevelManager.kt` - 45 campaign puzzle definitions, secret rooms, and endless generation.
- `app/src/main/java/com/traptheory/game/TrapSystem.kt` - level-specific trap and environmental behavior.
- `app/src/main/java/com/traptheory/game/ParticleSystem.kt` - fixed-capacity particle pool.
- `app/src/main/java/com/traptheory/game/InputManager.kt` - multi-touch input and persisted control positions.
- `app/src/main/java/com/traptheory/game/AudioManager.kt` - locally synthesized audio and vibration.
- `app/src/main/java/com/traptheory/game/SettingsManager.kt` and `SaveManager.kt` - persistent preferences and campaign state.
- `app/src/main/res/layout/` - XML activity layouts hosting the native custom views.
- `.github/workflows/build.yml` - push, pull-request, and manual APK builds.
- `.github/workflows/release.yml` - manual versioned GitHub Release publishing.
- `src/App.tsx` - optional browser landing page only; it is not an Android game runtime.

## Android Build

Requirements for a local build are JDK 17 and Android SDK Platform 36 with Build Tools 35.0.0. The Android Gradle Plugin is 8.10.1 and Gradle 8.11.1 is downloaded by the checked-in wrapper. Android Studio is supported; compatible Android IDEs on phones can open the same Gradle project. If an on-device IDE cannot provision the SDK or JDK, the GitHub Actions workflow builds the APK without a PC.

Open the repository root (the folder containing `settings.gradle.kts`) as an Android/Gradle project. Allow Gradle sync, select JDK 17, and install Android SDK Platform 36 if the IDE requests it. Do not add `local.properties` to Git; it contains machine-specific SDK paths.

From a terminal with the Android SDK configured:

```bash
./gradlew --version
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew test
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The default unsigned release APK is written to `app/build/outputs/apk/release/app-release-unsigned.apk`; a signed release uses `app-release.apk`.

## Use GitHub Actions From A Phone

1. Create a new empty repository on GitHub. Do not initialize it with a README if you are pushing this project as-is.
2. Upload or clone the complete project, including the hidden `.github/workflows/` directory and the Gradle wrapper files.
3. Push to the `main` branch. `Android APK Build` runs on each push to `main`, every pull request, and manual `workflow_dispatch` runs.
4. Open the repository's **Actions** tab and select a completed **Android APK Build** run.
5. Download `TRAP-THEORY-debug-apk` for an installable debug build or `TRAP-THEORY-release-apk` for the unsigned release build. Failed-run logs are attached as `TRAP-THEORY-build-logs`.

Git command-line example after creating the empty remote repository:

```bash
git init
git add .
git commit -m "Build TRAP THEORY Android game"
git branch -M main
git remote add origin https://github.com/YOUR-NAME/YOUR-REPOSITORY.git
git push -u origin main
```

The workflow uses JDK 17, installs Android SDK Platform 36 and Build Tools 35.0.0, accepts SDK licenses, resolves Gradle dependencies, builds debug and release APKs with `./gradlew :app:assembleDebug` and `./gradlew :app:assembleRelease`, and uploads the results with the official artifact upload action. Build output is streamed into artifacts-friendly logs so compiler failures remain available after a failed run.

## Releases And Signing

The debug APK is signed automatically with Android's development debug key and can be installed for testing. A release APK built without signing credentials is **unsigned**; Android will not install it as a normal app until it is signed. A **signed release APK** is suitable for installation and future updates when the same private key is kept safe.

To enable signed releases, create a release keystore outside the repository and add these Actions repository secrets under **Settings > Secrets and variables > Actions**:

- `ANDROID_KEYSTORE_BASE64` - base64-encoded JKS/keystore bytes.
- `ANDROID_KEY_ALIAS` - alias of the signing key.
- `ANDROID_STORE_PASSWORD` - keystore password.
- `ANDROID_KEY_PASSWORD` - key password.

For a Linux/Termux shell, encode without line breaks using `base64 -w 0 release.jks` and put the output directly into the GitHub secret. Never paste passwords or keystore contents into a source file, commit, workflow output, or issue. The release workflow decodes the key into the runner's temporary directory and does not print it. Without all four secrets, the release still publishes an explicitly labelled unsigned APK.

To publish a release, open **Actions > Publish Android Release > Run workflow**, enter a version such as `1.0.0` or `v1.0.0`, and run it from the repository's default branch. The workflow builds `:app:assembleRelease`, derives an Android version code from the version, creates a `v<version>` tag, and attaches `TRAP-THEORY-v<version>.apk` to the GitHub Release.

## Repository Hygiene

`.gitignore` excludes Gradle/Android build output, `local.properties`, IDE caches, Vite output, keystores, and local secrets. Keep signing credentials in GitHub Actions Secrets only. No API keys, backend credentials, or online services are required by the game.