@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.budgiet.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgiet.R
import com.example.budgiet.Result
import com.example.budgiet.into
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.ActionDialog
import com.example.budgiet.ui.utils.ActionDialogPadding
import com.example.budgiet.ui.utils.Corner
import com.example.budgiet.ui.utils.FilledTextIconButton
import com.example.budgiet.ui.utils.ItemActionsMenu
import com.example.budgiet.ui.utils.ListColumn
import com.example.budgiet.ui.utils.PlainToolTipBox
import com.example.budgiet.ui.utils.halfRoundedCornerShape

val FAKE_ITEMS = mapOf(
    "Ham" to Item("Ham", 1.0, 5.99),
    "Cheese" to Item("Cheese", 4.0, 2.59),
    "Bread" to Item("Bread", 2.0, 4.19),
    "Crackers" to Item("Crackers", 2.0, 1.89),
    "Chicken" to Item("Chicken", 3.5, 4.99),
)

data class Item(
    val name: String,
    // val classification: ???
    val amount: Double,
    val unitPrice: Double, // TODO: use money struct
    // TODO: val unitType: Pounds, liters, unit, etc.
)

class ItemsViewModel: ViewModel() {
    // FIXME: preserve order
    val items = mutableStateMapOf<String, Item>()
    var additionalTaxAmount by mutableStateOf<Double?>(null)

    fun totalPrice() = this.items.values.sumOf { it.amount * it.unitPrice } + (additionalTaxAmount ?: 0.0)

    // TODO: doc
    fun displayFieldSummary(): String {
        val currencySymbol = "$" // TODO: use currency in money struct, all items must have the same currency, or thats a DB error
        val itemsCount = this.items.values
            .sumOf { it.amount } // FIXME: count items with a measure other than unit as a single item
            .toInt()
        val itemsPrice = this.items.values
            .sumOf { it.amount * it.unitPrice }
        val displayTax = this.additionalTaxAmount?.let { tax -> " + $currencySymbol$tax tax" }

        val itemsWord = if (itemsCount == 1) "item" else "items"

        return "$itemsCount $itemsWord ($currencySymbol$itemsPrice) ${displayTax ?: "" }"
    }

    fun validateName(name: String, isNew: Boolean = true): Result<Unit> {
        val msg = if (name.isEmpty()) {
            "Name must not be empty"
        } else if (isNew && this.items.contains(name)) {
            "An item with this name already exists. Edit the price/amount of that item instead."
        } else {
            null
        }

        return msg?.let { Result.Err(Exception(msg)) }
            ?: Result.Ok(Unit)
    }
    fun parsePrice(value: String): Result<Double> {
        return runCatching { value.toDouble() }.into()
        // TODO: real impl
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
    onClickAdd: () -> Unit,
    onClickOcr: () -> Unit,
) {
    if (viewModel.items.isNotEmpty()) {
        Text(viewModel.displayFieldSummary(),
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            textAlign = TextAlign.End,
            overflow = TextOverflow.Visible,
        )
    }

    PlainToolTipBox("Add or view items") {
        val addIcon = @Composable {
            Icon(painterResource(R.drawable.add_24px), null)
        }
        val shape = halfRoundedCornerShape(Corner.Right)

        // Collapse button if there are items (like tags button).
        if (viewModel.items.isEmpty()) {
            FilledTextIconButton(
                icon = addIcon,
                text = { Text("Add items") },
                shape = shape,
                colors = ButtonDefaults.filledTonalButtonColors(),
                onClick = onClickAdd,
            )
        } else {
            // FIXME: Remove small padding around icon button
            FilledIconButton(
                content = addIcon,
                shape = shape,
                onClick = onClickAdd,
            )
        }
    }

    // TODO: should this be available when there already are items?
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
    state: ItemsDialogState,
    onStateChange: (ItemsDialogState) -> Unit,
    onDismiss: () -> Unit,
) {
    val newItemCollapseTransition = updateTransition(state, "NewItemCollapseButton")

    var editingItemName by remember(state) { mutableStateOf(when (state) {
        is ItemsDialogState.Edit -> state.value.name
        else -> ""
    }) }
    var editingItemPrice by remember(state) { mutableStateOf(when (state) {
        is ItemsDialogState.Edit -> state.value.unitPrice.toString()
        else -> ""
    }) }
    var editingItemAmount by remember(state) { mutableStateOf(when (state) {
        is ItemsDialogState.Edit -> state.value.amount.toString()
        else -> ""
    }) }
    var taxAmount by remember { mutableStateOf(viewModel.additionalTaxAmount?.toString() ?: "") }
    var editItemNameError by remember(state) { mutableStateOf<Result<Unit>>(Result.Ok(Unit)) }
    var editItemPriceResult by remember(state) { mutableStateOf<Result<Double>>(Result.Ok(0.0)) }
    var editItemAmountResult by remember(state) { mutableStateOf<Result<Double>>(Result.Ok(0.0)) }
    var taxAmountResult by remember(state) { mutableStateOf<Result<Double>>(Result.Ok(0.0)) }

    val columnSpacing = 2.dp
    val nameColumnWeight = 0.6f
    val priceColumnWeight = 0.3f
    val amountColumnWeight = 0.3f
    val columnLabelTextStyle = MaterialTheme.typography.labelMedium
    val rowShape = MaterialTheme.shapes.medium
    val dividerPaddingSize = ActionDialogPadding.TightlyPacked.actionsSpacerHeight * 2
    val itemColumnFieldPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        padding = ActionDialogPadding.TightlyPacked,
        title = { Text("Transaction items",
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = ActionDialogPadding.Default.dialogEdges.calculateStartPadding(
                        layoutDirection
                    )
                ),
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
                    else -> { Box { } }
                }
            }

