package com.example.budgiet.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgiet.Location
import com.example.budgiet.R
import com.example.budgiet.RecentItems
import com.example.budgiet.Result
import com.example.budgiet.addNewLocation
import com.example.budgiet.formatPrice
import com.example.budgiet.formatRelativeToPresent
import com.example.budgiet.getCurrencyIcon
import com.example.budgiet.getLocationsSearchPage
import com.example.budgiet.getRecentLocations
import com.example.budgiet.graphemeStringLength
import com.example.budgiet.graphemeStringTake
import com.example.budgiet.parsePrice
import com.example.budgiet.rememberListPager
import com.example.budgiet.rememberQueryListPager
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.ActionDialog
import com.example.budgiet.ui.utils.ActionDialogPadding
import com.example.budgiet.ui.utils.Corner
import com.example.budgiet.ui.utils.DatePickerDialog
import com.example.budgiet.ui.utils.FilledTextIconButton
import com.example.budgiet.ui.utils.LazyDropdownMenu
import com.example.budgiet.ui.utils.ListColumn
import com.example.budgiet.ui.utils.ListColumnItemScope
import com.example.budgiet.ui.utils.PlainSearchBar
import com.example.budgiet.ui.utils.PlainToolTipBox
import com.example.budgiet.ui.utils.TextIconButton
import com.example.budgiet.ui.utils.halfRoundedCornerShape
import com.example.budgiet.ui.utils.hideDropdownMenuPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Currency
import java.util.Locale
import kotlin.math.ceil

/** The maximum number of characters (graphemes) allowed in the Description field.
 * This value should not be changed as the database enforces the value. */
const val DESCRIPTION_MAX_LENGTH = 255
val DESCRIPTION_FIELD_MIN_HEIGHT = 125.dp
val DESCRIPTION_FIELD_MAX_HEIGHT = 300.dp

val FIELD_MAX_WIDTH = 275.dp
// How much time (in ms) should pass after an input on a field for its input to be validated.
private const val FIELD_TIMEOUT = 500L

val TAG_GRID_MAX_HEIGHT = 400.dp
val TAG_SELECTED_BORDER_COLOR = Color.Yellow // TODO: WIP
val TAG_SHAPE
    @Composable get() = MaterialTheme.shapes.medium


class NewTransactionViewModel: ViewModel() {
    var date by mutableStateOf<LocalDate>(LocalDate.now())
    var location by mutableStateOf<Location?>(null)
    var currency by mutableStateOf<Currency>(Currency.getInstance(Locale.getDefault()))
    var totalPrice by mutableDoubleStateOf(0.0)
    var tags = mutableStateListOf<Tag>()
    var description by mutableStateOf("")

    fun submit() {
        TODO()
    }

    fun cancel() {
        this.date = LocalDate.now()
        this.location = null
        // Currency should persist even after a cancel
        // this.currency = Currency.getInstance(Locale.getDefault())
        this.totalPrice = 0.0
        this.description = ""
    }

    companion object {
        fun getAllTags(): List<Tag> {
            TODO()
        }
        fun createNewTag(tag: Tag) {
            TODO("create tag $tag")
        }
    }
}

data class Tag(
    val name: String,
    val icon: String?,
    val color: Color,
)

