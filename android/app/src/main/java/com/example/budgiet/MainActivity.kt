package com.example.budgiet

import android.os.Bundle
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.budgiet.ui.NewTransactionForm
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.PlainToolTipBox
import java.util.Collections

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

private val orderedCurrencies: MutableList<String> = mutableStateListOf()

/** Returns an ordered [List] of **currency codes**, sorted by *most recent use*. */
// TODO: load from storage
fun getRecentlySelectedCurrencies(): List<String> = orderedCurrencies
/** Marks a [Currency][java.util.Currency] as recently used (a.k.a. it was just selected),
 * moving it to the front of the [List] of recent currencies,
 * which is **sorted** by latest use.
 *
 * See [getRecentlySelectedCurrencies] to read from this [List]. */
// TODO: save to storage
fun markCurrencyRecentlyUsed(currencyCode: String) {
    // Find currency in the argument
    when (val idx = orderedCurrencies.indexOf(currencyCode)) {
        // The currency was already first in the list; do nothing.
        0 -> { }
        // Currency was not found in the List, so it must be prepended.
        -1 -> orderedCurrencies.add(0, currencyCode)
        // Swap the first currency with the target currency (arg).
        else -> Collections.swap(orderedCurrencies, 0, idx)
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
