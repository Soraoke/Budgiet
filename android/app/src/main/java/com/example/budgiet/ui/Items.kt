@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.budgiet.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
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

// TODO: docs
@Composable
fun RowScope.ItemsField(
    viewModel: ItemsViewModel,
    onClickAdd: () -> Unit,
    onClickOcr: () -> Unit,
) {
    if (viewModel.items.isNotEmpty()) {
        Text(viewModel.displayFieldSummary(),
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
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

// TODO: doc all
sealed class ItemsDialogState {
    object View: ItemsDialogState()
    object New: ItemsDialogState()
    class Edit(val value: Item): ItemsDialogState()
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

// TODO: doc
@Composable
private fun ItemsViewDialog(
    modifier: Modifier = Modifier,
    viewModel: ItemsViewModel,
    state: ItemsDialogState,
    onStateChange: (ItemsDialogState) -> Unit,
    onDismiss: () -> Unit,
) {
    // TODO: use tempItems?
    // TODO: review tooltips/contentDescriptions because this no longer uses temp items
//    val tempItems = remember { mutableStateMapOf<String, Item>().apply {
//        putAll(viewModel.items)
//    } }
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
    var editItemNameError by remember(state) { mutableStateOf<Result<Unit>>(Result.Ok(Unit)) }
    var editItemPriceResult by remember(state) { mutableStateOf<Result<Double>>(Result.Ok(0.0)) }
    var editItemAmountResult by remember(state) { mutableStateOf<Result<Double>>(Result.Ok(0.0)) }

    val columnSpacing = 2.dp
    val nameColumnWeight = 0.6f
    val priceColumnWeight = 0.3f
    val amountColumnWeight = 0.3f
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        padding = ActionDialogPadding.TightlyPacked,
        title = { Text("Transaction items",
            modifier = Modifier.fillMaxWidth()
                .padding(start = ActionDialogPadding.Default.dialogEdges.calculateStartPadding(layoutDirection)),
            style = MaterialTheme.typography.headlineSmall,
        ) },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
                        PlainToolTipBox("Apply items") {
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
                LocalTextStyle provides MaterialTheme.typography.labelMedium,
            ) {
                Text("Name", Modifier.weight(nameColumnWeight))
                Text("Price", Modifier.weight(priceColumnWeight))
                Text("Amount", Modifier.weight(amountColumnWeight))
            } }

            ListColumn() {
                this.items(
                    items = viewModel.items.values.toList(),
                    key = { it.name },
                ) { item ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        var multiplySignXOffset by remember(density, LocalTextStyle) { mutableStateOf<Dp?>(null) }
                        var showMenu by remember { mutableStateOf(false) }

                        val sharpColumnShape = MaterialTheme.shapes.extraSmall
                        val roundColumnShape = MaterialTheme.shapes.medium

                        Row(
                            modifier = Modifier.applyHeightToList(),
                            horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                        ) {
                            @Composable
                            fun columnModifier(side: Int)
                                = Modifier.weight(when (side) {
                                       -1 -> nameColumnWeight
                                        0 -> priceColumnWeight
                                        1 -> amountColumnWeight
                                        else -> throw Exception("Unreachable")
                                    })
                                    .clip(run {
                                        val startShape = halfRoundedCornerShape(Corner.Right,
                                            sharpSize = sharpColumnShape.bottomEnd,
                                            roundSize = roundColumnShape.bottomStart,
                                        )
                                        val endShape = halfRoundedCornerShape(Corner.Left,
                                            sharpSize = sharpColumnShape.bottomStart,
                                            roundSize = roundColumnShape.bottomEnd,
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
                                            // TODO: detect which column was long-pressed and make the cursor focus on that column first.
                                            showMenu = true
                                        },
                                    )
                                    .background(ListItemDefaults.containerColor)
                                    .padding(vertical = 16.dp, horizontal = 12.dp)


                            Box(columnModifier(-1)) {
                                // TODO: make into editable fields
                                Text(item.name)
                            }
                            Box(columnModifier(0)) {
                                Text(item.unitPrice.toString())
                            }
                            Box(columnModifier(1)
                                .onGloballyPositioned { coords -> with(density) {
                                    if (multiplySignXOffset == null) {
                                        multiplySignXOffset = coords.size.width.toDp() * -1
                                    }
                                } }
                            ) {
                                Text(item.amount.toString())
                            }
                        }

                        // Multiply icon.
                        Icon(painterResource(R.drawable.close_24px), contentDescription = null,
                            modifier = Modifier.offset(x = multiplySignXOffset?.minus(12.dp) ?: 0.dp, y = 0.dp),
                        )

                        ItemActionsMenu(
                            expanded = showMenu,
                            onDismiss = { showMenu = false },
                            onEditClick = { onStateChange(ItemsDialogState.Edit(item)) },
                            onDeleteClick = { viewModel.items.remove(item.name) },
                        )
                    }
                }
            }
        }

        var rowWidth by remember { mutableStateOf<Dp?>(null) }
        var newButtonWidth by remember { mutableStateOf<Dp?>(null) }

        newItemCollapseTransition.AnimatedVisibility({ state is ItemsDialogState.New }) {
            HorizontalDivider(Modifier.padding(top = ActionDialogPadding.TightlyPacked.actionsSpacerHeight))
        }

        // New Item button ...
        Row(modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> with(density) {
                rowWidth = coords.size.width.toDp()
            } },
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            newItemCollapseTransition.AnimatedContent(
                modifier = Modifier.padding(start = rowWidth?.let { rowWidth -> newButtonWidth?.let { newButtonWidth ->
                    newItemCollapseTransition.animateDp() { state ->
                        if (state is ItemsDialogState.New) {
                            0.dp
                        } else {
                            (rowWidth - newButtonWidth) / 2
                        }
                    }.value
                } } ?: 0.dp)
            ) { state -> when (state) {
                is ItemsDialogState.View -> {
                    FilledTextIconButton(
                        modifier = Modifier.onGloballyPositioned { coords -> with(density) {
                            newButtonWidth = coords.size.width.toDp()
                        } },
                        icon = { Icon(painterResource(R.drawable.add_24px), null) },
                        text = { Text("New item") },
                        onClick = { onStateChange(ItemsDialogState.New) },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                    )
                }
                // ... Becomes a "Cancel" button when editing.
                is ItemsDialogState.New -> {
                    PlainToolTipBox("Discard new item") {
                        FilledIconButton(onClick = { onStateChange(ItemsDialogState.View) }) {
                            Icon(painterResource(R.drawable.close_24px), null)
                        }
                    }
                }
                is ItemsDialogState.Edit -> { }
                is ItemsDialogState.Ocr -> throw Exception("Unreachable")
            } }

            // New item value fields.
            newItemCollapseTransition.AnimatedVisibility(visible = { state is ItemsDialogState.New }) { Row {
                @Composable
                fun NewItemField(
                    modifier: Modifier = Modifier,
                    label: String,
                    value: String,
                    onValueChange: (String) -> Unit,
                ) {
                    OutlinedTextField(
                        modifier = modifier,
                        label = { Text(label, autoSize = TextAutoSize.StepBased(maxFontSize = LocalTextStyle.current.fontSize)) },
                        value = value,
                        onValueChange = onValueChange,
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        maxLines = 1,
                    )
                }

                NewItemField(
                    modifier = Modifier.weight(nameColumnWeight),
                    label = "Name",
                    value = editingItemName,
                    onValueChange = {
                        editingItemName = it
                        editItemNameError = viewModel.validateName(it, isNew = true)
                    },
                )
                NewItemField(
                    modifier = Modifier.weight(priceColumnWeight),
                    label = "Price",
                    value = editingItemPrice,
                    onValueChange = {
                        editingItemPrice = it
                        editItemPriceResult = viewModel.parsePrice(it)
                    },
                )
                NewItemField(
                    modifier = Modifier.weight(amountColumnWeight),
                    label = "Amount",
                    value = editingItemAmount,
                    onValueChange = {
                        editingItemAmount = it
                        editItemAmountResult = viewModel.parseAmount(it)
                    },
                )
            } }
        }

        // New/Edit item errors.
        when (state) {
            is ItemsDialogState.View -> { }
            is ItemsDialogState.New,
            is ItemsDialogState.Edit -> {
                fun errorMsg(error: Throwable): String
                        = error.message ?: error.javaClass.name

                if (editItemNameError is Result.Err) {
                    Text(errorMsg(editItemNameError.unwrapErr()), color = MaterialTheme.colorScheme.error)
                }
                if (editItemPriceResult is Result.Err) {
                    Text(errorMsg(editItemPriceResult.unwrapErr()), color = MaterialTheme.colorScheme.error)
                }
                if (editItemAmountResult is Result.Err) {
                    Text(errorMsg(editItemAmountResult.unwrapErr()), color = MaterialTheme.colorScheme.error)
                }
            }
            is ItemsDialogState.Ocr -> throw Exception("Unreachable")
        }
    }
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
