package com.example.signaltrust.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.signaltrust.ui.theme.Amber
import com.example.signaltrust.ui.theme.Green
import com.example.signaltrust.ui.theme.Red

@Composable
fun RiskBadge(level: String, score: Int) {
    val color = when (level.uppercase()) {
        "HIGH" -> Red
        "MEDIUM" -> Amber
        else -> Green
    }
    
    val icon = when (level.uppercase()) {
        "HIGH" -> Icons.Default.Warning
        "MEDIUM" -> Icons.Default.PriorityHigh
        else -> Icons.Default.CheckCircle
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${level.uppercase()} RISK • $score/100",
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun SafetyCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            content()
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Reports : Screen("reports", "Reports", Icons.Default.Assessment)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun SignalTrustBottomBar(navController: NavController) {
    val items = listOf(Screen.Home, Screen.Search, Screen.Reports, Screen.Settings)
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
