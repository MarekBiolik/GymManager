# Flows and layout intent

## One job per section

Each screen or major section should state one job, one primary headline, and usually one short supporting line plus one CTA group. Avoid packing unrelated modules into the first viewport.

## Block / bento

Follow [Fitness/Gym visual style](../../../../docs/specs/visual-style-fitness-gym.md): large rectangular blocks, clear hierarchy, cards only when they hold a real interaction or navigable unit.

## Cross-platform

Describe intent once; let `android-compose` / `web-react` / `ios-swiftui` implement natively ([ADR 0003](../../../../docs/decisions/0003-native-ui-per-platform.md)). Module shell navigation remains [OQ-08](../../../../docs/open-questions.md) until decided.

## Empty / error / loading

Name these states in the contract. Errors need cause + next action. Offline write blocked under MVP sync — say so when relevant ([ADR 0008](../../../../docs/decisions/0008-sync-strategy-online-first.md)).
