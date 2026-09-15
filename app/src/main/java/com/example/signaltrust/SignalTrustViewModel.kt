package com.example.signaltrust

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class RiskResult(
    val number: String,
    val score: Int,
    val level: String,
    val category: String,
    val reasons: List<String>,
    val confidence: String = "High",
    val action: String = "",
)

data class Report(
    val id: String,
    val number: String,
    val category: String,
    val riskLevel: String,
    val timestamp: String,
    val description: String = "",
)

data class ReportBackendRequest(
    val phoneNumber: String,
    val category: String,
    val description: String? = null,
)

class SignalTrustViewModel : ViewModel() {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val backendUrl = "http://10.0.2.2:8000"

    var protectionActive by mutableStateOf(true)
        private set

    var currentResult by mutableStateOf<RiskResult?>(null)
        private set

    var reports by mutableStateOf<List<Report>>(emptyList())
        private set

    var totalReports by mutableIntStateOf(0)
        private set

    var fraudCampaigns by mutableIntStateOf(0)
        private set

    init {
        refreshAll()
    }

    fun refreshAll() {
        fetchStats()
        fetchReports()
    }

    private fun fetchStats() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$backendUrl/dashboard/stats")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        val result = gson.fromJson(body, Map::class.java)
                        totalReports = (result["total_reports"] as? Number)?.toInt() ?: 0
                        fraudCampaigns = (result["fraud_campaigns"] as? Number)?.toInt() ?: 0
                    }
                }
            } catch (ignored: Exception) {
                // Fallback or keep current
            }
        }
    }

    private fun fetchReports() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$backendUrl/dashboard/reports")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        @Suppress("UNCHECKED_CAST")
                        val resultList = gson.fromJson(body, List::class.java) as List<Map<String, Any>>
                        
                        reports = resultList.map { item ->
                            Report(
                                id = (item["id"] as? Number)?.toInt()?.toString() ?: "0",
                                number = item["number"] as? String ?: "Unknown",
                                category = item["category"] as? String ?: "Report",
                                riskLevel = if (((item["risk_score"] as? Number)?.toInt() ?: 0) >= 75) "HIGH" else "MEDIUM",
                                timestamp = "Recent", // Simplified
                                description = "",
                            )
                        }
                    }
                }
            } catch (ignored: Exception) {
                // Fallback to local simulation if backend fails
                if (reports.isEmpty()) {
                    reports = listOf(
                        Report("1", "9000000003", "OTP fraud", "HIGH", "2 minutes ago"),
                        Report("2", "9000000002", "Possible spam", "MEDIUM", "Yesterday"),
                    )
                }
            }
        }
    }

    fun toggleProtection() {
        protectionActive = !protectionActive
    }

    fun checkNumber(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$backendUrl/reputation?phone_number=$number")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        val result = gson.fromJson(body, Map::class.java)
                        
                        val score = (result["risk_score"] as? Number)?.toInt() ?: 0
                        val level = result["risk_level"] as? String ?: "LOW"
                        val masked = result["phone_number"] as? String ?: number
                        
                        currentResult = RiskResult(
                            number = masked,
                            score = score,
                            level = level,
                            category = if (score > 0) (result["category"] as? String ?: "Detected Threat") else "Verified business",
                            reasons = listOf("Backend analysis complete", "Reputation score: $score"),
                            action = when {
                                score >= 75 -> "Never share OTPs, PINs, or banking passwords"
                                score >= 40 -> "Screen the call before sharing personal information"
                                else -> "Allow and show caller information"
                            },
                        )
                    }
                }
            } catch (ignored: Exception) {
                // Fallback to local simulation if backend fails
                val cleanNumber = number.replace("\\s".toRegex(), "").takeLast(10)
                currentResult = when (cleanNumber) {
                    "9000000001" -> RiskResult(
                        number = number,
                        score = 8,
                        level = "LOW",
                        category = "Verified business",
                        reasons = listOf("Verified demo organization", "No recent reports", "No suspicious connections"),
                        action = "Allow and show caller information",
                    )
                    "9000000002" -> RiskResult(
                        number = number,
                        score = 52,
                        level = "MEDIUM",
                        category = "Possible spam",
                        reasons = listOf("Three community reports", "Caller is not verified", "Similar calling pattern"),
                        action = "Screen the call before sharing personal information",
                    )
                    else -> RiskResult(
                        number = number,
                        score = 91,
                        level = "HIGH",
                        category = "OTP fraud",
                        reasons = listOf("Ten recent reports", "Connected to four suspicious numbers", "Several OTP-fraud reports", "Caller is not verified"),
                        action = "Never share OTPs, PINs, or banking passwords",
                    )
                }
            }
        }
    }

    fun submitReport(number: String, category: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reportReq = ReportBackendRequest(number, category, description)
                val json = gson.toJson(reportReq)
                val body = json.toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url("$backendUrl/reports")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        refreshAll()
                    }
                }
            } catch (ignored: Exception) {
                // Fallback to local
                val newReport = Report(
                    id = System.currentTimeMillis().toString(),
                    number = number,
                    category = category,
                    riskLevel = "HIGH",
                    timestamp = "Just now",
                    description = description,
                )
                reports = listOf(newReport) + reports
            }
        }
    }
    
    fun markAsSafe(number: String) {
        // In a real app, this would also be a backend call
        currentResult?.let {
            if (it.number == number) {
                currentResult = it.copy(
                    score = maxOf(0, it.score - 10),
                    level = "LOW",
                )
            }
        }
    }

    fun simulateIncomingCall(context: Context, number: String, score: Int) {
        val intent = Intent(context, SpamWarningActivity::class.java).apply {
            putExtra("number", number)
            putExtra("score", score)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
