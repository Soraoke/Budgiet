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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
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
import kotlin.math.min
import kotlin.math.sin

val RING_DIAMETER = 250.dp
val RING_THICKNESS = 28.dp
val RING_BORDER_THICKNESS = 3.dp
val RING_BORDER_COLOR = Color.White
val HUE_CURSOR_BALL_SIZE = RING_THICKNESS
val COLOR_PALETTE_ITEM_SIZE = 32.dp
const val MAX_USER_COLOR_ITEMS = 5

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
        ColorRing(
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
private fun ColorRing(
    modifier: Modifier = Modifier,
    color: Color,
    onColorChange: (Color) -> Unit,
) {
    val cursorShape = RoundedCornerShape(percent = 50)
    val shadowElevation = 5.dp

    // Circle box
    Box(modifier
        .size(RING_DIAMETER)
        .rotate(-40f)
    ) {
        // Color Ring (HUE)
        Canvas(Modifier
            .shadow(shadowElevation, CircleShape)
            .fillMaxSize()
            .rotate(-90f)
            .scale(1f, -1f)
        ) {
            val ringStrokeWidth = RING_THICKNESS.toPx()
            val borderStrokeWidth = RING_BORDER_THICKNESS.toPx()
            val colorVariety = 8

            // Ring
            this.drawCircle(
                brush = Brush.sweepGradient(List(colorVariety) { i ->
                    Color.hsl(360f * i.toFloat() / (colorVariety - 1), 1f, 0.5f)
                }),
                radius = (this.size.minDimension - ringStrokeWidth) / 2,
                style = Stroke(width = ringStrokeWidth),
            )
            // Outer Border
            this.drawCircle(
                color = RING_BORDER_COLOR,
                radius = (this.size.minDimension - borderStrokeWidth) / 2,
                style = Stroke(borderStrokeWidth),
            )
            // Inner Border
            this.drawCircle(
                color = RING_BORDER_COLOR,
                radius = (this.size.minDimension) / 2 - ringStrokeWidth,
                style = Stroke(borderStrokeWidth),
            )
        }

        // HUE cursor (ball)
        /** The position of the HUE cursor in terms of the angle (in degrees) along the Color Ring. */
        @Suppress("LocalVariableName")
        val _degrees = colorToDegrees(color) ?: 0.0
        val positionDegrees = remember(_degrees) { _degrees } /* Ball starts at red (at the top). */

        val offset = with(LocalDensity.current) {
            degreesToOffset(positionDegrees, HUE_CURSOR_BALL_SIZE, RING_DIAMETER)
        }

        Column {
            Text("(x = ${offset.x}, y = ${offset.y})")
            Text("degrees = $positionDegrees")
        }
        Box(Modifier
            .offset { offset.round() }
            .shadow(shadowElevation, CircleShape)
            .clip(cursorShape)
            .background(Color.hsl(normalizeDegrees(positionDegrees - 90).toFloat(), 1f, 0.5f))
            .border(RING_BORDER_THICKNESS, RING_BORDER_COLOR, cursorShape)
            .size(HUE_CURSOR_BALL_SIZE + 1.dp)
            .pointerInput(Unit) {
                this.detectDragGestures { change, dragAmount ->
                    change.consume()
                    val change = offsetToDegrees(offset + dragAmount, HUE_CURSOR_BALL_SIZE, RING_DIAMETER)

                    onColorChange(degreesToColor(positionDegrees + change))
                }
            }
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

/** Converts the **[Offset]** value of an **`object`** in a **`plane`** / surface to an angle (in *degrees*).
 *
 * The **[Offset]** is a *float pair* (x, y) relative to the Top-Left corner of the **`plane`**.
 *
 * The **`object`** and **`plane`** must have equal *width and height* dimensions. */
private fun Density.offsetToDegrees(offset: Offset, objectSize: Dp, planeSize: Dp): Double {
//    println("objectSize = ${objectSize.toPx()}")
//    println("planeSize = ${planeSize.toPx()}")
    println("offset (x = ${offset.x}, y = ${offset.y})")
    // Translate the object's offset to a plane with the origin at the center.
    val translation = - (planeSize.toPx() - objectSize.toPx()) / 2
    val centered = Offset(
        x = offset.x + translation,
        y = (offset.y - translation),
    )
    println("centered (x = ${centered.x}, y = ${centered.y})")
    val theta = atan2(centered.y, centered.x)
    val degrees = Math.toDegrees(theta.toDouble()) - 90
    TODO()

    return degrees
}

private fun Density.degreesToOffset(degrees: Double, objectSize: Dp, planeSize: Dp): Offset {
    val center = (planeSize.toPx() - objectSize.toPx()) / 2
    val degrees = degrees
    return Offset(
        x = center + cos(Math.toRadians(degrees).toFloat()) * center,
        y = center - sin(Math.toRadians(degrees).toFloat()) * center,
    )
}

/** Get the HUE of a color.
 * Returns `null` if the Floating point math failed.
 *
 * Formula obtained from [this article](https://www.niwa.nu/2013/05/math-behind-colorspace-conversions-rgb-hsl/). */
private fun colorToDegrees(color: Color): Double? {
    val (red, green, blue) = color
    val min = min(red, min(green, blue))

    val degrees = try {
        when {
            // Red is max
            red > green && red > blue -> {
                (green - blue) / (red - min)
            }
            // Green is max
            green > red && green > blue -> {
                2f + (blue - red) / (green - min)
            }
            // Blue is max
            else -> {
                4f + (red - green) / (blue - min)
            }
        } * 60f + 90f
    } catch(_: Throwable) { Float.NaN }

    return if (degrees.isNaN() || degrees.isInfinite()) {
        null
    } else {
        normalizeDegrees(degrees.toDouble())
    }
}
private fun degreesToColor(degrees: Double): Color {
    val degrees = degrees - 90.0 // Apply self-imposed angle offset.
    val normalized = if (degrees < 0) {
        360.0 + degrees % 360.0
    } else {
        degrees % 360.0
    }
    return Color.hsl(normalized.toFloat(), 1f, 0.5f)
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