            // The "Submit" button is different depending on whether the user is currently inputting data for a new item.
            //   If it is, the button adds the new item to the list.
            //   Otherwise, the button applies changes made to the item list and closes the dialog.
            newItemCollapseTransition.AnimatedContent { state ->
                val canSubmit = when (state) {
                    is ItemsDialogState.New -> {{
                        editItemNameError is Result.Ok && editItemPriceResult is Result.Ok && editItemAmountResult is Result.Ok
                    }}
                    is ItemsDialogState.Edit -> {{
                        editingItemName.isNotEmpty() && editingItemPrice.isNotEmpty() && editingItemAmount.isEmpty()
                        && editItemNameError is Result.Ok && editItemPriceResult is Result.Ok && editItemAmountResult is Result.Ok
                    }}
                    else -> {{ true }}
                }
                val submit = {
                    editItemNameError = viewModel.validateName(editingItemName, isNew = true)
                    editItemPriceResult = viewModel.parsePrice(editingItemPrice)
                    editItemAmountResult = viewModel.parseAmount(editingItemAmount)

                    if (canSubmit()) {
                        viewModel.items[editingItemName] = Item(editingItemName, editItemPriceResult.unwrap(), editItemAmountResult.unwrap())
                        onStateChange(ItemsDialogState.View)
                    }
                }

                when (state) {
                    is ItemsDialogState.View -> {
                        PlainToolTipBox("Close items dialog") {
                            FilledTextIconButton(
                                icon = { Icon(painterResource(R.drawable.check_24px), null) },
                                text = { Text("Done") },
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
            Row { CompositionLocalProvider(
                LocalTextStyle provides columnLabelTextStyle,
            ) {
                Text("Name", Modifier.weight(nameColumnWeight))
                // TODO: display used currency symbol
                Text("Price ($)", Modifier.weight(priceColumnWeight))
                Text("Amount", Modifier.weight(amountColumnWeight))
            } }

            ListColumn() {
                this.items(
                    items = viewModel.items.values.toList(),
                    key = { it.name },
                ) { item ->
                    Column {
                        var showMenu by remember { mutableStateOf(false) }
                        val isEditing = state is ItemsDialogState.Edit && state.value.name == item.name
                        var focusedField by remember { mutableStateOf<Int?>(null) }

                        val sharpColumnShape = MaterialTheme.shapes.extraSmall

                        @Composable
                        fun RowScope.ItemColumnField(
                            side: Int,
                            modifier: Modifier = Modifier,
                            staticValue: String,
                            editingValue: String,
                            onEditingValueChange: (String) -> Unit,
                            isError: Boolean,
                        ) {
                            val isSelected = showMenu || isEditing
                            val focusRequester = remember { FocusRequester() }

                            LaunchedEffect(side, focusedField, isEditing) {
                                if (isEditing && focusedField == side) {
                                    focusRequester.requestFocus()
                                }
                            }

                            Box(Modifier
                                .weight(
                                    when (side) {
                                        -1 -> nameColumnWeight
                                        0 -> priceColumnWeight
                                        1 -> amountColumnWeight
                                        else -> throw Exception("Unreachable")
                                    }
                                )
                                .clip(run {
                                    val startShape = halfRoundedCornerShape(
                                        Corner.Right,
                                        sharpSize = sharpColumnShape.bottomEnd,
                                        roundSize = rowShape.bottomStart,
                                    )
                                    val endShape = halfRoundedCornerShape(
                                        Corner.Left,
                                        sharpSize = sharpColumnShape.bottomStart,
                                        roundSize = rowShape.bottomEnd,
                                    )

                                    when (side) {
                                        -1 -> startShape
                                        0 -> sharpColumnShape
                                        1 -> endShape
                                        else -> throw Exception("Unreachable")
                                    }
                                })
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = {
                                        showMenu = true
                                        focusedField = side
                                    },
                                )
                                .background(if (isSelected) {
                                    MaterialTheme.colorScheme.surfaceContainerLowest
                                } else {
                                    ListItemDefaults.containerColor
                                })
                                .padding(itemColumnFieldPadding)
                                .then(modifier)
                            ) {
                                if (isEditing) {
                                    SmallBasicTextField(
                                        modifier = Modifier.focusRequester(focusRequester),
                                        value = editingValue,
                                        onValueChange = onEditingValueChange,
                                        isError = isError,
                                    )
                                } else {
                                    Text(staticValue)
                                }
                            }
                        }

                        newItemCollapseTransition.AnimatedVisibility(visible = { isEditing }) {
                            Text("Edit item",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.Start)
                                    .padding(start = columnSpacing * 4)
                            )
                        }

                        Box(contentAlignment = Alignment.CenterEnd) {
                            var amountColumnWidth by remember(density, LocalTextStyle) { mutableStateOf<Dp?>(null) }

                            Row(
                                modifier = Modifier.applyHeightToList(),
                                horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                            ) {
                                ItemColumnField(-1,
                                    staticValue = item.name,
                                    editingValue = editingItemName,
                                    onEditingValueChange = {
                                        editingItemName = it
                                        editItemNameError = viewModel.validateName(it, isNew = false)
                                    },
                                    isError = editItemNameError is Result.Err,
                                )
                                ItemColumnField(0,
                                    staticValue = item.unitPrice.toString(),
                                    editingValue = editingItemPrice,
                                    onEditingValueChange = {
                                        editingItemPrice = it
                                        editItemPriceResult = viewModel.parsePrice(it)
                                    },
                                    isError = editItemPriceResult is Result.Err,
                                )
                                ItemColumnField(1,
                                    modifier = Modifier.onGloballyPositioned { coords -> with(density) {
                                        if (amountColumnWidth == null) {
                                            amountColumnWidth = coords.size.width.toDp() * -1
                                        }
                                    } },
                                    staticValue = item.amount.toString(),
                                    editingValue = editingItemAmount,
                                    onEditingValueChange = {
                                        editingItemAmount = it
                                        editItemAmountResult = viewModel.parseAmount(it)
                                    },
                                    isError = editItemAmountResult is Result.Err,
                                )
                            }

                            // Multiply icon.
                            Icon(painterResource(R.drawable.close_24px), contentDescription = null,
                                modifier = Modifier.offset(x = amountColumnWidth?.minus(12.dp + columnSpacing / 2) ?: 0.dp, y = 0.dp),
                            )

                            ItemActionsMenu(
                                expanded = showMenu,
                                onDismiss = { showMenu = false },
                                onEditClick = {
                                    onStateChange(ItemsDialogState.Edit(item))
                                },
                                onDeleteClick = { viewModel.items.remove(item.name) },
                            )
                        }
                    }
                }
            }
        }

        newItemCollapseTransition.AnimatedVisibility(visible = { state ->
            state is ItemsDialogState.View
        }) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(0.5f)) {
                Text("Tax amount (optional)",
                    style = columnLabelTextStyle,
                    modifier = Modifier.padding(top = dividerPaddingSize, start = 10.dp)
                )

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // TODO: display used currency symbol
                    Icon(painterResource(R.drawable.currency_dollar_24px), null)
                    Box(Modifier
                        .clip(rowShape)
                        .background(ListItemDefaults.containerColor)
                        .padding(itemColumnFieldPadding)
                        .fillMaxWidth()
                    ) {
                        SmallBasicTextField(
                            value = taxAmount,
                            onValueChange = {
                                taxAmount = it
                                TODO("parse")
                            },
                            isError = taxAmountResult is Result.Err,
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
                    // TODO: display used currency symbol
                    Icon(painterResource(R.drawable.currency_dollar_24px), null)
                    Box(Modifier
                        .clip(rowShape)
                        .background(ListItemDefaults.containerColor)
                        .padding(itemColumnFieldPadding)
                        .fillMaxWidth()
                    ) {
                        Text("${viewModel.totalPrice()}")
                    }
                }
            }
        } }

