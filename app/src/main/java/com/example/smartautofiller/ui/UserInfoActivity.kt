package com.example.smartautofiller.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartautofiller.MainActivity
import com.example.smartautofiller.data.UserProfile
import com.example.smartautofiller.viewmodel.ProfileViewModel
import com.example.smartautofiller.ui.theme.SmartautofillerTheme

val PurpleMain = Color(0xFF7F77DD)
val BlueMain   = Color(0xFF378ADD)
val GradientOnboard = Brush.linearGradient(listOf(PurpleMain, BlueMain))

// India ke states
val INDIA_STATES = listOf(
    "Andhra Pradesh","Arunachal Pradesh","Assam","Bihar","Chhattisgarh",
    "Goa","Gujarat","Haryana","Himachal Pradesh","Jharkhand","Karnataka",
    "Kerala","Madhya Pradesh","Maharashtra","Manipur","Meghalaya","Mizoram",
    "Nagaland","Odisha","Punjab","Rajasthan","Sikkim","Tamil Nadu","Telangana",
    "Tripura","Uttar Pradesh","Uttarakhand","West Bengal",
    "Delhi","Jammu & Kashmir","Ladakh","Puducherry","Chandigarh"
)

val COUNTRIES = listOf(
    "🇮🇳 India","🇺🇸 USA","🇬🇧 UK","🇦🇺 Australia","🇨🇦 Canada",
    "🇸🇬 Singapore","🇦🇪 UAE","🇩🇪 Germany","🇫🇷 France","🇯🇵 Japan",
    "🇧🇩 Bangladesh","🇵🇰 Pakistan","🇳🇵 Nepal","🇱🇰 Sri Lanka","Other"
)

val DAYS   = (1..31).map { it.toString().padStart(2, '0') }
val MONTHS = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
val YEARS  = (1940..2015).map { it.toString() }.reversed()

class UserInfoActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartautofillerTheme {
                val vm: ProfileViewModel = viewModel()
                UserInfoScreen(
                    onFinish = { profile ->
                        vm.addProfile(profile)
                        getSharedPreferences("autofill_prefs", MODE_PRIVATE)
                            .edit().putBoolean("user_info_done", true).apply()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onSkip = {
                        getSharedPreferences("autofill_prefs", MODE_PRIVATE)
                            .edit().putBoolean("user_info_done", true).apply()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun UserInfoScreen(onFinish: (UserProfile) -> Unit, onSkip: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }

    // Data states
    var country  by remember { mutableStateOf("🇮🇳 India") }
    var state    by remember { mutableStateOf("") }
    var name     by remember { mutableStateOf("") }
    var gender   by remember { mutableStateOf("") }
    var day      by remember { mutableStateOf("01") }
    var month    by remember { mutableStateOf("Jan") }
    var year     by remember { mutableStateOf("2000") }

    val bgColor = Color(0xFF12121F)

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        AnimatedContent(targetState = step, transitionSpec = {
            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
        }, label = "step") { currentStep ->
            when (currentStep) {
                0 -> CountryStateStep(country = country, state = state, onCountryChange = { country = it }, onStateChange = { state = it }, onNext = { step++ }, onSkip = onSkip)
                1 -> NameStep(name = name, onNameChange = { name = it }, onNext = { step++ }, onBack = { step-- }, onSkip = onSkip)
                2 -> GenderStep(gender = gender, onGenderSelect = { gender = it; step++ }, onBack = { step-- }, onSkip = onSkip)
                3 -> DobStep(day = day, month = month, year = year, onDayChange = { day = it }, onMonthChange = { month = it }, onYearChange = { year = it },
                    onFinish = {
                        val monthNum = (MONTHS.indexOf(month) + 1).toString().padStart(2, '0')
                        val dob = "$day/$monthNum/$year"
                        val countryClean = country.replace(Regex("[^a-zA-Z\\s]"), "").trim()
                        val customFields = mutableMapOf<String, String>()
                        if (gender.isNotBlank())       customFields["Gender"]        = gender
                        if (dob.isNotBlank())          customFields["Date of Birth"] = dob
                        if (countryClean.isNotBlank()) customFields["Country"]       = countryClean
                        if (state.isNotBlank())        customFields["State"]         = state
                        val profile = UserProfile(
                            profileName  = "My Info",   // ✅ Special name
                            fullName     = name.trim(),
                            customFields = customFields
                        )
                        onFinish(profile)
                    },
                    onBack = { step-- }, onSkip = onSkip)
            }
        }

        // Progress dots
        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(modifier = Modifier.size(if (step == i) 20.dp else 8.dp, 8.dp).clip(RoundedCornerShape(50))
                    .background(if (step == i) PurpleMain else Color.White.copy(alpha = 0.3f)))
            }
        }
    }
}

