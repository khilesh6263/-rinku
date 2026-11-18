package com.example

import android.app.Application
import com.example.data.local.AssistantPreferencesRepository
import com.example.data.local.CommandHistoryDatabase
import com.example.data.local.CommandHistoryRepository
import com.example.system.BabySpeechRecognizerManager
import com.example.system.BabyTtsManager
import com.example.system.CallActionExecutor
import com.example.system.CallSessionCoordinator
import com.example.system.PermissionCapabilityChecker

class BabyAssistantApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        container.ttsManager.shutdown()
        container.speechManager.cancelAndDestroy()
    }
}

class AppContainer(app: Application) {
    val preferencesRepository = AssistantPreferencesRepository(app)
    private val database = CommandHistoryDatabase.getInstance(app)
    val historyRepository = CommandHistoryRepository(database.commandHistoryDao())
    val capabilityChecker = PermissionCapabilityChecker(app)
    val ttsManager = BabyTtsManager(app)
    val speechManager = BabySpeechRecognizerManager(app)
    val actionExecutor = CallActionExecutor(app)
    val coordinator = CallSessionCoordinator(
        appContext = app,
        preferencesRepository = preferencesRepository,
        historyRepository = historyRepository,
        capabilityChecker = capabilityChecker,
        ttsManager = ttsManager,
        speechManager = speechManager,
        actionExecutor = actionExecutor
    )
}
