pub mod recent_items;
pub mod dispatch;
// NOTE: This file is generated using the pre-build script.
mod locale_currency_map;

use std::{fmt::Display, sync::LazyLock};
use rusty_money::Findable;
use serde::de::Visitor;
use crate::{Currency, Locale, Money, price::ParseMoneyError};

pub trait CurrencyExt {
    /// Returns the [`Currency`] that is currently in use by the application.
    ///
    /// This value is set by the user in the application's settings page.
    fn current() -> Currency;
    /// Returns the [`Currency`] that is used for the given [`Locale`].
    fn locale_default(locale: Locale) -> Currency;
    /// Returns a slice containing all the [`Currencies`][Currency] that exist in this program.
    fn list_all() -> &'static [Currency];
}
impl CurrencyExt for Currency {
    #[inline(always)]
    fn current() -> Currency { crate::current_currency() }
    fn locale_default(locale: Locale) -> Currency {
        locale.name()
            // Regions are always 2 uppercase ASCII characters
            .split(|c| c == '_' || c == '-')
            .find(|s| s.len() == 2 && s.chars().all(|c| c >= 'A' && c <= 'Z'))
            .and_then(|region| locale_currency_map::LOCALE_CURRENCY_MAP.get(region))
            .and_then(|code| rusty_money::iso::Currency::find(code))
            .unwrap_or(XXX)
    }
    #[inline(always)]
    fn list_all() -> &'static [Currency] { crate::price::ALL_CURRENCIES }
}

static XXX: Currency = &rusty_money::iso::Currency {
    iso_alpha_code: "XXX",
    iso_numeric_code: "999",
    name: "Unknown",
    exponent: 2,
    minor_units: 1,
    locale: rusty_money::Locale::EnUs,
    symbol: "$",
    symbol_first: true,
};

pub trait MoneyExt: Sized {
    fn parse_value(s: &str, currency: Currency, locale: Locale) -> Result<Self, ParseMoneyError>;
    fn format(self, locale: Locale, include_symbol: bool) -> String;
}
impl MoneyExt for Money {
    #[inline(always)]
    fn parse_value(s: &str, currency: Currency, locale: Locale) -> Result<Self, ParseMoneyError> {
        crate::price::parse_money_amount(s, currency, locale)
    }
    #[inline(always)]
    fn format(self, locale: Locale, include_symbol: bool) -> String {
        crate::price::format_money(&self, locale, include_symbol)
    }
}

pub trait LocaleExt {
    /// Returns the [`Locale`] that is currently in use by the application.
    ///
    /// This value is set by the user in the application's settings page.
    fn current() -> Locale;
    /// Returns the [`Currency`] that is used for the given [`Locale`].
    fn default_currency(&self) -> Currency;
    /// Returns a slice containing all the [`Locales`][Locale] that exist in this program.
    fn list_all() -> &'static [Locale];
}
impl LocaleExt for Locale {
    #[inline(always)]
    fn current() -> Locale { crate::current_locale() }
    #[inline(always)]
    fn default_currency(&self) -> Currency {
        Currency::locale_default(*self)
    }
    fn list_all() -> &'static [Locale] {
        static ALL_LOCALES: LazyLock<Box<[Locale]>> = LazyLock::new(|| {
            Locale::available_names()
                .iter()
                .map(|name| Locale::from_name(name)
                    .unwrap_or_else(|err| panic!("Could not parse Locale name \"{name}\" provided by 'Locale::available_names()': {err}"))
                )
                .collect()
        });
        ALL_LOCALES.as_ref()
    }
}

/// A generic implementation of [`Visitor`] for types that want to implement [`Deserialize`] by parsing a [`str`].
pub struct StringVisitor {
    pub expecting_desc: String,
}
impl StringVisitor {
    #[inline(always)]
    pub fn new(expecting: impl Display) -> Self {
        Self { expecting_desc: expecting.to_string() }
    }
}
impl<'de> Visitor<'de> for StringVisitor {
    type Value = String;

    fn expecting(&self, formatter: &mut std::fmt::Formatter) -> std::fmt::Result {
        formatter.write_str(&self.expecting_desc)
    }

    fn visit_str<Err>(self, v: &str) -> Result<Self::Value, Err>
    where Err: serde::de::Error {
        Ok(v.to_string())
    }
    fn visit_string<Err>(self, v: String) -> Result<Self::Value, Err>
    where Err: serde::de::Error {
        Ok(v)
    }
}
