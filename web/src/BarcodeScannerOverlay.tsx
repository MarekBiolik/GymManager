import { useEffect, useRef } from 'react'
import { startBarcodeScan } from './barcodeScan'
import { t } from './i18n'

type Props = {
  onDetected: (code: string) => void
  onCancel: () => void
  onError: (message: string) => void
}

export function BarcodeScannerOverlay({
  onDetected,
  onCancel,
  onError,
}: Props) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const onDetectedRef = useRef(onDetected)
  const onCancelRef = useRef(onCancel)
  const onErrorRef = useRef(onError)

  onDetectedRef.current = onDetected
  onCancelRef.current = onCancel
  onErrorRef.current = onError

  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    let stopped = false
    let session: { stop: () => void } | null = null

    void startBarcodeScan(video, (code) => {
      if (stopped) return
      session?.stop()
      onDetectedRef.current(code)
    })
      .then((s) => {
        if (stopped) {
          s.stop()
          return
        }
        session = s
      })
      .catch((err: unknown) => {
        if (stopped) return
        const message = err instanceof Error ? err.message : ''
        onErrorRef.current(
          /NotAllowed|Permission|NotReadable/i.test(message)
            ? t.addFood.cameraPermissionDenied
            : t.addFood.cameraUnavailable,
        )
        onCancelRef.current()
      })

    return () => {
      stopped = true
      session?.stop()
    }
  }, [])

  return (
    <div className="scanner-overlay" role="dialog" aria-label={t.addFood.scanAria}>
      <video ref={videoRef} className="scanner-video" muted playsInline />
      <div className="scanner-frame" aria-hidden="true" />
      <p className="scanner-hint">{t.addFood.scanHint}</p>
      <button
        type="button"
        className="primary-btn scanner-cancel"
        onClick={() => onCancelRef.current()}
      >
        {t.addFood.cancel}
      </button>
    </div>
  )
}