private enum class DialogState {
    None, DatePicker, LocationPicker, TagsPicker;
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionForm(
    modifier: Modifier = Modifier,
    viewModel: NewTransactionViewModel,
    onDismiss: () -> Unit,
) {
    var dialogState by remember { mutableStateOf(DialogState.None) }
    val dialogDismiss = { dialogState = DialogState.None }

    Column(
        modifier = modifier,
    ) {
        FormField("Date") {
            OutlinedTextField(
                readOnly = true,
                onValueChange = {},
                value = viewModel.date.formatRelativeToPresent(),
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    PlainToolTipBox("Select Date") {
                        IconButton(onClick = { dialogState = DialogState.DatePicker }) {
                            Icon(painterResource(R.drawable.date_range_24px), "Select Date")
                        }
                    }
                },
            )
        }
        FormField("Location") {
            FilledTonalButton(
                onClick = { dialogState = DialogState.LocationPicker },
                // Modify the shape on the left-side of the button to connect with the auto-select location button.
                shape = halfRoundedCornerShape(Corner.Right),
            ) {
                Text(
                    if (viewModel.location != null) {
                        viewModel.location!!.name
                    } else {
                        "Select Location"
                    }
                )
            }
            PlainToolTipBox("Auto-select Location") {
                FilledIconButton(
                    onClick = { TODO() },
                    shape = halfRoundedCornerShape(Corner.Left)
                ) {
                    Icon(painterResource(R.drawable.location_on_24px), "Auto-select Location")
                }
            }
        }
        FormField("Price") {
            PriceField(
                selectedPrice = viewModel.totalPrice,
                onPriceChange = { viewModel.totalPrice = it },
                selectedCurrency = viewModel.currency,
                onCurrencyChange = { viewModel.currency = it }
            )
        }
        FormField("Tags") {
            if (viewModel.tags.isNotEmpty()) {
                Row(Modifier
                    .widthIn(max = FIELD_MAX_WIDTH)
                    .border(
                        width = OutlinedTextFieldDefaults.UnfocusedBorderThickness,
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.outline,
                    ),
                    horizontalArrangement = Arrangement.End,
                ) {

                }
            }
            PlainToolTipBox("Attach a Tag to this transaction") {
                val onClick = { dialogState = DialogState.TagsPicker }
                val tagIcon = @Composable {
                    Icon(painterResource(R.drawable.label_24px), "Attach tag")
                }

                if (viewModel.tags.isEmpty()) {
                    FilledTextIconButton(
                        onClick = onClick,
                        icon = tagIcon,
                        text = { Text("Attach tag") },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                    )
                } else {
                    IconButton(
                        onClick = onClick,
                        shape = halfRoundedCornerShape(Corner.Left),
                        content = tagIcon,
                    )
                }
            }
        }
        FormField("Description", labelPosition = LabelPosition.AboveContent) {
            DescriptionField(fieldValue = viewModel.description) { viewModel.description = it }
        }

        FormField(null, horizontalArrangement = Arrangement.SpaceBetween) {
            TextIconButton(
                onClick = {
                    onDismiss()
                    viewModel.cancel()
                },
                icon = { Icon(painterResource(R.drawable.close_24px), "Cancel") },
                text = { Text("Cancel") }
            )
            FilledTextIconButton(
                onClick = {
                    onDismiss()
                    viewModel.submit()
                },
                icon = { Icon(painterResource(R.drawable.check_24px), "Submit") },
                text = { Text("Submit") },
            )
        }
    }

    when (dialogState) {
        DialogState.None -> { }
        DialogState.DatePicker -> DatePickerDialog(
            selectedDate = viewModel.date,
            onDismiss = dialogDismiss,
            onSubmit = { viewModel.date = it },
        )
        DialogState.LocationPicker -> LocationPickerDialog(
            onDismiss = dialogDismiss,
            onSubmit = { viewModel.location = it },
        )
        DialogState.TagsPicker -> TagsPickerDialog(
            selectedTags = viewModel.tags,
            onSubmit = { viewModel.tags.addAll(it) },
            onDismiss = dialogDismiss,
        )
    }
}

/** Dictates how the *[FormField]*'s **label/title** is positioned in the element.
 *
 * Whether the main content is **small** and the label should appear [Beside][LabelPosition.BesidesContent] (to the left of) the content,
 * or the main content is **large** and the label should appear directly [Above][LabelPosition.AboveContent] the content. */
private enum class LabelPosition {
    AboveContent, BesidesContent,
}

