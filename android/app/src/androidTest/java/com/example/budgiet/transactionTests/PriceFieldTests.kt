package com.example.budgiet.transactionTests

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.budgiet.assertEquals
import com.example.budgiet.getSemanticsProperty
import com.example.budgiet.ui.PriceField
import org.junit.Rule
import org.junit.Test
import java.util.Currency

private const val PRICE_FIELD_TEST_TAG = "priceInputField"

private data class PriceAssertion(
    val text: String?,
    val onFocusAssertion: String?,
    val onUnfocusAssertion: String?,
    val includeEditableText: Boolean
)

class PriceFieldTests {
    private class TestState(
        private val rule: ComposeContentTestRule,
        initialPrice: String = "",
        initialCurrency: Currency = Currency.getInstance("USD"),
    ) {
        val priceInputNode
            get() = this.rule.onNodeWithTag(PRICE_FIELD_TEST_TAG)

        // This node is used to re-direct focus to see if
        // number formatting is done
        val buttonNode
            get() = this.rule.onNodeWithTag("button")

        init {
            this.rule.setContent {
                var selectedPrice by remember { mutableStateOf(initialPrice) }
                var selectedCurrency by remember { mutableStateOf(initialCurrency) }
                val focusManager = LocalFocusManager.current

                Column {
                    PriceField(
                        modifier = Modifier.testTag(PRICE_FIELD_TEST_TAG),
                        selectedPrice = selectedPrice,
                        onPriceChange = { selectedPrice = it },
                        selectedCurrency = selectedCurrency,
                        onCurrencyChange = { selectedCurrency = it },
                    )

                    // TODO: get rid of button
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = { focusManager.clearFocus() },
                        content = {},
                    )
                }
            }
        }

        fun priceTestCase(
            assertions: List<PriceAssertion>,
        ) {

            assertions.forEach { assertion ->
                if (assertion.text != null) {
                    priceInputNode
                        .performTextInput(assertion.text)
                }

                if (assertion.onFocusAssertion != null) {
                    priceInputNode.assertIsFocused()
                    priceInputNode
                        .assertTextEquals(
                            assertion.onFocusAssertion,
                            includeEditableText = assertion.includeEditableText
                        )
                }

                if (assertion.onUnfocusAssertion != null) {
                    buttonNode.performClick()
                    priceInputNode.assertIsNotFocused()
                    priceInputNode.assertTextEquals(
                        assertion.onUnfocusAssertion,
                        includeEditableText = assertion.includeEditableText
                    )
                }

            }

        }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsPlaceHolderValue() {
        val state = TestState(this.composeTestRule, initialPrice = "")

        // Placeholder text should be visible when input is empty.
        state.priceInputNode
            .getSemanticsProperty(SemanticsProperties.Text)
            .map { list -> list.map { s -> s.text } }
            .getOrThrow()
            .assertEquals(listOf("0.00"))
    }

    @Test
    fun enterWholeInteger() {
        val state = TestState(this.composeTestRule)

        state.priceTestCase(listOf(
            PriceAssertion(
                "100",
                "100",
                null,
                true
            ),
            PriceAssertion(
                null,
                null,
                "100.00",
                true
            )
        ))
    }

    @Test
    fun enterDecimal() {
        val state = TestState(this.composeTestRule)

        state.priceTestCase(listOf(
            PriceAssertion(
                "100.00",
                "100.00",
                null,
                true
            ),
            PriceAssertion(
                null,
                null,
                "100.00",
                true
            )
        ))
    }

    @Test
    fun enterInvalidCharacter() {
        val state = TestState(this.composeTestRule)

        state.priceTestCase(listOf(
            PriceAssertion(
                "f",
                "f",
                null,
                true
            ),
            PriceAssertion(
                null,
                null,
                "Invalid character 'f' used",
                false
            )
        ))
    }

    @Test
    fun enterHalfValidHalfInvalidCharacters() {
        val state = TestState(this.composeTestRule)

        state.priceTestCase(listOf(
            PriceAssertion(
                "1",
                "1",
                null,
                true
            ),
            PriceAssertion(
                "f",
                "1f",
                null,
                true
            ),
            PriceAssertion(
                null,
                null,
                "Invalid character 'f' used",
                false
            )
        ))
    }

    @Test
    fun enterLeadingZero() {
        val state = TestState(this.composeTestRule)

        state.priceTestCase(listOf(
            PriceAssertion(
                "01",
                "01",
                null,
                true
            ),
            PriceAssertion(
                null,
                null,
                "Leading un-fractional 0s are not allowed",
                false
            )
        ))
    }

    @Test
    fun enterManyDecimals() {
        val state = TestState(this.composeTestRule)

        state.priceTestCase(listOf(
            PriceAssertion(
                "1..0",
                "1..0",
                null,
                true
            ),
            PriceAssertion(
                null,
                null,
                "Decimal '.' exists already",
                false
            )
        ))
    }

    @Test
    fun enterTooManyDecimalPlaces() {
        val state = TestState(this.composeTestRule)

        state.priceTestCase(listOf(
            PriceAssertion(
                "1.000",
                "1.000",
                null,
                true
            ),
            PriceAssertion(
                null,
                null,
                "USD uses up to 2 decimal places",
                false
            )
        ))
    }

    @Test
    fun enterInvalidDecimalSymbol() {
        val state = TestState(this.composeTestRule)

        state.priceTestCase(listOf(
            PriceAssertion(
                "1,00",
                "1,00",
                null,
                true
            ),
            PriceAssertion(
                null,
                null,
                "Digits must be in groups of 3 if using a group separator (',')",
                false
            )
        ))
    }
}