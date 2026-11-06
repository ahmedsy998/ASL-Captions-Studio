# ASL Captions Studio — Advanced AI Video Editor for Android

**Package ID:** `com.asldesign.captionsstudio`  
**Platform:** Android 8.0+ (API 26 – API 36)  
**Primary Language:** Arabic (RTL Default) & English (LTR)  
**UI Framework:** Jetpack Compose (Material 3 customized with Tasneem-inspired brand palette)

## Core Features

1. **Tasneem-Inspired Brand Identity & Dual Themes**:
   - **Primary Orange (`#E8872F`)**, **Dark Silver (`#343A40`)**, **Deep Charcoal (`#1D2227`)**, **Soft Ivory (`#F5F1EA`)**, and **Silver Gray (`#C5C8CB`)**.
   - Complete **Premium Dark Theme** and **Premium Light Theme** with instant RTL/LTR layout switching.
   - Bundled Arabic & English Google Fonts (`Tajawal`, `Cairo`, `Amiri`, `Changa`, `Montserrat`).

2. **AI Automatic Caption Generator & Smart Tools**:
   - Powered by **Firebase AI Logic (`gemini-3.8-flash`)** secured with **Firebase App Check** (`configureAppCheck` in `MainActivity.onCreate()`).
   - Replaceable `SpeechRecognitionProvider` architecture supporting:
     - `FirebaseGeminiSpeechProvider` (Multimodal audio/video chunk transcription with word-level alignment and overlap deduplication)
     - `WhisperCompatibleSpeechProvider` (Configurable Whisper endpoint with word-level timestamps)
     - `LocalAndroidSpeechProvider` (Android On-Device Speech Engine)
   - Supports Arabic (Modern Standard, Syrian/Levantine, Egyptian, Gulf) and English.
   - 10 AI Smart Editing Tools: Automatic Captions, Language & Dialect Detection, Transcript Correction, Automatic Punctuation, Smart Sentence Segmentation, Long Silence Removal, Scene Change Detection, Template Recommendation, Keyword Highlighting, and Social Video Description Generation.

3. **Advanced Caption Editing & 25 Animation Presets**:
   - Instant switching between **Word-by-Word (`كلمة بكلمة`)** and **Full Sentence (`جملة كاملة`)** modes.
   - 25 real mathematical animation presets (`CaptionAnimationEngine`) shared between the Jetpack Compose Live Preview and the Android Canvas MP4 Export Renderer.
   - 20 built-in caption templates across 5 categories (`Podcast`, `Social Media`, `Cinematic`, `Dynamic`, `Arabic`) plus custom user-saved templates.

4. **Multi-Track Timeline, 20 Video Effects & Audio Studio**:
   - 5-track timeline (Video, Captions, Original Audio Waveform, Effects, Music & SFX) with split at playhead, trim, delete, zoom, and scrubbing.
   - 20 Video Effects & Color Grading presets rendered via `ColorMatrix` and geometric transforms.
   - Real 1080p / 720p (30 FPS / 60 FPS) H.264 `.mp4` video export using Android `MediaCodec` + `MediaMuxer` (`RealMp4ExportEngine`).

## Building & Testing

- Run Unit & Robolectric Tests:
  ```bash
  gradle :app:testDebugUnitTest
  ```
- Build Debug APK:
  ```bash
  gradle :app:assembleDebug
  ```
