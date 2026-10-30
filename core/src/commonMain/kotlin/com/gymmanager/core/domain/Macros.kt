package com.gymmanager.core.domain

/**
 * Macro snapshot / per-100g values. Calories derived as 4P + 4C + 9F unless overridden.
 */
data class Macros(
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val kcal: Double = derivedKcal(proteinG, carbsG, fatG),
) {
    companion object {
        fun derivedKcal(proteinG: Double, carbsG: Double, fatG: Double): Double =
            proteinG * 4.0 + carbsG * 4.0 + fatG * 9.0

        fun per100gScaled(per100: Macros, grams: Double): Macros {
            val factor = grams / 100.0
            return Macros(
                proteinG = per100.proteinG * factor,
                carbsG = per100.carbsG * factor,
                fatG = per100.fatG * factor,
            )
        }

        fun zero(): Macros = Macros(0.0, 0.0, 0.0, 0.0)
    }

    operator fun plus(other: Macros): Macros =
        Macros(
            proteinG = proteinG + other.proteinG,
            carbsG = carbsG + other.carbsG,
            fatG = fatG + other.fatG,
            kcal = kcal + other.kcal,
        )
}

enum class AmountUnit {
    G,
    SERVING,
    ;

    fun toDb(): String =
        when (this) {
            G -> "g"
            SERVING -> "serving"
        }

    companion object {
        fun fromDb(value: String): AmountUnit =
            when (value) {
                "g" -> G
                "serving" -> SERVING
                else -> error("Unknown amount unit: $value")
            }
    }
}

/**
 * Resolve logged amount to grams given optional serving definition.
 */
fun resolveGrams(
    amount: Double,
    unit: AmountUnit,
    servingGrams: Double?,
    defaultServingG: Double?,
): Double {
    require(amount > 0.0) { "amount must be > 0" }
    return when (unit) {
        AmountUnit.G -> amount
        AmountUnit.SERVING -> {
            val gramsPerServing =
                servingGrams
                    ?: defaultServingG
                    ?: error("serving unit requires food_serving or default_serving_g")
            require(gramsPerServing > 0.0)
            amount * gramsPerServing
        }
    }
}
