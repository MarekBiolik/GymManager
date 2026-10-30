/**
 * Thin supabase-js glue for nutrition_settings, meal_types, foods + servings, food_entries (ADR 0006).
 */
import {
  addMacros,
  macros,
  per100gScaled,
  recipeMacrosPer100g,
  resolveGrams,
  type AmountUnit,
  type Macros,
} from "./macros";
import { cacheGet, cacheKeys, cachePut, cacheRemove } from "./readCache";
import { supabase } from "./supabaseClient";

export type ReadOutcome<T> =
  | { kind: "fresh"; data: T }
  | { kind: "stale"; data: T; fetchedAt: number }
  | { kind: "empty"; reason: "offline" | "error" | "not_found" };

export type WriteOutcome<T> =
  | { kind: "ok"; data: T }
  | { kind: "offline" }
  | { kind: "error"; message: string };

export type NutritionSettingsRow = {
  user_id: string;
  calorie_target_kcal: number;
  protein_target_g: number;
  carbs_target_g: number;
  fat_target_g: number;
  height_cm: number | null;
  water_target_ml: number | null;
  created_at: string;
  updated_at: string;
};

export type MealTypeRow = {
  id: string;
  user_id: string;
  name: string;
  position: number;
  created_at: string;
  updated_at: string;
};

export type FoodRow = {
  id: string;
  user_id: string;
  name: string;
  protein_g: number;
  carbs_g: number;
  fat_g: number;
  default_serving_g: number | null;
  is_recipe: boolean;
  is_favorite: boolean;
  last_used_at: string | null;
  external_source: string | null;
  external_id: string | null;
  created_at: string;
  updated_at: string;
};

export type FoodServingRow = {
  id: string;
  user_id: string;
  food_id: string;
  label: string;
  grams: number;
  position: number;
  created_at: string;
  updated_at: string;
};

export type FoodEntryRow = {
  id: string;
  user_id: string;
  food_id: string | null;
  meal_type_id: string;
  eaten_on: string;
  amount: number;
  unit: AmountUnit;
  food_serving_id: string | null;
  grams: number;
  protein_g: number;
  carbs_g: number;
  fat_g: number;
  kcal: number;
  created_at: string;
  updated_at: string;
};

export type RecipeIngredientRow = {
  id: string;
  user_id: string;
  recipe_food_id: string;
  component_food_id: string;
  grams: number;
  created_at: string;
  updated_at: string;
};

export type WaterEntryRow = {
  id: string;
  user_id: string;
  drunk_on: string;
  amount_ml: number;
  created_at: string;
  updated_at: string;
};

function isOnline(): boolean {
  return typeof navigator === "undefined" ? true : navigator.onLine;
}

function foodMacrosPer100(row: FoodRow): Macros {
  return macros(Number(row.protein_g), Number(row.carbs_g), Number(row.fat_g));
}

async function readCached<T>(
  userId: string,
  key: string,
  fetcher: () => Promise<T | null>,
  notFoundEmpty = true,
): Promise<ReadOutcome<T>> {
  if (!isOnline() || !supabase) {
    const hit = cacheGet<T>(key);
    if (hit && hit.userId === userId) {
      return { kind: "stale", data: hit.payload, fetchedAt: hit.fetchedAt };
    }
    return { kind: "empty", reason: !supabase ? "error" : "offline" };
  }
  try {
    const data = await fetcher();
    if (data == null && notFoundEmpty) {
      cacheRemove(key);
      return { kind: "empty", reason: "not_found" };
    }
    if (data == null) {
      return { kind: "empty", reason: "not_found" };
    }
    cachePut(key, { payload: data, fetchedAt: Date.now(), userId });
    return { kind: "fresh", data };
  } catch {
    const hit = cacheGet<T>(key);
    if (hit && hit.userId === userId) {
      return { kind: "stale", data: hit.payload, fetchedAt: hit.fetchedAt };
    }
    return { kind: "empty", reason: "error" };
  }
}

