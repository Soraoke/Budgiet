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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
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
import com.example.budgiet.ui.utils.onMeasureCoords
import com.example.budgiet.validateFieldInput
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.nanoseconds

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
    val totalPrice get() = when (this.amount) {
        is Amount.Measured -> this.unitPrice * this.amount.value
        is Amount.Units -> this.unitPrice * this.amount.value.toInt()
    }
}

sealed class Amount {
    class Measured(val value: Double, val label: String): Amount()
    class Units(val value: UInt): Amount()

    enum class Type {
        Measured, Units;

        override fun toString() = when (this) {
            Measured -> "Measured"
            Units -> "Units"
        }
    }

    val textValue get() = when (this) {
        is Measured -> NumberFormat.getNumberInstance()
            .apply {
                minimumFractionDigits = 1
                maximumFractionDigits = 3
            }
            .format(this.value)
            .trim()
        is Units -> this.value.toString()
    }
    val type get() = when (this) {
        is Measured -> Type.Measured
        is Units -> Type.Units
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
        fun parseValue(type: Type, value: String): Result<Double> {
            return runCatching { when (type) {
                // TODO: Use different parsers to emit better error messages
                Type.Measured -> value.toDouble()
                Type.Units -> value.toInt().toDouble()
            } }.into()
        }
    }
}

sealed class Tax {
    // TODO: use Money type later for this
    class CurrencyAmount(val price: Double): Tax()
    class Percentage(val v: Double): Tax()

    enum class Type {
        CurrencyAmount, Percentage;

        override fun toString() = when (this) {
            CurrencyAmount -> "Currency Amount"
            Percentage -> "Percentage"
        }
    }

    val value get() = when (this) {
        is CurrencyAmount -> this.price
        is Percentage -> this.v
    }
    val type get() = when (this) {
        is CurrencyAmount -> Type.CurrencyAmount
        is Percentage -> Type.Percentage
    }

    companion object {
        fun new(type: Type, value: Double): Tax {
            return when (type) {
                Type.CurrencyAmount -> CurrencyAmount(value)
                Type.Percentage -> Percentage(value)
            }
        }

        fun parse(type: Type, value: String, currency: Currency, locale: Locale): Result<Tax> {
            return when (type) {
                Type.CurrencyAmount -> currency.validateFieldInput(value, locale).map { CurrencyAmount(it) }
                Type.Percentage -> if (value.isEmpty()) {
                    Result.Ok(Percentage(0.0))
                } else {
                    RealNumberFieldState.defaultParser(value).map { Percentage(it) }
                }
            }
        }
    }
}

class ItemsViewModel: ViewModel() {
    // TODO: Have a database table of item names the user has used, and have a different screen to show aggregate data of each item across transactions.
    val items = mutableStateListOf<Item>()
    var tax by mutableStateOf<Tax>(Tax.CurrencyAmount(0.0))

