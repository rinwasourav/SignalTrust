package com.example.signaltrust.ui.screens

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.signaltrust.SignalTrustViewModel
import com.example.signaltrust.ui.components.RiskBadge
import com.example.signaltrust.ui.components.SafetyCard
import com.example.signaltrust.ui.theme.Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SignalTrustViewModel,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("SignalTrust", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primaryContainer)
                Text("Your safety network", style = MaterialTheme.typography.bodyLarge)
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SafetyCard(title = "Protecting your calls") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (viewModel.protectionActive) Color(0xFF176B4D) else Color.Gray)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (viewModel.protectionActive) "Protection active" else "Protection paused")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = viewModel.protectionActive, onCheckedChange = { viewModel.toggleProtection() })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onSearchClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Search a phone number", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Community protection", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SafetyCard(modifier = Modifier.weight(1f)) {
                Text(viewModel.totalReports.toString(), style = MaterialTheme.typography.headlineMedium)
                Text("reports", style = MaterialTheme.typography.labelMedium)
            }
            SafetyCard(modifier = Modifier.weight(1f)) {
                Text(viewModel.fraudCampaigns.toString(), style = MaterialTheme.typography.headlineMedium)
                Text("campaigns", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Recent activity", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        if (viewModel.reports.isEmpty()) {
            Text("No recent activity", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        } else {
            viewModel.reports.take(2).forEach { report ->
                SafetyCard(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (report.riskLevel == "HIGH") Icons.Default.Warning else Icons.Default.PriorityHigh,
                            contentDescription = null,
                            tint = if (report.riskLevel == "HIGH") Red else Color(0xFF8A5A00)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(report.number, fontWeight = FontWeight.Bold)
                            Text("${report.category} • ${report.riskLevel} risk", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val context = LocalContext.current
        Text("Demo Simulation", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        SafetyCard(title = "Test incoming calls") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.simulateIncomingCall(context, "9000000003", 91) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Text("Simulate Fraud Call")
                }
                OutlinedButton(
                    onClick = { viewModel.simulateIncomingCall(context, "9000000002", 52) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simulate Spam Call")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onResult: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search number") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Phone number", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { 
                    phoneNumber = it
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("+91 | 9000000000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = error != null,
                supportingText = { error?.let { Text(it) } }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val cleanNumber = phoneNumber.filter { it.isDigit() }
                    if (cleanNumber.length < 10) {
                        error = "Enter a valid phone number."
                    } else {
                        onResult(cleanNumber)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Check number")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Privacy note", style = MaterialTheme.typography.titleMedium)
            Text(
                "Number is tokenized before reputation lookup. Your search is private.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskResultScreen(
    number: String,
    viewModel: SignalTrustViewModel,
    onBack: () -> Unit,
    onReport: () -> Unit
) {
    LaunchedEffect(number) {
        viewModel.checkNumber(number)
    }

    val result = viewModel.currentResult

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caller result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (result == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RiskBadge(level = result.level, score = result.score)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(result.category, style = MaterialTheme.typography.headlineMedium)
                        if (result.level == "LOW") {
                            Text("Demo Hospital", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                SafetyCard(title = "Why this result?") {
                    result.reasons.forEach { reason ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(
                                if (result.level == "LOW") Icons.Default.Check else Icons.Default.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (result.level == "LOW") Color(0xFF176B4D) else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(reason, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SafetyCard(title = "Recommended action") {
                    Text(result.action, style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.markAsSafe(number) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark safe")
                    }
                    Button(
                        onClick = onReport,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Red)
                    ) {
                        Text("Report")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    initialNumber: String,
    viewModel: SignalTrustViewModel,
    onBack: () -> Unit,
    onSubmitted: (String, String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var phoneNumber by remember { mutableStateOf(if (initialNumber == "unknown") "" else initialNumber) }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "Bank impersonation", "OTP or PIN request", "Government impersonation",
        "Loan or investment scam", "Delivery scam", "Job scam",
        "Robocall or telemarketing", "Harassment", "Other"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (step == 1) "Report a number" else "Report details") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step == 2) step = 1 else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (step == 1) {
                Text("Enter the phone number you want to report.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Phone number", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { 
                        phoneNumber = it
                        phoneError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    supportingText = { phoneError?.let { Text(it) } }
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val cleanNumber = phoneNumber.filter { it.isDigit() }
                        if (cleanNumber.length < 10) {
                            phoneError = "Enter a valid phone number."
                        } else {
                            phoneNumber = cleanNumber
                            step = 2
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Continue")
                }
            } else {
                Text("Number: $phoneNumber", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Why are you reporting this number?", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                
                categories.forEach { cat ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (category == cat),
                                onClick = { category = cat },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (category == cat), onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(cat)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("What happened? (Optional)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Describe what happened...") }
                )
                Text(
                    "Do not include passwords, OTPs, card numbers, or other private information.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                SafetyCard(title = "Privacy Confirmation") {
                    Text("Your report will be shared anonymously. Your identity will not be shown.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                        Text("I confirm that this report is accurate and contains no OTP, PIN, password, or card number.", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.submitReport(phoneNumber, category, description)
                        onSubmitted(phoneNumber, category)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = confirmed && category.isNotEmpty()
                ) {
                    Text("Submit anonymous report")
                }
            }
        }
    }
}


@Composable
fun ReportSuccessScreen(
    number: String,
    category: String,
    onHome: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(
                text = "✓",
                fontSize = 64.sp,
                color = Color(0xFF176B4D),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Report submitted", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            SafetyCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Number: $number", fontWeight = FontWeight.Bold)
                    Text("Category: $category")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Your report was submitted anonymously. It may help protect other SignalTrust users.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                Text("Return to home")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: SignalTrustViewModel) {
    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reports") }) }
    ) { padding ->
        if (viewModel.reports.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No reports found", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(viewModel.reports) { report ->
                    SafetyCard(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(if (report.riskLevel == "HIGH") Red.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (report.riskLevel == "HIGH") Icons.Default.Warning else Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = if (report.riskLevel == "HIGH") Red else Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(report.number, fontWeight = FontWeight.Bold)
                                Text("${report.category} • ${report.riskLevel} risk", style = MaterialTheme.typography.bodyMedium)
                                Text(report.timestamp, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Protection", style = MaterialTheme.typography.titleLarge)
            SettingItem("Caller warnings", true)
            SettingItem("Automatic blocking", false)
            SettingItem("SMS analysis", false)
            SettingItem("Audio analysis", false)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Privacy", style = MaterialTheme.typography.titleLarge)
            ListItem(headlineContent = { Text("Permissions") }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("My data") }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Delete account", color = Red) })
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("About SignalTrust", style = MaterialTheme.typography.titleLarge)
            ListItem(headlineContent = { Text("Help and safety") })
            Text("Version 1.0.0-demo", modifier = Modifier.padding(16.dp), color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun SettingItem(title: String, initialValue: Boolean) {
    var checked by remember { mutableStateOf(initialValue) }
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = { checked = it }) }
    )
}
