# Windows LM Studio Setup & Local Network Guide

To pair your Android phone with LM Studio running on your Windows PC, follow these steps:

---

## 1. Install & Launch LM Studio
1. Download and install **LM Studio for Windows** from [lmstudio.ai](https://lmstudio.ai).
2. Launch LM Studio.

## 2. Download or Select a Language Model
1. Go to the **Search** tab (magnifying glass) in LM Studio.
2. Search for a modern instruct model (e.g., `Qwen2.5-7B-Instruct`, `Llama-3.1-8B-Instruct`, or `Mistral-7B-Instruct`).
3. Download a recommended quantized GGUF format (such as `Q4_K_M`).

## 3. Enable the Local Inference Server
1. Click the **Developer / Local Server** tab in the left sidebar (icon `<->`).
2. At the top, select the downloaded model from the dropdown to load it into memory.
3. In the right panel, configure the server:
   - **Port**: `1234` (default).
   - **Serve on Local Network (0.0.0.0)**: **Toggle ON**.
     > *Important*: By default, LM Studio only listens on `localhost (127.0.0.1)`. You must enable network serving so your phone can reach it.
4. Click **Start Server**.

---

## 4. Find Your Windows Laptop's Local IP Address
1. Open PowerShell on Windows and run:
   ```powershell
   ipconfig
   ```
2. Look for your active Wi-Fi adapter's **IPv4 Address** (e.g., `192.168.1.150`).

---

## 5. Configure Windows Defender Firewall
Ensure your firewall permits incoming connections on port `1234`:
```powershell
New-NetFirewallRule -DisplayName "LM Studio Local AI Server" -Direction Inbound -LocalPort 1234 -Protocol TCP -Action Allow
```

---

## 6. Configure Nexus AI on Android
1. Connect your Android phone to the **same Wi-Fi network** as your Windows laptop.
2. Open **Nexus AI**.
3. Go to **Server Setup** (or tap the Connection badge at the top).
4. Enter your laptop's IPv4 address (e.g. `192.168.1.150`) and port `1234`.
5. Tap **Test Connection**. Nexus AI will query `/v1/models`.
6. Once marked **Connected**, tap **Save & Apply**.
7. Start chatting with real-time token streaming!
