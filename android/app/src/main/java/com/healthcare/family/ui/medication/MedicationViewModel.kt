package com.healthcare.family.ui.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.remote.api.AddMedicationRequest
import com.healthcare.family.data.remote.api.MedicationDto
import com.healthcare.family.data.repository.MedicationRepository
import com.healthcare.family.worker.MedicationReminderManager
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
    val calendarData: Map<String, List<MedicationCalendarRecord>> = emptyMap(),
    val ocrRecognizedName: String = "",
)

@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val reminderManager: MedicationReminderManager,
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
        notes: String? = null,
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
                    notes = notes,
                ),
            ).fold(
                onSuccess = { medication ->
                    _uiState.update { it.copy(isLoading = false, successMessage = "添加成功") }
                    // 设置用药提醒
                    reminderManager.scheduleReminders(medication)
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
                    // 取消用药提醒
                    reminderManager.cancelReminders(medicationId)
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

    fun updateError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun updateOcrName(name: String) {
        _uiState.update { it.copy(ocrRecognizedName = name) }
    }

    fun loadCalendarData(year: Int, month: Int) {
        viewModelScope.launch {
            medicationRepository.getCalendarData(year, month).fold(
                onSuccess = { data ->
                    _uiState.update { it.copy(calendarData = data) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                },
            )
        }
    }
}
