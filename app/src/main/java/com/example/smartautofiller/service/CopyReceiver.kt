package com.example.smartautofiller.service

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class CopyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val textToCopy = intent?.getStringExtra("text") ?: return
        val label = intent.getStringExtra("label") ?: "Data"

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SmartAutofill", textToCopy)
        clipboard.setPrimaryClip(clip)

        // ✅ FIX: Android 13+ pe Toast background se restricted hai
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "$label Copied!", Toast.LENGTH_SHORT).show()
        }

        // ✅ FIX: Sensitive data 30 second baad clipboard se clear karo
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val currentClip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                if (currentClip == textToCopy) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            } catch (_: Exception) {}
        }, 30_000L)
    }
}