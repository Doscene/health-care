package com.healthcare.family.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.remote.api.PlanDto
import com.healthcare.family.data.repository.AppointmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanUiState(
    val plans: List<PlanDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val addSuccess: Boolean = false,
)

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init {
        loadPlans()
    }

    fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            appointmentRepository.getAppointments().fold(
                onSuccess = { plans ->
                    _uiState.update { it.copy(plans = plans, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun addPlan(hospital: String, department: String, date: String, reminderDays: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            appointmentRepository.addAppointment(hospital, department, date, reminderDays).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, addSuccess = true) }
                    loadPlans()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun deletePlan(planId: String) {
        viewModelScope.launch {
            appointmentRepository.deleteAppointment(planId).fold(
                onSuccess = { loadPlans() },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                },
            )
        }
    }

    fun clearAddSuccess() {
        _uiState.update { it.copy(addSuccess = false) }
    }
}
