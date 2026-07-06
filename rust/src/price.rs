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

pub static ALL_CURRENCIES: &[Currency] = &[
    rusty_money::iso::AED, rusty_money::iso::AFN, rusty_money::iso::ALL, rusty_money::iso::AMD, rusty_money::iso::ANG, rusty_money::iso::AOA, rusty_money::iso::ARS, rusty_money::iso::AUD, rusty_money::iso::AWG, rusty_money::iso::AZN, rusty_money::iso::BAM, rusty_money::iso::BBD, rusty_money::iso::BDT, rusty_money::iso::BGN, rusty_money::iso::BHD, rusty_money::iso::BIF, rusty_money::iso::BMD, rusty_money::iso::BND, rusty_money::iso::BOB, rusty_money::iso::BRL, rusty_money::iso::BSD, rusty_money::iso::BTN, rusty_money::iso::BWP, rusty_money::iso::BYN, rusty_money::iso::BYR, rusty_money::iso::BZD, rusty_money::iso::CAD, rusty_money::iso::CDF, rusty_money::iso::CHF, rusty_money::iso::CLF, rusty_money::iso::CLP, rusty_money::iso::CNY, rusty_money::iso::COP, rusty_money::iso::CRC, rusty_money::iso::CUC, rusty_money::iso::CUP, rusty_money::iso::CVE, rusty_money::iso::CZK, rusty_money::iso::DJF, rusty_money::iso::DKK, rusty_money::iso::DOP, rusty_money::iso::DZD, rusty_money::iso::EGP, rusty_money::iso::ERN, rusty_money::iso::ETB, rusty_money::iso::EUR, rusty_money::iso::FJD, rusty_money::iso::FKP, rusty_money::iso::GBP, rusty_money::iso::GEL, rusty_money::iso::GHS, rusty_money::iso::GIP, rusty_money::iso::GMD, rusty_money::iso::GNF, rusty_money::iso::GTQ, rusty_money::iso::GYD, rusty_money::iso::HKD, rusty_money::iso::HNL, rusty_money::iso::HRK, rusty_money::iso::HTG, rusty_money::iso::HUF, rusty_money::iso::IDR, rusty_money::iso::ILS, rusty_money::iso::INR, rusty_money::iso::IQD, rusty_money::iso::IRR, rusty_money::iso::ISK, rusty_money::iso::JMD, rusty_money::iso::JOD, rusty_money::iso::JPY, rusty_money::iso::KES, rusty_money::iso::KGS, rusty_money::iso::KHR, rusty_money::iso::KMF, rusty_money::iso::KPW, rusty_money::iso::KRW, rusty_money::iso::KWD, rusty_money::iso::KYD, rusty_money::iso::KZT, rusty_money::iso::LAK, rusty_money::iso::LBP, rusty_money::iso::LKR, rusty_money::iso::LRD, rusty_money::iso::LSL, rusty_money::iso::LYD, rusty_money::iso::MAD, rusty_money::iso::MDL, rusty_money::iso::MGA, rusty_money::iso::MKD, rusty_money::iso::MMK, rusty_money::iso::MNT, rusty_money::iso::MOP, rusty_money::iso::MRU, rusty_money::iso::MUR, rusty_money::iso::MVR, rusty_money::iso::MWK, rusty_money::iso::MXN, rusty_money::iso::MYR, rusty_money::iso::MZN, rusty_money::iso::NAD, rusty_money::iso::NGN, rusty_money::iso::NIO, rusty_money::iso::NOK, rusty_money::iso::NPR, rusty_money::iso::NZD, rusty_money::iso::OMR, rusty_money::iso::PAB, rusty_money::iso::PEN, rusty_money::iso::PGK, rusty_money::iso::PHP, rusty_money::iso::PKR, rusty_money::iso::PLN, rusty_money::iso::PYG, rusty_money::iso::QAR, rusty_money::iso::ROL, rusty_money::iso::RON, rusty_money::iso::RSD, rusty_money::iso::RUB, rusty_money::iso::RWF, rusty_money::iso::SAR, rusty_money::iso::SBD, rusty_money::iso::SCR, rusty_money::iso::SDG, rusty_money::iso::SEK, rusty_money::iso::SGD, rusty_money::iso::SHP, rusty_money::iso::SKK, rusty_money::iso::SLE, rusty_money::iso::SLL, rusty_money::iso::SOS, rusty_money::iso::SRD, rusty_money::iso::SSP, rusty_money::iso::STD, rusty_money::iso::STN, rusty_money::iso::SVC, rusty_money::iso::SYP, rusty_money::iso::SZL, rusty_money::iso::THB, rusty_money::iso::TJS, rusty_money::iso::TMT, rusty_money::iso::TND, rusty_money::iso::TOP, rusty_money::iso::TRY, rusty_money::iso::TTD, rusty_money::iso::TWD, rusty_money::iso::TZS, rusty_money::iso::UAH, rusty_money::iso::UGX, rusty_money::iso::USD, rusty_money::iso::UYU, rusty_money::iso::UYW, rusty_money::iso::UZS, rusty_money::iso::VED, rusty_money::iso::VES, rusty_money::iso::VND, rusty_money::iso::VUV, rusty_money::iso::WST, rusty_money::iso::XAF, rusty_money::iso::XAG, rusty_money::iso::XAU, rusty_money::iso::XBA, rusty_money::iso::XBB, rusty_money::iso::XBC, rusty_money::iso::XBD, rusty_money::iso::XCD, rusty_money::iso::XCG, rusty_money::iso::XDR, rusty_money::iso::XOF, rusty_money::iso::XPD, rusty_money::iso::XPF, rusty_money::iso::XPT, rusty_money::iso::XTS, rusty_money::iso::YER, rusty_money::iso::ZAR, rusty_money::iso::ZMK, rusty_money::iso::ZMW, rusty_money::iso::ZWG, rusty_money::iso::ZWL
];
