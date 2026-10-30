/**
 * OpenFoodFacts client-side fetch (CORS). Maps products → foods insert shape.
 * Spec: docs/specs/openfoodfacts-import-hybrid.md
 */

export const OFF_EXTERNAL_SOURCE = "openfoodfacts";

export type OffSearchHit = {
  code: string;
  name: string;
  proteinGPer100: number;
  carbsGPer100: number;
  fatGPer100: number;
  defaultServingG: number | null;
};

export type OffProductPreview = OffSearchHit;

type OffNutriments = {
  proteins_100g?: number;
  carbohydrates_100g?: number;
  fat_100g?: number;
  serving_size?: string;
};

function num(v: unknown): number {
  const n = typeof v === "number" ? v : Number(v);
  return Number.isFinite(n) ? n : 0;
}

function pickName(product: Record<string, unknown>): string {
  const en = product.product_name_en;
  if (typeof en === "string" && en.trim()) {
    return en.trim();
  }
  const generic = product.product_name;
  if (typeof generic === "string" && generic.trim()) {
    return generic.trim();
  }
  return "Unknown product";
}

function parseServingG(nutriments: OffNutriments): number | null {
  const raw = nutriments.serving_size;
  if (typeof raw !== "string") {
    return null;
  }
  const match = raw.match(/([\d.]+)\s*g/i);
  if (!match) {
    return null;
  }
  const g = Number(match[1]);
  return g > 0 ? g : null;
}

function mapProduct(code: string, product: Record<string, unknown>): OffProductPreview | null {
  const nutriments = (product.nutriments ?? {}) as OffNutriments;
  const name = pickName(product);
  if (!name) {
    return null;
  }
  return {
    code,
    name,
    proteinGPer100: num(nutriments.proteins_100g),
    carbsGPer100: num(nutriments.carbohydrates_100g),
    fatGPer100: num(nutriments.fat_100g),
    defaultServingG: parseServingG(nutriments),
  };
}

export async function searchOpenFoodFacts(query: string): Promise<OffSearchHit[]> {
  const q = query.trim();
  if (!q) {
    return [];
  }
  const url = new URL("https://world.openfoodfacts.org/cgi/search.pl");
  url.searchParams.set("search_terms", q);
  url.searchParams.set("json", "1");
  url.searchParams.set("page_size", "20");
  url.searchParams.set("fields", "code,product_name,product_name_en,nutriments");

  const res = await fetch(url.toString());
  if (!res.ok) {
    throw new Error(`OFF search failed (${res.status})`);
  }
  const body = (await res.json()) as { products?: Record<string, unknown>[] };
  const products = body.products ?? [];
  const hits: OffSearchHit[] = [];
  for (const p of products) {
    const code = String(p.code ?? "");
    if (!code) {
      continue;
    }
    const mapped = mapProduct(code, p);
    if (mapped) {
      hits.push(mapped);
    }
  }
  return hits;
}

export async function fetchOpenFoodFactsBarcode(barcode: string): Promise<OffProductPreview | null> {
  const code = barcode.trim();
  if (!code) {
    return null;
  }
  const url = `https://world.openfoodfacts.org/api/v2/product/${encodeURIComponent(code)}.json`;
  const res = await fetch(url);
  if (res.status === 404) {
    return null;
  }
  if (!res.ok) {
    throw new Error(`OFF product lookup failed (${res.status})`);
  }
  const body = (await res.json()) as { product?: Record<string, unknown>; status?: number };
  if (body.status === 0 || !body.product) {
    return null;
  }
  return mapProduct(code, body.product);
}

export function offToFoodInsert(userId: string, preview: OffProductPreview) {
  return {
    user_id: userId,
    name: preview.name,
    protein_g: preview.proteinGPer100,
    carbs_g: preview.carbsGPer100,
    fat_g: preview.fatGPer100,
    default_serving_g: preview.defaultServingG,
    is_recipe: false,
    is_favorite: false,
    external_source: OFF_EXTERNAL_SOURCE,
    external_id: preview.code,
  };
}
