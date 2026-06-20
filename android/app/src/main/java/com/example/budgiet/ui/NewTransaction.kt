@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.budgiet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgiet.DbEntry
import com.example.budgiet.R
import com.example.budgiet.RecentItems
import com.example.budgiet.Result
import com.example.budgiet.formatPrice
import com.example.budgiet.formatRelativeToPresent
import com.example.budgiet.getCurrencyIcon
import com.example.budgiet.graphemeStringLength
import com.example.budgiet.graphemeStringTake
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.DatePickerDialog
import com.example.budgiet.ui.utils.FilledTextIconButton
import com.example.budgiet.ui.utils.LazyDropdownMenu
import com.example.budgiet.ui.utils.PlainSearchBar
import com.example.budgiet.ui.utils.PlainToolTipBox
import com.example.budgiet.ui.utils.RealNumberFieldState
import com.example.budgiet.ui.utils.TextIconButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Currency
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/** The maximum number of characters (graphemes) allowed in the Description field.
 * This value should not be changed as the database enforces the value. */
const val DESCRIPTION_MAX_LENGTH = 255
val DESCRIPTION_FIELD_MIN_HEIGHT = 125.dp
val DESCRIPTION_FIELD_MAX_HEIGHT = 300.dp

val FIELD_MAX_WIDTH = 275.dp
// How much time (in ms) should pass after an input on a field for its input to be validated.
val FIELD_TIMEOUT = 500.milliseconds

class NewTransactionViewModel(
    // TODO: choose currency (and locale) from settings instead, only default to locale if the setting is not set.
    locale: Locale = Locale.getDefault(),
    initialCurrency: Currency = Currency.getInstance(locale),
): ViewModel() {
    var date by mutableStateOf<LocalDate>(LocalDate.now())
    var location = LocationViewModel()
    var currency by mutableStateOf(initialCurrency)
    var customPrice by mutableDoubleStateOf(0.0)
    val items = ItemsViewModel()
    val tags = TagsViewModel()
    var description by mutableStateOf("")

    fun submit() {
        TODO()
    }

    fun cancel() {
        this.date = LocalDate.now()
        this.location.selectedLocation = null
        // Currency should persist even after a cancel
        // this.currency = Currency.getInstance(Locale.getDefault())
        this.customPrice = 0.0
        this.items.reset()
        this.tags.selectedTags.clear()
        this.description = ""
    }
}

private sealed class DialogState {
    object None: DialogState()
    object DatePicker: DialogState()
    class LocationPicker(state: LocationPickerState): DialogState() {
        var state by mutableStateOf(state)
    }
    class Items(state: ItemsDialogState): DialogState() {
        var state by mutableStateOf(state)
    }
    object TagsPicker: DialogState()

