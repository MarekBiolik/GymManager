#!/usr/bin/env node
/**
 * Thin design-token generator (ADR 0010).
 * Reads design/tokens/**, resolves {refs}, emits Compose / SwiftUI / CSS+TS.
 * Prefer semantic tokens in UI; do not hand-edit generated outputs.
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const designRoot = path.resolve(__dirname, "..");
const repoRoot = path.resolve(designRoot, "..");
const tokensRoot = path.join(designRoot, "tokens");
const checkOnly = process.argv.includes("--check");

function walkJsonFiles(dir, acc = []) {
  if (!fs.existsSync(dir)) return acc;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walkJsonFiles(full, acc);
    else if (entry.name.endsWith(".json") && entry.name !== "$metadata.json") acc.push(full);
  }
  return acc;
}

function deepMerge(a, b) {
  if (Array.isArray(a) || Array.isArray(b)) return b;
  if (typeof a !== "object" || a === null || typeof b !== "object" || b === null) return b;
  const out = { ...a };
  for (const [k, v] of Object.entries(b)) {
    out[k] = k in out ? deepMerge(out[k], v) : v;
  }
  return out;
}

function isTokenLeaf(node) {
  return node && typeof node === "object" && "$value" in node;
}

function flatten(node, prefix = [], out = new Map()) {
  if (isTokenLeaf(node)) {
    out.set(prefix.join("."), node);
    return out;
  }
  if (!node || typeof node !== "object") return out;
  for (const [k, v] of Object.entries(node)) {
    flatten(v, [...prefix, k], out);
  }
  return out;
}

function resolveValue(raw, lookup, stack = []) {
  if (typeof raw !== "string") return raw;
  const m = raw.match(/^\{([^}]+)\}$/);
  if (!m) return raw;
  const ref = m[1];
  if (stack.includes(ref)) throw new Error(`Circular token ref: ${[...stack, ref].join(" -> ")}`);
  const target = lookup.get(ref);
  if (!target) throw new Error(`Unresolved token ref: {${ref}}`);
  return resolveValue(target.$value, lookup, [...stack, ref]);
}

function cssName(pathKey) {
  return `--${pathKey.replace(/\./g, "-")}`;
}

function pascalName(pathKey) {
  return pathKey
    .split(/[.-]/)
    .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
    .join("");
}

function camelName(pathKey) {
  const p = pascalName(pathKey);
  return p.charAt(0).toLowerCase() + p.slice(1);
}

function parsePx(value) {
  if (typeof value !== "string") return null;
  const m = value.match(/^(-?\d+(?:\.\d+)?)px$/);
  return m ? Number(m[1]) : null;
}

function parseMs(value) {
  if (typeof value !== "string") return null;
  const m = value.match(/^(-?\d+(?:\.\d+)?)ms$/);
  return m ? Number(m[1]) : null;
}

function hexToCompose(hex) {
  const h = hex.replace("#", "");
  if (h.length !== 6) throw new Error(`Expected #RRGGBB, got ${hex}`);
  return `Color(0xFF${h.toUpperCase()})`;
}

function hexToSwift(hex) {
  const h = hex.replace("#", "");
  const r = parseInt(h.slice(0, 2), 16) / 255;
  const g = parseInt(h.slice(2, 4), 16) / 255;
  const b = parseInt(h.slice(4, 6), 16) / 255;
  const fmt = (n) => n.toFixed(3);
  return `Color(red: ${fmt(r)}, green: ${fmt(g)}, blue: ${fmt(b)})`;
}

// Load primitives + dark semantic (preferred) + shared semantic non-color if any.
// Skip color.light.json for active emit (OQ-09 stub).
const files = walkJsonFiles(tokensRoot).filter(
  (f) => !f.replace(/\\/g, "/").endsWith("semantic/color.light.json"),
);

let tree = {};
for (const file of files) {
  const json = JSON.parse(fs.readFileSync(file, "utf8"));
  tree = deepMerge(tree, json);
}

const flat = flatten(tree);
const resolved = new Map();
for (const [key, leaf] of flat.entries()) {
  resolved.set(key, {
    type: leaf.$type,
    value: resolveValue(leaf.$value, flat),
    description: leaf.$description,
  });
}

// Public emit set: prefer semantic roles (color.bg.*, color.text.*, …) + scales used by UI.
const emitKeys = [...resolved.keys()].filter((k) => {
  if (k.startsWith("color.bg.") || k.startsWith("color.text.") || k.startsWith("color.action.")) return true;
  if (k.startsWith("color.border.") || k.startsWith("color.feedback.")) return true;
  if (k.startsWith("space.") || k.startsWith("radius.") || k.startsWith("type.") || k.startsWith("motion.")) return true;
  return false;
}).sort();

const outFiles = {
  android: path.join(
    repoRoot,
    "apps/android/src/main/java/com/gymmanager/android/theme/GeneratedTokens.kt",
  ),
  ios: path.join(repoRoot, "apps/ios/GymManager/Theme/GeneratedTokens.swift"),
  css: path.join(repoRoot, "apps/web/src/styles/tokens.css"),
  ts: path.join(repoRoot, "apps/web/src/theme/tokens.ts"),
};

function emitKotlin() {
  const lines = [
    "// GENERATED FILE — do not edit by hand. Run: pnpm --filter @gymmanager/design generate",
    "package com.gymmanager.android.theme",
    "",
    "import androidx.compose.ui.graphics.Color",
    "import androidx.compose.ui.unit.Dp",
    "import androidx.compose.ui.unit.TextUnit",
    "import androidx.compose.ui.unit.dp",
    "import androidx.compose.ui.unit.sp",
    "",
    "/** Semantic design tokens from design/tokens (Fitness/Gym OLED dark). */",
    "object GymTokens {",
  ];
  for (const key of emitKeys) {
    const { type, value } = resolved.get(key);
    const name = pascalName(key);
    if (type === "color" && typeof value === "string" && value.startsWith("#")) {
      lines.push(`    val ${name}: Color = ${hexToCompose(value)}`);
    } else if (type === "dimension") {
      const px = parsePx(value);
      if (px != null) {
        if (key.includes("size") || key.includes("lineHeight")) {
          lines.push(`    val ${name}: TextUnit = ${px}.sp`);
        } else {
          lines.push(`    val ${name}: Dp = ${px}.dp`);
        }
      }
    } else if (type === "duration") {
      const ms = parseMs(value);
      if (ms != null) lines.push(`    val ${name}: Int = ${ms}`);
    } else if (type === "fontWeight") {
      lines.push(`    val ${name}: Int = ${Number(value)}`);
    }
  }
  lines.push("}", "");
  return lines.join("\n");
}