    val totalPrice: Double get() = run {
        val itemsSum = this.items.sumOf { it.totalPrice }
        val taxAmount = when (val tax = this.tax) {
            is Tax.CurrencyAmount -> tax.price
            is Tax.Percentage -> itemsSum * tax.v * 0.01
        }
        itemsSum + taxAmount
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
        val tax = this.tax
        val displayTax = if (tax.value != 0.0 && this.items.isNotEmpty()) {
            when (tax) {
                is Tax.CurrencyAmount -> " + ${currency.symbol}${currency.formatPrice(tax.price, locale)} tax"
                is Tax.Percentage -> " + ${tax.v}% tax"
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
        this.tax = Tax.CurrencyAmount(0.0)
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
    val coroutineScope = rememberCoroutineScope()
    val newItemCollapseTransition = updateTransition(state, "NewItemCollapseButton")
    val autoValidateTimings = remember { AutoValidateTimings(coroutineScope) }

    // Don't use the currency or locale as keys here; the field value should only be reset when state changes.
    val editItemState = remember(state) {
        val preData = when (state) {
            is ItemsDialogState.Edit -> state.value
            else -> null
        }
        object {
            val name = StringTextFieldState(preData?.name ?: "",
                validator = { viewModel.validateName(it, isNew = state !is ItemsDialogState.Edit) },
            )
            val unitPrice = RealNumberFieldState.moneyFieldState(preData?.unitPrice ?: 0.0, emptyInitialTextIfZero = true, currency, locale, autoValidateTimings)
            val amount = object {
                var type by mutableStateOf(preData?.amount?.type ?: Amount.Type.Units)
                val value = run {
                    val parser = { s: String -> Amount.parseValue(type, s) }
                    preData?.let {
                        RealNumberFieldState(it.amount.textValue, parser = parser)
                    } ?: RealNumberFieldState("", parser = parser)
                }
                val label = StringTextFieldState(
                    initialValue = when (val amount = preData?.amount) {
                        is Amount.Measured -> amount.label
                        is Amount.Units, null -> ""
                    },
                    validator = { Amount.validateLabel(it) },
                )
            }
        }
    }
    val taxAmountState = remember { RealNumberFieldState(
        initialValue = viewModel.tax.value,
        emptyInitialTextIfZero = true,
        parser = { text -> Tax.parse(viewModel.tax.type, text, currency, locale).map { it.value } },
        formatter = { v -> when (viewModel.tax.type) {
            Tax.Type.CurrencyAmount -> currency.formatPrice(v, locale)
            Tax.Type.Percentage -> v.toString()
        } },
        autoValidateTimings,
    ).apply { skipEmptyTextFormat = true } }

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
                        && !(editItemState.amount.type == Amount.Type.Measured
                            && editItemState.amount.label.isError
                        )

                    when (state) {
                        is ItemsDialogState.View -> { !taxAmountState.isError }
                        is ItemsDialogState.New -> containsNoErrors
                        is ItemsDialogState.Edit -> run {
                            val item = state.value
                            // Allow submitting only if any field was modified.
                            editItemState.name.fieldText != item.name
                            || editItemState.unitPrice.fieldText != currency.formatPrice(item.unitPrice, locale)
                            || editItemState.amount.value.fieldText != item.amount.textValue
                            || editItemState.amount.type != item.amount.type
                            || if (item.amount is Amount.Measured) {
                                editItemState.amount.label.fieldText != item.amount.label
                            } else { false }
                        } && containsNoErrors
                        else -> { true }
                    }
                }
                val submit = {
                    editItemState.name.doValidate()
                    editItemState.unitPrice.doValidate()
                    editItemState.amount.value.doValidate()
                    if (editItemState.amount.type == Amount.Type.Measured) {
                        editItemState.amount.label.doValidate()
                    }

                    if (canSubmit()) {
                        val newData = Item(
                            name = editItemState.name.fieldText,
                            unitPrice = editItemState.unitPrice.parseResult.unwrap(),
                            amount = when (editItemState.amount.type) {
                                Amount.Type.Measured -> Amount.Measured(editItemState.amount.value.parseResult.unwrap(), editItemState.amount.label.fieldText)
                                Amount.Type.Units -> Amount.Units(editItemState.amount.value.parseResult.unwrap().toUInt())
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

            val listState = rememberLazyListState()
            // Index of the row that the Menu is anchored to.
            var menuPosition by remember { mutableStateOf<Int?>(null) }
            var focusedField by remember { mutableStateOf<UInt?>(null) }

            ListColumn(state = listState) {
                this.itemsIndexed(
                    items = viewModel.items,
                    key = { _, item -> item.name },
                ) { idx, item -> Box {
                    val showMenu = menuPosition?.let { it == idx } ?: false
                    val isEditing = state is ItemsDialogState.Edit && state.value.name == item.name

                    AnimatedContent(isEditing) { isEditing ->
                        val animationScope = this
                        val density = LocalDensity.current
                        val fontSize = with(density) { LocalTextStyle.current.fontSize.value.toDp() }

                        if (isEditing) {
                            EditingItemListBox(
                                modifier = Modifier.padding(vertical = COLUMN_SPACING),
                                currency = currency,
                                focusedField = focusedField,
                                nameState = editItemState.name,
                                priceState = editItemState.unitPrice,
                                amountValueState = editItemState.amount.value,
                                amountLabelState = editItemState.amount.label,
                                amountType = editItemState.amount.type,
                                onHasLabelChange = { editItemState.amount.type = it }
                            )
                            LaunchedEffect(Unit) {
                                delay(animationScope.transition.totalDurationNanos.nanoseconds)
                                val rowHeight = fontSize + ITEM_LIST_ROW_PADDING.calculateTopPadding() + ITEM_LIST_ROW_PADDING.calculateBottomPadding()
                                val offset = with(density) { (-rowHeight / 2).roundToPx() }
                                listState.animateScrollToItem(idx, offset)
                            }
                        } else {
                            StaticItemListRow(
                                currency = currency,
                                locale = locale,
                                showTotalColumn = showTotalColumn,
                                isSelected = showMenu,
                                onLongClick = {
                                    menuPosition = idx
                                    focusedField = it
                                },
                                data = item,
                            )
                        }
                    }

                    ItemActionsMenu(
                        expanded = showMenu,
                        onDismiss = { menuPosition = null },
                        onEditClick = { onStateChange(ItemsDialogState.Edit(item)) },
                        onDeleteClick = { viewModel.removeItem(item.name) },
                    )
                } }
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
                                modifier = Modifier.semantics { stateDescription = viewModel.tax.type.toString() },
                                onClick = {
                                    val taxValue = viewModel.tax.value
                                    viewModel.tax = when (viewModel.tax) {
                                        is Tax.CurrencyAmount -> Tax.Percentage(taxValue)
                                        is Tax.Percentage -> Tax.CurrencyAmount(taxValue)
                                    }
                                    taxAmountState.doValidate()
                                },
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp)
                            ) {
                                Icon(painterResource(R.drawable.arrow_drop_down_24px), null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Icon(when (viewModel.tax) {
                                    is Tax.CurrencyAmount -> currencyIcon
                                    is Tax.Percentage -> painterResource(R.drawable.percent_24px)
                                }, null)
                            }
                        }

                        Box(Modifier
                            .clip(ITEM_ROW_SHAPE)
                            .background(ListItemDefaults.containerColor)
                            .padding(itemFieldPadding)
                            .fillMaxWidth()
                        ) {
                            SmallBasicTextField(
                                modifier = Modifier.semantics { contentDescription = "Tax value" },
                                value = taxAmountState.fieldText,
                                onValueChange = { text ->
                                    taxAmountState.fieldText = text
                                    taxAmountState.ifParseOk { viewModel.tax = Tax.new(viewModel.tax.type, it) }
                                },
                                isError = taxAmountState.isError,
                                placeholderText = when (viewModel.tax) {
                                    is Tax.CurrencyAmount -> currency.formatPrice(0.0, locale)
                                    is Tax.Percentage -> "0.0"
                                },
                                keyboardOptions = RealNumberFieldState.keyboardOptions,
                            )
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
                        amountType = editItemState.amount.type,
                        onHasLabelChange = { editItemState.amount.type = it }
                    )
                }
                else -> { }
            }
        }

        // New/Edit item errors.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
            @Composable
            fun ErrorText(prefix: String, result: Result<*>) {
                if (result is Result.Err) {
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                        )) {
                            append("$prefix:")
                        }
                        append(" ${result.error.message ?: result.error.javaClass.name}")
                    })
                }
            }

            newItemCollapseTransition.AnimatedContent { state ->
                when (state) {
                    is ItemsDialogState.View -> {
                        ErrorText("Tax error", taxAmountState.parseResult)
                    }
                    is ItemsDialogState.New,
                    is ItemsDialogState.Edit -> Column {
                        ErrorText("Name error", editItemState.name.parseResult)
                        ErrorText("Price error", editItemState.unitPrice.parseResult)
                        ErrorText("Amount Value error", editItemState.amount.value.parseResult)
                        ErrorText("Amount Label error", editItemState.amount.label.parseResult)
                    }
                    is ItemsDialogState.Ocr -> throw Exception("Unreachable")
                }
            }
        }
    }
}