@Composable
private fun FormField(
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

@Suppress("AssignedValueIsNeverRead")
@Composable
fun LocationPickerDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onSubmit: (Location) -> Unit,
) {
    var showNewLocationDialog by remember { mutableStateOf(false) }

    var newLocationAdded by remember { mutableStateOf(false) }

    if (showNewLocationDialog) {
        NewLocationDialog(
            modifier = modifier,
            onDismiss = { showNewLocationDialog = false },
            onSubmit = { name, address ->
                addNewLocation(name, address)
                newLocationAdded = true
            },
        )
    } else {
        LocationSearchDialog(
            modifier = modifier,
            onDismiss = onDismiss,
            onSubmit = onSubmit,
            onNewClick = { showNewLocationDialog = true },
            newLocationAdded = newLocationAdded,
        )
    }
}

/** Display a [Dialog][ActionDialog] that contains a [List][ListColumn] of [Location]s
 * and a [SearchBar][PlainSearchBar] to filter through all the locations in the list.
 *
 * The user can click a [Location] item to select it.
 *
 * @param newLocationAdded When a new [Location] item was added to the list of locations,
 *   The first item in the list of *Recents* will have an *animated scrim* to indicate that that item was just added.
 * @param onNewClick The action to run when the `"New Location"` button is clicked.  */
@Composable
private fun LocationSearchDialog(
    modifier: Modifier = Modifier,
    newLocationAdded: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (Location) -> Unit,
    onNewClick: () -> Unit,
) {
    val searchColumnSize = 3.5f
    val scrimAnimationSpeed = 250 // In millis
    val scrimAnimationDuration = scrimAnimationSpeed * 5 // Repeat n times; In millis
    val scrimColor = MaterialTheme.colorScheme.secondaryContainer
    val pageSize = ceil(searchColumnSize).toUInt() * 3u
    val searchState = rememberTextFieldState()

    val searchPager = rememberQueryListPager(
        queryState = searchState,
        getPage = { query, start, len -> getLocationsSearchPage(query, start, len) },
        // Page size should have enough items to scroll down several times the number of items showed.
        pageSize = pageSize,
    )
    val recentsPager = rememberListPager(
        getPage = { start, len -> getRecentLocations(start, len) },
        pageSize = pageSize,
    )

    fun close() {
        onDismiss()
        // Cancel pending page loading jobs.
        searchState.clearText()
        searchPager.refresh()
    }

    ActionDialog(
        onDismiss = { close() },
        padding = ActionDialogPadding.TightlyPacked,
        title = {
            PlainSearchBar(
                placeholderText = "Search existing locations",
                onQueryChange = { searchPager.refresh() },
                state = searchState,
            )
        },
        actions = {
            TextButton(onClick = { close() }) {
                Text("Cancel")
            }

            PlainToolTipBox("Add new location") {
                FilledTextIconButton(
                    onClick = onNewClick,
                    icon = { Icon(painterResource(R.drawable.add_24px), "New Location") },
                    text = { Text("New") },
                )
            }
        }
    ) {
        AnimatedContent(searchState.text.isEmpty()) { queryIsEmpty ->
            @Composable
            fun ListColumnItemScope.LocationItem(location: Location, animateScrim: Boolean = false) {
                var runAnimation by remember { mutableStateOf(animateScrim) }
                val containerColor = if (runAnimation) {
                    LaunchedEffect(Unit) {
                        delay(scrimAnimationDuration.toLong())
                        runAnimation = false
                    }
                    rememberInfiniteTransition()
                        .animateColor(
                            initialValue = ListItemDefaults.containerColor,
                            targetValue = scrimColor,
                            animationSpec = infiniteRepeatable(
                                animation = tween(scrimAnimationSpeed),
                                repeatMode = RepeatMode.Reverse,
                            ),
                        )
                        .value
                } else {
                    ListItemDefaults.containerColor
                }

                this.DataItem(
                    modifier = modifier.clickable(onClick = {
                        onSubmit(location)
                        close()
                    }),
                    colors = ListItemDefaults.colors(
                        containerColor = containerColor,
                    ),
                    headlineContent = { Text(location.name) },
                    supportingContent = { Text(location.address) },
                )
            }

            Column {
                // Show search results if the SearchBar has a query,
                // otherwise show recent locations
                if (queryIsEmpty) {
                    Text("Recent",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = ActionDialogPadding.TightlyPacked.dialogEdges.calculateStartPadding(LocalLayoutDirection.current))
                    )

                    ListColumn(visibleItems = searchColumnSize) {
                        this.pagedItemsIndexed(
                            pager = recentsPager,
                            itemKey = { _, location -> location.id.toInt() }, // Why can't use UInt ....
                        ) { idx, location ->
                            if (idx == 0) {
                                LocationItem(location, newLocationAdded)
                            } else {
                                LocationItem(location)
                            }
                        }
                    }
                } else {
                    ListColumn(visibleItems = searchColumnSize) {
                        this.pagedItems(
                            pager = searchPager,
                            itemKey = { location -> location.id.toInt() },
                        ) { location -> this.LocationItem(location) }
                    }
                }
            }
        }
    }
}

