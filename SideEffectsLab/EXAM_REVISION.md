# Side Effects — 2-minute cheat sheet

- **LaunchedEffect(key):** suspend work triggered by entering Composition; key change cancels and restarts it.
- **rememberCoroutineScope():** start coroutine from a click/callback; cancelled when composable leaves Composition.
- **rememberUpdatedState(value):** read latest callback/value inside a long-running effect without restarting it.
- **DisposableEffect(key):** register a listener; always remove it in `onDispose`.
- **SideEffect:** runs after successful composition; synchronize Compose state with non-Compose code.
- **produceState(initial, key):** run a producer and expose its result as Compose `State`; producer cancels/restarts on key changes.
- **derivedStateOf:** use when inputs change frequently but the UI-relevant result changes less often.
- **snapshotFlow { stateRead }:** observe Compose state as a cold Flow; collect inside a coroutine.

## Remember the key rules
1. If a changed value should restart the effect, make it a key.
2. If a changing callback should be read fresh but must not restart the effect, use `rememberUpdatedState`.
3. Never call `LaunchedEffect` inside `onClick`; use `rememberCoroutineScope().launch`.
4. Never register a listener in `DisposableEffect` without cleanup.
5. Avoid side effects directly in the composable body; recomposition can run more than once or be discarded.
6. `derivedStateOf` is not needed for every computed value. Use it only when it reduces meaningful updates.

## Mini quiz
1. Which API starts a coroutine from a button click? A) SideEffect B) rememberCoroutineScope C) derivedStateOf
2. Which API removes a listener when the composable leaves? A) DisposableEffect B) snapshotFlow C) produceState
3. Which API lets an ongoing effect read the newest callback without restarting? A) LaunchedEffect(callback) B) rememberUpdatedState C) SideEffect
4. Which API turns snapshot state reads into a Flow? A) snapshotFlow B) produceState C) rememberCoroutineScope
5. When does SideEffect run? A) During every attempted composition B) After successful composition is applied C) Only when key changes
6. When should derivedStateOf be considered? A) Every time you compute a value B) When input changes more often than the UI-relevant result C) Only for strings
7. What happens to LaunchedEffect when its key changes? A) Old coroutine is cancelled and effect restarts B) It always keeps running C) App crashes
8. Why is `LaunchedEffect(Unit)` inside `onClick` wrong? A) It is a composable API and cannot be called from an ordinary callback B) Unit is not allowed C) Click callbacks cannot run code

### Answer key
1-B, 2-A, 3-B, 4-A, 5-B, 6-B, 7-A, 8-A.
