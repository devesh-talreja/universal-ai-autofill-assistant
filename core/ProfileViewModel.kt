package com.example.smartautofiller.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartautofiller.data.AppDatabase
import com.example.smartautofiller.data.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.userProfileDao()

    val allProfiles: StateFlow<List<UserProfile>> = dao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProfile(profile: UserProfile) {
        viewModelScope.launch {
            if (profile.id == 0) dao.insertProfile(profile)
            else dao.updateProfile(profile)
        }
    }

    fun deleteProfile(profile: UserProfile) {
        viewModelScope.launch { dao.deleteProfile(profile) }
    }

    // ── Root Detection ────────────────────────────────────────
    fun isDeviceRooted(): Boolean {
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su",
            "/data/local/su", "/su/bin/su"
        )
        rootPaths.forEach { path -> if (File(path).exists()) return true }

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val result = process.inputStream.bufferedReader().readLine()
            !result.isNullOrBlank()
        } catch (e: Exception) { false }
    }

    // ── Export ────────────────────────────────────────────────
    fun exportProfiles(context: Context, onResult: (success: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            try {
                val profiles = dao.getAllProfilesList()
                if (profiles.isEmpty()) { onResult(false, "No profiles to export!"); return@launch }

                val json = Json.encodeToString(profiles)
                val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "AI_Autofill_Backup_$dateStr.json"
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.writeText(json)
                onResult(true, "✓ Exported to Downloads/$fileName")
            } catch (e: Exception) {
                onResult(false, "Export failed: ${e.message}")
            }
        }
    }

    // ── Import ────────────────────────────────────────────────
    fun importProfiles(context: Context, uri: Uri, onResult: (success: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val json = inputStream?.bufferedReader()?.readText() ?: run {
                    onResult(false, "Cannot read file!"); return@launch
                }
                inputStream.close()

                val profiles = Json.decodeFromString<List<UserProfile>>(json)
                if (profiles.isEmpty()) { onResult(false, "No profiles found!"); return@launch }

                profiles.forEach { dao.insertProfile(it.copy(id = 0)) }
                onResult(true, "✓ ${profiles.size} profile(s) imported!")
            } catch (e: Exception) {
                onResult(false, "Import failed: Invalid file format")
            }
        }
    }
}