package com.healthcare.family.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.remote.api.MemberDetailDto
import com.healthcare.family.data.repository.FamilyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberDetailUiState(
    val detail: MemberDetailDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberDetailUiState())
    val uiState: StateFlow<MemberDetailUiState> = _uiState.asStateFlow()

    fun loadMemberDetail(familyId: String, memberId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            familyRepository.getMemberDetail(familyId, memberId).fold(
                onSuccess = { detail -> _uiState.update { it.copy(detail = detail, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } },
            )
        }
    }
}
