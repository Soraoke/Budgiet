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
import androidx.compose.ui.test.pressKey
import com.example.budgiet.getSemanticsProperty
import com.example.budgiet.ui.DESCRIPTION_MAX_LENGTH
import com.example.budgiet.ui.DescriptionField
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

const val DESCRIPTION_FIELD_TAG = "DescriptionField"

private class TestState(private val rule: ComposeContentTestRule) {

    val descriptionField
        get() = this.rule.onNodeWithTag(DESCRIPTION_FIELD_TAG)

    init {
        rule.setContent {
            var fieldValue by remember { mutableStateOf("") }
            DescriptionField(
                modifier = Modifier.testTag(DESCRIPTION_FIELD_TAG),
                fieldValue = fieldValue,
                onValueChange = { fieldValue = it },
            )
        }
    }

    /** Assert that the **character counter** Node is at a certain number. */
    fun assertCounter(count: Int) = assertEquals(
        count,
        this.descriptionField
            .getSemanticsProperty(SemanticsProperties.Text)
            .getOrThrow()
            // The counter Node is always the last in the TextField Node.
            .last()
            // Parse the character count.
            .text
            .split('/', limit = 2)[0]
            .toInt()
    )

    fun assertIsError() {
        assertEquals(
            "Invalid input",
            this.descriptionField
                .getSemanticsProperty(SemanticsProperties.Error)
                .getOrThrow(),
        )
        assertEquals(
            "Description is too long!",
            this.descriptionField
                .getSemanticsProperty(SemanticsProperties.Text)
                .getOrThrow()
                [0].text,
        )
    }

    fun assertIsNotError() {
        assertEquals(
            null,
            this.descriptionField
                .getSemanticsProperty(SemanticsProperties.Error)
                .getOrNull(),
        )
        assertEquals(
            1,
            this.descriptionField
                .getSemanticsProperty(SemanticsProperties.Text)
                .getOrThrow()
                .size,
        )
    }
}

class DescriptionFieldTests {
    @get:Rule
    val rule = createComposeRule()

    // TODO: test graphemes with multiple codepoints (e.g. ä (are you sure this is the multi-codepoint variant?), certain emojis)

    @Test
    fun pasteLimitTest() {
        val extraLength = 10
        val state = TestState(rule)

        // Paste more than MAX_LENGTH at once.
        state.descriptionField.performTextInput("a".repeat(DESCRIPTION_MAX_LENGTH) + "b".repeat(extraLength))

        // Check that it is in error state.
        state.assertIsError()

        // Field should NOT keep extra characters while in error state.
        state.assertCounter(DESCRIPTION_MAX_LENGTH)
        assertEquals(
            DESCRIPTION_MAX_LENGTH,
            state.descriptionField
                .getSemanticsProperty(SemanticsProperties.EditableText)
                .getOrThrow()
                .text.length,
        )

        @OptIn(ExperimentalTestApi::class)
        state.descriptionField.performKeyInput {
            this.pressKey(Key.Backspace)
        }
        state.assertCounter(DESCRIPTION_MAX_LENGTH - 1)

        // Check that it is NOT in error state.
        state.assertIsNotError()
    }

    @Test
    fun typingLimitTest() {
        val state = TestState(rule)

        // Check that counter starts at 0.
        state.assertCounter(0)

        // Type ASCII characters up to the limit.
        (0..DESCRIPTION_MAX_LENGTH).forEach { _ ->
            state.descriptionField.performTextInput("a")
        }

        assertEquals(
            "a".repeat(DESCRIPTION_MAX_LENGTH),
            state.descriptionField
                .getSemanticsProperty(SemanticsProperties.EditableText)
                .getOrThrow()
                .text,
        )

        // Check that the counter is updated.
        state.assertCounter(DESCRIPTION_MAX_LENGTH)

        // Type one more ASCII character, check that it is ignored/blocked.
        state.descriptionField.performTextInput("b")
        assert(
            !state.descriptionField
                .getSemanticsProperty(SemanticsProperties.EditableText)
                .getOrThrow()
                .contains('b')
        )

        // Check that it is NOT in error state
        state.assertIsNotError()
    }
}
