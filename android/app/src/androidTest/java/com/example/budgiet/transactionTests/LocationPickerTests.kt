package com.example.budgiet.transactionTests

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.budgiet.DbEntry
import com.example.budgiet.assert
import com.example.budgiet.getSemanticsProperty
import com.example.budgiet.onDescendants
import com.example.budgiet.ui.FAKE_LOCATIONS
import com.example.budgiet.ui.Location
import com.example.budgiet.ui.LocationField
import com.example.budgiet.ui.LocationPickerDialog
import com.example.budgiet.ui.LocationPickerState
import com.example.budgiet.ui.LocationViewModel
import org.junit.Rule
import org.junit.Test

const val DIALOG_TEST_TAG = "LocationPickerDialog"

class LocationPickerTests {
    private class TestState(
        private val rule: ComposeContentTestRule,
        locations: Map<UInt, Location> = FAKE_LOCATIONS,
        initiallySelectedLocation: DbEntry<Location>? = null,
    ) {
        // These buttons exist mutually exclusive.

        /** The button that opens the **LocationSearchDialog**. */
        val selectLocationButton
            get() = this.rule.onNode(hasContentDescriptionExactly("Select location"))
                .assertHasClickAction()
        /** The `"New"` button in the **LocationSearchDialog**. */
        val newButton
            get() = this.rule.onNode(hasContentDescriptionExactly("Add new location"))
                .onChild()
                .assertHasClickAction()
        /** The `"Submit"` button in the **LocationEditorDialog**. */
        val submitButton
            get() = this.rule.onNode(
                hasContentDescriptionExactly("Submit new location")
                or hasContentDescriptionExactly("Save changes")
            ).onChild()
                .assertHasClickAction()

        val dialogNode
            get() = this.rule.onNode(hasTestTag(DIALOG_TEST_TAG))

        fun showLocationPickerDialog() {
            if (dialogState == null) {
                this.rule.onNode(hasContentDescriptionExactly("Select location"))
                    .performClick()
            }
        }

        /** Look for a specific item in the scrollable list.
         * Note: must call [showLocationPickerDialog] before calling this. */
        fun onLocationItem(itemName: String)
            = this.dialogNode
                .onDescendants(this.rule)
                .filterToOne(hasScrollAction())
                .performScrollToNode(hasText(itemName))
                .onChildren()
                .filterToOne(hasText(itemName))

        private var dialogState by mutableStateOf<LocationPickerState?>(null)

        init {
            val viewModel = LocationViewModel().apply {
                useAlternativeLocations(locations)
                selectedLocation = initiallySelectedLocation
            }

            this.rule.setContent {
                Row {
                    LocationField(
                        viewModel = viewModel,
                        onClickSelect = { dialogState = LocationPickerState.Search },
                        onClickNearby = { dialogState = LocationPickerState.Nearby },
                    )
                }

                dialogState?.let { state ->
                    LocationPickerDialog(
                        modifier = Modifier.testTag(DIALOG_TEST_TAG),
                        viewModel = viewModel,
                        state = state,
                        onStateChange = { dialogState = it },
                        onDismiss = { dialogState = null },
                    )
                }
            }
        }
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun selectLocation() {
        val state = TestState(this.rule)
        val targetItem = FAKE_LOCATIONS[3u]!!

        // Select the item.
        state.showLocationPickerDialog()
        state.onLocationItem(targetItem.name)
            .performClick()

        // Check that the item was selected.
        state.selectLocationButton
            .assertTextEquals(targetItem.toString())
    }

    @Test
    fun searchLocation() {
        val state = TestState(this.rule)
        val targetName = FAKE_LOCATIONS[3u]!!.name
        val query = targetName.take(5)

        state.showLocationPickerDialog()
        state.dialogNode
            .onDescendants(this.rule)
            .filterToOne(hasContentDescriptionExactly("Search"))
            .performTextInput(query)

        // Check that the item shows up.
        state.onLocationItem(targetName)
            .assertExists()
    }

    /** Tests that a Location is displayed at the top of the recents list after it is added. */
    @Test
    fun newLocation() {
        val state = TestState(this.rule)
        val name = "Amogus"
        val address = "124 Sus Street"

        state.showLocationPickerDialog()
        state.newButton.performClick()

        state.dialogNode
            .onDescendants(this.rule)
            .filterToOne(hasText("Name"))
            .performTextInput(name)
        state.dialogNode
            .onDescendants(this.rule)
            .filterToOne(hasText("Address (optional)"))
            .performTextInput(address)

        state.submitButton.performClick()

        this.rule.onNode(hasTextExactly("Recent"))
            .assertExists()

        // Check that the new item exists AND is first in the list.
        state.dialogNode
            .onDescendants(this.rule)
            .filterToOne(hasScrollAction())
            .onChildAt(0)
            .getSemanticsProperty(SemanticsProperties.Text)
            .getOrThrow()
            .assert({
                it.size == 2
                && it[0].text == name
                && it[1].text == address
            })
    }

    @Test
    fun editLocation() {
        val state = TestState(this.rule)
        val targetItem = FAKE_LOCATIONS[0u]!!
        val newName = "Chipped"

        state.showLocationPickerDialog()
        state.onLocationItem(targetItem.name)
            .performMouseInput { longClick() }

        this.rule.onNode(hasContentDescriptionExactly("Edit"))
            .performClick()

        // Check that submit is disabled before making changes
        state.submitButton
            .assertIsNotEnabled()

        state.dialogNode
            .onDescendants(this.rule)
            .filterToOne(hasText("Name"))
            .apply { performTextClearance() }
            .performTextInput(newName)

        state.submitButton.performClick()

        // Check that the new item exists AND is first in the list.
        state.dialogNode
            .onDescendants(this.rule)
            .filterToOne(hasScrollAction())
            .onChildAt(0)
            .getSemanticsProperty(SemanticsProperties.Text)
            .getOrThrow()
            .assert({
                it.size == 2
                && it[0].text == newName
                && it[1].text == targetItem.address
            })
    }

    @Test
    fun deleteLocation() {
        val state = TestState(this.rule)
        val targetName = FAKE_LOCATIONS[3u]!!.name

        state.showLocationPickerDialog()
        state.onLocationItem(targetName)
            .performMouseInput { longClick() }

        this.rule.onNode(hasContentDescriptionExactly("Delete"))
            .performClick()

        // Check that the item does not exist anymore.
        state.dialogNode
            .onDescendants(this.rule)
            .filterToOne(hasScrollAction())
            .apply {
                runCatching { performScrollToNode(hasText(targetName)) }
                    .assert({ it.isFailure }) { "Expected Scrollable list not to contain item with name \"$targetName\"" }
            }
            .onChildren()
            .filterToOne(hasText(targetName))
            .assertDoesNotExist()
    }

    @Test
    fun invalidNewLocation() {
        val state = TestState(this.rule)
        val targetItem = FAKE_LOCATIONS[0u]!!

        state.showLocationPickerDialog()
        state.newButton.performClick()

        state.dialogNode
            .onDescendants(this.rule)
            .filterToOne(hasText("Name"))
            .performTextInput(targetItem.name)
        state.dialogNode
            .onDescendants(this.rule)
            .filterToOne(hasText("Address (optional)"))
            .apply { targetItem.address?.let { performTextInput(it) } }

        state.submitButton
            .assertIsEnabled()
            .performClick()

        // Check that the error state is displayed.
        state.dialogNode
            .onDescendants(this.rule)
            .filterToOne(SemanticsMatcher("has text anywhere") { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.let { text ->
                    text.any { it.text.contains("already exists") }
                } ?: false
            })
            .assertExists()

        state.submitButton.assertIsNotEnabled()
    }

    @Test
    fun nearbyLocations() {
        // TODO:
    }

    @Test
    fun nearbyAddresses() {
        // TODO:
    }
}