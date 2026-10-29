import { createClient, type Session } from "@supabase/supabase-js";

/**
 * Thin Supabase client helper. Reads URL + anon key from Vite env.
 * Never put service_role in the web client.
 *
 * OAuth redirect: must match Supabase Dashboard → Authentication → URL Configuration
 * (Site URL `http://localhost:5173`, Additional Redirect URLs e.g. `http://localhost:5173/**`).
 * See docs/specs/idp-config-checklist.md. Live Google smoke requires maintainer IdP setup (#73).
 */
const url = import.meta.env.VITE_SUPABASE_URL as string | undefined;
const anon = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined;

export const supabaseConfigured = Boolean(url && anon);

/** Vite dev default; production uses the deployed origin. */
export const defaultWebOrigin = "http://localhost:5173";

export const supabase =
  url && anon
    ? createClient(url, anon, {
        auth: {
          detectSessionInUrl: true,
          persistSession: true,
          flowType: "pkce",
        },
      })
    : null;

/**
 * Return URL after OAuth (Supabase → app). Uses current origin so dev/prod match Dashboard allow-list.
 */
export function oauthRedirectTo(): string {
  if (typeof window === "undefined") {
    return defaultWebOrigin;
  }
  return window.location.origin + window.location.pathname;
}

export async function restoreSession(): Promise<Session | null> {
  if (!supabase) {
    return null;
  }
  const { data, error } = await supabase.auth.getSession();
  if (error) {
    throw error;
  }
  return data.session;
}

export async function signInWithGoogle(): Promise<void> {
  if (!supabase) {
    throw new Error("Supabase is not configured. Set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY.");
  }
  const { error } = await supabase.auth.signInWithOAuth({
    provider: "google",
    options: {
      redirectTo: oauthRedirectTo(),
    },
  });
  if (error) {
    throw error;
  }
}

export async function signOut(): Promise<void> {
  if (!supabase) {
    return;
  }
  const { error } = await supabase.auth.signOut();
  if (error) {
    throw error;
  }
}

export function subscribeToAuthSession(
  onSession: (session: Session | null) => void,
): () => void {
  if (!supabase) {
    onSession(null);
    return () => {};
  }
  const {
    data: { subscription },
  } = supabase.auth.onAuthStateChange((_event, session) => {
    onSession(session);
  });
  return () => subscription.unsubscribe();
}
