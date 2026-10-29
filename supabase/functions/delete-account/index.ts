import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

function jsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return jsonResponse({ error: "Missing or invalid Authorization header" }, 401);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!supabaseUrl || !anonKey || !serviceRoleKey) {
    console.error("[delete-account] Missing Supabase environment variables");
    return jsonResponse({ error: "Server configuration error" }, 500);
  }

  // Identify caller from their JWT only — never trust a uid from the request body.
  const asUser = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authHeader } },
  });

  const token = authHeader.slice("Bearer ".length);
  const {
    data: { user },
    error: authError,
  } = await asUser.auth.getUser(token);

  if (authError || !user?.id) {
    return jsonResponse({ error: "Unauthorized" }, 401);
  }

  const asAdmin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });

  const { error: deleteError } = await asAdmin.auth.admin.deleteUser(user.id);

  if (deleteError) {
    console.error("[delete-account] deleteUser failed:", deleteError.message);
    return jsonResponse({ error: "Failed to delete account" }, 500);
  }

  // App rows (profiles, measurements, …) are removed via FK ON DELETE CASCADE.
  return jsonResponse({ ok: true }, 200);
});
