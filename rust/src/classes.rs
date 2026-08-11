#![doc(hidden)]

use std::{fmt::Display, todo};
use chrono::{DateTime, Utc};
use rust_decimal::{Decimal, prelude::{FromPrimitive, ToPrimitive as _}};
use rusty_money::Findable as _;
use uniffi::{Record, custom_type, export};
use crate::{Currency, Locale, Money, MyError, current_currency, current_locale, price::{ALL_CURRENCIES, ParseMoneyError}};

/// Returns a slice containing all the [`Currencies`][Currency] that exist in this program.
///
/// NOTE: This function should only be called *ONCE* in the entire lifetime of the program.
/// The returned list should be cached in the memory of the native application running.
#[export]
fn list_all_currencies() -> Vec<Currency> {
    ALL_CURRENCIES.to_vec()
}

#[derive(Record, Clone, Copy)]
// #[export(Display)]
pub struct FfiCurrency { ptr: u64 }
#[export]
impl FfiCurrency {
    pub fn name(self) -> String { <Self as Into<Currency>>::into(self).name.to_string() }
    pub fn code(self) -> String { <Self as Into<Currency>>::into(self).iso_alpha_code.to_string() }
    pub fn symbol(self) -> String { <Self as Into<Currency>>::into(self).symbol.to_string() }
}
impl Display for FfiCurrency {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.code())
    }
}
impl From<Currency> for FfiCurrency {
    fn from(value: Currency) -> Self {
        Self { ptr: value as *const rusty_money::iso::Currency as usize as u64 }
    }
}
impl Into<Currency> for FfiCurrency {
    fn into(self) -> Currency {
        // SAFETY: All Currencies are a reference to a static global that lives the entire lifetime of the program, so converting ptr to reference at any point should be safe.
        unsafe { &*(self.ptr as *const rusty_money::iso::Currency) }
    }
}
custom_type!(Currency, FfiCurrency, { remote });

#[derive(Record)]
pub struct FfiDecimal { data: Vec<u8> }
#[export]
impl FfiDecimal {
    pub fn to_int(self) -> i32 {
        let dec = <Self as Into<Decimal>>::into(self);
        dec.to_i32()
            .expect(&format!("Could not convert Decimal {dec} to i32"))
    }
}
impl From<Decimal> for FfiDecimal {
    fn from(value: Decimal) -> Self {
        Self { data: Vec::from(Decimal::serialize(&value)) }
    }
}
impl Into<Decimal> for FfiDecimal {
    fn into(self) -> Decimal {
        Decimal::deserialize(self.data.try_into()
            .unwrap_or_else(|vec: Vec<u8>| panic!("Error converting Decimal from foreign language: Array must contain 16 bytes, but it had {}", vec.len()))
        )
    }
}
custom_type!(Decimal, FfiDecimal, { remote });

#[derive(Record)]
pub struct FfiMoney {
    pub amount: FfiDecimal,
    pub currency: FfiCurrency,
}
#[export]
impl FfiMoney {
    pub fn format(self, locale: Locale, include_symbol: bool) -> String {
        crate::price::format_money(&self.into(), locale, include_symbol)
    }
}
impl From<Money> for FfiMoney {
    fn from(value: Money) -> Self {
        Self { amount: FfiDecimal::from(*value.amount()), currency: FfiCurrency::from(value.currency()) }
    }
}
impl Into<Money> for FfiMoney {
    fn into(self) -> Money {
        Money::from_decimal(self.amount.into(), self.currency.into())
    }
}
custom_type!(Money, FfiMoney, { remote });

#[derive(Record)]
pub struct FfiLocale { code: String }
#[export]
impl FfiLocale {
    pub fn code(self) -> String { self.code }
}
impl From<Locale> for FfiLocale {
    fn from(value: Locale) -> Self {
        Self { code: value.name().to_string() }
    }
}
impl Into<Locale> for FfiLocale {
    fn into(self) -> Locale {
        Locale::from_name(&self.code)
            .unwrap_or_else(|err| panic!("Error converting Locale from foreign language: {err}"))
    }
}
custom_type!(Locale, FfiLocale, { remote });

type DateTimeUtc = DateTime<Utc>;
custom_type!(DateTimeUtc, i64, {
    remote,
    lower: |value: DateTime<Utc>| value.timestamp(),
    try_lift: |value: i64| DateTime::<Utc>::from_timestamp(value, 0)
        .ok_or(uniffi::deps::anyhow::Error::msg(format!("Error converting DateTimeUtc from foreign language: seconds/nanoseconds out of range"))),
});

// --- EXPORT Assiciated functions ---

#[export]
#[doc(hidden)]
fn currency_look_up(code: &str) -> Option<Currency> {
    rusty_money::iso::Currency::find(code)
        .map(|inner| inner.into())
}
#[export]
#[doc(hidden)]
/// Returns the [`Currency`] that is currently in use by the application.
///
/// This value is set by the user in the application's settings page.
fn currency_current() -> Currency {
    current_currency()
}
#[export]
#[doc(hidden)]
fn currency_locale_default(locale: Locale) -> Currency {
    todo!()
}

#[export]
#[doc(hidden)]
fn decimal_zero() -> Decimal { Decimal::ZERO }
/// Get a [`Decimal`][FfiDecimal] number from a `Double` number.
///
/// Will throw an error if the `Double` has too many digits,
/// so only use this method with `Double` literals,
/// not ones that result form an operation.
#[export]
#[doc(hidden)]
fn decimal_new(n: f64) -> Decimal {
    Decimal::from_f64(n)
        .expect(&format!("Error converting f64 ({n:?}) to Decimal: too many digits"))
}
#[export]
#[doc(hidden)]
fn decimal_from_str(s: &str) -> Result<Decimal, MyError> {
    Decimal::from_str_exact(s)
        .map_err(|err| err.to_string().into())
}

#[export]
#[doc(hidden)]
fn money_from_decimal(amount: Decimal, currency: Currency) -> Money {
    Money::from_decimal(amount, currency)
}
#[export]
#[doc(hidden)]
fn money_parse_value(s: &str, currency: Currency, locale: Locale) -> Result<Money, ParseMoneyError> {
    crate::price::parse_money_amount(s, currency, locale)
}

/// Returns the [`Locale`] that is currently in use by the application.
///
/// This value is set by the user in the application's settings page.
#[export]
#[doc(hidden)]
fn locale_current() -> Locale {
    current_locale()
}
