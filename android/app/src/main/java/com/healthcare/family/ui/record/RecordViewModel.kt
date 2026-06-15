package com.healthcare.family.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.remote.api.BpRecordDto
import com.healthcare.family.data.remote.api.BgRecordDto
import com.healthcare.family.data.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordUiState(
    val systolic: String = "",
    val diastolic: String = "",
    val heartRate: String = "",
    val bgType: String = "fasting",
    val bgValue: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val recentBpRecords: List<BpRecordDto> = emptyList(),
    val recentBgRecords: List<BgRecordDto> = emptyList(),
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    init {
        loadRecentRecords()
    }

    fun onSystolicChanged(value: String) {
        _uiState.update { it.copy(systolic = value, errorMessage = null, successMessage = null) }
    }

    fun onDiastolicChanged(value: String) {
        _uiState.update { it.copy(diastolic = value, errorMessage = null, successMessage = null) }
    }

    fun onHeartRateChanged(value: String) {
        _uiState.update { it.copy(heartRate = value, errorMessage = null, successMessage = null) }
    }

    fun onBgTypeChanged(type: String) {
        _uiState.update { it.copy(bgType = type, errorMessage = null, successMessage = null) }
    }

    fun onBgValueChanged(value: String) {
        _uiState.update { it.copy(bgValue = value, errorMessage = null, successMessage = null) }
    }

    fun submitBpRecord() {
        val state = _uiState.value
        val sys = state.systolic.toIntOrNull()
        val dia = state.diastolic.toIntOrNull()
        val hr = state.heartRate.toIntOrNull()

        if (sys == null || dia == null) {
            _uiState.update { it.copy(errorMessage = "请输入有效的血压数值") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            recordRepository.addBpRecord(sys, dia, hr).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "血压记录成功",
                            systolic = "",
                            diastolic = "",
                            heartRate = "",
                        )
                    }
                    loadRecentRecords()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun submitBgRecord() {
        val state = _uiState.value
        val value = state.bgValue.toDoubleOrNull()

        if (value == null) {
            _uiState.update { it.copy(errorMessage = "请输入有效的血糖数值") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            recordRepository.addBgRecord(state.bgType, value).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "血糖记录成功",
                            bgValue = "",
                        )
                    }
                    loadRecentRecords()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    private fun loadRecentRecords() {
        viewModelScope.launch {
            recordRepository.getBpRecords(5).onSuccess { records ->
                _uiState.update { it.copy(recentBpRecords = records) }
            }
            recordRepository.getBgRecords(5).onSuccess { records ->
                _uiState.update { it.copy(recentBgRecords = records) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
