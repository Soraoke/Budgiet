package com.example.budgiet

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorModel
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.IllegalFormatException

@SuppressLint("ExperimentalAnnotationRetention")
@RequiresOptIn(message = "This part of the API is visible only for testing.")
internal annotation class UsableInTestsOnly

/** A *sum type* that represents an operation's **result**.
 *
 * A [Result] can be [Ok] if the operation was *successful* and provides a good value,
 * or [Err] if the operation *failed* and threw an [Exception] ([Throwable]).
 *
 * ## Example
 *
 * ```kotlin
 * when (val result = ...) {
 *     is Result.Ok -> useValue(result.value)
 *     is Result.Err -> useError(result.error)
 * }
 * ``` */
sealed class Result<out T> {
    class Ok<out T>(val value: T) : Result<T>()
    class Err(val error: Throwable) : Result<Nothing>()

    /** Unwraps the Result to retrieve it's Ok value
     * If the Result is Err, it will return null
     *
     * @return Ok Result value or null */
    fun getOkOrNull(): T? {
        return when (this) {
            is Ok -> this.value
            is Err -> null
        }
    }

    /** Unwraps the Result to retrieve it's Ok value
     * If the Result is Err, the exception value will be *thrown*.
     *
     * @return Ok Result value */
    fun unwrap(): T {
        return when (this) {
            is Ok -> this.value
            is Err -> throw this.error
        }
    }

    /** Unwraps the Result to retrieve it's Err throwable
     * If the Result is Ok, it will return null
     *
     * @return Err Result throwable or null */
    fun getErrOrNull(): Throwable? {
        return when (this) {
            is Ok -> null
            is Err -> this.error
        }
    }

    /** Unwraps the Result to retrieve it's Err throwable
     * If the Result is Ok, an exception will be *thrown*.
     *
     * @return Ok Result value */
    fun unwrapErr(): Throwable {
        return when (this) {
            is Ok -> throw RuntimeException("Expected error value, found Ok(${this.value})")
            is Err -> this.error
        }
    }

    /** Transforms the value of `this` if it is [Ok].
     * Does nothing otherwise. */
    fun <U> map(transform: (T) -> U): Result<U> {
        return when(this) {
            is Ok -> Ok(transform(this.value))
            is Err -> this
        }
    }

    fun isOkAnd(predicate: (T) -> Boolean): Boolean = this is Ok && predicate(this.value)
    fun isErrAnd(predicate: (Throwable) -> Boolean): Boolean = this is Err && predicate(this.error)

    companion object {
        /** Converts a [kotlin.Result] to this app's custom [Result]. */
        fun <T> fromKt(result: kotlin.Result<T>): Result<T> {
            return if (result.isSuccess) {
                Ok(result.getOrNull()!!)
            } else {
                Err(result.exceptionOrNull()!!)
            }
        }
    }
}
/** Converts a [kotlin.Result] to this app's custom [Result]. */
fun <T> kotlin.Result<T>.into(): Result<T> = Result.fromKt(this)

/** Unwrap a **nullable** value, or *throw* and [Exception][Throwable] if it was `null`.
 *
 * Can pass a function that generates a custom [exception] to *throw* when the value is `null`.*/
fun <T> T?.unwrap(exception: (() -> Throwable)? = null): T
    = if (exception == null) this!! else this ?: throw exception()

fun localDateFromUtcMillis(utcMillis: Long): LocalDate {
    return Instant.ofEpochMilli(utcMillis)
        // NOTE: This does not set the timezone of the Date to UTC,
        // but instead interprets the millis as set in UTC, which is what the DatePicker provides.
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
}
fun LocalDate.formatRelativeToPresent(): String {
    val now = LocalDate.now()

    return when (this) {
        // I know this arm is unreachable, but wanted to include it for completeness.
        // Maybe we'll want to use it for something else later.
        now.plusDays(1) -> "Tomorrow"
        now -> "Today"
        now.minusDays(1) -> "Yesterday"
        // Formatting method taken from https://stackoverflow.com/a/56668796/32115191.
        else -> DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .let { formatter ->
                this.format(formatter)
            }
    }
}

private fun colorComponentToHex(i: Float): String
    = (255 * i).toUInt()
        .toHexString(HexFormat {
            this.upperCase = true
            this.number {
                this.removeLeadingZeros = true
                this.minLength = 2
            }
        })

fun Color.rgbToHex(): String {
    if (this.colorSpace.model != ColorModel.Rgb) {
        throw IllegalArgumentException("Color($this) must be in RGB color space to convert to hex code")
    }
    return "${colorComponentToHex(this.red)}${colorComponentToHex(this.green)}${colorComponentToHex(this.blue)}"
}