/** Displays a single row of an [Item]'s data.
 *
 * @param rowShape The *rounder* (edges) [Shape] of the whole row (applied to each column as needed).
 * @param showTotalColumn Whether the column with the "Total" value should be displayed.
 *   This should only be true on wide screens.
 * @param isSelected Whether the row is selected and should have a *highlighted* background color.
 * @param onLongClick The action that is done when a column of the Row has a `LongClick` event.
 *   The argument passed in this callback is the *index* of the column that corresponds to a field in [EditingItemListBox]
 *   (or `null` if the column does not correspond to one of those fields). */
@Composable
private fun StaticItemListRow(
    modifier: Modifier = Modifier,
    currency: Currency,
    locale: Locale,
    rowShape: CornerBasedShape = ITEM_ROW_SHAPE,
    showTotalColumn: Boolean,
    isSelected: Boolean,
    onLongClick: (column: UInt?) -> Unit,
    data: Item,
) {
    val columnShapes = ItemColumnsShapes(rowShape)
    var amountColumnOffset by remember(LocalDensity.current, LocalTextStyle.current) { mutableStateOf<Float?>(null) }
    var totalColumnOffset by remember(LocalDensity.current, LocalTextStyle.current) { mutableStateOf<Float?>(null) }

    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
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
                    .onMeasureCoords(needsMeasure = amountColumnOffset == null) { coords ->
                        amountColumnOffset = coords.positionInParent().x
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
                        .onMeasureCoords(needsMeasure = totalColumnOffset == null) { coords ->
                            totalColumnOffset = coords.positionInParent().x
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
            .offset { IntOffset(x = amountColumnOffset?.roundToInt()?.let {
                it - (iconSize + COLUMN_SPACING).roundToPx() / 2
            } ?: 0, y = 0) },
        )
        // Equal icon.
        if (showTotalColumn) {
            Icon(painterResource(R.drawable.equal_24px), contentDescription = null, modifier = Modifier
                .align(Alignment.CenterStart)
                .size(iconSize)
                .offset { IntOffset(x = totalColumnOffset?.roundToInt()?.let {
                    it - (iconSize + COLUMN_SPACING).roundToPx() / 2
                } ?: 0, y = 0) },
            )
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
 * @param amountType The state of the *[Amount] type* toggle button, and whether the "Amount Label" field should be displayed.
 * @param onHasLabelChange Updates the state object of the [amountType] state. */
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
    amountType: Amount.Type,
    onHasLabelChange: (Amount.Type) -> Unit,
) {
    val columnShapes = ItemColumnsShapes(rowShape)

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
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(focusedField) {
            if (focusedField == columnNum) {
                focusRequester.requestFocus()
            }
        }

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
                modifier = Modifier.focusRequester(focusRequester)
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
        Text(text,
            // The label should not be detected by semantics/screen reader to avoid duplicates
            // since each editable TextField has its own Content Description.
            modifier.clearAndSetSemantics { },
            softWrap = false, maxLines = 1, style = style,
        )
    }

    LabeledBorderBox(
        modifier = modifier,
        borderWidth = 2.dp,
        borderShape = rowShape,
        label = { Label(boxLabel, style = MaterialTheme.typography.labelSmall) },
    ) {
        Column(Modifier.padding(ITEM_LIST_ROW_PADDING),
            verticalArrangement = Arrangement.spacedBy(COLUMN_SPACING * 2),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING * 2)) {
                Column(Modifier.weight(NAME_COLUMN_WEIGHT)) {
                    Label("Name")
                    ItemColumnField(
                        modifier = Modifier.semantics { contentDescription = "Name" },
                        columnNum = 0u,
                        shape = columnShapes.Start,
                        value = nameState.fieldText,
                        onValueChange = { nameState.fieldText = it },
                        isError = nameState.isError,
                    )
                }
                Column(Modifier.weight(PRICE_COLUMN_WEIGHT)) {
                    val currencySymbol = currency.symbol
                    Label("Price ($currencySymbol)")
                    ItemColumnField(
                        modifier = Modifier.semantics { contentDescription = "Price in $currencySymbol" },
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
                val displayLabelTransition = updateTransition(amountType)

                Column {
                    Label("Amount")
                    PlainToolTipBox("Change amount type") {
                        TextButton(
                            modifier = Modifier.semantics { stateDescription = amountType.toString() },
                            onClick = { onHasLabelChange(when (amountType) {
                                Amount.Type.Measured -> Amount.Type.Units
                                Amount.Type.Units -> Amount.Type.Measured
                            }) },
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp),
                        ) {
                            Icon(painterResource(R.drawable.arrow_drop_down_24px), null,
                                modifier = Modifier.size(20.dp),
                            )
                            Icon(painterResource(when (amountType) {
                                Amount.Type.Measured -> R.drawable.scale_24px
                                Amount.Type.Units -> R.drawable.units_24px
                            }), null)
                        }
                    }
                }
                Spacer(Modifier.width(COLUMN_SPACING * 2))

                /** Returns either argument value depending on the [Amount.Type]. */
                fun <T> Amount.Type.typeState(labelExpanded: T, labelCollapsed: T): T {
                    return when (this) {
                        Amount.Type.Measured -> labelExpanded
                        Amount.Type.Units -> labelCollapsed
                    }
                }

                Column(Modifier
                    .weight(displayLabelTransition.animateFloat { it.typeState(0.5f, 1.0f) }.value)
                ) {
                    displayLabelTransition.AnimatedContent { type -> when (type) {
                        Amount.Type.Measured -> Label(amountType.toString())
                        Amount.Type.Units -> Label(amountType.toString())
                    } }
                    ItemColumnField(
                        Modifier.semantics { contentDescription = "Amount value" },
                        columnNum = 2u,
                        shape = amountType.typeState(columnShapes.Start, rowShape),
                        value = amountValueState.fieldText,
                        onValueChange = { amountValueState.fieldText = it },
                        isError = amountValueState.isError,
                        keyboardOptions = RealNumberFieldState.keyboardOptions,
                    )
                }
                Spacer(Modifier.width(displayLabelTransition.animateDp { it.typeState(COLUMN_SPACING * 2, 0.dp) }.value))

                displayLabelTransition.AnimatedVisibility({ it == Amount.Type.Measured },
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                    modifier = Modifier.weight(displayLabelTransition.animateFloat { it.typeState(0.5f, 0.001f) }.value),
                ) {
                    Column {
                        Label("Label")
                        ItemColumnField(
                            Modifier.semantics { contentDescription = "Amount Measurement label" },
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
private fun LabeledBorderBox(
    modifier: Modifier = Modifier,
    labelXOffset: Dp = 15.dp,
    labelMaskPadding: Dp = 4.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderShape: Shape = MaterialTheme.shapes.medium,
    label: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var labelSize by remember { mutableStateOf(DpSize(0.dp, 0.dp)) }
    var contentSize by remember { mutableStateOf(DpSize(0.dp, 0.dp)) }

    Box(Modifier
        .padding(top = max(0.dp, (labelSize.height / 2f) - 2.dp))
        .then(modifier)
    ) {
        Box(Modifier
            .width(contentSize.width)
            .height(contentSize.height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                val stroke = borderWidth.toPx()
                val halfStroke = stroke / 2f

                this.withTransform({ translate(left = halfStroke, top = halfStroke) }) {
                    val outlineSize = Size(this.size.width - stroke, this.size.height - stroke)
                    this.drawOutline(
                        outline = borderShape.createOutline(outlineSize, this.layoutDirection, this),
                        color = borderColor,
                        style = Stroke(
                            width = borderWidth.toPx(),
                        )
                    )
                }
                this.drawRect(
                    topLeft = Offset(x = labelXOffset.toPx(), y = (-labelSize.height / 2f).toPx()),
                    size = Size((labelSize.width + labelMaskPadding * 2f).toPx(), labelSize.height.toPx()),
                    color = Color.Transparent,
                    blendMode = BlendMode.SrcOut,
                )
            }
        )
        Layout(listOf(label, content)) { (labelMeasurables, childMeasurables), constraints ->
            var labelMaxWidth = 0
            var labelMaxHeight = 0
            var childrenMaxWidth = 0
            var childrenMaxHeight = 0

            val labelPlaceables = labelMeasurables.map {
                it.measure(constraints).also { placeable ->
                    if (placeable.width > labelMaxWidth) {
                        labelMaxWidth = placeable.width
                    }
                    if (placeable.height > labelMaxHeight) {
                        labelMaxHeight = placeable.height
                    }
                }
            }

            val placeables = childMeasurables.map {
                it.measure(constraints).also { placeable ->
                    if (placeable.width > childrenMaxWidth) {
                        childrenMaxWidth = placeable.width
                    }
                    if (placeable.height > childrenMaxHeight) {
                        childrenMaxHeight = placeable.height
                    }
                }
            }

            labelSize = DpSize(labelMaxWidth.toDp(), labelMaxHeight.toDp())
            contentSize = DpSize(childrenMaxWidth.toDp(), childrenMaxHeight.toDp())

            layout(childrenMaxWidth, childrenMaxHeight) {
                placeables.forEach { it.placeRelative(0, 0) }
                labelPlaceables.forEach {
                    it.placeRelative(
                        x = (labelXOffset + labelMaskPadding).roundToPx(),
                        y = -labelMaxHeight / 2,
                    )
                }
            }
        }
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
                tax = Tax.CurrencyAmount(4.08)
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
