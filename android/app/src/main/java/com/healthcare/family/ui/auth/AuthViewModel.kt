package com.healthcare.family.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.local.TokenManager
import com.healthcare.family.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val phone: String = "",
    val code: String = "",
    val isLoading: Boolean = false,
    val isCodeSent: Boolean = false,
    val countdown: Int = 0,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
    val agreedToPrivacy: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun onPhoneChanged(phone: String) {
        _uiState.update { it.copy(phone = phone, errorMessage = null) }
    }

    fun onCodeChanged(code: String) {
        _uiState.update { it.copy(code = code, errorMessage = null) }
    }

    fun onPrivacyToggled(agreed: Boolean) {
        _uiState.update { it.copy(agreedToPrivacy = agreed) }
    }

    fun sendCode() {
        val phone = _uiState.value.phone
        if (!isValidPhone(phone)) {
            _uiState.update { it.copy(errorMessage = "请输入正确的手机号") }
            return
        }
        if (!_uiState.value.agreedToPrivacy) {
            _uiState.update { it.copy(errorMessage = "请先同意隐私协议") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.sendCode(phone).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isCodeSent = true) }
                    startCountdown()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "发送失败") }
                },
            )
        }
    }

    fun login() {
        val state = _uiState.value
        if (!isValidPhone(state.phone)) {
            _uiState.update { it.copy(errorMessage = "请输入正确的手机号") }
            return
        }
        if (state.code.length != 6) {
            _uiState.update { it.copy(errorMessage = "请输入6位验证码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.login(state.phone, state.code).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "登录失败") }
                },
            )
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 60 downTo 0) {
                _uiState.update { it.copy(countdown = i) }
                delay(1000)
            }
        }
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.matches(Regex("^1[3-9]\\d{9}$"))
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
