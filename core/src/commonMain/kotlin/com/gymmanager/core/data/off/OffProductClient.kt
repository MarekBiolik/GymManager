package com.gymmanager.core.data.off

import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.Macros
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class OffProductCandidate(
    val code: String,
    val name: String,
    val macrosPer100g: Macros,
    val defaultServingG: Double?,
)

interface OffProductClient {
    suspend fun search(query: String, pageSize: Int = 20): List<OffProductCandidate>

    suspend fun lookupBarcode(code: String): OffProductCandidate?
}

class KtorOffProductClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://world.openfoodfacts.org",
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        },
) : OffProductClient {
    override suspend fun search(
        query: String,
        pageSize: Int,
    ): List<OffProductCandidate> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val url =
            URLBuilder()
                .takeFrom("$baseUrl/cgi/search.pl")
                .apply {
                    parameters.append("search_terms", trimmed)
                    parameters.append("search_simple", "1")
                    parameters.append("action", "process")
                    parameters.append("json", "1")
                    parameters.append("page_size", pageSize.toString())
                    parameters.append("fields", OFF_FIELDS)
                }.buildString()
        val body = httpClient.get(url).bodyAsText()
        val parsed = json.decodeFromString(OffSearchResponse.serializer(), body)
        return parsed.products.mapNotNull { it.toCandidate() }
    }

    override suspend fun lookupBarcode(code: String): OffProductCandidate? {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return null
        val url = "$baseUrl/api/v2/product/$trimmed?fields=$OFF_FIELDS"
        val body = httpClient.get(url).bodyAsText()
        val parsed = json.decodeFromString(OffProductResponse.serializer(), body)
        if (parsed.status != 1) return null
        return parsed.product?.toCandidate()
    }

    companion object {
        private const val OFF_FIELDS =
            "code,product_name,product_name_en,nutriments,serving_quantity,serving_size"
    }
}

fun OffProductCandidate.toFoodDraft(
    userId: String,
    nowEpochMs: Long,
): Food =
    Food(
        id = "",
        userId = userId,
        name = name,
        macrosPer100g = macrosPer100g,
        defaultServingG = defaultServingG,
        isRecipe = false,
        isFavorite = false,
        lastUsedAtEpochMs = null,
        externalSource = "openfoodfacts",
        externalId = code,
        createdAtEpochMs = nowEpochMs,
        updatedAtEpochMs = nowEpochMs,
    )

@Serializable
private data class OffSearchResponse(
    val products: List<OffProductJson> = emptyList(),
)

@Serializable
private data class OffProductResponse(
    val status: Int = 0,
    val product: OffProductJson? = null,
)

@Serializable
private data class OffProductJson(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_en") val productNameEn: String? = null,
    val nutriments: OffNutrimentsJson? = null,
    @SerialName("serving_quantity") val servingQuantity: Double? = null,
)

@Serializable
private data class OffNutrimentsJson(
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
)

private fun OffProductJson.toCandidate(): OffProductCandidate? {
    val id = code?.trim().orEmpty()
    if (id.isEmpty()) return null
    val name =
        productName?.takeIf { it.isNotBlank() }
            ?: productNameEn?.takeIf { it.isNotBlank() }
            ?: return null
    val n = nutriments ?: return null
    val p = n.proteins100g ?: return null
    val c = n.carbohydrates100g ?: return null
    val f = n.fat100g ?: return null
    return OffProductCandidate(
        code = id,
        name = name,
        macrosPer100g = Macros(p, c, f),
        defaultServingG = servingQuantity?.takeIf { it > 0.0 },
    )
}

expect fun createPlatformHttpClient(): HttpClient
