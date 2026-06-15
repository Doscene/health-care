package com.healthcare.family.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthcare.family.ui.home.child.ChildHomeContent
import com.healthcare.family.ui.home.elderly.ElderlyHomeContent
import com.healthcare.family.ui.home.young.YoungHomeContent

/**
 * 首页：根据用户角色显示不同内容。
 */
@Composable
fun HomeScreen(
    userRole: String?,
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // 根据角色加载数据（兼容大小写）
    LaunchedEffect(userRole) {
        when (userRole?.lowercase()) {
            "patient" -> viewModel.loadPatientHome()
            "family" -> viewModel.loadFamilyHome()
            else -> viewModel.loadPatientHome()
        }
    }

    when (userRole?.lowercase()) {
        "patient" -> YoungHomeContent(
            onNavigate = onNavigate,
            patientHome = uiState.patientHome,
            isLoading = uiState.isLoading,
        )
        "family" -> ChildHomeContent(
            onNavigate = onNavigate,
            familyHome = uiState.familyHome,
            isLoading = uiState.isLoading,
        )
        else -> ElderlyHomeContent(
            onNavigate = onNavigate,
            patientHome = uiState.patientHome,
            isLoading = uiState.isLoading,
        )
    }
}
