package com.healthcare.family.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.remote.api.ChemistryDto
import com.healthcare.family.data.remote.api.FamilyChallengeDto
import com.healthcare.family.data.remote.api.FamilyDto
import com.healthcare.family.data.remote.api.FamilyGoalDto
import com.healthcare.family.data.remote.api.FamilyReminderDto
import com.healthcare.family.data.remote.api.MemberDto
import com.healthcare.family.data.remote.api.MemberHealthSummaryDto
import com.healthcare.family.data.repository.FamilyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyUiState(
    val families: List<FamilyDto> = emptyList(),
    val selectedFamilyId: String? = null,
    val members: List<MemberDto> = emptyList(),
    val memberSummaries: List<MemberHealthSummaryDto> = emptyList(),
    val chemistry: ChemistryDto? = null,
    val goals: List<FamilyGoalDto> = emptyList(),
    val challenges: List<FamilyChallengeDto> = emptyList(),
    val reminders: List<FamilyReminderDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createSuccess: Boolean = false,
    val joinSuccess: Boolean = false,
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    init {
        loadFamilies()
    }

    fun loadFamilies() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            familyRepository.getMyFamilies().fold(
                onSuccess = { families ->
                    val firstId = families.firstOrNull()?.id
                    _uiState.update {
                        it.copy(
                            families = families,
                            selectedFamilyId = firstId ?: it.selectedFamilyId,
                            isLoading = false,
                        )
                    }
                    // 自动加载第一个家庭的成员
                    if (firstId != null) {
                        loadMembers(firstId)
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun createFamily(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            familyRepository.createFamily(name).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, createSuccess = true) }
                    loadFamilies()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun verifyAndJoin(code: String, role: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            familyRepository.joinFamily(code, role).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, joinSuccess = true) }
                    loadFamilies()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun loadMembers(familyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            familyRepository.getMembers(familyId).fold(
                onSuccess = { members ->
                    _uiState.update { it.copy(members = members, isLoading = false) }
                    loadPhase3Data(familyId)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    private fun loadPhase3Data(familyId: String) {
        viewModelScope.launch {
            familyRepository.getFamilySummary(familyId).fold(
                onSuccess = { summaries -> _uiState.update { it.copy(memberSummaries = summaries) } },
                onFailure = { },
            )
        }
        viewModelScope.launch {
            familyRepository.getChemistry(familyId).fold(
                onSuccess = { chemistry -> _uiState.update { it.copy(chemistry = chemistry) } },
                onFailure = { },
            )
        }
        viewModelScope.launch {
            familyRepository.getGoals(familyId).fold(
                onSuccess = { goals -> _uiState.update { it.copy(goals = goals) } },
                onFailure = { },
            )
        }
        viewModelScope.launch {
            familyRepository.getChallenges(familyId).fold(
                onSuccess = { challenges -> _uiState.update { it.copy(challenges = challenges) } },
                onFailure = { },
            )
        }
        viewModelScope.launch {
            familyRepository.getReminders(familyId).fold(
                onSuccess = { reminders -> _uiState.update { it.copy(reminders = reminders) } },
                onFailure = { },
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
