package com.healthcare.family.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.local.TokenManager
import com.healthcare.family.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoleSelectionUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false,
)

@HiltViewModel
class RoleSelectionViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoleSelectionUiState())
    val uiState: StateFlow<RoleSelectionUiState> = _uiState.asStateFlow()

    /** 保存用户角色到本地并同步到后端 */
    fun saveRole(role: UserRole, extra: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 映射为后端 selfRole 值
            val selfRole = if (role == UserRole.PATIENT) "patient" else "family"

            // 映射病种名称为后端格式
            val diseases = if (role == UserRole.PATIENT) {
                when (extra) {
                    "高血压" -> listOf("hypertension")
                    "糖尿病" -> listOf("diabetes")
                    "双病" -> listOf("hypertension", "diabetes")
                    else -> emptyList()
                }
            } else {
                emptyList()
            }

            // 先保存到本地
            tokenManager.saveUserRole(selfRole)

            // 调用后端接口
            userRepository.updateRole(selfRole, diseases).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, success = true) }
                },
                onFailure = { _ ->
                    // 后端调用失败不影响本地保存，允许用户继续
                    _uiState.update {
                        it.copy(isLoading = false, success = true, errorMessage = null)
                    }
                },
            )
        }
    }
}
