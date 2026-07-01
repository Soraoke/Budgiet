use boltffi::{CustomFfiConvertible, custom_ffi, custom_type, data};
use rust_decimal::Decimal;
use rusty_money::{Findable as _, Locale};

use crate::price::ParseMoneyError;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Currency(pub &'static rusty_money::iso::Currency);
#[data(impl)]
impl Currency {
    pub fn name(&self) -> &str { self.0.name }
    pub fn code(&self) -> &str { self.0.iso_alpha_code }
    pub fn symbol(&self) -> &str { self.0.symbol }

    /// Returns a slice containing all the [`Currencies`][Currency] that exist in this program.
    ///
    /// NOTE: This function should only be called *ONCE* in the entire lifetime of the program.
    /// The returned list should be cached in the memory of the native application running.
    pub fn list() -> &'static [Currency] { todo!() }
}
#[custom_ffi]
impl CustomFfiConvertible for Currency {
    type FfiRepr = String;
    type Error = boltffi::CustomTypeConversionError;

    fn into_ffi(&self) -> Self::FfiRepr {
        self.0.iso_alpha_code.to_string()
    }
    fn try_from_ffi(repr: Self::FfiRepr) -> Result<Self, Self::Error> {
        Ok(Currency(rusty_money::iso::Currency::find(&repr).ok_or(boltffi::CustomTypeConversionError)?))
    }
}


#[data]
#[derive(Debug, Clone, Copy)]
pub struct Money {
    pub amount: Decimal,
    pub currency: Currency,
}
#[data(impl)]
impl Money {
    pub fn parse_value(s: &str, currency: Currency, locale: Locale) -> Result<Money, ParseMoneyError> {
        crate::price::parse_money_amount(s, currency, locale)
    }

    pub fn format(&self, locale: Locale, include_symbol: bool) -> String {
        crate::price::format_money(self, locale, include_symbol)
    }

    #[inline]
    pub fn from_decimal(amount: Decimal, currency: Currency) -> Self {
        Self { amount, currency }
    }
}

custom_type! {
    pub Decimal,
    remote = rust_decimal::Decimal,
    repr = Vec<u8>,
    into_ffi = |value: &Decimal| Vec::from(Decimal::serialize(value)),
    try_from_ffi = |bytes: Vec<u8>| Ok(Decimal::deserialize(bytes.try_into()
        .map_err(|_| boltffi::CustomTypeConversionError)?
    )),
}
custom_type! {
    pub Locale,
    remote = Locale,
    repr = u8,
    into_ffi = |value: &Locale| match value {
        Locale::EnUs => 0,
        Locale::EnIn => 1,
        Locale::EnEu => 2,
        Locale::EnBy => 3,
    },
    try_from_ffi = |value: u8| match value {
        0 => Ok(Locale::EnUs),
        1 => Ok(Locale::EnIn),
        2 => Ok(Locale::EnEu),
        3 => Ok(Locale::EnBy),
        _ => Err(boltffi::CustomTypeConversionError),
    },
}