function emitSwift() {
  const lines = [
    "// GENERATED FILE — do not edit by hand. Run: pnpm --filter @gymmanager/design generate",
    "import SwiftUI",
    "",
    "/// Semantic design tokens from design/tokens (Fitness/Gym OLED dark).",
    "enum GymTokens {",
  ];
  for (const key of emitKeys) {
    const { type, value } = resolved.get(key);
    const name = camelName(key);
    if (type === "color" && typeof value === "string" && value.startsWith("#")) {
      lines.push(`    static let ${name}: Color = ${hexToSwift(value)}`);
    } else if (type === "dimension") {
      const px = parsePx(value);
      if (px != null) lines.push(`    static let ${name}: CGFloat = ${px}`);
    } else if (type === "duration") {
      const ms = parseMs(value);
      if (ms != null) lines.push(`    static let ${name}: Double = ${ms / 1000}`);
    } else if (type === "fontWeight") {
      lines.push(`    static let ${name}: Int = ${Number(value)}`);
    }
  }
  lines.push("}", "");
  return lines.join("\n");
}

function emitCss() {
  const lines = [
    "/* GENERATED FILE — do not edit by hand. Run: pnpm --filter @gymmanager/design generate */",
    ":root {",
  ];
  for (const key of emitKeys) {
    const { type, value } = resolved.get(key);
    const name = cssName(key);
    if (type === "color" || type === "dimension" || type === "duration" || type === "fontWeight") {
      const v = Array.isArray(value) ? value.join(", ") : value;
      lines.push(`  ${name}: ${v};`);
    }
  }
  lines.push("}", "");
  return lines.join("\n");
}

function emitTs() {
  const entries = [];
  for (const key of emitKeys) {
    const { type, value } = resolved.get(key);
    if (type === "color" || type === "dimension" || type === "duration" || type === "fontWeight") {
      entries.push(`  ${JSON.stringify(key)}: ${JSON.stringify(value)}`);
    }
  }
  return [
    "// GENERATED FILE — do not edit by hand. Run: pnpm --filter @gymmanager/design generate",
    "export const tokens = {",
    entries.join(",\n"),
    "} as const;",
    "",
    "export type TokenPath = keyof typeof tokens;",
    "",
  ].join("\n");
}

const generated = {
  [outFiles.android]: emitKotlin(),
  [outFiles.ios]: emitSwift(),
  [outFiles.css]: emitCss(),
  [outFiles.ts]: emitTs(),
};

let dirty = false;
for (const [file, content] of Object.entries(generated)) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const existing = fs.existsSync(file) ? fs.readFileSync(file, "utf8") : null;
  if (existing !== content) {
    dirty = true;
    if (!checkOnly) fs.writeFileSync(file, content, "utf8");
  }
}

if (checkOnly) {
  if (dirty) {
    console.error("Generated token outputs are out of date. Run: pnpm --filter @gymmanager/design generate");
    process.exit(1);
  }
  console.log("Token outputs are up to date.");
} else {
  console.log("Wrote:");
  for (const f of Object.keys(generated)) console.log(" -", path.relative(repoRoot, f));
}
