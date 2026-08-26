#![doc(hidden)]

use boltffi::{custom_type, data};
use chrono::{DateTime, Utc};
use rust_decimal::{Decimal, prelude::{FromPrimitive, ToPrimitive as _}};
use rusty_money::Findable as _;
use crate::{Currency, Locale, Money, utils::{CurrencyExt as _, LocaleExt as _}};

#[data]
#[derive(Clone, Copy)]
pub struct FfiCurrency { ptr: usize }
#[data(impl)]
impl FfiCurrency {
    pub fn name(self) -> String { self.from_ffi().name.to_string() }
    pub fn code(self) -> String { self.from_ffi().iso_alpha_code.to_string() }
    pub fn symbol(self) -> String { self.from_ffi().symbol.to_string() }

    pub fn look_up(code: &str) -> Option<Self> {
        rusty_money::iso::Currency::find(code)
            .map(|inner| Self::to_ffi(&inner))
    }

    /// Returns the [`Currency`] that is currently in use by the application.
    ///
    /// This value is set by the user in the application's settings page.
    pub fn current() -> Self { Self::to_ffi(&crate::current_currency()) }
    /// Returns the [`Currency`] that is used for the given [`Locale`].
    pub fn locale_default(locale: Locale) -> Self { Self::to_ffi(&Currency::locale_default(locale)) }
    pub fn to_string(self) -> String { self.code() }

    /// Returns a slice containing all the [`Currencies`][Currency] that exist in this program.
    ///
    /// NOTE: This function should only be called *ONCE* in the entire lifetime of the program.
    /// The returned list should be cached in the memory of the native application running.
    pub fn list_all() -> Vec<Self> { Currency::list_all().iter().map(|c| FfiCurrency::to_ffi(c)).collect::<Vec<_>>() }
}
impl FfiCurrency {
    fn to_ffi(value: &Currency) -> Self {
        Self { ptr: *value as *const rusty_money::iso::Currency as usize }
    }
    const fn from_ffi(self) -> Currency {
        // SAFETY: All Currencies are a reference to a static global that lives the entire lifetime of the program, so converting ptr to reference at any point should be safe.
        unsafe { &*(self.ptr as *const rusty_money::iso::Currency) }
    }
}
custom_type! {
    pub Currency,
    remote = Currency,
    repr = FfiCurrency,
    into_ffi = FfiCurrency::to_ffi,
    try_from_ffi = |value: FfiCurrency| Ok(value.from_ffi()),
}

#[data]
pub struct FfiDecimal { data: Vec<u8> }
#[data(impl)]
impl FfiDecimal {
    pub const ZERO: Decimal = Decimal::ZERO;

    /// Get a [`Decimal`][FfiDecimal] number from a `Double` number.
    ///
    /// Will throw an error if the `Double` has too many digits,
    /// so only use this method with `Double` literals,
    /// not ones that result form an operation.
    pub fn new(n: f64) -> Self {
        Self::to_ffi(&Decimal::from_f64(n)
            .unwrap_or_else(|| panic!("Error converting f64 ({n:?}) to Decimal: too many digits"))
        )
    }

    pub fn from_str(s: &str) -> Result<Self, String> {
        Decimal::from_str_exact(s)
            .map(|val| Self::to_ffi(&val))
            .map_err(|err| err.to_string())
    }

    pub fn to_int(self) -> i32 {
        let dec = self.from_ffi()
            .expect("Error converting Decimal data from ffi");
        dec.to_i32()
            .expect(&format!("Could not convert Decimal {dec} to i32"))
    }
}
impl FfiDecimal {
    fn to_ffi(value: &Decimal) -> Self {
        Self { data: Vec::from(Decimal::serialize(value)) }
    }
    fn from_ffi(self) -> Result<Decimal, boltffi::CustomTypeConversionError> {
        Ok(Decimal::deserialize(self.data.try_into()
            .map_err(|_| boltffi::CustomTypeConversionError)?
        ))
    }
}
custom_type! {
    pub Decimal,
    remote = Decimal,
    repr = FfiDecimal,
    into_ffi = FfiDecimal::to_ffi,
    try_from_ffi = FfiDecimal::from_ffi,
}

#[data]
pub struct FfiLocale { code: String }
#[data(impl)]
impl FfiLocale {
    pub fn code(self) -> String { self.code }

    /// Returns the [`Locale`] that is currently in use by the application.
    ///
    /// This value is set by the user in the application's settings page.
    pub fn current() -> Self { Self::to_ffi(&crate::current_locale()) }

    /// Returns a slice containing all the [`Locales`][Locale] that exist in this program.
    ///
    /// NOTE: This function should only be called *ONCE* in the entire lifetime of the program.
    /// The returned list should be cached in the memory of the native application running.
    pub fn list_all() -> Vec<Self> { Locale::list_all().iter().map(|c| FfiLocale::to_ffi(c)).collect::<Vec<_>>() }
}
impl FfiLocale {
    fn to_ffi(value: &Locale) -> Self {
        Self { code: value.name().to_string() }
    }
    fn from_ffi(self) -> Result<Locale, boltffi::CustomTypeConversionError> {
        Locale::from_name(&self.code)
            .map_err(|_| boltffi::CustomTypeConversionError)
    }
}
custom_type! {
    pub Locale,
    remote = Locale,
    repr = FfiLocale,
    into_ffi = FfiLocale::to_ffi,
    try_from_ffi = FfiLocale::from_ffi,
}

#[data]
pub struct FfiMoney {
    pub amount: FfiDecimal,
    pub currency: FfiCurrency,
}
#[data(impl)]
impl FfiMoney {
    pub fn parse_value(s: &str, currency: Currency, locale: Locale) -> Result<Self, String> {
        crate::price::parse_money_amount(s, currency, locale)
            .map(|money| Self::to_ffi(&money))
            .map_err(|err| err.to_string())
    }

    pub fn format(self, locale: Locale, include_symbol: bool) -> String {
        crate::price::format_money(&self.from_ffi().expect("Error converting FfiMoney to Money"), locale, include_symbol)
    }

    pub fn from_decimal(amount: Decimal, currency: Currency) -> Self {
        Self::to_ffi(&Money::from_decimal(amount, currency))
    }
}
impl FfiMoney {
    fn to_ffi(value: &Money) -> Self {
        Self { amount: FfiDecimal::to_ffi(value.amount()), currency: FfiCurrency::to_ffi(&value.currency()) }
    }
    fn from_ffi(self) -> Result<Money, boltffi::CustomTypeConversionError> {
        Ok(Money::from_decimal(self.amount.from_ffi()?, self.currency.from_ffi()))
    }
}
custom_type! {
    pub Money,
    remote = Money,
    repr = FfiMoney,
    into_ffi = FfiMoney::to_ffi,
    try_from_ffi = FfiMoney::from_ffi,
}

custom_type! {
    pub DateTimeUtc,
    remote = DateTime<Utc>,
    repr = i64,
    into_ffi = |value: &DateTime<Utc>| value.timestamp(),
    try_from_ffi = |value: i64| DateTime::<Utc>::from_timestamp(value, 0)
        .ok_or(boltffi::CustomTypeConversionError),
}
