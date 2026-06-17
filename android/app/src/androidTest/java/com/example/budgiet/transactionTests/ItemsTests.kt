package com.example.budgiet.transactionTests

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.budgiet.assert
import com.example.budgiet.assertEquals
import com.example.budgiet.getSemanticsProperty
import com.example.budgiet.onDescendants
import com.example.budgiet.ui.Amount
import com.example.budgiet.ui.FAKE_ITEMS
import com.example.budgiet.ui.FIELD_TIMEOUT
import com.example.budgiet.ui.Item
import com.example.budgiet.ui.ItemsDialog
import com.example.budgiet.ui.ItemsDialogState
import com.example.budgiet.ui.ItemsField
import com.example.budgiet.ui.ItemsViewModel
import com.example.budgiet.ui.Tax
import com.example.budgiet.ui.theme.BudgietTheme
import org.junit.Rule
import org.junit.Test
import java.util.Currency
import java.util.Locale

const val ITEMS_FIELD_TEST_TAG = "itemsField"
const val ITEMS_DIALOG_TEST_TAG = "itemsDialog"
const val NAME_FIELD_DESC = "Name"
const val PRICE_FIELD_DESC = "Price in $"
const val AMOUNT_TYPE_BUTTON_DESC = "Change amount type"
const val AMOUNT_VALUE_FIELD_DESC = "Amount value"
const val AMOUNT_LABEL_FIELD_DESC = "Amount Measurement label"

class ItemsTests {
    private class TestState(
        private val rule: ComposeContentTestRule,
        items: List<Item> = FAKE_ITEMS,
        taxValue: Double = 0.0,
    ) {
        val itemsField get() = run {
            if (this.dialogState != null) {
                this.dialogState = null
            }
            this.rule.onNodeWithTag(ITEMS_FIELD_TEST_TAG)
        }
        val itemsDialog get() = run {
            if (this.dialogState == null) {
                this.dialogState = ItemsDialogState.View
            }
            this.rule.onNodeWithTag(ITEMS_DIALOG_TEST_TAG)
        }
        val itemsListColumn get()
             = this.itemsDialog
                .onDescendants(this.rule)
                .filterToOne(hasScrollAction())

        fun getItemField(row: UInt, col: UInt)
            = this.itemsListColumn
                .onChildAt(row.toInt())
                .onChildAt(col.toInt())

        /** Wait for the [auto-validation][com.example.budgiet.ui.utils.AutoValidateTimings] delays. */
        // Add arbitrary timeout padding just in case.
        fun waitUntilAutoValidation() { runCatching {
            this.rule.waitUntil(FIELD_TIMEOUT.inWholeMilliseconds + 10) { false }
        } }

        private var dialogState by mutableStateOf<ItemsDialogState?>(null)
        val viewModel = ItemsViewModel().apply {
            this.items.addAll(items)
            this.tax = Tax.CurrencyAmount(taxValue)
        }

        init {
            val locale = Locale.US
            val currency = Currency.getInstance("USD")

            this.rule.setContent { BudgietTheme {
                Row(Modifier.testTag(ITEMS_FIELD_TEST_TAG)) {
                    ItemsField(
                        viewModel = viewModel,
                        locale = locale,
                        currency = currency,
                        onClickAdd = { dialogState = ItemsDialogState.View },
                        onClickOcr = { dialogState = ItemsDialogState.Ocr },
                    )
                }

                this.dialogState?.let { state ->
                    ItemsDialog(
                        modifier = Modifier.testTag(ITEMS_DIALOG_TEST_TAG),
                        viewModel = viewModel,
                        locale = locale,
                        currency = currency,
                        state = state,
                        onStateChange = { dialogState = it },
                        onDismiss = { dialogState = null },
                    )
                }
            } }
        }
    }

    @get:Rule
    val rule = createComposeRule()

