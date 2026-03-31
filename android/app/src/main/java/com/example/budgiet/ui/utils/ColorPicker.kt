@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.budgiet.ui.utils

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import com.example.budgiet.R
import com.example.budgiet.RecentItems
import com.example.budgiet.rgbToHex
import com.example.budgiet.ui.SELECTED_TAG_BORDER_COLOR
import com.example.budgiet.ui.theme.ColorPalette
import com.example.budgiet.ui.theme.DarkColorScheme
import com.example.budgiet.ui.theme.LightColorScheme
import com.example.budgiet.unwrap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

val HUE_CURSOR_BALL_SIZE = 24.dp
val RING_SIZE = 150.dp
val RING_BORDER_THICKNESS = 3.dp
val RING_BORDER_COLOR = Color.White
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
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceDim,
                )
            ) {
                // Fix contrast with icon color and background color if needed.
                val tint = if (color.luminance() < 0.35) {
                    DarkColorScheme.onPrimaryContainer
                } else if (color.luminance() < 0.70) {
                    DarkColorScheme.primary
                } else {
                    LightColorScheme.onPrimaryContainer
                }
                Icon(
                    painter = painterResource(R.drawable.colors_24px),
                    tint = tint,
                    contentDescription = "Change tag color",
                )
            }
        }


        if (showPaletteMenu) {
            Popup(
                properties = POPUP_PROPERTIES,
                alignment = Alignment.BottomEnd,
                offset = IntOffset(x = 4, y = 4),
                onDismissRequest = { showPaletteMenu = false }
            ) {

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

@Composable
fun ColorPickerDialog(
    modifier: Modifier = Modifier,
    title: String = "Choose a color",
    initialColor: Color = Color.Red,
    onSubmit: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var color by remember { mutableStateOf(initialColor) }

    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        title = { Text(title) },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            FilledTextIconButton(
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
            onColorChange = { color = it },
        )
    }
}

@Composable
private fun ColorRing(
    modifier: Modifier = Modifier,
    color: Color,
    onColorChange: (Color) -> Unit,
) {
    val cursorShape = RoundedCornerShape(percent = 50)
    /** The position of the HUE cursor (ball) along the Color Ring. (-1.0 to 1.0) */
    var cursorPosition by remember { mutableFloatStateOf(-1.0f) }

    // Circle box
    Box(modifier.size(RING_SIZE)) {
        // Color Ring (HUE)
        AndroidView(
            modifier = Modifier.size(RING_SIZE),
            factory = { context -> ColorRingGLSurfaceView(context) },
            // update = { },
        )

        // HUE cursor (ball)
        Box(Modifier
            .clip(cursorShape)
            .background(color)
            .border(RING_BORDER_THICKNESS, RING_BORDER_COLOR, cursorShape)
            .size(HUE_CURSOR_BALL_SIZE)
//            .offset {  }
            // TODO: shadow
        )
    }
}

private const val VERTEX_SHADER =
"""
#version 330
layout (location = 0) in vec2 coords;

//uniform vec2 iResolution;

void main() {
    gl_Position = vec4(coords.x, coords.y, 0.0, 0.0);
}
"""

/** The shader that calculates the **color** of pixels.
 *
 * Imagine a Color Ring as if it was a 1-dimensional line (a color strip) containing a range of values
 * from -1.0 to 1.0 (starting at 0.0, and looping back to 0.0 at the end of the line).
 * Each one of these values must map to a *fully-saturated* RGB color.
 * The line can be divided into *3 sections* for each respective RGB channel.
 * Each section *interpolates* from one channel to the other (i.e. Red - Green, Green - Blue, Blue - Red).
 * Each section is then divided into *2 subsections* that interpolates the value of an individual channel from 0 to 1.0.
 * E.g. 1st section has a subsection for Green 0 - 1.0 and another for Red 1.0 - 0.
 *
 * > Note that sum of the 3 channels must be in the range of 1.0 - 2.0 to keep the color fully saturated. */
private const val COLOR_RING_FRAGMENT_SHADER =
"""
#version 330
out vec4 fragColor;

//uniform vec2 iResolution;

void main() {
//    float r = if (coordNormalized.x > 0) 1.0 else 0.5;
//    float b = if (coordNormalized.y > 0) 1.0 else 0.5;
    fragColor = vec4(1.0, 0.0, 0.5, 1.0);
}
"""

/** Code obtained from [dev.to](https://dev.to/den4ic/morphing-geometric-shapes-with-sdf-in-glsl-fragment-shaders-and-visualization-in-jetpack-compose-5db8). */
private class ColorRingGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {
    private val renderer: ColorRingRenderer = ColorRingRenderer()

    init {
        setEGLContextClientVersion(3)
        setRenderer(this.renderer)
    }
}

/** Code obtained from [dev.to](https://dev.to/den4ic/morphing-geometric-shapes-with-sdf-in-glsl-fragment-shaders-and-visualization-in-jetpack-compose-5db8). */
private class ColorRingRenderer: GLSurfaceView.Renderer {
    private var shaderProgram = 0
    private var vertexBufferObject = 0
    private var vertexArrayObject = 0
    private var screenWidth = 0
    private var screenHeight = 0
    private val vertices = Vertices(listOf(
        Vertex(Coords(-0.5f, -0.5f)),
        Vertex(Coords( 0.5f, -0.5f)),
        Vertex(Coords( 0.0f,  0.5f)),
    ))

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        fun loadShader(type: Int, shaderCode: String): Int
            = GLES30.glCreateShader(type).also { shader ->
                GLES30.glShaderSource(shader, shaderCode)
                GLES30.glCompileShader(shader)
                // Check for errors
                val success = withIntBuffer {
                    GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, it)
                }[0]
                if (success == GLES30.GL_FALSE) {
                    throw RuntimeException("Error compiling shader: ${GLES30.glGetShaderInfoLog(shader)}")
                }
            }

        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, COLOR_RING_FRAGMENT_SHADER)
        this.shaderProgram = GLES30.glCreateProgram()
            .also { shaderProgram ->
                GLES30.glAttachShader(shaderProgram, vertexShader)
                GLES30.glAttachShader(shaderProgram, fragmentShader)
                GLES30.glLinkProgram(shaderProgram)
                // Check for errors
                val success = withIntBuffer {
                    GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, it)
                }[0]
                if (success == GLES30.GL_FALSE) {
                    throw RuntimeException("Error linking shader program: ${GLES30.glGetProgramInfoLog(shaderProgram)}")
                }
                // Deallocate compiled shader sources
                GLES30.glDeleteShader(vertexShader)
                GLES30.glDeleteShader(fragmentShader)
            }

        // Send vertices to the GPU only once.
        this.vertexArrayObject = withIntBuffer { GLES30.glGenVertexArrays(1, it) }[0]
        this.vertexBufferObject = withIntBuffer { GLES30.glGenBuffers(1, it) }[0]
        GLES30.glBindVertexArray(this.vertexArrayObject)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, this.vertexBufferObject)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, this.vertices.size, this.vertices.buffer, GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(0, Coords.len, GLES30.GL_FLOAT, false, Vertex.stride, Coords.offset)
        GLES30.glEnableVertexAttribArray(0)

        // Only draw to the surface once
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(this.shaderProgram)
        GLES30.glBindVertexArray(this.vertexArrayObject)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, this.vertices.len)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        this.setResolution(width, height)
    }

    /** Render loop: */
    override fun onDrawFrame(gl: GL10?) {
//        GLES30.glClearColor(0f, 0f, 0f, 0f)
//        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
//        GLES30.glUseProgram(this.shaderProgram)
//        GLES30.glBindVertexArray(this.vertexArrayObject)
//        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, this.vertices.len)
    }

    private fun setResolution(width: Int, height: Int) {
        this.screenWidth = width
        this.screenHeight = height
//        GLES30.glUniform2f(
//            GLES30.glGetUniformLocation(this.shaderProgram, "iResolution"),
//            this.screenWidth.toFloat(), this.screenHeight.toFloat()
//        )
    }
}

