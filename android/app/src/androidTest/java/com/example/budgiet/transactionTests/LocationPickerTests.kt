package com.example.budgiet.transactionTests

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import com.example.budgiet.ui.LocationPickerDialog
import org.junit.Rule
import org.junit.Test

const val DIALOG_TEST_TAG = "LocationPickerDialog"

class LocationPickerTests {
    private class TestState(
        private val rule: ComposeContentTestRule,
    ) {
        // These two buttons exist mutually exclusive.
        val newButton
            get() = this.rule.onNode(hasTextExactly("New"))
        val doneButton
            get() = this.rule.onNode(hasTextExactly("Submit"))

        init {
            this.rule.setContent {
                LocationPickerDialog(
                    modifier = Modifier.testTag(DIALOG_TEST_TAG),
                    onDismiss = { },
                    onSubmit = { },
                )
            }
        }
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun showNewLocationDialog() {
        val state = TestState(this.rule)

        state.newButton.performClick()
        this.rule.onNode(hasTextExactly("New location"))
            .assertExists()
    }

    /** Tests that a Location is displayed at the top of the recents list after it is added. */
    @Test
    fun newLocationInRecents() {
        val name = "Amogus"
        val address = "124 Sus Street"
        val state = TestState(this.rule)

        state.newButton.performClick()
        println(this.rule.onNodeWithTag(DIALOG_TEST_TAG).printToString())

        this.rule.onNode(hasText("Name"))
            .performTextInput(name)
        this.rule.onNode(hasText("Address"))
            .performTextInput(address)

        state.doneButton.performClick()

        this.rule.onNode(hasTextExactly("Recent"))
            .assertExists()

        this.rule.onNodeWithText(name)
            .assertExists()
            .onParent()
            .assertHasClickAction()
        this.rule.onNodeWithText(address)
            .assertExists()
            .onParent()
            .assertHasClickAction()
    }

    /** Tests that the Auto-select Location button works */
    @Test
    fun autoSelectLocation() {
        // TODO:
    }
}