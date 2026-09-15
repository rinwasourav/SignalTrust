package com.example.signaltrust

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.signaltrust.ui.theme.SignalTrustTheme
import com.example.signaltrust.ui.theme.Red

class SpamWarningActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure the activity shows over the lock screen and call screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val number = intent.getStringExtra("number") ?: "9000000003"
        val score = intent.getIntExtra("score", 91)

        setContent {
            SignalTrustTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Red,
                            modifier = Modifier.size(80.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Potential spam call",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Red,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Red.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Caller: $number", fontWeight = FontWeight.Bold)
                                Text("Risk score: $score/100", color = Red)
                                Text("Category: Possible OTP fraud", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            "This caller may request personal or banking information. Never share OTPs, PINs, or passwords.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { finish() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Red)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Block caller")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = {
                                // In a real demo, we'd navigate to the report screen
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Report, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Report number")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        TextButton(onClick = { finish() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}
