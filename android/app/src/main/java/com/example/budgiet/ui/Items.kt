@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.budgiet.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CornerBasedShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
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
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.ActionDialog
import com.example.budgiet.ui.utils.ActionDialogPadding
import com.example.budgiet.ui.utils.AutoValidateTimings
import com.example.budgiet.ui.utils.Corner
import com.example.budgiet.ui.utils.FilledTextIconButton
import com.example.budgiet.ui.utils.ItemActionsMenu
import com.example.budgiet.ui.utils.ListColumn
import com.example.budgiet.ui.utils.PlainToolTipBox
import com.example.budgiet.ui.utils.RealNumberFieldState
import com.example.budgiet.ui.utils.StringTextFieldState
import com.example.budgiet.ui.utils.halfRoundedCornerShape
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

private val COLUMN_SPACING = 2.dp
private val ITEM_LIST_ROW_PADDING = PaddingValues(vertical = 14.dp, horizontal = 8.dp)
private val ITEM_ROW_SHAPE @Composable get() = MaterialTheme.shapes.medium
private const val NAME_COLUMN_WEIGHT = 0.6f
private const val PRICE_COLUMN_WEIGHT = 0.3f
private const val AMOUNT_COLUMN_WEIGHT = 0.3f
private const val TOTAL_COLUMN_WEIGHT = 0.3f

val FAKE_ITEMS = listOf(
    Item("Ham", 5.99, Amount.Units(1u)),
    Item("Cheese", 2.59, Amount.Measured(1.0, "lbs")),
    Item("Bread", 4.19, Amount.Units(2u)),
    Item("Crackers", 1.89, Amount.Units(1u)),
    Item("Chicken", 4.99, Amount.Measured(3.5, "lbs")),
)

data class Item(
    val name: String,
    val unitPrice: Double, // TODO: use money struct
    val amount: Amount,
) {
    // TODO: format to 3 decimal places max
    val totalPrice get() = when (this.amount) {
        is Amount.Measured -> this.unitPrice * this.amount.value
        is Amount.Units -> this.unitPrice * this.amount.value.toInt()
    }
}

sealed class Amount {
    class Measured(val value: Double, val label: String): Amount()
    class Units(val value: UInt): Amount()

    val textValue get() = when (this) {
        is Measured -> this.value.toString()
        is Units -> this.value.toString()
    }

