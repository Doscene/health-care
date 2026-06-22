package com.healthcare.family.data.repository

import com.healthcare.family.data.remote.api.CheckItemRequest
import com.healthcare.family.data.remote.api.CustomMenuDto
import com.healthcare.family.data.remote.api.DailyMenuDto
import com.healthcare.family.data.remote.api.DietRecordDto
import com.healthcare.family.data.remote.api.HealthCareApi
import com.healthcare.family.data.remote.api.RecordDietRequest
import com.healthcare.family.data.remote.api.RecipeDto
import com.healthcare.family.data.remote.api.SaveCustomMenuRequest
import com.healthcare.family.data.remote.api.SaveShoppingListRequest
import com.healthcare.family.data.remote.api.ShoppingItemDto
import com.healthcare.family.data.remote.api.ShoppingListDto
import com.healthcare.family.data.remote.api.SubstitutionDto
import com.healthcare.family.util.toUserFriendlyMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DietRepository @Inject constructor(
    private val api: HealthCareApi,
) {

    suspend fun getRecipes(suitableFor: String? = null): Result<List<RecipeDto>> {
        return try {
            val resp = api.getRecipes(suitableFor)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getRecipe(recipeId: String): Result<RecipeDto> {
        return try {
            val resp = api.getRecipe(recipeId)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getDailyMenu(): Result<DailyMenuDto> {
        return try {
            val resp = api.getDailyMenu()
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getSubstitutions(ingredient: String): Result<SubstitutionDto> {
        return try {
            val resp = api.getSubstitutions(ingredient)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getRecommendedRecipes(diseases: String? = null, people: String? = null): Result<List<RecipeDto>> {
        return try {
            val resp = api.getRecommendedRecipes(diseases, people)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun recordDiet(mealType: String, description: String, imageUrl: String? = null): Result<DietRecordDto> {
        return try {
            val resp = api.recordDiet(RecordDietRequest(mealType, description, imageUrl))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getDietRecords(limit: Int = 30): Result<List<DietRecordDto>> {
        return try {
            val resp = api.getDietRecords(limit)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getCustomMenu(): Result<CustomMenuDto> {
        return try {
            val resp = api.getCustomMenu()
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun saveCustomMenu(request: SaveCustomMenuRequest): Result<CustomMenuDto> {
        return try {
            val resp = api.saveCustomMenu(request)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getShoppingList(): Result<ShoppingListDto> {
        return try {
            val resp = api.getShoppingList()
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun saveShoppingList(items: List<ShoppingItemDto>): Result<ShoppingListDto> {
        return try {
            val resp = api.saveShoppingList(SaveShoppingListRequest(items))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun checkShoppingItem(listId: String, itemIndex: Int, checked: Boolean): Result<ShoppingListDto> {
        return try {
            val resp = api.checkShoppingItem(listId, itemIndex, CheckItemRequest(checked))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }
}
