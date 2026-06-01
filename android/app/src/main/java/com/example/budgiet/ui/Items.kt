@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.budgiet.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.example.budgiet.R
import com.example.budgiet.Result
import com.example.budgiet.formatPrice
import com.example.budgiet.getCurrencyIcon
import com.example.budgiet.into
import com.example.budgiet.parsePrice
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.ActionDialog
import com.example.budgiet.ui.utils.ActionDialogPadding
import com.example.budgiet.ui.utils.Corner
import com.example.budgiet.ui.utils.FilledTextIconButton
import com.example.budgiet.ui.utils.ItemActionsMenu
import com.example.budgiet.ui.utils.ListColumn
import com.example.budgiet.ui.utils.MoneyFieldWrapper
import com.example.budgiet.ui.utils.PlainToolTipBox
import com.example.budgiet.ui.utils.RealNumberFieldState
import com.example.budgiet.ui.utils.halfRoundedCornerShape
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

val FAKE_ITEMS = listOf(
    Item("Ham", 5.99, 1.0),
    Item("Cheese", 2.59, 4.0),
    Item("Bread", 4.19, 2.0),
    Item("Crackers", 1.89, 2.0),
    Item("Chicken", 4.99, 3.5),
)

data class Item(
    val name: String,
    // val classification: ???
    val unitPrice: Double, // TODO: use money struct
    val amount: Double,
    // TODO: val unitType: Pounds, liters, unit, etc.
)

class ItemsViewModel: ViewModel() {
    sealed class TaxType {
        // TODO: use Money type later for this
        object CurrencyAmount: TaxType()
        object Percentage: TaxType()

        override fun toString() = when (this) {
            is CurrencyAmount -> "currency amount"
            is Percentage -> "percentage"
        }
    }

    // TODO: Have a database table of item names the user has used, and have a different screen to show aggregate data of each item across transactions.
    val items = mutableStateListOf<Item>()
    var taxType by mutableStateOf<TaxType>(TaxType.CurrencyAmount)
    var taxValue by mutableDoubleStateOf(0.0)

    val totalPrice: Double get() {
        val itemsSum = this.items.sumOf { it.amount * it.unitPrice }
        val taxAmount = when (this.taxType) {
            is TaxType.CurrencyAmount -> this.taxValue
            is TaxType.Percentage -> itemsSum * this.taxValue * 0.01
        }
        return itemsSum + taxAmount
    }

    /** Produces a message to display in the [NewTransactionForm] [ItemsField].
     * Includes the number of items,  */
    fun displayFieldSummary(currency: Currency, locale: Locale): String {
        val itemsCount = this.items
            .sumOf { it.amount } // FIXME: count items with a measure other than unit as a single item, if its units count each unit, otherwise count as 1
            .toInt()
        val itemsPrice = currency.formatPrice(
            this.items.sumOf { it.amount * it.unitPrice },
            locale = locale,
        )
        val displayTax = if (this.taxValue != 0.0 && this.items.isNotEmpty()) {
            when (this.taxType) {
                is TaxType.CurrencyAmount -> " + ${currency.symbol}${currency.formatPrice(this.taxValue, locale)} tax"
                is TaxType.Percentage -> " + ${this.taxValue}% tax"
            }
        } else ""

        val itemsWord = if (itemsCount == 1) "item" else "items"

        return "$itemsCount $itemsWord (${currency.symbol}$itemsPrice)$displayTax"
    }

    fun removeItem(name: String) {
        val idx = this.items.indexOfFirst { it.name == name }
        this.items.removeAt(idx)
    }
    fun reset() {
        this.items.clear()
        this.taxValue = 0.0
    }

    fun validateName(name: String, isNew: Boolean = true): Result<Unit> {
        val msg = if (name.isEmpty()) {
            "Name must not be empty"
        } else if (isNew && this.items.find { it.name == name } != null) {
            "An item with this name already exists. Edit the price/amount of that item instead."
        } else {
            null
        }

        return msg?.let { Result.Err(Exception(msg)) }
            ?: Result.Ok(Unit)
    }
    fun parseAmount(value: String): Result<Double> {
        return runCatching { value.toDouble() }.into()
        // TODO: real impl
    }
}

