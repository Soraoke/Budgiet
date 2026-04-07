@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.budgiet.ui.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.core.graphics.ColorUtils
import com.example.budgiet.R
import com.example.budgiet.RecentItems
import com.example.budgiet.fromHex
import com.example.budgiet.rgbToHex
import com.example.budgiet.rgbaToHex
import com.example.budgiet.ui.SELECTED_TAG_BORDER_COLOR
import com.example.budgiet.ui.theme.ColorPalette
import com.example.budgiet.ui.theme.DarkColorScheme
import com.example.budgiet.ui.theme.LightColorScheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

val COLOR_WHEEL_DIAMETER = 250.dp
val RING_THICKNESS = 28.dp
val RING_BORDER_THICKNESS = 3.dp
val RING_BORDER_COLOR = Color.White
/** Size of the gap between the HUE color ring and the SL color circle. */
val RING_AND_INNER_GAP = 6.dp
val HUE_CURSOR_BALL_SIZE = RING_THICKNESS
val SL_CURSOR_BALL_SIZE = HUE_CURSOR_BALL_SIZE * 0.75f
val COLOR_PALETTE_ITEM_SIZE = 32.dp
const val MAX_USER_COLOR_ITEMS = 5

/** Rotate the color ring 90 degrees so that red starts at the top. */
const val COLOR_RING_ROTATION = 130.0

