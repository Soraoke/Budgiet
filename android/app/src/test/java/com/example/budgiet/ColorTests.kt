package com.example.budgiet

import androidx.compose.ui.graphics.Color
import org.junit.Test

val BLACK_SOLID = Color(red = 0, green = 0, blue = 0, alpha = 0xFF)
val TRANSPARENT = Color(red = 0, green = 0, blue = 0, alpha = 0)
val LEMON_SOLID = Color(red = 0xFF, green = 0xFF, blue = 0xAA, alpha = 0xFF)
val LEMON_TRANSLUCENT = Color(red = 0xFF, green = 0xFF, blue = 0xAA, alpha = 0xEE)

class ColorUnitTests {
    @Test
    fun toHex() {
        BLACK_SOLID.rgbToHex()
            .assertEquals("000000")
        LEMON_SOLID.rgbToHex()
            .assertEquals("FFFFAA")
    }

    @Test
    fun fromHex() {
        // Test 3-digit hex code (no alpha)
        Color.fromHex("000")
            .assertEquals(BLACK_SOLID)
        Color.fromHex("FFA")
            .assertEquals(LEMON_SOLID)
        // Test 4-digit hex code (with alpha)
        Color.fromHex("0000")
            .assertEquals(TRANSPARENT)
        Color.fromHex("FFAE")
            .assertEquals(LEMON_TRANSLUCENT)
        // Test 6-digit hex code (no alpha)
        Color.fromHex("000000")
            .assertEquals(BLACK_SOLID)
        Color.fromHex("FFFFAA")
            .assertEquals(LEMON_SOLID)
        // Test 8-digit hex code (with alpha)
        Color.fromHex("00000000")
            .assertEquals(TRANSPARENT)
        Color.fromHex("FFFFAAEE")
            .assertEquals(LEMON_TRANSLUCENT)
        // Test Errors
        runCatching { Color.fromHex("FFJ") }
            .assert({ it.isFailure })
        runCatching { Color.fromHex("") }
            .assert({ it.isFailure })
        runCatching { Color.fromHex("00") }
            .assert({ it.isFailure })
        runCatching { Color.fromHex("00000") }
            .assert({ it.isFailure })
        runCatching { Color.fromHex("0000000") }
            .assert({ it.isFailure })
        runCatching { Color.fromHex("000000000") }
            .assert({ it.isFailure })
    }
}