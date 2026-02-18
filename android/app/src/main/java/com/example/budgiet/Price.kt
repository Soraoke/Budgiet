package com.example.budgiet

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import java.io.File
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
fun Currency.parsePrice(price: String, locale: Locale): Result<Double?> {
    if (price == "") {
        return Result.Ok(null)
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

/** Validates correctness of Transaction [PriceField][com.example.budgiet.ui.PriceField]'s input
 * and *formats* the number appropriately on success to display in the field.
 *
 * @param price the price input in Transaction form
 * @param this the currency code of the price (e.g. USD)
 * @return a result of formatted price input or a specific price parsing error */
fun Currency.validatePriceInput(price: String, locale: Locale): Result<String>
    = this.parsePrice(price, locale)
        .map { price ->
            price?.let {
                this.formatPrice(price, locale)
            } ?: ""
        }

const val RECENT_CURRENCIES_FILE_NAME = "recentCurrencies.txt"
const val RECENT_CURRENCIES_LOG_TAG = "RecentlyUsedCurrencies"

/** Returns an ordered [List] of **currency codes**, sorted by *most recent use*.
 *
 * The return value tells the state of the data:
 *  * **`null`**: The data is still being loaded.
 *  * **[Result.Err]**: There was an error loading the data.
 *  * **[Result.Ok]**: The data finished loading successfully.
 *
 *  Since this is a [Composable] with an internal [MutableState],
 *  changes in the state will propagate to the caller and it will be recomposed,
 *  even if this function itself does not return a [MutableState]. */
@Composable
fun getRecentlySelectedCurrencies(): State<Result<List<String>>?> {
    if (recentlyUsedCurrencies.value == null) {
        // Load ordered currencies from storage.

        val context = LocalContext.current
        rememberWork(recentlyUsedCurrencies) {
            // FIXME: Composable does not recompose when work is finished,
            //  even though the recentlyUsedCurrencies MutableState is modified with the returned value.
            //  btw, this only happens when recentlyUsedCurrencies is NOT in MainActivity.
            // FIXME: its a timing issue!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//            delay(50)
            val file = File(context.filesDir, RECENT_CURRENCIES_FILE_NAME)
            // Read the entirety of the file to move around the elements.
            file.createNewFile()
            file.readText()
                .split('\n')
                // Last element will always be empty because the file always ends with newLine (unless it is empty).
                .dropLast(1)
                .toMutableStateList()
        }
    }

    if (recentlyUsedCurrencies.value is Result.Err) {
        Log.e(RECENT_CURRENCIES_LOG_TAG, "Error reading recent currencies from storage: ${(recentlyUsedCurrencies.value as Result.Err).error}")
    }

    @Suppress("UNCHECKED_CAST")
    return recentlyUsedCurrencies as State<Result<List<String>>?>
}

/** Removes (clears) all currencies from the *ordered list* in memory and from the file in storage. */
fun Context.clearRecentlyUsedCurrencies() {
    // Clear in memory
    recentlyUsedCurrencies.value = Result.Ok(mutableStateListOf())
    // Clear in storage
    dispatchWork {
        Log.i(RECENT_CURRENCIES_LOG_TAG, "Clear recently used currencies in storage.")
        val file = File(this.filesDir, RECENT_CURRENCIES_FILE_NAME)
        file.writeText("")
    }
}

/** Marks a [Currency][java.util.Currency] as recently used (a.k.a. it was just selected),
 * moving it to the front of the [List] of recent currencies,
 * which is **sorted** by latest use.
 *
 * This function will also write to the [File] in storage the same content as the [List] in memory.
 *
 * See [getRecentlySelectedCurrencies] to read from this [List]. */
fun Context.markCurrencyRecentlyUsed(currencyCode: String) {
    when (recentlyUsedCurrencies.value) {
        null, is Result.Err -> recentlyUsedCurrencies.value = Result.Ok(mutableStateListOf())
        is Result.Ok -> { }
    }
    val orderedCurrencies = recentlyUsedCurrencies.value!!.unwrap() as MutableList<String>

    Log.i(RECENT_CURRENCIES_LOG_TAG, "Moving Currency \"$currencyCode\" to the front of MutableStateList in memory.")

    // Apply to mutable list in memory
    // Find currency in the argument
    when (val idx = orderedCurrencies.indexOf(currencyCode)) {
        // The currency was already first in the list; do nothing.
        0 -> { }
        // Currency was not found in the List, so it must be prepended.
        -1 -> orderedCurrencies.add(0, currencyCode)
        // Remove target currency (arg) from the List, and put it in the front.
        else -> {
            orderedCurrencies.add(0, orderedCurrencies.removeAt(idx))
        }
    }

    // Apply to storage
    dispatchWork {
        Log.i(RECENT_CURRENCIES_LOG_TAG, "Moving Currency \"$currencyCode\" to the front of File in storage.")
        val file = File(this.filesDir, RECENT_CURRENCIES_FILE_NAME)
        file.createNewFile()
        // Write the modified list
        file.writeText(orderedCurrencies.joinToString(separator = "", truncated = "") { code -> "$code\n" })
    }
}
