use std::{fmt::Display, str::FromStr as _};
use boltffi::export;
use itertools::Itertools as _;
use num_format::ToFormattedString as _;
use rust_decimal::Decimal;
use thiserror::Error;
use crate::{Currency, Locale, Money};

#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub struct ParseMoneyError {
    pub currency: Currency,
    pub locale: Locale,
    pub kind: ParseMoneyErrorKind,
}
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ParseMoneyErrorKind {
    Empty,
    LeadingZeroes,
    InvalidChars { list: Vec<char> },
    IncorrectGroupSep { found_sep: char },
    InvalidGroupSize,
    IncorrectDecimalSep { found_sep: char },
    MultipleDecimalSeps,
    TooManyFractionalDigits { found_digits: u32 },
    GroupSepInFractionalSection,
}
impl Display for ParseMoneyError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        #![allow(unstable_name_collisions)]
        let currency_code = self.currency.iso_alpha_code;
        let currency_minor_digits = self.currency.exponent;
        let decimal_sep = self.locale.decimal();
        let group_sep = self.locale.separator();

        f.write_str(&match &self.kind {
            ParseMoneyErrorKind::Empty
                => format!("price string must not be empty"),
            ParseMoneyErrorKind::LeadingZeroes
                => format!("Leading un-fractional 0s are not allowed"),
            ParseMoneyErrorKind::InvalidChars { list }
                => format!("Invalid characters: {}", list.iter().map(char::to_string).intersperse(" ".to_string()).collect::<String>()),
            ParseMoneyErrorKind::IncorrectGroupSep { found_sep }
                => format!("Your locale uses '{group_sep}' as a group separator instead of {found_sep}"),
            ParseMoneyErrorKind::InvalidGroupSize
                => format!("Digits must be in groups of 3 if using a group separator ('{group_sep}')"),
            ParseMoneyErrorKind::IncorrectDecimalSep { found_sep }
                => format!("Your locale uses '{decimal_sep}' as a decimal separator instead of {found_sep}"),
            ParseMoneyErrorKind::MultipleDecimalSeps
                => format!("There can only be 1 decimal separator ('{decimal_sep}')"),
            ParseMoneyErrorKind::TooManyFractionalDigits { found_digits }
                => format!("Your currency ({currency_code}) uses up to {currency_minor_digits} decimal places, but found {found_digits}"),
            ParseMoneyErrorKind::GroupSepInFractionalSection
                => format!("Group separators ('{group_sep}') are not allowed after the decimal separator ('{decimal_sep}')"),
        })
    }
}
impl ParseMoneyErrorKind {
    /// Convenience method for the parser function.
    fn new(self, currency: Currency, locale: Locale) -> Result<Money, ParseMoneyError> {
        Err(ParseMoneyError { currency, locale, kind: self })
    }
}

// This method is courtesy of my dear friend and brother [Asder](https://github.com/asder8215/)
pub fn parse_money_amount(price: &str, currency: Currency, locale: Locale) -> Result<Money, ParseMoneyError> {
    if price.is_empty() {
        return ParseMoneyErrorKind::Empty.new(currency, locale);
    }

    let possible_decimal_seps = &['.', ',', '\u{066B}'];
    // How many decimal places are allowed by the currency.
    // (e.g. USD uses 2 decimal places, crypto uses a variety number of decimal places).
    let exponent = currency.exponent;
    // Separates the decimal digits from unit digits (locale-specific) (i.e. ',', '.').
    let decimal_sep = locale.decimal();
    // Separate digits in the thousands (local-specific).
    let group_sep = locale.separator();

    let mut prev_digit: Option<char> = None;
    // idx of the decimal point in the price string
    let mut decimal_idx: Option<usize> = None;
    // Gets a value when first group separator is found; resets to 0
    let mut group_size: Option<usize> = None;
    // Keeps track of any invalid characters in the price string.
    let mut invalid_chars = vec![];

    for (idx, c) in price.chars().enumerate() {
        if c.is_digit(10) {
            if prev_digit == Some('0')
            && decimal_idx == None
            && idx == 1 {
                // 0100 is not allowed, 0.00 is allowed
                return ParseMoneyErrorKind::LeadingZeroes.new(currency, locale);
            }
            if let Some(group_size) = &mut group_size
            && decimal_idx.is_none() {
                *group_size += 1;
                if *group_size > 3 {
                    return ParseMoneyErrorKind::InvalidGroupSize.new(currency, locale);
                }
            }
            prev_digit = Some(c);
        } else if c.to_string() == decimal_sep {
            // Has more than one decimal point...
            if decimal_idx.is_none() {
                // might be trying to use it as group separator.
                return if group_size.is_none()
                && decimal_idx.is_some_and(|n| idx - n > 3) {
                    ParseMoneyErrorKind::IncorrectGroupSep { found_sep: c }
                } else {
                    ParseMoneyErrorKind::MultipleDecimalSeps
                }.new(currency, locale);
            }
            // if more digits used after decimal (i.e. 100.000 USD), return an error.
            let decimal_digits = (price.len() - idx - 1) as u32;
            if decimal_digits > exponent {
                return ParseMoneyErrorKind::TooManyFractionalDigits { found_digits: decimal_digits }.new(currency, locale);
            }
            // Opened group (with separator), but group does not contain enough digits.
            if group_size.is_some_and(|n| n != 3) {
                return ParseMoneyErrorKind::InvalidGroupSize.new(currency, locale);
            }
            decimal_idx = Some(idx);
        } else if c.to_string() == group_sep {
            if decimal_idx.is_some() {
                return ParseMoneyErrorKind::GroupSepInFractionalSection.new(currency, locale);
            }
            if group_size.is_some_and(|n| n != 3) {
                return ParseMoneyErrorKind::InvalidGroupSize.new(currency, locale);
            }
            group_size = Some(0);
        } else if possible_decimal_seps.contains(&c) && decimal_idx.is_none() {
            return ParseMoneyErrorKind::IncorrectDecimalSep { found_sep: c }.new(currency, locale);
        } else {
            invalid_chars.push(c);
        }
    }

    // price string contained invalid characters.
    if !invalid_chars.is_empty() {
        return ParseMoneyErrorKind::InvalidChars { list: invalid_chars }.new(currency, locale);
    }

    // Opened group (with separator), but group does not contain enough digits.
    if group_size.is_some_and(|n| n != 3 && n != 0) {
        return ParseMoneyErrorKind::InvalidGroupSize.new(currency, locale);
    }

    // Price string was validated, now convert it to a string that can be parsed
    let price = price.replace(decimal_sep, ".");
    let price = price.replace(group_sep, "");
    let price = price.trim();

    Ok(Money::from_decimal(Decimal::from_str(price).unwrap(), currency))
}