/** Displays the total number of [Item]s and their *total cost*,
 * along with 2 buttons to bring up dialogs to view/edit the items or scan a receipt.
 *
 * @param onClickAdd The action that runs when the `"Add items"` button is *clicked*.
 *   This action should open the [ItemsViewDialog].
 * @param onClickOcr The action that runs when the `"Scan a receipt"` button is *clicked*.
 *   This action should open the [ItemsOcrDialog].*/
@Composable
fun RowScope.ItemsField(
    viewModel: ItemsViewModel,
    locale: Locale,
    currency: Currency,
    onClickAdd: () -> Unit,
    onClickOcr: () -> Unit,
) {
    if (viewModel.items.isNotEmpty()) {
        Text(viewModel.displayFieldSummary(currency, locale),
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            textAlign = TextAlign.End,
            overflow = TextOverflow.Visible,
        )
    }

    // Collapse button if there are items (like tags button).
    if (viewModel.items.isEmpty()) {
        PlainToolTipBox("Add items") {
            FilledTextIconButton(
                icon = { Icon(painterResource(R.drawable.add_24px), null) },
                text = { Text("Add items") },
                shape = halfRoundedCornerShape(Corner.Right),
                colors = ButtonDefaults.filledTonalButtonColors(),
                onClick = onClickAdd,
            )
        }
        PlainToolTipBox("Scan a receipt") {
            FilledIconButton(
                shape = halfRoundedCornerShape(Corner.Left),
                onClick = onClickOcr,
            ) {
                Icon(painterResource(R.drawable.document_scanner_24px),  null,
                    modifier = Modifier.padding(start = 6.dp, end = 10.dp),
                )
            }
        }
    } else {
        PlainToolTipBox("View items") {
            FilledTonalIconButton (
                content = { Icon(painterResource(R.drawable.receipt_long_24px), null) },
                onClick = onClickAdd,
            )
        }
    }
}

/** State structure for [ItemsDialog].
 *
 * Can be one of **[View]**, **[New]**, **[Edit]**, and **[Ocr]**. */
sealed class ItemsDialogState {
    /** Displays the List of [Item]s with no pending actions. */
    object View: ItemsDialogState()
    /** Displays a series of **TextField**s below the List of [Item]s to create a new [Item].
     * The `"Submit"` button adds the new item and returns the **state** to [View]. */
    object New: ItemsDialogState()
    /** Converts the row of a specific [Item] into a series of **TextField**s to edit the data of that [Item].
     * The `"Submit"` button modifies the items list in memory and returns the **state** to [View]. */
    class Edit(val value: Item): ItemsDialogState()
    /** Prompts user for a scan of a receipt (see [ItemsOcrDialog]). */
    object Ocr: ItemsDialogState()
}
@Composable
fun ItemsDialog(
    modifier: Modifier = Modifier,
    viewModel: ItemsViewModel,
    locale: Locale,
    currency: Currency,
    state: ItemsDialogState,
    onStateChange: (ItemsDialogState) -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        is ItemsDialogState.Ocr -> ItemsOcrDialog(
            modifier = modifier,
            onDismiss = onDismiss,
        )
        else -> ItemsViewDialog(
            modifier = modifier,
            viewModel = viewModel,
            state = state,
            onStateChange = onStateChange,
            currency = currency,
            locale = locale,
            onDismiss = onDismiss,
        )
    }
}

/** Displays the [Dialog][ActionDialog] that contain the [Item]s data for this transaction.
 *
 * The UI of the dialog depends on the **`state`** (see [ItemsDialogState]).
 * The dialog can be in any state *except* [Ocr][ItemsDialogState.Ocr], since that is handled in a separate composable. */
