package com.example.budgiet

import org.junit.Test
import java.util.Currency
import java.util.Locale

// basic style.
val usd: Locale = Locale.US
// Uses 3 decimal places, and space-like group separator.
val tnd: Locale = Locale.of("fr", "TN")
//val bhd: Locale = Locale.of("ar", "BH")
// Uses period for digit separator, and comma for decimal separator (reversed).
val itl: Locale = Locale.ITALY

/**
 * Transaction Price Field Positive and Negative Tests
 */
class PriceFieldTests {
    // TODO: test with locales that use different digits.

    // Test in terms of locale
    fun Locale.parsePrice(price: String)
        = Currency.getInstance(this)
            .parsePrice(price, this)

    // Test in terms of locale
    fun Locale.formatPrice(price: Double)
        = Currency.getInstance(this)
            .formatPrice(price, this)

    @Test
    fun parsePriceTest() {
        // Parse Integer.
        usd.parsePrice("1").unwrap()
            .assertEquals(1.0)
        tnd.parsePrice("1").unwrap()
            .assertEquals(1.0)
        itl.parsePrice("1").unwrap()
            .assertEquals(1.0)

        // Parse decimal point.
        usd.parsePrice("1.1").unwrap()
            .assertEquals(1.1)
        itl.parsePrice("1,1").unwrap()
            .assertEquals(1.1)

        // Parse empty decimal places.
        usd.parsePrice("1.").unwrap()
            .assertEquals(1.0)
        itl.parsePrice("1,").unwrap()
            .assertEquals(1.0)

        // Parse empty unit places.
        usd.parsePrice(".1").unwrap()
            .assertEquals(0.1)
        itl.parsePrice(",1").unwrap()
            .assertEquals(0.1)

        // Parse digit separator.
        usd.parsePrice("1,234,567.8").unwrap()
            .assertEquals(1234567.8)
        tnd.parsePrice("1 234 567,8").unwrap()
            .assertEquals(1234567.8)
        itl.parsePrice("1.234.567,8").unwrap()
            .assertEquals(1234567.8)
        // Parse WITHOUT digit separator.
        usd.parsePrice("1234567.8").unwrap()
            .assertEquals(1234567.8)
        tnd.parsePrice("1234567,8").unwrap()
            .assertEquals(1234567.8)
        itl.parsePrice("1234567,8").unwrap()
            .assertEquals(1234567.8)

        // Error on invalid Characters.
        usd.parsePrice("(100)").unwrapErr()
            .message
            .assertEquals("Invalid character '(' used")
        tnd.parsePrice("a100").unwrapErr()
            .message
            .assertEquals("Invalid character 'a' used")

        // Error on more than one decimal point.
        usd.parsePrice("1..0").unwrapErr()
            .message
            .assertEquals("Decimal '.' exists already")
        itl.parsePrice("1,,0").unwrapErr()
            .message
            .assertEquals("Decimal ',' exists already")

        // Error on incorrect group separator placement
        usd.parsePrice("1,23,4").unwrapErr()
            .message
            .assertEquals("Digits must be in groups of 3 if using a group separator (',')")
        itl.parsePrice("1.2345.6").unwrapErr()
            .message
            .assertEquals("Digits must be in groups of 3 if using a group separator ('.')")

        // Error on too many decimal digits.
        usd.parsePrice("1.000").unwrapErr()
            .message
            .assertEquals("USD uses up to 2 decimal places")
        tnd.parsePrice("1,0000").unwrapErr()
            .message
            .assertEquals("TND uses up to 3 decimal places")
        itl.parsePrice("1,000").unwrapErr()
            .message
            .assertEquals("EUR uses up to 2 decimal places")

        // Error on leading 0 digits.
        usd.parsePrice("0100").unwrapErr()
            .message
            .assertEquals("Leading un-fractional 0s are not allowed")
        usd.parsePrice("00100").unwrapErr()
            .message
            .assertEquals("Leading un-fractional 0s are not allowed")
        tnd.parsePrice("000100").unwrapErr()
            .message
            .assertEquals("Leading un-fractional 0s are not allowed")

        // Error on incorrect decimal separators.
        usd.parsePrice("1,00").unwrapErr()
            .message
            .assertEquals("Digits must be in groups of 3 if using a group separator (',')")
        tnd.parsePrice("1.000").unwrapErr()
            .message
            .assertEquals("Your locale uses ',' as a decimal separator")
        itl.parsePrice("1.00").unwrapErr()
            .message
            .assertEquals("Digits must be in groups of 3 if using a group separator ('.')")

        // No need to test this, will fail with too many decimal places error (its more efficient).
//        // Error group separator in decimal places
//        usd.parsePrice("1.00,000").unwrapErr()
//            .message
//            .assertEquals("Group separators (',') are not allowed in decimal digits")
//        itl.parsePrice("1,00.000").unwrapErr()
//            .message
//            .assertEquals("Group separators ('.') are not allowed in decimal digits")
    }

    @Test
    fun formatPriceTest() {
        // Integer value
        usd.formatPrice(10.0)
            .assertEquals("10.00")

        // Decimal places
        usd.formatPrice(10.11)
            .assertEquals("10.11")
        tnd.formatPrice(10.111)
            .assertEquals("10,111")
        itl.formatPrice(10.11)
            .assertEquals("10,11")

        // Digit separator
        usd.formatPrice(1234.0)
            .assertEquals("1,234.00")
        tnd.formatPrice(1234.0)
            .assertEquals("1 234,000")
        itl.formatPrice(1234.0)
            .assertEquals("1.234,00")

        // Digit separator AND decimal places
        usd.formatPrice(1234.11)
            .assertEquals("1,234.11")
        tnd.formatPrice(1234.111)
            .assertEquals("1 234,111")
        itl.formatPrice(1234.11)
            .assertEquals("1.234,11")

        // Too many decimal digits (round up)
        usd.formatPrice(10.116)
            .assertEquals("10.12")
        tnd.formatPrice(10.1116)
            .assertEquals("10,112")
        itl.formatPrice(10.116)
            .assertEquals("10,12")
        // (round down)
        usd.formatPrice(10.113)
            .assertEquals("10.11")
        tnd.formatPrice(10.1113)
            .assertEquals("10,111")
        itl.formatPrice(10.113)
            .assertEquals("10,11")
    }
}