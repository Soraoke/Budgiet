package com.example.budgiet.transactionTests

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextRange
import com.example.budgiet.assert
import com.example.budgiet.assertEquals
import com.example.budgiet.formatPrice
import com.example.budgiet.getSemanticsProperty
import com.example.budgiet.ui.FAKE_ITEMS
import com.example.budgiet.ui.FIELD_TIMEOUT
import com.example.budgiet.ui.NewTransactionViewModel
import com.example.budgiet.ui.PriceField
import org.junit.Rule
import org.junit.Test
import java.util.Currency
import java.util.Locale

private const val PRICE_FIELD_TEST_TAG = "priceInputField"

class PriceFieldTests {
    private class TestState(
        private val rule: ComposeContentTestRule,
        initialPrice: Double = 0.0,
        initialCurrency: Currency = Currency.getInstance("USD"),
    ) {
        val textField
            get() = this.rule.onNodeWithTag(PRICE_FIELD_TEST_TAG)

        val errorMessage: String?
            get() = this.textField
                .getSemanticsProperty(SemanticsProperties.Text)
                .map { list -> list.last().text }
                .getOrNull()

        fun inputPrice(price: String) {
            this.textField
                .performTextInput(price)

            // Wait for the check delay. Add arbitrary timeout padding just in case.
            runCatching { this.rule.waitUntil(FIELD_TIMEOUT + 10) { false } }
        }

        val viewModel = NewTransactionViewModel().apply {
            this.customPrice = initialPrice
            this.currency = initialCurrency
        }
        val locale: Locale = Locale.US

        init {
            this.rule.setContent {
                PriceField(
                    modifier = Modifier.testTag(PRICE_FIELD_TEST_TAG),
                    viewModel = this.viewModel,
                    locale = locale,
                )
            }
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsPlaceHolderValue() {
        val state = TestState(this.composeTestRule, initialPrice = 0.0)

        // Text field should start empty.
        state.textField
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .map { it.text }
            .getOrThrow()
            .assertEquals("")

        // Placeholder text should be visible when input is empty.
        state.textField
            .getSemanticsProperty(SemanticsProperties.Text)
            .map { list -> list.map { s -> s.text } }
            .getOrThrow()
            .assertEquals(listOf("0.00"))
    }

    @Test
    fun inputPrice() {
        val state = TestState(this.composeTestRule, initialPrice = 0.0)

        state.inputPrice("42")

        // Check that the cents value was appended automatically.
        state.textField
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .map { it.text }
            .getOrThrow()
            .assertEquals("42.00")

        // Check that there is no error
        state.errorMessage.assertEquals(null)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun appendToPrice() {
        val state = TestState(this.composeTestRule, initialPrice = 0.0)

        // Input initial price.
        state.inputPrice("432")

        // Insert text before the decimal point (cursor stays to the left of the decimal).
        state.inputPrice("123")

        // Insert text after the decimal point.
        val editableTextLength = state.textField
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .getOrThrow().text
            .length
        // Remove the automatic decimal place values.
        state.textField
            .apply { performTextInputSelection(TextRange(editableTextLength), relativeToOriginalText = false) }
            .performKeyInput {
                repeat(2) { this.pressKey(Key.Backspace) }
            }
        state.inputPrice("21")

        // Check final field text.
        state.textField
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .getOrThrow().text
            .assertEquals("432,123.21")
    }

    @Test
    fun inputBadPrice() {
        val state = TestState(this.composeTestRule, initialPrice = 0.0)

        state.inputPrice("bad")

        state.textField
            .apply { this
                .getSemanticsProperty(SemanticsProperties.Error)
                .getOrThrow()
                .assertEquals("Invalid input")
            }.apply { this
                .getSemanticsProperty(SemanticsProperties.Text)
                .getOrThrow()
                .joinToString { it }
                .assert({ it.contains("Invalid character") }) { "Price field did not have an error message" }
            }
    }

    /** Test that the price field is not editable when the transaction form has items. */
    @Test
    fun disabledWithItems() {
        val state = TestState(this.composeTestRule, initialPrice = 0.0)
        val customPrice = "12.00"

        state.inputPrice(customPrice)

        // Add items, price field will display the total price of those items.
        state.viewModel.items.items.addAll(FAKE_ITEMS)
        val totalPrice = state.viewModel.currency.formatPrice(state.viewModel.items.totalPrice, state.locale)

        state.textField
            .assertIsNotEnabled()
        state.textField
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .getOrThrow()
            .text
            .assertEquals(totalPrice)

        // Remove items, price field should revert to previous customPrice.
        state.viewModel.items.items.clear()

        state.textField
            .assertIsEnabled()
        state.textField
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .getOrThrow()
            .text
            .assertEquals(customPrice)
    }
}