    companion object {
        private const val LABEL_CHAR_LIMIT = 7

        fun validateLabel(label: String): Result<Unit> {
            return if (label.isEmpty()) {
                Result.Err(Exception("label must not be empty."))
            } else if (label.length > this.LABEL_CHAR_LIMIT) {
                Result.Err(Exception("The length of the label for an Amount must not exceed ${this.LABEL_CHAR_LIMIT} characters."))
            } else {
                Result.Ok(Unit)
            }
        }
    }
}

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
        val itemsSum = this.items.sumOf { it.totalPrice }
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
            .sumOf { when (it.amount) {
                is Amount.Measured -> 1
                is Amount.Units -> it.amount.value.toInt()
            } }
        val itemsPrice = currency.formatPrice(
            this.items.sumOf { it.totalPrice },
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
    val autoValidateTimings = AutoValidateTimings.rememberScope()

    // Don't use the currency or locale as keys here; the field value should only be reset when state changes.
    val editItemState = remember(state) {
        val preData = when (state) {
            is ItemsDialogState.Edit -> state.value
            else -> null
        }
        object {
            val name = StringTextFieldState(preData?.name ?: "",
                validator = { viewModel.validateName(it, isNew = state !is ItemsDialogState.Edit) },
                autoValidateTimings = null,
            )
            val unitPrice = RealNumberFieldState.moneyFieldState(preData?.unitPrice, currency, locale, autoValidateTimings)
            val amount = object {
                // false indicates Amount.Unit
                // Defaults to Amount.Unit when creating a new Item.
                var hasLabel by mutableStateOf(preData?.let { it.amount is Amount.Measured } ?: false)
                val value = run {
                    val parser = { s: String ->
                        Result.Ok(if (hasLabel) { s.toDouble() } else { s.toInt().toDouble() })
                    }
                    preData?.let {
                        RealNumberFieldState(it.amount.textValue, parser = parser, autoValidateTimings = null)
                    } ?: RealNumberFieldState("", parser = parser, autoValidateTimings = null)
                }
                val label = StringTextFieldState(
                    initialValue = when (val amount = preData?.amount) {
                        is Amount.Measured -> amount.label
                        is Amount.Units, null -> ""
                    },
                    validator = { Amount.validateLabel(it) },
                    autoValidateTimings = null,
                )
            }
        }
    }
    val taxAmountState = remember { when (viewModel.taxType) {
        is ItemsViewModel.TaxType.CurrencyAmount -> RealNumberFieldState.moneyFieldState(viewModel.taxValue, currency, locale, autoValidateTimings)
        is ItemsViewModel.TaxType.Percentage -> RealNumberFieldState(viewModel.taxValue,
            parser = { runCatching { if (it.isEmpty()) { 0.0 } else { it.toDouble() } }.into() },
        )
    } }

    val columnLabelTextStyle = MaterialTheme.typography.labelMedium
    val dividerPaddingSize = ActionDialogPadding.TightlyPacked.actionsSpacerHeight * 2
    val itemFieldPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp)
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
                                        taxAmountState.fieldText = ""
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
                val canSubmit = {
                    val containsNoErrors =
                        !editItemState.name.isError
                        && !editItemState.unitPrice.isError
                        && !editItemState.amount.value.isError
                        && (!editItemState.amount.hasLabel || !editItemState.amount.label.isError)

                    when (state) {
                        is ItemsDialogState.View -> { !taxAmountState.isError }
                        is ItemsDialogState.New -> containsNoErrors
                        is ItemsDialogState.Edit -> run {
                            val item = state.value
                            // Allow submitting only if any field was modified.
                            editItemState.name.fieldText != item.name
                            || editItemState.unitPrice.fieldText != currency.formatPrice(item.unitPrice, locale)
                            || editItemState.amount.value.fieldText != item.amount.textValue
                            || when (item.amount) {
                                is Amount.Measured -> if (editItemState.amount.hasLabel) { editItemState.amount.label.fieldText != item.amount.label } else { true }
                                is Amount.Units -> editItemState.amount.hasLabel
                            }
                        } && containsNoErrors
                        else -> { true }
                    }
                }
                val submit = {
                    editItemState.name.doValidate()
                    editItemState.unitPrice.doValidate()
                    editItemState.amount.value.doValidate()
                    if (editItemState.amount.hasLabel) {
                        editItemState.amount.label.doValidate()
                    }

                    if (canSubmit()) {
                        val newData = Item(
                            name = editItemState.name.fieldText,
                            unitPrice = editItemState.unitPrice.parseResult.unwrap(),
                            amount = if (editItemState.amount.hasLabel) {
                                Amount.Measured(editItemState.amount.value.parseResult.unwrap(), editItemState.amount.label.fieldText)
                            } else {
                                Amount.Units(editItemState.amount.value.parseResult.unwrap().toUInt())
                            }
                        )
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
                Text("Name", Modifier.weight(NAME_COLUMN_WEIGHT))
                Text("Price (${currency.symbol})", Modifier.weight(PRICE_COLUMN_WEIGHT))
                Text("Amount", Modifier.weight(AMOUNT_COLUMN_WEIGHT))
                if (showTotalColumn) {
                    Text("Total (${currency.symbol})", Modifier.weight(TOTAL_COLUMN_WEIGHT))
                }
            } }

            var focusedField by remember { mutableStateOf<UInt?>(null) }
            val amountColumnOffset = remember(LocalDensity.current, LocalTextStyle.current) { mutableStateOf<Float?>(null) }
            val totalColumnOffset = remember(LocalDensity.current, LocalTextStyle.current) { mutableStateOf<Float?>(null) }

            // TODO: When Editing, scroll to half a row above the Editing Box.
            ListColumn {
                this.items(
                    items = viewModel.items,
                    key = { it.name },
                ) { item ->
                    var showMenu by remember { mutableStateOf(false) }

                    newItemCollapseTransition.AnimatedContent { state ->
                        val isEditing = state is ItemsDialogState.Edit && state.value.name == item.name

                        if (isEditing) {
                            EditingItemListBox(
                                currency = currency,
                                focusedField = focusedField,
                                nameState = editItemState.name,
                                priceState = editItemState.unitPrice,
                                amountValueState = editItemState.amount.value,
                                amountLabelState = editItemState.amount.label,
                                hasLabel = editItemState.amount.hasLabel,
                                onHasLabelChange = { editItemState.amount.hasLabel = it }
                            )
                        } else {
                            StaticItemListRow(
                                currency = currency,
                                locale = locale,
                                showTotalColumn = showTotalColumn,
                                isSelected = showMenu,
                                amountColumnOffset = amountColumnOffset,
                                totalColumnOffset = totalColumnOffset,
                                onLongClick = {
                                    showMenu = true
                                    focusedField = it
                                },
                                data = item,
                            )
                        }
                    }

                    ItemActionsMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        onEditClick = { onStateChange(ItemsDialogState.Edit(item)) },
                        onDeleteClick = { viewModel.removeItem(item.name) },
                    )
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
                        horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING * 2),
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
                            .clip(ITEM_ROW_SHAPE)
                            .background(ListItemDefaults.containerColor)
                            .padding(itemFieldPadding)
                            .fillMaxWidth()
                        ) {
                            when (viewModel.taxType) {
                                is ItemsViewModel.TaxType.CurrencyAmount -> {
                                    SmallBasicTextField(
                                        modifier = Modifier.semantics { contentDescription = "Tax value" },
                                        value = taxAmountState.fieldText,
                                        onValueChange = { taxAmountState.fieldText = it },
                                        isError = taxAmountState.isError,
                                        placeholderText = currency.formatPrice(0.0, locale),
                                        keyboardOptions = RealNumberFieldState.keyboardOptions,
                                    )
                                }
                                is ItemsViewModel.TaxType.Percentage -> {
                                    SmallBasicTextField(
                                        modifier = Modifier.semantics { contentDescription = "Tax value" },
                                        value = taxAmountState.fieldText,
                                        onValueChange = { taxAmountState.fieldText = it },
                                        isError = taxAmountState.isError,
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
                            .clip(ITEM_ROW_SHAPE)
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
                    HorizontalDivider(Modifier.padding(vertical = dividerPaddingSize))

                    EditingItemListBox(
                        boxLabel = "New Item",
                        currency = currency,
                        nameState = editItemState.name,
                        priceState = editItemState.unitPrice,
                        amountValueState = editItemState.amount.value,
                        amountLabelState = editItemState.amount.label,
                        hasLabel = editItemState.amount.hasLabel,
                        onHasLabelChange = { editItemState.amount.hasLabel = it }
                    )
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
                        if (taxAmountState.isError) {
                            Text("Tax error: ${errorMsg(taxAmountState.parseResult.unwrapErr())}")
                        }
                    }
                    is ItemsDialogState.New,
                    is ItemsDialogState.Edit -> Column {
                        if (editItemState.name.isError) {
                            Text("Name error: ${errorMsg(editItemState.name.parseResult.unwrapErr())}")
                        }
                        if (editItemState.unitPrice.isError) {
                            Text("Price error: ${errorMsg(editItemState.unitPrice.parseResult.unwrapErr())}")
                        }
                        if (editItemState.amount.value.isError) {
                            Text("Amount Value error: ${errorMsg(editItemState.amount.value.parseResult.unwrapErr())}")
                        }
                        if (editItemState.amount.hasLabel && editItemState.amount.label.isError) {
                            Text("Amount Label error: ${errorMsg(editItemState.amount.label.parseResult.unwrapErr())}")
                        }
                    }
                    is ItemsDialogState.Ocr -> throw Exception("Unreachable")
                }
            }
        }
    }
}

