package com.example.budgiet.transactionTests

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.text.input.ImeAction
import com.example.budgiet.ui.FAKE_TAGS
import com.example.budgiet.ui.Tag
import com.example.budgiet.ui.TagCreatorDialog
import com.example.budgiet.ui.TagsField
import com.example.budgiet.ui.TagsPickerDialog
import org.junit.Rule
import org.junit.Test

private const val TAGS_FIELD_TEST_TAG = "tagsFieldContent"
private const val TAGS_DIALOG_TEST_TAG = "tagsPickerDialog"

class TagsPickerTests {
    private class TestState(
        private val rule: ComposeContentTestRule,
        tags: List<Tag> = FAKE_TAGS,
    ) {
        val tagsFieldContent
            get() = this.rule.onNodeWithTag(TAGS_FIELD_TEST_TAG)
        val tagsPickerDialog get() = run {
            if (!this.showPicker) {
                this.rule
                    .onNode(hasContentDescriptionExactly("Attach tag"))
                    .performClick()
            }
            this.rule.onNodeWithTag(TAGS_DIALOG_TEST_TAG)
        }
        val tagCreatorDialog get() = run {
            val isTagCreatorDialog = this
                .tagsPickerDialog
                .runCatching { assertTextContains("Create new tag") }
                .getOrNull()
                .let { it != null }

            if (!isTagCreatorDialog) {
                this.tagsPickerDialog
                    .onChildren()
                    .filterToOne(hasContentDescriptionExactly("New tag"))
                    .performClick()
            }
            this.tagsPickerDialog
        }

        val selectedTags = mutableStateSetOf<Tag>()

        private val allTags = SnapshotStateSet<Tag>().apply { addAll(tags) }
        private var showPicker by mutableStateOf(false)

        init {
            this.rule.setContent {
                Row {
                    TagsField(
                        modifier = Modifier.testTag(TAGS_FIELD_TEST_TAG),
                        selectedTags = selectedTags,
                        onButtonClick = { showPicker = true },
                    )
                }

                if (showPicker) {
                    TagsPickerDialog(
                        modifier = Modifier.testTag(TAGS_DIALOG_TEST_TAG),
                        allTags = this.allTags,
                        selectedTags = selectedTags,
                        onNewTag = { this.allTags.add(it) },
                        onSubmit = { selectedTags.addAll(it) },
                        onDismiss = { showPicker = false },
                    )
                }
            }
        }
    }

    @get:Rule
    val rule = createComposeRule()

    /** Tests that tags selected in the [TagsPickerDialog] will also appear in the [TestState.tagsFieldContent] (after pressing 'Done' of course). */
    @Test
    fun selectTagsFromPicker() {
        val state = TestState(this.rule)

        state.tagsFieldContent.assertDoesNotExist()

        // Select a Tag from the TagsPickerDialog.
        state.tagsPickerDialog
            .onChildren()
            .filterToOne(hasTextExactly(FAKE_TAGS[0].name))
            .performClick()
        state.tagsPickerDialog
            .onChildren()
            .filterToOne(hasTextExactly("Cancel") and hasClickAction())
            .performClick()

        // Check that the tag is displayed in the field content.
        val tag = state.tagsFieldContent
            .onChildren()
            .filterToOne(hasTextExactly(FAKE_TAGS[0].name))
            .assertExists()

        // Check that the tag can be de-selected.
        tag.onChildren()
            .filterToOne(hasContentDescriptionExactly("Remove tag"))
            .performClick()

        state.tagsFieldContent.assertDoesNotExist()
    }

    /** Tests that the SearchBar in [TagsPickerDialog] filters tags by name correctly. */
    @Test
    fun tagsPickerSearch() {
        val state = TestState(this.rule)
        val searchText = "ut"

        state.tagsPickerDialog
            .onChildren()
            .filterToOne(hasImeAction(ImeAction.Search))
            .performTextInput(searchText)

        state.tagsPickerDialog
            .onChildren()
            .filterToOne(hasScrollAction())
            .onChildren()
            .assertAll(hasText(searchText, substring = true, ignoreCase = true))
    }

    /** Tests that a Tag appears in the [TagsPickerDialog] after pressing 'Submit' in the [TagCreatorDialog]. */
    @Test
    fun newTag() {
        val state = TestState(this.rule)
        val newTagName = "MyNewTag"

        state.tagCreatorDialog
            .onChildren()
            .apply { printToLog("HUF", Int.MAX_VALUE) }
            .filterToOne(hasText("Tag name") and hasImeAction(ImeAction.Default))
            .performTextInput(newTagName)

        state.tagCreatorDialog
            .onChildren()
            .filterToOne(hasContentDescriptionExactly("Submit"))
            .performClick()

        state.tagsPickerDialog
            .onChildren()
            .filterToOne(hasScrollAction())
            .onChildren()
            .assertAny(hasTextExactly(newTagName))
    }

    /** Tests that the tag name is validated,
     * and that the tag ***cannot*** be submitted. */
    @Test
    fun invalidNewTag() {
        val state = TestState(this.rule)
        val newTagName = FAKE_TAGS[0].name

        state.tagCreatorDialog
            .onChildren()
            .filterToOne(hasText("Tag name") and hasImeAction(ImeAction.Default))
            .performTextInput(newTagName)

        state.tagCreatorDialog
            .onChildren()
            .filterToOne(hasContentDescriptionExactly("Submit"))
            .performClick()
            // Submit button is disabled after attempting to submit bad tag data.
            .assertIsNotEnabled()

        state.tagCreatorDialog
            .assertTextContains("already exists", substring = true, ignoreCase = true)
    }
}