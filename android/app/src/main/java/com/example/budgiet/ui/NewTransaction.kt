package com.example.budgiet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.paging.PagingConfig
import com.example.budgiet.Location
import com.example.budgiet.R
import com.example.budgiet.Result
import com.example.budgiet.formatRelativeToPresent
import com.example.budgiet.getLocationsSearchPage
import com.example.budgiet.getRecentLocations
import com.example.budgiet.graphemeStringLength
import com.example.budgiet.graphemeStringTake
import com.example.budgiet.parsePrice
import com.example.budgiet.rememberQueryListPager
import com.example.budgiet.rememberWork
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.DatePickerDialog
import com.example.budgiet.ui.utils.FilledTextIconButton
import com.example.budgiet.ui.utils.ListColumn
import com.example.budgiet.ui.utils.ListItemScope
import com.example.budgiet.ui.utils.PagedListColumn
import com.example.budgiet.ui.utils.PagerController
import com.example.budgiet.ui.utils.PlainSearchBar
import com.example.budgiet.ui.utils.PlainToolTipBox
import com.example.budgiet.ui.utils.TextIconButton
import java.time.LocalDate
import java.util.Currency
import kotlin.math.ceil

/** The maximum number of characters (graphemes) allowed in the Description field.
 * This value should not be changed as the database enforces the value. */
const val DESCRIPTION_MAX_LENGTH = 255
val DESCRIPTION_FIELD_MIN_HEIGHT = 125.dp
val DESCRIPTION_FIELD_MAX_HEIGHT = 300.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionForm(modifier: Modifier = Modifier) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var selectedPrice by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = modifier,
    ) {
        FormField("Date") {
            OutlinedTextField(
                readOnly = true,
                onValueChange = {},
                value = selectedDate.formatRelativeToPresent(),
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    PlainToolTipBox("Select Date") {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(painterResource(R.drawable.date_range_24px), "Select Date")
                        }
                    }
                },
            )
        }
        FormField("Location") {
            /** The border-radius of the shape of the two buttons in this field.
             * This only applies to the corners that connect the buttons. */
            val inBetweenBorderRadius = MaterialTheme.shapes.extraSmall.bottomEnd

            FilledTonalButton(
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
                    Icon(painterResource(R.drawable.location_on_24px), "Auto-select Location")
                }
            }
        }
        FormField("Price") {
            PriceField(initialPrice = selectedPrice, onPriceChange = { selectedPrice = it })
        }
        FormField("Description", labelPosition = LabelPosition.AboveContent) {
            DescriptionField(fieldValue = description) { description = it }
        }

        FormField(null, horizontalArrangement = Arrangement.SpaceBetween) {
            TextIconButton(
                onClick = { TODO() },
                icon = { Icon(painterResource(R.drawable.close_24px), "Cancel") },
                text = { Text("Cancel") }
            )
            FilledTextIconButton(
                onClick = { TODO() },
                icon = { Icon(painterResource(R.drawable.check_24px), "Submit") },
                text = { Text("Submit") },
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                showDatePicker = false
            },
            onSubmit = { selectedDate = it },
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

/** Dictates how the *[FormField]*'s **label/title** is positioned in the element.
 *
 * Whether the main content is **small** and the label should appear [Beside][LabelPosition.BesidesContent] (to the left of) the content,
 * or the main content is **large** and the label should appear directly [Above][LabelPosition.AboveContent] the content. */
enum class LabelPosition {
    AboveContent, BesidesContent,
}

@Composable
fun FormField(
    label: String?,
    modifier: Modifier = Modifier,
    labelPosition: LabelPosition = LabelPosition.BesidesContent,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable (RowScope.() -> Unit)
) {
    val headlineRow = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = content,
        )
    }

    when (labelPosition) {
        LabelPosition.BesidesContent -> ListItem(
            modifier = modifier,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            // The ListItem places leadingContent to the left of headlineContent, and adds spacing.
            leadingContent = label?.let { { Text(label) } },
            headlineContent = headlineRow,
        )
        LabelPosition.AboveContent -> ListItem(
            modifier = modifier,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                ) {
                    if (label != null) {
                        Text(
                            label,
                            color = ListItemDefaults.colors().leadingIconColor,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    headlineRow()
                }
            }
        )
    }
}

@Composable
fun LocationPickerDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onSubmit: (Location) -> Unit,
) {
    val dialogPadding = 8.dp
    val searchColumnSize = 3.5f
    // Page size should have enough items to scroll down several times the number of items showed.
    val searchPageSize = ceil(searchColumnSize).toInt() * 3
    val searchState = rememberTextFieldState()

    val searchPagerController = remember { PagerController() }
    // These are the items shown if the search does not have a query
    val recentItems by rememberWork { getRecentLocations() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true,
        )
    ) {
        Card(
            modifier = modifier.fillMaxWidth() // PRO TIP: doesn't actually fill max width, it has a margin
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
                        modifier = modifier.clickable(onClick = {
                            onSubmit(location)
                            onDismiss()
                        }),
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
                        FilledTextIconButton(
                            onClick = { TODO() },
                            icon = { Icon(painterResource(R.drawable.add_24px), "New Location") },
                            text = { Text("New") },
                        )
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
@Composable
fun DescriptionField(
    modifier: Modifier = Modifier,
    fieldValue: String,
    onValueChange: (String) -> Unit,
) {
    // Whether the user has pasted content that goes over the MAX_LENGTH of the field.
    // When this happens, the field becomes an error state.
    var pasteOverflow by remember { mutableStateOf(false) }

    OutlinedTextField(
        modifier = modifier.fillMaxWidth()
            .heightIn(min = DESCRIPTION_FIELD_MIN_HEIGHT, max = DESCRIPTION_FIELD_MAX_HEIGHT),
        value = fieldValue,
        onValueChange = { newDescription ->
            // Get the String length, but in units of graphemes.
            val length = graphemeStringLength(newDescription)

            @Suppress("AssignedValueIsNeverRead")
            pasteOverflow = length > DESCRIPTION_MAX_LENGTH

            // Implement character limit with a cutoff,
            // instead of not replacing the description value in the first place.
            onValueChange(graphemeStringTake(newDescription, DESCRIPTION_MAX_LENGTH))
        },
        shape = MaterialTheme.shapes.large,
        textStyle = LocalTextStyle.current.copy(
            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            lineHeight = 1.5.em,
        ),
        placeholder = { Text(
            "Write details about the transaction here...",
            color = MaterialTheme.colorScheme.outline,
        ) },
        isError = pasteOverflow,
        supportingText = {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (pasteOverflow) {
                    Text("Description is too long!")
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    "${graphemeStringLength(fieldValue)}/$DESCRIPTION_MAX_LENGTH",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
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
