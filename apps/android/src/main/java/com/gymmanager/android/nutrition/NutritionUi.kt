package com.gymmanager.android.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymmanager.android.theme.GymTokens
import com.gymmanager.core.domain.GoalProgress
import com.gymmanager.core.domain.Macros
import com.gymmanager.core.domain.NutritionSettings
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult
import kotlin.math.roundToInt

data class NutritionBannerState(
    val cacheBanner: String? = null,
    val errorMessage: String? = null,
    val online: Boolean = true,
)

fun <T> ReadResult<T>.unwrap(banner: (String?) -> Unit): T? =
    when (this) {
        is ReadResult.Fresh -> {
            banner(null)
            data
        }
        is ReadResult.Stale -> {
            banner("Showing cached data (last updated $fetchedAtEpochMs)")
            data
        }
        is ReadResult.Empty -> {
            banner(null)
            null
        }
    }

fun WriteResult<*>.errorMessageOrNull(): String? =
    when (this) {
        is WriteResult.OfflineRejected -> "A network connection is required to save."
        is WriteResult.Failed -> message
        is WriteResult.Success -> null
    }

@Composable
fun NutritionStatusBanner(state: NutritionBannerState) {
    if (!state.online) {
        Text(
            text = "Offline — reads may use cache; saves need a connection.",
            color = GymTokens.ColorFeedbackWarn,
            modifier = Modifier.fillMaxWidth().padding(bottom = GymTokens.Space2),
        )
    }
    state.cacheBanner?.let {
        Text(
            text = it,
            color = GymTokens.ColorTextSecondary,
            modifier = Modifier.fillMaxWidth().padding(bottom = GymTokens.Space2),
        )
    }
    state.errorMessage?.let {
        Text(
            text = it,
            color = GymTokens.ColorFeedbackError,
            modifier = Modifier.fillMaxWidth().padding(bottom = GymTokens.Space2),
        )
    }
}

@Composable
fun MacroGoalSummary(
    consumed: Macros,
    settings: NutritionSettings?,
    progress: GoalProgress?,
) {
    val kcalText =
        if (settings != null) {
            "${consumed.kcal.roundToInt()} / ${settings.calorieTargetKcal.roundToInt()} kcal"
        } else {
            "${consumed.kcal.roundToInt()} kcal"
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GymTokens.Space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        progress?.let { GoalRing(ratio = it.calories) }
        Text(text = kcalText, color = GymTokens.ColorTextPrimary)
    }
    progress?.let { p ->
        GoalProgressBar("Calories", p.calories)
        GoalProgressBar(
            "Protein",
            p.protein,
            detail =
                settings?.let {
                    "${consumed.proteinG.roundToInt()} / ${it.proteinTargetG.roundToInt()} g"
                },
        )
        GoalProgressBar(
            "Carbs",
            p.carbs,
            detail =
                settings?.let {
                    "${consumed.carbsG.roundToInt()} / ${it.carbsTargetG.roundToInt()} g"
                },
        )
        GoalProgressBar(
            "Fat",
            p.fat,
            detail =
                settings?.let {
                    "${consumed.fatG.roundToInt()} / ${it.fatTargetG.roundToInt()} g"
                },
        )
    }
}

@Composable
private fun GoalRing(ratio: Double) {
    val clamped = ratio.coerceIn(0.0, 1.0).toFloat()
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { clamped },
            modifier = Modifier.size(64.dp),
            color = GymTokens.ColorActionPrimary,
            trackColor = GymTokens.ColorBgMuted,
        )
        Text(
            text = "${(ratio * 100).roundToInt()}%",
            color = GymTokens.ColorTextPrimary,
        )
    }
}

@Composable
private fun GoalProgressBar(
    label: String,
    ratio: Double,
    detail: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = GymTokens.Space1)) {
        Text(
            text = detail?.let { "$label · $it" } ?: label,
            color = GymTokens.ColorTextSecondary,
        )
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0.0, 1.0).toFloat() },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(top = 2.dp)
                    .background(GymTokens.ColorBgMuted),
            color = GymTokens.ColorActionPrimary,
            trackColor = GymTokens.ColorBgMuted,
        )
    }
}