#!/usr/bin/env node
import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const isWin = process.platform === "win32";
const gradlew = path.join(root, isWin ? "gradlew.bat" : "gradlew");
const args = process.argv.slice(2);
const result = spawnSync(gradlew, args, { stdio: "inherit", cwd: root, shell: isWin });
process.exit(result.status ?? 1);
