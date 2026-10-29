package uz.ttpu.sideeffectslab

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Stable
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "SideEffectsLab"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { SideEffectsApp() } }
    }
}

private enum class LabScreen(val label: String) {
    SPLASH("1. Splash"), SNACKBAR("2. Snackbar"), LIFECYCLE("3. Lifecycle"),
    ANALYTICS("4. SideEffect"), SCROLL("5. Scroll / Flow"), PRODUCE("6. produceState")
}

@Composable
private fun SideEffectsApp() {
    var selected by remember { mutableStateOf(LabScreen.SPLASH) }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("SideEffectsLab", style = MaterialTheme.typography.headlineMedium)
            Text("Practice 9 — Jetpack Compose Side Effects")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LabScreen.values().take(3).forEach { item ->
                    Button(onClick = { selected = item }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text(item.label, maxLines = 2) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LabScreen.values().drop(3).forEach { item ->
                    Button(onClick = { selected = item }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text(item.label, maxLines = 2) }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            key(selected) {
                when (selected) {
                    LabScreen.SPLASH -> SplashDemo()
                    LabScreen.SNACKBAR -> SnackbarDemo()
                    LabScreen.LIFECYCLE -> LifecycleDemo()
                    LabScreen.ANALYTICS -> SideEffectDemo()
                    LabScreen.SCROLL -> ScrollDemo()
                    LabScreen.PRODUCE -> ProduceStateDemo()
                }
            }
        }
    }
}

@Composable
private fun SplashDemo() {
    var version by remember { mutableIntStateOf(0) }
    var restartOnLambdaChange by remember { mutableStateOf(false) }
    val currentCallback by rememberUpdatedState(newValue = { Log.d(TAG, "updated callback: v$version") })
    val capturedVersion = remember { version }
    LaunchedEffect(Unit) {
        while (true) { delay(500); version++ }
    }
    LaunchedEffect(if (restartOnLambdaChange) version else Unit) {
        delay(2_000)
        Log.d(TAG, "captured callback: v$capturedVersion")
    }
    LaunchedEffect(Unit) {
        delay(2_000)
        currentCallback()
    }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Task 1 — Splash screen", style = MaterialTheme.typography.titleLarge)
        Text("Version counter: v$version (updates every 500 ms)")
        Text("Captured callback logs the initial version. rememberUpdatedState reads the latest callback without restarting the timer.")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = restartOnLambdaChange, onCheckedChange = { restartOnLambdaChange = it })
            Text("Restart timer whenever callback version changes")
        }
        Text("Open Logcat and filter by $TAG. Wait at least 2 seconds to see the callbacks.")
        Text("Expected: captured callback logs its captured version; updated callback logs the latest version.")
    }
}

@Composable
private fun SnackbarDemo() {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var clicks by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Task 2 — Snackbar", style = MaterialTheme.typography.titleLarge)
        SnackbarHost(hostState)
        Text("Button taps: $clicks")
        Button(onClick = {
            clicks++
            scope.launch { hostState.showSnackbar("Hello from rememberCoroutineScope() — tap $clicks") }
        }) { Text("Show snackbar") }
        Text("The click callback starts a coroutine with scope.launch. LaunchedEffect is for work triggered by entering Composition, not directly for a click callback.")
    }
}

@Composable
private fun LifecycleDemo() {
    var visible by remember { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Task 3 — Lifecycle logger", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = visible, onCheckedChange = { visible = it })
            Spacer(Modifier.width(8.dp)); Text("Show lifecycle observer")
        }
        if (visible) LifecycleObserverContent()
        Text("Toggle the switch and check Logcat ($TAG). Each addObserver should have a matching removeObserver.")
    }
}

@Composable
private fun LifecycleObserverContent() {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_STOP) {
                Log.d(TAG, "Lifecycle event: $event")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        Log.d(TAG, "addObserver")
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            Log.d(TAG, "removeObserver")
        }
    }
    Text("Observer is attached to the current LifecycleOwner.")
}

private object DemoAnalytics {
    var userType: String = "guest"
}

