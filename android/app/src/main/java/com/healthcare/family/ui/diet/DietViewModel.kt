package com.healthcare.family.ui.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.remote.api.DailyMenuDto
import com.healthcare.family.data.remote.api.RecipeDto
import com.healthcare.family.data.remote.api.SubstitutionDto
import com.healthcare.family.data.repository.DietRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DietUiState(
    val recipes: List<RecipeDto> = emptyList(),
    val dailyMenu: DailyMenuDto? = null,
    val substitution: SubstitutionDto? = null,
    val selectedRecipe: RecipeDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class DietViewModel @Inject constructor(
    private val dietRepository: DietRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DietUiState())
    val uiState: StateFlow<DietUiState> = _uiState.asStateFlow()

    init {
        loadRecipes()
    }

    fun loadRecipes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            dietRepository.getRecipes().fold(
                onSuccess = { recipes -> _uiState.update { it.copy(recipes = recipes, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } },
            )
        }
    }

    fun loadDailyMenu() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            dietRepository.getDailyMenu().fold(
                onSuccess = { menu -> _uiState.update { it.copy(dailyMenu = menu, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } },
            )
        }
    }

    fun loadRecipeDetail(recipeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            dietRepository.getRecipe(recipeId).fold(
                onSuccess = { recipe -> _uiState.update { it.copy(selectedRecipe = recipe, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } },
            )
        }
    }

    fun searchSubstitutions(ingredient: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            dietRepository.getSubstitutions(ingredient).fold(
                onSuccess = { sub -> _uiState.update { it.copy(substitution = sub, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } },
            )
        }
    }
}
