# Stitch MCP Integration & Design System Specification

## 1. Stitch MCP Connectivity & Discovery Audit

In accordance with Phase 1 & Phase 2 and Step 1 of the Mandatory Google Stitch MCP Integration requirements:
- **Server Inspected**: `https://stitch.googleapis.com/mcp` registered in `~/.gemini/antigravity-ide/mcp_config.json`.
- **Tools Discovered in Schema**:
  - `create_project`, `get_project`, `delete_project`, `list_projects`
  - `create_design_system`, `create_design_system_from_design_md`, `update_design_system`, `list_design_systems`, `apply_design_system`
  - `generate_screen_from_text`, `edit_screens`, `generate_variants`, `get_screen`, `list_screens`, `download_file`, `upload_design_md`
- **Execution Diagnostic**:
  Direct STDIO proxy test via `npx -y mcp-remote` connected successfully to `https://stitch.googleapis.com/mcp` using `StreamableHTTPClientTransport`. Upon tool invocation, the Google OAuth authorization endpoint rejected unauthenticated dynamic client registration:
  ```json
  {"jsonrpc":"2.0","id":2,"error":{"code":-32001,"message":"mcp-remote: Incompatible auth server: does not support dynamic client registration"}}
  ```
  Per Rule: *"If Stitch MCP is unavailable, document the issue and provide setup steps. Do not falsely claim Stitch was used."*

### Setup Steps to Authenticate Stitch MCP
1. Provide a valid Google Cloud OAuth2 client ID/secret or service account authorized for Stitch API.
2. In `mcp_config.json`, pass the bearer token: `--header "Authorization: Bearer <GCP_ACCESS_TOKEN>"` or refresh token configuration.
3. Once authenticated, `list_projects` and `generate_screen_from_text` can be executed directly.

---

## 2. Complete Application Visual Design Specifications (13 Screens)

The design follows the exact Design Brief specified in the Master Prompt:
- **Dark charcoal background**: Surface `0xFF111318`, Background `0xFF0C0E12`, Card/Container `0xFF191C24`
- **Accent colors**: Subtle violet (`0xFF8C7CFF`) and electric blue (`0xFF4E95FF`)
- **Typography**: Clean modern sans-serif with distinct weight hierarchy (Medium 500 for headers, Regular 400 for message bodies, SemiBold 600 for labels)
- **Geometry**: Rounded corners (16dp cards, 24dp floating pill composer, 20dp dialogs)
- **High text contrast**: Primary text `0xFFF1F3F9`, Secondary text `0xFFA2A7B5`, Code text `0xFFE0E6F0`
- **Zero neon/harsh glow**: Velvet-soft elevation tints and subtle 1dp border strokes (`0x1FFFFFFF`)

### Screen 1: Splash Screen
- **Background**: Deep charcoal gradient from `#0C0E12` to `#141722`.
- **Center**: Nexus AI glowing hexagon logo with Violet/Blue gradient accent.
- **Title**: "NEXUS AI" (28sp, Bold, Tracking 2sp) with subtitle "Local Intelligence. Absolute Privacy." (14sp, Gray).
- **Bottom**: Subtle animated linear loading indicator and version `1.0.0`.

### Screen 2: First-Time Welcome Screen (Onboarding)
- **Visuals**: Feature highlights with polished icons:
  - 🔒 100% Local & Private (LM Studio inference on your local network).
  - ⚡ Real-Time Token Streaming (Low latency SSE stream).
  - 💬 ChatGPT-style conversational experience with Markdown & Code formatting.
- **Action**: "Get Started" primary pill button transitioning to LM Studio connection setup.

### Screen 3: LM Studio Connection Setup
- **Top**: Navigation bar with "Server Setup" title and Help/Troubleshooting icon.
- **Cards**:
  - IP Address input field (`192.168.x.x` with validation and hint).
  - Port field (`1234` default with numeric keyboard).
  - Quick-preset chips (`localhost`, `10.0.2.2` for emulator, `Auto-detect Wi-Fi`).
  - Connection status badge: Live ping latency pill (Connected / Connecting / Disconnected / Error).
- **Action Buttons**:
  - "Test Connection" (triggers `/v1/models` check).
  - "Save & Connect" (persists to DataStore and fetches models).