@Composable
private fun SideEffectDemo() {
    var userType by remember { mutableStateOf("guest") }
    val uiCommitCount = remember { intArrayOf(0) }
    SideEffect {
        DemoAnalytics.userType = userType
        uiCommitCount[0]++
        Log.d(TAG, "SideEffect committed: userType=${DemoAnalytics.userType}, commit=${uiCommitCount[0]}")
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Task 4 — SideEffect + analytics", style = MaterialTheme.typography.titleLarge)
        Text("Selected user type: $userType")
        Text("Commit counter is logged in Logcat (filter: $TAG).")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("guest", "member", "premium").forEach { type ->
                Button(onClick = { userType = type }) { Text(type) }
            }
        }
        Text("SideEffect runs after a successful composition is applied, so the external analytics object is not updated by a discarded composition attempt.")
    }
}

@Stable
private class RecompositionCounter {
    private val value = AtomicInteger(0)
    fun increment() { value.incrementAndGet() }
    fun current(): Int = value.get()
}

@Composable
private fun DirectIndexCounter(index: Int, counter: RecompositionCounter) {
    SideEffect { counter.increment() }
    Text("Direct index reader — index: $index")
}

@Composable
private fun DerivedBooleanCounter(show: Boolean, counter: RecompositionCounter) {
    SideEffect { counter.increment() }
    Text("derivedStateOf reader — show button: $show")
}

@Composable
private fun ScrollDemo() {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val showTopButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val scope = rememberCoroutineScope()
    var events by remember { mutableIntStateOf(0) }
    val directCounter = remember { RecompositionCounter() }
    val derivedCounter = remember { RecompositionCounter() }
    // Refresh the displayed external counters periodically; these counters are not Compose state,
    // so the SideEffect in each child does not create a recomposition loop.
    var refreshTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { delay(500); refreshTick++ }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> index > 0 }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                events++
                Log.d(TAG, "Scrolled past first item event #$events")
            }
    }
    Column(Modifier.fillMaxSize()) {
        Text("Task 5 — derivedStateOf + snapshotFlow", style = MaterialTheme.typography.titleLarge)
        // Reading refreshTick causes the counter labels to refresh without storing the counts in Compose state.
        Text("Recomposition counts (refresh $refreshTick): direct=${directCounter.current()}, derived=${derivedCounter.current()}")
        Text("Analytics events (crossing index 0 → >0): $events")
        DirectIndexCounter(index = listState.firstVisibleItemIndex, counter = directCounter)
        DerivedBooleanCounter(show = showTopButton, counter = derivedCounter)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { listState.animateScrollToItem(0) } }) { Text("Back to top") }
            if (showTopButton) Text("Top button is visible")
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items((0..99).toList()) { index ->
                ListItem(headlineContent = { Text("Item $index") }, supportingContent = { Text("Scroll to observe state and Flow updates") })
                HorizontalDivider()
            }
        }
        Text("snapshotFlow is collected in LaunchedEffect; distinctUntilChanged + filter(true) emits once each time you cross from item 0 to an index greater than 0.")
    }
}

private sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data class Success(val message: String) : ImageLoadState
    data class Error(val message: String) : ImageLoadState
}

private object FakeImageRepository {
    suspend fun load(url: String): ImageLoadState {
        delay(500)
        return if (url.trim().endsWith(".png", ignoreCase = true)) {
            ImageLoadState.Success("Loaded image URL: $url")
        } else {
            ImageLoadState.Error("URL must end with .png")
        }
    }
}

@Composable
private fun ProduceStateDemo() {
    var url by remember { mutableStateOf("https://example.com/photo.png") }
    val imageState by produceState<ImageLoadState>(initialValue = ImageLoadState.Loading, key1 = url) {
        Log.d(TAG, "producer started for $url")
        value = ImageLoadState.Loading
        try {
            value = FakeImageRepository.load(url)
        } finally {
            Log.d(TAG, "producer finished/cancelled for $url")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Task 6 — produceState", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Image URL (try .png or .jpg)") }, modifier = Modifier.fillMaxWidth())
        when (val state = imageState) {
            ImageLoadState.Loading -> CircularProgressIndicator()
            is ImageLoadState.Success -> Text("SUCCESS: ${state.message}")
            is ImageLoadState.Error -> Text("ERROR: ${state.message}")
        }
        Text("Changing the URL changes the key, cancels the previous producer and starts a new one. Identical State values are conflated and do not trigger extra recompositions.")
    }
}