export async function readNutritionSettings(userId: string): Promise<ReadOutcome<NutritionSettingsRow>> {
  const key = cacheKeys.nutritionSettings(userId);
  return readCached(userId, key, async () => {
    const { data, error } = await supabase!
      .from("nutrition_settings")
      .select("*")
      .eq("user_id", userId)
      .maybeSingle();
    if (error) {
      throw error;
    }
    return data as NutritionSettingsRow | null;
  });
}

export async function updateNutritionSettings(
  userId: string,
  patch: Partial<
    Pick<
      NutritionSettingsRow,
      "calorie_target_kcal" | "protein_target_g" | "carbs_target_g" | "fat_target_g" | "height_cm" | "water_target_ml"
    >
  >,
): Promise<WriteOutcome<NutritionSettingsRow>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { data, error } = await supabase
      .from("nutrition_settings")
      .update(patch)
      .eq("user_id", userId)
      .select()
      .single();
    if (error) {
      throw error;
    }
    cacheRemove(cacheKeys.nutritionSettings(userId));
    return { kind: "ok", data: data as NutritionSettingsRow };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Update failed" };
  }
}

export async function readMealTypes(userId: string): Promise<ReadOutcome<MealTypeRow[]>> {
  const key = cacheKeys.mealTypesList(userId);
  return readCached(userId, key, async () => {
    const { data, error } = await supabase!
      .from("meal_types")
      .select("*")
      .eq("user_id", userId)
      .order("position", { ascending: true });
    if (error) {
      throw error;
    }
    return (data ?? []) as MealTypeRow[];
  }, false);
}

export async function upsertMealType(
  userId: string,
  input: { id?: string; name: string; position: number },
): Promise<WriteOutcome<MealTypeRow>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    if (input.id) {
      const { data, error } = await supabase
        .from("meal_types")
        .update({ name: input.name, position: input.position })
        .eq("id", input.id)
        .eq("user_id", userId)
        .select()
        .single();
      if (error) {
        throw error;
      }
      cacheRemove(cacheKeys.mealTypesList(userId));
      return { kind: "ok", data: data as MealTypeRow };
    }
    const { data, error } = await supabase
      .from("meal_types")
      .insert({ user_id: userId, name: input.name, position: input.position })
      .select()
      .single();
    if (error) {
      throw error;
    }
    cacheRemove(cacheKeys.mealTypesList(userId));
    return { kind: "ok", data: data as MealTypeRow };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Meal type save failed" };
  }
}

export async function deleteMealType(userId: string, id: string): Promise<WriteOutcome<null>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { error } = await supabase.from("meal_types").delete().eq("id", id).eq("user_id", userId);
    if (error) {
      throw error;
    }
    cacheRemove(cacheKeys.mealTypesList(userId));
    return { kind: "ok", data: null };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Delete failed" };
  }
}

function sortFoods(rows: FoodRow[]): FoodRow[] {
  return [...rows].sort((a, b) => {
    if (a.is_favorite !== b.is_favorite) {
      return a.is_favorite ? -1 : 1;
    }
    const aUsed = a.last_used_at ?? "";
    const bUsed = b.last_used_at ?? "";
    if (aUsed !== bUsed) {
      return aUsed > bUsed ? -1 : 1;
    }
    return a.name.localeCompare(b.name, undefined, { sensitivity: "base" });
  });
}

export async function readFoods(userId: string): Promise<ReadOutcome<FoodRow[]>> {
  const key = cacheKeys.foodsList(userId);
  return readCached(
    userId,
    key,
    async () => {
      const { data, error } = await supabase!.from("foods").select("*").eq("user_id", userId);
      if (error) {
        throw error;
      }
      return sortFoods((data ?? []) as FoodRow[]);
    },
    false,
  );
}

export async function insertFood(
  userId: string,
  input: Omit<FoodRow, "id" | "user_id" | "created_at" | "updated_at" | "last_used_at">,
): Promise<WriteOutcome<FoodRow>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { data, error } = await supabase
      .from("foods")
      .insert({ ...input, user_id: userId })
      .select()
      .single();
    if (error) {
      throw error;
    }
    cacheRemove(cacheKeys.foodsList(userId));
    return { kind: "ok", data: data as FoodRow };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Insert failed" };
  }
}

