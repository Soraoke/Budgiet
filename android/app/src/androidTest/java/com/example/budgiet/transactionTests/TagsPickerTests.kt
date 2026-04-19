package com.example.budgiet.transactionTests

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import com.example.budgiet.filterNodes
import com.example.budgiet.getSemanticsProperty
import com.example.budgiet.onDescendants
import com.example.budgiet.ui.FAKE_TAGS
import com.example.budgiet.ui.Tag
import com.example.budgiet.ui.TagEditorDialog
import com.example.budgiet.ui.TagsField
import com.example.budgiet.ui.TagsPickerDialog
import com.example.budgiet.ui.TagsViewModel
import org.junit.Rule
import org.junit.Test

private const val TAGS_FIELD_TEST_TAG = "tagsFieldContent"
private const val TAGS_DIALOG_TEST_TAG = "tagsPickerDialog"

class TagsPickerTests {
    private class TestState(
        private val rule: ComposeContentTestRule,
        tags: List<Tag> = FAKE_TAGS,
        initiallySelectedTags: List<String> = listOf(),
    ) {
        val tagsFieldContainer
            get() = this.rule.onNodeWithTag(TAGS_FIELD_TEST_TAG)
        val tagsPickerDialog get() = run {
            if (!this.showPicker) {
                this.rule
                    .onNode(hasContentDescriptionExactly("Attach tag"))
                    .performClick()
            }
            this.rule.onNodeWithTag(TAGS_DIALOG_TEST_TAG)
        }
        val tagEditorDialog
            get() = this.rule.onNode(hasAnyAncestor(isDialog())
                and (hasTestTag(TAGS_DIALOG_TEST_TAG)
                    or hasAnyChild(hasText("Create new tag")
                    or hasText("Edit tag"))))

        fun openTagCreatorDialog(): SemanticsNodeInteraction {
            // Open Tag creator dialog.
            this.tagsPickerDialog
                .onDescendants(this.rule)
                .filterToOne(hasTextExactly("New tag"))
                .performClick()
            return this.tagEditorDialog
        }

        private var showPicker by mutableStateOf(false)

        init {
            val viewModel = TagsViewModel().apply {
                useAlternativeTags(tags)
                selectedTags.addAll(initiallySelectedTags)
            }

            this.rule.setContent {
                Row {
                    TagsField(
                        modifier = Modifier.testTag(TAGS_FIELD_TEST_TAG),
                        viewModel = viewModel,
                        onButtonClick = { showPicker = true },
                    )
                }

                if (showPicker) {
                    TagsPickerDialog(
                        modifier = Modifier.testTag(TAGS_DIALOG_TEST_TAG),
                        viewModel = viewModel,
                        onDismiss = { showPicker = false },
                    )
                }
            }
        }
    }

    @get:Rule
    val rule = createComposeRule()

    /** Tests that tags selected in the [TagsPickerDialog] will also appear in the [TestState.tagsFieldContainer] (after pressing 'Done' of course). */
    @Test
    fun selectTagsFromPicker() {
        val state = TestState(this.rule)

        state.tagsFieldContainer.assertDoesNotExist()

        // Select a Tag from the TagsPickerDialog.
        state.tagsPickerDialog
            .onDescendants(this.rule)
            .filterToOne(hasTextExactly(FAKE_TAGS[0].name))
            .assertExists()
            .performClick()
        state.tagsPickerDialog
            .onDescendants(this.rule)
            .filterToOne(hasContentDescriptionExactly("Submit"))
            .performClick()

        // Check that the tag is displayed in the field content.
        state.tagsFieldContainer
            .onDescendants(this.rule)
            .filterToOne(hasTextExactly(FAKE_TAGS[0].name))
            .assertExists()

        // Check that the tag can be de-selected.
            .onChildren()
            .filterToOne(hasContentDescriptionExactly("Remove tag"))
            .performClick()

        // The tags field container is hidden if there are no Tags.
        state.tagsFieldContainer.assertDoesNotExist()
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

    /** Tests that a Tag appears in the [TagsPickerDialog] after pressing 'Submit' in the [TagEditorDialog]. */
    @Test
    fun newTag() {
        val state = TestState(this.rule)
        val newTagName = "MyNewTag"

        state.openTagCreatorDialog()

        state.tagEditorDialog
            .onDescendants(this.rule)
            .filterToOne(hasText("Tag name") and hasImeAction(ImeAction.Default))
            .performTextInput(newTagName)

        state.tagEditorDialog
            .onDescendants(this.rule)
            .filterToOne(hasContentDescriptionExactly("Submit"))
            .performClick()

        state.tagsPickerDialog
            .onDescendants(this.rule)
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

        state.openTagCreatorDialog()

        state.tagEditorDialog
            .onDescendants(this.rule)
            .filterToOne(hasText("Tag name") and hasImeAction(ImeAction.Default))
            .performTextInput(newTagName)

        state.tagEditorDialog
            .onDescendants(this.rule)
            .filterToOne(hasContentDescriptionExactly("Submit"))
            .performClick()
            // Submit button is disabled after attempting to submit bad tag data.
            .assertIsNotEnabled()

        state.tagEditorDialog
            .onDescendants(this.rule)
            .assertAny(hasText("already exists", substring = true, ignoreCase = true))
    }

    @Test
    fun editTag() {
        val tagName = FAKE_TAGS[1].name
        val state = TestState(this.rule, initiallySelectedTags = listOf(tagName))

        state.tagsFieldContainer
            .onDescendants(this.rule)
            .filterToOne(hasTextExactly(tagName))
            .performMouseInput { longClick() }

        this.rule.onNode(hasContentDescriptionExactly("Edit tag"))
            .performClick()

        val currentColor = state.tagEditorDialog
            .onDescendants(this.rule)
            .filterToOne(hasContentDescriptionExactly("Change tag color"))
            .performClick()
            .getSemanticsProperty(SemanticsProperties.StateDescription)
            .getOrThrow()

        // Select new color
        this.rule.onNode(hasContentDescriptionExactly("Color menu"))
            .onDescendants(this.rule)
            // Select any color other than the already selected one.
            .filterNodes {
                val stateDescription = it.config.getOrNull(SemanticsProperties.StateDescription)
                !stateDescription.isNullOrEmpty()
                && stateDescription != currentColor
            }
            .onFirst()
            .performClick()

        state.tagEditorDialog
            .onDescendants(this.rule)
            .filterToOne(hasContentDescriptionExactly("Submit"))
            .assertIsEnabled()
            .performClick()

        // TagEditorDialog is closed after successful submit
        state.tagEditorDialog.assertDoesNotExist()
    }

    @Test
    fun deleteTag() {
        val tagName = FAKE_TAGS[1].name
        val state = TestState(this.rule, initiallySelectedTags = listOf(tagName))

        state.tagsFieldContainer
            .onDescendants(this.rule)
            .filterToOne(hasTextExactly(tagName))
            .performMouseInput { longClick() }

        this.rule.onNode(hasContentDescriptionExactly("Delete tag"))
            .performClick()

        this.rule.onNode(hasTextExactly(tagName))
            .assertDoesNotExist()
    }
}