/** Display a [Dialog][ActionDialog] that prompts the user for information of a [Location] they want to create. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewLocationDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onSubmit: (name: String, address: String) -> Unit
) {
    val menuShape = MaterialTheme.shapes.medium
    val menuItemPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
    val maxMenuItems = 10u

    val locationName = remember { mutableStateOf("") }
    val locationAddress = remember { mutableStateOf("") }
    val nameError = remember { mutableStateOf(false) }
    val addressError = remember { mutableStateOf(false) }

    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        title = {
            Text("New location",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            PlainToolTipBox("Submit new location") {
                FilledTextIconButton(
                    onClick = {
                        nameError.value = locationName.value.isEmpty()
                        addressError.value = locationAddress.value.isEmpty()

                        if (!nameError.value && !addressError.value) {
                            onSubmit(locationName.value, locationAddress.value)
                            onDismiss()
                        }
                    },
                    icon = { Icon(painterResource(R.drawable.check_24px), "Submit") },
                    text = { Text("Submit") },
                )
            }
        }
    ) {
        @Composable
        fun Field(
            modifier: Modifier = Modifier,
            label: String,
            value: MutableState<String>,
            isError: MutableState<Boolean>,
        ) {
            TextField(
                modifier = modifier.fillMaxWidth(),
                label = { Text(label) },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                value = value.value,
                onValueChange = { newValue ->
                    isError.value = newValue.isEmpty()
                    value.value = newValue
                },
                isError = isError.value,
                supportingText = if (!isError.value) null else {{
                    Text("Must not be empty")
                }},
            )
        }

        val suggestedNames = remember(key1 = locationName.value) {
            getLocationsSearchPage(locationName.value, 0u, maxMenuItems)
                .map { it.name }
                // Only allow a single instance of a name to exist.
                // Due to the list becoming a set, some items wil be culled,
                // so the size won't necessarily be the same as maxMenuItems.
                .toSet()
        }

        var shouldShowMenu by remember { mutableStateOf(false) }
        val expanded = shouldShowMenu
            && locationName.value.isNotEmpty()
            && suggestedNames.isNotEmpty()

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { shouldShowMenu = it },
        ) {
            Field(
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    .onFocusEvent { if (it.isFocused) shouldShowMenu = true },
                label = "Name",
                value = locationName,
                isError = nameError,
            )
            ExposedDropdownMenu(
                modifier = Modifier
                    .hideDropdownMenuPadding()
                    .heightIn(max = 150.dp),
                expanded = expanded,
                onDismissRequest = { shouldShowMenu = false },
                matchAnchorWidth = false,
                shape = menuShape,
            ) {
                suggestedNames.forEach { name ->
                    DropdownMenuItem(
                        modifier = Modifier
                            .widthIn(min = 0.dp)
                            .padding(menuItemPadding)
                            .clip(menuShape),
                        text = { Text(name) },
                        onClick = {
                            locationName.value = name
                            shouldShowMenu = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(ActionDialogPadding.Default.titleSpacerHeight))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Field(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(end = 8.dp),
                label = "Address",
                value = locationAddress,
                isError = addressError,
            )
            PlainToolTipBox("Get address from GPS coordinates") {
                FilledIconButton(
                    onClick = { TODO() },
                    modifier = Modifier.size(TextFieldDefaults.MinHeight - 4.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryFixed,
                        contentColor = MaterialTheme.colorScheme.onPrimaryFixed,
                    )
                ) {
                    Icon(painterResource(R.drawable.location_on_24px),
                        "Auto-detect Address",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PriceField(
    modifier: Modifier = Modifier,
    selectedPrice: Double,
    onPriceChange: (Double) -> Unit,
    locale: Locale = Locale.getDefault(),
    selectedCurrency: Currency = remember { Currency.getInstance(locale) },
    onCurrencyChange: (Currency) -> Unit,
) {
    var fieldValue by remember { mutableStateOf(if (selectedPrice == 0.0) "" else {
        selectedCurrency.formatPrice(selectedPrice, locale)
    }) }
    var currencyMenuOpen by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    fun validateInput() {
        if (fieldValue.isNotEmpty()) {
            val price = fieldValue.filter { c -> c != DecimalFormatSymbols.getInstance(locale).groupingSeparator }
            when (val result = selectedCurrency.parsePrice(price, locale)) {
                is Result.Ok -> {
                    onPriceChange(result.value)
                    fieldValue = selectedCurrency.formatPrice(result.value, locale)
                    parseError = null
                }
                is Result.Err -> parseError = result.error.message
            }
        }
    }

    // Set a timer to run validateInput() on timeout.
    LaunchedEffect(fieldValue) {
        delay(FIELD_TIMEOUT)
        validateInput()
    }

    OutlinedTextField(
        modifier = modifier
            // FIXME: TextField does not grow with the input text's width
            .widthIn(min = 50.dp, max = FIELD_MAX_WIDTH)
            .width(IntrinsicSize.Min)
            // If another UI element is focused before the timeout, validate the input here and cancel the timer.
            .onFocusChanged { _ -> validateInput() },
        value = fieldValue,
        onValueChange = { fieldValue = it },
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        singleLine = true,
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }
        ),
        leadingIcon = {
            CurrencySelectorButton(
                showCurrencyMenu = currencyMenuOpen,
                onMenuStateChange = { currencyMenuOpen = it },
                selectedCurrency = selectedCurrency,
                onCurrencyChange = onCurrencyChange,
            )
        },
        placeholder = {
            Text(
                selectedCurrency.formatPrice(0.0, locale),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outline,
            )
        },
        isError = parseError != null,
        supportingText = parseError?.let { parseError -> {
            Text(parseError)
        } },
    )
}

/** Display a [Button][TextButton] that gives the user the option of choosing the [Currency] for the value of the [PriceField].
 *
 * @param hideDefaultCurrencyCode Whether the **currency code** (e.g. `"USD"`)
 *   will be displayed on the button if [selectedCurrency] is the **default** currency of the [locale].
 *   This should be set to true on production code.
 * @param showCurrencyMenu Whether the [DropdownMenu][androidx.compose.material3.DropdownMenu] listing all the currencies should be displayed.
 * @param onMenuStateChange What to do when the Composable requests a change in the
 *   [DropdownMenu][androidx.compose.material3.DropdownMenu]'s state (i.e. *open* or *close*).
 * @param locale Determines which [Currency] is shown in the menu first,
 *   and whether the **currency code** is displayed on the button (according to [hideDefaultCurrencyCode]).
 * @param selectedCurrency The [Currency] (code and icon) to display on the button.
 * @param onCurrencyChange What to do when the Composable requests a change in the [selectedCurrency]. */
