# SideEffectsLab — Practice 9

A Jetpack Compose learning app demonstrating the six practical tasks in **Practice 9 — Side-effects**.

## Requirements
- Android Studio with Android SDK 35 installed
- JDK 17
- Internet access for the first Gradle sync

## Open and run
1. Extract the ZIP file.
2. In Android Studio, choose **Open** and select the `SideEffectsLab` folder.
3. Allow Gradle sync to finish. If asked, install Android SDK 35.
4. Run the `app` configuration on an emulator or Android device.
5. Open **Logcat** and filter by `SideEffectsLab` for task logs.

## Practical tasks

1. **Splash:** `LaunchedEffect` waits two seconds; `rememberUpdatedState` allows a long-running effect to use the latest callback without restarting its timer. The checkbox demonstrates the problem with changing effect keys.
2. **Snackbar:** `rememberCoroutineScope` starts `showSnackbar()` from a button click. The coroutine scope is tied to the composable's presence in Composition.
3. **Lifecycle logger:** `DisposableEffect(lifecycleOwner)` registers a `LifecycleEventObserver`; `onDispose` removes it. Toggle the switch to see matching add/remove logs.
4. **SideEffect and analytics:** `SideEffect` writes the committed Compose state to a plain Kotlin analytics object and logs the number of successful commits. It does not run for a discarded composition attempt.
5. **Scroll:** `derivedStateOf` derives a Boolean for the “back to top” condition. `snapshotFlow` observes the list index and sends a demo analytics log only when the user crosses from index 0 to an index greater than 0. The app also shows index observations and Boolean changes for comparison.
6. **produceState:** a fake repository waits 500 ms and returns Success for URLs ending in `.png`, otherwise Error. Changing the URL restarts the producer; the `finally` log helps demonstrate completion/cancellation.

## API quick guide
- `LaunchedEffect(keys)`: launch suspend work when entering Composition; cancel/restart when keys change or the composable leaves.
- `rememberCoroutineScope()`: obtain a Composition-scoped coroutine scope for callbacks such as button clicks.
- `rememberUpdatedState(value)`: expose the latest value to an ongoing effect without restarting it.
- `DisposableEffect(keys)`: register a resource/listener and clean it up in `onDispose`.
- `SideEffect`: run after a successful composition is applied to synchronize state with non-Compose code.
- `produceState(initialValue, keys)`: convert asynchronous work or another source into Compose `State`.
- `derivedStateOf`: use when inputs change more often than the result that the UI needs.
- `snapshotFlow { ... }`: observe Compose snapshot state as a cold Flow.

## GitHub
Commit each practical task separately as requested by the worksheet. Suggested sequence:
- `Task 1: Splash and rememberUpdatedState`
- `Task 2: Snackbar and coroutine scope`
- `Task 3: Lifecycle observer cleanup`
- `Task 4: SideEffect analytics`
- `Task 5: derivedStateOf and snapshotFlow`
- `Task 6: produceState demo`

This project is a teaching demo. Verify it in your installed Android Studio/SDK; dependency resolution and device behavior can vary by environment.