/** just why bro... */
var parentDialogOffset by mutableStateOf(IntOffset(0, 0))

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
                modifier = modifier,
                onClick = { showPaletteMenu = true },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = color,
                    contentColor = correctContentContrast(color),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceDim,
                )
            ) {
                Icon(painterResource(R.drawable.colors_24px), "Change tag color")
            }
        }

        DropdownMenu(
            modifier = Modifier
                .padding(horizontal = DROPDOWN_MENU_VERTICAL_PADDING),
            properties = POPUP_PROPERTIES,
            shape = MaterialTheme.shapes.large,
            offset = DpOffset(
                x = when (LocalLayoutDirection.current) {
                    // I don't know why but the menu seems to multiply the offset value, throwing it way off.
                    LayoutDirection.Ltr -> -(22.5.dp) * ColorPalette.size
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
                .clip(itemShape)
                .apply {
                    if (isSelectable && color == selectedColor) {
                        // FIXME: doesn't show
                        shadow(50.dp, shape = itemShape)
                    }
                }
                .background(color)
                .border(
                    width = if (isSelectable && color == selectedColor) 3.dp else 1.dp,
                    shape = itemShape,
                    color = if (isSelectable && color == selectedColor) SELECTED_TAG_BORDER_COLOR else MaterialTheme.colorScheme.outline,
                )
                .then(this)

            @Composable
            fun ColorItem(color: Color, modifier: Modifier = Modifier) {
                PlainToolTipBox(
                    text = "#${color.rgbToHex()}",
                    dialogPosition = parentDialogOffset,
                ) {
                    Box(modifier.itemModifier(color, true))
                }
            }

            Column(verticalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    ColorPalette.forEachIndexed { i, color ->
                        if (i != 0) {
                            Spacer(Modifier.width(spaceBetween))
                        }
                        ColorItem(
                            color = color,
                            modifier = Modifier.clickable {
                                onColorChange(color)
                                showPaletteMenu = false
                            }
                        )
                    }
                }
                Spacer(Modifier.height(spaceBetween))
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    RecentItems.Color.items().value
                        ?.getOkOrNull()
                        ?.forEach { color ->
                            ColorItem(
                                color = color,
                                modifier = Modifier.clickable {
                                    onColorChange(color)
                                    showPaletteMenu = false
                                    RecentItems.Color.moveToFront(color, context)
                                },
                            )
                            Spacer(Modifier.width(spaceBetween))
                        }

                    PlainToolTipBox("Add new color", dialogPosition = parentDialogOffset) {
                        Box(
                            modifier = Modifier
                                .itemModifier(MaterialTheme.colorScheme.surfaceDim, false)
                                .clickable { showColorPickerDialog = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.add_24px), "Add new color")
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
                if (color !in ColorPalette) {
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

// TODO: doc
@Composable
fun ColorPickerDialog(
    modifier: Modifier = Modifier,
    title: String = "Choose a color",
    allowAlpha: Boolean = true,
    initialColor: Color = Color.Red,
    onSubmit: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var color by remember { mutableStateOf(initialColor) }
    var textField by remember { mutableStateOf(if (allowAlpha) color.rgbaToHex() else color.rgbToHex()) }
    var textFieldError by remember { mutableStateOf<String?>(null) }

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
        ColorWheel(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = color,
            onColorChange = {
                color = it
                textField = if (allowAlpha) it.rgbaToHex() else it.rgbToHex()
                if (textFieldError != null) {
                    textFieldError = null
                }
            },
        )
        Spacer(Modifier.height(10.dp))

        // HexCode
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                prefix = { Text("#") },
                value = textField,
                onValueChange = {
                    textField = it
                    try {
                        color = Color.fromHex(it, allowAlpha)
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
private fun ColorWheel(
    modifier: Modifier = Modifier,
    color: Color,
    onColorChange: (Color) -> Unit,
) {
    val shadowElevation = 5.dp
    val (hue, saturation, lightness) = remember(color) {
        val values = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), values)
        values
    }
    /** The color but with the saturation and lightness reset. */
    val hueColor = Color.hsl(hue, 1f, 0.5f)

    // Circle box
    Box(modifier.size(COLOR_WHEEL_DIAMETER)) {
        // HUE Color Ring
        Canvas(Modifier
            .shadow(shadowElevation, CircleShape)
            .fillMaxSize()
            .rotate(-COLOR_RING_ROTATION.toFloat())
            .scale(1f, -1f)
        ) {
            val ringStrokeWidth = RING_THICKNESS.toPx()
            val ringBorderStrokeWidth = RING_BORDER_THICKNESS.toPx()
            /** The number of colors that appear in the Angular Gradient of the HUE color ring. */
            val colorVariety = 8

            // Ring
            this.drawCircle(
                brush = Brush.sweepGradient(List(colorVariety) { i ->
                    Color.hsl(360f * i.toFloat() / (colorVariety - 1), 1f, 0.5f)
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

        // Saturation & Lightness Color Wheel
        Canvas(Modifier.fillMaxSize()) {
            /** Radius of the Saturation & Lightness color circle. */
            val radius = this.size.minDimension / 2 - RING_THICKNESS.toPx() - RING_AND_INNER_GAP.toPx()
            val borderStrokeWidth = 2f

            // Inner circle (displays saturation and lightness)
            this.drawCircle(
                color = Color.White,
                radius = radius,
            )
            // SL gradients obtained from [this video](https://www.youtube.com/watch?v=9zXZtHMqHnI).
            this.drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(hueColor, Color.Transparent),
                    start = this.center + Offset(radius * 0.75f, 0f),
                    end = this.center + Offset(-radius * 0.75f, 0f),
                ).let { brush ->
                    Brush.composite(
                        srcBrush = brush,
                        dstBrush = Brush.linearGradient(
                            colors = listOf(Color.Black, Color.White),
                            start = this.center + Offset(0f, radius),
                            end = this.center + Offset(0f, -radius * 0.75f),
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

        val density = with(LocalDensity.current) { this }
        /* Note that these Offset values are not always clamped to the SL's bounds.
         * This is to allow the user to drag outside of the SL circle and keeping the cursor *visibly* within its bounds. */
        var hueCursorOffset by remember { mutableStateOf(density.degreesToHueCursorOffset(hue.toDouble())) }
        var slCursorOffset by remember { mutableStateOf(density.colorToSlCursorOffset(saturation, lightness)) }

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

        // HUE cursor
        CursorBall(
            color = hueColor,
            diameter = HUE_CURSOR_BALL_SIZE + 1.dp,
            borderWidth = RING_BORDER_THICKNESS,
            offset = { hueCursorBounds(hueCursorOffset).round() },
            onDrag = { dragAmount ->
                // Note: Don't apply bounds to the actual offset value here.
                hueCursorOffset += dragAmount
                onColorChange(createColor(hueCursorOffset, slCursorOffset))
            },
            onDragEnd = {
                // When user releases, apply bounds to the actual offset value.
                hueCursorOffset = hueCursorBounds(hueCursorOffset)
            },
        )
        // Saturation & Lightness cursor
        CursorBall(
            color = color,
            diameter = SL_CURSOR_BALL_SIZE,
            borderWidth = RING_BORDER_THICKNESS * (SL_CURSOR_BALL_SIZE / HUE_CURSOR_BALL_SIZE),
            offset = { slCursorBounds(slCursorOffset).round() },
            onDrag = { dragAmount ->
                // Note: Don't apply bounds to the actual offset value here.
                slCursorOffset += dragAmount
                onColorChange(createColor(hueCursorOffset, slCursorOffset))
            },
            onDragEnd = {
                // When user releases, apply bounds to the actual offset value.
                slCursorOffset = slCursorBounds(slCursorOffset)
            },
        )
    }
}

@Composable
private fun correctContentContrast(background: Color): Color
    // Fix contrast with icon color and background color if needed.
    = if (background.alpha < 0.35) {
        MaterialTheme.colorScheme.onSurface
    } else if (background.luminance() < 0.35) {
        DarkColorScheme.onPrimaryContainer
    } else if (background.luminance() < 0.70) {
        DarkColorScheme.primary
    } else {
        LightColorScheme.onPrimaryContainer
    }

private fun Density.degreesToHueCursorOffset(degrees: Double): Offset {
    val degrees = degrees + COLOR_RING_ROTATION
    val cursorRadius = HUE_CURSOR_BALL_SIZE.toPx() / 2f
    val boundsRadius = COLOR_WHEEL_DIAMETER.toPx() / 2f - cursorRadius

    return Offset(
        x = boundsRadius + cos(Math.toRadians(degrees).toFloat()) * boundsRadius,
        y = boundsRadius - sin(Math.toRadians(degrees).toFloat()) * boundsRadius,
    )
}
private fun Density.hueCursorOffsetToDegrees(hueCursorOffset: Offset): Double {
    val boundOffset = hueCursorBounds(hueCursorOffset)
    val cursorRadius = HUE_CURSOR_BALL_SIZE.toPx() / 2f
    val boundsRadius = COLOR_WHEEL_DIAMETER.toPx() / 2f - cursorRadius
    /** Translate the Offset so that the origin is at the center of the circle. */
    val offsetNormalized = Offset(
        x = boundOffset.x - boundsRadius,
        y = (boundOffset.y - boundsRadius) * -1,
    )

    val angle = Math.toDegrees(atan2(offsetNormalized.y, offsetNormalized.x).toDouble())
    val degrees = normalizeDegrees(angle)
    return normalizeDegrees(degrees - COLOR_RING_ROTATION)
}
private fun Density.hueCursorBounds(hueCursorOffset: Offset): Offset {
    val cursorRadius = HUE_CURSOR_BALL_SIZE.toPx() / 2f
    val boundsRadius = COLOR_WHEEL_DIAMETER.toPx() / 2f - cursorRadius
    val boundsCenter = Offset(boundsRadius, boundsRadius)

    val distance = sqrt((hueCursorOffset.x - boundsCenter.x).pow(2f) + (hueCursorOffset.y - boundsCenter.y).pow(2f))
    return Offset(
        x = (hueCursorOffset.x - boundsCenter.x) * boundsRadius / distance + boundsCenter.x,
        y = (hueCursorOffset.y - boundsCenter.y) * boundsRadius / distance + boundsCenter.y,
    )
}

/** Keep the degrees value between 0 - 360 */
private fun normalizeDegrees(degrees: Double): Double
    = if (degrees < 0.0) {
        360.0 - (degrees * -1 % 360.0)
    } else if (degrees > 360.0) {
        degrees % 360.0
    } else {
        degrees
    }

/** Get the position of the **slCursor** from the [Color]'s data.
 *
 * All this do is reverse the operation in [createColor]. */
private fun Density.colorToSlCursorOffset(saturation: Float, lightness: Float): Offset {
    val offsetAmount = RING_THICKNESS.toPx() + RING_AND_INNER_GAP.toPx()
    val diameter =  COLOR_WHEEL_DIAMETER.toPx() - offsetAmount * 2f - SL_CURSOR_BALL_SIZE.toPx()

    return slCursorBounds(Offset(
        x = saturation * diameter + offsetAmount,
        y = (-1 * lightness + 1) * diameter + offsetAmount,
    ))
}
/** Clamps the **`slCursorOffset`** so that it stays within the bounds of the *Saturation & Lightness** color circle. */
private fun Density.slCursorBounds(slCursorOffset: Offset): Offset {
    val offsetAmount = RING_THICKNESS.toPx() + RING_AND_INNER_GAP.toPx()
    val cursorRadius = SL_CURSOR_BALL_SIZE.toPx() / 2f
    val boundsRadius =  COLOR_WHEEL_DIAMETER.toPx() / 2f - offsetAmount - cursorRadius
    val boundsCenter = Offset(offsetAmount + boundsRadius, offsetAmount + boundsRadius)

    val distance = sqrt((slCursorOffset.x - boundsCenter.x).pow(2f) + (slCursorOffset.y - boundsCenter.y).pow(2f))
    return if (distance > boundsRadius) {
        Offset(
            x = (slCursorOffset.x - boundsCenter.x) * boundsRadius / distance + boundsCenter.x,
            y = (slCursorOffset.y - boundsCenter.y) * boundsRadius / distance + boundsCenter.y,
        )
    } else {
        slCursorOffset
    }
}

/** Create a new [Color] based on the positions of the *HUE* and *Saturation & Lightness* **cursors**. */
private fun Density.createColor(hueCursorOffset: Offset, slCursorOffset: Offset): Color {
    /** Offset of the *Saturation & Lightness* Cursor normalized to the center of the Color Wheel, in the range of `0.0 - 1.0`. */
    val slNormalized = run {
        val offsetAmount = RING_THICKNESS.toPx() + RING_AND_INNER_GAP.toPx()
        val diameter =  COLOR_WHEEL_DIAMETER.toPx() - offsetAmount * 2f - SL_CURSOR_BALL_SIZE.toPx()
        val boundOffset = slCursorBounds(slCursorOffset)

        Offset(
            x = (boundOffset.x - offsetAmount) / diameter,
            // Reflect y axis.
            // FIXME: color is completely white at the top, when it should be white + red
            y = ((boundOffset.y - offsetAmount) / diameter - 1) * -1,
        )
    }

    return Color.hsl(hueCursorOffsetToDegrees(hueCursorOffset).toFloat(), slNormalized.x, slNormalized.y)
}