fun Color.rgbaToHex(): String
    = "${this.rgbToHex()}${colorComponentToHex(this.alpha)}"

/** Parse a *Hexadecimal string* to obtain an `RGB[A]` [Color].
 *
 * @throws IllegalArgumentException if the parsing fails. */
fun Color.Companion.fromHex(hex: String, allowAlpha: Boolean = true): Color {
    // Validate characters
    val hexValues = hex.map { char ->
        when (char) {
            in '0'..'9' -> char - '0'
            in 'a'..'f' -> char - 'a' + 10
            in 'A'..'F' -> char - 'A' + 10
            else -> throw IllegalArgumentException("Hex code contained invalid character '$char'. Hex characters must be decimal digits (0-9) or A-F (case insensitive).")
        }
    }

    if ((hex.length == 4 || hex.length == 8) && !allowAlpha) {
        throw IllegalArgumentException("Alpha (opacity) channel is not allowed here")
    }

    return when (hex.length) {
        in 0..2 -> throw IllegalArgumentException("Hex code must be at least 3 characters in length")
        in 3..4 -> {
            /** Copy the digit in the first *hexadecimal digit place* to the next *hexadecimal digit place*.
             *
             * This is done by moving the bits by a nibble. */
            fun cloneDigit(i: Int) = i shl 4 or i
            Color(
                red   = cloneDigit(hexValues[0]),
                green = cloneDigit(hexValues[1]),
                blue  = cloneDigit(hexValues[2]),
                alpha = cloneDigit(hexValues.getOrNull(3) ?: 0xF),
            )
        }
        6, 8 -> {
            Color(
                red   = hexValues[0] shl 4 or hexValues[1],
                green = hexValues[2] shl 4 or hexValues[3],
                blue  = hexValues[4] shl 4 or hexValues[5],
                alpha = hexValues.getOrNull(6)?.let {
                    hexValues[6] shl 4 or hexValues[7]
                } ?: 0xFF,
            )
        }
        else -> throw IllegalArgumentException("Hex code has invalid length (${hex.length}) to convert it to a color")
    }
}

/** Maps a **[Currency]** to one of our imported *drawable resources*.
 *
 * This will take the [Currency]'s [symbol][Currency.getSymbol] and try to match it with a resource.
 *
 * @returns `null` if we do not recognize the currency's *symbol*.
 * In this case, the caller should omit the currency icon,
 * or default to using the **[dollar][R.drawable.currency_dollar_24px]** icon if an icon is required. */
@Composable
fun getCurrencyIcon(currency: Currency): Painter? {
    val map = hashMapOf(
        '$' to R.drawable.currency_dollar_24px,
        '₱' to R.drawable.currency_peso_24px,
        '£' to R.drawable.currency_pound_24px,
        '¥' to R.drawable.currency_yen_24px,
        '₩' to R.drawable.currency_won_24px,
        '₪' to R.drawable.currency_shekel_24px,
        '₽' to R.drawable.currency_ruble_24px,
        '€' to R.drawable.currency_euro_24px,
        '₠' to R.drawable.currency_euro_24px,
        '₹' to R.drawable.currency_rupee_24px,
        'र' to R.drawable.currency_rupee_24px,
        '₨' to R.drawable.currency_rupee_24px,
        '₣' to R.drawable.currency_franc_24px,
        'Ꞙ' to R.drawable.currency_franc_24px,
        '₺' to R.drawable.currency_lira_24px,
        '₤' to R.drawable.currency_lira_24px,
        '₿' to R.drawable.currency_bitcoin_24px,
    )

    // Check if the symbol string contains an actual symbol at all
    for (c in currency.symbol) {
        map[c]?.let { res ->
            return painterResource(res)
        }
    }

    return null
}

// TODO: Actual implementation in Rust
fun graphemeStringLength(string: String): Int = string.length
// TODO: Actual implementation in Rust
fun graphemeStringTake(string: String, count: Int): String = string.take(count)

