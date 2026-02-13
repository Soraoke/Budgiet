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

    return formatter.format(price)
        // Remove leading and trailing whitespace
        .trim()
        // Remove currency symbol
        .trim { char -> char != '.' && char != ',' && !char.isDigit() }
}

/** Parse the value of the [PriceField][com.example.budgiet.ui.PriceField] input to a *decimal number* ([Double]).
 * The parsing is done according to **[Currency]** requirements (i.e. punctuation and decimal places).
 *
 * If the parsing fails, this function returns an [Result.Err] with the reason. */
fun Currency.parsePrice(price: String): Result<Double?> {
    if (price == "") {
        return Result.Ok(null)
    }

    val currency = this
    var prevDigit: Char? = null
    val priceLen = price.length
    var decimalFound = false
    // How many decimal places are allowed by the currency
    // (e.g. USD uses 2 decimal places, crypto uses a variety number of decimal places).
    val defaultFractionDigits = currency.defaultFractionDigits
    // Separates the decimal digits from unit digits (locale-specific) (i.e. ',', '.').
    val decimalSeparator = DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator
    // Separate digits in the thousands (local-specific).
    val digitSeparator = if (decimalSeparator == ',') { '.' } else { ',' }

    price.forEachIndexed { index, char ->
        if (char.isDigit()) {
            if (prevDigit == '0') {
                // 0100 is not allowed, 0.00 is allowed
                if (!decimalFound && index == 1) {
                    return Result.Err(NumberFormatException("Leading un-fractional 0s are not allowed"))
                }
            }
            prevDigit = char
        } else if (char == decimalSeparator) {
            if (decimalFound) {
                return Result.Err(NumberFormatException("Decimal '$decimalSeparator' exists already"))
            }
            // if more digits used after decimal (i.e. 100.000 USD), return an error
            else if (priceLen - index - 1 > defaultFractionDigits) {
                return Result.Err(NumberFormatException("${currency.currencyCode} uses up to $defaultFractionDigits decimal places"))
            }
            decimalFound = true
        } else if (char == digitSeparator) {
            return Result.Err(NumberFormatException("Your locale uses '$decimalSeparator' as decimal"))
        } else {
            return Result.Err(NumberFormatException("Invalid character '$char' used"))
        }
    }

    return Result.Ok(price.toDouble())
}

/** Validates correctness of Transaction [PriceField][com.example.budgiet.ui.PriceField]'s input
 * and *formats* the number appropriately on success to display in the field.
 *
 * @param price the price input in Transaction form
 * @param this the currency code of the price (e.g. USD)
 * @return a result of formatted price input or a specific price parsing error */
fun Currency.validatePriceInput(price: String): Result<String>
    = this.parsePrice(price)
        .map { price ->
            price?.let {
                this.formatPrice(price)
            } ?: ""
        }