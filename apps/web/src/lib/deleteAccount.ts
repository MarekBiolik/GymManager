/**
 * Calls the delete-account Edge Function with the user JWT.
 * Never uses service_role in the browser.
 */
import { supabase } from "./supabaseClient";

export type DeleteAccountResult =
  | { kind: "ok" }
  | { kind: "error"; message: string };

export async function deleteAccount(): Promise<DeleteAccountResult> {
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  const {
    data: { session },
  } = await supabase.auth.getSession();
  if (!session?.access_token) {
    return { kind: "error", message: "Not signed in" };
  }

  const { data, error } = await supabase.functions.invoke("delete-account", {
    method: "POST",
  });

  if (error) {
    return { kind: "error", message: error.message || "Delete account failed" };
  }
  if (data && typeof data === "object" && "error" in data) {
    return { kind: "error", message: String((data as { error: string }).error) };
  }
  return { kind: "ok" };
}
