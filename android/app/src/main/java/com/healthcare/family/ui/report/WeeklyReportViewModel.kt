package com.healthcare.family.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.remote.RetrofitClient
import com.healthcare.family.data.remote.api.WeeklyReportDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class WeeklyReportUiState(
    val isLoading: Boolean = true,
    val report: WeeklyReportDto? = null,
    val error: String? = null,
    val familyId: String? = null,
)

class WeeklyReportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyReportUiState())
    val uiState: StateFlow<WeeklyReportUiState> = _uiState.asStateFlow()

    fun setFamilyId(familyId: String) {
        _uiState.value = _uiState.value.copy(familyId = familyId)
        loadWeeklyReport()
    }

    fun loadWeeklyReport() {
        val familyId = _uiState.value.familyId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val lastMonday = LocalDate.now().minusWeeks(1)
                    .with(java.time.DayOfWeek.MONDAY)
                val weekStart = lastMonday.format(DateTimeFormatter.ISO_LOCAL_DATE)

                val response = RetrofitClient.api.getWeeklyReport(familyId, weekStart)

                if (response.code == 0) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        report = response.data,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message ?: "加载失败",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "网络错误",
                )
            }
        }
    }

    fun shareReport(onShare: (String) -> Unit) {
        val familyId = _uiState.value.familyId ?: return

        viewModelScope.launch {
            try {
                val lastMonday = LocalDate.now().minusWeeks(1)
                    .with(java.time.DayOfWeek.MONDAY)
                val weekStart = lastMonday.format(DateTimeFormatter.ISO_LOCAL_DATE)

                val response = RetrofitClient.api.getShareCard(familyId, weekStart)

                if (response.code == 0 && response.data != null) {
                    onShare(response.data.html)
                }
            } catch (_: Exception) {
            }
        }
    }
}
