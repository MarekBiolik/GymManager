import { useCallback, useEffect, useMemo, useState } from "react";
import { addDays, formatIsoDate, shortDayMonth, shortWeekday, weekStrip } from "../../lib/dateUtils";
import {
  addFoodEntry,
  addWaterEntry,
  copyFoodEntriesDay,
  copyFoodEntriesMeal,
  deleteFoodEntry,
  insertFood,
  readFoodEntriesDay,
  readFoods,
  readMealTypes,
  readNutritionSettings,
  readWaterDay,
  sumEntryMacros,
  type FoodEntryRow,
  type FoodRow,
  type MealTypeRow,
  type NutritionSettingsRow,
} from "../../lib/nutritionApi";
import { fetchOpenFoodFactsBarcode, offToFoodInsert, searchOpenFoodFacts, type OffSearchHit } from "../../lib/offClient";
import { GoalProgressBars } from "./GoalProgressBars";
import { StaleBanner } from "./StaleBanner";

type Props = { userId: string };

export function DiaryPanel({ userId }: Props) {
  const [selectedDay, setSelectedDay] = useState(() => formatIsoDate(new Date()));
  const [settings, setSettings] = useState<NutritionSettingsRow | null>(null);
  const [mealTypes, setMealTypes] = useState<MealTypeRow[]>([]);
  const [entries, setEntries] = useState<FoodEntryRow[]>([]);
  const [foods, setFoods] = useState<FoodRow[]>([]);
  const [waterMl, setWaterMl] = useState(0);
  const [banner, setBanner] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [addMealId, setAddMealId] = useState("");
  const [addFoodId, setAddFoodId] = useState("");
  const [addAmount, setAddAmount] = useState("100");
  const [addUnit, setAddUnit] = useState<"g" | "serving">("g");

  const [offQuery, setOffQuery] = useState("");
  const [offHits, setOffHits] = useState<OffSearchHit[]>([]);
  const [barcode, setBarcode] = useState("");

  const [copyFrom, setCopyFrom] = useState("");
  const [waterAdd, setWaterAdd] = useState("250");

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
      if (!addMealId && m.data[0]) {
        setAddMealId(m.data[0].id);
      }
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
      if (!addFoodId && f.data[0]) {
        setAddFoodId(f.data[0].id);
      }
    }
    if (w.kind === "fresh" || w.kind === "stale") {
      setWaterMl(w.data.reduce((acc, row) => acc + Number(row.amount_ml), 0));
    } else {
      setWaterMl(0);
    }
  }, [userId, selectedDay, addMealId, addFoodId]);

  useEffect(() => {
    void loadDay();
  }, [loadDay]);

  const consumed = useMemo(() => sumEntryMacros(entries), [entries]);
  const goals = settings
    ? {
        calorieTargetKcal: Number(settings.calorie_target_kcal),
        proteinTargetG: Number(settings.protein_target_g),
        carbsTargetG: Number(settings.carbs_target_g),
        fatTargetG: Number(settings.fat_target_g),
      }
    : { calorieTargetKcal: 2000, proteinTargetG: 150, carbsTargetG: 200, fatTargetG: 65 };

  const foodNameById = useMemo(() => new Map(foods.map((f) => [f.id, f.name])), [foods]);
  const strip = weekStrip(selectedDay);

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
        setError("No OpenFoodFacts results.");
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
    setAddFoodId(ins.data.id);
    setOffHits([]);
    void loadDay();
  }

  async function onBarcodeLookup() {
    setError(null);
    setBusy(true);
    try {
      const product = await fetchOpenFoodFactsBarcode(barcode);
      if (!product) {
        setError("Product not found for barcode.");
        return;
      }
      await importOffHit(product);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Barcode lookup failed");
    } finally {
      setBusy(false);
    }
  }

  async function onCopyDay() {
    if (!copyFrom) {
      setError("Pick a source day to copy from.");
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
    void loadDay();
  }

  async function onAddWater() {
    const ml = Number(waterAdd);
    if (!Number.isFinite(ml) || ml <= 0) {
      return;
    }
    setBusy(true);
    const result = await addWaterEntry(userId, selectedDay, ml);
    setBusy(false);
    if (result.kind === "ok") {
      void loadDay();
    } else if (result.kind === "offline") {
      setError("Offline — cannot log water.");
    } else if (result.kind === "error") {
      setError(result.message);
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
            onClick={() => setSelectedDay(iso)}
          >
            <span className="nutrition-day-wd">{shortWeekday(iso)}</span>
            <span>{shortDayMonth(iso)}</span>
          </button>
        ))}
        <button type="button" className="auth-button auth-button--secondary" onClick={() => setSelectedDay(formatIsoDate(new Date()))}>
          Today
        </button>
      </div>

      <GoalProgressBars
        consumed={consumed}
        goals={goals}
        waterMl={waterMl}
        waterTargetMl={settings?.water_target_ml != null ? Number(settings.water_target_ml) : null}
      />

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
                onClick={() => {
                  const from = addDays(selectedDay, -1);
                  void copyFoodEntriesMeal(userId, from, selectedDay, meal.id).then((result) => {
                    if (result.kind === "ok") {
                      void loadDay();
                    } else if (result.kind === "offline") {
                      setError("Offline — cannot copy meal.");
                    } else if (result.kind === "error") {
                      setError(result.message);
                    }
                  });
                }}
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
                      {entry.food_id ? foodNameById.get(entry.food_id) ?? "Food" : "Quick entry"} — {Number(entry.grams).toFixed(0)}g ·{" "}
                      {Math.round(Number(entry.kcal))} kcal
                    </span>
                    <button
                      type="button"
                      className="auth-button auth-button--secondary nutrition-btn-sm"
                      disabled={busy}
                      onClick={() => void deleteFoodEntry(userId, selectedDay, entry.id).then(() => loadDay())}
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
        <div className="nutrition-form-grid">
          <label className="hello-sync-label">
            Meal
            <select className="hello-sync-input" value={addMealId} onChange={(e) => setAddMealId(e.target.value)}>
              {mealTypes.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name}
                </option>
              ))}
            </select>
          </label>
          <label className="hello-sync-label">
            Food
            <select className="hello-sync-input" value={addFoodId} onChange={(e) => setAddFoodId(e.target.value)}>
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
            <input className="hello-sync-input" value={addAmount} onChange={(e) => setAddAmount(e.target.value)} inputMode="decimal" />
          </label>
          <label className="hello-sync-label">
            Unit
            <select className="hello-sync-input" value={addUnit} onChange={(e) => setAddUnit(e.target.value as "g" | "serving")}>
              <option value="g">g</option>
              <option value="serving">serving</option>
            </select>
          </label>
        </div>
        <button type="button" className="auth-button auth-button--primary" disabled={busy} onClick={() => void onAddEntry()}>
          Add to diary
        </button>
      </div>

      <div className="nutrition-card">
        <h3 className="nutrition-meal-title">OpenFoodFacts search</h3>
        <p className="auth-hint">Data from OpenFoodFacts — saved as your private food copy.</p>
        <div className="hello-sync-form">
          <input
            className="hello-sync-input"
            placeholder="Search products…"
            value={offQuery}
            onChange={(e) => setOffQuery(e.target.value)}
          />
          <button type="button" className="auth-button auth-button--secondary" disabled={busy} onClick={() => void onOffSearch()}>
            Search OFF
          </button>
        </div>
        {offHits.length > 0 ? (
          <ul className="nutrition-list">
            {offHits.map((hit) => (
              <li key={hit.code} className="nutrition-list-row">
                <span>
                  {hit.name} · P {hit.proteinGPer100.toFixed(1)} / C {hit.carbsGPer100.toFixed(1)} / F {hit.fatGPer100.toFixed(1)} per 100g
                </span>
                <button type="button" className="auth-button auth-button--primary nutrition-btn-sm" disabled={busy} onClick={() => void importOffHit(hit)}>
                  Import
                </button>
              </li>
            ))}
          </ul>
        ) : null}
        <label className="hello-sync-label">
          Barcode (paste)
          <input className="hello-sync-input" value={barcode} onChange={(e) => setBarcode(e.target.value)} placeholder="EAN / UPC" />
        </label>
        <button type="button" className="auth-button auth-button--secondary" disabled={busy} onClick={() => void onBarcodeLookup()}>
          Lookup barcode
        </button>
      </div>

      <div className="nutrition-card">
        <h3 className="nutrition-meal-title">Water</h3>
        <p className="app-sub">Logged today: {Math.round(waterMl)} ml</p>
        <div className="hello-sync-form">
          <input className="hello-sync-input" value={waterAdd} onChange={(e) => setWaterAdd(e.target.value)} inputMode="numeric" />
          <button type="button" className="auth-button auth-button--primary" disabled={busy} onClick={() => void onAddWater()}>
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
          <button type="button" className="auth-button auth-button--secondary" disabled={busy} onClick={() => void onCopyDay()}>
            Copy to {selectedDay}
          </button>
        </div>
        <button
          type="button"
          className="auth-button auth-button--secondary"
          onClick={() => setSelectedDay(addDays(selectedDay, -1))}
        >
          Previous day
        </button>
      </div>
    </section>
  );
}
