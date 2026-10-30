package com.gymmanager.android.nutrition

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gymmanager.android.theme.GymTokens
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult

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
