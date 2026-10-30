import { useCallback, useEffect, useState } from "react";
import {
  deleteFood,
  deleteFoodServing,
  insertFood,
  insertFoodServing,
  readFoodServings,
  readFoods,
  updateFood,
  type FoodRow,
  type FoodServingRow,
} from "../../lib/nutritionApi";
import {
  fetchOpenFoodFactsBarcode,
  offToFoodInsert,
  searchOpenFoodFacts,
  type OffSearchHit,
} from "../../lib/offClient";
import { StaleBanner } from "./StaleBanner";

type Props = { userId: string };

export function FoodsPanel({ userId }: Props) {
  const [foods, setFoods] = useState<FoodRow[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [servings, setServings] = useState<FoodServingRow[]>([]);
  const [banner, setBanner] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [name, setName] = useState("");
  const [protein, setProtein] = useState("0");
  const [carbs, setCarbs] = useState("0");
  const [fat, setFat] = useState("0");
  const [defaultServing, setDefaultServing] = useState("");

  const [servingLabel, setServingLabel] = useState("");
  const [servingGrams, setServingGrams] = useState("100");

  const [offQuery, setOffQuery] = useState("");
  const [offHits, setOffHits] = useState<OffSearchHit[]>([]);
  const [barcode, setBarcode] = useState("");

  const loadFoods = useCallback(async () => {
    setError(null);
    setBanner(null);
    const result = await readFoods(userId);
    if (result.kind === "fresh") {
      setFoods(result.data);
    } else if (result.kind === "stale") {
      setFoods(result.data);
      setBanner("Foods loaded from cache.");
    } else {
      setFoods([]);
      setError("Could not load foods.");
    }
  }, [userId]);

  useEffect(() => {
    void loadFoods();
  }, [loadFoods]);

  const selected = foods.find((f) => f.id === selectedId) ?? null;

  useEffect(() => {
    if (!selectedId) {
      setServings([]);
      return;
    }
    void readFoodServings(selectedId).then((r) => {
      if (r.kind === "fresh" || r.kind === "stale") {
        setServings(r.data);
      } else {
        setServings([]);
      }
    });
  }, [selectedId]);

  function selectFood(f: FoodRow) {
    setSelectedId(f.id);
    setName(f.name);
    setProtein(String(f.protein_g));
    setCarbs(String(f.carbs_g));
    setFat(String(f.fat_g));
    setDefaultServing(f.default_serving_g != null ? String(f.default_serving_g) : "");
  }

  function resetNew() {
    setSelectedId(null);
    setName("");
    setProtein("0");
    setCarbs("0");
    setFat("0");
    setDefaultServing("");
    setServings([]);
  }

  async function onSaveFood() {
    setError(null);
    const p = Number(protein);
    const c = Number(carbs);
    const f = Number(fat);
    const ds = defaultServing.trim() ? Number(defaultServing) : null;
    if (!name.trim()) {
      setError("Name is required.");
      return;
    }
    setBusy(true);
    const payload = {
      name: name.trim(),
      protein_g: p,
      carbs_g: c,
      fat_g: f,
      default_serving_g: ds,
      is_recipe: false,
      is_favorite: selected?.is_favorite ?? false,
      external_source: selected?.external_source ?? null,
      external_id: selected?.external_id ?? null,
    };
    const result = selectedId
      ? await updateFood(userId, selectedId, payload)
      : await insertFood(userId, payload);
    setBusy(false);
    if (result.kind === "offline") {
      setError("Offline — cannot save food.");
      return;
    }
    if (result.kind === "error") {
      setError(result.message);
      return;
    }
    void loadFoods();
    setSelectedId(result.data.id);
  }

  async function onToggleFavorite(food: FoodRow) {
    setError(null);
    setBusy(true);
    const result = await updateFood(userId, food.id, { is_favorite: !food.is_favorite });
    setBusy(false);
    if (result.kind === "ok") {
      void loadFoods();
    } else if (result.kind === "offline") {
      setError("Offline — cannot update favorite.");
    } else if (result.kind === "error") {
      setError(result.message);
    }
  }

  async function onDeleteFood() {
    if (!selectedId) {
      return;
    }
    setBusy(true);
    const result = await deleteFood(userId, selectedId);
    setBusy(false);
    if (result.kind === "ok") {
      resetNew();
      void loadFoods();
    } else if (result.kind === "error") {
      setError(result.message);
    } else if (result.kind === "offline") {
      setError("Offline — cannot delete food.");
    }
  }

  async function onAddServing() {
    if (!selectedId) {
      return;
    }
    const grams = Number(servingGrams);
    if (!servingLabel.trim() || !Number.isFinite(grams) || grams <= 0) {
      setError("Serving label and grams required.");
      return;
    }
    setBusy(true);
    const result = await insertFoodServing(userId, {
      food_id: selectedId,
      label: servingLabel.trim(),
      grams,
      position: servings.length,
    });
    setBusy(false);
    if (result.kind === "ok") {
      setServings((s) => [...s, result.data]);
      setServingLabel("");
    } else if (result.kind === "error") {
      setError(result.message);
    } else if (result.kind === "offline") {
      setError("Offline — cannot add serving.");
    }
  }

  async function onRemoveServing(id: string) {
    setBusy(true);
    const result = await deleteFoodServing(userId, id);
    setBusy(false);
    if (result.kind === "ok") {
      setServings((rows) => rows.filter((x) => x.id !== id));
    } else if (result.kind === "error") {
      setError(result.message);
    } else if (result.kind === "offline") {
      setError("Offline — cannot remove serving.");
    }
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
    setOffHits([]);
    void loadFoods();
    selectFood(ins.data);
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
    <section className="nutrition-panel" aria-label="Foods">
      <h2 className="nutrition-panel-title">Foods</h2>
      {banner ? <StaleBanner label={banner} /> : null}
      {error ? (
        <p className="auth-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="nutrition-split">
        <ul className="nutrition-list nutrition-food-list">
          {foods.map((f) => (
            <li key={f.id}>
              <button
                type="button"
                className={f.id === selectedId ? "nutrition-food-item is-selected" : "nutrition-food-item"}
                onClick={() => selectFood(f)}
              >
                {f.is_favorite ? "★ " : ""}
                {f.name}
              </button>
              <button
                type="button"
                className="auth-button auth-button--secondary nutrition-btn-sm"
                disabled={busy}
                onClick={() => void onToggleFavorite(f)}
              >
                {f.is_favorite ? "Unfav" : "Fav"}
              </button>
            </li>
          ))}
        </ul>

        <div className="nutrition-card">
          <h3 className="nutrition-meal-title">{selectedId ? "Edit food" : "New food"}</h3>
          <div className="nutrition-form-grid">
            <label className="hello-sync-label">
              Name
              <input className="hello-sync-input" value={name} onChange={(e) => setName(e.target.value)} />
            </label>
            <label className="hello-sync-label">
              Protein / 100g
              <input
                className="hello-sync-input"
                value={protein}
                onChange={(e) => setProtein(e.target.value)}
                inputMode="decimal"
              />
            </label>
            <label className="hello-sync-label">
              Carbs / 100g
              <input
                className="hello-sync-input"
                value={carbs}
                onChange={(e) => setCarbs(e.target.value)}
                inputMode="decimal"
              />
            </label>
            <label className="hello-sync-label">
              Fat / 100g
              <input className="hello-sync-input" value={fat} onChange={(e) => setFat(e.target.value)} inputMode="decimal" />
            </label>
            <label className="hello-sync-label">
              Default serving (g)
              <input
                className="hello-sync-input"
                value={defaultServing}
                onChange={(e) => setDefaultServing(e.target.value)}
                inputMode="decimal"
              />
            </label>
          </div>
          <div className="hello-sync-form">
            <button type="button" className="auth-button auth-button--primary" disabled={busy} onClick={() => void onSaveFood()}>
              Save
            </button>
            <button type="button" className="auth-button auth-button--secondary" onClick={resetNew}>
              New
            </button>
            {selectedId ? (
              <button
                type="button"
                className="auth-button auth-button--danger"
                disabled={busy}
                onClick={() => void onDeleteFood()}
              >
                Delete
              </button>
            ) : null}
          </div>

          {selected ? (
            <>
              <h4 className="nutrition-meal-title">Servings</h4>
              <ul className="nutrition-list">
                {servings.map((s) => (
                  <li key={s.id} className="nutrition-list-row">
                    <span>
                      {s.label} — {s.grams}g
                    </span>
                    <button
                      type="button"
                      className="auth-button auth-button--secondary nutrition-btn-sm"
                      disabled={busy}
                      onClick={() => void onRemoveServing(s.id)}
                    >
                      Remove
                    </button>
                  </li>
                ))}
              </ul>
              <div className="hello-sync-form">
                <input
                  className="hello-sync-input"
                  placeholder="Label"
                  value={servingLabel}
                  onChange={(e) => setServingLabel(e.target.value)}
                />
                <input
                  className="hello-sync-input"
                  placeholder="Grams"
                  value={servingGrams}
                  onChange={(e) => setServingGrams(e.target.value)}
                />
                <button
                  type="button"
                  className="auth-button auth-button--secondary"
                  disabled={busy}
                  onClick={() => void onAddServing()}
                >
                  Add serving
                </button>
              </div>
            </>
          ) : null}

          <h3 className="nutrition-meal-title">Open Food Facts import</h3>
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
        </div>
      </div>
    </section>
  );
}
