package com.healthcare.family.ui.kit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.remote.api.FirstAidGuideDto
import com.healthcare.family.data.remote.api.FirstAidKitDto
import com.healthcare.family.data.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FirstAidKitUiState(
    val items: List<FirstAidKitDto> = emptyList(),
    val guides: List<FirstAidGuideDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class FirstAidKitViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirstAidKitUiState())
    val uiState: StateFlow<FirstAidKitUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            alertRepository.getFirstAidKit().fold(
                onSuccess = { items ->
                    _uiState.update { it.copy(items = items, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun addItem(name: String, type: String, quantity: Int, expireDate: String?, notes: String?) {
        viewModelScope.launch {
            alertRepository.addFirstAidItem(
                com.healthcare.family.data.remote.api.FirstAidKitRequest(
                    name = name,
                    type = type,
                    quantity = quantity,
                    expireDate = expireDate,
                    notes = notes,
                ),
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "添加成功") }
                    loadItems()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                },
            )
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            alertRepository.deleteFirstAidItem(itemId).fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "已删除") }
                    loadItems()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                },
            )
        }
    }

    fun loadGuides() {
        viewModelScope.launch {
            alertRepository.getEmergencyGuides().fold(
                onSuccess = { guides ->
                    _uiState.update { it.copy(guides = guides) }
                },
                onFailure = {},
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
