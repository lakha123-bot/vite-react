package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.JarvisDatabase
import com.example.data.local.JarvisPreferences
import com.example.data.model.ChatMessage
import com.example.data.model.MemoryItem
import com.example.data.model.MessageRole
import com.example.data.model.Personality
import com.example.data.model.VoiceState
import com.example.data.repository.JarvisRepository
import com.example.device.BatteryInfo
import com.example.device.DeviceActionResult
import com.example.device.DeviceAssistantManager
import com.example.device.InstalledAppInfo
import com.example.device.SpeechManager
import com.example.gemini.ChatHistoryItem
import com.example.gemini.GeminiApiService
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisOverlayService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "JarvisViewModel"

    private val database = JarvisDatabase.getInstance(application)
    val preferences = JarvisPreferences(application)
    val repository = JarvisRepository(database.jarvisDao(), preferences)
    val deviceManager = DeviceAssistantManager(application)
    private val geminiApi = GeminiApiService()

    // Live State
    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _isLiveVoiceSessionActive = MutableStateFlow(false)
    val isLiveVoiceSessionActive: StateFlow<Boolean> = _isLiveVoiceSessionActive.asStateFlow()

    private val _liveTranscription = MutableStateFlow("")
    val liveTranscription: StateFlow<String> = _liveTranscription.asStateFlow()

    private val _liveJarvisResponse = MutableStateFlow("")
    val liveJarvisResponse: StateFlow<String> = _liveJarvisResponse.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _batteryInfo = MutableStateFlow(deviceManager.getBatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    private val _isFlashlightOn = MutableStateFlow(deviceManager.isFlashlightActive())
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val _mediaVolume = MutableStateFlow(deviceManager.getMediaVolume())
    val mediaVolume: StateFlow<Pair<Int, Int>> = _mediaVolume.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    // Preferences Flows
    val geminiApiKey = preferences.geminiApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val geminiModel = preferences.geminiModel.stateIn(viewModelScope, SharingStarted.Eagerly, "gemini-3.8-flash")
    val personalityId = preferences.personalityId.stateIn(viewModelScope, SharingStarted.Eagerly, "jarvis")
    val languageMode = preferences.languageMode.stateIn(viewModelScope, SharingStarted.Eagerly, "auto")
    val ttsPitch = preferences.ttsPitch.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val ttsSpeed = preferences.ttsSpeed.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val autoSpeak = preferences.autoSpeak.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val overlayEnabled = preferences.overlayEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val chatMessages = repository.messages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val aiMemories = repository.memories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isAccessibilityActive = JarvisAccessibilityService.isServiceActive

    private var speechManager: SpeechManager? = null
    val audioRms = MutableStateFlow(0f)

    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedInitialMemoryIfEmpty()
            loadInstalledApps()
        }

        speechManager = SpeechManager(
            context = application,
            onSpeechResult = { recognizedText ->
                onUserSpeechReceived(recognizedText)
            },
            onError = { err ->
                Log.w(TAG, "Speech error: $err")
                _errorMessage.value = err
                if (!_isLiveVoiceSessionActive.value) {
                    _voiceState.value = VoiceState.IDLE
                } else {
                    // Try to re-listen after a short delay in live session
                    viewModelScope.launch {
                        delay(1200L)
                        if (_isLiveVoiceSessionActive.value) {
                            listenInternal()
                        }
                    }
                }
            }
        )

        // Observe audio RMS from speech manager
        viewModelScope.launch {
            while (true) {
                speechManager?.let { sm ->
                    audioRms.value = sm.audioRms.value
                    if (sm.isSpeaking.value && _voiceState.value != VoiceState.SPEAKING) {
                        _voiceState.value = VoiceState.SPEAKING
                    }
                }
                delay(50L)
            }
        }

        // Periodic battery and volume monitor
        viewModelScope.launch {
            while (true) {
                _batteryInfo.value = deviceManager.getBatteryInfo()
                _mediaVolume.value = deviceManager.getMediaVolume()
                _isFlashlightOn.value = deviceManager.isFlashlightActive()
                delay(3000L)
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            try {
                _installedApps.value = deviceManager.getInstalledApps()
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load apps", e)
            }
        }
    }

    // Live Voice Session Controls
    fun toggleLiveVoiceSession() {
        if (_isLiveVoiceSessionActive.value) {
            stopLiveVoiceSession()
        } else {
            startLiveVoiceSession()
        }
    }

    fun startLiveVoiceSession() {
        _isLiveVoiceSessionActive.value = true
        _errorMessage.value = null
        _liveTranscription.value = "Listening for your command..."
        listenInternal()
    }

    fun stopLiveVoiceSession() {
        _isLiveVoiceSessionActive.value = false
        speechManager?.stopListening()
        speechManager?.stopSpeaking()
        streamJob?.cancel()
        _voiceState.value = VoiceState.IDLE
        _liveTranscription.value = ""
        _liveJarvisResponse.value = ""
    }

    private fun listenInternal() {
        if (!_isLiveVoiceSessionActive.value) return
        _voiceState.value = VoiceState.LISTENING
        speechManager?.startListening()
    }

    private fun onUserSpeechReceived(text: String) {
        _liveTranscription.value = text
        _voiceState.value = VoiceState.THINKING

        // Check Native Device Action first
        val actionResult = deviceManager.detectAndExecuteAction(text)
        if (actionResult != null) {
            handleActionResult(text, actionResult)
            return
        }

        // If no direct action, query Gemini AI
        queryGeminiAi(text, isVoiceTrigger = true)
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        _errorMessage.value = null

        // Check if user requested a native device action directly
        val actionResult = deviceManager.detectAndExecuteAction(text)
        if (actionResult != null) {
            handleActionResult(text, actionResult)
            return
        }

        queryGeminiAi(text, isVoiceTrigger = false)
    }

    private fun handleActionResult(userQuery: String, result: DeviceActionResult) {
        viewModelScope.launch {
            // Save User message
            repository.saveMessage(
                ChatMessage(
                    role = MessageRole.USER,
                    content = userQuery,
                    timestamp = System.currentTimeMillis()
                )
            )

            when (result) {
                is DeviceActionResult.Success -> {
                    _isFlashlightOn.value = deviceManager.isFlashlightActive()
                    _mediaVolume.value = deviceManager.getMediaVolume()
                    _batteryInfo.value = deviceManager.getBatteryInfo()

                    _liveJarvisResponse.value = result.message

                    // Save JARVIS message
                    repository.saveMessage(
                        ChatMessage(
                            role = MessageRole.JARVIS,
                            content = result.message,
                            timestamp = System.currentTimeMillis(),
                            actionExecuted = result.message
                        )
                    )

                    speakOut(result.spokenFeedback)
                }
                is DeviceActionResult.Error -> {
                    _liveJarvisResponse.value = result.error
                    _errorMessage.value = result.error
                    repository.saveMessage(
                        ChatMessage(
                            role = MessageRole.JARVIS,
                            content = "Command alert: ${result.error}",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    speakOut("I encountered a problem executing that action, Sir: ${result.error}")
                }
            }
        }
    }

    private fun queryGeminiAi(prompt: String, isVoiceTrigger: Boolean) {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _voiceState.value = VoiceState.THINKING
            _liveJarvisResponse.value = "Analyzing..."

            // Save user prompt in DB
            repository.saveMessage(
                ChatMessage(
                    role = MessageRole.USER,
                    content = prompt,
                    timestamp = System.currentTimeMillis()
                )
            )

            val key = geminiApiKey.value
            val model = geminiModel.value
            val pId = personalityId.value
            val personality = Personality.ALL.firstOrNull { it.id == pId } ?: Personality.JARVIS
            val memories = repository.memories.first()

            val memoryContext = if (memories.isNotEmpty()) {
                "\nUser Memory & System Facts:\n" + memories.joinToString("\n") { "- ${it.key}: ${it.value}" }
            } else ""

            val fullSystemPrompt = buildString {
                append(personality.promptModifier)
                append("\nSystem Name: LAKHA JARVIS AI (MK VII). Creator: LAKHA RATHOD.")
                append("\nYou have full native device capabilities. If the user asks you to perform native device controls (flashlight, camera, apps, volume, battery, dialer), confirm and guide them.")
                append("\nSupport Hindi, English, and Hinglish naturally. Keep spoken answers concise, elegant, and impactful.")
                append(memoryContext)
            }

            val history = chatMessages.value.map {
                ChatHistoryItem(
                    role = if (it.role == MessageRole.USER) "user" else "model",
                    text = it.content
                )
            }

            val accumulatedResponse = StringBuilder()

            try {
                geminiApi.streamGenerateContent(
                    apiKey = key,
                    model = model,
                    systemInstruction = fullSystemPrompt,
                    history = history,
                    newPrompt = prompt
                ).collect { chunk ->
                    accumulatedResponse.append(chunk)
                    _liveJarvisResponse.value = accumulatedResponse.toString()
                }

                val finalAnswer = accumulatedResponse.toString().trim()
                if (finalAnswer.isNotEmpty()) {
                    // Save response
                    repository.saveMessage(
                        ChatMessage(
                            role = MessageRole.JARVIS,
                            content = finalAnswer,
                            timestamp = System.currentTimeMillis()
                        )
                    )

                    speakOut(finalAnswer)
                } else {
                    _voiceState.value = VoiceState.IDLE
                }

            } catch (e: Throwable) {
                Log.e(TAG, "Gemini generation error", e)
                val errText = e.localizedMessage ?: "Unknown AI transmission failure."
                _errorMessage.value = errText
                _liveJarvisResponse.value = "Alert: $errText"
                _voiceState.value = VoiceState.ERROR

                repository.saveMessage(
                    ChatMessage(
                        role = MessageRole.JARVIS,
                        content = "⚠️ $errText",
                        timestamp = System.currentTimeMillis()
                    )
                )

                if (isVoiceTrigger && _isLiveVoiceSessionActive.value) {
                    speakOut("Sir, connection to Gemini neural engine failed: $errText")
                }
            }
        }
    }

    private fun speakOut(text: String) {
        val shouldSpeak = autoSpeak.value || _isLiveVoiceSessionActive.value
        if (shouldSpeak) {
            _voiceState.value = VoiceState.SPEAKING
            speechManager?.updateTtsSettings(ttsPitch.value, ttsSpeed.value)
            speechManager?.speak(text, languageMode.value)

            // In live voice mode, when speech finishes, continue listening loop!
            if (_isLiveVoiceSessionActive.value) {
                viewModelScope.launch {
                    // Estimate speech duration or wait until speech stops
                    delay(800L)
                    while (speechManager?.isSpeaking?.value == true) {
                        delay(200L)
                    }
                    delay(400L)
                    if (_isLiveVoiceSessionActive.value) {
                        listenInternal()
                    } else {
                        _voiceState.value = VoiceState.IDLE
                    }
                }
            } else {
                viewModelScope.launch {
                    delay(800L)
                    while (speechManager?.isSpeaking?.value == true) {
                        delay(200L)
                    }
                    _voiceState.value = VoiceState.IDLE
                }
            }
        } else {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun speakTextDirectly(text: String) {
        speechManager?.updateTtsSettings(ttsPitch.value, ttsSpeed.value)
        speechManager?.speak(text, languageMode.value)
    }

    fun stopSpeaking() {
        speechManager?.stopSpeaking()
        _voiceState.value = VoiceState.IDLE
    }

    // Manual Device Action Triggers
    fun toggleFlashlight() {
        val res = deviceManager.toggleFlashlight()
        _isFlashlightOn.value = deviceManager.isFlashlightActive()
        if (res is DeviceActionResult.Success) {
            speakTextDirectly(res.spokenFeedback)
        }
    }

    fun setMediaVolume(percent: Int) {
        val res = deviceManager.setMediaVolumePercentage(percent)
        _mediaVolume.value = deviceManager.getMediaVolume()
        if (res is DeviceActionResult.Success) {
            speakTextDirectly(res.spokenFeedback)
        }
    }

    fun launchCamera() {
        deviceManager.launchCamera()
    }

    fun launchDialer() {
        deviceManager.launchDialer()
    }

    fun launchSettings() {
        deviceManager.launchSettings()
    }

    fun launchApp(packageName: String) {
        deviceManager.launchAppByPackage(packageName)
    }

    // Accessibility Automation Actions
    fun triggerAccessibilityHome(): Boolean = JarvisAccessibilityService.executeHome()
    fun triggerAccessibilityBack(): Boolean = JarvisAccessibilityService.executeBack()
    fun triggerAccessibilityNotifications(): Boolean = JarvisAccessibilityService.executeNotifications()
    fun triggerAccessibilityQuickSettings(): Boolean = JarvisAccessibilityService.executeQuickSettings()
    fun triggerAccessibilityLock(): Boolean = JarvisAccessibilityService.executeLockScreen()

    // Floating Overlay
    fun toggleOverlay(enable: Boolean) {
        viewModelScope.launch {
            preferences.setOverlayEnabled(enable)
            if (enable) {
                JarvisOverlayService.start(getApplication())
            } else {
                JarvisOverlayService.stop(getApplication())
            }
        }
    }

    // Memory Controls
    fun addMemory(key: String, value: String, category: String = "USER") {
        viewModelScope.launch {
            repository.addMemory(key, value, category)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearMemories()
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearMessages()
        }
    }

    // Settings Updates
    fun updateApiKey(key: String) {
        viewModelScope.launch { preferences.setGeminiApiKey(key) }
    }

    fun updateModel(model: String) {
        viewModelScope.launch { preferences.setGeminiModel(model) }
    }

    fun updatePersonality(id: String) {
        viewModelScope.launch { preferences.setPersonalityId(id) }
    }

    fun updateLanguageMode(mode: String) {
        viewModelScope.launch { preferences.setLanguageMode(mode) }
    }

    fun updateTtsSettings(pitch: Float, speed: Float) {
        viewModelScope.launch {
            preferences.setTtsPitch(pitch)
            preferences.setTtsSpeed(speed)
            speechManager?.updateTtsSettings(pitch, speed)
        }
    }

    fun updateAutoSpeak(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoSpeak(enabled) }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.release()
    }
}
