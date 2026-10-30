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