// TODO: doc, note total value only shows up on wide screens
@Composable
private fun StaticItemListRow(
    modifier: Modifier = Modifier,
    currency: Currency,
    locale: Locale,
    rowShape: CornerBasedShape = ITEM_ROW_SHAPE,
    showTotalColumn: Boolean,
    isSelected: Boolean,
    amountColumnOffset: MutableState<Float?>,
    totalColumnOffset: MutableState<Float?>,
    onLongClick: (column: UInt?) -> Unit,
    data: Item,
) {
    val columnShapes = ItemColumnsShapes(rowShape)

    Column(modifier) {
        Box(contentAlignment = Alignment.CenterStart) {
            Row(horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING)) {
                ItemColumn(
                    modifier = Modifier.weight(NAME_COLUMN_WEIGHT),
                    shape = columnShapes.Start,
                    isSelected = isSelected,
                    onLongClick = { onLongClick(0u) },
                    content = { TextAutoSized(data.name) },
                )
                ItemColumn(
                    modifier = Modifier.weight(PRICE_COLUMN_WEIGHT),
                    shape = columnShapes.Middle,
                    isSelected = isSelected,
                    onLongClick = { onLongClick(1u) },
                    content = { TextAutoSized(currency.formatPrice(data.unitPrice, locale)) },
                )
                ItemColumn(
                    modifier = Modifier.weight(AMOUNT_COLUMN_WEIGHT)
                        .onGloballyPositioned { coords ->
                            if (amountColumnOffset.value == null) {
                                amountColumnOffset.value = coords.positionInParent().x
                            }
                        },
                    shape = if (showTotalColumn) { columnShapes.Middle } else { columnShapes.End },
                    isSelected = isSelected,
                    onLongClick = { onLongClick(2u) },
                    content = { TextAutoSized("${data.amount.textValue}${
                        when (data.amount) {
                            is Amount.Measured -> " ${data.amount.label}"
                            is Amount.Units -> ""
                        }
                    }") },
                )
                if (showTotalColumn) {
                    ItemColumn(
                        modifier = Modifier.weight(TOTAL_COLUMN_WEIGHT)
                            .onGloballyPositioned { coords ->
                                if (totalColumnOffset.value == null) {
                                    totalColumnOffset.value = coords.positionInParent().x
                                }
                            },
                        shape = columnShapes.End,
                        isSelected = isSelected,
                        onLongClick = { onLongClick(null) },
                        content = { TextAutoSized(currency.formatPrice(data.totalPrice, locale)) },
                    )
                }
            }

            val iconSize = 18.dp
            // Multiply icon.
            Icon(painterResource(R.drawable.close_24px), contentDescription = null, modifier = Modifier
                .align(Alignment.CenterStart)
                .size(iconSize)
                .offset { IntOffset(x = amountColumnOffset.value?.roundToInt()?.let {
                    it - (iconSize + COLUMN_SPACING).roundToPx() / 2
                } ?: 0, y = 0) },
            )
            // Equal icon.
            if (showTotalColumn) {
                Icon(painterResource(R.drawable.equal_24px), contentDescription = null, modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(iconSize)
                    .offset { IntOffset(x = totalColumnOffset.value?.roundToInt()?.let {
                        it - (iconSize + COLUMN_SPACING).roundToPx() / 2
                    } ?: 0, y = 0) },
                )
            }
        }
    }
}

