package com.griaai.app

import android.app.Application
import com.griaai.app.core.database.GriaDatabaseHelper
import com.griaai.app.core.datastore.SettingsDataStore
import com.griaai.app.core.llm.OnDeviceLlmEngine
import com.griaai.app.core.network.LmStudioStreamClient
import com.griaai.app.core.telemetry.HardwareMonitor
import com.griaai.app.core.voice.OfflineVoiceEngine
import com.griaai.app.data.repository.ChatRepositoryImpl
import com.griaai.app.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GriaApplication : Application() {

    lateinit var databaseHelper: GriaDatabaseHelper
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

        databaseHelper = GriaDatabaseHelper(this)
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
        }
    }

    companion object {
        lateinit var instance: GriaApplication
            private set
    }
}
