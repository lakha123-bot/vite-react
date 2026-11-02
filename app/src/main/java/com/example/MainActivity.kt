package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.JarvisTab
import com.example.ui.components.SciFiBottomNav
import com.example.ui.components.SciFiHudHeader
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardVoiceScreen
import com.example.ui.screens.DeviceCommandsScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpaceBlack
import com.example.ui.viewmodel.JarvisViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val jarvisViewModel: JarvisViewModel = viewModel()

                var currentTab by remember { mutableStateOf(JarvisTab.VOICE_HUD) }

                // Runtime Permissions Launcher
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    // Permissions granted / denied handled gracefully
                }

                val requestPermissions: () -> Unit = {
                    val needed = mutableListOf<String>()
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        needed.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        needed.add(Manifest.permission.CAMERA)
                    }
                    if (needed.isNotEmpty()) {
                        permissionsLauncher.launch(needed.toTypedArray())
                    }
                }

                // Handle Back Button navigation
                BackHandler(enabled = currentTab != JarvisTab.VOICE_HUD) {
                    currentTab = JarvisTab.VOICE_HUD
                }

                val batteryInfo by jarvisViewModel.batteryInfo.collectAsState()
                val voiceState by jarvisViewModel.voiceState.collectAsState()
                val geminiModel by jarvisViewModel.geminiModel.collectAsState()

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SpaceBlack),
                    containerColor = SpaceBlack,
                    topBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            SciFiHudHeader(
                                batteryInfo = batteryInfo,
                                voiceState = voiceState,
                                geminiModel = geminiModel,
                                onSettingsClick = { currentTab = JarvisTab.SETTINGS }
                            )
                        }
                    },
                    bottomBar = {
                        SciFiBottomNav(
                            selectedTab = currentTab,
                            onTabSelected = { currentTab = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            JarvisTab.VOICE_HUD -> {
                                DashboardVoiceScreen(
                                    viewModel = jarvisViewModel,
                                    onRequestRecordAudioPermission = requestPermissions
                                )
                            }
                            JarvisTab.CHAT -> {
                                ChatScreen(
                                    viewModel = jarvisViewModel,
                                    onRequestRecordAudioPermission = requestPermissions
                                )
                            }
                            JarvisTab.DEVICE -> {
                                DeviceCommandsScreen(
                                    viewModel = jarvisViewModel
                                )
                            }
                            JarvisTab.MEMORY -> {
                                MemoryScreen(
                                    viewModel = jarvisViewModel
                                )
                            }
                            JarvisTab.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = jarvisViewModel,
                                    onRequestRecordAudioPermission = requestPermissions
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
