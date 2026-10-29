# Practice 9 — Side-effects: Theory Answers

Use these as short study answers. Rewrite them in your own words if your teacher expects individual phrasing.

## 1. What is a side effect?
A side effect is an action that changes or interacts with something outside the current composable's UI description, such as loading data, writing analytics, registering a listener, or changing an external object. A composable body can run many times during recomposition, and a composition attempt can be discarded, so calling side-effect code directly may repeat work or run work for UI that was never committed. Keep data loading in a ViewModel/repository and expose UI state to Compose; this follows unidirectional data flow: state flows down to the UI, events flow up.

## 2. Match the eight APIs
- Suspend work triggered by entering Composition: `LaunchedEffect`.
- Start a coroutine from a callback such as `onClick`: `rememberCoroutineScope`.
- Read the latest value inside an ongoing effect without restarting it: `rememberUpdatedState`.
- Register and later remove a listener: `DisposableEffect`.
- Send Compose state to non-Compose code after a successful composition: `SideEffect`.
- Convert Flow/LiveData/RxJava or asynchronous producer work to Compose State: `produceState` (for Flow, `collectAsState` is also commonly used, but the worksheet's listed API is `produceState`).
- Avoid recomposition for every input change when only a derived result matters: `derivedStateOf`.
- Convert snapshot state reads into a Flow: `snapshotFlow`.

## 3. LaunchedEffect(pulseRateMs)
It starts when this composable enters Composition. If `pulseRateMs` changes from 3000 to 1000, the old coroutine is cancelled and a new one starts with the new delay. When the composable leaves Composition, the coroutine is cancelled. `isActive` reports whether the coroutine's job is still active; cancellation stops the loop at suspension points. The MiniComposition model expresses this as the old effect being cancelled when its key changes or it leaves.

## 4. LaunchedEffect(true)
A constant key means the effect does not restart when ordinary parameters change, so it can keep a stale captured `userId`; this is why a constant-key effect can resemble a suspicious `while(true)`. Use `LaunchedEffect(userId) { api.sendHello(userId) }` if a changed user ID should trigger a new request. A constant key is appropriate for one-time work tied to a screen's presence, such as waiting for a splash timeout; use `rememberUpdatedState` if that ongoing effect needs the newest callback.

## 5. Snackbar in a click callback
`LaunchedEffect` is a composable API and is not called from an ordinary `onClick` lambda. `rememberCoroutineScope()` returns a coroutine scope tied to the composable's Composition; its launched coroutines are cancelled when that composable leaves Composition. Correct code: `val scope = rememberCoroutineScope(); Button(onClick = { scope.launch { snackbarHostState.showSnackbar("Hi") } }) { Text("Show") }`. `showSnackbar` is `suspend`, so it must run inside a coroutine. Use `LaunchedEffect` for work caused by the composable entering Composition and `rememberCoroutineScope` for work caused by a user event.

## 6. rememberUpdatedState
An effect can capture the callback object that existed when it started; a later recomposition may pass a new callback while the old effect is still waiting. `rememberUpdatedState(onTimeout)` provides a stable state holder whose value is updated to the latest callback, so the effect can call `currentOnTimeout()` after the delay. Making `onTimeout` an effect key would cancel and restart the delay whenever the lambda identity changes. The model `captured -> v1`, `updatedState -> v3` shows the captured reference stays old while the state holder reads the latest value.

## 7. DisposableEffect and cleanup
A listener/observer retains a reference and may continue receiving events after the UI no longer needs it. Without cleanup, this can cause duplicate callbacks, stale behavior, or a memory leak. `onDispose { ... }` must be the final statement in a `DisposableEffect` block and removes the same observer that was registered. An empty `onDispose` is misleading if nothing needs cleanup; use `LaunchedEffect` for coroutine work or `SideEffect` for post-commit synchronization instead. Correct pattern:
```kotlin
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event -> /* handle event */ }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

## 8. MiniDisposable output
Expected four lines:
```text
A.addObserver(observer)
A.removeObserver(observer)
B.addObserver(observer)
B.removeObserver(observer)
```
The second `screen("A")` has the same key, so it does not create a second effect. When key changes from A to B, the old effect is disposed first, then the new B observer is added. Leaving Composition disposes B.

## 9. SideEffect
`SideEffect` runs after a successful composition has been applied. A direct call in the composable body can happen during an attempted composition that is later discarded, so the external analytics object might be updated with UI state that never committed. In the attempt model: direct call can happen even with `commits=false`; `SideEffect` does not run when `commits=false`; it runs when `commits=true`. `SideEffect` has no keys; it runs after each successful recomposition of the composable.

## 10. produceState
`produceState` creates a Compose `State` and launches a producer to update its `value`. The producer starts when the composable enters Composition and is cancelled when it leaves; if a key changes, the old producer is cancelled and a new one starts. State conflates equal values, so assigning a value equal to the current value does not cause an unnecessary recomposition. A composable that returns a value uses a lower-case name by convention, such as `loadNetworkImage`.

## 11. Implement produceState with remember + LaunchedEffect
```kotlin
@Composable
fun <T> myProduceState(
    initialValue: T,
    key: Any?,
    producer: suspend MutableState<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffect(key) {
        result.producer()
    }
    return result
}
```
`remember` keeps the state object across recompositions, and `LaunchedEffect` runs the suspending producer. When `key` changes, the previous producer coroutine is cancelled and the new producer starts. In a more general implementation, reset behavior may need to be specified explicitly; the simple worksheet model preserves the existing state until the producer writes another value.

## 12. derivedStateOf and scroll index
Reading `firstVisibleItemIndex` directly makes the composable react to every index change, even though the button only cares whether the index is greater than zero. `derivedStateOf` caches the derived Boolean and updates consumers only when that Boolean changes; `remember` keeps the derived state object stable. For `[0,0,1,2,3,4,5,3,1,0]`, direct index observation sees nine transitions after the initial value (duplicate 0 does not change state); the derived Boolean changes only at 0→positive and positive→0. Including the initial UI composition, the worksheet model reports 3 derived-state recompositions.

## 13. derivedStateOf for full name
`derivedStateOf` is unnecessary for `"$firstName $lastName"` because the result normally changes whenever either input changes, so it adds overhead without reducing recompositions. Use `val fullName = "$firstName $lastName"`. Use `derivedStateOf` when inputs change more frequently than the UI-relevant result, for example an index changing from 1 to 2 to 3 while a button visibility Boolean stays `true`. Caution: it is not a general-purpose wrapper for every derived value; use it only when it meaningfully reduces updates.

## 14. Waveform and derivedStateOf
Use `derivedStateOf` if waveform-related inputs update frequently but the UI only needs a less frequently changing derived result, such as a threshold Boolean. Do not use it when amplitudes are already precomputed and don't need derived state; when the waveform is static; or when the waveform is manually updated by `LaunchedEffect` or `animate*` and there is no high-frequency input/result mismatch to optimize. In those cases, derived state adds unnecessary complexity/overhead.

## 15. snapshotFlow analytics
`snapshotFlow { ... }` observes Compose snapshot state and emits a Flow of values when the result changes. It is cold because the block is observed only while a collector is active. `map` transforms each index into `index > 0`; `distinctUntilChanged` removes consecutive duplicate Boolean values; `filter { it }` keeps only `true` values. `collect` is a suspending operation, so it belongs inside a coroutine such as `LaunchedEffect`. For indices `0,0,1,2,5,3,0,0,1,4`, the Boolean sequence is `false,false,true,true,true,true,false,false,true,true`; after distinct and filter, two `true` events remain, so two analytics events are sent.

## 16. Effect key restart table
Assuming restart count excludes the initial launch:
- `keys = []`: 0 restarts; the effect does not restart when `owner` or `tick` changes, so it may use stale values.
- `keys = [owner]`: 1 restart when owner changes from A to B at tick 3; it ignores tick changes.
- `keys = [owner, tick]`: restarts whenever tick changes and when owner changes; with ticks 1→4 this is 3 tick-driven restarts, with owner change coinciding at tick 3. If the model treats owner and tick as separate updates, count each key change event; the important point is that this is more restarts than necessary when only owner should control the effect.
Correct choice depends on which values the effect must track; include every value that should trigger restart, and no others.

## 17. Lifecycle keys
`lifecycleOwner` should be a `DisposableEffect` key because the observer must be detached from the old lifecycle and attached to the new one if the owner changes. `onStart` and `onStop` can be wrapped in `rememberUpdatedState` and read inside the observer; they do not need to be keys because callback changes should not re-register the observer. If `lifecycleOwner` is not a key, the observer may remain registered to the old lifecycle. Rule: effect keys should include values whose change requires the effect to be disposed/restarted; use `rememberUpdatedState` for changing values that should be read without restarting.

## 18. Four errors in ChatScreen
1. `viewModel.markOpened(userId)` is a side effect directly in the composable body and may run repeatedly. Move it to `LaunchedEffect(userId)` or handle it in the ViewModel's event/data flow.
2. `LaunchedEffect(true)` captures `onClose`; if the callback changes, the effect still holds the old one. Use `rememberUpdatedState(onClose)` and call the current callback after the delay, or key the effect by a value that should restart it.
3. `DisposableEffect(owner)` registers an observer but does not return `onDispose`; remove the observer in cleanup.
4. `LaunchedEffect(Unit)` is called inside `onClick`, where a composable function cannot be invoked. Use `scope.launch { send() }` with `rememberCoroutineScope()`.

One corrected pattern:
```kotlin
@Composable
fun ChatScreen(userId: String, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val currentOnClose by rememberUpdatedState(onClose)
    val observer = remember {
        LifecycleEventObserver { _, _ -> /* handle lifecycle events */ }
    }
    LaunchedEffect(userId) { viewModel.markOpened(userId) }
    LaunchedEffect(Unit) { delay(5_000); currentOnClose() }
    DisposableEffect(owner, observer) {
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Button(onClick = { scope.launch { send() } }) { Text("Send") }
}
```

## 19. Eight API summary
| API | When it runs | Cleanup | What restarts/updates it |
|---|---|---|---|
| `LaunchedEffect` | On entering Composition / key change | Coroutine cancelled on key change or leaving Composition | Keys change |
| `rememberCoroutineScope` | Scope is remembered for the composable; launch it from callbacks | Launched coroutines cancelled when composable leaves Composition | Scope instance is tied to Composition; not a keyed effect |
| `rememberUpdatedState` | State holder is created/remembered and its value updates on recomposition | No explicit cleanup | Latest value updates; does not restart an effect by itself |
| `DisposableEffect` | On entering Composition and when keys change | `onDispose` | Keys change or composable leaves |
| `SideEffect` | After each successful composition is applied | No cleanup | Every successful recomposition; no keys |
| `produceState` | Producer starts on entering Composition | Producer coroutine cancelled on key change/leave | Keys change |
| `derivedStateOf` | Derived result recalculates when dependencies change | No explicit cleanup | Dependencies; consumers update when derived result changes |
| `snapshotFlow` | Flow block observed when collected | Collection cancelled with its coroutine | Snapshot state read by block changes; collector controls lifetime |

## Example use cases
- `LaunchedEffect`: load data for a screen or run a splash timer.
- `rememberCoroutineScope`: show a snackbar after a button tap.
- `rememberUpdatedState`: call the latest navigation callback after a delay.
- `DisposableEffect`: attach/detach a lifecycle observer.
- `SideEffect`: copy committed Compose state into analytics SDK configuration.
- `produceState`: show Loading/Success/Error while loading a remote image.
- `derivedStateOf`: show “back to top” only when scroll index is greater than zero.
- `snapshotFlow`: send an analytics event when the user scrolls past the first item.
