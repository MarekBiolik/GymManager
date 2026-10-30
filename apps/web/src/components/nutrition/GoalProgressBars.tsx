import { goalProgressOf, type Macros, type NutritionSettingsGoals } from "../../lib/macros";

type Props = {
  consumed: Macros;
  goals: NutritionSettingsGoals;
  waterMl?: number;
  waterTargetMl?: number | null;
};

function Bar({ label, value, target, ratio }: { label: string; value: number; target: number; ratio: number }) {
  const pct = Math.min(100, Math.round(ratio * 100));
  return (
    <div className="nutrition-progress">
      <div className="nutrition-progress-head">
        <span>{label}</span>
        <span className="nutrition-progress-meta">
          {Math.round(value)} / {Math.round(target)}
        </span>
      </div>
      <div
        className="nutrition-progress-track"
        role="progressbar"
        aria-label={label}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={pct}
      >
        <div className="nutrition-progress-fill" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

export function GoalProgressBars({ consumed, goals, waterMl, waterTargetMl }: Props) {
  const p = goalProgressOf(consumed, goals);
  return (
    <div className="nutrition-goals">
      <Bar label="Calories (kcal)" value={consumed.kcal} target={goals.calorieTargetKcal} ratio={p.calories} />
      <Bar label="Protein (g)" value={consumed.proteinG} target={goals.proteinTargetG} ratio={p.protein} />
      <Bar label="Carbs (g)" value={consumed.carbsG} target={goals.carbsTargetG} ratio={p.carbs} />
      <Bar label="Fat (g)" value={consumed.fatG} target={goals.fatTargetG} ratio={p.fat} />
      {waterTargetMl != null && waterTargetMl > 0 ? (
        <Bar
          label="Water (ml)"
          value={waterMl ?? 0}
          target={waterTargetMl}
          ratio={(waterMl ?? 0) / waterTargetMl}
        />
      ) : null}
    </div>
  );
}
