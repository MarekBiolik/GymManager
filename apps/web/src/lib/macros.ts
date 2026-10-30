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

export type DailyMacroTotals = {
  eatenOn: string;
  macros: Macros;
};

export type EntryMacroSnapshot = {
  eatenOn: string;
  proteinG: number;
  carbsG: number;
  fatG: number;
  kcal: number;
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

export function goalProgressOf(consumed: Macros, goals: NutritionSettingsGoals): GoalProgress {
  const ratio = (value: number, target: number) => (target <= 0 ? 0 : Math.max(0, value / target));
  return {
    calories: ratio(consumed.kcal, goals.calorieTargetKcal),
    protein: ratio(consumed.proteinG, goals.proteinTargetG),
    carbs: ratio(consumed.carbsG, goals.carbsTargetG),
    fat: ratio(consumed.fatG, goals.fatTargetG),
  };
}

export const NutritionAggregations = {
  dailyTotals(entries: EntryMacroSnapshot[]): DailyMacroTotals[] {
    const byDay = new Map<string, Macros>();
    for (const e of entries) {
      const snap = macros(e.proteinG, e.carbsG, e.fatG, e.kcal);
      const prev = byDay.get(e.eatenOn) ?? zeroMacros();
      byDay.set(e.eatenOn, addMacros(prev, snap));
    }
    return [...byDay.entries()]
      .map(([eatenOn, m]) => ({ eatenOn, macros: m }))
      .sort((a, b) => (a.eatenOn < b.eatenOn ? -1 : 1));
  },

  averageMacros(days: DailyMacroTotals[]): Macros {
    if (days.length === 0) {
      return zeroMacros();
    }
    const sum = days.reduce((acc, d) => addMacros(acc, d.macros), zeroMacros());
    const n = days.length;
    return {
      proteinG: sum.proteinG / n,
      carbsG: sum.carbsG / n,
      fatG: sum.fatG / n,
      kcal: sum.kcal / n,
    };
  },

  macroCalorieShares(m: Macros): { protein: number; carbs: number; fat: number } {
    const p = m.proteinG * 4;
    const c = m.carbsG * 4;
    const f = m.fatG * 9;
    const total = p + c + f;
    if (total <= 0) {
      return { protein: 0, carbs: 0, fat: 0 };
    }
    return { protein: p / total, carbs: c / total, fat: f / total };
  },
};
