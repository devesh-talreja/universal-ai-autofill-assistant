package com.example.smartautofiller

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartautofiller.data.ProfileSection
import com.example.smartautofiller.data.SectionField
import com.example.smartautofiller.data.UserProfile
import com.example.smartautofiller.security.PinManager
import com.example.smartautofiller.security.VerifyResult
import com.example.smartautofiller.service.SmartAccessibilityService
import com.example.smartautofiller.ui.CameraActivity
import com.example.smartautofiller.ui.FeaturesActivity
import com.example.smartautofiller.ui.PrivacyPolicyActivity
import com.example.smartautofiller.ui.theme.SmartautofillerTheme
import com.example.smartautofiller.viewmodel.ProfileViewModel
import kotlinx.coroutines.delay

val PurpleStart = Color(0xFF7F77DD)
val PurpleEnd   = Color(0xFF534AB7)
val BlueEnd     = Color(0xFF378ADD)
val GreenAccent = Color(0xFF1D9E75)
val RedAccent   = Color(0xFFE24B4A)
val GradientMain  = Brush.linearGradient(listOf(PurpleStart, BlueEnd))
val GradientGreen = Brush.linearGradient(listOf(GreenAccent, Color(0xFF0F6E56)))

fun maskEmail(email: String): String {
    if (email.isBlank() || !email.contains("@")) return email
    val parts = email.split("@")
    val name = parts[0]
    val masked = if (name.length <= 2) name else name.take(2) + "***"
    return "$masked@${parts[1]}"
}
fun maskPhone(phone: String): String {
    if (phone.length < 6) return phone
    return phone.take(3) + "****" + phone.takeLast(3)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { getSharedPreferences("autofill_prefs", MODE_PRIVATE) }
            val systemDark = isSystemInDarkTheme()
            var isDark by remember { mutableStateOf(when (prefs.getInt("theme_mode", 0)) { 1 -> false; 2 -> true; else -> systemDark }) }
            SmartautofillerTheme(darkTheme = isDark) {
                val vm: ProfileViewModel = viewModel()
                LaunchedEffect(Unit) { if (vm.isDeviceRooted()) Toast.makeText(this@MainActivity, "⚠️ Rooted device! Your data may be at risk.", Toast.LENGTH_LONG).show() }
                SecurityWrapper {
                    MainScreen(isDark = isDark, onThemeToggle = { isDark = !isDark; prefs.edit().putInt("theme_mode", if (isDark) 2 else 1).apply() })
                }
            }
        }
    }
}

// ── Security Wrapper ──────────────────────────────────────────
@Composable
fun SecurityWrapper(content: @Composable () -> Unit) {
    val context = LocalContext.current as FragmentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val pinManager = remember { PinManager(context) }
    var isAuthorized by remember { mutableStateOf(false) }
    var showPinScreen by remember { mutableStateOf(false) }
    var isSettingUpPin by remember { mutableStateOf(!pinManager.isPinSet()) }
    var backgroundTime by remember { mutableStateOf(0L) }
    val executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt = remember {
        BiometricPrompt(context, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { super.onAuthenticationSucceeded(result); isAuthorized = true; showPinScreen = false }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { super.onAuthenticationError(errorCode, errString); showPinScreen = true }
            override fun onAuthenticationFailed() { super.onAuthenticationFailed(); showPinScreen = true }
        })
    }
    val promptInfo = remember { BiometricPrompt.PromptInfo.Builder().setTitle("AI Autofill").setSubtitle("Use fingerprint or enter PIN").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).setNegativeButtonText("Use PIN").build() }
    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) { backgroundTime = System.currentTimeMillis() }
            override fun onStart(owner: LifecycleOwner) { val timeout = pinManager.getLockTimeoutMillis(); val elapsed = System.currentTimeMillis() - backgroundTime; if (timeout == 0L || elapsed >= timeout) { isAuthorized = false; showPinScreen = false } }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        if (pinManager.isPinSet() && pinManager.isBiometricEnabled()) {
            try { biometricPrompt.authenticate(promptInfo) } catch (e: Exception) { showPinScreen = true }
        } else if (pinManager.isPinSet()) {
            showPinScreen = true
        }
    }
    when {
        isSettingUpPin -> PinSetupScreen { pin -> pinManager.setPin(pin); isSettingUpPin = false; isAuthorized = true }
        isAuthorized -> content()
        showPinScreen -> PinScreen(pinManager = pinManager, onSuccess = { isAuthorized = true; showPinScreen = false }, onBiometricClick = { if (pinManager.isBiometricEnabled()) { showPinScreen = false; try { biometricPrompt.authenticate(promptInfo) } catch (e: Exception) { showPinScreen = true } } })
        else -> PinScreen(pinManager = pinManager, onSuccess = { isAuthorized = true; showPinScreen = false }, onBiometricClick = { if (pinManager.isBiometricEnabled()) { showPinScreen = false; try { biometricPrompt.authenticate(promptInfo) } catch (e: Exception) { showPinScreen = true } } })
    }
}

