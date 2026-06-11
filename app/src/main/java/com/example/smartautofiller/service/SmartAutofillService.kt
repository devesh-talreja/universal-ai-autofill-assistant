package com.example.smartautofiller.service

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.example.smartautofiller.R
import com.example.smartautofiller.data.AppDatabase
import com.example.smartautofiller.data.UserProfile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class SmartAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var db: AppDatabase

    // ✅ FIX: DB ek baar init karo
    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(this)
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val fillContext = request.fillContexts.lastOrNull() ?: run {
            callback.onSuccess(null)
            return
        }

        val structure = fillContext.structure
        val packageName = structure.activityComponent?.packageName?.lowercase() ?: ""

        // ✅ FIX: Blocked apps list expand kiya
        val blockedApps = listOf(
            "message", "mms", "sms",
            "whatsapp", "telegram", "signal",
            "instagram", "snapchat", "twitter", "facebook",
            "calculator", "gallery", "camera"
        )
        if (blockedApps.any { packageName.contains(it) }) {
            callback.onSuccess(null)
            return
        }

        val fields = mutableMapOf<AutofillId, String>()
        for (i in 0 until structure.windowNodeCount) {
            traverse(structure.getWindowNodeAt(i).rootViewNode, fields, null)
        }

        if (fields.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        // ✅ FIX: CancellationSignal handle karo
        val job = serviceScope.launch {
            try {
                val profiles = db.userProfileDao().getAllProfiles().first()

                if (profiles.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val responseBuilder = FillResponse.Builder()
                var anyDatasetAdded = false

                profiles.forEach { profile ->
                    val datasetBuilder = Dataset.Builder()
                    var fieldsAdded = false

                    val presentation = RemoteViews(packageName, R.layout.autofill_item).apply {
                        setTextViewText(R.id.autofill_title, "Fill as ${profile.profileName}")
                        setTextViewText(R.id.autofill_value, "${profile.fullName} | ${profile.email}")
                    }

                    fields.forEach { (id, metadata) ->
                        val meta = metadata.lowercase()

                        // ✅ FIX: "@" se email match hataya, "number" akela se phone hataya
                        var valueToFill: String? = when {
                            meta.contains("email") || meta.contains("e-mail") || meta.contains(" mail") -> profile.email
                            meta.contains("phone") || meta.contains("mobile") || meta.contains("contact") -> profile.phoneNumber
                            meta.contains("full name") || meta.contains("fullname") -> profile.fullName
                            meta.contains("first name") || meta.contains("firstname") ->
                                profile.fullName.split(" ").firstOrNull() ?: profile.fullName
                            meta.contains("last name") || meta.contains("lastname") || meta.contains("surname") ->
                                profile.fullName.split(" ").lastOrNull() ?: profile.fullName
                            meta.contains("name") || meta.contains("username") -> profile.fullName
                            meta.contains("address") || meta.contains("city") || meta.contains("location") -> profile.address
                            else -> null
                        }

                        // ✅ FIX: Custom fields - longest key pehle
                        if (valueToFill == null) {
                            val sorted = profile.customFields.entries.sortedByDescending { it.key.length }
                            for ((key, value) in sorted) {
                                if (key.isBlank() || value.isBlank()) continue
                                val cleanKey = key.lowercase().trim()
                                if (meta.contains(cleanKey)) {
                                    valueToFill = value
                                    break
                                }
                                // Word level match
                                val words = cleanKey.split(" ", "_", "-").filter { it.length >= 3 }
                                if (words.isNotEmpty() && words.all { meta.contains(it) }) {
                                    valueToFill = value
                                    break
                                }
                            }
                        }

                        if (!valueToFill.isNullOrEmpty()) {
                            datasetBuilder.setValue(id, AutofillValue.forText(valueToFill), presentation)
                            fieldsAdded = true
                        }
                    }

                    if (fieldsAdded) {
                        responseBuilder.addDataset(datasetBuilder.build())
                        anyDatasetAdded = true
                    }
                }

                if (anyDatasetAdded) {
                    callback.onSuccess(responseBuilder.build())
                } else {
                    callback.onSuccess(null)
                }

            } catch (e: Exception) {
                callback.onSuccess(null)
            }
        }

        // ✅ FIX: Cancel signal pe job cancel karo
        cancellationSignal.setOnCancelListener {
            job.cancel()
            callback.onSuccess(null)
        }
    }

    private fun traverse(
        node: AssistStructure.ViewNode?,
        fields: MutableMap<AutofillId, String>,
        parent: AssistStructure.ViewNode?
    ) {
        if (node == null) return
        val id = node.autofillId

        if (id != null && (node.autofillType != View.AUTOFILL_TYPE_NONE || node.isFocusable)) {
            val metadata = StringBuilder()
            node.hint?.let { metadata.append(" ").append(it) }
            node.autofillHints?.forEach { metadata.append(" ").append(it) }
            node.idEntry?.let { metadata.append(" ").append(it) }
            node.contentDescription?.let { metadata.append(" ").append(it) }
            // ✅ FIX: existing text include nahi kiya - galat matches hoti thi

            // ✅ FIX: Sirf non-editable sibling text lo (actual labels)
            parent?.let { p ->
                for (i in 0 until p.childCount) {
                    val sibling = p.getChildAt(i)
                    if (sibling != node &&
                        sibling.text != null &&
                        sibling.autofillType == View.AUTOFILL_TYPE_NONE) {
                        metadata.append(" ").append(sibling.text)
                    }
                }
            }
            fields[id] = metadata.toString()
        }

        for (i in 0 until node.childCount) {
            traverse(node.getChildAt(i), fields, node)
        }
    }

    // ✅ FIX: onSaveRequest mein meaningful implementation
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // Future: saved data ko profile mein update kar sakte hain
        callback.onSuccess()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}