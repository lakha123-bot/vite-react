package com.example.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BatteryInfo(
    val percentage: Int,
    val isCharging: Boolean,
    val statusText: String,
    val temperatureCelsius: Float
)

data class InstalledAppInfo(
    val name: String,
    val packageName: String
)

sealed class DeviceActionResult {
    data class Success(val message: String, val spokenFeedback: String) : DeviceActionResult()
    data class Error(val error: String) : DeviceActionResult()
}

class DeviceAssistantManager(private val context: Context) {
    private val TAG = "DeviceAssistantManager"
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var isTorchOn = false

    // Flashlight Control
    fun setFlashlight(enabled: Boolean): DeviceActionResult {
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enabled)
                isTorchOn = enabled
                val stateText = if (enabled) "ACTIVATED" else "DEACTIVATED"
                val speech = if (enabled) "Flashlight is now active, Sir." else "Flashlight turned off, Sir."
                DeviceActionResult.Success("Flashlight $stateText", speech)
            } else {
                DeviceActionResult.Error("No camera flash hardware detected on this device.")
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access error", e)
            DeviceActionResult.Error("Camera flash access error: ${e.localizedMessage}")
        } catch (e: Throwable) {
            Log.e(TAG, "Flashlight error", e)
            DeviceActionResult.Error("Flashlight error: ${e.localizedMessage}")
        }
    }

    fun toggleFlashlight(): DeviceActionResult {
        return setFlashlight(!isTorchOn)
    }

    fun isFlashlightActive(): Boolean = isTorchOn

    // Volume Control
    fun getMediaVolume(): Pair<Int, Int> {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return Pair(current, max)
    }

    fun setMediaVolumePercentage(percent: Int): DeviceActionResult {
        val bounded = percent.coerceIn(0, 100)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = ((bounded / 100.0) * max).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return DeviceActionResult.Success(
            "Media Volume set to $bounded%",
            "Media volume adjusted to $bounded percent, Sir."
        )
    }

    fun adjustVolume(delta: Int): DeviceActionResult {
        val (current, max) = getMediaVolume()
        val newVol = (current + delta).coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, AudioManager.FLAG_SHOW_UI)
        val pct = ((newVol.toDouble() / max) * 100).toInt()
        return DeviceActionResult.Success("Volume adjusted to $pct%", "Volume set to $pct percent, Sir.")
    }

    // Battery Status
    fun getBatteryInfo(): BatteryInfo {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, filter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val tempRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0

        val pct = if (level != -1 && scale != -1) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Standard"
        }
        val tempC = tempRaw / 10.0f
        return BatteryInfo(pct, isCharging, statusText, tempC)
    }

    // Installed Apps
    fun getInstalledApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val list = pm.queryIntentActivities(intent, 0)
        return list.mapNotNull { resolveInfo ->
            val appInfo = resolveInfo.activityInfo.applicationInfo
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 || appInfo.packageName.contains("google") || appInfo.packageName.contains("android")) {
                val label = pm.getApplicationLabel(appInfo).toString()
                InstalledAppInfo(name = label, packageName = appInfo.packageName)
            } else {
                null
            }
        }.distinctBy { it.packageName }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    fun launchAppByPackage(packageName: String): DeviceActionResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                DeviceActionResult.Success("Opened app $packageName", "Launching application now, Sir.")
            } else {
                DeviceActionResult.Error("Unable to locate launch intent for $packageName")
            }
        } catch (e: Throwable) {
            DeviceActionResult.Error("Could not launch application: ${e.localizedMessage}")
        }
    }

    fun launchAppByName(query: String): DeviceActionResult {
        val apps = getInstalledApps()
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        val match = apps.firstOrNull { it.name.lowercase(Locale.ROOT).contains(cleanQuery) }
        return if (match != null) {
            launchAppByPackage(match.packageName)
        } else {
            DeviceActionResult.Error("Application matching '$query' was not found on device.")
        }
    }

    // Launch Dial, Camera, Settings
    fun launchDialer(): DeviceActionResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            DeviceActionResult.Success("Dialer opened", "Opening telephone dialer, Sir.")
        } catch (e: Throwable) {
            DeviceActionResult.Error("Failed to open dialer: ${e.localizedMessage}")
        }
    }

    fun launchCamera(): DeviceActionResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            DeviceActionResult.Success("Camera opened", "Activating optical camera array, Sir.")
        } catch (e: Throwable) {
            // Fallback generic capture intent
            try {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                DeviceActionResult.Success("Camera opened", "Camera ready, Sir.")
            } catch (err: Throwable) {
                DeviceActionResult.Error("Failed to open camera: ${err.localizedMessage}")
            }
        }
    }

    fun launchSettings(): DeviceActionResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            DeviceActionResult.Success("Android Settings opened", "Opening system settings, Sir.")
        } catch (e: Throwable) {
            DeviceActionResult.Error("Failed to open settings: ${e.localizedMessage}")
        }
    }

    // Time & Date
    fun getCurrentTimeDateString(): String {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    // Parse Natural Language Commands (English, Hindi, Hinglish)
    fun detectAndExecuteAction(prompt: String): DeviceActionResult? {
        val lower = prompt.trim().lowercase(Locale.ROOT)

        // Flashlight ON
        if (lower.contains("torch on") || lower.contains("flashlight on") ||
            lower.contains("turn on flashlight") || lower.contains("turn on torch") ||
            lower.contains("torch chalu") || lower.contains("flashlight chalu") ||
            lower.contains("torch jalao") || lower.contains("light on")
        ) {
            return setFlashlight(true)
        }

        // Flashlight OFF
        if (lower.contains("torch off") || lower.contains("flashlight off") ||
            lower.contains("turn off flashlight") || lower.contains("turn off torch") ||
            lower.contains("torch band") || lower.contains("flashlight band") ||
            lower.contains("torch bujhao") || lower.contains("light off")
        ) {
            return setFlashlight(false)
        }

        // Battery
        if (lower.contains("battery") || lower.contains("charge kitna") || lower.contains("kitni charging")) {
            val b = getBatteryInfo()
            val chargingMsg = if (b.isCharging) "and is currently charging" else "on battery power"
            val hindiCharging = if (b.isCharging) "aur phone charge ho raha hai" else ""
            val spoken = if (lower.contains("kitna") || lower.contains("kitni")) {
                "Aapki battery ${b.percentage} percent hai, $hindiCharging, Sir."
            } else {
                "Battery power is at ${b.percentage} percent, $chargingMsg, Sir."
            }
            return DeviceActionResult.Success(
                "Battery: ${b.percentage}% (${b.statusText}) | Temp: ${b.temperatureCelsius}°C",
                spoken
            )
        }

        // Camera
        if (lower.contains("open camera") || lower.contains("camera kholo") ||
            lower.contains("launch camera") || lower.contains("photo khicho")
        ) {
            return launchCamera()
        }

        // Dialer
        if (lower.contains("open dialer") || lower.contains("dialer kholo") ||
            lower.contains("phone lagao") || lower.contains("call lagao") ||
            lower.contains("open phone")
        ) {
            return launchDialer()
        }

        // Android Settings
        if (lower.contains("open settings") || lower.contains("settings kholo") || lower.contains("system settings")) {
            return launchSettings()
        }

        // Volume adjustment
        if (lower.contains("volume") || lower.contains("awaz")) {
            val percentRegex = Regex("(\\d+)\\s*%")
            val match = percentRegex.find(lower)
            if (match != null) {
                val pct = match.groupValues[1].toIntOrNull() ?: 50
                return setMediaVolumePercentage(pct)
            }
            if (lower.contains("badhao") || lower.contains("up") || lower.contains("increase") || lower.contains("zyada")) {
                return adjustVolume(2)
            }
            if (lower.contains("kam") || lower.contains("down") || lower.contains("decrease") || lower.contains("ghatao")) {
                return adjustVolume(-2)
            }
            if (lower.contains("mute") || lower.contains("shant")) {
                return setMediaVolumePercentage(0)
            }
        }

        // App Launch (e.g. "open youtube", "youtube kholo", "open whatsapp", "whatsapp chalu karo")
        val appPrefixes = listOf("open ", "launch ", "chalu karo ", "kholo ")
        for (prefix in appPrefixes) {
            if (lower.startsWith(prefix)) {
                val appTarget = lower.removePrefix(prefix).trim()
                if (appTarget.isNotEmpty() && !listOf("camera", "dialer", "settings", "torch", "flashlight").contains(appTarget)) {
                    return launchAppByName(appTarget)
                }
            }
        }

        // Time / Date
        if (lower.contains("time kya") || lower.contains("what time") ||
            lower.contains("date kya") || lower.contains("what is the date") ||
            lower.contains("samay kya") || lower.contains("tarikh")
        ) {
            val td = getCurrentTimeDateString()
            return DeviceActionResult.Success("Current time: $td", "The current time is $td, Sir.")
        }

        return null
    }
}
