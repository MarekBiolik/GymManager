package com.gymmanager.core

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.application.account.DeleteAccount
import com.gymmanager.core.application.auth.AuthSession
import com.gymmanager.core.application.auth.DefaultAuthSession
import com.gymmanager.core.application.measurements.MeasurementRepository
import com.gymmanager.core.application.nutrition.FoodEntryRepository
import com.gymmanager.core.application.nutrition.FoodRepository
import com.gymmanager.core.application.nutrition.MealTypeRepository
import com.gymmanager.core.application.nutrition.NutritionSettingsRepository
import com.gymmanager.core.application.nutrition.RecipeRepository
import com.gymmanager.core.application.nutrition.WaterRepository
import com.gymmanager.core.application.profiles.ProfileRepository
import com.gymmanager.core.data.off.KtorOffProductClient
import com.gymmanager.core.data.off.OffProductClient
import com.gymmanager.core.data.off.createPlatformHttpClient
import com.gymmanager.core.data.supabase.SupabaseAuthGateway
import com.gymmanager.core.data.supabase.SupabaseConfig
import com.gymmanager.core.data.supabase.SupabaseDeleteAccount
import com.gymmanager.core.data.supabase.SupabaseFoodEntryRemote
import com.gymmanager.core.data.supabase.SupabaseFoodRemote
import com.gymmanager.core.data.supabase.SupabaseFoodServingRemote
import com.gymmanager.core.data.supabase.SupabaseMealTypeRemote
import com.gymmanager.core.data.supabase.SupabaseMeasurementRemote
import com.gymmanager.core.data.supabase.SupabaseNutritionSettingsRemote
import com.gymmanager.core.data.supabase.SupabaseProfileRemote
import com.gymmanager.core.data.supabase.SupabaseRecipeIngredientRemote
import com.gymmanager.core.data.supabase.SupabaseWaterEntryRemote
import com.gymmanager.core.data.supabase.createGymManagerSupabaseClient
import com.gymmanager.core.sync.InMemoryReadCache
import com.gymmanager.core.sync.ReadCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Clock

/**
 * Shared mobile wiring: auth + hello-sync + nutrition settings / meal types / foods / entries / recipes / water / OFF.
 */
data class GymManagerServices(
    val auth: AuthSession,
    val profiles: ProfileRepository,
    val measurements: MeasurementRepository,
    val nutritionSettings: NutritionSettingsRepository,
    val mealTypes: MealTypeRepository,
    val foods: FoodRepository,
    val foodEntries: FoodEntryRepository,
    val recipes: RecipeRepository,
    val water: WaterRepository,
    val offProducts: OffProductClient,
    val deleteAccount: DeleteAccount,
    val cache: ReadCache,
)

fun createGymManagerServices(
    config: SupabaseConfig,
    scope: CoroutineScope,
    network: NetworkStatus,
    cache: ReadCache = InMemoryReadCache(),
    nowEpochMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    offProducts: OffProductClient = KtorOffProductClient(createPlatformHttpClient()),
): GymManagerServices {
    val client = createGymManagerSupabaseClient(config)
    val foodRemote = SupabaseFoodRemote(client)
    val servingRemote = SupabaseFoodServingRemote(client)
    return GymManagerServices(
        auth = DefaultAuthSession(SupabaseAuthGateway(client, scope)),
        profiles =
            ProfileRepository(
                remote = SupabaseProfileRemote(client),
                cache = cache,
                network = network,
                nowEpochMs = nowEpochMs,
            ),
        measurements =
            MeasurementRepository(
                remote = SupabaseMeasurementRemote(client),
                cache = cache,
                network = network,
                nowEpochMs = nowEpochMs,
            ),
        nutritionSettings =
            NutritionSettingsRepository(
                remote = SupabaseNutritionSettingsRemote(client),
                cache = cache,
                network = network,
                nowEpochMs = nowEpochMs,
            ),
        mealTypes =
            MealTypeRepository(
                remote = SupabaseMealTypeRemote(client),
                cache = cache,
                network = network,
                nowEpochMs = nowEpochMs,
            ),
        foods =
            FoodRepository(
                remote = foodRemote,
                servingRemote = servingRemote,
                cache = cache,
                network = network,
                nowEpochMs = nowEpochMs,
            ),
        foodEntries =
            FoodEntryRepository(
                remote = SupabaseFoodEntryRemote(client),
                foodRemote = foodRemote,
                servingRemote = servingRemote,
                cache = cache,
                network = network,
                nowEpochMs = nowEpochMs,
            ),
        recipes =
            RecipeRepository(
                foodRemote = foodRemote,
                ingredientRemote = SupabaseRecipeIngredientRemote(client),
                network = network,
                nowEpochMs = nowEpochMs,
            ),
        water =
            WaterRepository(
                remote = SupabaseWaterEntryRemote(client),
                cache = cache,
                network = network,
                nowEpochMs = nowEpochMs,
            ),
        offProducts = offProducts,
        deleteAccount = SupabaseDeleteAccount(client),
        cache = cache,
    )
}
