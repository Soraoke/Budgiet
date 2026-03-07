// This file (and the utils package) contains miscellaneous UI Composables that act as helpers of other Composables.
package com.example.budgiet.ui.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import com.example.budgiet.R
import com.example.budgiet.localDateFromUtcMillis
import java.time.LocalDate

val DIALOG_PROPERTIES = DialogProperties(
    dismissOnBackPress = true,
    dismissOnClickOutside = true,
    usePlatformDefaultWidth = true,
)
val POPUP_PROPERTIES = PopupProperties(
    dismissOnBackPress = true,
    dismissOnClickOutside = true,
    focusable = true,
    clippingEnabled = true,
)

/** The space between the [Icon] and the [Text] in [TextIconButton] and [FilledTextIconButton]. */
val TEXT_ICON_BUTTON_SPACING = 4.dp

/** A *filled* [Button] that contains an [Icon] and some [Text].
 *
 * Same as [TextIconButton], but is *filled*.
 *
 * See [Button] for arguments. */
@Composable
fun FilledTextIconButton(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    // Use default padding for TextButton because it has less padding on the right.
    contentPadding: PaddingValues = ButtonDefaults.TextButtonWithIconContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    ) {
        icon()
        Spacer(Modifier.width(TEXT_ICON_BUTTON_SPACING))
        text()
    }
}

/** A box-open [Button] that contains an [Icon] and some [Text].
 *
 * See [TextButton] for arguments. */
@Composable
fun TextIconButton(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonWithIconContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    ) {
        icon()
        Spacer(Modifier.width(TEXT_ICON_BUTTON_SPACING))
        text()
    }
}

/** A shortcut for adding a [PlainTooltip] to some **content**.
 *
 * When the user activates the [PlainTooltip] (i.e. by long-pressing the **content**),
 * the **text** passed in will pup up with a box around it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainToolTipBox(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = @Suppress("DEPRECATION") TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip { Text(text) }
        },
        content = content,
    )
}

/** A [SearchBar][DockedSearchBar] that *does not* expand to show its result items.
 * Instead, the items must be placed in a different composable.
 *
 * The caller must update the search results when a *change in input* has been detected.
 * This is done through the **onQueryChange** Callback, which provides the *new search input*.
 *
 * The caller can also provide their own [TextFieldState] if they want to have control over the *search input*.
 *
 * @param placeholder Placeholder text that is displayed when the query is empty.
 * @param hideIconOnQuery Hide the **search Icon** when the user has text on the [SearchBar][PlainSearchBar]
 *   (i.e. the query text is not empty). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainSearchBar(
    modifier: Modifier = Modifier,
    onQueryChange: (CharSequence) -> Unit,
    state: TextFieldState = rememberTextFieldState(),
    placeholder: @Composable () -> Unit = { Text("Search", autoSize = TextAutoSize.StepBased(), softWrap = false, maxLines = 1) },
    hideIconOnQuery: Boolean = false,
) {
    SearchBarDefaults.InputField(
        modifier = modifier,
        query = state.text.toString(),
        onQueryChange = {
            state.edit { replace(0, length, it) }
            onQueryChange(state.text)
        },
        onSearch = { },
        expanded = false,
        onExpandedChange = { },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unfocusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        placeholder = placeholder,
        leadingIcon = if (!hideIconOnQuery || state.text.isEmpty()) { {
            Icon(painterResource(R.drawable.search_24px), null)
        } } else { null },
        trailingIcon = if (state.text.isNotEmpty()) { {
            PlainToolTipBox("Clear search") {
                IconButton(onClick = {
                    state.edit { replace(0, length, "") }
                    onQueryChange(state.text)
                }) {
                    Icon(painterResource(R.drawable.close_24px), "Clear search")
                }
            }
        } } else {
            null
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSubmit: (LocalDate) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDate = selectedDate,
        /** Only allow [Date][LocalDate]s that occurred.
         * That is, dates that are in the *past or present*. */
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return localDateFromUtcMillis(utcTimeMillis) <= LocalDate.now()
            }
            override fun isSelectableYear(year: Int): Boolean {
                return year <= LocalDate.now().year
            }
        },
    )

    DatePickerDialog(
        modifier = modifier,
        properties = DIALOG_PROPERTIES,
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onSubmit(datePickerState.getSelectedDate()!!) // There is always a selected date.
                }
            ) {
                Text("Ok")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = true,
        )
    }
}
