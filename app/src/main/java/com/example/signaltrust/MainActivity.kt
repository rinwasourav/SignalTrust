package com.example.signaltrust

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.signaltrust.ui.components.SignalTrustBottomBar
import com.example.signaltrust.ui.screens.*
import com.example.signaltrust.ui.theme.SignalTrustTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SignalTrustTheme {
                SignalTrustApp()
            }
        }
    }
}

@Composable
fun RequestPermissions(context: Context) {
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    LaunchedEffect(Unit) {
        // 1. Request Call Screening Role (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleLauncher.launch(intent)
            }
        }

        // 2. Request Overlay Permission (Display over other apps)
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            overlayLauncher.launch(intent)
        }
    }
}

@Composable
fun SignalTrustApp() {
    val navController = rememberNavController()
    val viewModel: SignalTrustViewModel = viewModel()
    val context = LocalContext.current

    // Request necessary roles and permissions for the demo
    RequestPermissions(context)

    Scaffold(
        bottomBar = {
            SignalTrustBottomBar(navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("report/unknown")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Report a number"
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onSearchClick = { navController.navigate("search") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }

            composable("search") {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onResult = { number ->
                        navController.navigate("result/$number")
                    }
                )
            }

            composable(
                route = "result/{number}",
                arguments = listOf(navArgument("number") { type = NavType.StringType })
            ) { backStackEntry ->
                val number = backStackEntry.arguments?.getString("number") ?: ""
                RiskResultScreen(
                    number = number,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onReport = { navController.navigate("report/$number") }
                )
            }

            composable(
                route = "report/{number}",
                arguments = listOf(navArgument("number") { type = NavType.StringType })
            ) { backStackEntry ->
                val number = backStackEntry.arguments?.getString("number") ?: "unknown"
                ReportScreen(
                    initialNumber = number,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSubmitted = { reportedNumber, category ->
                        navController.navigate("report-success/$reportedNumber/$category") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                )
            }

            composable(
                route = "report-success/{number}/{category}",
                arguments = listOf(
                    navArgument("number") { type = NavType.StringType },
                    navArgument("category") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val number = backStackEntry.arguments?.getString("number") ?: ""
                val category = backStackEntry.arguments?.getString("category") ?: ""
                ReportSuccessScreen(
                    number = number,
                    category = category,
                    onHome = { navController.navigate("home") }
                )
            }

            composable("reports") {
                ReportsScreen(viewModel = viewModel)
            }

            composable("settings") {
                SettingsScreen()
            }
        }
    }
}
