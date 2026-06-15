package com.example.smartautofiller.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.smartautofiller.MainActivity
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen {
                val prefs = getSharedPreferences("autofill_prefs", MODE_PRIVATE)
                val onboardingDone = prefs.getBoolean("onboarding_done", false)
                val userInfoDone   = prefs.getBoolean("user_info_done", false)
                when {
                    !onboardingDone -> startActivity(Intent(this, OnboardingActivity::class.java))
                    !userInfoDone   -> startActivity(Intent(this, com.example.smartautofiller.ui.UserInfoActivity::class.java))
                    else            -> startActivity(Intent(this, MainActivity::class.java))
                }
                finish()
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var iconVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }

    val iconScale by animateFloatAsState(
        targetValue = if (iconVisible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "iconScale"
    )
    val iconAlpha by animateFloatAsState(targetValue = if (iconVisible) 1f else 0f, animationSpec = tween(400), label = "iconAlpha")
    val textAlpha by animateFloatAsState(targetValue = if (textVisible) 1f else 0f, animationSpec = tween(500), label = "textAlpha")
    val textOffset by animateDpAsState(targetValue = if (textVisible) 0.dp else 20.dp, animationSpec = tween(500, easing = EaseOutCubic), label = "textOffset")
    val taglineAlpha by animateFloatAsState(targetValue = if (taglineVisible) 1f else 0f, animationSpec = tween(500), label = "taglineAlpha")

    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse), label = "pulse"
    )
    val rippleScale by rememberInfiniteTransition(label = "ripple").animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse), label = "ripple"
    )

    LaunchedEffect(Unit) {
        delay(100); iconVisible = true
        delay(400); textVisible = true
        delay(300); taglineVisible = true
        delay(1200); onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF7F77DD), Color(0xFF378ADD)))),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(300.dp).scale(rippleScale).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))
        Box(modifier = Modifier.size(200.dp).scale(rippleScale * 0.9f).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.scale(iconScale * pulseScale).alpha(iconAlpha)
                    .size(100.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                }
                Box(
                    modifier = Modifier.size(32.dp).align(Alignment.BottomEnd).offset(x = 8.dp, y = 8.dp)
                        .clip(CircleShape).background(Color(0xFF1D9E75)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text("AI Autofill", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.alpha(textAlpha).offset(y = textOffset))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Smart · Secure · Fast", fontSize = 15.sp, color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.alpha(taglineAlpha))
            Spacer(modifier = Modifier.height(60.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.alpha(taglineAlpha)) {
                repeat(3) { index ->
                    val dotScale by rememberInfiniteTransition(label = "dot$index").animateFloat(
                        initialValue = 0.6f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(600, delayMillis = index * 200, easing = EaseInOutSine), RepeatMode.Reverse),
                        label = "dotScale$index"
                    )
                    Box(modifier = Modifier.size(8.dp).scale(dotScale).clip(CircleShape).background(Color.White.copy(alpha = 0.8f)))
                }
            }
        }
    }
}