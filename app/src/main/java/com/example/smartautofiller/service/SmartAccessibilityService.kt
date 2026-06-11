package com.example.smartautofiller.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.smartautofiller.R
import com.example.smartautofiller.data.AppDatabase
import com.example.smartautofiller.data.UserProfile
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

class SmartAccessibilityService : AccessibilityService() {

    private var floatingView: View? = null
    private var selectorView: View? = null
    private var windowManager: WindowManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var db: AppDatabase
    private var isFilling = false
    private var currentPackageName: String = ""
    private var cachedPageLanguage: String = TranslateLanguage.ENGLISH
    private var lastCachedPage: String = ""
    private var lastFocusedNode: AccessibilityNodeInfo? = null

    companion object {
        var instance: SmartAccessibilityService? = null

        val SUPPORTED_LANGUAGES = mapOf(
            TranslateLanguage.HINDI    to "Hindi",
            TranslateLanguage.BENGALI  to "Bengali",
            TranslateLanguage.TAMIL    to "Tamil",
            TranslateLanguage.TELUGU   to "Telugu",
            TranslateLanguage.MARATHI  to "Marathi",
            TranslateLanguage.GUJARATI to "Gujarati",
            TranslateLanguage.KANNADA  to "Kannada",
            TranslateLanguage.ENGLISH  to "English",
        )

        val STANDARD_SHORTCUTS = mapOf(
            "name-"     to "fullName",
            "naam-"     to "fullName",
            "email-"    to "email",
            "mail-"     to "email",
            "gmail-"    to "email",
            "mob-"      to "phoneNumber",
            "phone-"    to "phoneNumber",
            "mobile-"   to "phoneNumber",
            "number-"   to "phoneNumber",
            "no-"       to "phoneNumber",
            "addr-"     to "address",
            "address-"  to "address",
            "pata-"     to "address",
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        db = AppDatabase.getDatabase(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.packageName?.toString()?.let { if (it.isNotBlank()) currentPackageName = it }
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            event?.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            val node = event.source
            if (node != null && (node.isEditable || node.className?.contains("EditText") == true)) {
                lastFocusedNode = node
            }
        }
    }

    fun setBubbleVisible(visible: Boolean) {
        if (visible) { if (floatingView == null) showFloatingBubble() }
        else removeFloatingBubble()
    }

    private fun showFloatingBubble() {
        if (windowManager == null) return
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_floating_bubble, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0; params.y = 200

        val longPressHandler = Handler(Looper.getMainLooper())
        var isLongPress = false
        var longPressRunnable: Runnable? = null

        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0; private var initialY = 0
            private var initialTouchX = 0f; private var initialTouchY = 0f
            private var isMoveAction = false

            override fun onTouch(v: View?, event: android.view.MotionEvent?): Boolean {
                when (event?.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        initialX = params.x; initialY = params.y
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        isMoveAction = false; isLongPress = false
                        longPressRunnable = Runnable { isLongPress = true; showProfileSelector(params.x, params.y) }
                        longPressHandler.postDelayed(longPressRunnable!!, 600)
                        return true
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX; val dy = event.rawY - initialTouchY
                        if (abs(dx) > 10 || abs(dy) > 10) { isMoveAction = true; longPressHandler.removeCallbacks(longPressRunnable!!) }
                        params.x = initialX + dx.toInt(); params.y = initialY + dy.toInt()
                        windowManager?.updateViewLayout(floatingView, params)
                        return true
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        longPressHandler.removeCallbacks(longPressRunnable!!)
                        val size = Point(); windowManager?.defaultDisplay?.getSize(size)
                        params.x = params.x.coerceIn(0, size.x - 150); params.y = params.y.coerceIn(0, size.y - 150)
                        windowManager?.updateViewLayout(floatingView, params)
                        if (!isMoveAction && !isLongPress) { v?.performClick(); fillFormSmart(null) }
                        return true
                    }
                }
                return false
            }
        })
        windowManager?.addView(floatingView, params)
    }

    private fun showProfileSelector(bubbleX: Int, bubbleY: Int) {
        serviceScope.launch {
            val profiles = db.userProfileDao().getAllProfiles().first()
            if (profiles.isEmpty()) { Toast.makeText(this@SmartAccessibilityService, "No profiles found!", Toast.LENGTH_SHORT).show(); return@launch }
            removeSelectorView()
            val inflater = LayoutInflater.from(this@SmartAccessibilityService)
            val selectorLayout = inflater.inflate(R.layout.layout_profile_selector, null)
            val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
            val size = Point(); windowManager?.defaultDisplay?.getSize(size)
            params.gravity = Gravity.TOP or Gravity.START
            params.x = (bubbleX + 120).coerceIn(0, size.x - 300); params.y = bubbleY.coerceIn(0, size.y - 400)
            val container = selectorLayout.findViewById<android.widget.LinearLayout>(R.id.profile_container)
            val closeBtn = selectorLayout.findViewById<android.widget.TextView>(R.id.btn_close_selector)
            profiles.forEach { profile ->
                val itemView = inflater.inflate(R.layout.layout_profile_item, container, false)
                itemView.findViewById<android.widget.TextView>(R.id.profile_item_name).text = profile.profileName
                itemView.findViewById<android.widget.TextView>(R.id.profile_item_sub).text = profile.fullName
                itemView.findViewById<android.widget.TextView>(R.id.profile_item_avatar).text = profile.profileName.take(1).uppercase()
                itemView.setOnClickListener { removeSelectorView(); fillFormSmart(profile) }
                container.addView(itemView)
            }
            closeBtn.setOnClickListener { removeSelectorView() }
            selectorView = selectorLayout
            windowManager?.addView(selectorLayout, params)
            Handler(Looper.getMainLooper()).postDelayed({ removeSelectorView() }, 5000)
        }
    }

    private fun removeSelectorView() { selectorView?.let { try { windowManager?.removeView(it) } catch (_: Exception) {}; selectorView = null } }
    private fun removeFloatingBubble() { removeSelectorView(); floatingView?.let { try { windowManager?.removeView(it) } catch (_: Exception) {}; floatingView = null } }

    // ── Smart Fill ────────────────────────────────────────────
    private fun fillFormSmart(selectedProfile: UserProfile?) {
        if (isFilling) return
        isFilling = true

        serviceScope.launch {
            try {
                val allProfiles = db.userProfileDao().getAllProfiles().first()
                if (allProfiles.isEmpty()) {
                    Toast.makeText(this@SmartAccessibilityService, "No profiles! Please add one.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val profile = selectedProfile ?: detectBestProfile(allProfiles)
                val focusedNode = getFocusedEditableNode()

                if (focusedNode != null) {
                    val currentText = focusedNode.text?.toString() ?: ""

                    // ── TEXT EXPANSION MODE ──
                    // Check karo koi shortcut hai kisi bhi line mein jo abhi fill nahi hai
                    val filled = tryFillAllShortcuts(focusedNode, currentText, profile)

                    if (!filled) {
                        // Koi shortcut nahi mila — normal form fill karo
                        fillAllFields(profile)
                    }
                } else {
                    fillAllFields(profile)
                }
            } catch (e: Exception) {
                Toast.makeText(this@SmartAccessibilityService, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isFilling = false
            }
        }
    }

    // ── Fill ALL shortcuts in text at once ────────────────────
    /**
     * Text mein saari lines scan karta hai.
     * Jo line shortcut se end ho rahi hai aur fill nahi hui — uske aage value add karta hai.
     *
     * Example:
     * Input:  "Name-\nGmail-\nMobile-"
     * Output: "Name- Pushpraj Singhal\nGmail- pushpraj@gmail.com\nMobile- 8109678194"
     */
    private fun tryFillAllShortcuts(node: AccessibilityNodeInfo, currentText: String, profile: UserProfile): Boolean {
        val lines = currentText.lines().toMutableList()
        var anyFilled = false

        for (i in lines.indices) {
            val line = lines[i]
            val lineLower = line.trim().lowercase()

            // Check karo line already filled hai ya nahi
            // Filled line = shortcut ke baad kuch text hai
            var matchedShortcut = false

            // Standard shortcuts check karo
            for ((shortcut, fieldKey) in STANDARD_SHORTCUTS) {
                if (lineLower == shortcut || lineLower.endsWith(shortcut)) {
                    // Line sirf shortcut hai — fill karo
                    val value = when (fieldKey) {
                        "fullName"    -> profile.fullName
                        "email"       -> profile.email
                        "phoneNumber" -> profile.phoneNumber
                        "address"     -> profile.address
                        else          -> null
                    }
                    if (!value.isNullOrBlank()) {
                        lines[i] = "$line $value"
                        anyFilled = true
                        matchedShortcut = true
                        break
                    }
                }
            }

            // Custom fields check karo
            if (!matchedShortcut) {
                for ((key, value) in profile.customFields) {
                    val shortcut = "${key.lowercase().replace(" ", "")}-"
                    if (lineLower == shortcut || lineLower.endsWith(shortcut)) {
                        if (value.isNotBlank()) {
                            lines[i] = "$line $value"
                            anyFilled = true
                            matchedShortcut = true
                            break
                        }
                    }
                }
            }

            // ✅ Section fields bhi check karo
            if (!matchedShortcut) {
                outer@ for (section in profile.sections) {
                    for (field in section.fields) {
                        if (field.label.isBlank() || field.value.isBlank()) continue
                        // Multiple shortcut variants try karo
                        val variants = listOf(
                            "${field.label.lowercase().replace(" ", "")}-",      // aadhaarnumber-
                            "${field.label.lowercase().replace(" ", "_")}-",     // aadhaar_number-
                            "${field.label.lowercase().split(" ").first()}-",    // aadhaar-
                            "${field.label.lowercase()}-"                        // aadhaar number-
                        )
                        for (shortcut in variants) {
                            if (lineLower == shortcut || lineLower.endsWith(shortcut)) {
                                lines[i] = "$line ${field.value}"
                                anyFilled = true
                                matchedShortcut = true
                                break@outer
                            }
                        }
                    }
                }
            }
        }

        if (anyFilled) {
            val newText = lines.joinToString("\n")
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            Toast.makeText(this, "✓ Done!", Toast.LENGTH_SHORT).show()
        }

        return anyFilled
    }

    private fun getFocusedEditableNode(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val allNodes = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(rootNode, allNodes)
        val focused = allNodes.firstOrNull { it.isFocused && (it.isEditable || it.className?.contains("EditText") == true) }
        if (focused != null) return focused
        return lastFocusedNode?.takeIf { it.isEditable || it.className?.contains("EditText") == true }
    }

    private suspend fun fillAllFields(profile: UserProfile) {
        val rootNode = rootInActiveWindow ?: run {
            Toast.makeText(this@SmartAccessibilityService, "Cannot read screen. Try again.", Toast.LENGTH_SHORT).show()
            return
        }
        android.util.Log.d("SmartFill", "Profile: ${profile.profileName}, Sections: ${profile.sections.size}, Fields: ${profile.sections.sumOf { it.fields.size }}, SectionData: ${getAllSectionFields(profile)}")
        val allNodes = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(rootNode, allNodes)
        val editableNodes = allNodes.filter { it.isEditable || it.className?.contains("EditText") == true }
        val labelNodes = allNodes.filter { !it.isEditable && it.text != null && it.text.isNotBlank() && it.text.length < 60 }
        val pageLanguage = detectPageLanguage(labelNodes)
        var filledCount = 0

        // ── 1. Text fields fill karo ──────────────────────────
        editableNodes.forEach { editNode ->
            val editBounds = Rect(); editNode.getBoundsInScreen(editBounds)
            val hintText    = editNode.hintText?.toString()?.trim() ?: ""
            val contentDesc = editNode.contentDescription?.toString()?.trim() ?: ""
            val viewId      = editNode.viewIdResourceName?.substringAfterLast("/")?.replace("_", " ")?.trim() ?: ""
            val bestLabel   = findBestLabel(editBounds, labelNodes)
            val fullContext = listOf(hintText, contentDesc, viewId, bestLabel).filter { it.isNotBlank() }.joinToString(" ").lowercase().trim()
            if (fullContext.isBlank()) return@forEach

            // ✅ Custom/Section fields PEHLE check karo, phir standard
            val englishValue = matchCustomField(fullContext, profile)
                ?: matchStandardField(fullContext, profile)
                ?: run {
                    if (pageLanguage != TranslateLanguage.ENGLISH) {
                        val translated = translateToEnglish(bestLabel.ifBlank { fullContext }, pageLanguage)
                        val tl = translated.lowercase()
                        matchCustomField(tl, profile) ?: matchStandardField(tl, profile)
                    } else null
                }

            if (!englishValue.isNullOrEmpty()) {
                val finalValue = if (pageLanguage != TranslateLanguage.ENGLISH && SUPPORTED_LANGUAGES.containsKey(pageLanguage))
                    translateText(englishValue, TranslateLanguage.ENGLISH, pageLanguage) else englishValue
                val args = Bundle()
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, finalValue)
                if (editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) filledCount++
            }
        }

        // ── 2. Auto-click: Radio/Checkbox/Button nodes ────────
        val clickableNodes = allNodes.filter { node ->
            val cls = node.className?.toString() ?: ""
            (node.isClickable || cls.contains("RadioButton") || cls.contains("CheckBox") || cls.contains("Button") || cls.contains("Switch")) &&
                    !node.isEditable && node.text != null && node.text.isNotBlank()
        }

        // Profile se saare values collect karo
        val allValues = mutableListOf<String>()
        allValues.add(profile.fullName.lowercase())
        allValues.add(profile.email.lowercase())
        allValues.add(profile.phoneNumber.lowercase())
        allValues.add(profile.address.lowercase())
        profile.customFields.values.forEach { allValues.add(it.lowercase()) }
        profile.sections.forEach { section -> section.fields.forEach { field -> allValues.add(field.value.lowercase()) } }

        // Gender specific matching
        val gender = profile.customFields["Gender"]?.lowercase() ?: ""
        val dob    = profile.customFields["Date of Birth"] ?: ""
        val country = profile.customFields["Country"]?.lowercase() ?: ""
        val state   = profile.customFields["State"]?.lowercase() ?: ""

        clickableNodes.forEach { node ->
            val nodeText = node.text?.toString()?.trim()?.lowercase() ?: return@forEach
            val nodeDesc = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
            val combined = "$nodeText $nodeDesc"

            var shouldClick = false

            // Gender match
            if (gender.isNotBlank()) {
                when (gender) {
                    "male"   -> if (combined.matches(Regex(".*\\b(male|m|mr|shri|श्री|पुरुष|man|boy|masculine)\\b.*")) && !combined.contains("female")) shouldClick = true
                    "female" -> if (combined.matches(Regex(".*\\b(female|f|ms|mrs|smt|महिला|woman|girl|feminine)\\b.*"))) shouldClick = true
                    "other"  -> if (combined.matches(Regex(".*\\b(other|prefer not|third|तृतीय)\\b.*"))) shouldClick = true
                }
            }

            // Country/State match
            if (!shouldClick && country.isNotBlank()) {
                if (combined.contains(country) || (country == "india" && (combined.contains("india") || combined.contains("भारत")))) shouldClick = true
            }
            if (!shouldClick && state.isNotBlank()) {
                if (combined.contains(state.lowercase())) shouldClick = true
            }

            // DOB match — day/month/year
            if (!shouldClick && dob.isNotBlank()) {
                val dobParts = dob.split("/")
                if (dobParts.size == 3) {
                    val d = dobParts[0]; val m = dobParts[1]; val y = dobParts[2]
                    if (nodeText == d || nodeText == m || nodeText == y || nodeText == dob) shouldClick = true
                }
            }

            // Custom fields match — koi bhi value
            if (!shouldClick) {
                profile.customFields.forEach { (key, value) ->
                    val v = value.lowercase()
                    if (v.isNotBlank() && v.length > 1 && combined.contains(v)) shouldClick = true
                }
            }

            // Section fields match
            if (!shouldClick) {
                profile.sections.forEach { section ->
                    section.fields.forEach { field ->
                        val v = field.value.lowercase()
                        if (v.isNotBlank() && v.length > 1 && combined.contains(v)) shouldClick = true
                    }
                }
            }

            if (shouldClick && !node.isChecked) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                filledCount++
                android.util.Log.d("SmartFill", "Auto-clicked: $nodeText")
            }
        }

        allNodes.forEach { try { it.recycle() } catch (_: Exception) {} }
        val langName = SUPPORTED_LANGUAGES[pageLanguage] ?: "English"
        val msg = if (filledCount > 0) "✓ $filledCount field(s) filled in $langName" else "No matching fields found."
        Toast.makeText(this@SmartAccessibilityService, msg, Toast.LENGTH_SHORT).show()
    }

    private suspend fun detectPageLanguage(labelNodes: List<AccessibilityNodeInfo>): String {
        val pageKey = currentPackageName + labelNodes.take(3).map { it.text }.joinToString()
        if (pageKey == lastCachedPage) return cachedPageLanguage
        val allText = labelNodes.take(10).mapNotNull { it.text?.toString() }.joinToString(" ").take(200)
        if (allText.isBlank()) return TranslateLanguage.ENGLISH
        return try {
            val langId = LanguageIdentification.getClient()
            val langCode = langId.identifyLanguage(allText).await()
            val detected = when (langCode) {
                "hi" -> TranslateLanguage.HINDI; "bn" -> TranslateLanguage.BENGALI
                "ta" -> TranslateLanguage.TAMIL; "te" -> TranslateLanguage.TELUGU
                "mr" -> TranslateLanguage.MARATHI; "gu" -> TranslateLanguage.GUJARATI
                "kn" -> TranslateLanguage.KANNADA; else -> TranslateLanguage.ENGLISH
            }
            cachedPageLanguage = detected; lastCachedPage = pageKey
            langId.close(); detected
        } catch (e: Exception) { TranslateLanguage.ENGLISH }
    }

    private suspend fun translateText(text: String, fromLang: String, toLang: String): String {
        if (fromLang == toLang) return text
        return try {
            val options = TranslatorOptions.Builder().setSourceLanguage(fromLang).setTargetLanguage(toLang).build()
            val translator = Translation.getClient(options)
            translator.downloadModelIfNeeded(com.google.mlkit.common.model.DownloadConditions.Builder().build()).await()
            val result = translator.translate(text).await()
            translator.close(); result
        } catch (e: Exception) { text }
    }

    private suspend fun translateToEnglish(text: String, fromLang: String): String = translateText(text, fromLang, TranslateLanguage.ENGLISH)

    private suspend fun detectBestProfile(profiles: List<UserProfile>): UserProfile {
        if (profiles.size == 1) return profiles.first()
        val pageText = StringBuilder(currentPackageName.lowercase())
        rootInActiveWindow?.let { root ->
            val allNodes = mutableListOf<AccessibilityNodeInfo>()
            findAllNodes(root, allNodes)
            allNodes.forEach { node -> node.text?.toString()?.let { pageText.append(" ").append(it.lowercase()) } }
        }
        val ctx = pageText.toString()
        val proKW = listOf("job","work","company","office","professional","career","linkedin","naukri","resume","cv")
        val perKW = listOf("personal","home","family","private","individual","contact","hobby")
        data class PS(val profile: UserProfile, var score: Int = 0)
        val scores = profiles.map { PS(it) }
        scores.forEach { ps ->
            val pn = ps.profile.profileName.lowercase()
            if (proKW.any { pn.contains(it) } && proKW.any { ctx.contains(it) }) ps.score += 10
            if (perKW.any { pn.contains(it) } && perKW.any { ctx.contains(it) }) ps.score += 10
            ps.profile.customFields.keys.forEach { key -> if (ctx.contains(key.lowercase())) ps.score += 5 }
            if (ctx.contains(pn)) ps.score += 8
        }
        return scores.maxByOrNull { it.score }?.profile ?: profiles.first()
    }

    private fun findBestLabel(editBounds: Rect, labelNodes: List<AccessibilityNodeInfo>): String {
        data class LC(val text: String, val score: Int)
        val candidates = mutableListOf<LC>()
        labelNodes.forEach { ln ->
            val lb = Rect(); ln.getBoundsInScreen(lb)
            val lt = ln.text?.toString()?.trim() ?: return@forEach
            if (lt.contains("@") || lt.contains("http") || lt.contains("indicates") || lt.contains("required question")) return@forEach
            val hd = abs(lb.centerX() - editBounds.centerX()); val vd = editBounds.top - lb.bottom
            var score = when {
                vd in 0..250 && hd < 300 -> 100; vd in 0..400 && hd < 200 -> 80
                abs(lb.centerY() - editBounds.centerY()) < 80 && hd < 400 -> 60
                vd in -100..400 && hd < 350 -> 40; else -> return@forEach
            }
            if (hd < 100) score += 20
            candidates.add(LC(lt, score))
        }
        return candidates.maxByOrNull { it.score }?.text ?: ""
    }

    private fun matchStandardField(context: String, profile: UserProfile): String? {
        // ✅ Pehle check karo context mein koi specific keyword toh nahi jo standard field se match na kare
        val specificKeywords = listOf("aadhaar", "aadhar", "pan", "passport", "voter", "driving", "account", "ifsc", "branch", "roll", "enrol", "registration", "application", "pin code", "pincode", "zip")
        if (specificKeywords.any { context.contains(it) }) return null // Custom field handle karega

        return when {
            containsWord(context, "email") || containsWord(context, "mail") || context.contains("e-mail") -> profile.email.takeIf { it.isNotBlank() }
            containsWord(context, "phone") || containsWord(context, "mobile") || containsWord(context, "contact") || context.contains("phone number") || context.contains("mobile number") -> profile.phoneNumber.takeIf { it.isNotBlank() }
            context.contains("full name") || context.contains("fullname") -> profile.fullName.takeIf { it.isNotBlank() }
            context.contains("first name") || containsWord(context, "firstname") -> profile.fullName.split(" ").firstOrNull()?.takeIf { it.isNotBlank() }
            context.contains("last name") || containsWord(context, "lastname") || containsWord(context, "surname") -> profile.fullName.split(" ").lastOrNull()?.takeIf { it.isNotBlank() }
            containsWord(context, "name") -> profile.fullName.takeIf { it.isNotBlank() }
            containsWord(context, "address") || containsWord(context, "location") -> profile.address.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    private fun matchCustomField(context: String, profile: UserProfile): String? {
        // ✅ PRIORITY 1: Section fields — longer label = more specific = higher priority
        val allSectionFields = profile.sections.flatMap { it.fields }
            .filter { it.label.isNotBlank() && it.value.isNotBlank() }
            .sortedByDescending { it.label.length }

        for (field in allSectionFields) {
            val ck = field.label.lowercase().trim()
            val words = ck.split(" ", "_", "-").filter { it.length >= 3 }

            // Exact full match — highest priority
            if (containsWord(context, ck) || context.contains(ck)) return field.value

            // All words must match (e.g. "aadhaar number" → both "aadhaar" and "number" in context)
            if (words.size > 1 && words.all { context.contains(it) }) return field.value

            // First significant word match (only if 6+ chars to avoid false matches)
            val firstWord = words.firstOrNull() ?: continue
            if (firstWord.length >= 6 && containsWord(context, firstWord)) return field.value
        }

        // ✅ PRIORITY 2: Custom fields — longer key = more specific
        val sorted = profile.customFields.entries.sortedByDescending { it.key.length }
        for ((key, value) in sorted) {
            if (key.isBlank() || value.isBlank()) continue
            val ck = key.lowercase().trim()
            val words = ck.split(" ", "_", "-").filter { it.length >= 3 }

            if (containsWord(context, ck) || context.contains(ck)) return value
            if (words.size > 1 && words.all { context.contains(it) }) return value
            if (words.any { it.length >= 6 && containsWord(context, it) }) return value
        }

        return null
    }

    // ── Debug: Get all section fields as string ───────────────
    private fun getAllSectionFields(profile: UserProfile): String {
        val sb = StringBuilder()
        profile.sections.forEach { section ->
            section.fields.forEach { field ->
                sb.append("${field.label}=${field.value} | ")
            }
        }
        return sb.toString()
    }

    private fun containsWord(context: String, word: String): Boolean {
        if (context == word) return true
        return Regex("(^|[^a-z0-9])${Regex.escape(word)}([^a-z0-9]|$)").containsMatchIn(context)
    }

    private fun findAllNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        list.add(node)
        for (i in 0 until node.childCount) { findAllNodes(node.getChild(i) ?: continue, list) }
    }

    override fun onInterrupt() {}
    override fun onDestroy() {
        super.onDestroy()
        removeFloatingBubble()
        instance = null
        serviceScope.cancel()
    }
}