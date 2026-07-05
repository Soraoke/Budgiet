#![allow(unstable_name_collisions)]

use boltffi::export;
use itertools::Itertools as _;
use thiserror::Error;
use crate::{Currency, Locale, Money};

#[boltffi::error]
#[derive(Debug, Clone, Error)]
pub enum ParseMoneyError {
    #[error("price string must not be empty")]
    Empty,
    #[error("Leading un-fractional 0s are not allowed")]
    LeadingZeroes,
    #[error("Invalid characters: {}", list.iter().map(String::as_str).intersperse(" ").collect::<String>())]
    InvalidChars { list: Vec<String> },
    #[error("Your locale uses '{group_sep}' as a group separator instead of {found_sep}")]
    IncorrectGroupSep { group_sep: String, found_sep: String },
    #[error("Digits must be in groups of 3 if using a group separator ('{group_sep}')")]
    InvalidGroupSize { group_sep: String },
    #[error("Your locale uses '{decimal_sep}' as a decimal separator instead of {found_sep}")]
    IncorrectDecimalSep { decimal_sep: String, found_sep: String },
    #[error("There can only be 1 decimal separator ('{decimal_sep}')")]
    MultipleDecimalSeps { decimal_sep: String },
    #[error("Your currency ({currency_code}) uses up to {currency_digits} decimal places, but found {found_digits}")]
    TooManyFractionalDigits { currency_code: String, currency_digits: u32, found_digits: u32 },
    #[error("Group separators ('{group_sep}') after the decimal separator ('{decimal_sep}')")]
    GroupSepInFractionalSection { group_sep: String, decimal_sep: String },
}

pub fn parse_money_amount(price: &str, currency: Currency, locale: Locale) -> Result<Money, ParseMoneyError> {
    // if (price == "") {
    //     return Result.Err(NumberFormatException("price string must not be empty"))
    // }

    // val currency = this
    // val possibleDecimalSeparators = listOf('.', ',', '\u066B')
    // // How many decimal places are allowed by the currency
    // // (e.g. USD uses 2 decimal places, crypto uses a variety number of decimal places).
    // val defaultFractionDigits = currency.defaultFractionDigits
    // val symbols = DecimalFormatSymbols.getInstance(locale)
    // // Separates the decimal digits from unit digits (locale-specific) (i.e. ',', '.').
    // val decimalSeparator = symbols.decimalSeparator
    // // Separate digits in the thousands (local-specific).
    // val groupSeparator = symbols.groupingSeparator
    //
    // var prevDigit: Char? = null
    // // Index of the decimal point in the price string
    // var decimalIdx: Int? = null
    // // Gets a value when first group separator is found; resets to 0
    // var groupSize: Int? = null
    //
    // fun incorrectGroupSizeError()
    //     = Result.Err(NumberFormatException("Digits must be in groups of 3 if using a group separator ('$groupSeparator')"))
    //
    // price.forEachIndexed { index, char ->
    //     if (char.isDigit()) {
    //         if (prevDigit == '0'
    //         && decimalIdx == null
    //         && index == 1) {
    //             // 0100 is not allowed, 0.00 is allowed
    //             return Result.Err(NumberFormatException("Leading un-fractional 0s are not allowed"))
    //         }
    //         if (groupSize != null && decimalIdx == null) {
    //             groupSize += 1
    //             if (groupSize > 3) {
    //                 return incorrectGroupSizeError()
    //             }
    //         }
    //         prevDigit = char
    //     } else if (char == decimalSeparator) {
    //         // Has more than one decimal point...
    //         if (decimalIdx != null) {
    //             // might be trying to use it as group separator.
    //             return Result.Err(if (groupSize == null && index - decimalIdx > 3) {
    //                 NumberFormatException("Your locale uses '$groupSeparator' as a group separator")
    //             } else {
    //                 NumberFormatException("Decimal '$decimalSeparator' exists already")
    //             })
    //         }
    //         // if more digits used after decimal (i.e. 100.000 USD), return an error
    //         if (price.length - index - 1 > defaultFractionDigits) {
    //             return Result.Err(NumberFormatException("${currency.currencyCode} uses up to $defaultFractionDigits decimal places"))
    //         }
    //         // Opened group (with separator), but group does not contain enough digits.
    //         if (groupSize != null && groupSize != 3) {
    //             return incorrectGroupSizeError()
    //         }
    //         decimalIdx = index
    //     } else if (char == groupSeparator) {
    //         if (decimalIdx != null) {
    //             return Result.Err(NumberFormatException("Group separators ('$groupSeparator') are not allowed in decimal digits"))
    //         }
    //         if (groupSize != null && groupSize != 3) {
    //             return incorrectGroupSizeError()
    //         }
    //         groupSize = 0
    //     } else if (char in possibleDecimalSeparators && decimalIdx == null) {
    //         return Result.Err(NumberFormatException("Your locale uses '$decimalSeparator' as a decimal separator"))
    //     } else {
    //         return Result.Err(NumberFormatException("Invalid character '$char' used"))
    //     }
    // }
    //
    // // Opened group (with separator), but group does not contain enough digits.
    // if (groupSize != null && groupSize != 3 && groupSize != 0) {
    //     return incorrectGroupSizeError()
    // }
    //
    // // Price string was validated, now convert it to a string that can be parsed
    // val price = price.replace("$groupSeparator", "")
    //     .replace("$decimalSeparator", ".")
    //     .trim()
    //
    // return try {
    //     Result.Ok(price.toDouble())
    // } catch (e: NumberFormatException) {
    //     Result.Err(e)
    // }
    todo!("Parse {price:?} with {currency:?} and {locale:?}")
}

// TODO: doc; does not return ParseMoneyError::Empty
//   This function should take full next text value, input key, input position;
//   and should return the transformed field value, and whether there should be a delay before applying it.
#[export]
pub fn validate_money_field_input(s: &str, currency: Currency, locale: Locale) -> Result<Money, ParseMoneyError> {
    // return if (fieldValue.isNotEmpty()) {
    //     val price = fieldValue.filter { c -> c != DecimalFormatSymbols.getInstance(locale).groupingSeparator }
    //     this.parsePrice(price, locale)
    // } else {
    //     Result.Ok(0.0)
    // }
    todo!("Parse {s:?} with {currency:?} and {locale:?}, checking keystrokes")
}

/// Formats the price value according to the [`Currency`] and [`Locale`] being used.
///
/// Rounds *decimal digits* to the *nearest* allowed digits.
/// For example, [`USD`][rusty_money::iso::USD] allows 2 decimal digits,
/// so `1.345` is rounded to `1.35`, and `1.344` is rounded to `1.34`.
///
/// This returns the **price** formatted with the *decimal point* (if applicable) and *digit separators*.
/// The returned string *does not* include the currency symbol, as that is displayed as a separate `Icon`.
///
/// Heavily inspired by [this article](https://www.codestudy.net/blog/how-can-i-convert-numbers-to-currency-format-in-android/).
pub fn format_money(money: &Money, locale: Locale, include_symbol: bool) -> String {
    // when (price) {
    //     Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY -> return "NaN"
    // }
    //
    // return NumberFormat.getCurrencyInstance(locale)
    //     .applyProperties(this)
    //     .format(price)
    //     // Remove leading and trailing whitespace
    //     .trim()
    //     // Remove currency symbol
    //     .trim { char ->
    //         // First character can be decimal separator, so don't trim it
    //         char != DecimalFormatSymbols.getInstance(locale).decimalSeparator
    //         && !char.isDigit()
    //     }
    todo!("Format {money:?} with {locale:?}, optionally including the currency symbol ({include_symbol:?})")
}