@Composable fun LockScreen(onUnlock: () -> Unit, onUsePin: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(GradientMain), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
            Spacer(modifier = Modifier.height(24.dp))
            Text("AI Autofill", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Verify identity to continue", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(40.dp))
            Button(onClick = onUnlock, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth(0.7f).height(50.dp)) { Icon(Icons.Rounded.Fingerprint, null, tint = PurpleStart); Spacer(modifier = Modifier.width(8.dp)); Text("Use Fingerprint", color = PurpleStart, fontWeight = FontWeight.SemiBold) }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onUsePin) { Text("Use PIN instead", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp) }
        }
    }
}

@Composable fun PinSetupScreen(onPinSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }; var confirmPin by remember { mutableStateOf("") }; var step by remember { mutableStateOf(1) }; var errorMsg by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize().background(GradientMain), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Pin, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
            Spacer(modifier = Modifier.height(20.dp))
            Text(if (step == 1) "Set Your PIN" else "Confirm PIN", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(if (step == 1) "Enter a 4-digit PIN" else "Enter PIN again to confirm", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(32.dp))
            PinDots(filledCount = if (step == 1) pin.length else confirmPin.length)
            if (errorMsg.isNotEmpty()) { Spacer(modifier = Modifier.height(8.dp)); Text(errorMsg, color = Color(0xFFFFCDD2), fontSize = 13.sp) }
            Spacer(modifier = Modifier.height(32.dp))
            PinKeypad(onNumber = { num -> errorMsg = ""; if (step == 1) { if (pin.length < 4) pin += num; if (pin.length == 4) step = 2 } else { if (confirmPin.length < 4) confirmPin += num; if (confirmPin.length == 4) { if (pin == confirmPin) onPinSet(pin) else { errorMsg = "PINs don't match!"; confirmPin = ""; pin = ""; step = 1 } } } }, onDelete = { errorMsg = ""; if (step == 1) { if (pin.isNotEmpty()) pin = pin.dropLast(1) } else { if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1) else { step = 1; if (pin.isNotEmpty()) pin = pin.dropLast(1) } } })
        }
    }
}

