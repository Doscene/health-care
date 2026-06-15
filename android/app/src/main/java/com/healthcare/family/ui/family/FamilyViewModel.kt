package com.healthcare.family.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.remote.api.FamilyDto
import com.healthcare.family.data.remote.api.MemberDto
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
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
