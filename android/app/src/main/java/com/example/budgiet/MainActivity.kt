package com.example.budgiet

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgiet.ui.NewTransactionForm
import com.example.budgiet.ui.NewTransactionViewModel
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.PlainToolTipBox
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class MainActivity : ComponentActivity() {
    private val newTransactionViewModel by this.viewModels<NewTransactionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        UserIcons.load(this)
        setContent {
            BudgietTheme {
                MainPage(modifier = Modifier.fillMaxSize(), newTransactionViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(modifier: Modifier = Modifier, newTransactionViewModel: NewTransactionViewModel) {
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    val dismissBottomSheet: () -> Unit = {
        coroutineScope.launch {
            sheetState.hide()
            showBottomSheet = false
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            PlainToolTipBox(text = "Add new transaction record") {
                FloatingActionButton(onClick = { showBottomSheet = true }) {
                    Icon(painterResource(R.drawable.add_24px), "New Transaction")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Press the '+ Transaction' button to get started.",
                Modifier.padding(horizontal = 6.dp)
            )
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight()
                .windowInsetsPadding(WindowInsets.statusBars),
            sheetState = sheetState,
            onDismissRequest = dismissBottomSheet,
        ) {
            NewTransactionForm(
                viewModel = newTransactionViewModel,
                onDismiss = dismissBottomSheet,
            )
        }
    }
}

private var userIcons: Map<String, Int>? = null
private var userIconsJob: Job? = null
// Add extra layer to mutex (value -> job -> mutex) so that the operation to check if the value is initialized is not expensive.
private val userIconsJobMutex: Mutex = Mutex()
/** Note: should run in [WORKER_THREAD]. */
private fun initUserIcons(context: Context) {
    val icons = run {
        // Must be done this way, otherwise all the array elements will be 0.
        val badArray = context.resources.obtainTypedArray(R.array.usericons)
        val icons = arrayOfNulls<Int>(badArray.length())
        for (i in 0..<badArray.length()) {
            icons[i] = badArray.getResourceId(i, 0)
        }
        badArray.recycle()
        @Suppress("UNCHECKED_CAST")
        icons as Array<Int>
    }

    userIcons = icons.associateBy { res ->
        context.resources
            .getResourceName(res)
            // name starts with "${package_name}:drawable/usericon_".
            .split("/usericon_", limit = 2)
            .last()
    }
}

/** Provides an interface to access *icons selectable by the user* from the Android App's **Drawable** repository.
 * This maps the icon's **name** to the [Drawable ID][androidx.annotation.DrawableRes].
 *
 * The icons are loaded during the app's [MainActivity] initialization.
 * If an icon is accessed but the icon IDs are not finished loading, the access will return a default value. */
object UserIcons: Map<String, Int> {
    override val size: Int get() = userIcons?.size ?: 0
    override val keys: Set<String> get() = userIcons?.keys ?: setOf()
    override val values: Collection<Int> get() = userIcons?.values ?: listOf()
    override val entries: Set<Map.Entry<String, Int>> get() = userIcons?.entries ?: setOf()

    override fun isEmpty(): Boolean = userIcons?.isEmpty() ?: true
    override fun containsKey(key: String): Boolean = userIcons?.containsKey(key) ?: false
    override fun containsValue(value: Int): Boolean = userIcons?.containsValue(value) ?: false
    override fun get(key: String): Int? = userIcons?.get(key)

    /** Attempts to *load key-value* pairs (name to DrawableId) of the [UserIcons] stored in the App's package.
     *
     * Does nothing if it has already been called once before. */
    internal fun load(context: Context) {
        if (userIcons == null) {
            if (userIconsJobMutex.tryLock()) {
                if (userIconsJob == null) {
                    @OptIn(DelicateCoroutinesApi::class)
                    userIconsJob = GlobalScope.launch {
                        runWork {
                            initUserIcons(context)
                        }
                    }
                }
                userIconsJobMutex.unlock()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPagePreview() {
    BudgietTheme {
        MainPage(newTransactionViewModel = viewModel<NewTransactionViewModel>())
    }
}