        // New Item button ...
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            newItemCollapseTransition.AnimatedContent() { state ->
                when (state) {
                    is ItemsDialogState.View -> {
                        FilledTextIconButton(
                            modifier = Modifier.padding(top = dividerPaddingSize),
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
                            isError: Boolean,
                        ) {
                            OutlinedTextField(
                                modifier = modifier,
                                label = { Text(label,
                                    autoSize = TextAutoSize.StepBased(maxFontSize = LocalTextStyle.current.fontSize),
                                    overflow = TextOverflow.Visible,
                                    maxLines = 1,
                                    softWrap = false,
                                ) },
                                value = value,
                                onValueChange = onValueChange,
                                shape = MaterialTheme.shapes.medium,
                                isError = isError,
                                singleLine = true,
                                maxLines = 1,
                            )
                        }

                        HorizontalDivider(Modifier.padding(vertical = dividerPaddingSize))
                        Text("New Item", style = columnLabelTextStyle)

                        Row(horizontalArrangement = Arrangement.spacedBy(columnSpacing)) {
                            NewItemField(
                                modifier = Modifier.weight(nameColumnWeight),
                                label = "Name",
                                value = editingItemName,
                                onValueChange = {
                                    editingItemName = it
                                    editItemNameError = viewModel.validateName(it, isNew = true)
                                },
                                isError = editItemNameError is Result.Err,
                            )
                            NewItemField(
                                modifier = Modifier.weight(priceColumnWeight),
                                label = "Price",
                                value = editingItemPrice,
                                onValueChange = {
                                    editingItemPrice = it
                                    editItemPriceResult = viewModel.parsePrice(it)
                                },
                                isError = editItemPriceResult is Result.Err,
                            )
                            NewItemField(
                                modifier = Modifier.weight(amountColumnWeight),
                                label = "Amount",
                                value = editingItemAmount,
                                onValueChange = {
                                    editingItemAmount = it
                                    editItemAmountResult = viewModel.parseAmount(it)
                                },
                                isError = editItemAmountResult is Result.Err,
                            )
                        }
                    }
                    else -> { }
                }
            }
        }

        // New/Edit item errors.
        when (state) {
            is ItemsDialogState.View -> { }
            is ItemsDialogState.New,
            is ItemsDialogState.Edit -> CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error){
                fun errorMsg(error: Throwable): String
                    = error.message ?: error.javaClass.name

                if (editItemNameError is Result.Err) {
                    Text(errorMsg(editItemNameError.unwrapErr()))
                }
                if (editItemPriceResult is Result.Err) {
                    Text(errorMsg(editItemPriceResult.unwrapErr()))
                }
                if (editItemAmountResult is Result.Err) {
                    Text(errorMsg(editItemAmountResult.unwrapErr()))
                }
                if (taxAmountResult is Result.Err) {
                    Text(errorMsg(taxAmountResult.unwrapErr()))
                }
            }
            is ItemsDialogState.Ocr -> throw Exception("Unreachable")
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
    @Suppress("AssignedValueIsNeverRead")
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
        singleLine = true,
        maxLines = 1,
    )
}

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
    BudgietTheme { Row {
        ItemsField(
            viewModel = viewModel<ItemsViewModel>(),
            onClickAdd = { },
            onClickOcr = { },
        )
    } }
}

@Preview(showBackground = true)
@Composable
fun ItemsFieldFilledPreview() {
    BudgietTheme { Row {
        ItemsField(
            viewModel = viewModel<ItemsViewModel>().apply {
                items.putAll(FAKE_ITEMS)
            },
            onClickAdd = { },
            onClickOcr = { },
        )
    } }
}

@Preview(showBackground = true)
@Composable
fun ItemsDialogPreview() {
    BudgietTheme {
        ItemsDialog(
            viewModel = viewModel<ItemsViewModel>().apply {
                items.putAll(FAKE_ITEMS)
            },
            state = ItemsDialogState.View,
            onStateChange = { },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ItemsDialogNewItemPreview() {
    BudgietTheme {
        ItemsDialog(
            viewModel = viewModel<ItemsViewModel>(),
            state = ItemsDialogState.New,
            onStateChange = { },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ItemsDialogEditItemPreview() {
    BudgietTheme {
        ItemsDialog(
            viewModel = viewModel<ItemsViewModel>().apply {
                items[FAKE_ITEMS.values.first().name] = FAKE_ITEMS.values.first()
            },
            state = ItemsDialogState.Edit(FAKE_ITEMS.values.first()),
            onStateChange = { },
            onDismiss = { },
        )
    }
}