@Composable fun PinScreen(pinManager: PinManager, onSuccess: () -> Unit, onBiometricClick: () -> Unit) {
    var pin by remember { mutableStateOf("") }; var errorMsg by remember { mutableStateOf("") }; var lockedSeconds by remember { mutableIntStateOf(pinManager.getRemainingLockoutSeconds()) }
    val isBiometricEnabled = remember { pinManager.isBiometricEnabled() }
    LaunchedEffect(lockedSeconds) { if (lockedSeconds > 0) { delay(1000); lockedSeconds-- } }
    Box(modifier = Modifier.fillMaxSize().background(GradientMain), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(if (lockedSeconds > 0) Icons.Rounded.LockClock else Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
            Spacer(modifier = Modifier.height(20.dp))
            Text(if (lockedSeconds > 0) "App Locked" else "Enter PIN", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(if (lockedSeconds > 0) "Try again in ${lockedSeconds}s" else "Enter your 4-digit PIN", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(32.dp))
            PinDots(filledCount = if (lockedSeconds > 0) 0 else pin.length)
            if (errorMsg.isNotEmpty()) { Spacer(modifier = Modifier.height(8.dp)); Text(errorMsg, color = Color(0xFFFFCDD2), fontSize = 13.sp) }
            Spacer(modifier = Modifier.height(32.dp))
            if (lockedSeconds <= 0) {
                PinKeypad(onNumber = { num -> errorMsg = ""; if (pin.length < 4) { pin += num; if (pin.length == 4) { when (val result = pinManager.verifyPin(pin)) { is VerifyResult.Success -> onSuccess(); is VerifyResult.WrongPin -> { errorMsg = "Wrong PIN! ${result.attemptsRemaining} attempts left"; pin = "" }; is VerifyResult.LockedOut -> { lockedSeconds = result.secondsRemaining; errorMsg = "Too many attempts!"; pin = "" }; else -> { pin = "" } } } } }, onDelete = { errorMsg = ""; if (pin.isNotEmpty()) pin = pin.dropLast(1) })
                // ✅ Fingerprint sirf tab dikhe jab enabled ho
                if (isBiometricEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onBiometricClick) { Icon(Icons.Rounded.Fingerprint, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Use Fingerprint", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp) }
                }
            }
        }
    }
}

@Composable fun PinDots(filledCount: Int) { Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { repeat(4) { i -> Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(if (i < filledCount) Color.White else Color.White.copy(alpha = 0.3f))) } } }
@Composable fun PinKeypad(onNumber: (String) -> Unit, onDelete: () -> Unit) {
    val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","⌫"))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { keys.forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { row.forEach { key -> if (key.isEmpty()) Spacer(modifier = Modifier.size(72.dp)) else Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape).clickable { if (key == "⌫") onDelete() else onNumber(key) }, contentAlignment = Alignment.Center) { if (key == "⌫") Icon(Icons.Rounded.Backspace, null, tint = Color.White, modifier = Modifier.size(22.dp)) else Text(key, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Color.White) } } } } }
}

// ── Main Screen ───────────────────────────────────────────────
@Composable
fun MainScreen(viewModel: ProfileViewModel = viewModel(), isDark: Boolean = false, onThemeToggle: () -> Unit = {}) {
    val profiles by viewModel.allProfiles.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<UserProfile?>(null) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("autofill_prefs", android.content.Context.MODE_PRIVATE) }
    var isBubbleEnabled by remember { mutableStateOf(prefs.getBoolean("bubble_enabled", false)) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val pinManager = remember { PinManager(context) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { viewModel.importProfiles(context, it) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() } } }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Header
            Box(modifier = Modifier.fillMaxWidth().background(GradientMain).padding(top = 52.dp, start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("Universal", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f)); Text("AI Autofill", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        Box {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.2f)).clickable { menuExpanded = true }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Menu, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface).width(240.dp)) {
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(12.dp)); Text(if (isDark) "Light Mode" else "Dark Mode", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) } }, onClick = { onThemeToggle(); menuExpanded = false })
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Accessibility, null, tint = if (SmartAccessibilityService.instance != null) GreenAccent else RedAccent, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(12.dp)); Column { Text("Accessibility", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface); Text(if (SmartAccessibilityService.instance != null) "Active ✓" else "Tap to enable", fontSize = 11.sp, color = if (SmartAccessibilityService.instance != null) GreenAccent else RedAccent) } } }, onClick = { if (SmartAccessibilityService.instance == null) context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); menuExpanded = false })
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Layers, null, tint = if (Settings.canDrawOverlays(context)) GreenAccent else RedAccent, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(12.dp)); Column { Text("Overlay", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface); Text(if (Settings.canDrawOverlays(context)) "Granted ✓" else "Tap to grant", fontSize = 11.sp, color = if (Settings.canDrawOverlays(context)) GreenAccent else RedAccent) } } }, onClick = { if (!Settings.canDrawOverlays(context)) context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))); menuExpanded = false })
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(12.dp)); Text("Change PIN", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) } }, onClick = { showChangePinDialog = true; menuExpanded = false })
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Fingerprint, null, tint = if (pinManager.isBiometricEnabled()) GreenAccent else MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(12.dp)); Column { Text("Fingerprint Lock", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface); Text(if (pinManager.isBiometricEnabled()) "ON ✓" else "OFF", fontSize = 11.sp, color = if (pinManager.isBiometricEnabled()) GreenAccent else MaterialTheme.colorScheme.onSurfaceVariant) } } }, onClick = { pinManager.setBiometricEnabled(!pinManager.isBiometricEnabled()); menuExpanded = false; Toast.makeText(context, if (pinManager.isBiometricEnabled()) "Fingerprint ON!" else "Fingerprint OFF", Toast.LENGTH_SHORT).show() })
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(12.dp)); Column { Text("Lock Timeout", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface); val t = pinManager.getLockTimeoutMins(); Text(if (t == 0) "Instant" else "$t min", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }, onClick = { showTimeoutDialog = true; menuExpanded = false })
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Upload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(12.dp)); Text("Export Profiles", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) } }, onClick = { viewModel.exportProfiles(context) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }; menuExpanded = false })
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(12.dp)); Text("Import Profiles", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) } }, onClick = { importLauncher.launch("application/json"); menuExpanded = false })
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.PlayCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(12.dp)); Text("Features Tour", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) } }, onClick = { context.startActivity(Intent(context, FeaturesActivity::class.java)); menuExpanded = false })
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.PrivacyTip, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(12.dp)); Text("Privacy Policy", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) } }, onClick = { context.startActivity(Intent(context, PrivacyPolicyActivity::class.java)); menuExpanded = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatChip(modifier = Modifier.weight(1f), label = "Profiles", value = "${profiles.size}")
                        StatChip(modifier = Modifier.weight(1f), label = "Sections", value = "${profiles.sumOf { it.sections.size }}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                BubbleToggleCard(enabled = isBubbleEnabled, onToggle = { enabled -> if (SmartAccessibilityService.instance == null) Toast.makeText(context, "Please enable Accessibility Service first!", Toast.LENGTH_LONG).show() else { isBubbleEnabled = enabled; prefs.edit().putBoolean("bubble_enabled", enabled).apply(); SmartAccessibilityService.instance?.setBubbleVisible(enabled) } })
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("My Profiles", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(GradientMain).clickable { editingProfile = null; showDialog = true }.padding(horizontal = 16.dp, vertical = 8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Add New", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium) } }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // ✅ My Info special card
                val myInfoProfile = profiles.firstOrNull { it.profileName == "My Info" }
                if (myInfoProfile != null) {
                    MyInfoCard(profile = myInfoProfile, onClick = { editingProfile = myInfoProfile; showDialog = true })
                    Spacer(modifier = Modifier.height(12.dp))
                }
                // Normal profiles
                val otherProfiles = profiles.filter { it.profileName != "My Info" }
                if (otherProfiles.isEmpty() && myInfoProfile == null) EmptyProfilesCard { editingProfile = null; showDialog = true }
                else otherProfiles.forEach { profile -> ProfileCard(profile = profile, onDelete = { viewModel.deleteProfile(profile) }, onClick = { editingProfile = profile; showDialog = true }); Spacer(modifier = Modifier.height(10.dp)) }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showDialog) AddProfileDialog(profileToEdit = editingProfile, onDismiss = { showDialog = false }, onSave = { viewModel.addProfile(it); showDialog = false })
    if (showChangePinDialog) ChangePinDialog(onDismiss = { showChangePinDialog = false }, onPinChanged = { newPin -> PinManager(context).setPin(newPin); Toast.makeText(context, "PIN changed!", Toast.LENGTH_SHORT).show(); showChangePinDialog = false })
    if (showTimeoutDialog) {
        AlertDialog(onDismissRequest = { showTimeoutDialog = false }, shape = RoundedCornerShape(24.dp), containerColor = MaterialTheme.colorScheme.surface,
            title = { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(GradientMain), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Timer, null, tint = Color.White, modifier = Modifier.size(18.dp)) }; Spacer(modifier = Modifier.width(10.dp)); Text("Lock Timeout", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) } },
            text = { val options = listOf(0 to "Instant", 1 to "1 minute", 5 to "5 minutes", 15 to "15 minutes", 30 to "30 minutes"); var selected by remember { mutableIntStateOf(pinManager.getLockTimeoutMins()) }; Column { Text("App kitni der baad lock ho?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.height(12.dp)); options.forEach { (mins, label) -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selected = mins; pinManager.setLockTimeoutMins(mins); showTimeoutDialog = false; Toast.makeText(context, "Lock timeout: $label", Toast.LENGTH_SHORT).show() }.padding(vertical = 10.dp, horizontal = 4.dp)) { RadioButton(selected = selected == mins, onClick = { selected = mins; pinManager.setLockTimeoutMins(mins); showTimeoutDialog = false; Toast.makeText(context, "Lock timeout: $label", Toast.LENGTH_SHORT).show() }, colors = RadioButtonDefaults.colors(selectedColor = PurpleStart)); Spacer(modifier = Modifier.width(8.dp)); Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) } } } },
            confirmButton = {}, dismissButton = { TextButton(onClick = { showTimeoutDialog = false }) { Text("Cancel") } })
    }
}

// ── Reusable UI ───────────────────────────────────────────────
@Composable fun StatChip(modifier: Modifier = Modifier, label: String, value: String) {
    Box(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)).padding(vertical = 12.dp, horizontal = 14.dp)) { Column { Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White); Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f)) } }
}
@Composable fun BubbleToggleCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (enabled) GradientMain else Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFFBDBDBD)))), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) { Text("AI Bubble", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface); Text(if (enabled) "Floating bubble active" else "Tap to enable bubble", fontSize = 12.sp, color = if (enabled) GreenAccent else MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = enabled, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PurpleStart, uncheckedThumbColor = Color.White, uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
        }
    }
}

