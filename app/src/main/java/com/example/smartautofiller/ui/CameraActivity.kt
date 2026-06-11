package com.example.smartautofiller.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.smartautofiller.ui.theme.SmartautofillerTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

// ── Data model ────────────────────────────────────────────────
data class ScannedData(
    val docType: String = "Document",
    val name: String = "",
    val fatherName: String = "",
    val dob: String = "",
    val panNumber: String = "",
    val aadhaarNumber: String = "",
    val dlNumber: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val rawText: String = "",
    val sectionFields: List<Pair<String, String>> = emptyList()
)

class CameraActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartautofillerTheme {
                CameraScreen(
                    onResult = { data ->
                        val result = Intent().apply {
                            putExtra("detected_name",    data.name)
                            putExtra("detected_email",   data.email)
                            putExtra("detected_phone",   data.phone)
                            putExtra("detected_pan",     data.panNumber)
                            putExtra("detected_aadhaar", data.aadhaarNumber)
                            putExtra("detected_dob",     data.dob)
                            putExtra("detected_address", data.address)
                            putExtra("detected_dl",      data.dlNumber)
                            putExtra("detected_father",  data.fatherName)
                            putExtra("doc_type",         data.docType)
                            val labels = data.sectionFields.map { it.first }.toTypedArray()
                            val values = data.sectionFields.map { it.second }.toTypedArray()
                            putExtra("section_labels", labels)
                            putExtra("section_values", values)
                        }
                        setResult(Activity.RESULT_OK, result)
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun CameraScreen(onResult: (ScannedData) -> Unit, onBack: () -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) Toast.makeText(context, "Camera permission required!", Toast.LENGTH_LONG).show()
    }
    LaunchedEffect(Unit) { if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Rounded.CameraAlt, null, tint = Color(0xFF7F77DD), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Camera Permission Required", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Document scan karne ke liye camera access do", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F77DD))) { Text("Allow Camera") }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onBack) { Text("Back", color = Color.White.copy(alpha = 0.7f)) }
            }
        }
        return
    }

    var scannedData by remember { mutableStateOf<ScannedData?>(null) }
    var isScanning  by remember { mutableStateOf(false) }
    val executor    = remember { Executors.newSingleThreadExecutor() }
    val recognizer  = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Camera preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    val preview  = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val capture  = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
                    imageCapture = capture
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Scan frame
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.2f).align(Alignment.TopCenter).background(Color.Black.copy(alpha = 0.55f)))
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.25f).align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.55f)))
            Box(modifier = Modifier.fillMaxWidth(0.9f).height(210.dp).border(2.dp, Color(0xFF7F77DD), RoundedCornerShape(12.dp)))
        }

        // Top bar
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
            }
            Text("Document Scan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.size(40.dp))
        }

        // Hint
        if (scannedData == null) {
            Column(modifier = Modifier.align(Alignment.Center).offset(y = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Document frame mein rakho", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PAN", "Aadhaar", "DL", "Marksheet").forEach { label ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                            Text(label, fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Bottom panel
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val data = scannedData
            if (data != null) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F1E)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp).heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(Color(0xFF7F77DD), Color(0xFF378ADD)))), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.DocumentScanner, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(data.docType, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7F77DD))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        data.sectionFields.forEach { (label, value) ->
                            if (value.isNotBlank()) ScanRow(Icons.Rounded.CheckCircle, label, value)
                        }
                        if (data.sectionFields.isEmpty()) {
                            Text("Koi data detect nahi hua.\nDocument seedha rakho, achhi roshni mein.", fontSize = 12.sp, color = Color(0xFFFF6B6B), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { scannedData = null }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Retry")
                    }
                    Button(onClick = { onResult(data) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F77DD))) {
                        Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Use Data")
                    }
                }
            } else {
                if (isScanning) {
                    CircularProgressIndicator(color = Color(0xFF7F77DD), modifier = Modifier.size(56.dp), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Scanning...", fontSize = 13.sp, color = Color.White)
                } else {
                    Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF7F77DD).copy(alpha = 0.85f)).border(3.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        IconButton(onClick = {
                            isScanning = true
                            val capture = imageCapture ?: run { isScanning = false; return@IconButton }
                            capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)
                                    recognizer.process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            scannedData = extractAllData(visionText.text)
                                            isScanning = false
                                        }
                                        .addOnFailureListener {
                                            isScanning = false
                                            Toast.makeText(context, "Scan failed. Try again.", Toast.LENGTH_SHORT).show()
                                        }
                                    image.close()
                                }
                                override fun onError(e: ImageCaptureException) { isScanning = false }
                            })
                        }) {
                            Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap to scan", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun ScanRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Icon(icon, null, tint = Color(0xFF7F77DD), modifier = Modifier.size(14.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 9.sp, color = Color(0xFF9E9E9E))
            Text(value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
    Divider(color = Color.White.copy(alpha = 0.05f))
}

// ── ML Kit Extraction (Free + Offline) ───────────────────────
fun extractAllData(text: String): ScannedData {
    val lines     = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    val upperText = text.uppercase()

    // ── Document type ─────────────────────────────────────────
    val docType = when {
        upperText.contains("INCOME TAX") || upperText.contains("PERMANENT ACCOUNT") -> "PAN Card"
        upperText.contains("AADHAAR") || upperText.contains("AADHAR") || upperText.contains("UIDAI") || upperText.contains("आधार") -> "Aadhaar Card"
        upperText.contains("DRIVING LICENCE") || upperText.contains("DRIVING LICENSE") -> "Driving Licence"
        upperText.contains("PASSPORT") -> "Passport"
        upperText.contains("VOTER") -> "Voter ID"
        upperText.contains("MARKSHEET") || upperText.contains("BOARD OF SECONDARY") ||
                upperText.contains("SECONDARY EDUCATION") || upperText.contains("माध्यमिक") ||
                upperText.contains("CBSE") || upperText.contains("ICSE") || upperText.contains("MPBSE") -> {
            when {
                upperText.contains("10+2") || upperText.contains("HIGHER SECONDARY") || upperText.contains("12TH") -> "12th Marksheet"
                else -> "10th Marksheet"
            }
        }
        else -> "Document"
    }

    val sectionFields = mutableListOf<Pair<String, String>>()

    // ── Regex patterns ────────────────────────────────────────
    val panRegex     = Regex("[A-Z]{5}[0-9]{4}[A-Z]")
    val aadhaarRegex = Regex("\\d{4}[\\s]?\\d{4}[\\s]?\\d{4}")
    val dobRegex     = Regex("[0-3]\\d[/\\-.][0-1]\\d[/\\-.][12]\\d{3}")
    val emailRegex   = Regex("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")
    val phoneRegex   = Regex("(?:\\+91[\\s]?)?[6-9]\\d{9}")

    val pan      = panRegex.find(upperText.replace("\\s".toRegex(), ""))?.value ?: ""
    val aadhaar  = aadhaarRegex.find(text)?.value?.replace(" ","")?.let { if (it.length == 12) it else "" } ?: ""
    val dob      = dobRegex.find(text)?.value ?: ""
    val email    = emailRegex.find(text)?.value ?: ""
    val phone    = phoneRegex.find(text)?.value ?: ""

    // ── Name detection ────────────────────────────────────────
    val skipWords = setOf("income tax", "govt", "government", "permanent account", "uidai",
        "driving", "motor vehicle", "passport", "election", "india", "भारत", "board",
        "secondary", "education", "marksheet", "certificate", "माध्यमिक", "शिक्षा", "मण्डल",
        "cbse", "icse", "mpbse", "high school", "higher secondary", "bhopal", "subject",
        "theory", "practical", "maximum", "minimum", "obtained", "remarks", "date", "roll",
        "enrol", "centre", "school", "regular", "private", "grand total", "result", "pass",
        "division", "shri", "sushri", "certified", "appeared", "examination", "year")

    fun isSkip(line: String): Boolean {
        val lo = line.lowercase().trim()
        return lo.length < 3 || skipWords.any { lo.contains(it) } ||
                lo.all { it.isDigit() || it == '/' || it == '-' || it == ' ' || it == '.' } ||
                lo.contains("@") || lo.contains("http") || panRegex.containsMatchIn(lo.uppercase())
    }

    fun looksLikeName(t: String): Boolean {
        if (isSkip(t)) return false
        if (t.length < 3 || t.length > 50) return false
        if (t.count { it.isDigit() } > 2) return false
        val words = t.split("\\s+".toRegex()).filter { it.isNotBlank() }
        return words.isNotEmpty() && words.size <= 5 && words.count { it[0].isLetter() } >= 1
    }

    fun cleanName(s: String) = s.replace(Regex("[^a-zA-Z\\s\\u0900-\\u097F]"), "").trim().take(50)

    var name = ""
    var fatherName = ""
    var motherName = ""

    // Label-based detection
    for (i in lines.indices) {
        val line = lines[i]
        val lo = line.lowercase()
        val next = lines.getOrNull(i + 1) ?: ""

        // Name
        if (name.isBlank() && (lo.contains("shri/sushri") || lo.contains("shri") || lo.contains("certified that"))) {
            val candidate = line.substringAfterLast(" ").trim()
            if (looksLikeName(candidate)) name = cleanName(candidate)
            else if (looksLikeName(next)) name = cleanName(next)
        }
        if (name.isBlank() && (lo.startsWith("name") || lo.contains("name:"))) {
            val after = line.substringAfter(":").trim()
            if (looksLikeName(after)) name = cleanName(after)
            else if (looksLikeName(next)) name = cleanName(next)
        }

        // Father name
        if (fatherName.isBlank() && (lo.contains("father") || lo.contains("s/o") || lo.contains("husband"))) {
            val after = line.substringAfter(":").trim()
            if (looksLikeName(after)) fatherName = cleanName(after)
            else if (looksLikeName(next)) fatherName = cleanName(next)
        }

        // Mother name
        if (motherName.isBlank() && lo.contains("mother")) {
            val after = line.substringAfter(":").trim()
            if (looksLikeName(after)) motherName = cleanName(after)
            else if (looksLikeName(next)) motherName = cleanName(next)
        }
    }

    // Fallback name
    if (name.isBlank()) {
        for (line in lines) {
            if (isSkip(line)) continue
            if (emailRegex.containsMatchIn(line) || phoneRegex.containsMatchIn(line)) continue
            if (line.contains(Regex("\\d{5,}"))) continue
            if (looksLikeName(line)) { name = cleanName(line); break }
        }
    }

    // ── Marksheet specific fields ─────────────────────────────
    var rollNo = ""; var enrolNo = ""; var sNo = ""; var board = ""; var examYear = ""
    var grandTotal = ""; var result = ""; var schoolName = ""

    if (docType.contains("Marksheet")) {

        // Board name
        for (line in lines) {
            val lo = line.lowercase()
            if (lo.contains("board of") || lo.contains("madhyamik") || lo.contains("माध्यमिक") || lo.contains("cbse") || lo.contains("icse")) {
                board = line.trim().take(60); break
            }
        }

        // Exam year
        val yearRegex = Regex("20[12][0-9]")
        examYear = yearRegex.find(text)?.value ?: ""

        // S.No / Certificate No
        val snoRegex = Regex("(?:S\\.?NO|S\\.?N\\.?O|CERTIFICATE NO|SER\\.? NO)[.:\\s]*([A-Z0-9]{4,12})", RegexOption.IGNORE_CASE)
        sNo = snoRegex.find(text)?.groupValues?.get(1) ?: ""

        // Roll Number
        val rollRegex = Regex("(?:ROLL[\\s\\-]?(?:NO|NUMBER|NUMB)[.:\\s]*|ROLL[:\\s]+)([A-Z0-9]{6,12})", RegexOption.IGNORE_CASE)
        rollNo = rollRegex.find(text)?.groupValues?.get(1) ?: ""

        // Enrolment Number
        val enrolRegex = Regex("(?:ENROL[A-Z]*[\\s\\-]?(?:NO|NUMBER)[.:\\s]*|ENROL[:\\s]+)([A-Z0-9/]{6,15})", RegexOption.IGNORE_CASE)
        enrolNo = enrolRegex.find(text)?.groupValues?.get(1) ?: ""

        // Grand Total
        val totalRegex = Regex("(?:GRAND[\\s]?TOTAL|महायोग)[\\s:]*([0-9]{2,4})", RegexOption.IGNORE_CASE)
        grandTotal = totalRegex.find(text)?.groupValues?.get(1) ?: ""

        // Result
        val resultRegex = Regex("(?:RESULT|परिणाम)[\\s:/]*([A-Z ]{4,30})", RegexOption.IGNORE_CASE)
        result = resultRegex.find(text)?.groupValues?.get(1)?.trim() ?: ""
        if (result.isBlank() && upperText.contains("FIRST DIVISION")) result = "PASS IN FIRST DIVISION"
        else if (result.isBlank() && upperText.contains("SECOND DIVISION")) result = "PASS IN SECOND DIVISION"
        else if (result.isBlank() && upperText.contains("PASS")) result = "PASS"

        // School name
        for (i in lines.indices) {
            val lo = lines[i].lowercase()
            if (lo.contains("school") || lo.contains("vidyalaya") || lo.contains("college")) {
                if (!lo.contains("high school certificate") && !lo.contains("board")) {
                    schoolName = lines[i].trim().take(60); break
                }
            }
        }

        // ── Subject marks detection ───────────────────────────
        // Known subjects list
        val knownSubjects = listOf(
            "ENGLISH", "HINDI", "SANSKRIT", "MATHEMATICS", "MATH", "SCIENCE",
            "SOCIAL SCIENCE", "PHYSICS", "CHEMISTRY", "BIOLOGY", "COMPUTER",
            "HISTORY", "GEOGRAPHY", "CIVICS", "ECONOMICS", "POLITICAL SCIENCE",
            "ACCOUNTANCY", "BUSINESS STUDIES", "PHYSICAL EDUCATION",
            "HOME SCIENCE", "DRAWING", "MUSIC", "URDU", "MARATHI", "GUJARATI",
            "अंग्रेजी", "हिंदी", "गणित", "विज्ञान", "सामाजिक विज्ञान"
        )

        // Marks pattern: 3-digit number at end of line (obtained marks)
        val marksRegex = Regex("(\\d{3})\\s*$")

        for (i in lines.indices) {
            val line = lines[i]
            val lineUpper = line.uppercase().trim()

            // Check if line contains a known subject
            val matchedSubject = knownSubjects.firstOrNull { subject ->
                lineUpper.contains(subject) && !lineUpper.contains("MAXIMUM") &&
                        !lineUpper.contains("MINIMUM") && !lineUpper.contains("SUBJECT")
            }

            if (matchedSubject != null) {
                // Try to find marks on same line
                val numbersInLine = Regex("\\d{2,3}").findAll(line).map { it.value.toInt() }.toList()

                // Marks obtained usually last 3-digit number or specific position
                val marks = when {
                    numbersInLine.size >= 3 -> {
                        // Format: Theory Practical Total OR Max Min Obtained
                        // Last meaningful number is usually obtained marks
                        val last = numbersInLine.lastOrNull { it in 1..100 }
                        last?.toString() ?: ""
                    }
                    numbersInLine.size == 1 -> numbersInLine[0].toString()
                    else -> ""
                }

                val subjectName = matchedSubject.lowercase().replaceFirstChar { it.uppercase() }
                if (marks.isNotBlank()) {
                    sectionFields.add(subjectName to marks)
                }
            }
        }
    }

    // ── Address ───────────────────────────────────────────────
    var address = ""
    val addrLabels = listOf("address", "addr", "पता", "flat", "house", "village", "dist", "pin code")
    for (i in lines.indices) {
        if (addrLabels.any { lines[i].lowercase().contains(it) }) {
            val parts = mutableListOf<String>()
            val first = lines[i].substringAfter(":").trim()
            if (first.length > 3) parts.add(first)
            for (j in i + 1 until minOf(i + 4, lines.size)) {
                if (lines[j].length > 3 && !panRegex.containsMatchIn(lines[j].uppercase())) parts.add(lines[j])
                else break
            }
            address = parts.joinToString(", ").take(120)
            break
        }
    }

    // ── Build section fields in order ────────────────────────
    val orderedFields = mutableListOf<Pair<String, String>>()

    if (docType.contains("Marksheet")) {
        if (board.isNotBlank())      orderedFields.add("Board" to board)
        if (examYear.isNotBlank())   orderedFields.add("Exam Year" to examYear)
        if (sNo.isNotBlank())        orderedFields.add("S.No / Certificate" to sNo)
        if (enrolNo.isNotBlank())    orderedFields.add("Enrolment No" to enrolNo)
        if (rollNo.isNotBlank())     orderedFields.add("Roll Number" to rollNo)
        if (name.isNotBlank())       orderedFields.add("Student Name" to name)
        if (fatherName.isNotBlank()) orderedFields.add("Father's Name" to fatherName)
        if (motherName.isNotBlank()) orderedFields.add("Mother's Name" to motherName)
        if (dob.isNotBlank())        orderedFields.add("Date of Birth" to dob)
        if (schoolName.isNotBlank()) orderedFields.add("School" to schoolName)
        // Subject marks (already added above)
        orderedFields.addAll(sectionFields)
        if (grandTotal.isNotBlank()) orderedFields.add("Grand Total" to grandTotal)
        if (result.isNotBlank())     orderedFields.add("Result" to result)
    } else {
        when (docType) {
            "PAN Card" -> {
                if (name.isNotBlank())       orderedFields.add("Name" to name)
                if (fatherName.isNotBlank()) orderedFields.add("Father's Name" to fatherName)
                if (dob.isNotBlank())        orderedFields.add("Date of Birth" to dob)
                if (pan.isNotBlank())        orderedFields.add("PAN Number" to pan)
            }
            "Aadhaar Card" -> {
                if (name.isNotBlank())       orderedFields.add("Name" to name)
                if (dob.isNotBlank())        orderedFields.add("Date of Birth" to dob)
                if (aadhaar.isNotBlank())    orderedFields.add("Aadhaar Number" to aadhaar)
                if (address.isNotBlank())    orderedFields.add("Address" to address)
                if (phone.isNotBlank())      orderedFields.add("Phone" to phone)
            }
            "Driving Licence" -> {
                if (name.isNotBlank())       orderedFields.add("Name" to name)
                if (dob.isNotBlank())        orderedFields.add("Date of Birth" to dob)
                val dl = Regex("[A-Z]{2}[0-9]{2}[\\s]?[0-9]{4,7}").find(upperText)?.value ?: ""
                if (dl.isNotBlank())         orderedFields.add("DL Number" to dl)
                if (address.isNotBlank())    orderedFields.add("Address" to address)
            }
            else -> {
                if (name.isNotBlank())       orderedFields.add("Name" to name)
                if (fatherName.isNotBlank()) orderedFields.add("Father's Name" to fatherName)
                if (dob.isNotBlank())        orderedFields.add("Date of Birth" to dob)
                if (pan.isNotBlank())        orderedFields.add("PAN Number" to pan)
                if (aadhaar.isNotBlank())    orderedFields.add("Aadhaar Number" to aadhaar)
                if (phone.isNotBlank())      orderedFields.add("Phone" to phone)
                if (email.isNotBlank())      orderedFields.add("Email" to email)
                if (address.isNotBlank())    orderedFields.add("Address" to address)
            }
        }
    }

    return ScannedData(
        docType       = docType,
        name          = name,
        fatherName    = fatherName,
        dob           = dob,
        panNumber     = pan,
        aadhaarNumber = aadhaar,
        address       = address,
        phone         = phone,
        email         = email,
        rawText       = text,
        sectionFields = orderedFields
    )
}