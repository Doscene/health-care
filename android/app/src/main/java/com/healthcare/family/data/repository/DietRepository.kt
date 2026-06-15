package com.healthcare.family.data.repository

import com.healthcare.family.data.remote.api.DailyMenuDto
import com.healthcare.family.data.remote.api.HealthCareApi
import com.healthcare.family.data.remote.api.RecipeDto
import com.healthcare.family.data.remote.api.SubstitutionDto
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
            Result.failure(e)
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
            Result.failure(e)
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
            Result.failure(e)
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
            Result.failure(e)
        }
    }
}