val profileGradients = listOf(Brush.linearGradient(listOf(Color(0xFF7F77DD), Color(0xFF534AB7))), Brush.linearGradient(listOf(Color(0xFF1D9E75), Color(0xFF378ADD))), Brush.linearGradient(listOf(Color(0xFFD4537E), Color(0xFF7F77DD))), Brush.linearGradient(listOf(Color(0xFFEF9F27), Color(0xFFD85A30))))

@Composable fun ProfileCard(profile: UserProfile, onDelete: () -> Unit, onClick: () -> Unit) {
    val gi = (profile.profileName.hashCode() and 0x7FFFFFFF) % profileGradients.size
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(profileGradients[gi]), contentAlignment = Alignment.Center) { Text(profile.profileName.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.profileName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(profile.fullName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(maskEmail(profile.email), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (profile.phoneNumber.isNotBlank()) Text(maskPhone(profile.phoneNumber), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Show section badges
                if (profile.sections.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        profile.sections.take(3).forEach { section ->
                            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("${section.icon} ${section.name}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                            }
                        }
                        if (profile.sections.size > 3) Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)) { Text("+${profile.sections.size - 3} more", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))) { Icon(Icons.Rounded.Delete, null, tint = RedAccent, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable fun MyInfoCard(profile: UserProfile, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() },
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GradientMain)) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(GradientMain), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("My Info", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(PurpleStart.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("Personal", fontSize = 10.sp, color = PurpleStart, fontWeight = FontWeight.Medium)
                    }
                }
                if (profile.fullName.isNotBlank()) Text(profile.fullName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    profile.customFields["Gender"]?.let { Text("👤 $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    profile.customFields["Date of Birth"]?.let { Text("🎂 $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    profile.customFields["State"]?.let { Text("📍 $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Icon(Icons.Rounded.Edit, null, tint = PurpleStart, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable fun EmptyProfilesCard(onAdd: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) }
            Spacer(modifier = Modifier.height(16.dp))
            Text("No profiles yet", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Add your first profile to start\nautofilling forms instantly", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(GradientMain).clickable { onAdd() }.padding(horizontal = 28.dp, vertical = 12.dp)) { Text("Add Profile", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
        }
    }
}

// ── Add/Edit Profile Dialog — Premium UI ──────────────────────
@Composable
fun AddProfileDialog(profileToEdit: UserProfile?, onDismiss: () -> Unit, onSave: (UserProfile) -> Unit) {
    var name     by remember { mutableStateOf(profileToEdit?.profileName ?: "") }
    var fullName by remember { mutableStateOf(profileToEdit?.fullName ?: "") }
    var email    by remember { mutableStateOf(profileToEdit?.email ?: "") }
    var phone    by remember { mutableStateOf(profileToEdit?.phoneNumber ?: "") }
    var address  by remember { mutableStateOf(profileToEdit?.address ?: "") }
    val fieldsList = remember { mutableStateListOf<Pair<String,String>>().apply { profileToEdit?.customFields?.forEach { add(it.key to it.value) } } }
    val sectionsList = remember { mutableStateListOf<ProfileSection>().apply { profileToEdit?.sections?.forEach { add(it) } } }
    var newKey   by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var showAddSection by remember { mutableStateOf(false) }
    var newSectionName by remember { mutableStateOf("") }
    var newSectionIcon by remember { mutableStateOf("📋") }
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val context  = LocalContext.current
    val fieldBg = Color.White.copy(alpha = 0.05f)
    val dividerColor = Color.White.copy(alpha = 0.07f)
    val labelColor = Color.White.copy(alpha = 0.4f)
    val hintColor = Color.White.copy(alpha = 0.25f)
    val sectionBg = Color(0xFF7F77DD).copy(alpha = 0.1f)
    val sectionBorder = Color(0xFF7F77DD).copy(alpha = 0.25f)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            data.getStringExtra("detected_name")?.takeIf { it.isNotBlank() }?.let { fullName = it }
            data.getStringExtra("detected_email")?.takeIf { it.isNotBlank() }?.let { email = it }
            data.getStringExtra("detected_phone")?.takeIf { it.isNotBlank() }?.let { phone = it }
            val docType = data.getStringExtra("doc_type") ?: ""
            val labels  = data.getStringArrayExtra("section_labels") ?: emptyArray()
            val values  = data.getStringArrayExtra("section_values") ?: emptyArray()
            if (docType.isNotBlank() && labels.isNotEmpty()) {
                val icon = when {
                    docType.contains("PAN")       -> "🪪"
                    docType.contains("Aadhaar")   -> "🆔"
                    docType.contains("Driving")   -> "🚗"
                    docType.contains("Passport")  -> "📕"
                    docType.contains("Voter")     -> "🗳️"
                    docType.contains("Marksheet") -> "🎓"
                    else -> "📋"
                }
                val sectionFields = labels.zip(values).map { (l, v) -> SectionField(label = l, value = v) }
                val newSection = ProfileSection(name = docType, icon = icon, fields = sectionFields)
                if (sectionsList.none { it.name == docType }) {
                    sectionsList.add(newSection)
                    expandedSection = newSection.id
                }
                Toast.makeText(context, "✓ '$docType' section added!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        // ✅ Theme-aware colors
        val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
        val bgColor = if (isDark) Color(0xFF1A1A2E) else Color(0xFFF8F8FF)
        val textColor = if (isDark) Color.White else Color(0xFF1A1A2E)
        val fieldBg = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
        val dividerColor = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.07f)
        val labelColor = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)
        val hintColor = if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.22f)
        val sectionBg = PurpleStart.copy(alpha = if (isDark) 0.1f else 0.06f)
        val sectionBorder = PurpleStart.copy(alpha = if (isDark) 0.25f else 0.2f)
        val subTextColor = if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.35f)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() }) {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f).align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(bgColor)
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ── Header ────────────────────────────────
                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(GradientMain), contentAlignment = Alignment.Center) {
                            Icon(if (profileToEdit == null) Icons.Rounded.PersonAdd else Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (profileToEdit == null) "New Profile" else "Edit Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Text("Fill in your details", fontSize = 11.sp, color = textColor.copy(alpha = 0.45f))
                        }
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(textColor.copy(alpha = 0.08f)).clickable { launcher.launch(Intent(context, CameraActivity::class.java)) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CameraAlt, null, tint = textColor.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                        }
                    }

                    // ── Scrollable Content ────────────────────
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {

                        // BASIC INFO label
                        Text("BASIC INFO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PurpleStart, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Grouped fields card
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(fieldBg)) {
                            Column {
                                PremiumField(value = name, onValueChange = { name = it }, label = "Profile Name *", hint = "e.g. Personal, Work...", isLast = false, labelColor = labelColor, hintColor = hintColor, dividerColor = dividerColor, textColor = textColor)
                                PremiumField(value = fullName, onValueChange = { fullName = it }, label = "Full Name", hint = "Your full name", isLast = false, labelColor = labelColor, hintColor = hintColor, dividerColor = dividerColor, textColor = textColor)
                                PremiumField(value = email, onValueChange = { email = it }, label = "Email", hint = "example@gmail.com", isLast = false, labelColor = labelColor, hintColor = hintColor, dividerColor = dividerColor, textColor = textColor)
                                PremiumField(value = phone, onValueChange = { phone = it }, label = "Phone", hint = "+91 00000 00000", isLast = false, labelColor = labelColor, hintColor = hintColor, dividerColor = dividerColor, textColor = textColor)
                                PremiumField(value = address, onValueChange = { address = it }, label = "Address", hint = "Your address", isLast = true, labelColor = labelColor, hintColor = hintColor, dividerColor = dividerColor, textColor = textColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // CUSTOM FIELDS label
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("CUSTOM FIELDS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PurpleStart, letterSpacing = 1.sp)
                            Text("Extra info add karo", fontSize = 10.sp, color = subTextColor)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Existing custom fields
                        fieldsList.forEachIndexed { index, pair ->
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(12.dp)).background(fieldBg).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pair.first, fontSize = 10.sp, color = labelColor)
                                    BasicTextField(
                                        value = pair.second,
                                        onValueChange = { fieldsList[index] = pair.first to it },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = textColor),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(PurpleStart),
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { inner -> Box { if (pair.second.isEmpty()) Text("Value", fontSize = 14.sp, color = hintColor); inner() } }
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(RedAccent.copy(alpha = 0.15f)).clickable { fieldsList.removeAt(index) }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Delete, null, tint = RedAccent, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Add new custom field
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PurpleStart.copy(alpha = 0.08f)).border(1.dp, PurpleStart.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(12.dp)) {
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(fieldBg).padding(horizontal = 12.dp, vertical = 10.dp)) {
                                        BasicTextField(value = newKey, onValueChange = { newKey = it }, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = textColor), cursorBrush = androidx.compose.ui.graphics.SolidColor(PurpleStart), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> Box { if (newKey.isEmpty()) Text("Field Name", fontSize = 13.sp, color = hintColor); inner() } })
                                    }
                                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(fieldBg).padding(horizontal = 12.dp, vertical = 10.dp)) {
                                        BasicTextField(value = newValue, onValueChange = { newValue = it }, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = textColor), cursorBrush = androidx.compose.ui.graphics.SolidColor(PurpleStart), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> Box { if (newValue.isEmpty()) Text("Value", fontSize = 13.sp, color = hintColor); inner() } })
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(GradientMain).clickable { if (newKey.isNotBlank()) { fieldsList.add(newKey.trim() to newValue.trim()); newKey = ""; newValue = "" } }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Add Field", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium) }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTIONS label
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("SECTIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PurpleStart, letterSpacing = 1.sp)
                                Text("10th, PAN, Bank alag sections", fontSize = 10.sp, color = subTextColor)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(GradientMain).clickable { showAddSection = true }.padding(horizontal = 12.dp, vertical = 7.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(13.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Add", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium) }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Section cards
                        sectionsList.forEachIndexed { sIdx, section ->
                            val isExpanded = expandedSection == section.id
                            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(14.dp)).background(sectionBg).border(1.dp, sectionBorder, RoundedCornerShape(14.dp))) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (isExpanded) null else section.id }, verticalAlignment = Alignment.CenterVertically) {
                                        Text(section.icon, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(section.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                                            Text("${section.fields.size} fields", fontSize = 10.sp, color = textColor.copy(alpha = 0.4f))
                                        }
                                        Icon(if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(RedAccent.copy(alpha = 0.12f)).clickable { sectionsList.removeAt(sIdx) }, contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Delete, null, tint = RedAccent, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Divider(color = textColor.copy(alpha = 0.08f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        section.fields.forEachIndexed { fIdx, field ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(10.dp)).background(fieldBg).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(field.label, fontSize = 9.sp, color = labelColor)
                                                    BasicTextField(
                                                        value = field.value,
                                                        onValueChange = { newVal -> val f = section.fields.toMutableList(); f[fIdx] = field.copy(value = newVal); sectionsList[sIdx] = section.copy(fields = f) },
                                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = textColor),
                                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(PurpleStart),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        decorationBox = { inner -> Box { if (field.value.isEmpty()) Text("Value", fontSize = 13.sp, color = hintColor); inner() } }
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(modifier = Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)).background(RedAccent.copy(alpha = 0.12f)).clickable { val f = section.fields.toMutableList(); f.removeAt(fIdx); sectionsList[sIdx] = section.copy(fields = f) }, contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Rounded.Close, null, tint = RedAccent, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                        var newFieldLabel by remember { mutableStateOf("") }
                                        var newFieldValue by remember { mutableStateOf("") }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(fieldBg).padding(horizontal = 10.dp, vertical = 8.dp)) {
                                                BasicTextField(value = newFieldLabel, onValueChange = { newFieldLabel = it }, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = textColor), cursorBrush = androidx.compose.ui.graphics.SolidColor(PurpleStart), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> Box { if (newFieldLabel.isEmpty()) Text("Field name", fontSize = 12.sp, color = hintColor); inner() } })
                                            }
                                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(fieldBg).padding(horizontal = 10.dp, vertical = 8.dp)) {
                                                BasicTextField(value = newFieldValue, onValueChange = { newFieldValue = it }, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = textColor), cursorBrush = androidx.compose.ui.graphics.SolidColor(PurpleStart), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> Box { if (newFieldValue.isEmpty()) Text("Value", fontSize = 12.sp, color = hintColor); inner() } })
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PurpleStart.copy(alpha = 0.2f)).clickable { if (newFieldLabel.isNotBlank()) { val f = section.fields.toMutableList(); f.add(SectionField(label = newFieldLabel.trim(), value = newFieldValue.trim())); sectionsList[sIdx] = section.copy(fields = f); newFieldLabel = ""; newFieldValue = "" } }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Add, null, tint = PurpleStart, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Add Field", fontSize = 12.sp, color = PurpleStart) }
                                        }
                                    }
                                }
                            }
                        }

                        // Add Section inline
                        if (showAddSection) {
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PurpleStart.copy(alpha = 0.1f)).border(1.dp, PurpleStart.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(14.dp)) {
                                Column {
                                    Text("New Section", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Icon:", fontSize = 10.sp, color = textColor.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("📋","🎓","💼","🏦","🏠","📊","🆔","✍️","🚗","📕").forEach { emoji ->
                                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (newSectionIcon == emoji) PurpleStart else textColor.copy(alpha = 0.08f)).clickable { newSectionIcon = emoji }, contentAlignment = Alignment.Center) { Text(emoji, fontSize = 18.sp) }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(fieldBg).padding(horizontal = 14.dp, vertical = 12.dp)) {
                                        BasicTextField(value = newSectionName, onValueChange = { newSectionName = it }, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = textColor), cursorBrush = androidx.compose.ui.graphics.SolidColor(PurpleStart), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> Box { if (newSectionName.isEmpty()) Text("Section Name (e.g. 10th Marksheet)", fontSize = 14.sp, color = hintColor); inner() } })
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(textColor.copy(alpha = 0.06f)).clickable { showAddSection = false; newSectionName = "" }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) { Text("Cancel", fontSize = 13.sp, color = textColor.copy(alpha = 0.6f)) }
                                        Box(modifier = Modifier.weight(2f).clip(RoundedCornerShape(10.dp)).background(GradientMain).clickable { if (newSectionName.isNotBlank()) { sectionsList.add(ProfileSection(name = newSectionName.trim(), icon = newSectionIcon)); expandedSection = sectionsList.last().id; showAddSection = false; newSectionName = "" } }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) { Text("Create Section", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium) }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ── Bottom Buttons ────────────────────────
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(textColor.copy(alpha = 0.06f)).clickable { onDismiss() }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) { Text("Cancel", fontSize = 14.sp, color = textColor.copy(alpha = 0.6f)) }
                        Box(modifier = Modifier.weight(2f).clip(RoundedCornerShape(14.dp)).background(GradientMain).clickable {
                            if (name.isBlank()) { Toast.makeText(context, "Profile name zaruri hai!", Toast.LENGTH_SHORT).show(); return@clickable }
                            onSave(UserProfile(id = profileToEdit?.id ?: 0, profileName = name.trim(), fullName = fullName.trim(), email = email.trim(), phoneNumber = phone.trim(), address = address.trim(), customFields = fieldsList.toMap(), sections = sectionsList.toList()))
                        }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) { Text("Save Profile", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold) }
                    }
                } // Column
            } // Inner Box
        } // Outer Box
    } // Dialog
} // AddProfileDialog

