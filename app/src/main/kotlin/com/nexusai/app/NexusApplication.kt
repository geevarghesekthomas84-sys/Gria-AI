package com.nexusai.app

import android.app.Application
import com.nexusai.app.core.database.NexusDatabaseHelper
import com.nexusai.app.core.datastore.SettingsDataStore
import com.nexusai.app.core.llm.OnDeviceLlmEngine
import com.nexusai.app.core.network.LmStudioStreamClient
import com.nexusai.app.data.repository.ChatRepositoryImpl
import com.nexusai.app.domain.repository.ChatRepository

class NexusApplication : Application() {

    lateinit var databaseHelper: NexusDatabaseHelper
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var streamClient: LmStudioStreamClient
        private set

    lateinit var onDeviceEngine: OnDeviceLlmEngine
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
        chatRepository = ChatRepositoryImpl(
            dbHelper = databaseHelper,
            dataStore = settingsDataStore,
            streamClient = streamClient,
            onDeviceEngine = onDeviceEngine
        )
    }

    companion object {
        lateinit var instance: NexusApplication
            private set
    }
}

