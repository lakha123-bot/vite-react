package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.LanguageMode
import com.example.data.model.Personality
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowPurple
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SpaceBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    onRequestRecordAudioPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apiKey by viewModel.geminiApiKey.collectAsState()
    val model by viewModel.geminiModel.collectAsState()
    val personalityId by viewModel.personalityId.collectAsState()
    val languageMode by viewModel.languageMode.collectAsState()
    val ttsPitch by viewModel.ttsPitch.collectAsState()
    val ttsSpeed by viewModel.ttsSpeed.collectAsState()
    val autoSpeak by viewModel.autoSpeak.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()

    var tempApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

    val supportedModels = listOf(
        "gemini-3.8-flash" to "Gemini 3.8 Flash (Default / Balanced)",
        "gemini-3.5-flash-lite" to "Gemini 3.5 Flash-Lite (Fast)",
        "gemini-4-argon" to "Gemini 4 Argon (Reasoning)"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SYSTEM CONFIGURATION // SETTINGS",
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = NeonCyan
        )

        // Gemini API Configuration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gemini_config_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = "API Key", tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GEMINI API CONFIGURATION", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    val hasKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (hasKey) NeonGreen.copy(alpha = 0.2f) else NeonAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (hasKey) "ACTIVE" else "KEY NEEDED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasKey) NeonGreen else NeonAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your Google Gemini API key here or configure it in AI Studio Secrets panel. This key is stored securely in on-device DataStore.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = tempApiKey,
                    onValueChange = { tempApiKey = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            viewModel.updateApiKey(tempApiKey)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = SpaceBlack),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_api_key_btn")
                    ) {
                        Text("SAVE API KEY", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Model Selection
                Text("NEURAL MODEL", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                supportedModels.forEach { (modId, label) ->
                    val isSelected = modId == model
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DeepNavy else Color.Transparent)
                            .border(1.dp, if (isSelected) NeonCyan else CardBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateModel(modId) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) NeonCyan else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = label, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Voice & Audio Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("voice_config_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Voice", tint = NeonPurple, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SYNTHESIS & VOICE OUTPUT", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto speak toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto-Speak AI Responses", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Vocalize text via TextToSpeech automatically", fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = { viewModel.updateAutoSpeak(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = SpaceBlack, checkedTrackColor = NeonPurple)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pitch slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Speech Pitch: ${"%.1f".format(ttsPitch)}x", fontSize = 12.sp, color = TextPrimary)
                }
                Slider(
                    value = ttsPitch,
                    onValueChange = { viewModel.updateTtsSettings(it, ttsSpeed) },
                    valueRange = 0.5f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple, inactiveTrackColor = DeepNavy)
                )

                // Speed slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Speech Speed: ${"%.1f".format(ttsSpeed)}x", fontSize = 12.sp, color = TextPrimary)
                }
                Slider(
                    value = ttsSpeed,
                    onValueChange = { viewModel.updateTtsSettings(ttsPitch, it) },
                    valueRange = 0.5f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple, inactiveTrackColor = DeepNavy)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Language Mode
                Text("SPEECH LANGUAGE MODE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguageMode.values().forEach { mode ->
                        val isSel = languageMode == mode.code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) DeepNavy else CardSurface)
                                .border(1.dp, if (isSel) NeonCyan else CardBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateLanguageMode(mode.code) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.displayName.substringBefore(" "),
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) NeonCyan else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // AI Personality Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("personality_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "Personality", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI PERSONALITY CORE", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Personality.ALL.forEach { p ->
                    val isSelected = p.id == personalityId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) DeepNavy else CardSurface)
                            .border(1.dp, if (isSelected) NeonCyan else CardBorder, RoundedCornerShape(10.dp))
                            .clickable { viewModel.updatePersonality(p.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) NeonCyan else TextPrimary)
                            Text(p.title, fontSize = 11.sp, color = TextMuted)
                        }
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // App & Creator Showcase Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("about_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepNavy),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowCyan)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = NeonCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LAKHA JARVIS AI",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardSurface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "CREATOR: LAKHA RATHOD",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "A premier futuristic native Android AI assistant. Powered by Google Gemini with real-time streaming, voice synthesis, Room database memory, and native hardware orchestration.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
