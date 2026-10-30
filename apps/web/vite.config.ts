import path from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");

// Web talks to Supabase via supabase-js only — no KMP binary (ADR 0006).
// envDir = monorepo root so root `.env.local` / `.env.example` are loaded (Vite default is apps/web/).
export default defineConfig({
  plugins: [react()],
  envDir: repoRoot,
});
