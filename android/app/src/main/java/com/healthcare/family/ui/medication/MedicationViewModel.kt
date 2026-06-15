package com.healthcare.family.ui.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.remote.api.AddMedicationRequest
import com.healthcare.family.data.remote.api.MedicationDto
import com.healthcare.family.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicationUiState(
    val medications: List<MedicationDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicationUiState())
    val uiState: StateFlow<MedicationUiState> = _uiState.asStateFlow()

    init {
        loadMedications()
    }

    fun loadMedications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            medicationRepository.getMedications().fold(
                onSuccess = { meds ->
                    _uiState.update { it.copy(medications = meds, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun addMedication(
        name: String,
        specification: String,
        dosagePerTime: Int,
        frequencyPerDay: Int,
        timeSlots: List<String>,
        startDate: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            medicationRepository.addMedication(
                AddMedicationRequest(
                    name = name,
                    specification = specification,
                    dosagePerTime = dosagePerTime,
                    frequencyPerDay = frequencyPerDay,
                    timeSlots = timeSlots,
                    remindTimes = timeSlots,
                    startDate = startDate,
                ),
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, successMessage = "添加成功") }
                    loadMedications()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun deleteMedication(medicationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            medicationRepository.deleteMedication(medicationId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, successMessage = "已删除") }
                    loadMedications()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
