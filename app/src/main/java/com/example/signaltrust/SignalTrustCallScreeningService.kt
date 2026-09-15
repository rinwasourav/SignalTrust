package com.example.signaltrust

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class SignalTrustCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: return
        Log.d("SignalTrust", "Screening call from: $number")

        // For demo purposes, we trigger the warning activity for known fraud numbers
        val cleanNumber = number.filter { it.isDigit() }.takeLast(10)
        
        // Simulating risk lookup
        val riskScore = when (cleanNumber) {
            "9000000003" -> 91
            "9000000002" -> 52
            else -> 0
        }

        if (riskScore >= 40) {
            val intent = Intent(this, SpamWarningActivity::class.java).apply {
                putExtra("number", number)
                putExtra("score", riskScore)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
            
            // SILENCE_CALL requires API 29
            val responseBuilder = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                responseBuilder.setSilenceCall(riskScore >= 75)
            }
            
            respondToCall(callDetails, responseBuilder.build())
        } else {
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }
}
