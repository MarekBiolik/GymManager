import { useAuthSession } from "../hooks/useAuthSession";
import { defaultWebOrigin, oauthRedirectTo } from "../lib/supabaseClient";
import { AccountSettingsPanel } from "./AccountSettingsPanel";
import { HelloSyncPanel } from "./HelloSyncPanel";

export function AuthPanel() {
  const { state, signInWithGoogle, signOut } = useAuthSession();

  if (state.status === "loading") {
    return <p className="app-sub">Checking session…</p>;
  }

  if (state.status === "unconfigured") {
    return (
      <div className="auth-panel">
        <p className="app-sub">
          Supabase env missing. Copy <code className="auth-code">.env.example</code> to{" "}
          <code className="auth-code">.env.local</code> and set{" "}
          <code className="auth-code">VITE_SUPABASE_*</code> (anon key only).
        </p>
      </div>
    );
  }

  if (state.status === "error") {
    return (
      <div className="auth-panel">
        <p className="auth-error" role="alert">
          {state.message}
        </p>
        <button type="button" className="auth-button auth-button--primary" onClick={() => void signInWithGoogle()}>
          Try Sign in with Google
        </button>
      </div>
    );
  }

  if (state.status === "signed_in") {
    const userId = state.session.user.id;
    return (
      <div className="auth-panel auth-panel--wide">
        <p className="app-sub">Signed in</p>
        <p className="auth-meta" title={userId}>
          User {userId.slice(0, 8)}…
        </p>
        <button type="button" className="auth-button auth-button--secondary" onClick={() => void signOut()}>
          Sign out
        </button>
        <HelloSyncPanel userId={userId} />
        <AccountSettingsPanel userId={userId} onDeleted={() => void signOut()} />
        <p className="auth-hint">Live OAuth / RLS smoke needs IdP (#73) and a configured Supabase project.</p>
      </div>
    );
  }

  return (
    <div className="auth-panel">
      <button type="button" className="auth-button auth-button--primary" onClick={() => void signInWithGoogle()}>
        Sign in with Google
      </button>
      <p className="auth-hint">
        OAuth return URL: <code className="auth-code">{oauthRedirectTo() || defaultWebOrigin}</code>. Configure
        Supabase Auth redirect allow-list per{" "}
        <code className="auth-code">docs/specs/idp-config-checklist.md</code>. Live smoke needs IdP setup (#73).
      </p>
    </div>
  );
}
