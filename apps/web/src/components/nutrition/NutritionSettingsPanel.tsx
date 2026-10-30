import { useCallback, useEffect, useState } from "react";
import { AccountSettingsPanel } from "../AccountSettingsPanel";
import { insertMeasurement, readMeasurements } from "../../lib/helloSync";
import {
  deleteMealType,
  readMealTypes,
  readNutritionSettings,
  updateNutritionSettings,
  upsertMealType,
  type MealTypeRow,
  type NutritionSettingsRow,
} from "../../lib/nutritionApi";
import { StaleBanner } from "./StaleBanner";

type Props = {
  userId: string;
  onAccountDeleted: () => void;
};

export function NutritionSettingsPanel({ userId, onAccountDeleted }: Props) {
  const [settings, setSettings] = useState<NutritionSettingsRow | null>(null);
  const [mealTypes, setMealTypes] = useState<MealTypeRow[]>([]);
  const [banner, setBanner] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [calories, setCalories] = useState("2000");
  const [protein, setProtein] = useState("150");
  const [carbs, setCarbs] = useState("200");
  const [fat, setFat] = useState("65");
  const [height, setHeight] = useState("");
  const [waterTarget, setWaterTarget] = useState("");

  const [newMealName, setNewMealName] = useState("");
  const [weightValue, setWeightValue] = useState("");
  const [weightDate, setWeightDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [recentWeights, setRecentWeights] = useState<{ at: string; value: number }[]>([]);

  const load = useCallback(async () => {
    setError(null);
    setBanner(null);
    const [s, m, meas] = await Promise.all([readNutritionSettings(userId), readMealTypes(userId), readMeasurements(userId)]);
    if (s.kind === "fresh" || s.kind === "stale") {
      setSettings(s.data);
      setCalories(String(s.data.calorie_target_kcal));
      setProtein(String(s.data.protein_target_g));
      setCarbs(String(s.data.carbs_target_g));
      setFat(String(s.data.fat_target_g));
      setHeight(s.data.height_cm != null ? String(s.data.height_cm) : "");
      setWaterTarget(s.data.water_target_ml != null ? String(s.data.water_target_ml) : "");
      if (s.kind === "stale") {
        setBanner("Settings from cache.");
      }
    }
    if (m.kind === "fresh" || m.kind === "stale") {
      setMealTypes(m.data);
    }
    if (meas.kind === "fresh" || meas.kind === "stale") {
      setRecentWeights(
        meas.data
          .filter((row) => row.metric === "weight" || row.metric === "body_weight")
          .slice(0, 8)
          .map((row) => ({ at: row.measured_at.slice(0, 10), value: row.value })),
      );
    }
  }, [userId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function onSaveGoals() {
    setBusy(true);
    const result = await updateNutritionSettings(userId, {
      calorie_target_kcal: Number(calories),
      protein_target_g: Number(protein),
      carbs_target_g: Number(carbs),
      fat_target_g: Number(fat),
      height_cm: height.trim() ? Number(height) : null,
      water_target_ml: waterTarget.trim() ? Number(waterTarget) : null,
    });
    setBusy(false);
    if (result.kind === "ok") {
      setSettings(result.data);
    } else if (result.kind === "offline") {
      setError("Offline — cannot save settings.");
    } else if (result.kind === "error") {
      setError(result.message);
    }
  }

  async function onAddMealType() {
    if (!newMealName.trim()) {
      return;
    }
    setBusy(true);
    const result = await upsertMealType(userId, { name: newMealName.trim(), position: mealTypes.length });
    setBusy(false);
    if (result.kind === "ok") {
      setNewMealName("");
      void load();
    } else if (result.kind === "error") {
      setError(result.message);
    }
  }

  async function onRenameMeal(mt: MealTypeRow, name: string) {
    setBusy(true);
    await upsertMealType(userId, { id: mt.id, name, position: mt.position });
    setBusy(false);
    void load();
  }

  async function onLogWeight() {
    const v = Number(weightValue);
    if (!Number.isFinite(v) || v <= 0) {
      setError("Enter a valid weight.");
      return;
    }
    setBusy(true);
    const result = await insertMeasurement(userId, {
      metric: "weight",
      value: v,
      measuredAtIso: new Date(`${weightDate}T12:00:00`).toISOString(),
    });
    setBusy(false);
    if (result.kind === "ok") {
      setWeightValue("");
      void load();
    } else if (result.kind === "offline") {
      setError("Offline — cannot log weight.");
    } else if (result.kind === "error") {
      setError(result.message);
    }
  }

  return (
    <section className="nutrition-panel" aria-label="Nutrition settings">
      <h2 className="nutrition-panel-title">Settings</h2>
      {banner ? <StaleBanner label={banner} /> : null}
      {error ? (
        <p className="auth-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="nutrition-card">
        <h3 className="nutrition-meal-title">Goals</h3>
        <div className="nutrition-form-grid">
          <label className="hello-sync-label">
            Calorie target
            <input className="hello-sync-input" value={calories} onChange={(e) => setCalories(e.target.value)} inputMode="numeric" />
          </label>
          <label className="hello-sync-label">
            Protein (g)
            <input className="hello-sync-input" value={protein} onChange={(e) => setProtein(e.target.value)} inputMode="decimal" />
          </label>
          <label className="hello-sync-label">
            Carbs (g)
            <input className="hello-sync-input" value={carbs} onChange={(e) => setCarbs(e.target.value)} inputMode="decimal" />
          </label>
          <label className="hello-sync-label">
            Fat (g)
            <input className="hello-sync-input" value={fat} onChange={(e) => setFat(e.target.value)} inputMode="decimal" />
          </label>
          <label className="hello-sync-label">
            Height (cm)
            <input className="hello-sync-input" value={height} onChange={(e) => setHeight(e.target.value)} inputMode="decimal" />
          </label>
          <label className="hello-sync-label">
            Water target (ml)
            <input className="hello-sync-input" value={waterTarget} onChange={(e) => setWaterTarget(e.target.value)} inputMode="numeric" />
          </label>
        </div>
        <button type="button" className="auth-button auth-button--primary" disabled={busy} onClick={() => void onSaveGoals()}>
          Save goals
        </button>
        {settings ? (
          <p className="auth-hint">Last updated {new Date(settings.updated_at).toLocaleString()}</p>
        ) : null}
      </div>

      <div className="nutrition-card">
        <h3 className="nutrition-meal-title">Meal types</h3>
        <ul className="nutrition-list">
          {mealTypes.map((mt) => (
            <li key={mt.id} className="nutrition-list-row">
              <input
                className="hello-sync-input"
                defaultValue={mt.name}
                onBlur={(e) => {
                  if (e.target.value.trim() && e.target.value !== mt.name) {
                    void onRenameMeal(mt, e.target.value.trim());
                  }
                }}
              />
              <button
                type="button"
                className="auth-button auth-button--secondary nutrition-btn-sm"
                disabled={busy || mealTypes.length <= 1}
                onClick={() => void deleteMealType(userId, mt.id).then(() => load())}
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
        <div className="hello-sync-form">
          <input className="hello-sync-input" placeholder="New meal name" value={newMealName} onChange={(e) => setNewMealName(e.target.value)} />
          <button type="button" className="auth-button auth-button--secondary" disabled={busy} onClick={() => void onAddMealType()}>
            Add meal
          </button>
        </div>
      </div>

      <div className="nutrition-card">
        <h3 className="nutrition-meal-title">Weight log</h3>
        <div className="hello-sync-form">
          <input className="hello-sync-input" value={weightValue} onChange={(e) => setWeightValue(e.target.value)} placeholder="kg" inputMode="decimal" />
          <input className="hello-sync-input" type="date" value={weightDate} onChange={(e) => setWeightDate(e.target.value)} />
          <button type="button" className="auth-button auth-button--primary" disabled={busy} onClick={() => void onLogWeight()}>
            Log weight
          </button>
        </div>
        <ul className="nutrition-list">
          {recentWeights.map((w) => (
            <li key={`${w.at}-${w.value}`}>
              {w.at}: {w.value} kg
            </li>
          ))}
        </ul>
      </div>

      <AccountSettingsPanel userId={userId} onDeleted={onAccountDeleted} />
    </section>
  );
}
