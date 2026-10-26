# GymManager

Personal diet tracker for logging meals and macros (kcal, protein, carbs, fat). Mobile-first web UI with a small Express + SQLite API.

The **UI is Polish** for now; product search can pull nutrition data from [Open Food Facts](https://world.openfoodfacts.org/) (ODbL). Attribution and licensing notes belong in docs, not in the in-app chrome.

## Stack

- **web** — Vite + React + TypeScript
- **server** — Express + SQLite
- **data** — SQLite file at `data/gymmanager.db` (gitignored; created on first API start)

## Run locally

Install dependencies (from the repo root):

```bash
npm run install:all
```

Start the API in one terminal:

```bash
npm run dev:server
```

API listens on `http://127.0.0.1:3001`. Database path: `data/gymmanager.db`.

Start the web app in another terminal:

```bash
npm run dev:web
```

Open the URL printed by Vite (usually `http://127.0.0.1:5173`). Dev proxy forwards `/api` to the server. You can also open that URL on a phone on the same network.

## Project layout

| Path | Role |
|------|------|
| `web/` | React UI |
| `server/` | REST API |
| `data/` | Local SQLite database |
