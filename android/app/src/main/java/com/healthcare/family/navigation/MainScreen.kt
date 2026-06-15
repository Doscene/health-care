package com.healthcare.family.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.healthcare.family.data.local.TokenManager
import com.healthcare.family.ui.diet.DietScreen
import com.healthcare.family.ui.discover.DiscoverScreen
import com.healthcare.family.ui.family.FamilyScreen
import com.healthcare.family.ui.home.HomeScreen
import com.healthcare.family.ui.profile.ProfileScreen

/**
 * 主界面：Scaffold + 底部导航栏 + NavHost。
 */
@Composable
fun MainScreen(
    tokenManager: TokenManager,
    onNavigateToDetail: (String) -> Unit,
    onLogout: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val userRole by tokenManager.userRole.collectAsState(initial = null)

    // 根据角色过滤可见 Tab
    val visibleTabs = BottomTab.entries.filter { tab ->
        tab.visibleForRoles.isEmpty() || tab.visibleForRoles.contains(userRole)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                visibleTabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") {
                HomeScreen(userRole = userRole, onNavigate = onNavigateToDetail)
            }
            composable("family") {
                FamilyScreen(onNavigate = onNavigateToDetail)
            }
            composable("diet") {
                DietScreen(onNavigate = onNavigateToDetail)
            }
            composable("discover") {
                DiscoverScreen(onNavigate = onNavigateToDetail)
            }
            composable("profile") {
                ProfileScreen(onNavigate = onNavigateToDetail, onLogout = onLogout)
            }
        }
    }
}
