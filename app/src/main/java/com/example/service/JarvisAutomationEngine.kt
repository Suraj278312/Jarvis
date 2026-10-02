package com.example.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import com.example.data.local.JarvisRepository
import com.example.data.local.NoteEntity
import com.example.data.model.ScreenInfo

class JarvisAutomationEngine(
    private val context: Context,
    private val repository: JarvisRepository
) {
    companion object {
        private const val TAG = "JarvisAutomationEngine"
    }

    // --- App Launching ---

    fun openApp(appNameOrPackage: String): String {
        val pm = context.packageManager
        val query = appNameOrPackage.trim().lowercase()

        // Direct package launch check
        val directIntent = pm.getLaunchIntentForPackage(appNameOrPackage)
        if (directIntent != null) {
            directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(directIntent)
            return "Successfully opened application ($appNameOrPackage)."
        }

        // Fuzzy search by app label
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString().lowercase()
            val pkg = info.activityInfo.packageName
            if (label.contains(query) || pkg.lowercase().contains(query)) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "Opened ${info.loadLabel(pm)} ($pkg)."
                }
            }
        }

        // Specific well-known app fallbacks
        when (query) {
            "youtube" -> return openWebOrApp("https://www.youtube.com", "com.google.android.youtube")
            "maps", "google maps" -> return openWebOrApp("geo:0,0?q=", "com.google.android.apps.maps")
            "chrome", "browser" -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                return "Opened Web Browser."
            }
            "camera" -> {
                val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(cameraIntent)
                return "Opened Camera."
            }
        }

        return "Could not find an installed application matching '$appNameOrPackage'."
    }

    private fun openWebOrApp(url: String, packageName: String? = null): String {
        val pm = context.packageManager
        if (packageName != null) {
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "Opened $packageName."
            }
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
        return "Opened $url."
    }

    // --- System Settings ---

    fun openSettingsScreen(settingType: String): String {
        val action = when (settingType.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
        return "Navigated to $settingType settings."
    }

    fun adjustVolume(direction: String): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val adjustDir = when (direction.lowercase()) {
            "up", "increase", "raise" -> AudioManager.ADJUST_RAISE
            "down", "decrease", "lower" -> AudioManager.ADJUST_LOWER
            "mute" -> AudioManager.ADJUST_MUTE
            "unmute" -> AudioManager.ADJUST_UNMUTE
            else -> AudioManager.ADJUST_SAME
        }
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, adjustDir, AudioManager.FLAG_SHOW_UI)
        return "Volume adjusted ($direction)."
    }

    // --- Web Search ---

    fun searchWeb(query: String): String {
        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(android.app.SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(searchIntent)
            "Searching the web for '$query'."
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            "Opened browser search for '$query'."
        }
    }

    // --- Accessibility Automation ---

    fun readScreen(): ScreenInfo? {
        val service = JarvisAccessibilityService.getInstance()
        return service?.inspectCurrentScreen()
    }

    fun tapScreenElement(target: String): Boolean {
        val service = JarvisAccessibilityService.getInstance()
        return service?.clickElement(target) ?: false
    }

    fun typeIntoField(text: String, hint: String? = null): Boolean {
        val service = JarvisAccessibilityService.getInstance()
        return service?.typeText(text, hint) ?: false
    }

    fun scrollScreen(forward: Boolean): Boolean {
        val service = JarvisAccessibilityService.getInstance()
        return service?.scroll(forward) ?: false
    }

    fun navigateSystem(action: String): Boolean {
        val service = JarvisAccessibilityService.getInstance()
        return service?.navigateGlobal(action) ?: false
    }

    // --- Communications ---

    fun lookupContactNumber(name: String): String? {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex != -1) {
                    return it.getString(numberIndex)
                }
            }
        }
        return null
    }

    fun makePhoneCall(numberOrContact: String): String {
        val resolvedNumber = lookupContactNumber(numberOrContact) ?: numberOrContact.replace("[^0-9+]".toRegex(), "")
        if (resolvedNumber.isBlank()) {
            return "Could not determine phone number for '$numberOrContact'."
        }

        val hasCallPerm = context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val action = if (hasCallPerm) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val callIntent = Intent(action, Uri.parse("tel:$resolvedNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(callIntent)
        return "Initiating call to $resolvedNumber."
    }

    fun sendSms(numberOrContact: String, message: String): String {
        val resolvedNumber = lookupContactNumber(numberOrContact) ?: numberOrContact.replace("[^0-9+]".toRegex(), "")
        if (resolvedNumber.isBlank()) {
            return "Could not determine destination phone number for '$numberOrContact'."
        }

        val hasSmsPerm = context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        if (hasSmsPerm) {
            return try {
                val smsManager = context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                smsManager.sendTextMessage(resolvedNumber, null, message, null, null)
                "SMS sent successfully to $resolvedNumber."
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending SMS directly, opening SMS app", e)
                openSmsApp(resolvedNumber, message)
            }
        } else {
            return openSmsApp(resolvedNumber, message)
        }
    }

    private fun openSmsApp(number: String, message: String): String {
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$number")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(smsIntent)
        return "Opened messaging composer for $number with pre-filled message."
    }

    // --- Notes & Memory Management ---

    suspend fun createNote(title: String, content: String, tags: String = "voice"): String {
        val note = NoteEntity(title = title, content = content, tags = tags)
        val id = repository.insertNote(note)
        return "Note created successfully (ID: $id, Title: '$title')."
    }

    suspend fun readNotesSummary(query: String? = null): String {
        val notes = if (query.isNullOrBlank()) {
            repository.getNotesList()
        } else {
            repository.getNotesList().filter {
                it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
            }
        }
        if (notes.isEmpty()) {
            return "No notes found matching your request."
        }
        return notes.take(5).joinToString("\n---\n") {
            "ID: ${it.id} | Title: ${it.title}\nContent: ${it.content}"
        }
    }

    suspend fun saveMemoryFact(category: String, key: String, value: String): String {
        repository.insertMemory(category, key, value)
        return "Remembered: '$key' is '$value' under [$category]."
    }
}
