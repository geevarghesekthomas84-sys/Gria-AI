package com.nexusai.app

import android.app.Application
import com.nexusai.app.core.database.NexusDatabaseHelper
import com.nexusai.app.core.datastore.SettingsDataStore
import com.nexusai.app.core.llm.OnDeviceLlmEngine
import com.nexusai.app.core.network.LmStudioStreamClient
import com.nexusai.app.core.telemetry.HardwareMonitor
import com.nexusai.app.core.voice.OfflineVoiceEngine
import com.nexusai.app.data.repository.ChatRepositoryImpl
import com.nexusai.app.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NexusApplication : Application() {

    lateinit var databaseHelper: NexusDatabaseHelper
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var streamClient: LmStudioStreamClient
        private set

    lateinit var onDeviceEngine: OnDeviceLlmEngine
        private set

    lateinit var voiceEngine: OfflineVoiceEngine
        private set

    lateinit var hardwareMonitor: HardwareMonitor
        private set

    lateinit var chatRepository: ChatRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        databaseHelper = NexusDatabaseHelper(this)
        settingsDataStore = SettingsDataStore(this)
        streamClient = LmStudioStreamClient()
        onDeviceEngine = OnDeviceLlmEngine(this)
        voiceEngine = OfflineVoiceEngine(this)
        hardwareMonitor = HardwareMonitor(this, onDeviceEngine)
        chatRepository = ChatRepositoryImpl(
            dbHelper = databaseHelper,
            dataStore = settingsDataStore,
            streamClient = streamClient,
            onDeviceEngine = onDeviceEngine
        )

        CoroutineScope(Dispatchers.IO).launch {
            settingsDataStore.migrateOldSystemPrompt()
            runCatching {
                onDeviceEngine.tryAutoLoadAvailableModel()
            }
        }
    }

    companion object {
        lateinit var instance: NexusApplication
            private set
    }
}