    companion object {
        val Saver = mapSaver(
            save = { state: DialogState -> mapOf(
                "discriminant" to state.javaClass.simpleName,
                "locationPickerState" to if (state is LocationPicker) {
                    val state = state.state
                    mapOf(
                        "discriminant" to state.javaClass.simpleName,
                        "location" to when (state) {
                            is LocationPickerState.Edit -> mapOf(
                                "id" to state.location.id,
                                "name" to state.location.data.name,
                                "address" to state.location.data.address,
                                "lastUsed" to state.location.data.lastUsed,
                            )
                            else -> null
                        },
                    )
                } else null,
                "itemsDialogState" to if (state is Items) {
                    val state = state.state
                    mapOf(
                        "discriminant" to state.javaClass.simpleName,
                        "item" to when (state) {
                            is ItemsDialogState.Edit -> mapOf(
                                "name" to state.value.name,
                                "unitPrice" to state.value.unitPrice,
                                "amountType" to state.value.amount.type.toString(),
                                "amountValue" to when (state.value.amount) {
                                    is Amount.Units -> state.value.amount.value
                                    is Amount.Measured -> state.value.amount.value
                                },
                                "amountLabel" to when (state.value.amount) {
                                    is Amount.Measured -> state.value.amount.label
                                    else -> null
                                }
                            )
                            else -> null
                        },
                    )
                } else null,
            ) },
            restore = { map -> when (val discriminant = map["discriminant"]) {
                "None" -> None
                "DatePicker" -> DatePicker
                "LocationPicker" -> LocationPicker(@Suppress("UNCHECKED_CAST") run {
                    val map = map["locationPickerState"] as Map<String, Any?>

                    when (val discriminant = map["discriminant"]) {
                        "Search" -> LocationPickerState.Search
                        "Nearby" -> LocationPickerState.Nearby
                        "New" -> LocationPickerState.New
                        "Edit" -> LocationPickerState.Edit(run {
                            val location = map["location"]!! as Map<String, Any?>

                            DbEntry(
                                id = location["id"]!! as UInt,
                                data = Location(
                                    name = location["name"]!! as String,
                                    address = location["address"] as String?,
                                    lastUsed = location["lastUsed"] as LocalTime?,
                                )
                            )
                        })
                        else -> throw IllegalStateException("Invalid discriminant for 'LocationPickerState': $discriminant")
                    }
                })
                "Items" -> Items(@Suppress("UNCHECKED_CAST") run {
                    val map = map["itemsDialogState"] as Map<String, Any?>

                    when (val discriminant = map["discriminant"]) {
                        "View" -> ItemsDialogState.View
                        "New" -> ItemsDialogState.New
                        "Ocr" -> ItemsDialogState.Ocr
                        "Edit" -> ItemsDialogState.Edit(run {
                            val item = map["item"]!! as Map<String, Any?>

                            Item(
                                name = item["name"]!! as String,
                                unitPrice = item["unitPrice"]!! as Double,
                                amount = when (val type = item["amountType"]!! as String) {
                                    Amount.Type.Units.toString() -> Amount.Units(item["amountValue"]!! as UInt)
                                    Amount.Type.Measured.toString() -> Amount.Measured(
                                        value = item["amountValue"]!! as Double,
                                        label = item["amountLabel"]!! as String,
                                    )
                                    else -> throw IllegalStateException("Invalid discriminant for ItemsDialogState.Edit.Item.amount.type: $type")
                                }
                            )
                        })
                        else -> throw IllegalStateException("Invalid discriminant for 'DialogState': $discriminant")
                    }
                })
                "TagsPicker" -> TagsPicker
                else -> throw IllegalStateException("Invalid discriminant for 'DialogState': $discriminant")
            } },
        )
    }
}