- **Troubleshooting Accordion**: Step-by-step instructions for Windows Firewall and LM Studio Server toggle.

### Screen 4: Chat Home Screen (Empty Conversation State)
- **Top Bar**: Nexus AI icon, Model Selector dropdown pill, New Chat button, Drawer icon.
- **Center Hero Layout**:
  - AI Orb avatar with subtle pulse.
  - Heading: "How can I help you today?" (24sp, SemiBold).
  - Grid of suggested prompt cards (14dp rounded, dark surface with hover/press reaction):
    1. "Explain a technical concept (e.g. Raft consensus)"
    2. "Help me write code (e.g. Kotlin Coroutine Flow)"
    3. "Analyze an error message or stack trace"
    4. "Summarize complex notes or documentation"
- **Bottom**: Floating message composer ready for input.

### Screen 5: Active Conversation Screen
- **Message List**: LazyColumn with smooth physics and automatic scroll-to-bottom anchor.
- **User Bubble**: Right-aligned, dark violet-tinted container (`#25213B`), rounded corners (18dp, bottom-right 4dp).
- **Assistant Bubble**: Left-aligned, dark charcoal surface (`#181B24`), full width with 8dp avatar icon.
- **Markdown & Code Rendering**:
  - Headings, bullet points, bold/italic formatting.
  - Code blocks with dark slate background (`#0A0C10`), language badge header, and one-tap "Copy Code" button.
- **Message Actions Toolbar**: Copy text, Edit message, Regenerate response, Share, Delete.
- **Streaming State**: Real-time token appending with soft glowing cursor indicator and active "Stop Generating" button.

### Screen 6: Conversation History Drawer
- **Header**: Nexus AI branding with total conversation count and storage usage.
- **Search Bar**: Quick filter query field with clear button.
- **Sections**:
  - "Pinned / Favorites" (highlighted star items).
  - "Today", "Previous 7 Days", "Older".
- **Item Card**: Title, preview text snippet, timestamp, pin toggle, delete action with swipe-to-reveal.
- **Footer**: New Chat action button and Settings shortcut.

### Screen 7: AI Model Selection Dialog / Sheet
- **Modal BottomSheet**:
  - Header: "Select Language Model" with Refresh icon button.
  - List of models fetched live from LM Studio (`/v1/models`).
  - Selected model indicator (Checkmark and violet glow border).
  - Model details: Model ID, architecture/quantization tags (e.g. `Q4_K_M`, `GGUF`), context window indicator.
  - Empty/Error state: "No models found. Please load a model in LM Studio."

### Screen 8: AI Settings
- **Sliders & Inputs**:
  - Temperature slider (`0.0` to `2.0`, default `0.7`).
  - Maximum output tokens (`256` to `8192`, default `2048`).
  - System prompt text area with expandable template presets (e.g., "Concise Expert", "Coding Specialist", "Creative Writer").
  - Context window strategy: "Send last N messages" slider or "Full chat history".

### Screen 9: Appearance & Preferences Settings
- **Theme Selection**: System Default, Dark Charcoal (OLED), Dark Indigo.
- **Accent Color**: Violet (`#8C7CFF`), Electric Blue (`#4E95FF`), Emerald Teal (`#2DD4BF`).
- **Typography Scale**: Compact, Standard, Comfortable.
- **Code Block Wrap**: Toggle line wrapping in code blocks.

### Screen 10: Chat Search
- **Dedicated Search Screen**:
  - Search input with instant search as you type.
  - Highlighted match snippets within conversation messages.
  - Direct navigation to matched message within the chat session.

### Screen 11: Empty Conversation State
- Clean, uncluttered layout with quick starter chips and model status bar.

### Screen 12: Connection Error State
- **Full-Screen / Banner Component**:
  - Icon: Broken link / Server offline.
  - Headline: "Unable to reach LM Studio".
  - Error Details: HTTP error code, connection refused, or timeout.
  - Quick Actions: "Retry", "Edit Connection IP", "Diagnostic Guide".

### Screen 13: About Application Screen
- **Content**:
  - Nexus AI v1.0.0.
  - 100% Local Processing & Open Source Privacy Guarantee.
  - Architecture: Jetpack Compose, Material 3, Clean Architecture, Room, OkHttp SSE.
  - Built for pairing with LM Studio Windows Server.