@Composable
private fun ItemsViewDialog(
    modifier: Modifier = Modifier,
    viewModel: ItemsViewModel,
    locale: Locale,
    currency: Currency,
    state: ItemsDialogState,
    onStateChange: (ItemsDialogState) -> Unit,
    onDismiss: () -> Unit,
) {
    val newItemCollapseTransition = updateTransition(state, "NewItemCollapseButton")

    var editItemName by remember(state) { mutableStateOf(when (state) {
        is ItemsDialogState.Edit -> state.value.name
        else -> ""
    }) }
    var editItemNameError by remember(state) { mutableStateOf<Result<Unit>>(Result.Ok(Unit)) }
    // Don't use the currency or locale as keys here; the field value should only be reset when state changes.
    val editItemPriceState = remember(state) { RealNumberFieldState(initialFieldValue = when (state) {
        is ItemsDialogState.Edit -> currency.formatPrice(state.value.unitPrice, locale)
        else -> ""
    }) }
    val editItemAmountState = remember(state) { RealNumberFieldState(initialFieldValue = when (state) {
        is ItemsDialogState.Edit -> state.value.amount.toString()
        else -> ""
    }) }
    val taxAmountState = remember { when (viewModel.taxType) {
        is ItemsViewModel.TaxType.CurrencyAmount -> RealNumberFieldState.moneyFieldState(viewModel.taxValue, currency, locale)
        is ItemsViewModel.TaxType.Percentage -> RealNumberFieldState(viewModel.taxValue)
    } }

    val columnSpacing = 2.dp
    val nameColumnWeight = 0.6f
    val priceColumnWeight = 0.3f
    val amountColumnWeight = 0.3f
    val totalColumnWeight = 0.3f
    val columnLabelTextStyle = MaterialTheme.typography.labelMedium
    val rowShape = MaterialTheme.shapes.medium
    val dividerPaddingSize = ActionDialogPadding.TightlyPacked.actionsSpacerHeight * 2
    val itemColumnFieldPadding = PaddingValues(vertical = 14.dp, horizontal = 8.dp)
    val itemFieldPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        padding = ActionDialogPadding.TightlyPacked,
        title = { Text("Transaction items",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ActionDialogPadding.Default.dialogEdges.calculateStartPadding(layoutDirection)),
            style = MaterialTheme.typography.headlineSmall,
        ) },
        actions = {
            newItemCollapseTransition.AnimatedContent { state ->
                when (state) {
                    is ItemsDialogState.New,
                    is ItemsDialogState.Edit -> {
                        TextButton(onClick = { onStateChange(ItemsDialogState.View) }) {
                            Text("Cancel")
                        }
                    }
                    else -> if (viewModel.items.isNotEmpty()) {
                        var showConfirmationDialog by remember { mutableStateOf(false) }

                        FilledTextIconButton(
                            icon = { Icon(painterResource(R.drawable.delete_forever), null) },
                            text = { Text("Clear") },
                            modifier = Modifier.border(ButtonDefaults.outlinedButtonBorder().width,MaterialTheme.colorScheme.error, ButtonDefaults.shape),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            onClick = { showConfirmationDialog = true },
                        )

                        if (showConfirmationDialog) {
                            AlertDialog(
                                onDismissRequest = { showConfirmationDialog = false },
                                title = { Text("Discard items") },
                                text = { Text("Do you want to delete all the items from the list?") },
                                confirmButton = { FilledTextIconButton(
                                    modifier = Modifier.semantics { contentDescription = "confirm" },
                                    icon = { Icon(painterResource(R.drawable.delete_forever), null) },
                                    text = { Text("Clear") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    ),
                                    onClick = {
                                        showConfirmationDialog = false
                                        viewModel.reset()
                                        taxAmountState.fieldText.value = ""
                                    },
                                ) },
                                dismissButton = {
                                    TextButton(onClick = { showConfirmationDialog = false }) {
                                        Text("Cancel")
                                    }
                                },
                            )
                        }
                    } else { Box { } }
                }
            }

            // The "Submit" button is different depending on whether the user is currently inputting data for a new item.
            //   If it is, the button adds the new item to the list.
            //   Otherwise, the button applies changes made to the item list and closes the dialog.
            newItemCollapseTransition.AnimatedContent { state ->
                val canSubmit = { when (state) {
                    is ItemsDialogState.View -> { taxAmountState.parseResult.value is Result.Ok }
                    is ItemsDialogState.New -> {
                        editItemNameError is Result.Ok
                        && editItemPriceState.parseResult.value is Result.Ok
                        && editItemAmountState.parseResult.value is Result.Ok
                    }
                    is ItemsDialogState.Edit -> {
                        val item = state.value

                        (editItemName != item.name
                        || editItemPriceState.fieldText.value != currency.formatPrice(item.unitPrice, locale)
                        || editItemAmountState.fieldText.value != item.amount.toString()
                        ) && editItemNameError is Result.Ok
                        && editItemPriceState.parseResult.value is Result.Ok
                        && editItemAmountState.parseResult.value is Result.Ok
                    }
                    else -> { true }
                } }
                val submit = {
                    editItemNameError = viewModel.validateName(editItemName, isNew = state !is ItemsDialogState.Edit)
                    editItemPriceState.parseResult.value = currency.parsePrice(editItemPriceState.fieldText.value, locale)
                    editItemAmountState.parseResult.value = viewModel.parseAmount(editItemAmountState.fieldText.value)

                    if (canSubmit()) {
                        val newData = Item(editItemName, editItemPriceState.parseResult.value.unwrap(), editItemAmountState.parseResult.value.unwrap())
                        when (state) {
                            is ItemsDialogState.Edit -> {
                                val idx = viewModel.items.indexOfFirst { it.name == state.value.name }
                                // There are no data races, the item will exist, so idx can't be -1 and an exception will never be thrown.
                                viewModel.items[idx] = newData
                            }
                            is ItemsDialogState.New -> viewModel.items.add(newData)
                            else -> throw Exception("UNREACHABLE")

                        }
                        onStateChange(ItemsDialogState.View)
                    }
                }

                when (state) {
                    is ItemsDialogState.View -> {
                        PlainToolTipBox("Close items dialog") {
                            FilledTextIconButton(
                                icon = { Icon(painterResource(R.drawable.check_24px), null) },
                                text = { Text("Done") },
                                enabled = canSubmit(),
                                onClick = onDismiss,
                            )
                        }
                    }
                    is ItemsDialogState.New -> {
                        PlainToolTipBox("Submit new item") {
                            FilledTextIconButton(
                                icon = { Icon(painterResource(R.drawable.check_24px), null) },
                                text = { Text("Submit") },
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                enabled = canSubmit(),
                                onClick = submit,
                            )
                        }
                    }
                    is ItemsDialogState.Edit -> {
                        PlainToolTipBox("Save changes") {
                            FilledTextIconButton(
                                icon = { Icon(painterResource(R.drawable.check_24px), null) },
                                text = { Text("Save edit") },
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                enabled = canSubmit(),
                                onClick = submit,
                            )
                        }
                    }
                    is ItemsDialogState.Ocr -> throw Exception("Unreachable")
                }
            }
        },
    ) {
        if (viewModel.items.isEmpty()) {
            Column(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(ListItemDefaults.containerColor)
                    .padding(ActionDialogPadding.Default.dialogEdges),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("There are no items.", textAlign = TextAlign.Center)
                Text("Press \"New item\" to create an entry", textAlign = TextAlign.Center)
            }
        } else {
            val showTotalColumn = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)
                .windowSizeClass
                .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

            Row { CompositionLocalProvider(
                LocalTextStyle provides columnLabelTextStyle,
            ) {
                Text("Name", Modifier.weight(nameColumnWeight))
                Text("Price (${currency.symbol})", Modifier.weight(priceColumnWeight))
                Text("Amount", Modifier.weight(amountColumnWeight))
                if (showTotalColumn) {
                    Text("Total (${currency.symbol})", Modifier.weight(totalColumnWeight))
                }
            } }

            ListColumn {
                this.items(
                    items = viewModel.items,
                    key = { it.name },
                ) { item ->
                    Column {
                        var showMenu by remember { mutableStateOf(false) }
                        val isEditing = state is ItemsDialogState.Edit && state.value.name == item.name
                        val isSelected = showMenu || isEditing
                        var focusedField by remember { mutableStateOf<FocusRequester?>(null) }

                        val middleColumnShape = MaterialTheme.shapes.extraSmall
                        val startColumnShape = halfRoundedCornerShape(
                            Corner.Right,
                            sharpSize = middleColumnShape.bottomEnd,
                            roundSize = rowShape.bottomStart,
                        )
                        val endColumnShape = halfRoundedCornerShape(
                            Corner.Left,
                            sharpSize = middleColumnShape.bottomStart,
                            roundSize = rowShape.bottomEnd,
                        )

                        @Composable
                        fun RowScope.ItemColumn(
                            modifier: Modifier = Modifier,
                            weight: Float,
                            shape: Shape = middleColumnShape,
                            onLongClick: () -> Unit,
                            content: @Composable BoxScope.() -> Unit,
                        ) { Box(
                            modifier = modifier
                                .weight(weight)
                                .clip(shape)
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = onLongClick,
                                )
                                .background(if (isSelected) {
                                    MaterialTheme.colorScheme.surfaceContainerLowest
                                } else {
                                    ListItemDefaults.containerColor
                                })
                                .then(modifier)
                                .padding(itemColumnFieldPadding),
                            content = content,
                        ) }

                        @Composable
                        fun RowScope.ItemColumnField(
                            modifier: Modifier = Modifier,
                            weight: Float,
                            shape: Shape = middleColumnShape,
                            staticValue: String,
                            editingValue: String,
                            onEditingValueChange: (String) -> Unit,
                            isError: Boolean,
                            keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
                        ) {
                            val focusRequester = remember { FocusRequester() }

                            ItemColumn(
                                modifier = modifier,
                                weight = weight,
                                shape = shape,
                                onLongClick = {
                                    showMenu = true
                                    focusedField = focusRequester
                                },
                            ) {
                                if (isEditing) {
                                    SmallBasicTextField(
                                        modifier = Modifier.focusRequester(focusRequester.apply {
                                            if (focusedField === this) {
                                                focusRequester.requestFocus()
                                            }
                                        }),
                                        value = editingValue,
                                        onValueChange = onEditingValueChange,
                                        isError = isError,
                                        keyboardOptions = keyboardOptions,
                                    )
                                } else {
                                    TextAutoSized(staticValue)
                                }
                            }
                        }

                        newItemCollapseTransition.AnimatedVisibility(visible = { isEditing }) {
                            Text("Edit item",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(start = columnSpacing * 4)
                            )
                        }

                        Box(contentAlignment = Alignment.CenterStart) {
                            var amountColumnOffset by remember(density, LocalTextStyle.current) { mutableStateOf<Float?>(null) }
                            var totalColumnOffset by remember(density, LocalTextStyle.current) { mutableStateOf<Float?>(null) }

                            Row(
                                modifier = Modifier.applyHeightToList(),
                                horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                            ) {
                                ItemColumnField(
                                    weight = nameColumnWeight,
                                    shape = startColumnShape,
                                    staticValue = item.name,
                                    editingValue = editItemName,
                                    onEditingValueChange = {
                                        editItemName = it
                                        editItemNameError = viewModel.validateName(it, isNew = false)
                                    },
                                    isError = editItemNameError is Result.Err,
                                )
                                MoneyFieldWrapper(
                                    state = editItemPriceState,
                                    currency = currency,
                                    locale = locale,
                                ) { state ->
                                    ItemColumnField(
                                        weight = priceColumnWeight,
                                        staticValue = currency.formatPrice(item.unitPrice, locale),
                                        editingValue = state.fieldText.value,
                                        onEditingValueChange = { state.fieldText.value = it },
                                        isError = state.parseResult.value is Result.Err,
                                        keyboardOptions = state.keyboardOptions,
                                    )
                                }
                                ItemColumnField(
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        if (amountColumnOffset == null) {
                                            amountColumnOffset = coords.positionInParent().x
                                        }
                                    },
                                    weight = amountColumnWeight,
                                    shape = if (showTotalColumn) { middleColumnShape } else { endColumnShape },
                                    staticValue = item.amount.toString(),
                                    editingValue = editItemAmountState.fieldText.value,
                                    onEditingValueChange = {
                                        editItemAmountState.fieldText.value = it
                                        editItemAmountState.parseResult.value = viewModel.parseAmount(it)
                                    },
                                    isError = editItemAmountState.parseResult.value is Result.Err,
                                    keyboardOptions = RealNumberFieldState.keyboardOptions,
                                )
                                // Column with total (=) for this specific item (only on wider screens).
                                if (showTotalColumn) {
                                    ItemColumn(
                                        modifier = Modifier.onGloballyPositioned { coords ->
                                            if (totalColumnOffset == null) {
                                                totalColumnOffset = coords.positionInParent().x
                                            }
                                        },
                                        weight = totalColumnWeight,
                                        shape = endColumnShape,
                                        onLongClick = { showMenu = true },
                                    ) {
                                        TextAutoSized(currency.formatPrice(item.unitPrice * item.amount, locale))
                                    }
                                }
                            }

                            val iconSize = 18.dp
                            // Multiply icon.
                            Icon(painterResource(R.drawable.close_24px), contentDescription = null, modifier = Modifier
                                .size(iconSize)
                                .offset { IntOffset(x = amountColumnOffset?.roundToInt()?.let {
                                    it - (iconSize + columnSpacing).roundToPx() / 2
                                } ?: 0, y = 0) },
                            )
                            // Equal icon.
                            if (showTotalColumn) {
                                Icon(painterResource(R.drawable.equal_24px), contentDescription = null, modifier = Modifier
                                    .size(iconSize)
                                    .offset { IntOffset(x = totalColumnOffset?.roundToInt()?.let {
                                        it - (iconSize + columnSpacing).roundToPx() / 2
                                    } ?: 0, y = 0) },
                                )
                            }

                            ItemActionsMenu(
                                expanded = showMenu,
                                onDismiss = { showMenu = false },
                                onEditClick = { onStateChange(ItemsDialogState.Edit(item)) },
                                onDeleteClick = { viewModel.removeItem(item.name) },
                            )
                        }
                    }
                }
            }

            newItemCollapseTransition.AnimatedVisibility(visible = { state ->
                state is ItemsDialogState.View
            }) { Row {
                val currencyIcon = getCurrencyIcon(currency) ?: painterResource(R.drawable.currency_dollar_24px)
                
                Column(Modifier.weight(0.6f)) {
                    Text("Tax value (optional)",
                        style = columnLabelTextStyle,
                        modifier = Modifier.padding(top = dividerPaddingSize, start = 10.dp)
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(columnSpacing * 2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlainToolTipBox("Switch tax type") {
                            TextButton(
                                modifier = Modifier.semantics { stateDescription = viewModel.taxType.toString() },
                                onClick = {
                                    viewModel.taxType = when (viewModel.taxType) {
                                        is ItemsViewModel.TaxType.CurrencyAmount -> ItemsViewModel.TaxType.Percentage
                                        is ItemsViewModel.TaxType.Percentage -> ItemsViewModel.TaxType.CurrencyAmount
                                    }
                                },
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp)
                            ) {
                                Icon(painterResource(R.drawable.arrow_drop_down_24px), null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Icon(when (viewModel.taxType) {
                                    is ItemsViewModel.TaxType.CurrencyAmount -> currencyIcon
                                    is ItemsViewModel.TaxType.Percentage -> painterResource(R.drawable.percent_24px)
                                }, null)
                            }
                        }

                        Box(Modifier
                            .clip(rowShape)
                            .background(ListItemDefaults.containerColor)
                            .padding(itemFieldPadding)
                            .fillMaxWidth()
                        ) {
                            when (viewModel.taxType) {
                                is ItemsViewModel.TaxType.CurrencyAmount -> {
                                    MoneyFieldWrapper(
                                        state = taxAmountState,
                                        onPriceChange = { viewModel.taxValue = it },
                                        currency = currency,
                                        locale = locale,
                                    ) { state ->
                                        SmallBasicTextField(
                                            modifier = Modifier.semantics { contentDescription = "Tax value" },
                                            value = state.fieldText.value,
                                            onValueChange = { state.fieldText.value = it },
                                            isError = state.parseResult.value is Result.Err,
                                            placeholderText = currency.formatPrice(0.0, locale),
                                            keyboardOptions = state.keyboardOptions,
                                        )
                                    }
                                }
                                is ItemsViewModel.TaxType.Percentage -> {
                                    fun parse(value: String) {
                                        val parseResult = runCatching {
                                            if (value.isEmpty()) { 0.0 } else { value.toDouble() }
                                        }.into()
                                        taxAmountState.parseResult.value = parseResult
                                        if (parseResult is Result.Ok) {
                                            viewModel.taxValue = parseResult.value
                                        }
                                    }

                                    // Reset error value on first compose.
                                    LaunchedEffect(Unit) { parse(taxAmountState.fieldText.value) }

                                    SmallBasicTextField(
                                        modifier = Modifier.semantics { contentDescription = "Tax value" },
                                        value = taxAmountState.fieldText.value,
                                        onValueChange = {
                                            taxAmountState.fieldText.value = it
                                            parse(it)
                                        },
                                        isError = taxAmountState.parseResult.value is Result.Err,
                                        placeholderText = "0.0",
                                        keyboardOptions = RealNumberFieldState.keyboardOptions,
                                    )
                                }
                            }

                        }
                    }
                }
                Column(Modifier.weight(0.5f)) {
                    Text("Total price",
                        style = columnLabelTextStyle,
                        modifier = Modifier.padding(top = dividerPaddingSize, start = 24.dp)
                    )

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(currencyIcon, null)

                        Box(Modifier
                            .clip(rowShape)
                            .background(ListItemDefaults.containerColor)
                            .padding(itemFieldPadding)
                            .fillMaxWidth()
                        ) {
                            Text(currency.formatPrice(viewModel.totalPrice, locale),
                                modifier = Modifier.semantics { contentDescription = "Total price amount" },
                            )
                        }
                    }
                }
            } }
        }

        // New Item button ...
        newItemCollapseTransition.AnimatedContent(
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { state ->
            when (state) {
                is ItemsDialogState.View -> {
                    FilledTextIconButton(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                            .wrapContentWidth()
                            .padding(top = dividerPaddingSize),
                        icon = { Icon(painterResource(R.drawable.add_24px), null) },
                        text = { Text("New item") },
                        onClick = { onStateChange(ItemsDialogState.New) },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                    )
                }
                is ItemsDialogState.New -> Column {
                    @Composable
                    fun NewItemField(
                        modifier: Modifier = Modifier,
                        label: String,
                        value: String,
                        onValueChange: (String) -> Unit,
                        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
                        isError: Boolean,
                    ) {
                        OutlinedTextField(
                            modifier = modifier,
                            label = { TextAutoSized(label) },
                            value = value,
                            onValueChange = onValueChange,
                            shape = MaterialTheme.shapes.medium,
                            isError = isError,
                            keyboardOptions = keyboardOptions,
                            singleLine = true,
                            maxLines = 1,
                        )
                    }

                    HorizontalDivider(Modifier.padding(vertical = dividerPaddingSize))
                    Text("New Item", style = columnLabelTextStyle)

                    Row(
                        modifier = Modifier.semantics { contentDescription = "New item fields" },
                        horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                    ) {
                        NewItemField(
                            modifier = Modifier.weight(nameColumnWeight),
                            label = "Name",
                            value = editItemName,
                            onValueChange = {
                                editItemName = it
                                editItemNameError = viewModel.validateName(it, isNew = true)
                            },
                            isError = editItemNameError is Result.Err,
                        )
                        MoneyFieldWrapper(
                            state = editItemPriceState,
                            currency = currency,
                            locale = locale,
                        ) { state ->
                            NewItemField(
                                modifier = Modifier.weight(priceColumnWeight),
                                label = "Price",
                                value = state.fieldText.value,
                                onValueChange = { state.fieldText.value = it },
                                isError = state.parseResult.value is Result.Err,
                                keyboardOptions = state.keyboardOptions,
                            )
                        }
                        NewItemField(
                            modifier = Modifier.weight(amountColumnWeight),
                            label = "Amount",
                            value = editItemAmountState.fieldText.value,
                            onValueChange = {
                                editItemAmountState.fieldText.value = it
                                editItemAmountState.parseResult.value = viewModel.parseAmount(it)
                            },
                            isError = editItemAmountState.parseResult.value is Result.Err,
                            keyboardOptions = RealNumberFieldState.keyboardOptions,
                        )
                    }
                }
                else -> { }
            }
        }

        // New/Edit item errors.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
            fun errorMsg(error: Throwable): String = error.message ?: error.javaClass.name
            newItemCollapseTransition.AnimatedContent { state ->
                when (state) {
                    is ItemsDialogState.View -> {
                        if (taxAmountState.parseResult.value is Result.Err) {
                            Text("Tax error: ${errorMsg(taxAmountState.parseResult.value.unwrapErr())}")
                        }
                    }
                    is ItemsDialogState.New,
                    is ItemsDialogState.Edit -> Column {
                        if (editItemNameError is Result.Err) {
                            Text("Name error: ${errorMsg(editItemNameError.unwrapErr())}")
                        }
                        if (editItemPriceState.parseResult.value is Result.Err) {
                            Text("Price error: ${errorMsg(editItemPriceState.parseResult.value.unwrapErr())}")
                        }
                        if (editItemAmountState.parseResult.value is Result.Err) {
                            Text("Amount error: ${errorMsg(editItemAmountState.parseResult.value.unwrapErr())}")
                        }
                    }
                    is ItemsDialogState.Ocr -> throw Exception("Unreachable")
                }
            }
        }
    }
}

