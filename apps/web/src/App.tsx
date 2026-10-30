/**
 * Thin web shell — supabase-js only (ADR 0006). No parallel TS domain.
 * Colors from generated CSS tokens (design/ SSOT).
 */
import { AuthPanel } from "./components/AuthPanel";

export default function App() {
  return (
    <main className="app-shell app-shell--settings">
      <h1 className="app-title">GymManager web</h1>
      <AuthPanel />
    </main>
  );
}
