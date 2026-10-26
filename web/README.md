# GymManager web

Mobile-first diet tracker UI (Vite + React + TypeScript). User-facing strings are Polish; source comments and docs are English.

Run the API first (`../server` or `npm run dev:server` from the repo root), then:

```bash
npm install
npm run dev
```

Vite proxies `/api` to `http://127.0.0.1:3001`. Open the URL from the terminal on a PC or on a phone on the same network.

Polish UI copy lives in `src/i18n/pl.ts` so an English locale can be added later without hunting through components.