@Composable
fun CurrencySelectorButton(
    modifier: Modifier = Modifier,
    hideDefaultCurrencyCode: Boolean = true,
    showCurrencyMenu: Boolean,
    onMenuStateChange: (Boolean) -> Unit,
    locale: Locale = Locale.getDefault(),
    selectedCurrency: Currency = remember { Currency.getInstance(locale) },
    onCurrencyChange: (Currency) -> Unit,
) {
    // TODO: choose currency (and locale) from settings instead, only default to locale if the setting is not set.
    val localeCurrency = remember { Currency.getInstance(locale) }

    PlainToolTipBox("Change currency") {
        TextButton(
            modifier = modifier.padding(start = 8.dp),
            onClick = { onMenuStateChange(!showCurrencyMenu) },
            contentPadding = ButtonDefaults.TextButtonContentPadding.let { padding ->
                PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding(),
                    start = padding.calculateStartPadding(LocalLayoutDirection.current) / 3,
                    end = 0.dp,
                )
            },
        ) {
            val icon = getCurrencyIcon(selectedCurrency)
            val code = selectedCurrency.currencyCode

            Icon(painterResource(R.drawable.arrow_drop_down_24px), "Open currency menu")
            if (icon != null) {
                Icon(icon, null)
            }
            // Only show currency name in the field if it is not the locale's currency.
            // If the icon is not shown, must show the currency code either way.
            if (code != localeCurrency.currencyCode
            || icon == null
            || !hideDefaultCurrencyCode) {
                Text(code)
                Spacer(Modifier.width(
                    ButtonDefaults.TextButtonContentPadding.calculateEndPadding(LocalLayoutDirection.current)
                ))
            }
        }
    }

    val recentCurrencies by RecentItems.Currency.items()
    // This list gets re-sorted (not recalculated) every time the state of recentCurrencies changes.
    val orderedCurrencies = remember {
        val currencies = Currency.getAvailableCurrencies()
            // This can be a MutableList, and not a MutableStateList because the sort state lies in recentCurrencies.
            .toMutableList()

        // Sort Locale Currency to the first position.
        currencies.remove(localeCurrency)
        currencies.add(0, localeCurrency)

        // Sort alphabetically (first, before recency order).
        currencies
            // ... but keep Locale currency first.
            .subList(1, currencies.size)
            .sortBy { currency -> currency.currencyCode }

        currencies
    }

    val currencySearchState = rememberTextFieldState()
    val currencyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    fun scrollToTop() {
        coroutineScope.launch {
            delay(500)
            currencyListState.scrollToItem(0)
        }
    }
    fun closeMenu() {
        onMenuStateChange(false)
        currencySearchState.clearText()
        scrollToTop()
    }

    fun List<Currency>.currencySearchFilter(query: CharSequence): List<Currency> {
        return this.filter { currency ->
            currency.currencyCode
                .contains(query, ignoreCase = true)
            || currency.displayName
                .contains(query, ignoreCase = true)
        }
    }

    LazyDropdownMenu(
        showDropdown = showCurrencyMenu,
        onDismiss = { closeMenu() },
        shape = MaterialTheme.shapes.large,
        state = currencyListState,
        leadingContent = {
            // FIXME: use shape large
            PlainSearchBar(
                modifier = Modifier.padding(4.dp),
                state = currencySearchState,
                onQueryChange = { scrollToTop() },
                hideIconOnQuery = true,
            )
        }
    ) {
        when (recentCurrencies) {
            // Don't show any extra items, but re-sort the currencies list.
            is Result.Ok -> {
                val recentCurrencies = (recentCurrencies as Result.Ok).value
                // Slice list of ordered currencies to keep the locale currency at the beginning.
                val orderedCurrencies = orderedCurrencies.subList(1, orderedCurrencies.size)

                // Put recently used currencies before other currencies (except locale).
                var insertIdx = 0
                for (recent in recentCurrencies) {
                    val idx = orderedCurrencies.indexOfFirst { it == recent }
                    // Skip if currency is not in orderedCurrencies (i.e. locale).
                    if (idx == -1) {
                        continue
                    }

                    orderedCurrencies.add(insertIdx++, orderedCurrencies.removeAt(idx))
                }
            }
            // Recent currencies not loaded yet.
            // Show loading item at the top.
            null -> this.item {
                this.LoadingItem()
            }
            // Show an error item at the top.
            is Result.Err -> this.item {
                val err = (recentCurrencies as Result.Err).error
                this.ErrorItem(err)
            }
        }

        this.items(
            items = orderedCurrencies.currencySearchFilter(currencySearchState.text),
            key = { currency -> currency.currencyCode }
        ) { currency ->
            PlainToolTipBox(currency.displayName) {
                this.MenuItem(
                    // Apply a scrim color for the one that is selected.
                    modifier = if (currency == selectedCurrency) {
                        Modifier.background(MaterialTheme.colorScheme.surfaceDim)
                    } else {
                        Modifier
                    },
                    headlineContent = { Text(currency.currencyCode) },
                    // Even if there is no icon for this currency, activate leadingIcon to align all the currency codes.
                    leadingIcon = {
                        getCurrencyIcon(currency)?.let { icon ->
                            Icon(icon, null)
                        }
                    },
                    onClick = {
                        closeMenu()
                        RecentItems.Currency.moveToFront(currency, context)
                        onCurrencyChange(currency)
                    },
                )
            }
        }

        this.item(key = "DELETE") {
            PlainToolTipBox("Clear list of recent currencies to reset the list to its original state") {
                this.MenuItem(
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.close_24px),
                            "Clear recents",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    headlineContent = {
                        Text("Reset", color = MaterialTheme.colorScheme.error)
                    },
                    onClick = {
                        closeMenu()
                        RecentItems.Currency.clear(context)
                        // Sort ordered currencies alphabetically to reset the list
                        orderedCurrencies
                            .subList(1, orderedCurrencies.size) // Don't include locale currency in the sorting.
                            .sortBy { currency -> currency.currencyCode }
                    }
                )
            }
        }
    }
}