// ── Step 1: Country & State ───────────────────────────────────
@Composable
fun CountryStateStep(country: String, state: String, onCountryChange: (String) -> Unit, onStateChange: (String) -> Unit, onNext: () -> Unit, onSkip: () -> Unit) {
    var showCountryPicker by remember { mutableStateOf(false) }
    var showStatePicker   by remember { mutableStateOf(false) }
    val isIndia = country.contains("India")

    StepWrapper(step = 0, title = "Where are you from?", subtitle = "Select your location", emoji = "🌍", onSkip = onSkip) {
        // Country picker
        SelectCard(label = "COUNTRY", value = country, onClick = { showCountryPicker = true })
        Spacer(modifier = Modifier.height(10.dp))
        // State picker (sirf India ke liye)
        if (isIndia) {
            SelectCard(label = "STATE", value = state.ifBlank { "Select state..." }, onClick = { showStatePicker = true })
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        NextButton("Next →") { if (country.isNotBlank()) onNext() }
    }

    if (showCountryPicker) PickerDialog("Select Country", COUNTRIES, onSelect = { onCountryChange(it); showCountryPicker = false }, onDismiss = { showCountryPicker = false })
    if (showStatePicker) PickerDialog("Select State", INDIA_STATES, onSelect = { onStateChange(it); showStatePicker = false }, onDismiss = { showStatePicker = false })
}

// ── Step 2: Name ──────────────────────────────────────────────
@Composable
fun NameStep(name: String, onNameChange: (String) -> Unit, onNext: () -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    StepWrapper(step = 1, title = "What's your name?", subtitle = "Used to autofill forms automatically", emoji = "👋", onSkip = onSkip, onBack = onBack) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.07f)).padding(horizontal = 18.dp, vertical = 16.dp)) {
            BasicTextField(
                value = name, onValueChange = onNameChange,
                textStyle = TextStyle(fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(PurpleMain),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner -> Box { if (name.isEmpty()) Text("e.g. Pushpraj Singhal", fontSize = 18.sp, color = Color.White.copy(alpha = 0.25f)); inner() } }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        NextButton("Next →") { if (name.isNotBlank()) onNext() }
    }
}

// ── Step 3: Gender ────────────────────────────────────────────
@Composable
fun GenderStep(gender: String, onGenderSelect: (String) -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    StepWrapper(step = 2, title = "Select your gender", subtitle = "Auto-selects gender options in forms", emoji = "", onSkip = onSkip, onBack = onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GenderCard(label = "Male", isSelected = gender == "Male", svgContent = {
                // Male cartoon SVG
                androidx.compose.ui.viewinterop.AndroidView(factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        loadData(maleSvg, "text/html", "UTF-8")
                    }
                }, modifier = Modifier.size(80.dp))
            }, onClick = { onGenderSelect("Male") })

            GenderCard(label = "Female", isSelected = gender == "Female", svgContent = {
                androidx.compose.ui.viewinterop.AndroidView(factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        loadData(femaleSvg, "text/html", "UTF-8")
                    }
                }, modifier = Modifier.size(80.dp))
            }, onClick = { onGenderSelect("Female") })
        }
        Spacer(modifier = Modifier.height(10.dp))
        // Other option
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (gender == "Other") PurpleMain.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f))
            .clickable { onGenderSelect("Other") }.padding(14.dp),
            contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🏳️‍🌈", fontSize = 24.sp)
                Text("Other / Prefer not to say", fontSize = 14.sp, color = if (gender == "Other") PurpleMain else Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun RowScope.GenderCard(label: String, isSelected: Boolean, svgContent: @Composable () -> Unit, onClick: () -> Unit) {
    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
        .background(if (isSelected) PurpleMain.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f))
        .clickable { onClick() }.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        svgContent()
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isSelected) PurpleMain else Color.White.copy(alpha = 0.7f))
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PurpleMain))
        }
    }
}

// ── Step 4: Date of Birth ─────────────────────────────────────
@Composable
fun DobStep(day: String, month: String, year: String, onDayChange: (String) -> Unit, onMonthChange: (String) -> Unit, onYearChange: (String) -> Unit, onFinish: () -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    var showDay   by remember { mutableStateOf(false) }
    var showMonth by remember { mutableStateOf(false) }
    var showYear  by remember { mutableStateOf(false) }

    StepWrapper(step = 3, title = "Date of Birth", subtitle = "Auto-fills DOB in any form", emoji = "🎂", onSkip = onSkip, onBack = onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Day
            DobSelector(label = "DAY", value = day, onClick = { showDay = true }, modifier = Modifier.weight(1f))
            // Month
            DobSelector(label = "MONTH", value = month, onClick = { showMonth = true }, modifier = Modifier.weight(1.2f))
            // Year
            DobSelector(label = "YEAR", value = year, onClick = { showYear = true }, modifier = Modifier.weight(1.3f))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GradientOnboard).clickable { onFinish() }.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            Text("Get Started ✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

    if (showDay)   PickerDialog("Select Day",   DAYS,   onSelect = { onDayChange(it); showDay = false },   onDismiss = { showDay = false })
    if (showMonth) PickerDialog("Select Month", MONTHS, onSelect = { onMonthChange(it); showMonth = false }, onDismiss = { showMonth = false })
    if (showYear)  PickerDialog("Select Year",  YEARS,  onSelect = { onYearChange(it); showYear = false },  onDismiss = { showYear = false })
}

@Composable
fun DobSelector(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.08f)).clickable { onClick() }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f), letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Icon(Icons.Rounded.ExpandMore, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
    }
}