// TODO: doc; does not return ParseMoneyError::Empty
//   This function should take full next text value, input key, input position;
//   and should return the transformed field value, and whether there should be a delay before applying it.
#[export]
pub fn validate_money_field_input(s: &str, currency: Currency, locale: Locale) -> Result<Money, String> {
    if s.is_empty() {
        Ok(Money::from_decimal(Decimal::ZERO, currency))
    } else {
        let price = s.chars()
            .filter(|&c| c.to_string() != locale.separator())
            .collect::<String>();
        parse_money_amount(&price, currency, locale)
            .map_err(|err| err.to_string())
    }
}

/// Formats the price value according to the [`Currency`] and [`Locale`] being used.
///
/// Rounds *decimal digits* to the *nearest* allowed digits.
/// For example, [`USD`][rusty_money::iso::USD] allows 2 decimal digits,
/// so `1.345` is rounded to `1.35`, and `1.344` is rounded to `1.34`.
///
/// This returns the **price** formatted with the *decimal point* (if applicable) and *digit separators*.
/// The returned string *does not* include the currency symbol, as that is displayed as a separate `Icon`.
pub fn format_money(money: &Money, locale: Locale, include_symbol: bool) -> String {
    let mut buf = String::new();
    let currency = money.currency();
    let amount = money.amount()
        .round_dp_with_strategy(currency.exponent, rust_decimal::RoundingStrategy::MidpointAwayFromZero)
        .normalize();

    if currency.symbol_first && include_symbol {
        buf.push_str(currency.symbol);
    }

    buf.push_str(&amount.as_i128().to_formatted_string(&locale));
    if currency.exponent > 0 {
        buf.push_str(locale.decimal());

        let fract = amount.fract().as_i128();
        // Add trailing Zeros to the fractional part to fill the currency's minor-unit digits.
        let width = currency.exponent as usize;
        buf.push_str(&format!("{fract:0>width$}"));
    }

    if !currency.symbol_first && include_symbol {
        buf.push_str(currency.symbol);
    }

    buf
}

pub(crate) static XXX: Currency = &rusty_money::iso::Currency {
    iso_alpha_code: "XXX",
    iso_numeric_code: "999",
    name: "Unknown",
    exponent: 2,
    minor_units: 1,
    locale: rusty_money::Locale::EnUs,
    symbol: "$",
    symbol_first: true,
};