/** Small TextField that fits inside the small columns of the Items list. */
@Composable
private fun SmallBasicTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    placeholderText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val lineColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    var cursorState by remember { mutableStateOf(when {
        value.isEmpty() -> TextRange.Zero
        else -> TextRange(value.length, value.length)
    }) }
    Box {
        if (value.isEmpty() && placeholderText != null) {
            Text(placeholderText, color = MaterialTheme.colorScheme.outline)
        }
        BasicTextField(
            modifier = modifier
                .drawBehind {
                    val lineStroke = 2.dp.toPx()
                    val lineNegativePadding = 3.dp.toPx()
                    val y = this.size.height - lineStroke + lineNegativePadding
                    this.drawLine(
                        color = lineColor,
                        start = Offset(-lineNegativePadding, y),
                        end = Offset(this.size.width + lineNegativePadding, y),
                        strokeWidth = lineStroke,
                        cap = StrokeCap.Round,
                    )
                },
            value = TextFieldValue(
                text = value,
                selection = cursorState,
            ),
            onValueChange = { newState ->
                cursorState = newState.selection
                if (newState.text != value) {
                    onValueChange(newState.text)
                }
            },
            textStyle = LocalTextStyle.current,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            maxLines = 1,
        )
    }
}

@Composable
fun TextAutoSized(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current,
) { Text(
    text = text,
    modifier = modifier,
    textAlign = textAlign,
    style = style,
    autoSize = TextAutoSize.StepBased(maxFontSize = style.fontSize),
    overflow = TextOverflow.Clip,
    maxLines = 1,
    softWrap = false,
) }

