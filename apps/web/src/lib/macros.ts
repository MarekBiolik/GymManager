/**
 * Macro math mirrored from core/domain/Macros.kt — presentation helpers only.
 */

export type Macros = {
  proteinG: number;
  carbsG: number;
  fatG: number;
  kcal: number;
};

export type AmountUnit = "g" | "serving";

export type NutritionSettingsGoals = {
  calorieTargetKcal: number;
  proteinTargetG: number;
  carbsTargetG: number;
  fatTargetG: number;
};

export type GoalProgress = {
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
};

export function derivedKcal(proteinG: number, carbsG: number, fatG: number): number {
  return proteinG * 4 + carbsG * 4 + fatG * 9;
}

export function macros(proteinG: number, carbsG: number, fatG: number, kcal?: number): Macros {
  return {
    proteinG,
    carbsG,
    fatG,
    kcal: kcal ?? derivedKcal(proteinG, carbsG, fatG),
  };
}

export function zeroMacros(): Macros {
  return macros(0, 0, 0, 0);
}

export function addMacros(a: Macros, b: Macros): Macros {
  return {
    proteinG: a.proteinG + b.proteinG,
    carbsG: a.carbsG + b.carbsG,
    fatG: a.fatG + b.fatG,
    kcal: a.kcal + b.kcal,
  };
}

export function per100gScaled(per100: Macros, grams: number): Macros {
  const factor = grams / 100;
  return macros(per100.proteinG * factor, per100.carbsG * factor, per100.fatG * factor);
}

/** Aggregate recipe component macros into per-100g values for the recipe food. */
export function recipeMacrosPer100g(totalComponentMacros: Macros, totalGrams: number): Macros {
  if (totalGrams <= 0) {
    throw new Error("recipe must have totalGrams > 0");
  }
  const factor = 100 / totalGrams;
  return macros(
    totalComponentMacros.proteinG * factor,
    totalComponentMacros.carbsG * factor,
    totalComponentMacros.fatG * factor,
  );
}

export function resolveGrams(
  amount: number,
  unit: AmountUnit,
  servingGrams: number | null | undefined,
  defaultServingG: number | null | undefined,
): number {
  if (amount <= 0) {
    throw new Error("amount must be > 0");
  }
  if (unit === "g") {
    return amount;
  }
  const gramsPerServing = servingGrams ?? defaultServingG;
  if (gramsPerServing == null || gramsPerServing <= 0) {
    throw new Error("serving unit requires food_serving or default_serving_g");
  }
  return amount * gramsPerServing;
}

export function goalProgressOf(consumed: Macros, goals: NutritionSettingsGoals): GoalProgress {
  const ratio = (value: number, target: number) => (target <= 0 ? 0 : Math.max(0, value / target));
  return {
    calories: ratio(consumed.kcal, goals.calorieTargetKcal),
    protein: ratio(consumed.proteinG, goals.proteinTargetG),
    carbs: ratio(consumed.carbsG, goals.carbsTargetG),
    fat: ratio(consumed.fatG, goals.fatTargetG),
  };
}
