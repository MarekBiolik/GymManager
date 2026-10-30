package com.gymmanager.core.domain

/**
 * Pure aggregation helpers for charts / diary summaries.
 */
object NutritionAggregations {
    fun dailyTotals(entries: List<FoodEntry>): List<DailyMacroTotals> =
        entries
            .groupBy { it.eatenOn }
            .map { (day, dayEntries) ->
                DailyMacroTotals(
                    eatenOn = day,
                    macros = dayEntries.fold(Macros.zero()) { acc, e -> acc + e.macros },
                )
            }.sortedBy { it.eatenOn }

    fun averageMacros(days: List<DailyMacroTotals>): Macros {
        if (days.isEmpty()) return Macros.zero()
        val sum = days.fold(Macros.zero()) { acc, d -> acc + d.macros }
        val n = days.size.toDouble()
        return Macros(
            proteinG = sum.proteinG / n,
            carbsG = sum.carbsG / n,
            fatG = sum.fatG / n,
            kcal = sum.kcal / n,
        )
    }

    /** Share of calories from P/C/F (0..1 each). */
    fun macroCalorieShares(macros: Macros): Triple<Double, Double, Double> {
        val p = macros.proteinG * 4.0
        val c = macros.carbsG * 4.0
        val f = macros.fatG * 9.0
        val total = p + c + f
        if (total <= 0.0) return Triple(0.0, 0.0, 0.0)
        return Triple(p / total, c / total, f / total)
    }
}