// ── Reusable Components ───────────────────────────────────────
@Composable
fun StepWrapper(step: Int, title: String, subtitle: String, emoji: String, onSkip: () -> Unit, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 60.dp, bottom = 80.dp)) {
        // Top bar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f))) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            } else Spacer(modifier = Modifier.size(40.dp))
            // Progress bar
            Box(modifier = Modifier.weight(1f).padding(horizontal = 12.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.15f))) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((step + 1) / 4f).clip(RoundedCornerShape(2.dp)).background(GradientOnboard))
            }
            TextButton(onClick = onSkip) { Text("Skip", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f)) }
        }
        Spacer(modifier = Modifier.height(36.dp))
        if (emoji.isNotBlank()) Text(emoji, fontSize = 44.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 32.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(28.dp))
        content()
    }
}

@Composable
fun SelectCard(label: String, value: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.07f)).clickable { onClick() }.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f), letterSpacing = 0.8.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Rounded.ExpandMore, null, tint = Color.White.copy(alpha = 0.4f))
    }
}

@Composable
fun NextButton(text: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GradientOnboard).clickable { onClick() }.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun PickerDialog(title: String, items: List<String>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF1E1E30))) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.6f)) }
                }
                Divider(color = Color.White.copy(alpha = 0.07f))
                Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    items.forEach { item ->
                        Box(modifier = Modifier.fillMaxWidth().clickable { onSelect(item) }.padding(horizontal = 20.dp, vertical = 14.dp)) {
                            Text(item, fontSize = 15.sp, color = Color.White)
                        }
                        Divider(color = Color.White.copy(alpha = 0.04f))
                    }
                }
            }
        }
    }
}

// Male SVG cartoon
val maleSvg = """
<html><body style="margin:0;background:transparent;display:flex;align-items:center;justify-content:center;height:80px">
<svg width="70" height="80" viewBox="0 0 70 80" xmlns="http://www.w3.org/2000/svg">
  <circle cx="35" cy="22" r="18" fill="#FFB347"/>
  <circle cx="35" cy="22" r="14" fill="#FFCC80"/>
  <ellipse cx="28" cy="21" rx="2.5" ry="3" fill="#5D4037"/>
  <ellipse cx="42" cy="21" rx="2.5" ry="3" fill="#5D4037"/>
  <path d="M27 30 Q35 36 43 30" stroke="#E57373" stroke-width="2" fill="none" stroke-linecap="round"/>
  <path d="M17 8 Q35 0 53 8 Q53 20 47 22 Q35 10 23 22 Q17 20 17 8Z" fill="#5B4FCF"/>
  <rect x="20" y="40" width="30" height="28" rx="8" fill="#5B8DEF"/>
  <rect x="10" y="42" width="12" height="20" rx="5" fill="#5B8DEF"/>
  <rect x="48" y="42" width="12" height="20" rx="5" fill="#5B8DEF"/>
  <rect x="22" y="68" width="11" height="12" rx="4" fill="#37474F"/>
  <rect x="37" y="68" width="11" height="12" rx="4" fill="#37474F"/>
</svg></body></html>
""".trimIndent()

// Female SVG cartoon
val femaleSvg = """
<html><body style="margin:0;background:transparent;display:flex;align-items:center;justify-content:center;height:80px">
<svg width="70" height="80" viewBox="0 0 70 80" xmlns="http://www.w3.org/2000/svg">
  <circle cx="35" cy="22" r="18" fill="#FFB347"/>
  <circle cx="35" cy="22" r="14" fill="#FFCC80"/>
  <ellipse cx="28" cy="21" rx="2.5" ry="3" fill="#5D4037"/>
  <ellipse cx="42" cy="21" rx="2.5" ry="3" fill="#5D4037"/>
  <path d="M27 30 Q35 37 43 30" stroke="#E57373" stroke-width="2" fill="none" stroke-linecap="round"/>
  <path d="M17 14 Q20 4 35 2 Q50 4 53 14 Q53 8 47 8 Q35 16 23 8 Q17 8 17 14Z" fill="#D4537E"/>
  <path d="M15 14 Q12 8 17 6 Q17 14 23 14Z" fill="#D4537E"/>
  <path d="M55 14 Q58 8 53 6 Q53 14 47 14Z" fill="#D4537E"/>
  <path d="M18 42 Q25 70 35 70 Q45 70 52 42 Z" fill="#F06292"/>
  <path d="M18 42 L13 44 Q10 55 14 62 L20 58Z" fill="#F06292"/>
  <path d="M52 42 L57 44 Q60 55 56 62 L50 58Z" fill="#F06292"/>
  <rect x="22" y="68" width="11" height="12" rx="4" fill="#880E4F"/>
  <rect x="37" y="68" width="11" height="12" rx="4" fill="#880E4F"/>
</svg></body></html>
""".trimIndent()