export async function updateFood(userId: string, id: string, patch: Partial<FoodRow>): Promise<WriteOutcome<FoodRow>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { data, error } = await supabase
      .from("foods")
      .update(patch)
      .eq("id", id)
      .eq("user_id", userId)
      .select()
      .single();
    if (error) {
      throw error;
    }
    cacheRemove(cacheKeys.foodsList(userId));
    return { kind: "ok", data: data as FoodRow };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Update failed" };
  }
}

export async function deleteFood(userId: string, id: string): Promise<WriteOutcome<null>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { error } = await supabase.from("foods").delete().eq("id", id).eq("user_id", userId);
    if (error) {
      throw error;
    }
    cacheRemove(cacheKeys.foodsList(userId));
    return { kind: "ok", data: null };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Delete failed" };
  }
}

export async function readFoodServings(foodId: string): Promise<ReadOutcome<FoodServingRow[]>> {
  if (!isOnline() || !supabase) {
    return { kind: "empty", reason: !supabase ? "error" : "offline" };
  }
  try {
    const { data, error } = await supabase
      .from("food_servings")
      .select("*")
      .eq("food_id", foodId)
      .order("position", { ascending: true });
    if (error) {
      throw error;
    }
    return { kind: "fresh", data: (data ?? []) as FoodServingRow[] };
  } catch {
    return { kind: "empty", reason: "error" };
  }
}

export async function insertFoodServing(
  userId: string,
  input: { food_id: string; label: string; grams: number; position: number },
): Promise<WriteOutcome<FoodServingRow>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { data, error } = await supabase
      .from("food_servings")
      .insert({ ...input, user_id: userId })
      .select()
      .single();
    if (error) {
      throw error;
    }
    return { kind: "ok", data: data as FoodServingRow };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Insert failed" };
  }
}

export async function deleteFoodServing(userId: string, id: string): Promise<WriteOutcome<null>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { error } = await supabase.from("food_servings").delete().eq("id", id).eq("user_id", userId);
    if (error) {
      throw error;
    }
    return { kind: "ok", data: null };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Delete failed" };
  }
}

export async function readFoodEntriesDay(userId: string, eatenOn: string): Promise<ReadOutcome<FoodEntryRow[]>> {
  const key = cacheKeys.foodEntriesDay(userId, eatenOn);
  return readCached(userId, key, async () => {
    const { data, error } = await supabase!
      .from("food_entries")
      .select("*")
      .eq("user_id", userId)
      .eq("eaten_on", eatenOn)
      .order("created_at", { ascending: true });
    if (error) {
      throw error;
    }
    return (data ?? []) as FoodEntryRow[];
  }, false);
}

export async function addFoodEntry(
  userId: string,
  input: {
    foodId: string;
    mealTypeId: string;
    eatenOn: string;
    amount: number;
    unit: AmountUnit;
    foodServingId?: string | null;
  },
): Promise<WriteOutcome<FoodEntryRow>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { data: foodData, error: foodErr } = await supabase
      .from("foods")
      .select("*")
      .eq("id", input.foodId)
      .eq("user_id", userId)
      .maybeSingle();
    if (foodErr) {
      throw foodErr;
    }
    if (!foodData) {
      return { kind: "error", message: "Food not found" };
    }
    const food = foodData as FoodRow;
    let servingGrams: number | null = null;
    if (input.foodServingId) {
      const { data: servingData, error: servingErr } = await supabase
        .from("food_servings")
        .select("*")
        .eq("id", input.foodServingId)
        .maybeSingle();
      if (servingErr) {
        throw servingErr;
      }
      servingGrams = servingData ? Number((servingData as FoodServingRow).grams) : null;
    }
    const grams = resolveGrams(input.amount, input.unit, servingGrams, food.default_serving_g);
    const snap = per100gScaled(foodMacrosPer100(food), grams);
    const { data, error } = await supabase
      .from("food_entries")
      .insert({
        user_id: userId,
        food_id: input.foodId,
        meal_type_id: input.mealTypeId,
        eaten_on: input.eatenOn,
        amount: input.amount,
        unit: input.unit,
        food_serving_id: input.foodServingId ?? null,
        grams,
        protein_g: snap.proteinG,
        carbs_g: snap.carbsG,
        fat_g: snap.fatG,
        kcal: snap.kcal,
      })
      .select()
      .single();
    if (error) {
      throw error;
    }
    await supabase
      .from("foods")
      .update({ last_used_at: new Date().toISOString() })
      .eq("id", input.foodId);
    cacheRemove(cacheKeys.foodEntriesDay(userId, input.eatenOn));
    cacheRemove(cacheKeys.foodsList(userId));
    return { kind: "ok", data: data as FoodEntryRow };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Add entry failed" };
  }
}

