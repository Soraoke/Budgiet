@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.budgiet.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
const val FIELD_TIMEOUT = 500L

class NewTransactionViewModel: ViewModel() {
    var date by mutableStateOf<LocalDate>(LocalDate.now())
    var location by mutableStateOf<Location?>(null)
    var currency by mutableStateOf<Currency>(Currency.getInstance(Locale.getDefault()))
    var totalPrice by mutableDoubleStateOf(0.0)
    var tags = mutableStateSetOf<Tag>()
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
        /** A fake "database" containing tag data in memory. will be removed once the real database is implemented. */
        private val fakeTagsDb = mutableStateListOf<Tag>()

        val tagNameCharLimit = 9

        fun getAllTags(): List<Tag> {
            // TODO:
            return this.fakeTagsDb
        }
        fun createNewTag(tag: Tag) {
            // TODO:
            this.fakeTagsDb.add(tag)
        }

        /** Check if the provided **`name`** can be used for a new Tag.
         * Otherwise, returns an **Error** message.
         *
         * @param allTags The collection of all the [Tag]s that have been created and that exist in the database.
         * @param name The name of the new [Tag] that is attempted to be created. */
        fun validateTagName(allTags: Collection<Tag>, name: String): String? {
            // TODO: only allow ascii and dont allow whitespace
            return if (name.isEmpty()) {
                "Tag name must not be empty."
            } else if (name.length > this.tagNameCharLimit) {
                "Tag name must be 9 characters or less."
            } else if (allTags.find { it.name == name } != null) {
                "A tag with this name already exists."
            } else {
                null
            }
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
@Composable
fun NewTransactionForm(
    modifier: Modifier = Modifier,
    viewModel: NewTransactionViewModel,
    onDismiss: () -> Unit,
) {
    var dialogState by remember { mutableStateOf(DialogState.None) }
    val dialogDismiss = { dialogState = DialogState.None }

    Column(modifier = modifier) {
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
            TagsField(
                selectedTags = viewModel.tags,
                onButtonClick = { dialogState = DialogState.TagsPicker },
            )
        }
        FormField("Description",
            labelPosition = LabelPosition.AboveContent,
        ) {
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
            allTags = NewTransactionViewModel.getAllTags(),
            selectedTags = viewModel.tags,
            onNewTag = { NewTransactionViewModel.createNewTag(it) },
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

@Composable
fun LocationPickerDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onSubmit: (Location) -> Unit,
) {
    var showNewLocationDialog by remember { mutableStateOf(false) }

    var newLocationAdded by remember { mutableStateOf(false) }

    @Suppress("AssignedValueIsNeverRead")
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
            Row(
                Modifier.fillMaxWidth(),
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
private fun NewTransactionPreview() {
    BudgietTheme {
        Box(Modifier.background(BottomSheetDefaults.ContainerColor)) {
            NewTransactionForm(
                viewModel = viewModel<NewTransactionViewModel>().apply {
                    FAKE_TAGS.subList(0, 3)
                        .forEach { tags.add(it) }
                },
                onDismiss = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationSearchPreview() {
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
private fun NewLocationPreview() {
    BudgietTheme {
        NewLocationDialog(
            onDismiss = {},
            onSubmit = { _, _ -> }
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
