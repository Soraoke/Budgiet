// This file (and the utils package) contains miscellaneous UI Composables that act as helpers of other Composables.
package com.example.budgiet.ui.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupPositionProvider
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

/** The *hardcoded* value for top and bottom padding of the [DropdownMenu][androidx.compose.material3.DropdownMenu].
 * This cannot be changed. */
val DROPDOWN_MENU_VERTICAL_PADDING = 8.dp

/** The default amount of space between a tooltip's content and its anchor. */
val TOOLTIP_ANCHOR_SPACING = 4.dp

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
 * the **text** passed in will pup up with a box around it.
 *
 * @param text The text that will be displayed as the content of the *tooltip*.
 * @param positioning Where the **tooltip** should be placed relative to the [content] anchor.
 * @param spacing The amount of space between the **tooltip** and the [content].
 * @param setContentDescription Adds a [ContentDescription][androidx.compose.ui.semantics.SemanticsPropertyReceiver.contentDescription]
 *   [Modifier] to the [TooltipBox] using the **[text]** argument.
 *   This is so that you can leave **contentDescription** as `null` for icons and such that are in the [TooltipBox][PlainToolTipBox].
 * @param dialogPosition The position of the first *window* that was created within the app's screen.
 *   This argument is only necessary if this composable is called from a
 *   [Popup][androidx.compose.ui.window.Popup] or [DropdownMenu][androidx.compose.material3.DropdownMenu] that is within a [Dialog][Dialog],
 *   the **tooltip** will be positioned relative to the dialog instead of the screen, because it is only aware of the window within which it is being rendered in.
 *   This causes the **tooltip's** position on the screen to be off because it thinks it is being positioned relative to the screen.
 *
 *   ### Example:
 *   ```kt
 *   val dialogPosition by remember { mutableStateOf(IntOffset(0, 0)) }
 *
 *   Dialog(modifier
 *       .onGloballyPositioned { coords -> dialogPosition = coords.positionOnScreen().round() }
 *   ) {
 *      DropdownMenu {
 *          PlainToolTipBox("my tooltip :)", dialogPosition = dialogPosition) {
 *              Text("Hii!")
 *          }
 *      }
 *   }
 *   ```
 * @param content The content that the **tooltip** will *anchor* to.
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainToolTipBox(
    text: String,
    modifier: Modifier = Modifier,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    spacing: Dp = TOOLTIP_ANCHOR_SPACING,
    setContentDescription: Boolean = true,
    dialogPosition: IntOffset? = null,
    content: @Composable (() -> Unit)
) {
    TooltipBox(
        modifier = modifier.run { if (setContentDescription) {
            semantics { contentDescription = text }
        } else this },
        positionProvider = if (dialogPosition == null) {
            TooltipDefaults.rememberTooltipPositionProvider(
                positioning = positioning,
                spacingBetweenTooltipAndAnchor = spacing,
            )
        } else {
            rememberTooltipWithinPopupPositionProvider(
                dialogPosition = dialogPosition,
                positioning = positioning,
                spacing = spacing
            )
        },
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip { Text(text) }
        },
        content = content,
    )
}

/** Obtained from [this stackoverflow response](https://stackoverflow.com/a/78968130).  */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberTooltipWithinPopupPositionProvider(
    dialogPosition: IntOffset,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    spacing: Dp = TOOLTIP_ANCHOR_SPACING,
): PopupPositionProvider {
    val spacing = with(LocalDensity.current) { spacing.roundToPx() }

    return remember(positioning, spacing, dialogPosition) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val anchorBounds = anchorBounds.translate(-dialogPosition.x, -dialogPosition.y)

                val centerX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                val left = IntOffset(
                    x = anchorBounds.left - popupContentSize.width - spacing,
                    y = anchorBounds.top,
                )
                val right = IntOffset(
                    x = anchorBounds.right + spacing,
                    y = anchorBounds.top,
                )

                val position = when (positioning) {
                    TooltipAnchorPosition.Above -> IntOffset(
                        x = centerX,
                        y = anchorBounds.top - popupContentSize.height - spacing,
                    )
                    TooltipAnchorPosition.Below -> IntOffset(
                        x = centerX,
                        y = anchorBounds.bottom + spacing,
                    )
                    TooltipAnchorPosition.Left -> left
                    TooltipAnchorPosition.Right -> right
                    TooltipAnchorPosition.Start -> when (layoutDirection) {
                        LayoutDirection.Ltr -> left
                        LayoutDirection.Rtl -> right
                    }
                    TooltipAnchorPosition.End -> when (layoutDirection) {
                        LayoutDirection.Ltr -> right
                        LayoutDirection.Rtl -> left
                    }
                    else -> throw Exception("unreachable branch")
                }

                return position
            }
        }
    }
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
 *   (i.e. the query text is not empty).
 *   This is useful when the Search bar has very limited width. */
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
    val verticalCrop = DROPDOWN_MENU_VERTICAL_PADDING
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
@JvmInline
@Suppress("unused")
value class Corner private constructor(private val bitFlag: Byte) {
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

sealed class BorderStyle {
    object Solid: BorderStyle()
    data class Dashed(
        val dashOnLength: Dp = 7.dp,
        val dashOffLength: Dp = 3.dp,
    ): BorderStyle()
}

fun Modifier.border(width: Dp, color: Color, shape: Shape, style: BorderStyle = BorderStyle.Solid): Modifier
    = when (style) {
        is BorderStyle.Solid -> this.border(width, color, shape)
        is BorderStyle.Dashed -> this.then(DashedBorderModifier(width, color, shape, style.dashOnLength, style.dashOffLength))
    }

/** Inspired by [this article](https://www.codestudy.net/blog/how-to-have-dashed-border-in-jetpack-compose/#solution-2-building-a-reusable-drawmodifier). */
private data class DashedBorderModifier(
    val strokeWidth: Dp,
    val color: Color,
    val shape: Shape,
    val dashOnLength: Dp,
    val dashOffLength: Dp,
): ModifierNodeElement<DashedBorderModifier.DashedBorderNode>() {
    override fun create() = DashedBorderNode(
        this.strokeWidth,
        this.color,
        this.shape,
        this.dashOnLength,
        this.dashOffLength,
    )

    override fun update(node: DashedBorderNode) {
        node.strokeWidth = this.strokeWidth
        node.color = this.color
        node.shape = this.shape
        node.dashOnLength = this.dashOnLength
        node.dashOffLength = this.dashOffLength
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dashedBorder"
        properties["color"] = color
        properties["strokeWidth"] = strokeWidth
        properties["dashOnLength"] = dashOnLength
        properties["dashOffLength"] = dashOffLength
        properties["shape"] = shape
    }

    data class DashedBorderNode(
        var strokeWidth: Dp,
        var color: Color,
        var shape: Shape,
        var dashOnLength: Dp,
        var dashOffLength: Dp,
    ): Modifier.Node(), DrawModifierNode, LayoutModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }

        override fun ContentDrawScope.draw() = this.let { drawScope -> with(this@DashedBorderNode) {
            // Convert dp to pixels using DrawScope's density.
            // Not sure why but the stroke is drawn with -1 width, so must compensate for that.
            val strokeWidth = (this.strokeWidth + 1.dp).toPx()
            val dashOnLength = this.dashOnLength.toPx()
            val dashOffLength = this.dashOffLength.toPx()

            drawScope.drawContent()
            
            drawScope.drawOutline(
                outline = shape.createOutline(drawScope.size, drawScope.layoutDirection, drawScope),
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(dashOnLength, dashOffLength),
                        phase = 0f
                    )
                ),
            )
        } }
    }
}