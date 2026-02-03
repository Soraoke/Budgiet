package com.example.budgiet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.paging.PagingConfig
import com.example.budgiet.Date
import com.example.budgiet.Location
import com.example.budgiet.Result
import com.example.budgiet.getLocationsSearchPage
import com.example.budgiet.getRecentLocations
import com.example.budgiet.parsePrice
import com.example.budgiet.rememberQueryListPager
import com.example.budgiet.rememberWork
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.DIALOG_PROPERTIES
import com.example.budgiet.ui.utils.DIALOG_SHAPE
import com.example.budgiet.ui.utils.DatePickerDialog
import com.example.budgiet.ui.utils.ListColumn
import com.example.budgiet.ui.utils.ListItemScope
import com.example.budgiet.ui.utils.PagedListColumn
import com.example.budgiet.ui.utils.PagerController
import com.example.budgiet.ui.utils.PlainSearchBar
import com.example.budgiet.ui.utils.PlainToolTipBox
import java.util.Currency
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionForm(modifier: Modifier = Modifier) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Date.now()) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var selectedPrice by remember { mutableStateOf("") }

    Column(
        modifier = modifier,
    ) {
        FormField("Date") {
            OutlinedTextField(
                readOnly = true,
                onValueChange = {},
                value = selectedDate.toString(),
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    PlainToolTipBox("Select Date") {
                        IconButton(onClick = { showDatePicker = !showDatePicker }) {
                            Icon(Icons.Filled.DateRange, "Select Date")
                        }
                    }
                },
            )
        }
        FormField("Location") {
            /** The border-radius of the shape of the two buttons in this field.
             * This only applies to the corners that connect the buttons. */
            val inBetweenBorderRadius = MaterialTheme.shapes.extraSmall.bottomEnd

            OutlinedButton(
                onClick = { showLocationPicker = true },
                // Modify the shape on the left-side of the button to connect with the auto-select location button.
                shape = RoundedCornerShape(
                    topStart = CornerSize(percent = 50), bottomStart = CornerSize(percent = 50),
                    topEnd = inBetweenBorderRadius, bottomEnd = inBetweenBorderRadius,
                )
            ) {
                Text(
                    if (selectedLocation != null) {
                        selectedLocation!!.name
                    } else {
                        "Select Location"
                    }
                )
            }
            PlainToolTipBox("Auto-select Location") {
                FilledIconButton(
                    onClick = { TODO() },
                    // Modify the shape on the right-side of the button to connect with the select location button.
                    shape = RoundedCornerShape(
                        topEnd = CornerSize(percent = 50), bottomEnd = CornerSize(percent = 50),
                        topStart = inBetweenBorderRadius, bottomStart = inBetweenBorderRadius,
                    )
                ) {
                    Icon(Icons.Outlined.LocationOn, "Auto-select Location")
                }
            }
        }
        FormField("Price") {
            PriceField(initialPrice = selectedPrice, onPriceChange = { selectedPrice = it })
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onSubmit = { date -> selectedDate = date },
        )
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                showLocationPicker = false
            },
            onSubmit = { location -> selectedLocation = location }
        )
    }

}

@Composable
fun FormField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable (RowScope.() -> Unit)
) {
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Text(label) },
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    )
}