pub static ALL_CURRENCIES: &[Currency] = &[
    XXX,
    rusty_money::iso::AED, rusty_money::iso::AFN, rusty_money::iso::ALL, rusty_money::iso::AMD, rusty_money::iso::ANG, rusty_money::iso::AOA, rusty_money::iso::ARS, rusty_money::iso::AUD, rusty_money::iso::AWG, rusty_money::iso::AZN, rusty_money::iso::BAM, rusty_money::iso::BBD, rusty_money::iso::BDT, rusty_money::iso::BGN, rusty_money::iso::BHD, rusty_money::iso::BIF, rusty_money::iso::BMD, rusty_money::iso::BND, rusty_money::iso::BOB, rusty_money::iso::BRL, rusty_money::iso::BSD, rusty_money::iso::BTN, rusty_money::iso::BWP, rusty_money::iso::BYN, rusty_money::iso::BYR, rusty_money::iso::BZD, rusty_money::iso::CAD, rusty_money::iso::CDF, rusty_money::iso::CHF, rusty_money::iso::CLF, rusty_money::iso::CLP, rusty_money::iso::CNY, rusty_money::iso::COP, rusty_money::iso::CRC, rusty_money::iso::CUC, rusty_money::iso::CUP, rusty_money::iso::CVE, rusty_money::iso::CZK, rusty_money::iso::DJF, rusty_money::iso::DKK, rusty_money::iso::DOP, rusty_money::iso::DZD, rusty_money::iso::EGP, rusty_money::iso::ERN, rusty_money::iso::ETB, rusty_money::iso::EUR, rusty_money::iso::FJD, rusty_money::iso::FKP, rusty_money::iso::GBP, rusty_money::iso::GEL, rusty_money::iso::GHS, rusty_money::iso::GIP, rusty_money::iso::GMD, rusty_money::iso::GNF, rusty_money::iso::GTQ, rusty_money::iso::GYD, rusty_money::iso::HKD, rusty_money::iso::HNL, rusty_money::iso::HRK, rusty_money::iso::HTG, rusty_money::iso::HUF, rusty_money::iso::IDR, rusty_money::iso::ILS, rusty_money::iso::INR, rusty_money::iso::IQD, rusty_money::iso::IRR, rusty_money::iso::ISK, rusty_money::iso::JMD, rusty_money::iso::JOD, rusty_money::iso::JPY, rusty_money::iso::KES, rusty_money::iso::KGS, rusty_money::iso::KHR, rusty_money::iso::KMF, rusty_money::iso::KPW, rusty_money::iso::KRW, rusty_money::iso::KWD, rusty_money::iso::KYD, rusty_money::iso::KZT, rusty_money::iso::LAK, rusty_money::iso::LBP, rusty_money::iso::LKR, rusty_money::iso::LRD, rusty_money::iso::LSL, rusty_money::iso::LYD, rusty_money::iso::MAD, rusty_money::iso::MDL, rusty_money::iso::MGA, rusty_money::iso::MKD, rusty_money::iso::MMK, rusty_money::iso::MNT, rusty_money::iso::MOP, rusty_money::iso::MRU, rusty_money::iso::MUR, rusty_money::iso::MVR, rusty_money::iso::MWK, rusty_money::iso::MXN, rusty_money::iso::MYR, rusty_money::iso::MZN, rusty_money::iso::NAD, rusty_money::iso::NGN, rusty_money::iso::NIO, rusty_money::iso::NOK, rusty_money::iso::NPR, rusty_money::iso::NZD, rusty_money::iso::OMR, rusty_money::iso::PAB, rusty_money::iso::PEN, rusty_money::iso::PGK, rusty_money::iso::PHP, rusty_money::iso::PKR, rusty_money::iso::PLN, rusty_money::iso::PYG, rusty_money::iso::QAR, rusty_money::iso::ROL, rusty_money::iso::RON, rusty_money::iso::RSD, rusty_money::iso::RUB, rusty_money::iso::RWF, rusty_money::iso::SAR, rusty_money::iso::SBD, rusty_money::iso::SCR, rusty_money::iso::SDG, rusty_money::iso::SEK, rusty_money::iso::SGD, rusty_money::iso::SHP, rusty_money::iso::SKK, rusty_money::iso::SLE, rusty_money::iso::SLL, rusty_money::iso::SOS, rusty_money::iso::SRD, rusty_money::iso::SSP, rusty_money::iso::STD, rusty_money::iso::STN, rusty_money::iso::SVC, rusty_money::iso::SYP, rusty_money::iso::SZL, rusty_money::iso::THB, rusty_money::iso::TJS, rusty_money::iso::TMT, rusty_money::iso::TND, rusty_money::iso::TOP, rusty_money::iso::TRY, rusty_money::iso::TTD, rusty_money::iso::TWD, rusty_money::iso::TZS, rusty_money::iso::UAH, rusty_money::iso::UGX, rusty_money::iso::USD, rusty_money::iso::UYU, rusty_money::iso::UYW, rusty_money::iso::UZS, rusty_money::iso::VED, rusty_money::iso::VES, rusty_money::iso::VND, rusty_money::iso::VUV, rusty_money::iso::WST, rusty_money::iso::XAF, rusty_money::iso::XAG, rusty_money::iso::XAU, rusty_money::iso::XBA, rusty_money::iso::XBB, rusty_money::iso::XBC, rusty_money::iso::XBD, rusty_money::iso::XCD, rusty_money::iso::XCG, rusty_money::iso::XDR, rusty_money::iso::XOF, rusty_money::iso::XPD, rusty_money::iso::XPF, rusty_money::iso::XPT, rusty_money::iso::XTS, rusty_money::iso::YER, rusty_money::iso::ZAR, rusty_money::iso::ZMK, rusty_money::iso::ZMW, rusty_money::iso::ZWG, rusty_money::iso::ZWL
];
