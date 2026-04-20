@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.budgiet.ui.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.TrackStopIndicatorSize
import androidx.compose.material3.SliderDefaults.drawStopIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.example.budgiet.R
import com.example.budgiet.RecentItems
import com.example.budgiet.fromHex
import com.example.budgiet.rgbToHex
import com.example.budgiet.rgbaToHex
import com.example.budgiet.ui.SELECTED_TAG_BORDER_COLOR
import com.example.budgiet.ui.theme.DarkColorScheme
import com.example.budgiet.ui.theme.LightColorScheme
import com.example.budgiet.ui.theme.UserColorPalette
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

val COLOR_WHEEL_DIAMETER = 250.dp
val RING_THICKNESS = 28.dp
val RING_BORDER_THICKNESS = 3.dp
val RING_BORDER_COLOR = Color.White
/** Size of the gap between the Hue color ring and the SV color circle. */
val RING_AND_INNER_GAP = 6.dp
val HUE_CURSOR_BALL_SIZE = RING_THICKNESS
val SV_CURSOR_BALL_SIZE = HUE_CURSOR_BALL_SIZE * 0.75f
val COLOR_PALETTE_ITEM_SIZE = 32.dp

/** Rotate the color ring 90 degrees so that red starts at the top. */
const val COLOR_RING_ROTATION = 130.0

/** just why bro... */
var parentDialogOffset by mutableStateOf(IntOffset(0, 0))

/** A [Button][IconButton] that opens a [DropdownMenu] with a *color palette* the user can choose from.
 *
 * Additionally, the [DropdownMenu] has a button that opens a full **color picker** with an *HSV* color wheel.
 * The menu also displays *recent* colors that were chosen from the full *HSV* color picker. */
@Composable
fun ColorPickerButton(
    modifier: Modifier = Modifier,
    color: Color,
    onColorChange: (Color) -> Unit,
) {
    var showPaletteMenu by rememberSaveable { mutableStateOf(false) }
    var showColorPickerDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Box(contentAlignment = Alignment.CenterEnd) {
        PlainToolTipBox("Change tag color") {
            IconButton(
                modifier = modifier
                    .semantics { this.stateDescription = color.rgbToHex() },
                onClick = { showPaletteMenu = true },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = color,
                    contentColor = correctContentContrast(color),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceDim,
                )
            ) {
                Icon(painterResource(R.drawable.colors_24px), null)
            }
        }

        DropdownMenu(
            modifier = Modifier.hideDropdownMenuPadding()
                .semantics { contentDescription = "Color menu" },
            properties = POPUP_PROPERTIES,
            shape = MaterialTheme.shapes.large,
            offset = DpOffset(
                x = when (LocalLayoutDirection.current) {
                    // I don't know why but the menu seems to multiply the offset value, throwing it way off.
                    LayoutDirection.Ltr -> -(22.5.dp) * UserColorPalette.size / 2
                    LayoutDirection.Rtl -> 0.dp
                },
                y = 2.dp
            ),
            expanded = showPaletteMenu,
            onDismissRequest = { showPaletteMenu = false },
        ) {
            val selectedColor = color
            val spaceBetween = 6.dp
            val itemShape = RoundedCornerShape(percent = 50)

            @Composable
            fun Modifier.itemModifier(color: Color, isSelectable: Boolean): Modifier = Modifier
                .size(COLOR_PALETTE_ITEM_SIZE)
                .run {
                    if (isSelectable && color == selectedColor) {
                        shadow(5.dp, shape = itemShape)
                    } else this
                }
                .clip(itemShape)
                .background(color)
                .border(
                    width = if (isSelectable && color == selectedColor) 3.dp else 1.dp,
                    shape = itemShape,
                    color = if (isSelectable && color == selectedColor) SELECTED_TAG_BORDER_COLOR else MaterialTheme.colorScheme.outline,
                )
                .then(this)

            @Composable
            fun ColorItem(
                color: Color,
                modifier: Modifier = Modifier,
                /** By default, the item will trigger an `onColorChange` and hide the menu when clicked.
                 * This argument allows running an additional action when the item is clicked.
                 */
                extraOnClick: (() -> Unit)? = null,
            ) {
                val colorStr = "#${color.rgbToHex()}"
                PlainToolTipBox(
                    text = colorStr,
                    setContentDescription = false,
                    dialogPosition = parentDialogOffset,
                ) {
                    Box(modifier
                        .semantics { this.stateDescription = colorStr }
                        .itemModifier(color, true)
                        .clickable {
                            onColorChange(color)
                            showPaletteMenu = false
                            extraOnClick?.invoke()
                        }
                    )
                }
            }

            /* Selectable colors is divided into 3 sections (lines):
             *   1. The *first* half of the [UserColorPalette].
             *   2. The *second* half of the [UserColorPalette].
             *   2. Recent colors selected by the user on the [ColorPickerDialog].
            */
            Column(
                modifier = Modifier.padding(DROPDOWN_MENU_VERTICAL_PADDING),
                verticalArrangement = Arrangement.spacedBy(spaceBetween)
            ) {
                val idxSplit = ceil(UserColorPalette.size.toFloat() / 2f).toInt()

                Row(horizontalArrangement = Arrangement.spacedBy(spaceBetween)) {
                    UserColorPalette
                        .subList(0, idxSplit)
                        .forEach { ColorItem(it) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(spaceBetween)) {
                    UserColorPalette
                        .subList(idxSplit, UserColorPalette.size)
                        .forEach { ColorItem(it) }
                }
                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(spaceBetween)) {
                    RecentItems.Color.items().value
                        ?.getOkOrNull()
                        ?.forEach { ColorItem(it, extraOnClick = {
                            RecentItems.Color.moveToFront(it, context)
                        }) }

                    PlainToolTipBox("Add new color", dialogPosition = parentDialogOffset) {
                        Box(
                            modifier = Modifier
                                .itemModifier(MaterialTheme.colorScheme.surfaceDim, false)
                                .clickable { showColorPickerDialog = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.add_24px), null)
                        }
                    }
                }
            }
        }
    }

    if (showColorPickerDialog) {
        @Suppress("AssignedValueIsNeverRead")
        ColorPickerDialog(
            allowAlpha = false,
            initialColor = color,
            onSubmit = { color ->
                onColorChange(color)
                // Don't add color to recents if it exists in the UserColorPalette.
                if (color !in UserColorPalette) {
                    RecentItems.Color.moveToFront(color, context)
                }
            },
            onDismiss = {
                showPaletteMenu = false
                showColorPickerDialog = false
            },
        )
    }
}

