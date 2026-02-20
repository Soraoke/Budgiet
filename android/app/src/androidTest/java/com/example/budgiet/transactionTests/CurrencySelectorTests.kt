package com.example.budgiet.transactionTests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import com.example.budgiet.assert
import com.example.budgiet.assertEquals
import com.example.budgiet.getSemanticsProperty
import com.example.budgiet.ui.CurrencySelectorButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.util.Currency
import java.util.Locale

private const val CURRENCY_SELECTOR_BUTTON_TEST_TAG = "currencySelectorButton"
private val defaultLocale = Locale.US
/** Currency code of a currency that will be selected later (apart from locale currency). */
private const val otherCurrency = "CAD"

class CurrencySelectorTests {
    private class TestState(
        private val rule: ComposeContentTestRule,
        initialCurrency: Currency = Currency.getInstance(defaultLocale),
        hideDefaultCurrencyCode: Boolean = true,
    ) {
        val button
            get() = this.rule.onNodeWithTag(CURRENCY_SELECTOR_BUTTON_TEST_TAG)

        val searchBar
            get() = run {
                this.openMenu()
                this.rule.onNodeWithContentDescription("Search")
            }

        /** NOTE: each currency item node is wrapped in a TooltipBox node, which are the actual children of the LazyColumn. */
        val currenciesList
            get() = run {
                this.openMenu()
                this.rule.onNode(hasScrollAction() and hasScrollToIndexAction())
            }

        init {
            this.rule.setContent {
                var selectedCurrency by remember { mutableStateOf(initialCurrency) }

                CurrencySelectorButton(
                    modifier = Modifier.testTag(CURRENCY_SELECTOR_BUTTON_TEST_TAG),
                    hideDefaultCurrencyCode = hideDefaultCurrencyCode,
                    locale = defaultLocale,
                    selectedCurrency = selectedCurrency,
                    onCurrencyChange = { selectedCurrency = it },
                )
            }
        }

        /** Clicks on the **button** to open the `DropdownMenu`. */
        fun openMenu() {
            if (this.rule.onNode(isPopup()).isNotDisplayed()) {
                this.button.performClick()
            }
        }

        /** Clicks on a MenuItem with the **currencyCode** Text. Performs a scroll to find the item. */
        fun selectCurrency(currencyCode: String) {
            this.currenciesList
                .performScrollToKey(currencyCode)
                .onChildren()
                .filterToOne(hasAnyChild(hasText(currencyCode)))
                .onChild()
                .performClick()

            // Why need to scroll here?? The composable does that already
            this.currenciesList.performScrollToIndex(0)
        }
    }

    @get:Rule
    val rule = createComposeRule()

    /** Tests that the Currency code (i.e. USD) is hidden if the selected currency is the Locale's default,
     * but show the code next to the icon otherwise */
    @Test
    fun hideLocaleCurrencyCode() {
        val state = TestState(this.rule)

        // Starts with Locale currency selected.
        state.button
            .getSemanticsProperty(SemanticsProperties.Text)
            .getOrNull()
            ?.assert({ it.isEmpty() }) { "Button shows currency code, even though locale currency is selected." }
    }

    /** Tests that the Currency code (i.e. USD) is shown even when the selected currency is the Locale's default. */
    @Test
    fun showLocaleCurrencyCode() {
        val state = TestState(this.rule, hideDefaultCurrencyCode = false)

        // Starts with Locale currency selected.
        state.button
            .assertTextEquals(Currency.getInstance(defaultLocale).currencyCode)
    }

    /** Tests that the Currency that is used by the Locale (i.e. USD) is always at the top of the list. */
    @Test
    fun localeCurrencyIsFirst() {
        val state = TestState(this.rule)

        fun assertFirstIsLocale() {
            state.currenciesList
                .onChildAt(0)
                .onChild()
                .assertTextEquals(Currency.getInstance(defaultLocale).currencyCode)
        }

        // List starts with Locale currency first.
        assertFirstIsLocale()

        // List keeps Locale currency first, even after it gets re-sorted.
        state.selectCurrency(otherCurrency)
        assertFirstIsLocale()
    }

    /** Tests that the Currency code that is selected is displayed in the Button's content. */
    @Test
    fun selectCurrency() {
        val state = TestState(this.rule)

        // Check that the loading indicator is not shown unnecessarily.
        runTest {
            delay(50)
            state.currenciesList
                .onChildAt(0)
                .getSemanticsProperty(SemanticsProperties.ProgressBarRangeInfo)
                .assert({ it.isFailure }) { "Item with progress indicator should not be shown" }
        }

        state.selectCurrency(otherCurrency)
        state.button.assertTextEquals(otherCurrency)

        // Check that the selected currency is directly below the locale currency (so it has idx 1).
        state.currenciesList
            .onChildAt(1)
            .onChild()
            .assertTextEquals(otherCurrency)
    }

    /** Tests that the correct currencies are displayed when a search is made by the user. */
    @Test
    fun searchCurrency() {
        val state = TestState(this.rule)

        fun assertOneCurrencyItem(currencyCode: String) {
            state.currenciesList
                .onChildren()
                .fetchSemanticsNodes()
                .map { it.children[0] }
                .let{ list ->
                    list.size.assertEquals(2)
                    list[0]
                        .config[SemanticsProperties.Text][0]
                        .text
                        .assertEquals(currencyCode)
                    list[1]
                        .config[SemanticsProperties.Text][0]
                        .text
                        .assertEquals("Reset")
                }
        }

        // Search for currency code
        state.searchBar
            .performTextInput(otherCurrency)
        assertOneCurrencyItem(otherCurrency)

        // Clear search bar input
        state.searchBar
            // Clear button is wrapped in PlainTooltipBox, so must do onChild() twice.
            .onChild()
            .onChild()
            .assertContentDescriptionEquals("Clear search")
            .assertHasClickAction()
            .performClick()

        // Search for currency display name.
        state.searchBar
            .performTextInput(otherCurrency)
        state.currenciesList
            .performScrollToIndex(0)
        assertOneCurrencyItem(otherCurrency)
    }

    /** Tests that the list of currencies is reset to the original state (sorted alphabetically) when the reset button is pressed. */
    @Test
    fun clearRecentCurrencies() {
        val state = TestState(this.rule)

        // Look for CAD
        state.selectCurrency(otherCurrency)

        // Check that CAD is on top
        state.currenciesList
            .onChildAt(1)
            .onChild()
            .assertTextEquals(otherCurrency)

        // Press Reset button
        state.currenciesList
            .performScrollToKey("DELETE")
            .onChildren()
            .filterToOne(hasAnyChild(hasText("Reset")))
            .performClick()

        // Check alphabetical order
        state.currenciesList
            .onChildren()
            .fetchSemanticsNodes()
            // Locale currency and Reset button are not included in this check.
            .drop(1)
            .dropLast(1)
            .map { node ->
                node.children[0]
                    .config[SemanticsProperties.Text]
                    .map { it.text }
                    .fold("") { acc, text -> "$acc$text" }
            }
            .assert({ currencies ->
                currencies == currencies.sorted()
            }) { "Currencies were not sorted in alphabetical order after reset" }
    }
}