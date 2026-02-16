package com.example.budgiet

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.budgiet.ui.NewTransactionForm
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.PlainToolTipBox
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgietTheme {
                MainPage(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(modifier: Modifier = Modifier) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

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
            Text("Press the '+ Transaction' button to get started.")
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight()
                .windowInsetsPadding(WindowInsets.statusBars),
            sheetState = sheetState,
            onDismissRequest = {
                @Suppress("AssignedValueIsNeverRead")
                showBottomSheet = false
            },
        ) {
            NewTransactionForm()
        }
    }
}

// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions
// TODO: semantic test these 3 functions

/* FIXME: I want to move all of this to Price.kt,
 * but for some reason it has a bug that the loading indicator shows up until the recentlyUsedCurrencies list is modified.
 * This only happens when i try to put it in Price.kt, but not when it is in MainActivity.kt. */
private val recentlyUsedCurrencies: MutableState<Result<SnapshotStateList<String>>?> = mutableStateOf(null)
private const val RECENT_CURRENCIES_FILE_NAME = "recentCurrencies.txt"
private const val RECENT_CURRENCIES_LOG_TAG = "RecentlyUsedCurrencies"

/** Returns an ordered [List] of **currency codes**, sorted by *most recent use*.
 *
 * The return value tells the state of the data:
 *  * **`null`**: The data is still being loaded.
 *  * **[Result.Err]**: There was an error loading the data.
 *  * **[Result.Ok]**: The data finished loading successfully.
 *
 *  Since this is a [Composable] with an internal [MutableState],
 *  changes in the state will propagate to the caller and it will be recomposed,
 *  even if this function itself does not return a [MutableState]. */
@Composable
fun getRecentlySelectedCurrencies(): State<Result<List<String>>?> {
    if (recentlyUsedCurrencies.value == null) {
        // Load ordered currencies from storage.

        val context = LocalContext.current
        val result = rememberWork(recentlyUsedCurrencies) {
            File(context.filesDir, RECENT_CURRENCIES_FILE_NAME)
                // Read the entirety of the file to move around the elements.
                .readText()
                .split('\n')
                // Last element will always be empty because the file always ends with newLine (unless it is empty).
                .dropLast(1)
                .toMutableStateList()
        }

        if (result.value is Result.Err) {
            Log.e(RECENT_CURRENCIES_LOG_TAG, "Error reading recent currencies from storage: ${(result.value as Result.Err).error}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    return recentlyUsedCurrencies as State<Result<List<String>>?>
}

/** Removes (clears) all currencies from the *ordered list* in memory and from the file in storage. */
fun Context.clearRecentlyUsedCurrencies() {
    // Clear in memory
    recentlyUsedCurrencies.value = Result.Ok(mutableStateListOf())
    // Clear in storage
    dispatchWork {
        Log.i(RECENT_CURRENCIES_LOG_TAG, "Clear recently used currencies in storage.")
        val file = File(this.filesDir, RECENT_CURRENCIES_FILE_NAME)
        file.writeText("")
    }
}

/** Marks a [Currency][java.util.Currency] as recently used (a.k.a. it was just selected),
 * moving it to the front of the [List] of recent currencies,
 * which is **sorted** by latest use.
 *
 * This function will also write to the [File] in storage the same content as the [List] in memory.
 *
 * See [getRecentlySelectedCurrencies] to read from this [List]. */
fun Context.markCurrencyRecentlyUsed(currencyCode: String) {
    when (recentlyUsedCurrencies.value) {
        null, is Result.Err -> recentlyUsedCurrencies.value = Result.Ok(mutableStateListOf())
        is Result.Ok -> { }
    }
    val orderedCurrencies = recentlyUsedCurrencies.value!!.getOkOrNull()!! as MutableList<String>

    Log.i(RECENT_CURRENCIES_LOG_TAG, "Moving Currency \"$currencyCode\" to the front of MutableStateList in memory.")

    // Apply to mutable list in memory
    // Find currency in the argument
    when (val idx = orderedCurrencies.indexOf(currencyCode)) {
        // The currency was already first in the list; do nothing.
        0 -> { }
        // Currency was not found in the List, so it must be prepended.
        -1 -> orderedCurrencies.add(0, currencyCode)
        // Remove target currency (arg) from the List, and put it in the front.
        else -> {
            orderedCurrencies.add(0, orderedCurrencies.removeAt(idx))
        }
    }

    // Apply to storage
    dispatchWork {
        Log.i(RECENT_CURRENCIES_LOG_TAG, "Moving Currency \"$currencyCode\" to the front of File in storage.")
        val file = File(this.filesDir, RECENT_CURRENCIES_FILE_NAME)
        file.createNewFile()
        // Write the modified list
        file.writeText(orderedCurrencies.joinToString(separator = "", truncated = "") { code -> "$code\n" })
    }
}


class Location(
    val id: UInt,
    val name: String,
    val address: String,
)
fun getRecentLocations(start: UInt = 0u, len: UInt = 10u): List<Location> {
    // Returns a list of bogus locations for now
    return List(len.toInt()) { i ->
        val id = i.toUInt() + start
        if (id % 2u == 0u) {
            Location(id = id, name = "Chipotle", "$id$id$id Main Street, Bronx NY")
        } else {
            Location(id = id, name = "Aldi", "$id$id$id IsNuts Lane, Los Angeles CA")
        }
    }
}
fun getLocationsSearchPage(query: CharSequence, start: UInt, len: UInt): List<Location> {
    // Returns a list of bogus locations for now
    return if (query.isEmpty()) {
        listOf()
    } else {
        List(len.toInt()) { i ->
            val id = i.toUInt() + start
            if (id % 2u == 0u) {
                Location(id = id, name = query.toString(), "$id$id$id Main Street, Bronx NY")
            } else {
                Location(id = id, name = query.toString(), "$id$id$id IsNuts Lane, Los Angeles CA")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainPagePreview() {
    BudgietTheme {
        MainPage()
    }
}
