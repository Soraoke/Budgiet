// This file (and the utils package) contains miscellaneous UI Composables that act as helpers of other Composables.
package com.example.budgiet.ui.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import com.example.budgiet.R
import com.example.budgiet.localDateFromUtcMillis
import java.time.LocalDate
import kotlin.experimental.and
import kotlin.experimental.or

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
 * @param placeholderText Placeholder text that is displayed when the query is empty.
 * @param hideIconOnQuery Hide the **search Icon** when the user has text on the [SearchBar][PlainSearchBar]
 *   (i.e. the query text is not empty). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainSearchBar(
    modifier: Modifier = Modifier,
    onQueryChange: (CharSequence) -> Unit,
    state: TextFieldState = rememberTextFieldState(),
    placeholderText: String = "Search",
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
        placeholder = {
            Text(
                text = placeholderText,
                softWrap = false,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(maxFontSize = LocalTextStyle.current.fontSize),
            )
        },
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

/** Structured padding values for [ActionDialog].
 *
 * Default values respect the [Material 3 Spec](https://m3.material.io/components/dialogs/specs).
 *
 * @param dialogEdges Padding to apply *around* all the content within the [Dialog][ActionDialog].
 * @param titleSpacerHeight How much space to put between the **title** and **content** Composables in the [Dialog][ActionDialog].
 * @param actionsSpacerHeight How much space to put between the **actions** and **content** Composables in the [Dialog][ActionDialog]. */
data class ActionDialogPadding(
    val dialogEdges: PaddingValues = PaddingValues(24.dp),
    val titleSpacerHeight: Dp = 16.dp,
    val actionsSpacerHeight: Dp = 24.dp,
) {
    companion object {
        /** Default values respecting the [Material 3 Spec](https://m3.material.io/components/dialogs/specs). */
        val Default = ActionDialogPadding()

        val TightlyPacked = ActionDialogPadding(
            dialogEdges = PaddingValues(all = 8.dp),
            titleSpacerHeight = 8.dp,
            actionsSpacerHeight = 4.dp,
        )
    }
}

/** Like an [AlertDialog][androidx.compose.material3.AlertDialog], but offers more freedom when placing the **action** buttons and title content.
 *
 * See [Card] for details on all other arguments.
 *
 * @param padding Structured padding values. See [ActionDialogPadding].
 * @param title A Composable to be displayed **above** of the [content].
 * @param actions A set of buttons (or any other composable) that are displayed **below** the [content] and laid out **horizontally**. */
@Composable
fun ActionDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    padding: ActionDialogPadding = ActionDialogPadding.Default,
    title: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DIALOG_PROPERTIES,
    ) {
        // PRO TIP: doesn't actually fill max width, it has a margin
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
        ) {
            Column(Modifier.padding(padding.dialogEdges)) {
                this.title()
                Spacer(Modifier.height(padding.titleSpacerHeight))
                this.content()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = padding.actionsSpacerHeight),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
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

/** DropdownMenu has a *hardcoded* vertical padding of `8.dp`, which should be so that the caller can apply their own padding. */
fun Modifier.hideDropdownMenuPadding(): Modifier = this.layout { measurable, constraints ->
    val verticalCrop = 8.dp
    val placeable = measurable.measure(constraints)
    fun Dp.toPxInt(): Int = this.toPx().toInt()

    layout(
        placeable.width,
        placeable.height - (verticalCrop * 2).toPxInt()
    ) {
        placeable.placeRelative(0, -verticalCrop.toPx().toInt())
    }
}

/** Indicates any of the 4 *sides* of a rectangular shape.
 *
 * This enum supports bitwise operations (e.g. **or**) to allow combinations of multiple variants. */
@Suppress("unused")
class Corner private constructor(private val bitFlag: Byte) {
    companion object {
        val TopLeft = Corner(0b0001)
        val TopRight = Corner(0b0010)
        val BottomLeft = Corner(0b0100)
        val BottomRight = Corner(0b1000)
        val Top = TopLeft or TopRight
        val Bottom = BottomLeft or BottomRight
        val Left = TopLeft or BottomLeft
        val Right = TopRight or BottomRight
    }

    /** Bit-wise operator **or**. */
    // No operator symbol |? this is ass...
    infix fun or(other: Corner): Corner
        = Corner(this.bitFlag or other.bitFlag)

    /** Whether the receiver (`this`) includes the specified [Corner] in its bitFlag. */
    infix fun includes(other: Corner): Boolean
        = (this.bitFlag and other.bitFlag) != 0.toByte()
}

/** Assign *fully-rounded* shape to all corners of a UI element (e.g. [Button]),
 * except for the corners specified in the **sharpSide** argument.
 * Those sides specified are assigned a *sharp* shape (small corner radius). */
@Composable
fun halfRoundedCornerShape(sharpSide: Corner): RoundedCornerShape {
    val sharpRadius = MaterialTheme.shapes.extraSmall.bottomEnd
    val roundRadius = CornerSize(percent = 50)
    val corner = { corner: Corner ->
        if (sharpSide includes corner) sharpRadius else roundRadius
    }

    return RoundedCornerShape(
        topStart = corner(Corner.TopLeft),
        bottomStart = corner(Corner.BottomLeft),
        topEnd = corner(Corner.TopRight),
        bottomEnd = corner(Corner.BottomRight),
    )
}