    /** Test that new items are added to the *end* of the list. */
    @Test
    fun appendItem() {
        val state = TestState(this.rule)
        val newItemName = "Amogus"
        val newItemPrice = "545.00"
        val newItemAmount = Amount.Units(42u)

        // Enter New Item mode.
        state.itemsDialog
            .onDescendants(this.rule)
            .filterToOne(hasTextExactly("New item"))
            .assertHasClickAction()
            .performClick()

        fun itemField(fieldDesc: String) = run {
            state.itemsDialog
                .onDescendants(this.rule)
                .filterToOne(hasContentDescriptionExactly(fieldDesc))
                .onChild()
        }

        // Edit Fields.
        itemField(NAME_FIELD_DESC).performTextInput(newItemName)
        itemField(PRICE_FIELD_DESC).performTextInput(newItemPrice)
        // Check that the Amount Type is Units
        itemField(AMOUNT_TYPE_BUTTON_DESC)
            .getSemanticsProperty(SemanticsProperties.StateDescription)
            .getOrThrow()
            .assertEquals(Amount.Type.Units.toString())
        itemField(AMOUNT_VALUE_FIELD_DESC).performTextInput(newItemAmount.textValue)

        state.itemsDialog
            .onDescendants(this.rule)
            .filterToOne(hasContentDescriptionExactly("Submit new item"))
            .onChild()
            .assertHasClickAction()
            .assertIsEnabled()
            .performClick()

        // Check that the item was added at the end of the list.
        // Also check that the columns are in the correct order.
        state.itemsListColumn
            .performScrollToIndex(state.viewModel.items.size - 1)
            .run { onChildAt(fetchSemanticsNode().children.size - 1) }
            .apply {
                onChildAt(0).assertTextEquals(newItemName)
                onChildAt(1).assertTextEquals(newItemPrice)
                onChildAt(2).assertTextEquals(newItemAmount.textValue)
            }
    }

    @Test
    fun editItem() {
        val state = TestState(this.rule)
        val editRow = 0u

        val newName = "Susus Amogus"
        val newPrice = "545.00"
        val newAmount = Amount.Measured(42.0, "lbs")

        // Enter edit mode.
        state.getItemField(editRow, 0u)
            .performMouseInput { longClick() }
        this.rule.onNode(hasContentDescriptionExactly("Edit"))
            .performClick()

        fun itemField(fieldDesc: String) = run {
            state.itemsListColumn
                .onChildAt(editRow.toInt())
                .onDescendants(this.rule)
                .filterToOne(hasContentDescriptionExactly(fieldDesc))
                .onChild()
        }
        fun editField(fieldDesc: String, newValue: String) {
            itemField(fieldDesc)
                .apply { performTextClearance() }
                .performTextInput(newValue)
        }

        // Edit Fields.
        editField(NAME_FIELD_DESC, newName)
        editField(PRICE_FIELD_DESC, newPrice)
        // Change Amount type
        itemField(AMOUNT_TYPE_BUTTON_DESC)
            .performClick()
            .getSemanticsProperty(SemanticsProperties.StateDescription)
            .getOrThrow()
            .assertEquals(Amount.Type.Measured.toString())
        editField(AMOUNT_VALUE_FIELD_DESC, newAmount.textValue)
        editField(AMOUNT_LABEL_FIELD_DESC, newAmount.label)

        // Save edit.
        state.itemsDialog
            .onDescendants(this.rule)
            .filterToOne(hasContentDescriptionExactly("Save changes"))
            .onChild()
            .assertHasClickAction()
            .assertIsEnabled()
            .performClick()

        // Check that the new values are reflected.
        state.getItemField(editRow, 0u)
            .assertTextEquals(newName)
        state.getItemField(editRow, 1u)
            .assertTextEquals(newPrice)
        state.getItemField(editRow, 2u)
            .assertTextEquals("${newAmount.textValue} ${newAmount.label}")
    }

    @Test
    fun deleteItem() {
        val state = TestState(this.rule)
        var itemName: String

        state.getItemField(0u, 0u)
            .also {
                itemName = it.getSemanticsProperty(SemanticsProperties.Text)
                    .getOrThrow()
                    .joinToString(separator = "") { s -> s.text }
            }
            .performMouseInput { longClick() }
        this.rule.onNode(hasContentDescriptionExactly("Delete"))
            .performClick()

        // Check that item was deleted.
        state.itemsListColumn
            .fetchSemanticsNode()
            .children.forEach { row ->
                row.children[0]
                    .config
                    .getOrNull(SemanticsProperties.Text)!!
                    .joinToString(separator = "") { s -> s.text }
                    .assert({ it != itemName }) { "Found item named \"$itemName\" when it should have been deleted" }
            }
    }