/** Displays a [Dialog][ActionDialog] a **Color Picker** with an *HSV* color wheel and a [TextField] with the *Hexadecimal RGB* color value.
 *
 * @param title The title text that is displayed at the top of the [Dialog][ActionDialog].
 * @param allowAlpha Whether the color can have **transparency** or is fully opaque. */
@Composable
fun ColorPickerDialog(
    modifier: Modifier = Modifier,
    title: String = "Choose a color",
    allowAlpha: Boolean = true,
    initialColor: Color = Color.Red,
    onSubmit: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    fun colorHex(color: Color)
        = if (allowAlpha) color.rgbaToHex() else color.rgbToHex()

    val colorCursorsState = HsvCursorsState.remember(initialColor)
    var alpha by remember(initialColor, allowAlpha) { mutableFloatStateOf(if (allowAlpha) initialColor.alpha else 1f) }
    var textField by remember(initialColor) { mutableStateOf(colorHex(initialColor)) }
    var textFieldError by remember(initialColor) { mutableStateOf<String?>(null) }

    val color = colorCursorsState.currentColor.copy(alpha = alpha)

    val itemPadding = 8.dp

    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        title = { Text(title) },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            FilledTextIconButton(
                colors = ButtonDefaults.buttonColors(
                    containerColor = color,
                    contentColor = correctContentContrast(color),
                ),
                border = BorderStroke(Dp.Hairline, MaterialTheme.colorScheme.outline),
                onClick = {
                    onSubmit(color)
                    onDismiss()
                },
                icon = { Icon(painterResource(R.drawable.check_24px), "Submit") },
                text = { Text("Submit") },
            )
        },
    ) {
        HsvColorWheel(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            state = colorCursorsState,
            onColorChange = {
                textField = colorHex(it.copy(alpha = alpha))
                textFieldError = null
            }
        )
        Spacer(Modifier.height(10.dp))

        if (allowAlpha) {
            // TODO: layout vertically if the Dialog has more horizontal space than vertical (i.e. landscape mode).
            Row(
                modifier = Modifier.padding(horizontal = itemPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val sliderThumbSize = DpSize(6.dp, 32.dp)
                val interactionSource = remember { MutableInteractionSource() }

                Text("A")
                Slider(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = itemPadding),
                    value = alpha,
                    valueRange = 0f..1f,
                    track = { state ->
                        SliderDefaults.Track(state,
                            colors = SliderDefaults.colors(
                                activeTrackColor = color,
                                inactiveTrackColor = MaterialTheme.colorScheme.primaryFixed,
                            ),
                            drawStopIndicator = {
                                this.drawStopIndicator(
                                    offset = it,
                                    color = color.copy(alpha = 1f),
                                    size = TrackStopIndicatorSize * 2,
                                )
                            }
                        )
                    },
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = interactionSource,
                            thumbSize = sliderThumbSize,
                        )
                    },
                    onValueChange = {
                        alpha = it
                        // Don't disturb text field if it has an error.
                        if (textFieldError == null) {
                            textField = colorHex(color.copy(alpha = it))
                        }
                    },
                )
            }
        }

        // HexCode
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("#") },
                value = textField,
                onValueChange = {
                    textField = it
                    try {
                        colorCursorsState.updateWith(Color.fromHex(it, allowAlpha))
                        textFieldError = null
                    } catch (e: IllegalArgumentException) {
                        textFieldError = e.localizedMessage
                    }
                },
                isError = textFieldError != null,
                supportingText = textFieldError?.let { error -> {
                    Text(error)
                } },
                maxLines = 1,
                singleLine = true,
            )
        }
    }
}

