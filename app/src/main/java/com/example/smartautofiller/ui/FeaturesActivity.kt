package com.example.smartautofiller.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.example.smartautofiller.ui.theme.SmartautofillerTheme

class FeaturesActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartautofillerTheme {
                FeaturesScreen(onBack = { finish() })
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FeaturesScreen(onBack: () -> Unit) {

    val gradients = listOf(
        listOf(Color(0xFF7F77DD), Color(0xFF378ADD)),
        listOf(Color(0xFF1D9E75), Color(0xFF378ADD)),
        listOf(Color(0xFFD4537E), Color(0xFF7F77DD)),
        listOf(Color(0xFF534AB7), Color(0xFF1D9E75)),
    )
    val titles = listOf("Smart Form Fill", "Profile Selector", "Text Expansion", "Secure & Smart")
    var currentPage by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Auto advance pages
    LaunchedEffect(currentPage) {
        kotlinx.coroutines.delay(5000)
        if (currentPage < 3) currentPage++
        else onBack()
    }

    // Trigger JS demo on page change
    LaunchedEffect(currentPage) {
        kotlinx.coroutines.delay(300)
        webViewRef?.evaluateJavascript("runDemo($currentPage);", null)
    }

    val gradient = Brush.linearGradient(gradients[currentPage])

    Column(modifier = Modifier.fillMaxSize().background(gradient)) {

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.Close, null, tint = Color.White)
            }
            Text("Features", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("${currentPage + 1}/4", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
        }

        Text(
            titles[currentPage],
            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // WebView with phone demo
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL(null, getDemoHtml(), "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dots + progress
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..3).forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == currentPage) 20.dp else 8.dp, 8.dp)
                            .background(
                                if (i == currentPage) Color.White else Color.White.copy(alpha = 0.4f),
                                RoundedCornerShape(50)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Close button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Close", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

fun getDemoHtml(): String = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<style>
* { margin:0; padding:0; box-sizing:border-box; }
body { background:transparent; display:flex; justify-content:center; align-items:flex-start; padding-top:10px; height:100vh; font-family:sans-serif; overflow:hidden; }
@keyframes bPulse{0%,100%{transform:scale(1)}50%{transform:scale(1.1)}}
@keyframes bTap{0%{transform:scale(1)}40%{transform:scale(0.82)}100%{transform:scale(1)}}
@keyframes rip1{0%{transform:scale(0.5);opacity:0.7}100%{transform:scale(3);opacity:0}}
@keyframes rip2{0%{transform:scale(0.5);opacity:0.5}100%{transform:scale(3.8);opacity:0}}
@keyframes fillIn{0%{background:#fff}50%{background:#eef7ff}100%{background:#f0fff8}}
@keyframes toastIn{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:translateY(0)}}
@keyframes typeIn{from{opacity:0;transform:scaleX(0.5)}to{opacity:1;transform:scaleX(1)}}
.phone{background:#1a1a2e;border-radius:32px;padding:7px;width:190px;margin:0 auto}
.screen{background:#fff;border-radius:24px;overflow:hidden;height:320px;position:relative}
.sbar{background:#7F77DD;padding:7px 12px 5px;display:flex;justify-content:space-between}
.flabel{font-size:10px;color:#555;font-weight:600;padding:0 10px;margin-bottom:2px}
.finput{margin:0 10px 8px;border:1.5px solid #e0e0e0;border-radius:6px;padding:5px 8px;font-size:10px;color:#aaa;font-style:italic;background:#fff;transition:all 0.3s}
.finput.filled{border-color:#1D9E75;background:#f0fff8;color:#0a6644;font-style:normal;font-weight:600;animation:fillIn 0.4s ease}
.finput.typing{border-color:#7F77DD;background:#f8f7ff;color:#3C3489;font-style:normal}
.bubble{width:44px;height:44px;border-radius:50%;background:linear-gradient(135deg,#7F77DD,#378ADD);display:flex;align-items:center;justify-content:center;position:relative;animation:bPulse 2s infinite}
.r1,.r2{position:absolute;width:44px;height:44px;border-radius:50%;top:0;left:0;background:rgba(127,119,221,0.4)}
.toast{background:#1D9E75;color:#fff;padding:5px 12px;border-radius:16px;font-size:10px;font-weight:700;text-align:center;animation:toastIn 0.4s ease}
.msgbox{margin:8px 10px;background:#f5f5f5;border-radius:8px;padding:8px;min-height:100px;font-size:10px;color:#333;white-space:pre-wrap}
</style>
</head>
<body>
<div class="phone">
  <div class="screen" id="scr">
    <div class="sbar" id="sb">
      <span style="color:white;font-size:9px;font-weight:700;" id="sbt">Any Type of Form</span>
      <span style="color:white;font-size:9px;">9:41</span>
    </div>
    <div id="content"></div>
    <div style="position:absolute;bottom:12px;left:10px;" id="bwrap">
      <div class="bubble" id="bubble">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M4 6h16M4 11h16M4 16h10" stroke="white" stroke-width="2.2" stroke-linecap="round"/>
          <circle cx="20" cy="20" r="4" fill="#1D9E75"/>
          <path d="M18.5 20l1.2 1.3L22 18.5" stroke="white" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <div class="r1" id="r1" style="display:none"></div>
        <div class="r2" id="r2" style="display:none"></div>
      </div>
    </div>
    <div id="toast-wrap" style="position:absolute;bottom:10px;left:8px;right:8px;display:none;">
      <div class="toast" id="tmsg"></div>
    </div>
    <div id="selector" style="display:none;position:absolute;left:58px;bottom:12px;background:white;border-radius:10px;padding:8px;box-shadow:0 4px 16px rgba(0,0,0,0.15);z-index:10;width:130px;"></div>
  </div>
</div>

<script>
const sleep = ms => new Promise(r => setTimeout(r, ms));

function setContent(html) { document.getElementById('content').innerHTML = html; }
function setSbar(color, text) {
  document.getElementById('sb').style.background = color;
  document.getElementById('sbt').textContent = text;
}
function resetBubble() {
  const b = document.getElementById('bubble');
  b.style.animation = 'bPulse 2s infinite';
  b.style.opacity = '1';
  b.style.display = 'flex';
  document.getElementById('r1').style.display = 'none';
  document.getElementById('r2').style.display = 'none';
  document.getElementById('bwrap').style.display = 'flex';
  document.getElementById('toast-wrap').style.display = 'none';
  document.getElementById('selector').style.display = 'none';
}

async function runDemo(n) {
  resetBubble();
  if (n === 0) await demo0();
  else if (n === 1) await demo1();
  else if (n === 2) await demo2();
  else if (n === 3) await demo3();
}

// ── Demo 0: Form Fill ──────────────────────────────────────
async function demo0() {
  setSbar('#7F77DD', 'Any Type of Form');
  setContent(`
    <div style="padding:8px 10px 4px;"><div style="font-size:11px;font-weight:700;color:#222;">Contact Information</div><div style="font-size:8px;color:#999;">* Required</div></div>
    <div style="height:1px;background:#eee;margin:0 10px 6px;"></div>
    <div class="flabel">Name *</div><div class="finput" id="fn">Your answer</div>
    <div class="flabel">Email *</div><div class="finput" id="fe">Your answer</div>
    <div class="flabel">Phone</div><div class="finput" id="fp">Your answer</div>
    <div class="flabel">Address</div><div class="finput" id="fa">Your answer</div>
    <div class="flabel">ID number</div><div class="finput" id="fi">Your answer</div>
  `);
  await sleep(1000);
  // Tap bubble
  const b = document.getElementById('bubble');
  b.style.animation = 'bTap 0.4s ease forwards';
  document.getElementById('r1').style.display = 'block';
  document.getElementById('r2').style.display = 'block';
  document.getElementById('r1').style.animation = 'rip1 0.7s ease forwards';
  document.getElementById('r2').style.animation = 'rip2 0.9s ease 0.1s forwards';
  await sleep(500);
  // Fill all at once
  b.style.animation = 'none'; b.style.opacity = '0.3';
  const data = {fn:'ABC XYZ', fe:'abc@example.com', fp:'0123456789', fa:'XYZ Colony, 123', fi:'XXXX-XXXX-XXXX'};
  Object.entries(data).forEach(([id, val]) => {
    const el = document.getElementById(id);
    if (el) { el.className = 'finput filled'; el.textContent = val; }
  });
  document.getElementById('bwrap').style.display = 'none';
  document.getElementById('tmsg').textContent = '✓ 5 fields filled instantly!';
  document.getElementById('toast-wrap').style.display = 'block';
}

// ── Demo 1: Profile Selector ──────────────────────────────
async function demo1() {
  setSbar('#1D9E75', 'Any Type of Form');
  setContent(`
    <div style="padding:8px 10px 4px;"><div style="font-size:11px;font-weight:700;color:#222;">Job Application</div><div style="font-size:8px;color:#999;">* Required</div></div>
    <div style="height:1px;background:#eee;margin:0 10px 6px;"></div>
    <div class="flabel">Full Name *</div><div class="finput" id="fn">Your answer</div>
    <div class="flabel">Email *</div><div class="finput" id="fe">Your answer</div>
    <div class="flabel">Phone</div><div class="finput" id="fp">Your answer</div>
    <div class="flabel">Company</div><div class="finput" id="fc">Your answer</div>
  `);
  await sleep(800);
  // Long press
  const b = document.getElementById('bubble');
  b.style.transform = 'scale(0.85)';
  b.style.transition = 'transform 0.6s';
  await sleep(700);
  // Show selector
  const sel = document.getElementById('selector');
  sel.style.display = 'block';
  sel.innerHTML = `
    <div style="font-size:8px;font-weight:700;color:#333;margin-bottom:6px;">Select Profile</div>
    <div style="display:flex;align-items:center;gap:6px;padding:4px;border-radius:6px;margin-bottom:4px;">
      <div style="width:24px;height:24px;border-radius:7px;background:linear-gradient(135deg,#7F77DD,#534AB7);display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;color:white;">P</div>
      <div><div style="font-size:9px;font-weight:600;color:#222;">Personal</div><div style="font-size:8px;color:#888;">ABC XYZ</div></div>
    </div>
    <div style="display:flex;align-items:center;gap:6px;padding:4px;border-radius:6px;background:#f0fff8;">
      <div style="width:24px;height:24px;border-radius:7px;background:linear-gradient(135deg,#1D9E75,#378ADD);display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;color:white;">W</div>
      <div><div style="font-size:9px;font-weight:600;color:#0a6644;">Work</div><div style="font-size:8px;color:#1D9E75;">ABC XYZ</div></div>
    </div>
  `;
  await sleep(1200);
  sel.style.display = 'none';
  b.style.transform = 'scale(1)'; b.style.opacity = '0.3';
  document.getElementById('bwrap').style.display = 'none';
  const data = {fn:'ABC XYZ', fe:'work@example.com', fp:'0123456789', fc:'XYZ Company Ltd'};
  Object.entries(data).forEach(([id, val]) => {
    const el = document.getElementById(id);
    if (el) { el.className = 'finput filled'; el.textContent = val; }
  });
  document.getElementById('tmsg').textContent = "✓ Filled with 'Work' profile!";
  document.getElementById('toast-wrap').style.display = 'block';
}

// ── Demo 2: Text Expansion ────────────────────────────────
async function demo2() {
  setSbar('#D4537E', 'Any App · WhatsApp · Notes');
  document.getElementById('bwrap').style.display = 'flex';
  setContent(`
    <div style="padding:8px 10px 4px;"><div style="font-size:11px;font-weight:700;color:#222;">Message Box</div></div>
    <div style="height:1px;background:#eee;margin:0 10px 6px;"></div>
    <div class="msgbox" id="msgbox"></div>
    <div style="padding:4px 10px;font-size:8px;color:#999;">Shortcuts: name- · gmail- · mob- · addr-</div>
  `);
  const msgbox = document.getElementById('msgbox');
  const lines = ['name-', '\ngmail-', '\nmob-', '\naddr-'];
  let text = '';
  for (let line of lines) {
    for (let c of line) { text += c; msgbox.textContent = text; await sleep(70); }
    await sleep(300);
  }
  await sleep(400);
  // Tap
  const b = document.getElementById('bubble');
  b.style.animation = 'bTap 0.4s ease forwards';
  document.getElementById('r1').style.display = 'block';
  document.getElementById('r2').style.display = 'block';
  document.getElementById('r1').style.animation = 'rip1 0.7s ease forwards';
  document.getElementById('r2').style.animation = 'rip2 0.9s ease 0.1s forwards';
  await sleep(500);
  b.style.opacity = '0.3'; b.style.animation = 'none';
  document.getElementById('bwrap').style.display = 'none';
  msgbox.style.color = '#0a6644';
  msgbox.style.fontWeight = '600';
  msgbox.textContent = 'name- ABC XYZ\ngmail- abc@example.com\nmob- 0123456789\naddr- XYZ Colony, 123';
  document.getElementById('tmsg').textContent = '✓ 4 lines filled instantly!';
  document.getElementById('toast-wrap').style.display = 'block';
}

// ── Demo 3: Dark Mode + Security ──────────────────────────
async function demo3() {
  setSbar('#534AB7', 'AI Autofill App');
  document.getElementById('bwrap').style.display = 'none';
  setContent(`
    <div style="padding:8px 10px 4px;display:flex;justify-content:space-between;align-items:center;">
      <div style="font-size:11px;font-weight:700;color:#222;" id="atitle">AI Autofill</div>
      <div id="tbtn" style="width:26px;height:26px;border-radius:7px;background:#EEEDFE;display:flex;align-items:center;justify-content:center;">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none"><path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" fill="#7F77DD"/></svg>
      </div>
    </div>
    <div style="height:1px;background:#eee;margin:0 10px 6px;"></div>
    <div style="margin:0 10px 8px;background:#EEEDFE;border-radius:10px;padding:10px;" id="c1">
      <div style="font-size:10px;font-weight:700;color:#534AB7;">AI Bubble</div>
      <div style="font-size:9px;color:#7F77DD;margin-top:2px;">Floating bubble active</div>
    </div>
    <div style="margin:0 10px 8px;background:#f5f5f5;border-radius:10px;padding:10px;" id="c2">
      <div style="font-size:10px;font-weight:700;color:#333;" id="pname">Personal Profile</div>
      <div style="font-size:9px;color:#888;">ABC XYZ · abc@example.com</div>
    </div>
    <div style="margin:0 10px;background:#f5f5f5;border-radius:10px;padding:10px;" id="c3">
      <div style="font-size:10px;font-weight:700;color:#333;">Work Profile</div>
      <div style="font-size:9px;color:#888;">ABC XYZ · work@example.com</div>
    </div>
  `);
  await sleep(1000);
  // Dark mode
  const scr = document.getElementById('scr');
  scr.style.transition = 'background 0.5s';
  scr.style.background = '#1a1a2e';
  document.getElementById('sb').style.background = '#26215C';
  document.getElementById('atitle').style.color = '#E8E6FF';
  document.getElementById('c1').style.background = '#3C3489';
  document.getElementById('c2').style.background = '#2A2845';
  document.getElementById('c3').style.background = '#2A2845';
  document.getElementById('pname').style.color = '#E8E6FF';
  document.getElementById('tmsg').textContent = '🌙 Dark mode on!';
  document.getElementById('toast-wrap').style.display = 'block';
  await sleep(1500);
  document.getElementById('toast-wrap').style.display = 'none';
  // Light mode wapas
  scr.style.background = '#fff';
  document.getElementById('sb').style.background = '#534AB7';
  document.getElementById('atitle').style.color = '#222';
  document.getElementById('c1').style.background = '#EEEDFE';
  document.getElementById('c2').style.background = '#f5f5f5';
  document.getElementById('c3').style.background = '#f5f5f5';
  document.getElementById('pname').style.color = '#333';
  document.getElementById('tmsg').textContent = '☀️ Light mode on!';
  document.getElementById('toast-wrap').style.display = 'block';
}

runDemo(0);
</script>
</body>
</html>
""".trimIndent()