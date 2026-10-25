# NEXUS AI — ChatGPT-Style Android Local AI Assistant

[![Android](https://img.shields.io/badge/Platform-Android_10+-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blue.svg)](https://developer.android.com/jetpack/compose)
[![LM Studio](https://img.shields.io/badge/Backend-LM_Studio_Local_LLM-orange.svg)](https://lmstudio.ai)
[![License](https://img.shields.io/badge/License-Apache_2.0-lightgrey.svg)](LICENSE)

**NEXUS AI** is an exceptionally polished, 100% private Android AI chat application delivering a ChatGPT-grade conversational experience powered by your own local LLMs running inside **LM Studio** on Windows.

Crafted around a **Quiet Luxury + Modern Minimalism** visual design ethos inspired by the simplicity of ChatGPT, typography of Claude, and attention to detail of native Apple applications.

---

## 🌟 Highlights & Key Features

### 1. Quiet Luxury Aesthetic
- Sophisticated warm-white/slate color palette (`#FAFAF8` base, `#F1F1EE` elevated surfaces, `#202124` text, `#344B70` midnight blue accents).
- Fluid transitions, refined whitespace, edge-to-edge layout, and adaptive Dark Mode.

### 2. Live Server Latency & Health Indicator
- Dynamic status pill in the Top App Bar (`Nexus · Mistral v ● 26ms`).
- Real-time roundtrip HTTP health probes to monitor local Wi-Fi connectivity to LM Studio.

### 3. Persona / Mode Presets
- One-tap horizontal pill selector to switch system behavior on the fly:
  - 🤖 **Personal AI**: Thoughtful, warm, concise daily companion.
  - 💻 **Senior Coder**: Clean, idiomatic, robust code-first engineering assistant.
  - 🛡️ **Security Analyst**: Defensive security specialist, architecture auditor, and hardening advisor.
  - ⚡ **Concise Synthesizer**: High-density, bullet-driven executive summaries.

### 4. Generation Speed & Telemetry
- Precise real-time token streaming telemetry displayed on every completed assistant message:
  `13.6 tok/s · 370 tokens · 27.3s`.
- Metrics are tracked and persisted locally in SQLite.

### 5. Multi-Language Code Syntax Highlighting
- Custom lightweight lexer supporting **Kotlin, Python, Bash, Rust, JavaScript/TypeScript, SQL, and JSON**.
- Quiet Luxury syntax palette: slate-violet keywords, terracotta strings, teal numbers, and muted sage comments.
- One-tap **Copy Code** with instant Apple-grade haptic confirmation.

### 6. On-Device Voice Input (Speech-to-Text)
- Integrated microphone button in the composer with animated breathing halo.
- Native speech recognition modal with live audio waveforms and real-time transcription.
- Automatically places spoken queries into the composer.

### 7. Apple-Grade Haptic Micro-Interactions
- Tactile feedback designed with precision:
  - `tick`: Mechanical detent on tapping persona chips, send, and stop.
  - `confirm`: Double-click haptic confirmation on copying code or message text.
  - `completionPulse`: Soft sensory pulse when the LLM finishes streaming its response.

### 8. 100% Private & Offline-Capable
- Zero telemetry, zero cloud tracking, zero data leaving your local network.
- All requests communicate exclusively over your Wi-Fi LAN to LM Studio (`http://<YOUR_PC_IP>:1234`).

---

## 📱 Architecture & Tech Stack

- **Pattern**: Clean Architecture (Domain, Data, Feature, Core) + MVI/MVVM
- **Language**: Kotlin 2.3.20 + Kotlin Coroutines & Flow
- **UI Framework**: Jetpack Compose + Material 3 + Edge-to-Edge
- **Networking**: Retrofit 2 + OkHttp 4 + OkHttp Server-Sent Events (SSE)
- **Serialization**: Kotlinx Serialization JSON
- **Persistence**: SQLite OpenHelper with reactive Kotlin Flow change triggers
- **Preferences**: AndroidX Preferences DataStore

---

## 🚀 Getting Started

### Prerequisites
1. **Android Studio** (Koala / Ladybug or newer) with Android SDK 35+.
2. **LM Studio** installed on your Windows PC ([lmstudio.ai](https://lmstudio.ai)).
3. Physical Android device or emulator running Android 10+ (API 29+).

### 1. Start LM Studio Server
1. Launch LM Studio and load any GGUF model (e.g. `Mistral-7B-Instruct-v0.3`, `Llama-3.1-8B`, `Qwen2.5-Coder`).
2. Navigate to the **Developer** tab (`<->`).
3. Check **Serve on Local Network (0.0.0.0)** and click **Start Server** on port `1234`.
4. Note your PC's local IPv4 address (run `ipconfig` in PowerShell, e.g. `192.168.1.100`).

### 2. Build & Install the App
```powershell
# Clone the repository
git clone https://github.com/geevarghesekthomas84-sys/nexus-ai.git
cd nexus-ai

# Build the debug APK
.\gradlew.bat assembleDebug

# Install on your connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Connect & Chat
1. Open **Nexus AI** on your phone.
2. Tap the **Server Status Badge** in the top bar or open **Settings**.
3. Enter your PC's IP address (e.g., `http://192.168.1.100:1234`) and tap **Save & Test Connection**.
4. Select your loaded model from the top bar dropdown and begin chatting!

---

## 👨‍💻 Developer & Author

Built with ❤️ by **GG** (**Geevarghese K Thomas**)

- **GitHub**: [@geevarghesekthomas84-sys](https://github.com/geevarghesekthomas84-sys)
- **LinkedIn**: [Geevarghese K Thomas](https://www.linkedin.com/in/geevarghese-k-thomas)
- **Support**: [Buy Me a Coffee](https://buymeacoffee.com/geevarghes2)

---

## 📄 License

This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE) file for details.
