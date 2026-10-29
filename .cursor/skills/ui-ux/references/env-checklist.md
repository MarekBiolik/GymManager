# UI/UX environment checklist

UX work is mostly docs/specs/prototypes — no special native toolchain required for contracts.

## Useful

| Tool | When |
|------|------|
| **Browser tools** | Review web flows / a11y smoke on running `apps/web` |
| **Android emulator** | Optional visual check after platform implement (#50) — not required to author a UX contract |
| **Design references** | External HIG / a11y docs via `research` when facts matter |

## Outputs (prefer in-repo)

| Artifact | Where |
|----------|--------|
| Short UX contract / flow notes | Issue comment, PR, or `docs/specs/` when durable |
| Throwaway prototype | Per `prototype` skill — do not promote to design-system-of-record early |

## Not required to start

- Final design tokens / branding package (phase 6)
- Closing OQ-08
- Mac / iOS Simulator (#49)
