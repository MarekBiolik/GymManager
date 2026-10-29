import type { Session } from "@supabase/supabase-js";
import { useCallback, useEffect, useState } from "react";
import { wipeHelloSyncCache } from "../lib/helloSync";
import {
  restoreSession,
  signInWithGoogle,
  signOut,
  subscribeToAuthSession,
  supabaseConfigured,
} from "../lib/supabaseClient";

export type AuthSessionState =
  | { status: "loading" }
  | { status: "unconfigured" }
  | { status: "signed_out" }
  | { status: "signed_in"; session: Session }
  | { status: "error"; message: string };

export function useAuthSession(): {
  state: AuthSessionState;
  signInWithGoogle: () => Promise<void>;
  signOut: () => Promise<void>;
} {
  const [state, setState] = useState<AuthSessionState>(() =>
    supabaseConfigured ? { status: "loading" } : { status: "unconfigured" },
  );

  useEffect(() => {
    if (!supabaseConfigured) {
      return;
    }

    let cancelled = false;

    void restoreSession()
      .then((session) => {
        if (cancelled) {
          return;
        }
        setState(session ? { status: "signed_in", session } : { status: "signed_out" });
      })
      .catch((err: unknown) => {
        if (cancelled) {
          return;
        }
        const message = err instanceof Error ? err.message : "Failed to restore session";
        setState({ status: "error", message });
      });

    const unsubscribe = subscribeToAuthSession((session) => {
      if (cancelled) {
        return;
      }
      setState(session ? { status: "signed_in", session } : { status: "signed_out" });
    });

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, []);

  const handleSignIn = useCallback(async () => {
    setState({ status: "loading" });
    try {
      await signInWithGoogle();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Sign-in failed";
      setState({ status: "error", message });
    }
  }, []);

  const handleSignOut = useCallback(async () => {
    try {
      const userId = state.status === "signed_in" ? state.session.user.id : null;
      await signOut();
      if (userId) {
        wipeHelloSyncCache(userId);
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Sign-out failed";
      setState({ status: "error", message });
    }
  }, [state]);

  return { state, signInWithGoogle: handleSignIn, signOut: handleSignOut };
}
