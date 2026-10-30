package com.gymmanager.core.application.nutrition

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.Macros
import com.gymmanager.core.domain.RecipeIngredient
import com.gymmanager.core.sync.EmptyReason
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RecipeRepositoryTest {
    private val userId = "user-a"

    @Test
    fun saveRecipeDerivesMacrosPer100gAndInsertsIngredients() =
        runTest {
            val oats =
                food(
                    id = "oats",
                    name = "Oats",
                    macros = Macros(proteinG = 10.0, carbsG = 60.0, fatG = 5.0),
                )
            val milk =
                food(
                    id = "milk",
                    name = "Milk",
                    macros = Macros(proteinG = 3.0, carbsG = 5.0, fatG = 3.0),
                )
            val foodRemote = RecipeFakeFoodRemote(mutableListOf(oats, milk))
            val ingredientRemote = FakeRecipeIngredientRemote()
            val repo =
                RecipeRepository(
                    foodRemote = foodRemote,
                    ingredientRemote = ingredientRemote,
                    network = NetworkStatus { true },
                    nowEpochMs = { 100L },
                )

            val result =
                assertIs<WriteResult.Success<Food>>(
                    repo.saveRecipe(
                        recipe =
                            Food(
                                id = "",
                                userId = userId,
                                name = "Overnight oats",
                                macrosPer100g = Macros.zero(),
                                defaultServingG = null,
                                isRecipe = true,
                                isFavorite = false,
                                lastUsedAtEpochMs = null,
                                externalSource = null,
                                externalId = null,
                                createdAtEpochMs = 0,
                                updatedAtEpochMs = 0,
                            ),
                        ingredients =
                            listOf(
                                "oats" to 100.0,
                                "milk" to 100.0,
                            ),
                    ),
                )

            // (10+3)/2 = 6.5 protein per 100g; carbs (60+5)/2 = 32.5; fat (5+3)/2 = 4
            assertEquals(6.5, result.data.macrosPer100g.proteinG)
            assertEquals(32.5, result.data.macrosPer100g.carbsG)
            assertEquals(4.0, result.data.macrosPer100g.fatG)
            assertEquals(200.0, result.data.defaultServingG)
            assertTrue(result.data.id.isNotBlank())
            assertEquals(2, ingredientRemote.rows.size)
            assertEquals(result.data.id, ingredientRemote.rows[0].recipeFoodId)
        }

    @Test
    fun saveRecipeOfflineRejected() =
        runTest {
            val repo =
                RecipeRepository(
                    foodRemote = RecipeFakeFoodRemote(),
                    ingredientRemote = FakeRecipeIngredientRemote(),
                    network = NetworkStatus { false },
                    nowEpochMs = { 1L },
                )
            assertIs<WriteResult.OfflineRejected>(
                repo.saveRecipe(
                    Food(
                        id = "",
                        userId = userId,
                        name = "R",
                        macrosPer100g = Macros.zero(),
                        defaultServingG = null,
                        isRecipe = true,
                        isFavorite = false,
                        lastUsedAtEpochMs = null,
                        externalSource = null,
                        externalId = null,
                        createdAtEpochMs = 0,
                        updatedAtEpochMs = 0,
                    ),
                    listOf("x" to 10.0),
                ),
            )
        }

    @Test
    fun listIngredientsOnlineAndOffline() =
        runTest {
            val ingredientRemote =
                FakeRecipeIngredientRemote(
                    mutableListOf(
                        RecipeIngredient(
                            id = "i1",
                            userId = userId,
                            recipeFoodId = "r1",
                            componentFoodId = "c1",
                            grams = 50.0,
                            createdAtEpochMs = 1,
                            updatedAtEpochMs = 1,
                        ),
                    ),
                )
            val online =
                RecipeRepository(
                    foodRemote = RecipeFakeFoodRemote(),
                    ingredientRemote = ingredientRemote,
                    network = NetworkStatus { true },
                    nowEpochMs = { 1L },
                )
            val fresh = assertIs<ReadResult.Fresh<List<RecipeIngredient>>>(online.listIngredients("r1"))
            assertEquals(1, fresh.data.size)

            val offline =
                RecipeRepository(
                    foodRemote = RecipeFakeFoodRemote(),
                    ingredientRemote = ingredientRemote,
                    network = NetworkStatus { false },
                    nowEpochMs = { 1L },
                )
            val empty = assertIs<ReadResult.Empty>(offline.listIngredients("r1"))
            assertEquals(EmptyReason.OFFLINE_MISS, empty.reason)
        }

    private fun food(
        id: String,
        name: String,
        macros: Macros,
    ) = Food(
        id = id,
        userId = userId,
        name = name,
        macrosPer100g = macros,
        defaultServingG = 100.0,
        isRecipe = false,
        isFavorite = false,
        lastUsedAtEpochMs = null,
        externalSource = null,
        externalId = null,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    )
}

private class RecipeFakeFoodRemote(
    private val rows: MutableList<Food> = mutableListOf(),
) : FoodRemote {
    override suspend fun listForUser(userId: String): List<Food> = rows.filter { it.userId == userId }

    override suspend fun getById(id: String): Food? = rows.firstOrNull { it.id == id }

    override suspend fun findByExternal(
        userId: String,
        source: String,
        externalId: String,
    ): Food? = null

    override suspend fun insert(food: Food): Food {
        val saved = food.copy(id = "recipe-${rows.size}")
        rows += saved
        return saved
    }

    override suspend fun update(food: Food): Food {
        val idx = rows.indexOfFirst { it.id == food.id }
        require(idx >= 0)
        rows[idx] = food
        return food
    }

    override suspend fun delete(id: String) {
        rows.removeAll { it.id == id }
    }
}

private class FakeRecipeIngredientRemote(
    val rows: MutableList<RecipeIngredient> = mutableListOf(),
) : RecipeIngredientRemote {
    override suspend fun listForRecipe(recipeFoodId: String): List<RecipeIngredient> =
        rows.filter { it.recipeFoodId == recipeFoodId }

    override suspend fun insert(ingredient: RecipeIngredient): RecipeIngredient {
        val saved = ingredient.copy(id = "ing-${rows.size}")
        rows += saved
        return saved
    }

    override suspend fun delete(id: String) {
        rows.removeAll { it.id == id }
    }

    override suspend fun deleteForRecipe(recipeFoodId: String) {
        rows.removeAll { it.recipeFoodId == recipeFoodId }
    }
}