@Composable
fun TagsPickerDialog(
    modifier: Modifier = Modifier,
    selectedTags: List<Tag>,
    onSubmit: (List<Tag>) -> Unit,
    onDismiss: () -> Unit,
) {
    val gridPadding = 4.dp
    val searchState = rememberTextFieldState()
    val allTags = NewTransactionViewModel.getAllTags()

    var showTagCreator by remember { mutableStateOf(false) }
    val innerSelectedTags = remember { mutableStateSetOf<Tag>() }
    innerSelectedTags.addAll(selectedTags)

    if (showTagCreator) {
        TagCreatorDialog(
            modifier = modifier,
            onSubmit = { NewTransactionViewModel.createNewTag(it) },
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                showTagCreator = false
            },
        )
    } else {
        ActionDialog(
            modifier = modifier,
            onDismiss = onDismiss,
            title = {
                PlainSearchBar(
                    state = searchState,
                    placeholderText = "Search tags",
                    onQueryChange = { },
                )
            },
            actions = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                PlainToolTipBox("Submit selected tags") {
                    FilledTextIconButton(
                        onClick = {
                            onSubmit(innerSelectedTags.toList())
                            onDismiss()
                        },
                        icon = { Icon(painterResource(R.drawable.check_24px), "Submit") },
                        text = { Text("Done") },
                    )
                }
            },
        ) {
            val modifier = Modifier.fillMaxWidth()
                .heightIn(min = TextFieldDefaults.MinHeight, max = TAG_GRID_MAX_HEIGHT)
                .border(width = 1.dp, shape = TAG_SHAPE, color = MaterialTheme.colorScheme.outline)

            if (allTags.isEmpty()) {
                Box(modifier) {
                    Column(
                        modifier = Modifier.padding(ActionDialogPadding.Default.dialogEdges),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("There are no tags.", textAlign = TextAlign.Center)
                        Text("Press \"New Tag\" to create one.", textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyVerticalGrid(
                    modifier = modifier,
                    contentPadding = PaddingValues(gridPadding),
                    columns = GridCells.Adaptive(minSize = 1.dp),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    this.items(
                        items = allTags,
                        key = { it.name },
                        span = { _ -> GridItemSpan(this.maxLineSpan) },
                    ) { tag ->
                        TagFrame(
                            tag = tag,
                            modifier = Modifier.clickable { innerSelectedTags.add(tag) },
                            isSelected = innerSelectedTags.contains(tag),
                        )
                    }
                }
            }

            FilledTextIconButton(
                modifier = Modifier.align(Alignment.End)
                    .padding(top = ActionDialogPadding.Default.titleSpacerHeight),
                colors = ButtonDefaults.filledTonalButtonColors(),
                icon = { Icon(painterResource(R.drawable.add_24px), "New tag") },
                text = { Text("New tag") },
                onClick = {
                    @Suppress("AssignedValueIsNeverRead")
                    showTagCreator = true
                },
            )
        }
    }
}

@Composable
fun TagCreatorDialog(
    modifier: Modifier = Modifier,
    onSubmit: (Tag) -> Unit,
    onDismiss: () -> Unit,
) {
    var icon by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(Color.Cyan) } // TODO: select random color

    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        title = { Text("Create new tag") },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            PlainToolTipBox("Submit new tag") {
                FilledTextIconButton(
                    onClick = {
                        onSubmit(Tag(name, icon, color))
                        onDismiss()
                    },
                    icon = { Icon(painterResource(R.drawable.check_24px), "Submit") },
                    text = { Text("Submit") },
                )
            }
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlainToolTipBox("Select tag icon") {
                IconButton(
                    modifier = Modifier.border(width = 2.dp, shape = IconButtonDefaults.standardShape, color = MaterialTheme.colorScheme.outline),
                    onClick = { TODO() }
                ) {

                }
            }
            TextField(
                label = { Text("Tag name") },
                value = name,
                onValueChange = { name = it },
            )
            // TODO: color picker
        }
    }
}

