import { useAuthSession } from "../hooks/useAuthSession";
import { defaultWebOrigin, oauthRedirectTo } from "../lib/supabaseClient";
import { NutritionApp } from "./nutrition/NutritionShell";

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
    return <NutritionApp userId={userId} onSignOut={() => void signOut()} onAccountDeleted={() => void signOut()} />;
  }

  return (
    <div className="auth-panel">
      <h1 className="app-title">GymManager web</h1>
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