//class Grapheme(private val inner: CharSequence) {
//    override fun toString(): String = this.inner.toString()
//    override fun equals(other: Any?): Boolean = this.inner == other
//    override fun hashCode(): Int = this.inner.hashCode()
//}
//
//// TODO: move this to Rust. The backend should be in charge of processing input. This is here for now to satisfy ui tests.
///** A wrapper around [String] that operates on [**graphemes**](https://en.wikipedia.org/wiki/Grapheme) instead of [characters][Char] or *code points*.
// *
// * This is useful to manage a String where you only care about *visual* character units. */
//class GraphemeString(private val inner: String): Comparable<String>, Iterable<Grapheme> {
//    constructor() : this(String())
//
//    val length: Int = run {
//        // Why forEach does not catch NoSuchElementException?? That's beyond me...
//        @Suppress("SpellCheckingInspection")
//        val iter = this@GraphemeString.charIdxIterator()
//        var count = 0
//        while (iter.next() != BreakIterator.DONE) {
//            count ++
//        }
//        count
//    }
//
//    /** Get the **grapheme** at the **index**.
//     *
//     * This will not work the same as [String]'s *get()*,
//     * as that indexes over [Char]s, but this indexes over **graphemes**.
//     *
//     * @throws IndexOutOfBoundsException */
//    operator fun get(index: Int): Grapheme {
//        // num of chars >= num of graphemes (always)
//        if (index > this.inner.length) {
//            throw IndexOutOfBoundsException("Provided index $index, but the String only has ${this.inner.length} characters.")
//        }
//
//        var count = 0
//        this.iterator().forEach { grapheme ->
//            if (count == index) {
//                return grapheme
//            }
//            count++
//        }
//
//        throw IndexOutOfBoundsException("Provided index $index, but the String only has $count graphemes.")
//    }
//
//    /** Returns a [GraphemeString] containing **graphemes** of the original string at the specified range of **indices**.
//     *
//     * @throws IndexOutOfBoundsException
//     * @throws IllegalArgumentException if [startIndex] > [endIndex] */
//    fun subSequence(startIndex: Int, endIndex: Int): GraphemeString {
//        if (startIndex > endIndex) {
//            throw IllegalArgumentException("startIndex ($startIndex) must not be greater than endIndex ($endIndex).")
//        }
//        // num of chars >= num of graphemes (always)
//        if (startIndex >= this.inner.length) {
//            throw IndexOutOfBoundsException("Provided startIndex $startIndex, but the String only has ${this.inner.length} characters.")
//        }
//        if (endIndex >= this.inner.length) {
//            throw IndexOutOfBoundsException("Provided endIndex $endIndex, but the String only has ${this.inner.length} characters.")
//        }
//
//        val it = this.charIdxIterator()
//        var startCharIndex: Int? = null
//        var endCharIndex: Int? = null
//        var count = 0
//        @Suppress("VariableInitializerIsRedundant")
//        var prev = 0
//        var current = 0
//
//        while (true) {
//            prev = current
//            current = it.next()
//            if (current == BreakIterator.DONE) {
//                break
//            }
//
//            if (count == startIndex) {
//                startCharIndex = prev
//            }
//            if (count == endIndex) {
//                endCharIndex = current
//            }
//
//            if (startCharIndex != null && endCharIndex != null) {
//                return GraphemeString(this.inner.slice(startCharIndex..endCharIndex))
//            }
//
//            count++
//        }
//
//        if (startCharIndex == null) {
//            throw IndexOutOfBoundsException("Provided startIndex $startIndex, but the String only has $count graphemes.")
//        }
//        @Suppress("KotlinConstantConditions")
//        if (endCharIndex == null) {
//            throw IndexOutOfBoundsException("Provided endIndex $endIndex, but the String only has $count graphemes.")
//        }
//
//        throw InternalError("Neither startCharIndex or endCharIndex were null, but the function did not return the string slice.")
//    }
//
//    fun take(count: Int): GraphemeString {
//        return if (this.length <= count) {
//            this
//        } else {
//            this.subSequence(0, count)
//        }
//    }
//
//    private fun charIdxIterator(): BreakIterator {
//        val it = BreakIterator.getCharacterInstance()
//        it.setText(this@GraphemeString.inner)
//        return it
//    }
//
//    override fun iterator(): Iterator<Grapheme> = object: Iterator<Grapheme> {
//        @Suppress("SpellCheckingInspection")
//        val iter = this@GraphemeString.charIdxIterator()
//        var prevBoundary = 0
//
//        override fun next(): Grapheme {
//            this.prevBoundary = this.iter.current()
//            val currentBoundary = this.iter.next()
//
//            if (currentBoundary == BreakIterator.DONE) {
//                throw NoSuchElementException()
//            }
//
//            return Grapheme(this@GraphemeString.inner.subSequence(this.prevBoundary, currentBoundary))
//        }
//
//        override fun hasNext(): Boolean = (this.iter.clone() as BreakIterator).next() == BreakIterator.DONE
//    }
//
//    override fun compareTo(other: String): Int = this.inner.compareTo(other)
//
//    override fun toString(): String = this.inner
//
//    override fun equals(other: Any?): Boolean = this.inner == other
//    override fun hashCode(): Int = this.inner.hashCode()
//}