/** Displays the necessary [TextFields][SmallBasicTextField] to edit the data of an [Item].
 *
 * @param boxLabel The text that will be displayed above all the fields (whether the box is for *editing* or *creating* a new [Item]).
 * @param rowShape The *rounder* (edges) [Shape] of a [TextField][SmallBasicTextField] in a row of other fields.
 * @param focusedField The *index* of the field that should start with keyboard focus (or none if `null`).
 *   Fields are laid out in the following order: **`0.`** Name, **`1.`** Unit Price, **`2.`** Amount Value, **`3.`** Amount Label.
 * @param nameState The state object for the "Name" field.
 * @param priceState The state object for the "Unit Price" field.
 * @param amountValueState The state object for the "Amount Value" field.
 * @param amountLabelState The state object for the "Amount Label" field.
 * @param hasLabel The state of the *[Amount] type* toggle button, and whether the "Amount Label" field should be displayed.
 * @param onHasLabelChange Updates the state object of the [hasLabel] state. */
@Composable
private fun EditingItemListBox(
    modifier: Modifier = Modifier,
    boxLabel: String = "Edit item",
    currency: Currency,
    rowShape: CornerBasedShape = ITEM_ROW_SHAPE,
    focusedField: UInt? = null,
    nameState: StringTextFieldState,
    priceState: RealNumberFieldState,
    amountValueState: RealNumberFieldState,
    amountLabelState: StringTextFieldState,
    hasLabel: Boolean,
    onHasLabelChange: (Boolean) -> Unit,
) {
    val columnShapes = ItemColumnsShapes(rowShape)
//    @Composable
//    fun Modifier.fieldFocuser(columnNum: UInt)
//        = this.focusRequester(remember { FocusRequester().apply {
//            if (focusedField == columnNum) {
//                this.requestFocus()
//            }
//        } })

    @Composable
    fun ItemColumnField(
        modifier: Modifier = Modifier,
        columnNum: UInt,
        shape: Shape,
        value: String,
        onValueChange: (String) -> Unit,
        isError: Boolean,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    ) {
        ItemColumn(
            modifier = modifier,
            shape = shape,
            padding = PaddingValues(
                start = ITEM_LIST_ROW_PADDING.calculateStartPadding(LocalLayoutDirection.current) * 2,
                end = ITEM_LIST_ROW_PADDING.calculateEndPadding(LocalLayoutDirection.current) * 2,
                top = ITEM_LIST_ROW_PADDING.calculateTopPadding(),
                bottom = ITEM_LIST_ROW_PADDING.calculateBottomPadding(),
            ),
            isSelected = true,
        ) {
            SmallBasicTextField(
                modifier = Modifier //.fieldFocuser(columnNum) // FIXME: doesnt work
                    .fillMaxWidth(),
                value = value,
                onValueChange = onValueChange,
                isError = isError,
                keyboardOptions = keyboardOptions,
            )
        }
    }

    @Composable
    fun Label(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.labelMedium) {
        Text(text, modifier, softWrap = false, maxLines = 1, style = style)
    }

    Column(modifier
        .padding(vertical = COLUMN_SPACING)
        .border(2.dp, MaterialTheme.colorScheme.outline, rowShape)
        .padding(ITEM_LIST_ROW_PADDING),
        verticalArrangement = Arrangement.spacedBy(COLUMN_SPACING * 2),
    ) {
        // TODO: overlay box label on top of border
        Label(boxLabel,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = COLUMN_SPACING * 4)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING * 2)) {
            Column(Modifier.weight(NAME_COLUMN_WEIGHT)) {
                Label("Name")
                ItemColumnField(
                    columnNum = 0u,
                    shape = columnShapes.Start,
                    value = nameState.fieldText,
                    onValueChange = { nameState.fieldText = it },
                    isError = nameState.isError,
                )
            }
            Column(Modifier.weight(PRICE_COLUMN_WEIGHT)) {
                Label("Price (${currency.symbol})")
                ItemColumnField(
                    columnNum = 1u,
                    shape = columnShapes.End,
                    value = priceState.fieldText,
                    onValueChange = { priceState.fieldText = it },
                    isError = priceState.isError,
                    keyboardOptions = RealNumberFieldState.keyboardOptions,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val amountType = if (hasLabel) { "Measurement" } else { "Units" }
            val displayLabelTransition = updateTransition(hasLabel)

            Column {
                Label("Amount")
                PlainToolTipBox("Change amount type") {
                    TextButton(
                        modifier = Modifier.semantics { stateDescription = amountType },
                        onClick = { onHasLabelChange(!hasLabel) },
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp),
                    ) {
                        Icon(painterResource(R.drawable.arrow_drop_down_24px), null,
                            modifier = Modifier.size(20.dp),
                        )
                        Icon(painterResource(if (hasLabel) {
                            R.drawable.scale_24px
                        } else {
                            R.drawable.units_24px
                        }), null)
                    }
                }
            }
            Spacer(Modifier.width(COLUMN_SPACING * 2))

            Column(Modifier.weight(displayLabelTransition.animateFloat { if (it) { 0.5f } else { 1.0f } }.value)) {
                displayLabelTransition.AnimatedContent { hasLabel ->
                    if (hasLabel) { Label(amountType) } else { Label(amountType) }
                }
                ItemColumnField(
                    columnNum = 2u,
                    shape = if (hasLabel) { columnShapes.Start } else { rowShape },
                    value = amountValueState.fieldText,
                    onValueChange = { amountValueState.fieldText = it },
                    isError = amountValueState.isError,
                    keyboardOptions = RealNumberFieldState.keyboardOptions,
                )
            }
            Spacer(Modifier.width(displayLabelTransition.animateDp { if (it) { COLUMN_SPACING * 2 } else { 0.dp } }.value))

            displayLabelTransition.AnimatedVisibility({ it },
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
                modifier = Modifier.weight(displayLabelTransition.animateFloat { if (it) { 0.5f } else { 0.001f } }.value),
            ) {
                Column {
                    Label("Label")
                    ItemColumnField(
                        columnNum = 3u,
                        shape = columnShapes.End,
                        value = amountLabelState.fieldText,
                        onValueChange = { amountLabelState.fieldText = it },
                        isError = amountLabelState.isError,
                        keyboardOptions = RealNumberFieldState.keyboardOptions,
                    )
                }
            }
        }
    }
}

@Suppress("PropertyName")
private class ItemColumnsShapes(
    private val rowShape: CornerBasedShape,
) {
    val Middle@Composable inline get() = ItemColumnsShapes.Middle
    val Start @Composable get() = halfRoundedCornerShape(
        Corner.Right,
        sharpSize = ItemColumnsShapes.Middle.bottomEnd,
        roundSize = rowShape.bottomStart,
    )
    val End @Composable get() = halfRoundedCornerShape(
        Corner.Left,
        sharpSize = ItemColumnsShapes.Middle.bottomStart,
        roundSize = rowShape.bottomEnd,
    )

    companion object {
        val Middle@Composable get() = MaterialTheme.shapes.extraSmall
    }
}

@Composable
private fun ItemColumn(
    modifier: Modifier = Modifier,
    shape: Shape = ItemColumnsShapes.Middle,
    padding: PaddingValues = ITEM_LIST_ROW_PADDING,
    isSelected: Boolean,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) { Box(
    modifier = modifier
        .clip(shape)
        .run { if (onLongClick != null) { this
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick,
            )
        } else this }
        .background(if (isSelected) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            ListItemDefaults.containerColor
        })
        .then(modifier)
        .padding(padding),
    content = content,
) }

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
private fun TextAutoSized(
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
