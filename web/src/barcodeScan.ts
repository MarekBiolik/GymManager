/** Native BarcodeDetector when available (Chromium, usually needs a secure context). */

type BarcodeDetectorLike = {
  detect: (
    source: ImageBitmapSource,
  ) => Promise<Array<{ rawValue?: string }>>
}

type BarcodeDetectorCtor = new (options?: {
  formats?: string[]
}) => BarcodeDetectorLike

function getBarcodeDetectorCtor(): BarcodeDetectorCtor | null {
  const ctor = (window as unknown as { BarcodeDetector?: BarcodeDetectorCtor })
    .BarcodeDetector
  return ctor ?? null
}

export function canUseCameraBarcodeScan(): boolean {
  return (
    typeof window !== 'undefined' &&
    window.isSecureContext &&
    !!navigator.mediaDevices?.getUserMedia &&
    !!getBarcodeDetectorCtor()
  )
}

/** Digits-only EAN/UPC (8–14). */
export function extractBarcode(value: string): string | null {
  const digits = value.replace(/\D/g, '')
  if (digits.length >= 8 && digits.length <= 14 && /^\d+$/.test(digits)) {
    return digits
  }
  return null
}

export type ScanSession = {
  stop: () => void
}

/**
 * Opens the rear camera and polls BarcodeDetector until a code is found or stopped.
 */
export async function startBarcodeScan(
  video: HTMLVideoElement,
  onCode: (code: string) => void,
): Promise<ScanSession> {
  const Detector = getBarcodeDetectorCtor()
  if (!Detector) {
    throw new Error('BarcodeDetector unavailable')
  }

  const stream = await navigator.mediaDevices.getUserMedia({
    audio: false,
    video: {
      facingMode: { ideal: 'environment' },
      width: { ideal: 1280 },
      height: { ideal: 720 },
    },
  })

  video.srcObject = stream
  video.setAttribute('playsinline', 'true')
  await video.play()

  const detector = new Detector({
    formats: ['ean_13', 'ean_8', 'upc_a', 'upc_e'],
  })

  let active = true
  let raf = 0

  const tick = async () => {
    if (!active) return
    try {
      if (video.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA) {
        const codes = await detector.detect(video)
        const raw = codes[0]?.rawValue?.replace(/\D/g, '')
        if (raw && raw.length >= 8) {
          active = false
          onCode(raw)
          return
        }
      }
    } catch {
      // Keep scanning through transient detect errors.
    }
    raf = window.setTimeout(() => {
      void tick()
    }, 250)
  }

  void tick()

  return {
    stop: () => {
      active = false
      window.clearTimeout(raf)
      stream.getTracks().forEach((track) => track.stop())
      video.srcObject = null
    },
  }
}