@Composable
private fun HsvColorWheel(
    modifier: Modifier = Modifier,
    state: HsvCursorsState,
    onColorChange: (Color) -> Unit,
) {
    val shadowElevation = 5.dp

    // Circle box
    Box(modifier.size(COLOR_WHEEL_DIAMETER)) {
        // Hue Color Ring
        Canvas(Modifier
            .shadow(shadowElevation, CircleShape)
            .fillMaxSize()
            .rotate(-COLOR_RING_ROTATION.toFloat())
            .scale(1f, -1f)
        ) {
            val ringStrokeWidth = RING_THICKNESS.toPx()
            val ringBorderStrokeWidth = RING_BORDER_THICKNESS.toPx()
            /** The number of colors that appear in the Angular Gradient of the Hue color ring. */
            val colorVariety = 8

            // Ring
            this.drawCircle(
                brush = Brush.sweepGradient(List(colorVariety) { i ->
                    Color.hsv(360f * i.toFloat() / (colorVariety - 1), 1f, 1f)
                }),
                radius = (this.size.minDimension - ringStrokeWidth) / 2,
                style = Stroke(ringStrokeWidth),
            )
            // Outer Border
            this.drawCircle(
                color = RING_BORDER_COLOR,
                radius = (this.size.minDimension - ringBorderStrokeWidth) / 2,
                style = Stroke(ringBorderStrokeWidth),
            )
            // Inner Border
            this.drawCircle(
                color = RING_BORDER_COLOR,
                radius = (this.size.minDimension) / 2 - ringStrokeWidth,
                style = Stroke(ringBorderStrokeWidth),
            )
        }

        // Saturation & Value (Brightness) Color Wheel
        Canvas(Modifier.fillMaxSize()) {
            /** Radius of the Saturation & Brightness color circle. */
            val radius = this.size.minDimension / 2 - RING_THICKNESS.toPx() - RING_AND_INNER_GAP.toPx()
            val borderStrokeWidth = 2f

            val gradientRadius = radius - SV_CURSOR_BALL_SIZE.toPx()

            // Inner circle (displays saturation and brightness)
            this.drawCircle(
                color = Color.White,
                radius = radius,
            )
            // SV gradients obtained from [this video](https://www.youtube.com/watch?v=9zXZtHMqHnI).
            this.drawCircle(
                brush = Brush.horizontalGradient(
                    colors = listOf(state.currentHueColor, Color.Transparent),
                    startX = this.center.x + gradientRadius,
                    endX = this.center.x - gradientRadius,
                ).let { brush ->
                    Brush.composite(
                        srcBrush = brush,
                        dstBrush = Brush.verticalGradient(
                            colors = listOf(Color.Black, Color.White),
                            // Don't apply visual gradient offset for Black because it is strong (radius > gradientRadius).
                            startY = this.center.y + radius,
                            endY = this.center.y - gradientRadius,
                        ),
                        blendMode = BlendMode.Multiply,
                    )
                },
                radius = radius,
            )
            // Border
            this.drawCircle(
                color = RING_BORDER_COLOR,
                radius =  radius,
                style = Stroke(borderStrokeWidth),
            )
        }

        @Composable
        fun CursorBall(
            modifier: Modifier = Modifier,
            color: Color,
            diameter: Dp,
            borderWidth: Dp,
            offset: Density.() -> IntOffset,
            onDrag: PointerInputScope.(Offset) -> Unit,
            onDragEnd: (PointerInputScope.() -> Unit)? = null,
        ) {
            Box(modifier
                .offset(offset)
                .shadow(shadowElevation, CircleShape)
                .clip(CircleShape)
                .background(color)
                .border(borderWidth, RING_BORDER_COLOR, CircleShape)
                .size(diameter)
                .pointerInput(Unit) {
                    this.detectDragGestures(
                        onDragEnd = { onDragEnd?.invoke(this) }
                    ) { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                }
            )
        }

        // Hue cursor
        CursorBall(
            color = state.currentHueColor,
            diameter = HUE_CURSOR_BALL_SIZE + 1.dp,
            borderWidth = RING_BORDER_THICKNESS,
            offset = { state.hueCursor.offset.round() },
            onDrag = { dragAmount ->
                state.hueCursor.drag(dragAmount)
                onColorChange(state.currentColor)
            },
            onDragEnd = { state.hueCursor.endDrag() },
        )
        // Saturation & Brightness cursor
        CursorBall(
            color = state.currentColor,
            diameter = SV_CURSOR_BALL_SIZE,
            borderWidth = RING_BORDER_THICKNESS * (SV_CURSOR_BALL_SIZE / HUE_CURSOR_BALL_SIZE),
            offset = { state.svCursor.offset.round() },
            onDrag = { dragAmount ->
                // Note: Don't apply bounds to the actual offset value here.
                state.svCursor.drag(dragAmount)
                onColorChange(state.currentColor)
            },
            onDragEnd = { state.svCursor.endDrag() },
        )
    }
}

@Composable
fun correctContentContrast(background: Color): Color
    // Fix contrast with icon color and background color if needed.
    = if (background.alpha < 0.35) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.contentColorFor(background)
            .takeOrElse {
                if (background.luminance() < 0.5f) {
                    DarkColorScheme.onPrimaryContainer
                } else {
                    LightColorScheme.onPrimaryContainer
                }
            }
    }

