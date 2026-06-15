package com.healthcare.family.ui.home

import androidx.compose.runtime.Composable
import com.healthcare.family.ui.home.elderly.ElderlyHomeContent
import com.healthcare.family.ui.home.child.ChildHomeContent
import com.healthcare.family.ui.home.young.YoungHomeContent

/**
 * 首页：根据用户角色显示不同内容。
 */
@Composable
fun HomeScreen(
    userRole: String?,
    onNavigate: (String) -> Unit,
) {
    when (userRole) {
        "PATIENT" -> YoungHomeContent(onNavigate = onNavigate)
        "FAMILY" -> ChildHomeContent(onNavigate = onNavigate)
        else -> ElderlyHomeContent(onNavigate = onNavigate) // 默认老人首页
    }
}
