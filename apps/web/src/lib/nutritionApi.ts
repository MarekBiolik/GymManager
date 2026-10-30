/**
 * Thin supabase-js glue for nutrition_settings, meal_types, foods + servings (ADR 0006).
 */
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

function isOnline(): boolean {
  return typeof navigator === "undefined" ? true : navigator.onLine;
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
