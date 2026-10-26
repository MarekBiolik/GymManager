const USER_AGENT = 'GymManager/0.1 (personal diet tracker; local use)'

export type FoodCandidate = {
  name: string
  brand: string | null
  barcode: string | null
  kcal_100g: number
  protein_100g: number
  carbs_100g: number
  fat_100g: number
  source: 'openfoodfacts'
  completeness: number | null
}

type OffNutriments = {
  'energy-kcal_100g'?: number
  energy_kcal_100g?: number
  proteins_100g?: number
  carbohydrates_100g?: number
  fat_100g?: number
}

type OffProduct = {
  code?: string
  product_name?: string
  product_name_pl?: string
  brands?: string
  nutriments?: OffNutriments
  completeness?: number
}

function pickName(product: OffProduct): string | null {
  const name =
    product.product_name_pl?.trim() ||
    product.product_name?.trim() ||
    null
  return name
}

function nutriment(value: number | undefined): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

export function mapOffProduct(product: OffProduct): FoodCandidate | null {
  const name = pickName(product)
  const n = product.nutriments ?? {}
  const kcal =
    nutriment(n['energy-kcal_100g']) ?? nutriment(n.energy_kcal_100g)
  const protein = nutriment(n.proteins_100g)
  const carbs = nutriment(n.carbohydrates_100g)
  const fat = nutriment(n.fat_100g)

  if (!name || kcal == null || protein == null || carbs == null || fat == null) {
    return null
  }

  return {
    name,
    brand: product.brands?.trim() || null,
    barcode: product.code?.trim() || null,
    kcal_100g: kcal,
    protein_100g: protein,
    carbs_100g: carbs,
    fat_100g: fat,
    source: 'openfoodfacts',
    completeness:
      typeof product.completeness === 'number' ? product.completeness : null,
  }
}

async function offFetch(url: string): Promise<Response> {
  return fetch(url, {
    headers: {
      'User-Agent': USER_AGENT,
      Accept: 'application/json',
    },
  })
}

export async function fetchByBarcode(
  barcode: string,
): Promise<FoodCandidate | null> {
  const code = barcode.replace(/\D/g, '')
  if (code.length < 8) return null

  const res = await offFetch(
    `https://world.openfoodfacts.org/api/v2/product/${encodeURIComponent(code)}.json?fields=code,product_name,product_name_pl,brands,nutriments,completeness`,
  )
  if (!res.ok) {
    throw new Error(`Open Food Facts HTTP ${res.status}`)
  }

  const body = (await res.json()) as {
    status: number
    product?: OffProduct
  }
  if (body.status !== 1 || !body.product) return null
  return mapOffProduct({ ...body.product, code })
}

export async function searchOff(query: string): Promise<FoodCandidate[]> {
  const q = query.trim()
  if (q.length < 2) return []

  const params = new URLSearchParams({
    search_terms: q,
    search_simple: '1',
    action: 'process',
    json: '1',
    page_size: '20',
    fields:
      'code,product_name,product_name_pl,brands,nutriments,completeness',
  })

  const res = await offFetch(
    `https://world.openfoodfacts.org/cgi/search.pl?${params.toString()}`,
  )
  if (!res.ok) {
    throw new Error(`Open Food Facts HTTP ${res.status}`)
  }

  const body = (await res.json()) as { products?: OffProduct[] }
  const products = body.products ?? []
  const mapped: FoodCandidate[] = []
  for (const product of products) {
    const candidate = mapOffProduct(product)
    if (candidate) mapped.push(candidate)
  }
  return mapped
}
