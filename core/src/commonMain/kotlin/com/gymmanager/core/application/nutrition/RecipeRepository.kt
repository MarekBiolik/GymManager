package com.gymmanager.core.application.nutrition

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.Macros
import com.gymmanager.core.domain.RecipeIngredient
import com.gymmanager.core.domain.recipeMacrosPer100g
import com.gymmanager.core.sync.EmptyReason
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult

class RecipeRepository(
    private val foodRemote: FoodRemote,
    private val ingredientRemote: RecipeIngredientRemote,
    private val network: NetworkStatus,
    private val nowEpochMs: () -> Long,
) {
    suspend fun listIngredients(recipeFoodId: String): ReadResult<List<RecipeIngredient>> {
        if (!network.isOnline()) return ReadResult.Empty(EmptyReason.OFFLINE_MISS)
        return try {
            ReadResult.Fresh(ingredientRemote.listForRecipe(recipeFoodId))
        } catch (_: Exception) {
            ReadResult.Empty(EmptyReason.ERROR_MISS)
        }
    }

    /**
     * Persists a recipe food and its ingredient lines. Derives [Food.macrosPer100g]
     * from component macros scaled by ingredient grams.
     *
     * @param ingredients pairs of component food id → grams
     */
    suspend fun saveRecipe(
        recipe: Food,
        ingredients: List<Pair<String, Double>>,
    ): WriteResult<Food> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        require(recipe.isRecipe)
        return try {
            var totalMacros = Macros.zero()
            var totalGrams = 0.0
            val resolved = mutableListOf<Pair<String, Double>>()
            for ((componentId, grams) in ingredients) {
                require(grams > 0.0)
                val component =
                    foodRemote.getById(componentId)
                        ?: return WriteResult.Failed("Component not found: $componentId")
                totalMacros += Macros.per100gScaled(component.macrosPer100g, grams)
                totalGrams += grams
                resolved += componentId to grams
            }
            val per100 = recipeMacrosPer100g(totalMacros, totalGrams)
            val now = nowEpochMs()
            val withMacros =
                recipe.copy(
                    macrosPer100g = per100,
                    defaultServingG = recipe.defaultServingG ?: totalGrams,
                    updatedAtEpochMs = now,
                )
            val saved =
                if (withMacros.id.isBlank()) {
                    foodRemote.insert(withMacros)
                } else {
                    ingredientRemote.deleteForRecipe(withMacros.id)
                    foodRemote.update(withMacros)
                }
            for ((componentId, grams) in resolved) {
                ingredientRemote.insert(
                    RecipeIngredient(
                        id = "",
                        userId = recipe.userId,
                        recipeFoodId = saved.id,
                        componentFoodId = componentId,
                        grams = grams,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now,
                    ),
                )
            }
            WriteResult.Success(saved)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Save recipe failed")
        }
    }
}