private class HsvCursorsState private constructor(initialColor: Color, private val cursorsData: CursorsData) {
    val hueCursor = Cursor { this.cursorsData.boundedHueCursorOffset(it) }
    val svCursor = Cursor { this.cursorsData.boundedSvCursorOffset(it) }

    init {
        this.updateWith(initialColor)
    }

    val currentColor get() = this.cursorsData.createColor(this.hueCursor.offset, this.svCursor.offset)
    val currentHueColor get() = Color.hsv(this.cursorsData.hueCursorOffsetToDegrees(this.hueCursor.offset).toFloat(), 1f, 1f)

    /** Modifies the values of **`hueCursorOffset`** and **`svCursorOffset`** with the *HSV* values from the new **`color`**. */
    fun updateWith(color: Color) {
        val (hue, saturation, value) = run {
            val values = FloatArray(3)
            android.graphics.Color.colorToHSV(color.toArgb(), values)
            values
        }

        // Don't change the Hue cursor position if it doesn't need to be changed.
        if (!(color.red == color.green && color.green == color.blue)) {
            this.hueCursor.offset = cursorsData.degreesToHueCursorOffset(Degrees(hue))
        }
        this.svCursor.offset = cursorsData.colorToSvCursorOffset(saturation, value)
    }

    companion object {
        /** Create an instance of [HsvCursorsState] that is remembered by a composable. */
        @Composable
        fun remember(initialColor: Color = Color.Red): HsvCursorsState = LocalDensity.current.let { density ->
            val cursorData = remember(density) { CursorsData(density) }
            remember(initialColor, cursorData) { HsvCursorsState(initialColor, cursorData) }
        }
    }
}

class Cursor internal constructor(
    private val bounds: (Offset) -> Offset,
) {
    /* Note that these Offset values are not always clamped to the SV's bounds.
     * This is to allow the user to drag outside of the SV circle and keeping the cursor *visibly* within its bounds. */
    private val _offset = mutableStateOf(Offset(0f, 0f))

    var offset: Offset
        get() = this.bounds(this._offset.value)
        set(value) { this._offset.value = this.bounds(value) }

    /** Note: Don't apply bounds to the actual offset value here. */
    internal fun drag(dragAmount: Offset) {
        this._offset.value += dragAmount
    }
    /** When user releases, apply bounds to the actual offset value. */
    internal fun endDrag() {
        this._offset.value = this.bounds(this._offset.value)
    }
}

