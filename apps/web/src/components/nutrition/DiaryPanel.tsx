import { useCallback, useEffect, useMemo, useState } from "react";
import { addDays, formatIsoDate, shortDayMonth, shortWeekday, weekStrip } from "../../lib/dateUtils";
import {
  addFoodEntry,
  addWaterEntry,
  copyFoodEntriesDay,
  copyFoodEntriesMeal,
  deleteFoodEntry,
  deleteWaterEntry,
  insertFood,
  readFoodEntriesDay,
  readFoods,
  readMealTypes,
  readWaterDay,
  readNutritionSettings,
  sumEntryMacros,
  type FoodEntryRow,
  type FoodRow,
  type MealTypeRow,
  type NutritionSettingsRow,
  type WaterEntryRow,
} from "../../lib/nutritionApi";
import {
  fetchOpenFoodFactsBarcode,
  offToFoodInsert,
  searchOpenFoodFacts,
  type OffSearchHit,
} from "../../lib/offClient";
import { GoalProgressBars } from "./GoalProgressBars";
import { StaleBanner } from "./StaleBanner";

type Props = { userId: string };

const DEFAULT_GOALS = { calorieTargetKcal: 2000, proteinTargetG: 150, carbsTargetG: 200, fatTargetG: 65 };

export function DiaryPanel({ userId }: Props) {
  const [selectedDay, setSelectedDay] = useState(() => formatIsoDate(new Date()));
  const [settings, setSettings] = useState<NutritionSettingsRow | null>(null);
  const [mealTypes, setMealTypes] = useState<MealTypeRow[]>([]);
  const [entries, setEntries] = useState<FoodEntryRow[]>([]);
  const [foods, setFoods] = useState<FoodRow[]>([]);
  const [banner, setBanner] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [waterEntries, setWaterEntries] = useState<WaterEntryRow[]>([]);
  const [waterAdd, setWaterAdd] = useState("250");
  const [copyFrom, setCopyFrom] = useState("");

  const [pickedMealId, setPickedMealId] = useState("");
  const [pickedFoodId, setPickedFoodId] = useState("");
  const [addAmount, setAddAmount] = useState("100");
  const [addUnit, setAddUnit] = useState<"g" | "serving">("g");

  const [offQuery, setOffQuery] = useState("");
  const [offHits, setOffHits] = useState<OffSearchHit[]>([]);
  const [barcode, setBarcode] = useState("");

  const loadDay = useCallback(async () => {
    setError(null);
    setBanner(null);
    const [s, m, e, f, w] = await Promise.all([
      readNutritionSettings(userId),
      readMealTypes(userId),
      readFoodEntriesDay(userId, selectedDay),
      readFoods(userId),
      readWaterDay(userId, selectedDay),
    ]);
    if (s.kind === "fresh") {
      setSettings(s.data);
    } else if (s.kind === "stale") {
      setSettings(s.data);
      setBanner("Goals loaded from cache (offline or error).");
    } else if (s.kind === "empty" && s.reason !== "not_found") {
      setError("Could not load nutrition settings.");
    }
    if (m.kind === "fresh" || m.kind === "stale") {
      setMealTypes(m.data);
    }
    if (e.kind === "fresh") {
      setEntries(e.data);
    } else if (e.kind === "stale") {
      setEntries(e.data);
      setBanner((b) => b ?? "Diary loaded from cache.");
    } else {
      setEntries([]);
    }
    if (f.kind === "fresh" || f.kind === "stale") {
      setFoods(f.data);
    }
    if (w.kind === "fresh" || w.kind === "stale") {
      setWaterEntries(w.data);
    } else {
      setWaterEntries([]);
    }
  }, [userId, selectedDay]);

  useEffect(() => {
    void loadDay();
  }, [loadDay]);

  const consumed = useMemo(() => sumEntryMacros(entries), [entries]);
  const waterMl = useMemo(
    () => waterEntries.reduce((acc, row) => acc + Number(row.amount_ml), 0),
    [waterEntries],
  );
  const waterTargetMl = settings?.water_target_ml != null ? Number(settings.water_target_ml) : null;
  const goals = settings
    ? {
        calorieTargetKcal: Number(settings.calorie_target_kcal),
        proteinTargetG: Number(settings.protein_target_g),
        carbsTargetG: Number(settings.carbs_target_g),
        fatTargetG: Number(settings.fat_target_g),
      }
    : DEFAULT_GOALS;

  const foodNameById = useMemo(() => new Map(foods.map((f) => [f.id, f.name])), [foods]);
  const strip = weekStrip(selectedDay);

  // Fall back to the first option until the user picks one.
  const addMealId = mealTypes.some((m) => m.id === pickedMealId) ? pickedMealId : (mealTypes[0]?.id ?? "");
  const addFoodId = foods.some((f) => f.id === pickedFoodId) ? pickedFoodId : (foods[0]?.id ?? "");

  async function onAddEntry() {
    setError(null);
    const amount = Number(addAmount);
    if (!addFoodId || !addMealId || !Number.isFinite(amount) || amount <= 0) {
      setError("Pick food, meal, and a positive amount.");
      return;
    }
    setBusy(true);
    const result = await addFoodEntry(userId, {
      foodId: addFoodId,
      mealTypeId: addMealId,
      eatenOn: selectedDay,
      amount,
      unit: addUnit,
    });
    setBusy(false);
    if (result.kind === "offline") {
      setError("Offline — connect to add entries.");
      return;
    }
    if (result.kind === "error") {
      setError(result.message);
      return;
    }
    void loadDay();
  }

  async function onDeleteEntry(id: string) {
    setError(null);
    setBusy(true);
    const result = await deleteFoodEntry(userId, selectedDay, id);
    setBusy(false);
    if (result.kind === "offline") {
      setError("Offline — connect to remove entries.");
      return;
    }
    if (result.kind === "error") {
      setError(result.message);
      return;
    }
    void loadDay();
  }

  async function onCopyDay() {
    setError(null);
    if (!copyFrom) {
      setError("Pick a source day to copy from.");
      return;
    }
    if (copyFrom === selectedDay) {
      setError("Source day must differ from the selected day.");
      return;
    }
    setBusy(true);
    const result = await copyFoodEntriesDay(userId, copyFrom, selectedDay);
    setBusy(false);
    if (result.kind === "offline") {
      setError("Offline — cannot copy day.");
      return;
    }
    if (result.kind === "error") {
      setError(result.message);
      return;
    }
    if (result.data.length === 0) {
      setError("No entries on the source day.");
    }
    void loadDay();
  }

  async function onCopyMeal(mealTypeId: string) {
    setError(null);
    const from = addDays(selectedDay, -1);
    setBusy(true);
    const result = await copyFoodEntriesMeal(userId, from, selectedDay, mealTypeId);
    setBusy(false);
    if (result.kind === "offline") {
      setError("Offline — cannot copy meal.");
      return;
    }
    if (result.kind === "error") {
      setError(result.message);
      return;
    }
    if (result.data.length === 0) {
      setError("No entries for this meal yesterday.");
    }
    void loadDay();
  }

  async function onAddWater() {
    setError(null);
    const ml = Number(waterAdd);
    if (!Number.isFinite(ml) || ml <= 0) {
      setError("Enter a positive amount in ml.");
      return;
    }
    setBusy(true);
    const result = await addWaterEntry(userId, selectedDay, ml);
    setBusy(false);
    if (result.kind === "offline") {
      setError("Offline — cannot log water.");
      return;
    }
    if (result.kind === "error") {
      setError(result.message);
      return;
    }
    void loadDay();
  }

  async function onDeleteWater(id: string) {
    setError(null);
    setBusy(true);
    const result = await deleteWaterEntry(userId, selectedDay, id);
    setBusy(false);
    if (result.kind === "offline") {
      setError("Offline — connect to remove water.");
      return;
    }
    if (result.kind === "error") {
      setError(result.message);
      return;
    }
    void loadDay();
  }

  async function onOffSearch() {
    setError(null);
    setOffHits([]);
    if (!offQuery.trim()) {
      return;
    }
    setBusy(true);
    try {
      const hits = await searchOpenFoodFacts(offQuery);
      setOffHits(hits);
      if (hits.length === 0) {
        setError("No Open Food Facts results.");
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "OFF search failed");
    } finally {
      setBusy(false);
    }
  }

  async function importOffHit(hit: OffSearchHit) {
    setBusy(true);
    const ins = await insertFood(userId, offToFoodInsert(userId, hit));
    setBusy(false);
    if (ins.kind === "offline") {
      setError("Offline — cannot import.");
      return;
    }
    if (ins.kind === "error") {
      setError(ins.message);
      return;
    }
    setPickedFoodId(ins.data.id);
    setOffHits([]);
    void loadDay();
  }

  async function onBarcodeLookup() {
    setError(null);
    setBusy(true);
    try {
      const product = await fetchOpenFoodFactsBarcode(barcode);
      if (!product) {
        setError("Product not found for that barcode.");
        setOffHits([]);
      } else {
        setOffHits([product]);
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Barcode lookup failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="nutrition-panel" aria-label="Food diary">
      <h2 className="nutrition-panel-title">Diary</h2>
      {banner ? <StaleBanner label={banner} /> : null}
      {error ? (
        <p className="auth-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="nutrition-week">
        {strip.map((iso) => (
          <button
            key={iso}
            type="button"
            className={iso === selectedDay ? "nutrition-day is-selected" : "nutrition-day"}
            aria-pressed={iso === selectedDay}
            onClick={() => setSelectedDay(iso)}
          >
            <span className="nutrition-day-wd">{shortWeekday(iso)}</span>
            <span>{shortDayMonth(iso)}</span>
          </button>
        ))}
        <button
          type="button"
          className="auth-button auth-button--secondary"
          onClick={() => setSelectedDay(formatIsoDate(new Date()))}
        >
          Today
        </button>
      </div>

      <GoalProgressBars consumed={consumed} goals={goals} waterMl={waterMl} waterTargetMl={waterTargetMl} />

      {mealTypes.map((meal) => {
        const mealEntries = entries.filter((e) => e.meal_type_id === meal.id);
        return (
          <div key={meal.id} className="nutrition-meal">
            <div className="nutrition-list-row">
              <h3 className="nutrition-meal-title">{meal.name}</h3>
              <button
                type="button"
                className="auth-button auth-button--secondary nutrition-btn-sm"
                disabled={busy}
                onClick={() => void onCopyMeal(meal.id)}
              >
                Copy from yesterday
              </button>
            </div>
            {mealEntries.length === 0 ? (
              <p className="app-sub">No entries</p>
            ) : (
              <ul className="nutrition-list">
                {mealEntries.map((entry) => (
                  <li key={entry.id} className="nutrition-list-row">
                    <span>
                      {entry.food_id ? (foodNameById.get(entry.food_id) ?? "Food") : "Quick entry"} —{" "}
                      {Number(entry.grams).toFixed(0)}g · {Math.round(Number(entry.kcal))} kcal
                    </span>
                    <button
                      type="button"
                      className="auth-button auth-button--secondary nutrition-btn-sm"
                      disabled={busy}
                      onClick={() => void onDeleteEntry(entry.id)}
                    >
                      Remove
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        );
      })}

      <div className="nutrition-card">
        <h3 className="nutrition-meal-title">Add entry</h3>
        {foods.length === 0 ? (
          <p className="app-sub">Add a food on the Foods tab first.</p>
        ) : (
          <>
            <div className="nutrition-form-grid">
              <label className="hello-sync-label">
                Meal
                <select
                  className="hello-sync-input"
                  value={addMealId}
                  onChange={(e) => setPickedMealId(e.target.value)}
                >
                  {mealTypes.map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="hello-sync-label">
                Food
                <select
                  className="hello-sync-input"
                  value={addFoodId}
                  onChange={(e) => setPickedFoodId(e.target.value)}
                >
                  {foods.map((f) => (
                    <option key={f.id} value={f.id}>
                      {f.name}
                      {f.is_recipe ? " (recipe)" : ""}
                    </option>
                  ))}
                </select>
              </label>
              <label className="hello-sync-label">
                Amount
                <input
                  className="hello-sync-input"
                  value={addAmount}
                  onChange={(e) => setAddAmount(e.target.value)}
                  inputMode="decimal"
                />
              </label>
              <label className="hello-sync-label">
                Unit
                <select
                  className="hello-sync-input"
                  value={addUnit}
                  onChange={(e) => setAddUnit(e.target.value as "g" | "serving")}
                >
                  <option value="g">g</option>
                  <option value="serving">serving</option>
                </select>
              </label>
            </div>
            <button
              type="button"
              className="auth-button auth-button--primary"
              disabled={busy}
              onClick={() => void onAddEntry()}
            >
              Add to diary
            </button>
          </>
        )}
      </div>

      <div className="nutrition-card">
        <h3 className="nutrition-meal-title">Water</h3>
        <p className="app-sub">
          Logged: {Math.round(waterMl)} ml
          {waterTargetMl != null && waterTargetMl > 0 ? ` of ${Math.round(waterTargetMl)} ml` : ""}
        </p>
        {waterEntries.length > 0 ? (
          <ul className="nutrition-list">
            {waterEntries.map((w) => (
              <li key={w.id} className="nutrition-list-row">
                <span>{Math.round(Number(w.amount_ml))} ml</span>
                <button
                  type="button"
                  className="auth-button auth-button--secondary nutrition-btn-sm"
                  disabled={busy}
                  onClick={() => void onDeleteWater(w.id)}
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
        ) : null}
        <div className="hello-sync-form">
          <input
            className="hello-sync-input"
            value={waterAdd}
            onChange={(e) => setWaterAdd(e.target.value)}
            inputMode="numeric"
            aria-label="Water amount in ml"
          />
          <button
            type="button"
            className="auth-button auth-button--primary"
            disabled={busy}
            onClick={() => void onAddWater()}
          >
            Add ml
          </button>
        </div>
      </div>

      <div className="nutrition-card">
        <h3 className="nutrition-meal-title">Copy day</h3>
        <div className="hello-sync-form">
          <input
            className="hello-sync-input"
            type="date"
            value={copyFrom}
            onChange={(e) => setCopyFrom(e.target.value)}
            aria-label="Copy from date"
          />
          <button
            type="button"
            className="auth-button auth-button--secondary"
            disabled={busy}
            onClick={() => void onCopyDay()}
          >
            Copy to {selectedDay}
          </button>
        </div>
      </div>

      <div className="nutrition-card">
        <h3 className="nutrition-meal-title">Open Food Facts search</h3>
        <p className="auth-hint">Data from Open Food Facts — saved as your private food copy.</p>
        <div className="hello-sync-form">
          <input
            className="hello-sync-input"
            placeholder="Search products…"
            value={offQuery}
            onChange={(e) => setOffQuery(e.target.value)}
          />
          <button
            type="button"
            className="auth-button auth-button--secondary"
            disabled={busy}
            onClick={() => void onOffSearch()}
          >
            Search OFF
          </button>
        </div>
        {offHits.length > 0 ? (
          <ul className="nutrition-list">
            {offHits.map((hit) => (
              <li key={hit.code} className="nutrition-list-row">
                <span>
                  {hit.name} · P {hit.proteinGPer100.toFixed(1)} / C {hit.carbsGPer100.toFixed(1)} / F{" "}
                  {hit.fatGPer100.toFixed(1)} per 100g
                </span>
                <button
                  type="button"
                  className="auth-button auth-button--primary nutrition-btn-sm"
                  disabled={busy}
                  onClick={() => void importOffHit(hit)}
                >
                  Import
                </button>
              </li>
            ))}
          </ul>
        ) : null}
        <label className="hello-sync-label">
          Barcode (paste)
          <input
            className="hello-sync-input"
            value={barcode}
            onChange={(e) => setBarcode(e.target.value)}
            placeholder="EAN / UPC"
          />
        </label>
        <button
          type="button"
          className="auth-button auth-button--secondary"
          disabled={busy}
          onClick={() => void onBarcodeLookup()}
        >
          Lookup barcode
        </button>
      </div>
    </section>
  );
}
