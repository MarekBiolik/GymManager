import { useEffect, useMemo, useState } from "react";
import { addDays, formatIsoDate, shortDayMonth } from "../../lib/dateUtils";
import { readMeasurements } from "../../lib/helloSync";
import { NutritionAggregations } from "../../lib/macros";
import { readFoodEntriesRange } from "../../lib/nutritionApi";
import { StaleBanner } from "./StaleBanner";

type Props = { userId: string };

export function ChartsPanel({ userId }: Props) {
  const [banner, setBanner] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [dailyKcal, setDailyKcal] = useState<{ eatenOn: string; kcal: number }[]>([]);
  const [avgMacros, setAvgMacros] = useState<{ protein: number; carbs: number; fat: number; kcal: number } | null>(null);
  const [weights, setWeights] = useState<{ at: string; kg: number }[]>([]);

  const to = formatIsoDate(new Date());
  const from = addDays(to, -13);

  useEffect(() => {
    setError(null);
    setBanner(null);
    void readFoodEntriesRange(userId, from, to).then((result) => {
      if (result.kind === "empty") {
        setDailyKcal([]);
        setAvgMacros(null);
        if (result.reason === "offline") {
          setBanner("Charts need network for entry range.");
        }
        return;
      }
      const snapshots = result.data.map((e) => ({
        eatenOn: e.eaten_on,
        proteinG: Number(e.protein_g),
        carbsG: Number(e.carbs_g),
        fatG: Number(e.fat_g),
        kcal: Number(e.kcal),
      }));
      const totals = NutritionAggregations.dailyTotals(snapshots);
      setDailyKcal(totals.map((d) => ({ eatenOn: d.eatenOn, kcal: d.macros.kcal })));
      const avg = NutritionAggregations.averageMacros(totals);
      setAvgMacros({ protein: avg.proteinG, carbs: avg.carbsG, fat: avg.fatG, kcal: avg.kcal });
    });

    void readMeasurements(userId).then((result) => {
      if (result.kind === "fresh" || result.kind === "stale") {
        const w = result.data
          .filter((m) => m.metric === "weight" || m.metric === "body_weight")
          .map((m) => ({ at: m.measured_at.slice(0, 10), kg: m.value }))
          .sort((a, b) => (a.at < b.at ? -1 : 1));
        setWeights(w);
        if (result.kind === "stale") {
          setBanner((b) => b ?? "Weight from cached measurements.");
        }
      }
    });
  }, [userId, from, to]);

  const maxKcal = useMemo(() => Math.max(1, ...dailyKcal.map((d) => d.kcal)), [dailyKcal]);

  return (
    <section className="nutrition-panel" aria-label="Charts">
      <h2 className="nutrition-panel-title">Charts</h2>
      <p className="app-sub">
        Last 14 days ({from} → {to})
      </p>
      {banner ? <StaleBanner label={banner} /> : null}
      {error ? (
        <p className="auth-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="nutrition-card">
        <h3 className="nutrition-meal-title">Daily calories</h3>
        {dailyKcal.length === 0 ? (
          <p className="app-sub">No diary data in range.</p>
        ) : (
          <div className="nutrition-chart-bars">
            {dailyKcal.map((d) => (
              <div key={d.eatenOn} className="nutrition-chart-bar-wrap">
                <div
                  className="nutrition-chart-bar"
                  style={{ height: `${Math.round((d.kcal / maxKcal) * 100)}%` }}
                  title={`${d.eatenOn}: ${Math.round(d.kcal)} kcal`}
                />
                <span className="nutrition-chart-label">{shortDayMonth(d.eatenOn)}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {avgMacros ? (
        <div className="nutrition-card">
          <h3 className="nutrition-meal-title">Average macros (logged days)</h3>
          <ul className="nutrition-list">
            <li>{Math.round(avgMacros.kcal)} kcal / day</li>
            <li>Protein {avgMacros.protein.toFixed(1)} g</li>
            <li>Carbs {avgMacros.carbs.toFixed(1)} g</li>
            <li>Fat {avgMacros.fat.toFixed(1)} g</li>
          </ul>
        </div>
      ) : null}

      <div className="nutrition-card">
        <h3 className="nutrition-meal-title">Weight (measurements)</h3>
        {weights.length === 0 ? (
          <p className="app-sub">Log weight in Settings.</p>
        ) : (
          <ul className="nutrition-list">
            {weights.map((w) => (
              <li key={`${w.at}-${w.kg}`}>
                {w.at}: {w.kg} kg
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
