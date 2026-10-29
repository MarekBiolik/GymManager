import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Web talks to Supabase via supabase-js only — no KMP binary (ADR 0006).
export default defineConfig({
  plugins: [react()],
});