/** Shows a [Dialog][ActionDialog] to allow the user to ***scan*** a picture of a *digital or paper receipt*
 * either directly by taking a picture, or selecting an existing image on their device.
 *
 * Both of these methods use Intents, so they do not grant (or ask the user to grant) any device permissions. */
@Composable
private fun ItemsOcrDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        padding = ActionDialogPadding.TightlyPacked,
        title = { Text("Scan itemized receipt") },
        actions = {

        },
    ) {
        // TODO: Should have options to take a picture directly,
        //   or use an already existing picture from image picker (note: must not ask for files access)
    }
}

@Preview(showBackground = true)
@Composable
fun ItemsFieldPreview() {
    val locale = remember { Locale.getDefault() }

    BudgietTheme { Row {
        ItemsField(
            viewModel = viewModel<ItemsViewModel>(),
            locale = locale,
            currency = remember(locale) { Currency.getInstance(locale) },
            onClickAdd = { },
            onClickOcr = { },
        )
    } }
}

@Preview(showBackground = true)
@Composable
fun ItemsFieldFilledPreview() {
    val locale = remember { Locale.getDefault() }

    BudgietTheme { Row {
        ItemsField(
            viewModel = viewModel<ItemsViewModel>().apply {
                items.addAll(FAKE_ITEMS)
                taxType = ItemsViewModel.TaxType.CurrencyAmount
                taxValue = 4.08
            },
            locale = locale,
            currency = remember(locale) { Currency.getInstance(locale) },
            onClickAdd = { },
            onClickOcr = { },
        )
    } }
}

