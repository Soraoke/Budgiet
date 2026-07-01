#![allow(unstable_name_collisions)]

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
    todo!("Parse {price:?} with {currency:?} and {locale:?}")
}

// TODO: doc; does not return ParseMoneyError::Empty
pub fn validate_money_field_input(s: &str, currency: Currency, locale: Locale) -> Result<Money, ParseMoneyError> {
    todo!("Parse {s:?} with {currency:?} and {locale:?}, checking keystrokes")
}


pub fn format_money(money: &Money, locale: Locale, include_symbol: bool) -> String {
    todo!("Format {money:?} with {locale:?}, optionally including the currency symbol ({include_symbol:?})")
}