export async function deleteFoodEntry(
  userId: string,
  eatenOn: string,
  id: string,
): Promise<WriteOutcome<null>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { error } = await supabase.from("food_entries").delete().eq("id", id).eq("user_id", userId);
    if (error) {
      throw error;
    }
    cacheRemove(cacheKeys.foodEntriesDay(userId, eatenOn));
    return { kind: "ok", data: null };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Delete failed" };
  }
}

export async function copyFoodEntriesDay(
  userId: string,
  fromDate: string,
  toDate: string,
): Promise<WriteOutcome<FoodEntryRow[]>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { data, error } = await supabase
      .from("food_entries")
      .select("*")
      .eq("user_id", userId)
      .eq("eaten_on", fromDate);
    if (error) {
      throw error;
    }
    const source = (data ?? []) as FoodEntryRow[];
    if (source.length === 0) {
      return { kind: "ok", data: [] };
    }
    const inserts = source.map(({ id: _id, created_at: _c, updated_at: _u, ...rest }) => ({
      ...rest,
      eaten_on: toDate,
    }));
    const { data: copied, error: insErr } = await supabase.from("food_entries").insert(inserts).select();
    if (insErr) {
      throw insErr;
    }
    cacheRemove(cacheKeys.foodEntriesDay(userId, toDate));
    return { kind: "ok", data: (copied ?? []) as FoodEntryRow[] };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Copy day failed" };
  }
}

export async function copyFoodEntriesMeal(
  userId: string,
  fromDate: string,
  toDate: string,
  mealTypeId: string,
  targetMealTypeId: string = mealTypeId,
): Promise<WriteOutcome<FoodEntryRow[]>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { data, error } = await supabase
      .from("food_entries")
      .select("*")
      .eq("user_id", userId)
      .eq("eaten_on", fromDate)
      .eq("meal_type_id", mealTypeId);
    if (error) {
      throw error;
    }
    const source = (data ?? []) as FoodEntryRow[];
    if (source.length === 0) {
      return { kind: "ok", data: [] };
    }
    const inserts = source.map(({ id: _id, created_at: _c, updated_at: _u, ...rest }) => ({
      ...rest,
      eaten_on: toDate,
      meal_type_id: targetMealTypeId,
    }));
    const { data: copied, error: insErr } = await supabase.from("food_entries").insert(inserts).select();
    if (insErr) {
      throw insErr;
    }
    cacheRemove(cacheKeys.foodEntriesDay(userId, toDate));
    return { kind: "ok", data: (copied ?? []) as FoodEntryRow[] };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Copy meal failed" };
  }
}

export function entryRowMacros(row: FoodEntryRow): Macros {
  return macros(Number(row.protein_g), Number(row.carbs_g), Number(row.fat_g), Number(row.kcal));
}

export function sumEntryMacros(rows: FoodEntryRow[]): Macros {
  return rows.reduce((acc, r) => addMacros(acc, entryRowMacros(r)), macros(0, 0, 0, 0));
}

export async function readRecipeIngredients(
  recipeFoodId: string,
): Promise<ReadOutcome<RecipeIngredientRow[]>> {
  if (!isOnline() || !supabase) {
    return { kind: "empty", reason: !supabase ? "error" : "offline" };
  }
  try {
    const { data, error } = await supabase
      .from("recipe_ingredients")
      .select("*")
      .eq("recipe_food_id", recipeFoodId);
    if (error) {
      throw error;
    }
    return { kind: "fresh", data: (data ?? []) as RecipeIngredientRow[] };
  } catch {
    return { kind: "empty", reason: "error" };
  }
}