private fun withIntBuffer(bufSize: Int = 1, f: (IntBuffer) -> Unit): List<Int> {
    return IntBuffer.allocate(bufSize)
        .also(f)
        .array()
        .toList()
}

data class Vertex(val coords: Coords /*, val color: Color*/) {
    abstract class Attribute(
        private val instanceClass: Class<*>,
    ) {
        /** The number of components in the [Attribute]. */
        val len: Int = instanceClass.fields.sumOf { 1 }
            .also { if (it > 4)
                throw IllegalStateException("${instanceClass.name}.len is $it, but an Attribute's length must not be greater than 4")
            }

        /** The size (in bytes) of all the components combined. */
        val size: Int = this.len * Float.SIZE_BYTES

        private var _offset: Int? = null
        /** How many components this [Attribute] is offset by from the start of the [Vertex]. */
        val offset: Int get() {
            if (this._offset == null) {
                this._offset = Vertex.attributes
                    .indexOf(this)
                    .let { if (it < 0) null else it }
                    .unwrap { NoSuchFieldException("class ${instanceClass.name} extends Vertex.Attribute, but is not declared in Vertex's fields") }
                    .let { idx -> Vertex.attributes.subList(0, idx) /* Remember it is exclusive */ }
                    .sumOf { it.len }
            }
            return this._offset!!
        }
    }

    companion object {
        val attributes = Vertex::class.java
            .declaredFields
            .filter { field -> !java.lang.reflect.Modifier.isStatic(field.modifiers) }
            .map { field -> field.type
                .declaredFields
                .find { inner -> inner.name == "Companion" && inner.type.simpleName == "Companion" && Attribute::class.java.isAssignableFrom(inner.type) }
                .unwrap { NoSuchFieldException("Class ${field.name} does not have a 'companion object' that implements Attribute, which is required for all fields of class Vertex") }
                .get(null)
                as Attribute
            }

        /** The *total size* (in bytes) of all of the combined [Attributes][Attribute] and their components. */
        val size = this.attributes.sumOf { it.size }

        /** How many bytes to move forward to get to the next instance of the same [Attribute] type in the [Vertex] Buffer.
         *
         * In this case, it is just the *total size* (in bytes) of the Vertex. */
        val stride get() = this.size
    }
}
data class Coords(val x: Float, val y: Float) {
    companion object: Vertex.Attribute(Coords::class.java)
}
data class Color(val r: Float, val g: Float, val b: Float, val a: Float) {
    companion object: Vertex.Attribute(Color::class.java)
}

class Vertices(vertices: List<Vertex>) {
    /** The number of individual [Vertices][Vertex] in this buffer. */
    val len = vertices.size
    /** The size (in bytes) of this buffer. */
    val size = this.len * Vertex.size

    val buffer: FloatBuffer = ByteBuffer
        .allocateDirect(this.size)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .also { buffer ->
            vertices.forEach { vertex ->
                buffer.put(vertex.coords.x)
                buffer.put(vertex.coords.y)
            }
        }
        .apply { position(0) }
}