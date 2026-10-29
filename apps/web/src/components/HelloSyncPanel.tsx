import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  deleteMeasurement,
  insertMeasurement,
  readMeasurements,
  readProfile,
  type MeasurementRow,
  type ProfileRow,
} from "../lib/helloSync";

type Props = {
  userId: string;
};

export function HelloSyncPanel({ userId }: Props) {
  const [profile, setProfile] = useState<ProfileRow | null>(null);
  const [measurements, setMeasurements] = useState<MeasurementRow[]>([]);
  const [staleFetchedAt, setStaleFetchedAt] = useState<number | null>(null);
  const [offlineHint, setOfflineHint] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [metric, setMetric] = useState("weight");
  const [value, setValue] = useState("70");
  const [busy, setBusy] = useState(false);
  const online = typeof navigator === "undefined" ? true : navigator.onLine;

  const refresh = useCallback(async () => {
    setMessage(null);
    const [p, m] = await Promise.all([readProfile(userId), readMeasurements(userId)]);
    let staleAt: number | null = null;
    if (p.kind === "fresh") {
      setProfile(p.data);
    } else if (p.kind === "stale") {
      setProfile(p.data);
      staleAt = p.fetchedAt;
    } else {
      setProfile(null);
      if (p.reason === "offline") {
        setOfflineHint("No network — no cached profile.");
      }
    }
    if (m.kind === "fresh") {
      setMeasurements(m.data);
    } else if (m.kind === "stale") {
      setMeasurements(m.data);
      staleAt = staleAt ?? m.fetchedAt;
    } else {
      setMeasurements([]);
      if (m.reason === "offline") {
        setOfflineHint("No network — no cached measurements.");
      }
    }
    setStaleFetchedAt(staleAt);
    if (staleAt != null) {
      setOfflineHint(`Showing cached data (last updated ${new Date(staleAt).toLocaleString()}).`);
    } else if (navigator.onLine) {
      setOfflineHint(null);
    }
  }, [userId]);

  useEffect(() => {
    void refresh();
    const onOnline = () => void refresh();
    window.addEventListener("online", onOnline);
    window.addEventListener("offline", onOnline);
    return () => {
      window.removeEventListener("online", onOnline);
      window.removeEventListener("offline", onOnline);
    };
  }, [refresh]);

  async function onAdd(e: FormEvent) {
    e.preventDefault();
    setMessage(null);
    if (!navigator.onLine) {
      setMessage("A network connection is required to save.");
      return;
    }
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) {
      setMessage("Enter a numeric value.");
      return;
    }
    setBusy(true);
    const result = await insertMeasurement(userId, {
      metric: metric.trim() || "weight",
      value: parsed,
      measuredAtIso: new Date().toISOString(),
    });
    setBusy(false);
    if (result.kind === "offline") {
      setMessage("A network connection is required to save.");
      return;
    }
    if (result.kind === "error") {
      setMessage(result.message);
      return;
    }
    await refresh();
  }

  async function onDelete(id: string) {
    setMessage(null);
    if (!navigator.onLine) {
      setMessage("A network connection is required to save.");
      return;
    }
    setBusy(true);
    const result = await deleteMeasurement(userId, id);
    setBusy(false);
    if (result.kind === "offline") {
      setMessage("A network connection is required to save.");
      return;
    }
    if (result.kind === "error") {
      setMessage(result.message);
      return;
    }
    await refresh();
  }

  return (
    <section className="hello-sync" aria-label="Hello sync">
      <h2 className="hello-sync-title">Hello sync</h2>
      {offlineHint ? (
        <p className="hello-sync-banner" role="status">
          {offlineHint}
        </p>
      ) : null}
      <div className="hello-sync-block">
        <h3 className="hello-sync-subtitle">Profile</h3>
        {profile ? (
          <p className="app-sub">
            Units: <strong>{profile.preferred_units}</strong>
          </p>
        ) : (
          <p className="app-sub">No profile loaded.</p>
        )}
      </div>
      <div className="hello-sync-block">
        <h3 className="hello-sync-subtitle">Measurements</h3>
        {measurements.length === 0 ? (
          <p className="app-sub">No measurements yet.</p>
        ) : (
          <ul className="hello-sync-list">
            {measurements.map((row) => (
              <li key={row.id} className="hello-sync-item">
                <span>
                  {row.metric}: {row.value}
                </span>
                <button
                  type="button"
                  className="auth-button auth-button--secondary"
                  disabled={busy || !online}
                  onClick={() => void onDelete(row.id)}
                >
                  Delete
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
      <form className="hello-sync-form" onSubmit={(e) => void onAdd(e)}>
        <label className="hello-sync-label">
          Metric
          <input
            className="hello-sync-input"
            value={metric}
            onChange={(e) => setMetric(e.target.value)}
            disabled={busy}
          />
        </label>
        <label className="hello-sync-label">
          Value
          <input
            className="hello-sync-input"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            disabled={busy}
            inputMode="decimal"
          />
        </label>
        <button
          type="submit"
          className="auth-button auth-button--primary"
          disabled={busy || !online}
          title={!online ? "A network connection is required to save." : undefined}
        >
          Add measurement
        </button>
      </form>
      {!online ? (
        <p className="hello-sync-banner" role="status">
          A network connection is required to save.
        </p>
      ) : null}
      {message ? (
        <p className="auth-error" role="alert">
          {message}
        </p>
      ) : null}
      {staleFetchedAt != null && online ? (
        <p className="auth-hint">Refreshed from cache earlier; retry when online recovers fully.</p>
      ) : null}
    </section>
  );
}
