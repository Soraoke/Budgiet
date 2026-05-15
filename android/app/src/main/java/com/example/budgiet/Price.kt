package com.example.budgiet

import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private fun NumberFormat.applyProperties(currency: Currency) = this.apply {
    this.currency = currency
    this.minimumFractionDigits = currency.defaultFractionDigits
    this.maximumFractionDigits = currency.defaultFractionDigits
    this.isGroupingUsed = true
    this.roundingMode = RoundingMode.HALF_UP
}

/** Formats the price value according to the [Currency] being used.
 *
 * Rounds *decimal digits* to the *nearest* allowed digits.
 * For example, `USD` allows 2 decimal digits,
 * so `1.345` is rounded to `1.35`, and `1.344` is rounded to `1.34`.
 *
 * This returns the **price** formatted with the *decimal point* (if applicable) and *digit separators*.
 * The returned string *does not* include the currency symbol, as that is displayed as a separate `Icon`.
 *
 * Heavily inspired by [this article](https://www.codestudy.net/blog/how-can-i-convert-numbers-to-currency-format-in-android/). */
fun Currency.formatPrice(price: Double, locale: Locale): String {
    @Suppress("KotlinConstantConditions")
    when (price) {
        Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY -> return "NaN"
    }

    return NumberFormat.getCurrencyInstance(locale)
        .applyProperties(this)
        .format(price)
        // Remove leading and trailing whitespace
        .trim()
        // Remove currency symbol
        .trim { char ->
            // First character can be decimal separator, so don't trim it
            char != DecimalFormatSymbols.getInstance(locale).decimalSeparator
            && !char.isDigit()
        }
}

/** Parse the value of the [PriceField][com.example.budgiet.ui.PriceField] input to a *decimal number* ([Double]).
 * The parsing is done according to **[Currency]** requirements (i.e. punctuation and decimal places).
 *
 * If the parsing fails, this function returns an [Result.Err] with the reason. */
fun Currency.parsePrice(price: String, locale: Locale): Result<Double> {
    if (price == "") {
        return Result.Err(NumberFormatException("price string must not be empty"))
    }

    val currency = this
    val possibleDecimalSeparators = listOf('.', ',', '\u066B')
    // How many decimal places are allowed by the currency
    // (e.g. USD uses 2 decimal places, crypto uses a variety number of decimal places).
    val defaultFractionDigits = currency.defaultFractionDigits
    val symbols = DecimalFormatSymbols.getInstance(locale)
    // Separates the decimal digits from unit digits (locale-specific) (i.e. ',', '.').
    val decimalSeparator = symbols.decimalSeparator
    // Separate digits in the thousands (local-specific).
    val groupSeparator = symbols.groupingSeparator

    var prevDigit: Char? = null
    // Index of the decimal point in the price string
    var decimalIdx: Int? = null
    // Gets a value when first group separator is found; resets to 0
    var groupSize: Int? = null

    fun incorrectGroupSizeError()
        = Result.Err(NumberFormatException("Digits must be in groups of 3 if using a group separator ('$groupSeparator')"))

    price.forEachIndexed { index, char ->
        if (char.isDigit()) {
            if (prevDigit == '0'
            && decimalIdx == null
            && index == 1) {
                // 0100 is not allowed, 0.00 is allowed
                return Result.Err(NumberFormatException("Leading un-fractional 0s are not allowed"))
            }
            if (groupSize != null && decimalIdx == null) {
                groupSize += 1
                if (groupSize > 3) {
                    return incorrectGroupSizeError()
                }
            }
            prevDigit = char
        } else if (char == decimalSeparator) {
            // Has more than one decimal point...
            if (decimalIdx != null) {
                // might be trying to use it as group separator.
                return Result.Err(if (groupSize == null && index - decimalIdx > 3) {
                    NumberFormatException("Your locale uses '$groupSeparator' as a group separator")
                } else {
                    NumberFormatException("Decimal '$decimalSeparator' exists already")
                })
            }
            // if more digits used after decimal (i.e. 100.000 USD), return an error
            if (price.length - index - 1 > defaultFractionDigits) {
                return Result.Err(NumberFormatException("${currency.currencyCode} uses up to $defaultFractionDigits decimal places"))
            }
            // Opened group (with separator), but group does not contain enough digits.
            if (groupSize != null && groupSize != 3) {
                return incorrectGroupSizeError()
            }
            decimalIdx = index
        } else if (char == groupSeparator) {
            if (decimalIdx != null) {
                return Result.Err(NumberFormatException("Group separators ('$groupSeparator') are not allowed in decimal digits"))
            }
            if (groupSize != null && groupSize != 3) {
                return incorrectGroupSizeError()
            }
            groupSize = 0
        } else if (char in possibleDecimalSeparators && decimalIdx == null) {
            return Result.Err(NumberFormatException("Your locale uses '$decimalSeparator' as a decimal separator"))
        } else {
            return Result.Err(NumberFormatException("Invalid character '$char' used"))
        }
    }

    // Opened group (with separator), but group does not contain enough digits.
    if (groupSize != null && groupSize != 3 && groupSize != 0) {
        return incorrectGroupSizeError()
    }

    // Price string was validated, now convert it to a string that can be parsed
    val price = price.replace("$groupSeparator", "")
        .replace("$decimalSeparator", ".")
        .trim()

    return try {
        Result.Ok(price.toDouble())
    } catch (e: NumberFormatException) {
        Result.Err(e)
    }
}

// TODO: move this function to rust.
//   The function should take full next text value, input key, input position;
//   and should return the transformed field value, and whether there should be a delay before applying it.
fun Currency.validateFieldInput(fieldValue: String, locale: Locale): Result<Double> {
    return if (fieldValue.isNotEmpty()) {
        val price = fieldValue.filter { c -> c != DecimalFormatSymbols.getInstance(locale).groupingSeparator }
        this.parsePrice(price, locale)
    } else {
        Result.Ok(0.0)
    }
}