// ── Premium Field Component ───────────────────────────────────
@Composable
fun PremiumField(value: String, onValueChange: (String) -> Unit, label: String, hint: String, isLast: Boolean, labelColor: Color, hintColor: Color, dividerColor: Color, textColor: Color = Color.White) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(modifier = Modifier.fillMaxWidth()
        .background(if (isFocused) Color(0xFF7F77DD).copy(alpha = 0.08f) else Color.Transparent)
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 10.sp, color = if (isFocused) PurpleStart else labelColor)
        Spacer(modifier = Modifier.height(3.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = textColor),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(PurpleStart),
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) Text(hint, fontSize = 14.sp, color = hintColor)
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp)
            .background(if (isFocused) PurpleStart.copy(alpha = 0.7f) else textColor.copy(alpha = 0.08f)))
    }
    if (!isLast) Divider(color = dividerColor, thickness = 0.5.dp)
}

@Composable fun ChangePinDialog(onDismiss: () -> Unit, onPinChanged: (String) -> Unit) {
    var newPin by remember { mutableStateOf("") }; var confirmPin by remember { mutableStateOf("") }; var step by remember { mutableStateOf(1) }; var errorMsg by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(24.dp), containerColor = MaterialTheme.colorScheme.surface,
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(GradientMain), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Pin, null, tint = Color.White, modifier = Modifier.size(18.dp)) }; Spacer(modifier = Modifier.width(10.dp)); Text(if (step == 1) "New PIN" else "Confirm PIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) } },
        text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Text(if (step == 1) "Enter new 4-digit PIN" else "Confirm your new PIN", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.height(20.dp)); val current = if (step == 1) newPin else confirmPin; Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { repeat(4) { i -> Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(if (i < current.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))) } }; if (errorMsg.isNotEmpty()) { Spacer(modifier = Modifier.height(8.dp)); Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }; Spacer(modifier = Modifier.height(20.dp)); val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","⌫")); keys.forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { key -> if (key.isEmpty()) Spacer(modifier = Modifier.size(56.dp)) else Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable { errorMsg = ""; if (key == "⌫") { if (step == 1) { if (newPin.isNotEmpty()) newPin = newPin.dropLast(1) } else { if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1) else { step = 1; if (newPin.isNotEmpty()) newPin = newPin.dropLast(1) } } } else { if (step == 1) { if (newPin.length < 4) { newPin += key; if (newPin.length == 4) step = 2 } } else { if (confirmPin.length < 4) { confirmPin += key; if (confirmPin.length == 4) { if (newPin == confirmPin) onPinChanged(newPin) else { errorMsg = "PINs don't match!"; confirmPin = ""; newPin = ""; step = 1 } } } } } }, contentAlignment = Alignment.Center) { if (key == "⌫") Icon(Icons.Rounded.Backspace, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) else Text(key, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary) } } }; Spacer(modifier = Modifier.height(8.dp)) } } },
        confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}