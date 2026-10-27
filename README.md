<div align="center">

# ✦ GRIA-AI ✦
### *Quiet Luxury On-Device & Local AI Assistant for Android*

A state-of-the-art, 100% private Android AI chat application delivering a ChatGPT-grade conversational experience powered directly by your own local LLMs running on-device or inside **LM Studio** on your PC.

[![Platform](https://img.shields.io/badge/Platform-Android%2010%2B%20(API%2029%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Backend](https://img.shields.io/badge/Inference-On--Device%20%26%20LM%20Studio-F89820?style=for-the-badge&logo=openai&logoColor=white)](https://lmstudio.ai)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)

<br/><br/>

<a href="#-interface-gallery">
  <img src="docs/assets/gria_ai_demo.gif" width="460" alt="Gria-AI Live Demo Animation" />
</a>

<br/><br/>

[ ✨ Highlights ](#-highlights) • [ 📱 Interface Gallery ](#-interface-gallery) • [ 🏛️ Architecture ](#️-architecture) • [ 🚀 Quick Start ](#-quick-start) • [ 👨‍💻 Author ](#-author--credits)

</div>

---

## 📱 Interface Gallery

Experience the **Quiet Luxury + Modern Minimalism** design system on real Android hardware:

<div align="center">
<table>
  <tr>
    <td align="center" width="50%">
      <b>✦ Gria-AI Welcome Screen</b><br/>
      <sub>Dark Graphite palette, Gria · Gemma 2B header, and persona pills</sub><br/><br/>
      <img src="docs/screenshots/01_home_screen.png" width="92%" alt="Gria-AI Home Screen" />
    </td>
    <td align="center" width="50%">
      <b>💬 Code Syntax Highlighting & Live Telemetry</b><br/>
      <sub>Multi-language lexer, copy button, and tok/s performance footer</sub><br/><br/>
      <img src="docs/screenshots/02_chat_syntax_telemetry.png" width="92%" alt="Gria-AI Chat Screen with Telemetry" />
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <b>📥 In-App Model Hub & Format Engine</b><br/>
      <sub>DownloadManager catalog, format tags (.bin, .task, .tflite), and Kaggle guides</sub><br/><br/>
      <img src="docs/screenshots/10_model_hub_formats.png" width="92%" alt="Gria-AI Model Hub and Formats" />
    </td>
    <td align="center" width="50%">
      <b>✨ Prompt Magic Wand Auto-Enhancer</b><br/>
      <sub>1-tap transforms casual user prompts into structured system-grade instructions</sub><br/><br/>
      <img src="docs/screenshots/11_prompt_enhancer_wand.png" width="92%" alt="Gria-AI Prompt Enhancer Wand" />
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <b>⚡ Neural Model Selector Bottom Sheet</b><br/>
      <sub>On-device GPU/CPU flatbuffers (.bin/.task) + LM Studio models</sub><br/><br/>
      <img src="docs/screenshots/12_model_selector_sheet.png" width="92%" alt="Gria-AI Model Selector Sheet" />
    </td>
    <td align="center" width="50%">
      <b>🎙️ Two-Way Offline Voice Assistant</b><br/>
      <sub>Hands-free voice orb with STT, on-device Gemma inference & TTS playback</sub><br/><br/>
      <img src="docs/screenshots/07_live_voice_assistant.png" width="92%" alt="Gria-AI Live Voice Assistant" />
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <b>📊 Hardware & RAM Live Monitor</b><br/>
      <sub>Snapdragon 8+ Gen 1, RAM usage gauge, Adreno 730 GPU & thermals</sub><br/><br/>
      <img src="docs/screenshots/08_hardware_ram_monitor.png" width="92%" alt="Gria-AI Hardware & RAM Monitor" />
    </td>
    <td align="center" width="50%">
      <b>🎭 Custom Persona & Behavior Studio</b><br/>
      <sub>System prompt editor, 1-tap Reset to Gria-AI, and Quick Persona Presets</sub><br/><br/>
      <img src="docs/screenshots/09_persona_behavior_settings.png" width="92%" alt="Gria-AI Persona Settings" />
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <b>⚡ 100% On-Device Gemma Inference</b><br/>
      <sub>39.9 tok/s local generation directly on Snapdragon Adreno GPU</sub><br/><br/>
      <img src="docs/screenshots/05_ondevice_gemma.png" width="92%" alt="Gria-AI On-Device Gemma Inference" />
    </td>
    <td align="center" width="50%">
      <b>📚 SQLite Conversation Drawer</b><br/>
      <sub>Searchable history, pinned chats, and quick access</sub><br/><br/>
      <img src="docs/screenshots/04_history_drawer.png" width="92%" alt="Gria-AI History Drawer" />
    </td>
  </tr>
</table>
</div>

---

## 🌟 Highlights

<table>
  <tr>
    <td width="50%">
      <h3>💭 DeepSeek-R1 "Thinking" Mode</h3>
      Real-time parsing of <code>&lt;think&gt;...&lt;/think&gt;</code> reasoning tokens. Features an animated pulsing indicator during active generation and a collapsible accordion with elapsed timer when finished, keeping chat threads clean while allowing full inspection of chain-of-thought.
    </td>
    <td width="50%">
      <h3>🎨 Live Artifacts & Interactive Canvas</h3>
      Instant live rendering of HTML, SVG, and web code blocks. CodeBlock features a <code>[Code]</code> / <code>[Canvas]</code> tab switch embedding a sandboxed Compose WebView for live preview of interactive diagrams, charts, and vector graphics.
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>✨ Prompt "Magic Wand" Auto-Enhancer</h3>
      AutoAwesome wand button beside the composer turns brief prompts (e.g. <i>"write snake in python"</i>) into structured, professional prompts with explicit goals, technical constraints, and edge-case requirements.
    </td>
    <td width="50%">
      <h3>🔍 Highlight-to-Ask in Any App</h3>
      Android <code>ACTION_PROCESS_TEXT</code> system integration. Highlight text in Chrome, WhatsApp, Gmail, or any app, tap <b>"Ask Gria"</b> in the context menu, and receive an instant on-device explanation or summary in a floating bottom sheet.
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>📥 In-App Model Hub & Multi-Format Engine</h3>
      Integrated model hub backed by Android <code>DownloadManager</code>. Supports <b>.bin, .task, .tflite, and .litert</b> formats with automatic TFL3 header and ZIP archive validation preventing corrupt downloads from crashing the app.
    </td>
    <td width="50%">
      <h3>🔒 Biometric Vault Lock</h3>
      Hardware-backed biometric authentication (Fingerprint, Face Unlock, or PIN fallback). Protects your conversations and telemetry on app resume and launch with a dedicated lock screen.
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>🎙️ Two-Way Offline Voice Assistant</h3>
      Full hands-free spoken conversation loop. Interactive animated glowing voice orb with on-device speech-to-text (STT), local Gemma intelligence, and offline text-to-speech (TTS) playback with haptic feedback.
    </td>
    <td width="50%">
      <h3>📊 Live Hardware & RAM Telemetry</h3>
      Real-time system telemetry bottom sheet. Live monitors Qualcomm Snapdragon cores, system RAM consumption gauge, model memory footprint, active Adreno GPU acceleration, and thermal headroom.
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>⚡ Real-Time Streaming & Telemetry</h3>
      Native Server-Sent Events (SSE) token delivery. Measures exact generation metrics on every assistant completion:
      <code>13.6 tok/s · 370 tokens · 27.3s</code>.
    </td>
    <td width="50%">
      <h3>🚀 Android Quick Settings Tile</h3>
      Android <code>TileService</code> lets users add a "Gria-AI" quick action to the notification shade for instant 1-tap capture and asking from anywhere in the OS.
    </td>
  </tr>
</table>

---

## 🏛️ Architecture

GRIA-AI is built strictly on **Clean Architecture + MVI/MVVM**, decoupling domain business logic from the UI presentation layer, on-device GPU inference, and LAN streaming transport.

```mermaid
graph TD
    subgraph MobileDevice["📱 Android Client (Gria-AI)"]
        UI["Jetpack Compose UI<br/><i>(Quiet Luxury + Material 3)</i>"]
        VM["ChatViewModel<br/><i>(MVI State & Haptic Engine)</i>"]
        Repo["ChatRepository<br/><i>(Clean Architecture)</i>"]
        DB[("SQLite Database<br/><i>(Messages & Telemetry)</i>")]
        SSE["OkHttp SSE Client<br/><i>(LM Studio LAN Engine)</i>"]
        OD["OnDeviceLlmEngine<br/><i>(MediaPipe GenAI · Adreno GPU)</i>"]
        Bio["BiometricManager<br/><i>(Biometric Vault Lock)</i>"]
        Hub["ModelHubManager<br/><i>(DownloadManager Catalog)</i>"]

        UI <--> VM
        VM <--> Repo
        Repo <--> DB
        Repo <--> SSE
        Repo <--> OD
        VM <--> Bio
        VM <--> Hub
    end

    subgraph LocalLAN["📡 Private Local Network (Wi-Fi)"]
        Router["LAN Router<br/><i>(192.168.1.x)</i>"]
    end

    subgraph HostPC["💻 Host Machine (LM Studio)"]
        LMS["LM Studio Server<br/><i>(Port 1234 · OpenAI API)</i>"]
        LLM["Local GGUF LLM<br/><i>(DeepSeek-R1 / Qwen 2.5 / Llama 3.1)</i>"]
        
        LMS <--> LLM
    end

    SSE <==>|"Encrypted / Cleartext LAN"| Router
    Router <==>|"SSE Stream & REST"| LMS
```

### Supported Model Formats

| Format / Engine | Deployment | Supported Models |
| :--- | :---: | :--- |
| **`.bin`** (LiteRT / Flatbuffer) | On-Device GPU/CPU | Google Gemma 2B IT, Phi-2, Falcon-RW-1B |
| **`.task`** (MediaPipe Bundle) | On-Device GPU/CPU | Official MediaPipe Tasks models with tokenizer metadata |
| **`.tflite` / `.litert`** | On-Device GPU/CPU | TensorFlow Lite / Google LiteRT quantized models |
| **`.gguf`** | LM Studio / PC LAN | DeepSeek-R1, Qwen 2.5, Llama 3.2, Mistral, Gemma 2 |

### Tech Stack Matrix

| Layer | Technologies |
| :--- | :--- |
| **UI & Presentation** | Jetpack Compose 2024, Material 3, Edge-to-Edge, Compose Animations, AndroidView WebView |
| **Architecture** | Clean Architecture (Domain, Data, Feature, Core) + MVVM / MVI |
| **Language & Concurrency** | Kotlin 2.3.20, Kotlin Coroutines, StateFlow, SharedFlow |
| **On-Device AI Engine** | Google MediaPipe Tasks GenAI (`tasks-genai:0.10.14`), Qualcomm OpenCL Adreno GPU backend |
| **Networking & SSE** | Retrofit 2, OkHttp 4, OkHttp Server-Sent Events (`EventSource`) |
| **System Integrations** | Android `BiometricPrompt`, `DownloadManager`, `PROCESS_TEXT` Intent, Quick Settings `TileService` |
| **Local Persistence** | SQLite OpenHelper with reactive Kotlin Flow change triggers |
| **Preferences** | AndroidX Preferences DataStore |
| **Audio & Haptics** | `Vibrator` / `VibrationEffect` API, `SpeechRecognizer`, Android native offline `TextToSpeech` |

---

## 🚀 Quick Start

### 1. Prerequisites
- **Android Studio** (Koala / Ladybug or newer)
- **Android SDK** API 35
- Physical device running **Android 10+ (API 29+)**
- *(Optional)* **LM Studio** installed on your PC ([lmstudio.ai](https://lmstudio.ai)) for larger models (DeepSeek-R1, Qwen 2.5, Llama 3)

### 2. Configure LM Studio (For Local Wi-Fi Mode)
1. Open LM Studio and load any GGUF model (e.g. `DeepSeek-R1-Distill-Qwen-1.5B`, `qwen2.5-0.5b-instruct`).
2. Navigate to the **Developer tab (`<->`)**.
3. Toggle **Serve on Local Network (0.0.0.0)** and click **Start Server** on port `1234`.
4. Run `ipconfig` in your PC terminal to get your IPv4 address (e.g., `192.168.1.100`).

### 3. Clone & Build
```powershell
# Clone the repository
git clone https://github.com/geevarghesekthomas84-sys/Gria-AI.git
cd Gria-AI

# Compile the debug APK
.\gradlew.bat assembleDebug

# Install onto your connected Android device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. Connect
1. Launch **Gria-AI** on your phone.
2. **For On-Device Mode**: Tap the model dropdown, select your on-device model (`gemma-2b-it-gpu-int4.bin`), and chat 100% offline!
3. **For LM Studio Mode**: Tap the **Server Status Badge** in the top bar, enter your PC's IP and port (e.g. `http://192.168.1.100:1234`), and tap **Save & Apply**.

---

## 👨‍💻 Author & Credits

Designed and engineered with passion by **GG** (**Geevarghese K Thomas**).

<div align="center">

[![GitHub](https://img.shields.io/badge/GitHub-geevarghesekthomas84--sys-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/geevarghesekthomas84-sys)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Geevarghese%20K%20Thomas-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/geevarghese-k-thomas)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-Donate-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/geevarghes2)

</div>

---

## 📄 License

```
Copyright 2026 Geevarghese K Thomas (GG)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
