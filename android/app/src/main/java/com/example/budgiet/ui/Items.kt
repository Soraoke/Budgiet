@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.budgiet.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import com.example.budgiet.DbEntry
import com.example.budgiet.R
import com.example.budgiet.ui.utils.Corner
import com.example.budgiet.ui.utils.FilledTextIconButton
import com.example.budgiet.ui.utils.PlainToolTipBox
import com.example.budgiet.ui.utils.halfRoundedCornerShape

data class Item(
    val name: String,
    // val classification: ???
    val amount: Double, // Pounds, liters, or units?
    val unitPrice: Double, // TODO: use money struct
    // TODO: how to denote tax item?
)

class ItemsViewModel: ViewModel() {
    val items = mutableStateSetOf<DbEntry<Item>>()
    var taxPrice by mutableStateOf<Double?>(null)
}

// TODO: docs
@Composable
fun RowScope.ItemsField(
    viewModel: ItemsViewModel,
    onClickAdd: () -> Unit,
    onClickOcr: () -> Unit,
) {
    if (viewModel.items.isNotEmpty()) {
        val currencySymbol = "$" // TODO: use currency in money struct, all items must have the same currency, or thats a DB error
        val itemsPrice = viewModel.items
            .map { it.data }
            .sumOf { it.amount * it.unitPrice }

        Text("${viewModel.items.size} items ($currencySymbol$itemsPrice) ${ viewModel.taxPrice?.let { taxPrice ->
            " + $currencySymbol$taxPrice"
        } ?: "" }")
    }

    PlainToolTipBox("Add or view items") {
        val addIcon = @Composable {
            Icon(painterResource(R.drawable.add_24px), null)
        }
        val shape = halfRoundedCornerShape(Corner.Right)

        // Collapse button if there are items (like tags button).
        if (viewModel.items.isNotEmpty()) {
            FilledTextIconButton(
                icon = addIcon,
                text = { Text("Add items") },
                shape = shape,
                onClick = onClickAdd,
            )
        } else {
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
            Icon(painterResource(R.drawable.document_scanner_24px),  null)
        }
    }
}

// TODO: doc all
sealed class ItemsDialogState {
    object Manual: ItemsDialogState()
    object Ocr: ItemsDialogState()
}
@Composable
fun ItemsDialog(
    modifier: Modifier = Modifier,
    state: ItemsDialogState,
    onStateChange: (ItemsDialogState) -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        is ItemsDialogState.Manual -> {
            // TODO
        }
        is ItemsDialogState.Ocr -> {
            // TODO
            //  Should have options to take a picture directly, or use an already existing picture from image picker (note: must not ask for files access)
        }
    }
}