@Composable
fun NewTransactionForm(
    modifier: Modifier = Modifier,
    viewModel: NewTransactionViewModel,
    userLocale: Locale,
    onDismiss: () -> Unit,
) {
    var dialogState by rememberSaveable(stateSaver = DialogState.Saver) { mutableStateOf(DialogState.None) }
    val dialogDismiss = { dialogState = DialogState.None }

    Column(modifier
        .verticalScroll(rememberScrollState())
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
                            Icon(painterResource(R.drawable.date_range_24px), null)
                        }
                    }
                },
            )
        }
        FormField("Location") {
            LocationField(
                viewModel = viewModel.location,
                onClickSelect = { dialogState = DialogState.LocationPicker(LocationPickerState.Search) },
                onClickNearby = { dialogState = DialogState.LocationPicker(LocationPickerState.Nearby) },
            )
        }
        FormField("Price") {
            PriceField(
                viewModel = viewModel,
                locale = userLocale,
            )
        }
        FormField("Items") {
            ItemsField(
                viewModel = viewModel.items,
                locale = userLocale,
                currency = viewModel.currency,
                onClickAdd = { dialogState = DialogState.Items(ItemsDialogState.View) },
                onClickOcr = { dialogState = DialogState.Items(ItemsDialogState.Ocr) },
            )
        }
        FormField("Tags") {
            TagsField(
                viewModel = viewModel.tags,
                onButtonClick = { dialogState = DialogState.TagsPicker },
            )
        }
        FormField("Description",
            labelPosition = LabelPosition.AboveContent,
        ) {
            DescriptionField(
                fieldValue = viewModel.description,
                onValueChange = { viewModel.description = it },
            )
        }

        FormField(null, horizontalArrangement = Arrangement.SpaceBetween) {
            TextIconButton(
                onClick = {
                    onDismiss()
                    // TODO: show alert dialog before deleting data
                    viewModel.cancel()
                },
                icon = { Icon(painterResource(R.drawable.close_24px), "Cancel") },
                text = { Text("Cancel") },
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

    when (val dialogState = dialogState) {
        DialogState.DatePicker -> DatePickerDialog(
            selectedDate = viewModel.date,
            onDismiss = dialogDismiss,
            onSubmit = { viewModel.date = it },
        )
        is DialogState.LocationPicker -> LocationPickerDialog(
            viewModel = viewModel.location,
            state = dialogState.state,
            onStateChange = { dialogState.state = it },
            onDismiss = dialogDismiss,
        )
        is DialogState.Items -> ItemsDialog(
            viewModel = viewModel.items,
            locale = userLocale,
            currency = viewModel.currency,
            state = dialogState.state,
            onStateChange = { dialogState.state = it },
            onDismiss = dialogDismiss,
        )
        DialogState.TagsPicker -> TagsPickerDialog(
            viewModel = viewModel.tags,
            onDismiss = dialogDismiss,
        )
        DialogState.None -> { }
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
fun PriceField(
    modifier: Modifier = Modifier,
    viewModel: NewTransactionViewModel,
    locale: Locale,
) {
    // TextField must be **disabled** when [items list][ItemsViewModel] is populated.
    val enabled = viewModel.items.items.isEmpty()
    val currency = viewModel.currency
    val state = RealNumberFieldState.rememberMoneyFieldState(viewModel.customPrice, emptyInitialTextIfZero = true, viewModel.currency, locale)

    // Will show tooltip on any interaction if disabled.
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val tooltipState = rememberTooltipState()

    val textField = @Composable {
        val scope = rememberCoroutineScope()
        var currencyMenuOpen by remember { mutableStateOf(false) }
        val shape = MaterialTheme.shapes.medium
        val fieldText = if (enabled) {
            state.fieldText
        } else {
            currency.formatPrice(viewModel.items.totalPrice, locale)
        }

        OutlinedTextField(
            modifier = modifier
                // FIXME: TextField does not grow with the input text's width
                .widthIn(min = 50.dp, max = FIELD_MAX_WIDTH)
                .width(IntrinsicSize.Min)
                .run { if (!enabled) { this
                    .clip(shape)
                    .hoverable(interactionSource = interactionSource)
                    .focusable(interactionSource = interactionSource)
                    .clickable(interactionSource = interactionSource) { scope.launch { tooltipState.show() } }
                } else this },
            interactionSource = interactionSource,
            value = fieldText,
            onValueChange = { state.fieldText = it },
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
            shape = shape,
            keyboardOptions = RealNumberFieldState.keyboardOptions,
            singleLine = true,
            enabled = enabled,
            readOnly = !enabled,
            leadingIcon = {
                CurrencySelectorButton(
                    showCurrencyMenu = currencyMenuOpen,
                    onMenuStateChange = { currencyMenuOpen = it },
                    locale = locale,
                    selectedCurrency = currency,
                    onCurrencyChange = { viewModel.currency = it },
                )
            },
            placeholder = {
                Text(viewModel.currency.formatPrice(0.0, locale),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outline,
                )
            },
            isError = enabled && state.isError,
            supportingText = state.parseResult.let { if (enabled && it is Result.Err) {{
                Text("Error: ${it.error.message}")
            }} else null },
        )
    }

    if (enabled) {
        textField()
    } else {
        LaunchedEffect(isFocused, isHovered) {
            if (isFocused || isHovered) {
                tooltipState.show()
            }
        }
        PlainToolTipBox(
            text = "Can't set price when items exist.\nClear items to set custom price.",
            state = tooltipState,
            content = textField,
        )
    }
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
    locale: Locale,
    selectedCurrency: Currency,
    onCurrencyChange: (Currency) -> Unit,
) {
    val localeCurrency = remember(locale) { Currency.getInstance(locale) }

    PlainToolTipBox("Select currency") {
        TextButton(
            modifier = modifier.padding(start = 8.dp)
                .semantics {
                    stateDescription = "${selectedCurrency.currencyCode} ${selectedCurrency.displayName}"
                },
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
            Icon(painterResource(R.drawable.arrow_drop_down_24px), null)

            val icon = getCurrencyIcon(selectedCurrency)
            val code = selectedCurrency.currencyCode

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
            delay(FIELD_TIMEOUT)
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
                    modifier = Modifier
                        .run { if (currency == selectedCurrency) {
                            background(MaterialTheme.colorScheme.surfaceDim)
                        } else this },
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
            PlainToolTipBox("Clear recents history") {
                this.MenuItem(
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.close_24px), null,
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
                    location.selectedLocation = DbEntry(0u, FAKE_LOCATIONS[0u]!!)
                    items.items.addAll(FAKE_ITEMS)
                    items.tax = Tax.Percentage(2.5)
                    tags.useAlternativeTags(FAKE_TAGS)
                    tags.selectedTags.addAll(FAKE_TAGS.subList(0, 3).map { it.name })
                },
                userLocale = remember { Locale.getDefault() },
                onDismiss = { }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 150, heightDp = 400)
@Composable
fun CurrenciesDropDownPreview() {
    val locale = remember { Locale.getDefault() }

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
            locale = locale,
            selectedCurrency = remember(locale) { Currency.getInstance(locale) },
            onCurrencyChange = { },
        )
    }
}