/** This class stores all the constant values with the applied [Density] so that they don't have to be recomputed on each recomposition.
 *
 * It also adds a nice structure to this part of the program :D */
internal class CursorsData(
    density: Density,
) {
    val hueCursorBounds = with(density) {
        val cursorRadius = HUE_CURSOR_BALL_SIZE.toPx() / 2f
        val radius = COLOR_WHEEL_DIAMETER.toPx() / 2f - cursorRadius
        Rect(
            center = Offset(radius, radius),
            radius = radius,
        )
    }

    val svCursorRadius = with(density) { SV_CURSOR_BALL_SIZE.toPx() / 2f }
    val svCursorBounds = with(density) {
        val offsetAmount = RING_THICKNESS.toPx() + RING_AND_INNER_GAP.toPx()
        val radius =  COLOR_WHEEL_DIAMETER.toPx() / 2f - offsetAmount - svCursorRadius
        Rect(
            center = Offset(offsetAmount + radius, offsetAmount + radius),
            radius = radius,
        )
    }
    /** Since the HSV selector area is a circle, the actual area of the S&L selector is a square within the circle and is smaller. */
    val svSelectionBounds = Rect(
        top = svCursorBounds.top + svCursorRadius * 2.3f,
        left = svCursorBounds.left + svCursorRadius * 2.3f,
        bottom = svCursorBounds.bottom,
        right = svCursorBounds.right - svCursorRadius * 2.25f,
    )

    /** Get the position of the **hueCursor** from the [Color]'s **Hue** value.
     *
     * > Note that `hue == hueCursorOffsetToDegrees(degreesToHueCursorOffset(color))`.
     * > Or at least it's supposed to. */
    fun degreesToHueCursorOffset(hue: Degrees): Offset {
        val degrees = hue + COLOR_RING_ROTATION
        return Offset(
            x = hueCursorBounds.width / 2f + cos(degrees).toFloat() * hueCursorBounds.width / 2f,
            y = hueCursorBounds.height / 2f - sin(degrees).toFloat() * hueCursorBounds.height / 2f,
        )
    }
    /** Returns the **Hue** value that corresponds to the **`hueCursor`**'s position. */
    fun hueCursorOffsetToDegrees(hueCursorOffset: Offset): Degrees {
        /** Translate the Offset so that the origin is at the center of the circle. */
        val offsetNormalized = Offset(
            x = hueCursorOffset.x - hueCursorBounds.width / 2f,
            y = (hueCursorOffset.y - hueCursorBounds.height / 2f) * -1,
        )

        return Degrees.fromRadians(atan2(offsetNormalized.y, offsetNormalized.x)) - COLOR_RING_ROTATION
    }
    /** Clamps the **`hueCursorOffset`** so that it stays within the bounds of the **Hue** Color Ring. */
    fun boundedHueCursorOffset(hueCursorOffset: Offset): Offset {
        val distance = sqrt((hueCursorOffset.x - hueCursorBounds.center.x).pow(2f) + (hueCursorOffset.y - hueCursorBounds.center.y).pow(2f))

        return Offset(
            x = (hueCursorOffset.x - hueCursorBounds.center.x) * hueCursorBounds.width / 2f / distance + hueCursorBounds.center.x,
            y = (hueCursorOffset.y - hueCursorBounds.center.y) * hueCursorBounds.height / 2f / distance + hueCursorBounds.center.y,
        )
    }

    /** Get the position of the **svCursor** from the [Color]'s **`saturation`** and **`brightness`** values.
     *
     * > Note that `color == svCursorOffsetToColor(colorToSvCursorOffset(color))`.
     * > Or at least it's supposed to. */
    fun colorToSvCursorOffset(saturation: Float, brightness: Float) = Offset(
        // Values are snapped to the edges of the selection circle area.
        x = when {
            saturation <= 0f -> svCursorBounds.left
            saturation >= 1f -> svCursorBounds.right
            else -> saturation * svSelectionBounds.width + svSelectionBounds.left
        },
        y = when {
            brightness <= 0f -> svCursorBounds.bottom
            brightness >= 1f -> svCursorBounds.top
            else -> flipValue(brightness) * svSelectionBounds.height + svSelectionBounds.top
        },
    )
    /** Returns the respective **`saturation`** and **`brightness`** values that correspond to the **`svCursor`**'s position.
     *
     * > Note that `offset == colorToSvCursorOffset(svCursorOffsetToColor(offset))`.
     * > Or at least it's supposed to.*/
    fun svCursorOffsetToColor(svCursorOffset: Offset) = Pair(
        first = when {
            svCursorOffset.x < svSelectionBounds.left -> 0f
            svCursorOffset.x > svSelectionBounds.right -> 1f
            else -> (svCursorOffset.x - svSelectionBounds.left) / svSelectionBounds.width
        },
        second = when {
            svCursorOffset.y < svSelectionBounds.top -> 1f
            svCursorOffset.y > svSelectionBounds.bottom -> 0f
            else -> flipValue((svCursorOffset.y - svSelectionBounds.top) / svSelectionBounds.height)
        },
    )
    /** Clamps the **`svCursorOffset`** so that it stays within the bounds of the *Saturation & Brightness* color circle. */
    fun boundedSvCursorOffset(svCursorOffset: Offset): Offset {
        val distance = sqrt((svCursorOffset.x - svCursorBounds.center.x).pow(2f) + (svCursorOffset.y - svCursorBounds.center.y).pow(2f))

        return if (distance > svCursorBounds.minDimension / 2f) {
            Offset(
                x = (svCursorOffset.x - svCursorBounds.center.x) * svCursorBounds.width / 2f / distance + svCursorBounds.center.x,
                y = (svCursorOffset.y - svCursorBounds.center.y) * svCursorBounds.height / 2f / distance + svCursorBounds.center.y,
            )
        } else {
            svCursorOffset
        }
    }

    /** Create a new [Color] based on the positions of the *Hue* and *Saturation & Brightness* **cursors**. */
    fun createColor(hueCursorOffset: Offset, svCursorOffset: Offset): Color {
        val hue = hueCursorOffsetToDegrees(hueCursorOffset).toFloat()
        val (saturation, brightness) = svCursorOffsetToColor(svCursorOffset)

        return Color.hsv(hue, saturation, brightness)
    }

    // Makes a value that is in range of 0 - 1 become in range of 1 - 0 instead.
    private fun flipValue(value: Float): Float = value * -1 + 1
}

