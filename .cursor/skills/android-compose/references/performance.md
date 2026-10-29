# Android Compose performance (trimmed)

## Prefer

- Stable keys in lazy lists; avoid index-only keys when items reorder
- Skip unnecessary recomposition: read state at the lowest level that needs it
- Prefer `Modifier` chains and remember only when measuring or expensive derived work (follow project Compose guidance; do not sprinkle `remember` by habit)
- Images / heavy work off the main composition path when scaffold adds them

## Avoid

- Allocating large collections or doing domain math inside `Composable` every frame
- Nested scrollables without nested-scroll coordination
- Unbounded `LazyColumn` inside another vertical scroll without height constraints
