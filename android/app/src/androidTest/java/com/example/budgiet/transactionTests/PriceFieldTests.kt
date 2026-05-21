package com.example.budgiet.transactionTests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
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
import com.example.budgiet.getSemanticsProperty
import com.example.budgiet.ui.FIELD_TIMEOUT
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
        val priceInputNode
            get() = this.rule.onNodeWithTag(PRICE_FIELD_TEST_TAG)

        val errorMessage: String?
            get() = this.priceInputNode
                .getSemanticsProperty(SemanticsProperties.Text)
                .map { list -> list.last().text }
                .getOrNull()

        fun inputPrice(price: String) {
            this.priceInputNode
                .performTextInput(price)

            // Wait for the check delay. Add arbitrary timeout padding just in case.
            runCatching { this.rule.waitUntil(FIELD_TIMEOUT + 10) { false } }
        }

        init {
            this.rule.setContent {
                var selectedPrice by remember { mutableStateOf(initialPrice) }
                var selectedCurrency by remember { mutableStateOf(initialCurrency) }

                PriceField(
                    modifier = Modifier.testTag(PRICE_FIELD_TEST_TAG),
                    selectedPrice = selectedPrice,
                    onPriceChange = { selectedPrice = it },
                    locale = remember { Locale.US },
                    selectedCurrency = selectedCurrency,
                    onCurrencyChange = { selectedCurrency = it },
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
        state.priceInputNode
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .map { it.text }
            .getOrThrow()
            .assertEquals("")

        // Placeholder text should be visible when input is empty.
        state.priceInputNode
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
        state.priceInputNode
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
        val editableTextLength = state.priceInputNode
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .getOrThrow().text
            .length
        // Remove the automatic decimal place values.
        state.priceInputNode
            .apply { performTextInputSelection(TextRange(editableTextLength), relativeToOriginalText = false) }
            .performKeyInput {
                repeat(2) { this.pressKey(Key.Backspace) }
            }
        state.inputPrice("21")

        // Check final field text.
        state.priceInputNode
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .getOrThrow().text
            .assertEquals("432,123.21")
    }

    @Test
    fun inputBadPrice() {
        val state = TestState(this.composeTestRule, initialPrice = 0.0)

        state.inputPrice("bad")

        state.priceInputNode
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
}