export async function saveRecipe(
  userId: string,
  recipe: {
    id?: string;
    name: string;
    defaultServingG?: number | null;
  },
  ingredients: { componentFoodId: string; grams: number }[],
): Promise<WriteOutcome<FoodRow>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    let totalMacros = macros(0, 0, 0, 0);
    let totalGrams = 0;
    for (const ing of ingredients) {
      const { data: comp, error: compErr } = await supabase
        .from("foods")
        .select("*")
        .eq("id", ing.componentFoodId)
        .eq("user_id", userId)
        .maybeSingle();
      if (compErr) {
        throw compErr;
      }
      if (!comp) {
        return { kind: "error", message: `Component not found: ${ing.componentFoodId}` };
      }
      totalMacros = addMacros(totalMacros, per100gScaled(foodMacrosPer100(comp as FoodRow), ing.grams));
      totalGrams += ing.grams;
    }
    const per100 = recipeMacrosPer100g(totalMacros, totalGrams);
    const defaultServingG = recipe.defaultServingG ?? (totalGrams > 0 ? totalGrams : null);
    let saved: FoodRow;
    if (recipe.id) {
      const { data, error } = await supabase
        .from("foods")
        .update({
          name: recipe.name,
          protein_g: per100.proteinG,
          carbs_g: per100.carbsG,
          fat_g: per100.fatG,
          default_serving_g: defaultServingG,
          is_recipe: true,
        })
        .eq("id", recipe.id)
        .eq("user_id", userId)
        .select()
        .single();
      if (error) {
        throw error;
      }
      saved = data as FoodRow;
      await supabase.from("recipe_ingredients").delete().eq("recipe_food_id", recipe.id);
    } else {
      const { data, error } = await supabase
        .from("foods")
        .insert({
          user_id: userId,
          name: recipe.name,
          protein_g: per100.proteinG,
          carbs_g: per100.carbsG,
          fat_g: per100.fatG,
          default_serving_g: defaultServingG,
          is_recipe: true,
          is_favorite: false,
        })
        .select()
        .single();
      if (error) {
        throw error;
      }
      saved = data as FoodRow;
    }
    if (ingredients.length > 0) {
      const rows = ingredients.map((ing) => ({
        user_id: userId,
        recipe_food_id: saved.id,
        component_food_id: ing.componentFoodId,
        grams: ing.grams,
      }));
      const { error: riErr } = await supabase.from("recipe_ingredients").insert(rows);
      if (riErr) {
        throw riErr;
      }
    }
    cacheRemove(cacheKeys.foodsList(userId));
    return { kind: "ok", data: saved };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Save recipe failed" };
  }
}

export async function readWaterDay(userId: string, drunkOn: string): Promise<ReadOutcome<WaterEntryRow[]>> {
  const key = cacheKeys.waterDay(userId, drunkOn);
  return readCached(userId, key, async () => {
    const { data, error } = await supabase!
      .from("water_entries")
      .select("*")
      .eq("user_id", userId)
      .eq("drunk_on", drunkOn)
      .order("created_at", { ascending: true });
    if (error) {
      throw error;
    }
    return (data ?? []) as WaterEntryRow[];
  }, false);
}

export async function addWaterEntry(
  userId: string,
  drunkOn: string,
  amountMl: number,
): Promise<WriteOutcome<WaterEntryRow>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  if (!(amountMl > 0)) {
    return { kind: "error", message: "Amount must be greater than 0" };
  }
  try {
    const { data, error } = await supabase
      .from("water_entries")
      .insert({ user_id: userId, drunk_on: drunkOn, amount_ml: amountMl })
      .select()
      .single();
    if (error) {
      throw error;
    }
    cacheRemove(cacheKeys.waterDay(userId, drunkOn));
    return { kind: "ok", data: data as WaterEntryRow };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Add water failed" };
  }
}

export async function deleteWaterEntry(
  userId: string,
  drunkOn: string,
  id: string,
): Promise<WriteOutcome<null>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { error } = await supabase.from("water_entries").delete().eq("id", id).eq("user_id", userId);
    if (error) {
      throw error;
    }
    cacheRemove(cacheKeys.waterDay(userId, drunkOn));
    return { kind: "ok", data: null };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Delete failed" };
  }
}
