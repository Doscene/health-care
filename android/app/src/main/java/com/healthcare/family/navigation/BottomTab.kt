package com.healthcare.family.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航 Tab 定义。
 * @param visibleForRoles 哪些角色可见，为空表示所有角色可见。
 */
enum class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val visibleForRoles: Set<String> = emptySet(),
) {
    HOME("home", "首页", Icons.Default.Home),
    FAMILY("family", "家庭", Icons.Default.FavoriteBorder),
    DIET("diet", "饮食", Icons.Default.MenuBook),
    DISCOVER("discover", "发现", Icons.Default.Search, visibleForRoles = setOf("patient")),
    PROFILE("profile", "我的", Icons.Default.Person),
}
