package com.example.smartautofiller.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.smartautofiller.ui.theme.SmartautofillerTheme

class PrivacyPolicyActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartautofillerTheme {
                PrivacyPolicyScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {

    val purpleStart = Color(0xFF7F77DD)
    val blueEnd     = Color(0xFF378ADD)
    val greenAccent = Color(0xFF1D9E75)

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(purpleStart, blueEnd)))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Privacy Policy", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            // Trust badges
            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your Data is Safe 🔐", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf(
                        "Data sirf aapke phone mein rahta hai",
                        "Koi bhi server pe data nahi jaata",
                        "Internet permission sirf ML Kit ke liye",
                        "Database encrypted hai (SQLCipher)",
                        "Export file aap khud control karte ho"
                    ).forEach { point ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null,
                                tint = greenAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(point, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            PolicySection(
                title = "Data Collection",
                content = "AI Autofill app koi bhi personal data collect nahi karta. Aapke saare profiles (naam, email, phone, address) sirf aapke phone ki encrypted database mein store hote hain. Koi bhi data humhare servers pe nahi jaata."
            )

            PolicySection(
                title = "Internet Permission",
                content = "Internet permission sirf Google ML Kit ke liye use hoti hai — pehli baar multi-language translation model download karne ke liye. Iske baad app completely offline kaam karta hai. Aapka koi personal data internet pe nahi jaata."
            )

            PolicySection(
                title = "Accessibility Service",
                content = "App Accessibility Service use karta hai taaki forms automatically fill kar sake. Yeh service sirf screen pe dikhne wale form fields read karti hai. Koi bhi data record nahi hota, store nahi hota, ya share nahi hota."
            )

            PolicySection(
                title = "Biometric & PIN",
                content = "Aapka fingerprint ya PIN sirf aapke device pe store hota hai — Android ke secure keystore mein. Hum kabhi bhi aapka biometric data access nahi kar sakte."
            )

            PolicySection(
                title = "Export/Import",
                content = "Jab aap profiles export karte ho, ek JSON file aapke Downloads folder mein save hoti hai. Yeh file sirf aapke control mein hai. Hum is file ko kabhi access nahi karte."
            )

            PolicySection(
                title = "Third Party Libraries",
                content = "App mein Google ML Kit (text recognition, language translation) use hota hai. ML Kit Google ki privacy policy follow karta hai. Baaki sab libraries (Room, Compose, Biometric) Android standard libraries hain."
            )

            PolicySection(
                title = "Contact",
                content = "Koi bhi privacy related sawaal ke liye app ke menu mein jaake feedback dे sakte hain."
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Last updated: March 2026",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PolicySection(title: String, content: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(6.dp))
        Text(content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    }
}