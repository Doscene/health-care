package com.healthcare.family.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthcare.family.data.remote.api.AlertDto
import com.healthcare.family.data.remote.api.AlertSummaryDto
import com.healthcare.family.ui.alert.RiskAlertDialog
import com.healthcare.family.ui.home.child.ChildHomeContent
import com.healthcare.family.ui.home.elderly.ElderlyHomeContent
import com.healthcare.family.ui.home.young.YoungHomeContent

/**
 * 首页：根据用户角色显示不同内容，含四级风险弹窗。
 */
@Composable
fun HomeScreen(
    userRole: String?,
    onNavigate: (String) -> Unit,
    onEmergencyCall: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeAlert by remember { mutableStateOf<AlertSummaryDto?>(null) }

    // 根据角色加载数据
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
            onAlertClick = { activeAlert = it },
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
            onAlertClick = { activeAlert = it },
            patientHome = uiState.patientHome,
            isLoading = uiState.isLoading,
        )
    }

    // 风险弹窗
    activeAlert?.let { summary ->
        val alert = AlertDto(
            id = summary.id,
            level = summary.level,
            triggerType = summary.triggerType,
            triggerValue = summary.triggerValue,
            status = "active",
            notifiedContacts = null,
            createdAt = summary.createdAt ?: "",
        )
        RiskAlertDialog(
            alert = alert,
            onAcknowledge = { activeAlert = null },
            onDismiss = { activeAlert = null },
            onEmergencyCall = onEmergencyCall,
        )
    }
}
