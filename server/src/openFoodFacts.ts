const USER_AGENT = 'GymManager/0.1 (personal diet tracker; local use)'
const RETRYABLE = new Set([429, 502, 503, 504])

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

export class OpenFoodFactsError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message)
    this.name = 'OpenFoodFactsError'
  }
}

function pickName(product: OffProduct): string | null {
  return product.product_name_pl?.trim() || product.product_name?.trim() || null
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

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function offFetch(url: string, attempts = 3): Promise<Response> {
  let lastError: unknown

  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      const res = await fetch(url, {
        headers: {
          'User-Agent': USER_AGENT,
          Accept: 'application/json',
        },
      })

      if (RETRYABLE.has(res.status) && attempt < attempts) {
        await sleep(400 * attempt * attempt)
        continue
      }

      return res
    } catch (err) {
      lastError = err
      if (attempt < attempts) {
        await sleep(400 * attempt * attempt)
        continue
      }
    }
  }

  throw new OpenFoodFactsError(
    lastError instanceof Error
      ? `Open Food Facts niedostępne: ${lastError.message}`
      : 'Open Food Facts niedostępne',
  )
}

async function readJson<T>(res: Response): Promise<T> {
  const text = await res.text()
  const trimmed = text.trim()
  if (!trimmed || trimmed.startsWith('<')) {
    throw new OpenFoodFactsError(
      'Open Food Facts chwilowo przeciążone — spróbuj za chwilę albo wpisz kod EAN',
      res.status,
    )
  }

  try {
    return JSON.parse(trimmed) as T
  } catch {
    throw new OpenFoodFactsError(
      'Open Food Facts zwróciło nieczytelną odpowiedź',
      res.status,
    )
  }
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
    throw new OpenFoodFactsError(
      res.status === 503
        ? 'Open Food Facts chwilowo przeciążone — spróbuj za chwilę'
        : `Open Food Facts HTTP ${res.status}`,
      res.status,
    )
  }

  const body = await readJson<{
    status: number
    product?: OffProduct
  }>(res)
  if (body.status !== 1 || !body.product) return null
  return mapOffProduct({ ...body.product, code })
}

function mapProducts(products: OffProduct[]): FoodCandidate[] {
  const mapped: FoodCandidate[] = []
  for (const product of products) {
    const candidate = mapOffProduct(product)
    if (candidate) mapped.push(candidate)
  }
  return mapped
}

async function searchViaCgi(query: string): Promise<FoodCandidate[]> {
  const params = new URLSearchParams({
    search_terms: query,
    search_simple: '1',
    action: 'process',
    json: '1',
    page_size: '20',
  })

  const res = await offFetch(
    `https://world.openfoodfacts.org/cgi/search.pl?${params.toString()}`,
  )
  if (!res.ok) {
    throw new OpenFoodFactsError(
      res.status === 503
        ? 'Open Food Facts chwilowo przeciążone — spróbuj za chwilę albo wpisz kod EAN'
        : `Open Food Facts HTTP ${res.status}`,
      res.status,
    )
  }

  const body = await readJson<{ products?: OffProduct[] }>(res)
  return mapProducts(body.products ?? [])
}

async function searchViaApiV2(query: string): Promise<FoodCandidate[]> {
  const params = new URLSearchParams({
    search_terms: query,
    page_size: '20',
    fields:
      'code,product_name,product_name_pl,brands,nutriments,completeness',
  })

  const res = await offFetch(
    `https://world.openfoodfacts.org/api/v2/search?${params.toString()}`,
  )
  if (!res.ok) {
    throw new OpenFoodFactsError(
      res.status === 503
        ? 'Open Food Facts chwilowo przeciążone — spróbuj za chwilę albo wpisz kod EAN'
        : `Open Food Facts HTTP ${res.status}`,
      res.status,
    )
  }

  const body = await readJson<{ products?: OffProduct[] }>(res)
  return mapProducts(body.products ?? [])
}

export async function searchOff(query: string): Promise<FoodCandidate[]> {
  const q = query.trim()
  if (q.length < 2) return []

  try {
    return await searchViaCgi(q)
  } catch (cgiError) {
    try {
      return await searchViaApiV2(q)
    } catch {
      throw cgiError
    }
  }
}