// FIXME: Fix previews from here down not showing.
@Preview(showBackground = true)
@Composable
fun ItemsDialogPreview() {
    val locale = remember { Locale.getDefault() }

    BudgietTheme {
        ItemsDialog(
            viewModel = viewModel<ItemsViewModel>().apply {
                items.addAll(FAKE_ITEMS)
            },
            locale = locale,
            currency = remember(locale) { Currency.getInstance(locale) },
            state = ItemsDialogState.View,
            onStateChange = { },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ItemsDialogNewItemPreview() {
    val locale = remember { Locale.getDefault() }

    BudgietTheme {
        ItemsDialog(
            viewModel = viewModel<ItemsViewModel>(),
            locale = locale,
            currency = remember(locale) { Currency.getInstance(locale) },
            state = ItemsDialogState.New,
            onStateChange = { },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ItemsDialogEditItemPreview() {
    val locale = remember { Locale.getDefault() }

    BudgietTheme {
        ItemsDialog(
            viewModel = viewModel<ItemsViewModel>().apply {
                items.add(FAKE_ITEMS.first())
            },
            locale = locale,
            currency = remember(locale) { Currency.getInstance(locale) },
            state = ItemsDialogState.Edit(FAKE_ITEMS.first()),
            onStateChange = { },
            onDismiss = { },
        )
    }
}
