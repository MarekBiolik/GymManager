# GymManager API

Express + SQLite backend for the diet tracker.

Database file: `../data/gymmanager.db` (gitignored; created on first start).

```bash
npm install
npm run dev
```

API: `http://127.0.0.1:3001`

Food search may query [Open Food Facts](https://world.openfoodfacts.org/) (ODbL) when a product is not already stored locally.
