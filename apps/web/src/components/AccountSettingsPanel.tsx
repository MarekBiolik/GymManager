import { useState } from "react";
import { deleteAccount } from "../lib/deleteAccount";
import { wipeHelloSyncCache } from "../lib/helloSync";
import { signOut } from "../lib/supabaseClient";

type Props = {
  userId: string;
  onDeleted: () => void;
};

/**
 * Account settings entry for deletion — two-step confirm (spec account-deletion.md).
 */
export function AccountSettingsPanel({ userId, onDeleted }: Props) {
  const [step, setStep] = useState<"idle" | "confirm">("idle");
  const [phrase, setPhrase] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onConfirmDelete() {
    setError(null);
    if (phrase.trim().toUpperCase() !== "DELETE") {
      setError('Type DELETE to confirm.');
      return;
    }
    setBusy(true);
    const result = await deleteAccount();
    if (result.kind === "error") {
      setBusy(false);
      setError(result.message);
      return;
    }
    wipeHelloSyncCache(userId);
    await signOut();
    setBusy(false);
    onDeleted();
  }

  return (
    <section className="account-settings" aria-label="Account settings">
      <h2 className="hello-sync-title">Account settings</h2>
      <p className="app-sub">
        Deleting your account permanently removes your GymManager data and Auth identity. Signing in
        again creates a new empty account.
      </p>
      {step === "idle" ? (
        <button
          type="button"
          className="auth-button auth-button--danger"
          onClick={() => {
            setStep("confirm");
            setPhrase("");
            setError(null);
          }}
        >
          Delete account…
        </button>
      ) : (
        <div className="account-settings-confirm">
          <p className="auth-error" role="alert">
            This cannot be undone. Type DELETE to confirm.
          </p>
          <input
            className="hello-sync-input"
            value={phrase}
            onChange={(e) => setPhrase(e.target.value)}
            disabled={busy}
            aria-label="Type DELETE to confirm"
            autoComplete="off"
          />
          <div className="hello-sync-form">
            <button
              type="button"
              className="auth-button auth-button--danger"
              disabled={busy}
              onClick={() => void onConfirmDelete()}
            >
              Delete forever
            </button>
            <button
              type="button"
              className="auth-button auth-button--secondary"
              disabled={busy}
              onClick={() => setStep("idle")}
            >
              Cancel
            </button>
          </div>
        </div>
      )}
      {error ? (
        <p className="auth-error" role="alert">
          {error}
        </p>
      ) : null}
      <p className="auth-hint">
        Requires deployed <code className="auth-code">delete-account</code> function and IdP (#73)
        for live smoke.
      </p>
    </section>
  );
}