@Composable
fun LocationPickerDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onSubmit: (Location) -> Unit,
) {
    val dialogPadding = 8.dp
    val textIconButtonPadding = 12.dp
    val textIconButtonSpacing = 4.dp
    val searchColumnSize = 3.5f
    // Page size should have enough items to scroll down several times the number of items showed.
    val searchPageSize = ceil(searchColumnSize).toInt() * 3
    val searchState = rememberTextFieldState()

    val searchPagerController = remember { PagerController() }
    // These are the items shown if the search does not have a query
    val recentItems by rememberWork { getRecentLocations() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DIALOG_PROPERTIES,
    ) {
        Card(
            modifier = modifier.fillMaxWidth(), // PRO TIP: doesn't actually fill max width, it has a margin
            shape = DIALOG_SHAPE,
        ) {
            Column(
                modifier = Modifier.padding(all = dialogPadding)
                // TODO: Animate height
            ) {
                // TODO: cancel getPage when the clear button is clicked
                PlainSearchBar(
                    onQueryChange = { searchPagerController.refresh() },
                    state = searchState,
                )

                Spacer(Modifier.height(dialogPadding))

                // Show search results if the SearchBar has a query,
                // otherwise show recent locations.
                if (searchState.text.isEmpty()) {
                    Text("Recent",
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = dialogPadding)
                    )
                }

                @Composable
                fun ListItemScope.LocationItem(location: Location) {
                    this.DataItem(
                        modifier = modifier.clickable(onClick = { onSubmit(location) }),
                        headlineContent = { Text(location.name) },
                        supportingContent = { Text(location.address) },
                    )
                }

                // Show search results if the SearchBar has a query,
                // otherwise show recent locations
                if (searchState.text.isEmpty()) {
                    ListColumn(visibleItems = searchColumnSize) {
                        when (recentItems) {
                            is Result.Ok -> {
                                items(
                                    items = (recentItems as Result.Ok).value,
                                    key = { location -> location.id.toInt() }, // Why can't use UInt ....
                                ) { location -> this.LocationItem(location) }
                            }
                            // Show the item as an Error if the task threw an Exception
                            is Result.Err -> {
                                val error = (recentItems as Result.Err).error
                                item { this.ErrorItem(type = error.javaClass.name, message = error.localizedMessage) }
                            }
                            // Show loading indicator while the items are being obtained
                            null -> item { this.LoadingItem() }
                        }
                    }
                } else {
                    PagedListColumn(
                        visibleItems = searchColumnSize,
                        pager = rememberQueryListPager(
                            queryState = searchState,
                            getPage = { query, start, len -> getLocationsSearchPage(query, start, len) },
                            config = PagingConfig(
                                pageSize = searchPageSize,
                                initialLoadSize = searchPageSize,
                                // Must be > pageSize * 3, let's make it 4 pages.
                                maxSize = searchPageSize * 4,
                                // Don't let the pager return a bunch of unloaded items, we are going to show a single unloaded item at a time.
                                enablePlaceholders = false,
                            )
                        ),
                        pagerController = searchPagerController,
                        itemKey = { location -> location.id.toInt() },
                        itemContent = { location -> this.LocationItem(location) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dialogPadding / 2),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // TODO: cancel work on getRecentLocations and getPage when this is clicked
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    PlainToolTipBox("Add new location") {
                        Button(
                            onClick = { TODO() },
                            contentPadding = PaddingValues(horizontal = textIconButtonPadding)
                        ) {
                            Icon(Icons.Filled.Add, "New Location")
                            Spacer(Modifier.width(textIconButtonSpacing))
                            Text("New")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriceField(modifier: Modifier = Modifier, initialPrice: String, onPriceChange: (String) -> Unit) {
    var parseError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        onValueChange = onPriceChange,
        value = initialPrice,
        modifier = modifier
            // fixme: use clamping than max for width
            .widthIn(max = 150.dp)
            .onFocusChanged { state ->
                /* FIXME: When currency field is added onto price, change "USD" to whatever
                *   currency code we are using */
                // When we lose focus on this text field, we should parse the price input
                // to see if it is invalid (outputting an error doing so) or format the
                // price accordingly if valid
                if (!state.isFocused) {
                    when (val result = parsePrice(initialPrice, Currency.getInstance("USD"))) {
                        is Result.Ok -> {
                            onPriceChange(result.value)
                            parseError = null
                        }

                        is Result.Err -> {
                            parseError = result.error.message
                        }
                    }
                }
            },
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
            }
        ),
        isError = parseError != null,
        placeholder = {
            Text(
                "0",
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outline,
            )
        },
        supportingText = {
            if (parseError != null) {
                Text(parseError as String)
            }
        }

        // TODO: Add Icon decoration for the price (like $ USD)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun NewTransactionPreview() {
    BudgietTheme {
        Box(Modifier.background(BottomSheetDefaults.ContainerColor)) {
            NewTransactionForm()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocationPickerPreview() {
    BudgietTheme {
        LocationPickerDialog(
            onDismiss = {},
            onSubmit = {},
        )
    }
}
