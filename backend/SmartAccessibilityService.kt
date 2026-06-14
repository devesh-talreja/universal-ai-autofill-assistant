package com.example.smartautofiller.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
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
            "mobileno-" to "phoneNumber",
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

            // Standard shortcuts check karo — EXACT match only
            // endsWith() use mat karo — "aadhar card no-" jaisi lines galat match ho jaati thi
            for ((shortcut, fieldKey) in STANDARD_SHORTCUTS) {
                if (lineLower == shortcut) {
                    // Line exactly shortcut hai — fill karo
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

            // Custom fields check karo — with smart suffix matching
            if (!matchedShortcut) {
                // Common suffixes jo user label ke saath type kar sakta hai
                // e.g. "aadhar card" key → "aadhar card no-", "aadhar card number-" bhi match hona chahiye
                val numberSuffixes = listOf(" no", " no.", " number", " num", " #", " card no", " card number")

                for ((key, value) in profile.customFields) {
                    if (value.isBlank()) continue
                    val keyLower = key.lowercase().trim()
                    val keyNoSpace = keyLower.replace(" ", "")

                    // ── All variants generate karo ──────────────────────────
                    val variants = mutableSetOf<String>()

                    // Basic variants
                    variants.add("$keyNoSpace-")            // "aadharcard-"
                    variants.add("$keyLower-")              // "aadhar card-"

                    // Suffix variants — "aadhar card no-", "aadhar card number-" etc.
                    for (suffix in numberSuffixes) {
                        variants.add("$keyLower$suffix-")                       // "aadhar card no-"
                        variants.add("$keyNoSpace${suffix.trim()}-")            // "aadhaercardno-"
                        variants.add("${keyLower}${suffix.replace(" ", "")}-")  // "aadharcard no-"
                    }

                    // ── Match check ─────────────────────────────────────────
                    if (lineLower in variants) {
                        lines[i] = "$line $value"
                        anyFilled = true
                        matchedShortcut = true
                        break
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
                        // EXACT match only — endsWith() remove kiya (galat matching hoti thi)
                        for (shortcut in variants) {
                            if (lineLower == shortcut) {
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
        android.util.Log.d("SmartFill", "Filling: ${profile.profileName}, Sections: ${profile.sections.size}")
        val allNodes = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(rootNode, allNodes)
        val editableNodes = allNodes.filter { it.isEditable || it.className?.contains("EditText") == true }
        val labelNodes = allNodes.filter { !it.isEditable && it.text != null && it.text.isNotBlank() && it.text.length < 60 }
        val pageLanguage = detectPageLanguage(labelNodes)
        var filledCount = 0

        // ── 1. Text fields fill karo ──────────────────────────
        editableNodes.forEach { editNode ->
            val editBounds  = Rect(); editNode.getBoundsInScreen(editBounds)
            val hintText    = editNode.hintText?.toString()?.trim() ?: ""
            val contentDesc = editNode.contentDescription?.toString()?.trim() ?: ""
            val viewId      = editNode.viewIdResourceName?.substringAfterLast("/")?.replace("_", " ")?.trim() ?: ""
            val bestLabel   = findBestLabel(editBounds, labelNodes)

            // ✅ hintText/contentDesc most accurate — use them first
            // bestLabel from screen position can pick wrong field label
            val primaryContext = listOf(hintText, contentDesc).filter { it.isNotBlank() }.joinToString(" ").lowercase().trim()
            val fullContext = listOf(hintText, contentDesc, viewId, bestLabel).filter { it.isNotBlank() }.joinToString(" ").lowercase().trim()
            if (fullContext.isBlank()) return@forEach

            // Try primary context first (hint + contentDesc only — most accurate)
            val englishValue = if (primaryContext.isNotBlank()) {
                matchStandardField(primaryContext, profile)
                    ?: matchCustomField(primaryContext, profile)
            } else null
            // Fallback to full context (includes bestLabel from position)
                ?: matchStandardField(fullContext, profile)
                ?: matchCustomField(fullContext, profile)
                ?: run {
                    if (pageLanguage != TranslateLanguage.ENGLISH) {
                        val translated = translateToEnglish(bestLabel.ifBlank { fullContext }, pageLanguage)
                        val tl = translated.lowercase()
                        matchStandardField(tl, profile) ?: matchCustomField(tl, profile)
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

        // ── 2. Auto-click: Radio/Checkbox/Button/Spinner/WebView nodes ──
        // WebView mein nodes delay se load hote hain — wait karo
        val isWebView = currentPackageName?.contains("chrome") == true ||
                currentPackageName?.contains("browser") == true ||
                currentPackageName?.contains("firefox") == true ||
                currentPackageName?.contains("webview") == true ||
                allNodes.any { it.className?.contains("WebView") == true }

        android.util.Log.d("SmartClick", "Package: $currentPackageName | isWebView: $isWebView | totalNodes: ${allNodes.size}")

        if (isWebView) delay(1000) // WebView nodes fully load hone do

        // Fresh nodes lo after delay
        val freshRoot = rootInActiveWindow
        val freshNodes = mutableListOf<AccessibilityNodeInfo>()
        if (freshRoot != null) findAllNodes(freshRoot, freshNodes)
        val nodesToCheck = if (isWebView) freshNodes else allNodes

        // ✅ Debug — log all clickable/radio/checkbox nodes
        nodesToCheck.forEach { node ->
            val txt = node.text?.toString() ?: ""
            val cls = node.className?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val clickable = node.isClickable
            val checked = node.isChecked
            val checkable = node.isCheckable
            if ((cls.contains("Radio") || cls.contains("Check") || cls.contains("Switch") ||
                        cls.contains("Toggle") || checkable) &&
                (txt.isNotBlank() || desc.isNotBlank())) {
                android.util.Log.d("SmartClick", "Interactive: text='$txt' desc='$desc' class=$cls clickable=$clickable checked=$checked checkable=$checkable")
            }
        }

        // ✅ Helper: Node se uska effective text nikalo (own text, contentDescription, ya sibling label)
        fun getNodeEffectiveText(node: AccessibilityNodeInfo): String {
            // 1. Node ka apna text
            val ownText = node.text?.toString()?.trim() ?: ""
            if (ownText.isNotBlank()) return ownText

            // 2. contentDescription
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            if (desc.isNotBlank()) return desc

            // 3. Child text (kuch radio buttons mein text child node mein hota hai)
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val childText = child.text?.toString()?.trim() ?: ""
                if (childText.isNotBlank() && childText.length < 50) return childText
            }

            // 4. Sibling text — same parent ke TextViews se label lo
            val parent = node.parent
            if (parent != null) {
                val nodeBounds = Rect(); node.getBoundsInScreen(nodeBounds)
                var closestText = ""
                var closestDist = Int.MAX_VALUE

                for (i in 0 until parent.childCount) {
                    val sibling = parent.getChild(i) ?: continue
                    if (sibling == node) continue
                    val sibText = sibling.text?.toString()?.trim() ?: ""
                    if (sibText.isBlank() || sibText.length > 50) continue
                    // Sibling non-editable hona chahiye (label hona chahiye)
                    if (sibling.isEditable) continue

                    val sibBounds = Rect(); sibling.getBoundsInScreen(sibBounds)
                    val dist = abs(sibBounds.centerY() - nodeBounds.centerY()) + abs(sibBounds.centerX() - nodeBounds.centerX())
                    if (dist < closestDist) {
                        closestDist = dist
                        closestText = sibText
                    }
                }
                if (closestText.isNotBlank() && closestDist < 300) return closestText
            }

            return ""
        }

        // ✅ Collect all interactive nodes (radio, checkbox, switch, spinner items)
        val clickableNodes = nodesToCheck.filter { node ->
            val cls = node.className?.toString() ?: ""
            if (node.isEditable) return@filter false

            // Skip already checked radio/checkbox nodes
            // (but still include them for matching — we'll skip later if needed)

            if (isWebView) {
                // WebView mein: check for role, checkable, ya clickable with short text
                val effectiveText = getNodeEffectiveText(node).lowercase()
                if (effectiveText.isBlank()) return@filter false
                if (effectiveText.length > 60) return@filter false
                // Skip common non-interactive text
                if (effectiveText.contains("submit") || effectiveText.contains("indicates") ||
                    effectiveText.contains("never submit") || effectiveText.contains("switch accounts") ||
                    effectiveText.contains("not shared")) return@filter false

                node.isClickable || node.isCheckable ||
                        cls.contains("Radio") || cls.contains("Check") ||
                        // WebView mein clickable parent ho sakta hai
                        (node.parent?.isClickable == true)
            } else {
                val effectiveText = getNodeEffectiveText(node).lowercase()
                if (effectiveText.isBlank()) return@filter false

                node.isClickable || node.isCheckable ||
                        cls.contains("RadioButton") || cls.contains("CheckBox") ||
                        cls.contains("Button") || cls.contains("Switch") || cls.contains("Toggle") ||
                        cls.contains("CheckedTextView") || cls.contains("Spinner") ||
                        cls.contains("ToggleButton") || cls.contains("CompoundButton")
            }
        }

        android.util.Log.d("SmartClick", "Clickable nodes found: ${clickableNodes.size}")

        // Profile se gender/dob/country/state — case-insensitive search across all custom field key variants
        fun findCustomField(vararg keys: String): String {
            for (key in keys) {
                for ((k, v) in profile.customFields) {
                    if (k.equals(key, ignoreCase = true)) return v
                }
            }
            return ""
        }

        val gender  = findCustomField("Gender", "gender", "Sex", "sex", "लिंग").lowercase()
        val dob     = findCustomField("Date of Birth", "date of birth", "DOB", "dob", "Birth Date", "birth date", "जन्म तिथि")
        val country = findCustomField("Country", "country", "Nation", "nation", "देश").lowercase()
        val state   = findCustomField("State", "state", "Province", "province", "राज्य").lowercase()
        val city    = findCustomField("City", "city", "शहर").lowercase()
        val marital = findCustomField("Marital Status", "marital status", "वैवाहिक स्थिति").lowercase()
        val religion = findCustomField("Religion", "religion", "धर्म").lowercase()
        val category = findCustomField("Category", "category", "वर्ग", "Caste", "caste").lowercase()
        val nationality = findCustomField("Nationality", "nationality", "राष्ट्रीयता").lowercase()

        clickableNodes.forEach { node ->
            val effectiveText = getNodeEffectiveText(node).lowercase()
            if (effectiveText.isBlank()) return@forEach

            val nodeDesc = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
            val combined = "$effectiveText $nodeDesc".trim()
            val cls = node.className?.toString() ?: ""
            val isRadioOrCheck = node.isCheckable || cls.contains("Radio") || cls.contains("Check")

            var shouldClick = false

            // ✅ Gender match — male/female/other with comprehensive matching
            if (!shouldClick && gender.isNotBlank()) {
                val genderAliases = mapOf(
                    "male" to listOf("male", "m", "mr", "mr.", "shri", "श्री", "पुरुष", "puruṣ", "purush", "boy", "man"),
                    "female" to listOf("female", "f", "ms", "ms.", "mrs", "mrs.", "smt", "smt.", "श्रीमती", "महिला", "girl", "woman", "stri"),
                    "other" to listOf("other", "others", "prefer not to say", "prefer not", "third gender", "transgender", "non-binary", "अन्य")
                )

                val matchAliases = genderAliases[gender] ?: listOf(gender)
                for (alias in matchAliases) {
                    if (effectiveText == alias || effectiveText.startsWith("$alias ") || effectiveText.endsWith(" $alias")) {
                        // ✅ Extra check: "male" should not match "female"
                        if (gender == "male" && combined.contains("female")) continue
                        shouldClick = true
                        break
                    }
                }
            }

            // ✅ Marital status match
            if (!shouldClick && marital.isNotBlank()) {
                val maritalAliases = mapOf(
                    "married" to listOf("married", "विवाहित"),
                    "unmarried" to listOf("unmarried", "single", "अविवाहित"),
                    "divorced" to listOf("divorced", "तलाकशुदा"),
                    "widowed" to listOf("widowed", "widow", "विधवा", "विधुर")
                )
                val matchAliases = maritalAliases[marital] ?: listOf(marital)
                for (alias in matchAliases) {
                    if (effectiveText == alias || effectiveText.contains(alias)) {
                        shouldClick = true; break
                    }
                }
            }

            // ✅ Religion match
            if (!shouldClick && religion.isNotBlank()) {
                if (effectiveText == religion || effectiveText.contains(religion)) shouldClick = true
            }

            // ✅ Category match
            if (!shouldClick && category.isNotBlank()) {
                if (effectiveText == category || effectiveText.contains(category)) shouldClick = true
            }

            // ✅ Nationality match
            if (!shouldClick && nationality.isNotBlank()) {
                if (effectiveText == nationality || effectiveText.contains(nationality)) shouldClick = true
            }

            // ✅ Country match
            if (!shouldClick && country.isNotBlank()) {
                val countryClean = country.replace(Regex("[^a-z\\s]"), "").trim()
                if (effectiveText == countryClean || effectiveText.contains(countryClean)) shouldClick = true
            }

            // ✅ State match
            if (!shouldClick && state.isNotBlank()) {
                if (effectiveText == state || effectiveText.contains(state)) shouldClick = true
            }

            // ✅ City match
            if (!shouldClick && city.isNotBlank()) {
                if (effectiveText == city || effectiveText.contains(city)) shouldClick = true
            }

            // ✅ DOB match (for dropdowns showing day/month/year values)
            if (!shouldClick && dob.isNotBlank()) {
                val parts = dob.split("/", "-", ".")
                if (parts.size == 3) {
                    val day = parts[0].trim()
                    val month = parts[1].trim()
                    val year = parts[2].trim()
                    if (effectiveText == day || effectiveText == month || effectiveText == year) shouldClick = true
                }
            }

            // ✅ Any custom field value match — exact only
            if (!shouldClick) {
                for ((_, value) in profile.customFields) {
                    val v = value.lowercase().trim()
                    if (v.isNotBlank() && v.length > 1 && effectiveText == v) {
                        shouldClick = true; break
                    }
                }
            }

            // ✅ Any section field value match — exact only
            if (!shouldClick) {
                outer@ for (section in profile.sections) {
                    for (field in section.fields) {
                        val v = field.value.lowercase().trim()
                        if (v.isNotBlank() && v.length > 1 && effectiveText == v) {
                            shouldClick = true; break@outer
                        }
                    }
                }
            }

            if (shouldClick) {
                // ✅ Skip if already checked (radio/checkbox already selected hai)
                if (isRadioOrCheck && node.isChecked) {
                    android.util.Log.d("SmartClick", "Skip already checked: '$effectiveText'")
                    return@forEach
                }

                android.util.Log.d("SmartClick", "Trying to click: '$effectiveText' class=$cls")

                // Method 1: Direct click on node
                var clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                android.util.Log.d("SmartClick", "Method1 direct click: $clicked")

                // Method 2: If checkable, try ACTION_SELECT
                if (!clicked && node.isCheckable) {
                    clicked = node.performAction(AccessibilityNodeInfo.ACTION_SELECT)
                    android.util.Log.d("SmartClick", "Method2 ACTION_SELECT: $clicked")
                }

                // Method 3: Traverse up to find clickable parent
                if (!clicked) {
                    var parent = node.parent
                    var depth = 0
                    while (parent != null && depth < 10) {
                        if (parent.isClickable) {
                            clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            android.util.Log.d("SmartClick", "Method3 parent click depth=$depth: $clicked")
                            if (clicked) break
                        }
                        parent = parent.parent
                        depth++
                    }
                }

                // Method 4: Focus + Click combination
                if (!clicked) {
                    node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                    delay(150)
                    clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    android.util.Log.d("SmartClick", "Method4 focus+click: $clicked")
                }

                // Method 5: Gesture tap at node coordinates (API 24+, best for WebView)
                if (!clicked) {
                    try {
                        val bounds = Rect()
                        node.getBoundsInScreen(bounds)
                        val x = bounds.exactCenterX()
                        val y = bounds.exactCenterY()
                        if (x > 0 && y > 0) {
                            val path = android.graphics.Path()
                            path.moveTo(x, y)
                            val gestureBuilder = android.accessibilityservice.GestureDescription.Builder()
                            gestureBuilder.addStroke(
                                android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 50)
                            )
                            val dispatched = dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
                                override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                                    android.util.Log.d("SmartClick", "Method5 gesture tap completed at ($x, $y)")
                                }
                                override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                                    android.util.Log.d("SmartClick", "Method5 gesture tap cancelled")
                                }
                            }, null)
                            if (dispatched) {
                                clicked = true
                                delay(200) // Gesture complete hone do
                            }
                            android.util.Log.d("SmartClick", "Method5 gesture dispatch: $dispatched at ($x, $y)")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SmartClick", "Method5 gesture failed: ${e.message}")
                    }
                }

                filledCount++
                android.util.Log.d("SmartClick", "Final click result for '$effectiveText': $clicked")
            }
        }

        // ── 3. Dropdown auto-select (Google Forms "Choose" dropdowns) ──
        // Flow: dropdown detect → click to open → wait → scan ALL windows → option click
        android.util.Log.d("SmartDropdown", "Starting dropdown detection...")

        // Fresh nodes lo kyunki radio click ke baad state change hua hoga
        val dropdownRoot = rootInActiveWindow
        val dropdownNodes = mutableListOf<AccessibilityNodeInfo>()
        if (dropdownRoot != null) findAllNodes(dropdownRoot, dropdownNodes)

        val dropdownTriggers = dropdownNodes.filter { node ->
            val txt = node.text?.toString()?.trim()?.lowercase() ?: ""
            val cls = node.className?.toString() ?: ""
            if (node.isEditable) return@filter false
            // "Choose", "Select", "चुनें", "-- Select --" — ye dropdown triggers hain
            (txt == "choose" || txt == "select" || txt == "select one" || txt == "चुनें" ||
                    txt == "-- select --" || txt == "-select-" ||
                    txt.startsWith("choose") || txt.startsWith("select") ||
                    txt.startsWith("चुनें")) &&
                    (node.isClickable || node.parent?.isClickable == true ||
                            cls.contains("Spinner") || cls.contains("Select"))
        }

        android.util.Log.d("SmartDropdown", "Dropdown triggers found: ${dropdownTriggers.size}")

        // Label nodes for dropdown context
        val dropdownLabelNodes = dropdownNodes.filter { 
            !it.isEditable && it.text != null && it.text.isNotBlank() && it.text.length < 60 
        }

        for (dropdownNode in dropdownTriggers) {
            // 1. Find what field this dropdown belongs to using nearby label
            val dropBounds = Rect(); dropdownNode.getBoundsInScreen(dropBounds)
            val nearbyLabel = findBestLabel(dropBounds, dropdownLabelNodes).lowercase().trim()
            if (nearbyLabel.isBlank()) {
                android.util.Log.d("SmartDropdown", "No label found for dropdown at ${dropBounds.top}")
                continue
            }

            android.util.Log.d("SmartDropdown", "Dropdown label: '$nearbyLabel'")

            // 2. Find what value to select from profile
            val targetValue = matchStandardField(nearbyLabel, profile)
                ?: matchCustomField(nearbyLabel, profile)
                ?: run {
                    if (pageLanguage != TranslateLanguage.ENGLISH) {
                        val translated = translateToEnglish(nearbyLabel, pageLanguage)
                        matchStandardField(translated.lowercase(), profile) 
                            ?: matchCustomField(translated.lowercase(), profile)
                    } else null
                }

            if (targetValue.isNullOrBlank()) {
                android.util.Log.d("SmartDropdown", "No matching value for label: '$nearbyLabel'")
                continue
            }

            android.util.Log.d("SmartDropdown", "Target value: '$targetValue' for label: '$nearbyLabel'")

            // 3. Click dropdown to open it — gesture tap most reliable for WebView
            val x = dropBounds.exactCenterX()
            val y = dropBounds.exactCenterY()
            var opened = false

            // Try gesture tap first (most reliable for WebView dropdowns)
            if (x > 0 && y > 0) {
                try {
                    val path = android.graphics.Path()
                    path.moveTo(x, y)
                    val gestureBuilder = android.accessibilityservice.GestureDescription.Builder()
                    gestureBuilder.addStroke(
                        android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 100)
                    )
                    opened = dispatchGesture(gestureBuilder.build(), null, null)
                } catch (_: Exception) {}
            }

            // Fallback to ACTION_CLICK
            if (!opened) {
                opened = dropdownNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            if (!opened) {
                var parent = dropdownNode.parent
                var depth = 0
                while (parent != null && depth < 5) {
                    if (parent.isClickable) {
                        opened = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        if (opened) break
                    }
                    parent = parent.parent
                    depth++
                }
            }

            android.util.Log.d("SmartDropdown", "Dropdown opened: $opened")
            if (!opened) continue

            // 4. Wait for dropdown options to appear — try with increasing delays
            val targetLower = targetValue.lowercase().trim()
            var optionClicked = false

            // Retry up to 3 times with increasing delay
            for (attempt in 1..3) {
                delay(if (attempt == 1) 600 else if (attempt == 2) 800 else 1200)

                android.util.Log.d("SmartDropdown", "Scanning for options (attempt $attempt)...")

                // 5. Scan ALL windows — dropdown popup may be in a different window
                val allOptionNodes = mutableListOf<AccessibilityNodeInfo>()
                try {
                    val windowList = windows
                    if (windowList != null && windowList.isNotEmpty()) {
                        for (window in windowList) {
                            val wRoot = window.root ?: continue
                            findAllNodes(wRoot, allOptionNodes)
                        }
                        android.util.Log.d("SmartDropdown", "Windows: ${windowList.size}, Total nodes: ${allOptionNodes.size}")
                    }
                } catch (_: Exception) {}

                // Fallback: rootInActiveWindow
                if (allOptionNodes.isEmpty()) {
                    val optRoot = rootInActiveWindow
                    if (optRoot != null) findAllNodes(optRoot, allOptionNodes)
                }

                // Try exact match first, then contains match
                for (pass in 1..2) {
                    for (optNode in allOptionNodes) {
                        val optText = optNode.text?.toString()?.trim()?.lowercase() ?: continue
                        if (optText.isBlank() || optText.length > 60) continue
                        if (optNode.isEditable) continue
                        // Skip the "Choose"/"Select" trigger text
                        if (optText == "choose" || optText == "select" || optText == "select one" || optText == "चुनें") continue

                        val matches = when (pass) {
                            1 -> optText == targetLower  // Exact match
                            2 -> optText.contains(targetLower) || targetLower.contains(optText)  // Partial
                            else -> false
                        }

                        if (matches) {
                            android.util.Log.d("SmartDropdown", "Found option: '$optText' (pass=$pass, attempt=$attempt)")

                            // Gesture tap on the option — most reliable for WebView
                            val optBounds = Rect()
                            optNode.getBoundsInScreen(optBounds)
                            val ox = optBounds.exactCenterX()
                            val oy = optBounds.exactCenterY()

                            var optClicked = false

                            // Method A: Gesture tap (best for WebView dropdowns)
                            if (ox > 0 && oy > 0) {
                                try {
                                    val tapPath = android.graphics.Path()
                                    tapPath.moveTo(ox, oy)
                                    val gb = android.accessibilityservice.GestureDescription.Builder()
                                    gb.addStroke(
                                        android.accessibilityservice.GestureDescription.StrokeDescription(tapPath, 0, 100)
                                    )
                                    optClicked = dispatchGesture(gb.build(), null, null)
                                    if (optClicked) delay(200)
                                } catch (_: Exception) {}
                            }

                            // Method B: Direct click
                            if (!optClicked) {
                                optClicked = optNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            }

                            // Method C: Parent click
                            if (!optClicked) {
                                var optParent = optNode.parent
                                var d = 0
                                while (optParent != null && d < 5) {
                                    if (optParent.isClickable) {
                                        optClicked = optParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                        if (optClicked) break
                                    }
                                    optParent = optParent.parent
                                    d++
                                }
                            }

                            if (optClicked) {
                                optionClicked = true
                                filledCount++
                                android.util.Log.d("SmartDropdown", "✓ Option '$optText' selected!")
                                break
                            }
                        }
                    }
                    if (optionClicked) break
                }

                // Cleanup option nodes
                allOptionNodes.forEach { try { it.recycle() } catch (_: Exception) {} }

                if (optionClicked) break
            }

            if (!optionClicked) {
                android.util.Log.d("SmartDropdown", "No option matched for '$targetValue' — closing dropdown")
                performGlobalAction(GLOBAL_ACTION_BACK)
                delay(300)
            } else {
                delay(400) // Dropdown close hone do
            }
        }

        // Cleanup dropdown nodes
        dropdownNodes.forEach { try { it.recycle() } catch (_: Exception) {} }

        // Cleanup — avoid double recycle
        val recycledNodes = mutableSetOf<Int>()
        if (isWebView) {
            freshNodes.forEach { node ->
                val hash = System.identityHashCode(node)
                if (hash !in recycledNodes) {
                    recycledNodes.add(hash)
                    try { node.recycle() } catch (_: Exception) {}
                }
            }
        }
        allNodes.forEach { node ->
            val hash = System.identityHashCode(node)
            if (hash !in recycledNodes) {
                recycledNodes.add(hash)
                try { node.recycle() } catch (_: Exception) {}
            }
        }

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
            // Skip irrelevant text
            if (lt.contains("@") || lt.contains("http") || lt.contains("indicates") ||
                lt.contains("required question") || lt.contains("never submit") ||
                lt.contains("google forms") || lt.length > 80) return@forEach

            val hd = abs(lb.centerX() - editBounds.centerX())
            val vd = editBounds.top - lb.bottom  // positive = label is above field

            var score = when {
                // ✅ Label directly above field (tight 0-80px) — highest priority
                vd in 0..80 && hd < 200 -> 150
                // Label inside field (floating label) — very high
                lb.top >= editBounds.top && lb.bottom <= editBounds.bottom && hd < 200 -> 140
                // Label above field (80-150px) — high
                vd in 80..150 && hd < 150 -> 100
                // Label above field (150-200px) — medium
                vd in 150..200 && hd < 100 -> 60
                // Same height as field — low
                abs(lb.centerY() - editBounds.centerY()) < 30 && hd < 150 -> 40
                else -> return@forEach  // Too far — ignore
            }
            // Prefer labels that are horizontally close
            if (hd < 50) score += 30
            else if (hd < 100) score += 15

            candidates.add(LC(lt, score))
        }
        return candidates.maxByOrNull { it.score }?.text ?: ""
    }

    private fun matchStandardField(context: String, profile: UserProfile): String? {
        // ✅ Specific keywords wale fields custom field handle karega
        val specificKeywords = listOf(
            "aadhaar", "aadhar", "pan", "passport", "voter", "driving",
            "account", "ifsc", "branch", "roll", "enrol", "registration",
            "application", "pin code", "pincode", "zip", "marksheet", "marks",
            "subject", "percentage", "percent", "grade", "score"
        )
        if (specificKeywords.any { context.contains(it) }) return null

        return when {
            // ── Email ──
            containsWord(context, "email") || containsWord(context, "mail") || context.contains("e-mail") ||
                    context.contains("ईमेल") || context.contains("ई-मेल") || context.contains("मेल") ->
                profile.email.takeIf { it.isNotBlank() }

            // ── Phone ──
            containsWord(context, "phone") || containsWord(context, "mobile") || containsWord(context, "contact") ||
                    context.contains("phone number") || context.contains("mobile number") ||
                    context.contains("फ़ोन") || context.contains("फोन") || context.contains("मोबाइल") ||
                    context.contains("दूरभाष") || context.contains("संपर्क") ->
                profile.phoneNumber.takeIf { it.isNotBlank() }

            // ── Full Name ──
            context.contains("full name") || context.contains("fullname") ||
                    context.contains("पूरा नाम") || context.contains("पूर्ण नाम") ->
                profile.fullName.takeIf { it.isNotBlank() }

            // ── First Name ──
            context.contains("first name") || containsWord(context, "firstname") ||
                    context.contains("पहला नाम") || context.contains("प्रथम नाम") ->
                profile.fullName.split(" ").firstOrNull()?.takeIf { it.isNotBlank() }

            // ── Last Name ──
            context.contains("last name") || containsWord(context, "lastname") || containsWord(context, "surname") ||
                    context.contains("उपनाम") || context.contains("अंतिम नाम") ->
                profile.fullName.split(" ").lastOrNull()?.takeIf { it.isNotBlank() }

            // ── Name (Hindi + English) ──
            containsWord(context, "name") || context.contains("नाम") || context.contains("naam") ->
                profile.fullName.takeIf { it.isNotBlank() }

            // ── Address ──
            containsWord(context, "address") || containsWord(context, "location") ||
                    context.contains("पता") || context.contains("पत्ता") || context.contains("ठिकाना") ->
                profile.address.takeIf { it.isNotBlank() }

            else -> null
        }
    }

    private fun matchCustomField(context: String, profile: UserProfile): String? {
        // ✅ Standard fields ko custom se override mat karo
        val standardOnlyContexts = listOf("full name", "first name", "last name", "your name", "enter name")
        if (standardOnlyContexts.any { context.contains(it) }) return null
        // Sirf "name" word ho aur koi specific ID keyword na ho → standard handle karega
        if (containsWord(context, "name") &&
            !context.contains("aadhaar") && !context.contains("pan") &&
            !context.contains("roll") && !context.contains("account") &&
            !context.contains("bank") && !context.contains("school") &&
            !context.contains("college") && !context.contains("father") &&
            !context.contains("mother") && !context.contains("guardian")) return null

        // ✅ PRIORITY 1: Section fields — longer label = more specific = higher priority
        val allSectionFields = profile.sections.flatMap { it.fields }
            .filter { it.label.isNotBlank() && it.value.isNotBlank() }
            .sortedByDescending { it.label.length }

        for (field in allSectionFields) {
            val ck = field.label.lowercase().trim()
            val words = ck.split(" ", "_", "-").filter { it.length >= 3 }

            // Exact full match — highest priority
            if (containsWord(context, ck) || context.contains(ck)) return field.value

            // All words must match
            if (words.size > 1 && words.all { context.contains(it) }) return field.value

            // First significant word match (only if 6+ chars)
            val firstWord = words.firstOrNull() ?: continue
            if (firstWord.length >= 6 && containsWord(context, firstWord)) return field.value
        }

        // ✅ PRIORITY 2: Custom fields — longer key = more specific
        val sorted = profile.customFields.entries
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
            .sortedByDescending { it.key.length }

        for ((key, value) in sorted) {
            val ck = key.lowercase().trim()
            val words = ck.split(" ", "_", "-").filter { it.length >= 3 }

            if (containsWord(context, ck) || context.contains(ck)) return value
            if (words.size > 1 && words.all { context.contains(it) }) return value
            if (words.any { it.length >= 6 && containsWord(context, it) }) return value
        }

        return null
    }

    private fun containsWord(context: String, word: String): Boolean {
        if (context == word) return true
        if (context.contains(word)) {
            // For non-latin (Hindi, etc.) — direct contains is enough
            val idx = context.indexOf(word)
            if (idx >= 0) {
                val before = if (idx > 0) context[idx - 1] else ' '
                val after = if (idx + word.length < context.length) context[idx + word.length] else ' '
                // Word boundary: space, start/end, or non-alphanumeric
                val beforeOk = !before.isLetterOrDigit()
                val afterOk = !after.isLetterOrDigit()
                if (beforeOk && afterOk) return true
            }
        }
        return false
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