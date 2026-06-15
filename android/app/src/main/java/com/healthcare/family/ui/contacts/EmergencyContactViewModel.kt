package com.healthcare.family.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.remote.api.ContactDto
import com.healthcare.family.data.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactsUiState(
    val contacts: List<ContactDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class EmergencyContactViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            alertRepository.getEmergencyContacts().fold(
                onSuccess = { contacts ->
                    _uiState.update { it.copy(contacts = contacts, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun addContact(name: String, phone: String, relation: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            alertRepository.addEmergencyContact(name, phone, relation).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, successMessage = "添加成功") }
                    loadContacts()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            alertRepository.deleteEmergencyContact(contactId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, successMessage = "已删除") }
                    loadContacts()
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
