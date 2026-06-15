package com.healthcare.family.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoleSelectionViewModel @Inject constructor(
    private val tokenManager: TokenManager,
) : ViewModel() {

    /** 保存用户角色到本地 */
    fun saveRole(role: UserRole, extra: String?) {
        viewModelScope.launch {
            tokenManager.saveUserRole(role.name)
            // TODO: 调用后端接口更新用户角色和病种
        }
    }
}
