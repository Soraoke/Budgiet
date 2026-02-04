package com.example.budgiet

import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

// TODO: unit test
/** Formats the price value according to the [Currency] being used.
 *
 * This returns the **price** formatted with the *decimal point* (if applicable) and *digit separators*.
 * The returned string *does not* include the currency symbol, as that is displayed as a separate `Icon`.
 *
 * Heavily inspired by [this article](https://www.codestudy.net/blog/how-can-i-convert-numbers-to-currency-format-in-android/). */
fun Currency.formatPrice(price: Double): String {
    @Suppress("KotlinConstantConditions")
    when (price) {
        Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY -> return "NaN"
    }

    val formatter = NumberFormat.getCurrencyInstance()
    formatter.currency = this
    formatter.minimumFractionDigits = this.defaultFractionDigits
    formatter.maximumFractionDigits = this.defaultFractionDigits

    var formatted = formatter.format(price)

    // Remove currency symbol
    formatted = if (!formatted.first().isDigit()) {
        formatted.removeRange(0..0)
    } else if (!formatted.last().isDigit()) {
        val i = formatted.length - 1
        formatted.removeRange(i..i)
    } else {
        formatted
    }

    // Remove leading and trailing whitespace
    return formatted.trim()
}

/**
 * Validates correctness of Transaction's price field input and
 * formats the number appropriately on success
 *
 * @param price the price input in Transaction form
 * @param currency the currency code of the price (e.g. USD)
 * @return a result of formatted price input or a specific price parsing error
 */
fun parsePrice(price: String, currency: Currency): Result<String> {
    if (price == "") {
        return Result.Ok("")
    }

    var prevDigit: Char? = null
    val priceLen = price.length
    var decimalFound = false
    // how many decimal places are allowed by the currency (i.e. USD uses 2 decimal places,
    // crypto could be using varying number of decimal places)
    val defaultFractionDigits = currency.defaultFractionDigits
    // locale-specific decimal separator (i.e. ',', '.')
    val decimalSeparator = DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator
    val otherDecimalSeparator = if (decimalSeparator == ',') {
        '.'
    } else {
        ','
    }
    price.forEachIndexed { index, char ->
        if (char.isDigit()) {
            if (prevDigit == '0') {
                // 0100 is not allowed, 0.00 is allowed
                if (!decimalFound && index == 1) {
                    return Result.Err(Exception("Leading un-fractional 0s are not allowed"))
                }
            }
            prevDigit = char
        } else if (char == decimalSeparator) {
            if (decimalFound) {
                return Result.Err(Exception("Decimal '$decimalSeparator' exists already"))
            }
            // if more digits used after decimal (i.e. 100.000 USD), return an error
            else if (priceLen - index - 1 > defaultFractionDigits) {
                return Result.Err(Exception("${currency.currencyCode} uses up to $defaultFractionDigits decimal places"))
            }
            decimalFound = true
        } else if (char == otherDecimalSeparator) {
            return Result.Err(Exception("Your locale uses '$decimalSeparator' as decimal"))
        }
        else {
            return Result.Err(Exception("Invalid character '$char' used"))
        }
    }

    val priceDouble = price.toDouble()
    val priceDecimalFormat = String.format("%.${defaultFractionDigits}f", priceDouble)

    return Result.Ok(priceDecimalFormat)
}