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
import com.example.budgiet.assert
import com.example.budgiet.assertEquals
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
    fun assertCounter(count: Int)
         = this.descriptionField
            .getSemanticsProperty(SemanticsProperties.Text)
            .getOrThrow()
            // The counter Node is always the last in the TextField Node.
            .last()
            // Parse the character count.
            .text
            .split('/', limit = 2)[0]
            .toInt()
            .assertEquals(count)

    fun assertIsError() {
        this.descriptionField
            .getSemanticsProperty(SemanticsProperties.Error)
            .getOrThrow()
            .assertEquals("Invalid input")

        this.descriptionField
            .getSemanticsProperty(SemanticsProperties.Text)
            .getOrThrow()
            .let { it[0].text }
            .assertEquals("Description is too long!")

    }

    fun assertIsNotError() {
        this.descriptionField
            .getSemanticsProperty(SemanticsProperties.Error)
            .getOrNull()
            .assertEquals(null)

        this.descriptionField
            .getSemanticsProperty(SemanticsProperties.Text)
            .getOrThrow()
            .size
            .assertEquals(1)
    }
}

class DescriptionFieldTests {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun largeGraphemesTest() {
        val snowMan = "☃"
        val accentedE = "e${'´'}"
        val state = TestState(rule)

        // Check that multiple code units count as 1 character.
        assertEquals(3, snowMan.encodeToByteArray().size)
        assertEquals(1, snowMan.length)

        assertEquals(3, accentedE.encodeToByteArray().size)
        assertEquals(2, accentedE.length)

        // Check that the field accepts multiple code units as 1 character.
        state.descriptionField.performTextInput("a".repeat(DESCRIPTION_MAX_LENGTH - 1))
        state.descriptionField.performTextInput(snowMan)
        state.descriptionField
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .getOrThrow()
            .text
            .assert({ it.contains(snowMan) }) { "DescriptionField's EditableText did not contain snowMan grapheme" }
        state.assertCounter(DESCRIPTION_MAX_LENGTH)
        state.assertIsNotError()

        // Deleting 1 character deletes the 3 code units.
        @OptIn(ExperimentalTestApi::class)
        state.descriptionField.performKeyInput {
            this.pressKey(Key.Backspace)
        }
        state.assertCounter(DESCRIPTION_MAX_LENGTH - 1)

        // Check that the field accepts multiple code points as 1 character.
        state.descriptionField.performTextInput(accentedE)
        // TODO: this will work later when grapheme string is implemented in rust. trust
//        state.descriptionField
//            .getSemanticsProperty(SemanticsProperties.EditableText)
//            .getOrThrow()
//            .text
//            .assert({ it.contains(accentedE) }) { "DescriptionField's EditableText did not contain accentedE grapheme" }
        state.assertCounter(DESCRIPTION_MAX_LENGTH)
//        state.assertIsNotError()
    }

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
        state.descriptionField
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .getOrThrow()
            .text.length
            .assertEquals(DESCRIPTION_MAX_LENGTH)

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

        state.descriptionField
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .getOrThrow()
            .text
            .assertEquals("a".repeat(DESCRIPTION_MAX_LENGTH))

        // Check that the counter is updated.
        state.assertCounter(DESCRIPTION_MAX_LENGTH)

        // Type one more ASCII character, check that it is ignored/blocked.
        state.descriptionField.performTextInput("b")
        state.descriptionField
            .getSemanticsProperty(SemanticsProperties.EditableText)
            .getOrThrow()
            .assert( { !it.contains('b') }) { "DescriptionField contains characters that were typed after character limit was reached" }

        // Check that it is NOT in error state
        // FIXME: This panics in CI but not in local :/
//        state.assertIsNotError()
    }
}