    /** Test that modifying the tax amount also modifies the total amount. */
    @Test
    fun tax() {
        val state = TestState(this.rule)

        val taxTypeButton = state.itemsDialog
            .onDescendants(this.rule)
            .filterToOne(hasContentDescriptionExactly("Switch tax type"))
        val taxField = state.itemsDialog
            .onDescendants(this.rule)
            .filterToOne(hasContentDescriptionExactly("Tax value"))

        fun getTotal(): Double
            = state.itemsDialog
                .onDescendants(this.rule)
                .filterToOne(hasContentDescriptionExactly("Total price amount"))
                .getSemanticsProperty(SemanticsProperties.Text)
                .getOrThrow()
                .joinToString(separator = "") { s -> s.text }
                .toDouble()

        // -- Test tax as percentage of items price. --
        val taxPercentage = "8.875"

        // Switch to using Percentage for tax.
        taxTypeButton
            .apply { performClick() }
            .onChild()
            .getSemanticsProperty(SemanticsProperties.StateDescription)
            .getOrThrow()
            .assertEquals(Tax.Type.Percentage.toString())

        // Input tax value
        taxField
            .apply { performTextInput(taxPercentage) }
            .also { state.waitUntilAutoValidation() }
            .assertTextEquals(taxPercentage)
        // Check total amount.
        getTotal().assertEquals(39.54)

        // -- Test tax as dollar amount. --
        val taxAmount = "42.00"

        // Switch to using CurrencyAmount for tax.
        taxTypeButton
            .apply { performClick() }
            .onChild()
            .getSemanticsProperty(SemanticsProperties.StateDescription)
            .getOrThrow()
            .assertEquals(Tax.Type.CurrencyAmount.toString())

        // Input tax value
        taxField
            .apply { performTextClearance() }
            .apply { performTextInput(taxAmount) }
            .also { state.waitUntilAutoValidation() }
            .assertTextEquals(taxAmount)
        // Check total amount.
        getTotal().assertEquals(78.32)
    }

    @Test
    fun clear() {
        val state = TestState(this.rule)

        // Click 'Clear' in main dialog...
        state.itemsDialog
            .onDescendants(this.rule)
            .filterToOne(hasTextExactly("Clear"))
            .assertHasClickAction()
            .performClick()
        // ... click confirm in alert dialog.
        this.rule.onNode(hasContentDescriptionExactly("confirm"))
            .performClick()

        state.viewModel.items.assert({ it.isEmpty() }) { "Items list should be empty after clicking 'Clear'" }

        state.itemsDialog
            .onDescendants(this.rule)
            .filterToOne(hasText("There are no items."))
            .assertExists()

        // Check cancel/clear button is hidden when items is empty.
        state.itemsDialog
            .onDescendants(this.rule)
            .filterToOne(hasTextExactly("Clear") or hasTextExactly("Cancel"))
            .assertDoesNotExist()
    }

    /** Test what the form field displays when there are items or when it is empty. */
    @Test
    fun fieldDisplay() {
        val state = TestState(this.rule)

        fun TestState.getFormFieldText(): String?
            = this.itemsField
                .onChildAt(0)
                .getSemanticsProperty(SemanticsProperties.Text)
                .getOrNull()
                ?.joinToString(separator = "") { s -> s.text }

        fun TestState.getElement(contentDescription: String)
            = this.itemsField
                .onChildren()
                .filterToOne(hasContentDescriptionExactly(contentDescription))

        // Test field with empty items.
        state.viewModel.items.clear()

        state.getFormFieldText() // It's considered empty if it is null
            ?.assert({ it.isEmpty() }) { "Items form field should have no text when there are no items" }
        state.getElement("Add items")
            .assertExists()
        state.getElement("Scan a receipt")
            .assertExists()
        state.getElement("View items")
            .assertDoesNotExist()

        // Test field with items.
        state.viewModel.items.addAll(FAKE_ITEMS)

        state.getFormFieldText()
            .assertEquals("6 items ($36.32)")
        state.getElement("Add items")
            .assertDoesNotExist()
        state.getElement("Scan a receipt")
            .assertDoesNotExist()
        state.getElement("View items")
            .assertExists()

        // Test field with items and tax.
        state.viewModel.tax = Tax.CurrencyAmount(2.10)

        state.getFormFieldText()
            .assertEquals("6 items ($36.32) + $2.10 tax")
    }

    /** Test the receipt image scanning procedure. */
    @Test
    fun scan() {
        // TODO:
    }
}