// TODO: doc: onRemove shows an X button if not null
@Composable
fun TagFrame(
    modifier: Modifier = Modifier,
    tag: Tag,
    isSelected: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.apply {
            background(color = tag.color, shape = TAG_SHAPE)
            if (isSelected) {
                border(width = 2.dp, shape = TAG_SHAPE, color = TAG_SELECTED_BORDER_COLOR)
            }
        },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(TODO()), null)
        Text(tag.name)
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(painterResource(R.drawable.close_24px), "Remove tag")
            }
        }
    }
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
        modifier = modifier
            .fillMaxWidth()
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
            NewTransactionForm(
                viewModel = viewModel<NewTransactionViewModel>().apply {
                    tags.add(Tag(name = "Tag1", icon = "dog", color = Color.Cyan))
                    tags.add(Tag(name = "Tag2", icon = "cat", color = Color.Yellow))
                    tags.add(Tag(name = "Tag3", icon = "shopping_cart", color = Color.Black))
                },
                onDismiss = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocationSearchPreview() {
    BudgietTheme {
        LocationSearchDialog(
            onDismiss = {},
            onSubmit = {},
            onNewClick = {}
        )
    }
}
@Preview(showBackground = true)
@Composable
fun NewLocationPreview() {
    BudgietTheme {
        NewLocationDialog(
            onDismiss = {},
            onSubmit = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TagsPickerPreview() {
    BudgietTheme {
        TagsPickerDialog(
            selectedTags = listOf(),
            onSubmit = { },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TagCreatorPreview() {
    BudgietTheme {
        TagCreatorDialog(
            onSubmit = { },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true, widthDp = 150, heightDp = 400)
@Composable
fun CurrenciesDropDownPreview() {
    BudgietTheme {
        CurrencySelectorButton(
            modifier = Modifier
                .padding(8.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.shapes.extraLarge,
                ),
            hideDefaultCurrencyCode = false,
            showCurrencyMenu = true,
            onMenuStateChange = { },
            onCurrencyChange = { },
        )
    }
}
