# Architecture & Engineering Guide — NEXUS AI

## 1. System Overview
Nexus AI is a private, production-grade Android AI chat application delivering a ChatGPT-style conversational experience. It communicates directly with locally hosted language models served by LM Studio over a local Wi-Fi network using standard OpenAI-compatible REST and Server-Sent Events (SSE) endpoints.

### Key Highlights:
- **Zero Cloud Leakage**: No prompts, tokens, or personal messages are transmitted outside your local area network.
- **ChatGPT-Style UX**: Top bar model selector, suggestion cards, multiline floating message composer with Send/Stop controls, markdown rendering, syntax-highlighted code blocks, and conversation drawer.
- **Local Persistence**: All conversations and individual message turns are saved in a local SQLite database with indices and reactive Kotlin Flows.

---

## 2. Clean Architecture & MVVM Structure

```text
NexusAI/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/nexusai/app/
│       │       ├── MainActivity.kt
│       │       ├── NexusApplication.kt
│       │       ├── core/
│       │       │   ├── components/   # MessageBubble, MessageComposer, WelcomeHero, ModelSelector, Markdown
│       │       │   ├── database/     # NexusDatabaseHelper (SQLite + Kotlin Flow)
│       │       │   ├── datastore/    # SettingsDataStore (Preferences DataStore)
│       │       │   ├── network/      # LmStudioApi (Retrofit), LmStudioStreamClient (OkHttp SSE)
│       │       │   └── theme/        # Color, Theme, Type (Stitch MCP design tokens)
│       │       ├── data/
│       │       │   ├── remote/dto/   # ChatDtos (Kotlinx Serialization)
│       │       │   └── repository/   # ChatRepositoryImpl
│       │       ├── domain/
│       │       │   ├── model/        # Conversation, Message, AIModel, ServerConfig
│       │       │   └── repository/   # ChatRepository
│       │       ├── feature/
│       │       │   ├── about/        # AboutScreen
│       │       │   ├── chat/         # ChatScreen, ChatViewModel, ChatUiState
│       │       │   ├── connection/   # ConnectionScreen
│       │       │   ├── history/      # SearchScreen, History Drawer
│       │       │   ├── onboarding/   # OnboardingScreen
│       │       │   └── settings/     # SettingsScreen
│       │       └── navigation/       # NexusNavHost, Screen
│       └── test/                     # Unit test suites (MarkdownParserTest, NexusDataAndModelTest)
```

---

## 3. Streaming & Networking Engine
- **SSE Parser**: Powered by `okhttp3.sse.EventSource`. It streams incoming token deltas incrementally (`StreamEvent.Token`), handles termination gracefully on `[DONE]`, and handles network errors without discarding already-rendered message fragments.
- **Context Management**: Configurable context window (`maxContextMessages`) automatically bounds outgoing multi-turn history to prevent out-of-context crashes on local small-context models.