@Suppress("unused")
@JvmInline
internal value class Degrees private constructor(private val degrees: Double) {
//    constructor(degrees: Double): this(normalize(degrees))
    constructor(degrees: Float): this(normalize(degrees.toDouble()))
    constructor(degrees: Int): this(normalize(degrees.toDouble()))

    val radians get() = Math.toRadians(this.degrees)

    operator fun plus(other: Degrees) = Degrees(normalize(this.degrees + other.degrees))
    operator fun plus(other: Double) = Degrees(normalize(this.degrees + other))
    operator fun plus(other: Float) = Degrees(normalize(this.degrees + other.toDouble()))

    operator fun minus(other: Degrees) = Degrees(normalize(this.degrees - other.degrees))
    operator fun minus(other: Double) = Degrees(normalize(this.degrees - other))
    operator fun minus(other: Float) = Degrees(normalize(this.degrees - other.toDouble()))

    operator fun times(other: Degrees) = Degrees(normalize(this.degrees * other.degrees))
    operator fun times(other: Double) = Degrees(normalize(this.degrees * other))
    operator fun times(other: Float) = Degrees(normalize(this.degrees * other.toDouble()))

    operator fun div(other: Degrees) = Degrees(normalize(this.degrees / other.degrees))
    operator fun div(other: Double) = Degrees(normalize(this.degrees / other))
    operator fun div(other: Float) = Degrees(normalize(this.degrees / other.toDouble()))

    fun toDouble() = this.degrees
    fun toFloat() = this.degrees.toFloat()
    fun toInt() = this.degrees.toInt()
    fun toUInt() = this.degrees.toUInt()

    companion object {
        fun fromRadians(radians: Double) = Degrees(normalize(Math.toDegrees(radians)))
        fun fromRadians(radians: Float) = fromRadians(radians.toDouble())

        /** Keep the degrees value between 0 - 360 */
        private fun normalize(degrees: Double): Double
            = if (degrees < 0.0) {
                360.0 - (degrees * -1 % 360.0)
            } else if (degrees > 360.0) {
                degrees % 360.0
            } else {
                degrees
            }
    }
}

private fun cos(degrees: Degrees) = cos(degrees.radians)
private fun sin(degrees: Degrees) = sin(degrees.radians)
