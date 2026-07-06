#![doc(hidden)]

use std::fmt::Display;
use boltffi::{custom_type, data};
use chrono::{DateTime, Utc};
use rust_decimal::{Decimal, prelude::FromPrimitive};
use rusty_money::{Findable as _, Locale};
use crate::{Currency, Money, price::{ALL_CURRENCIES, ParseMoneyError}};

#[derive(Clone, Copy)]
#[data]
pub struct FfiCurrency { ptr: usize }
#[data(impl)]
impl FfiCurrency {
    pub const fn name(self) -> &'static str { self.from_ffi().name }
    pub const fn code(self) -> &'static str { self.from_ffi().iso_alpha_code }
    pub const fn symbol(self) -> &'static str { self.from_ffi().symbol }

    pub fn look_up(code: &str) -> Option<Self> {
        rusty_money::iso::Currency::find(code)
            .map(|inner| Self::to_ffi(&inner))
    }

    /// Returns a slice containing all the [`Currencies`][Currency] that exist in this program.
    ///
    /// NOTE: This function should only be called *ONCE* in the entire lifetime of the program.
    /// The returned list should be cached in the memory of the native application running.
    pub fn list() -> Vec<Self> { ALL_CURRENCIES.iter().map(|c| FfiCurrency::to_ffi(c)).collect::<Vec<_>>() }
}
#[data(impl)]
impl Default for FfiCurrency {
    fn default() -> Self {
        todo!()
        // CURRENT_CURRENCY.read()
        //     .map(|lock| *lock)
        //     .unwrap_or(Currency(rusty_money::iso::USD))
    }
}
#[data(impl)]
impl Display for FfiCurrency {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(self.code())
    }
}
// #[data(impl)]
// impl FromStr for FfiCurrency {
//     type Err = ();

//     fn from_str(code: &str) -> Result<Self, Self::Err> {
//         Self::look_up(code).ok_or(())
//     }
// }
// impl Serialize for FfiCurrency {
//     fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
//     where S: serde::Serializer {
//         serializer.serialize_str(self.code())
//     }
// }
// impl<'de> Deserialize<'de> for FfiCurrency {
//     fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
//     where D: serde::Deserializer<'de> {
//         let code = deserializer.deserialize_str(StringVisitor::new("An ISO-4217 currency code"))?;
//         Self::look_up(&code)
//             .ok_or_else(|| format!("Could not find currency by code \"{code}\""))
//             .map_err(|err| serde::de::Error::custom(err))
//     }
// }
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
// #[data(impl)]
impl FfiDecimal {
    pub fn zero() -> Self { Self::to_ffi(&Decimal::ZERO) }
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
}
impl FfiDecimal {
    fn to_ffi(value: &Decimal) -> Self {
        Self { data: Vec::from(Decimal::serialize(value)) }
    }
    const fn from_ffi(self) -> Result<Decimal, boltffi::CustomTypeConversionError> {
        todo!()
        // Ok(Decimal::deserialize(self.data.try_into()
        //     .map_err(|_| boltffi::CustomTypeConversionError)?
        // ))
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
pub struct FfiMoney {
    pub amount: FfiDecimal,
    pub currency: FfiCurrency,
}
#[data(impl)]
impl FfiMoney {
    pub fn parse_value(s: &str, currency: Currency, locale: Locale) -> Result<Self, ParseMoneyError> {
        crate::price::parse_money_amount(s, currency, locale)
            .map(|money| Self::to_ffi(&money))
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

custom_type! {
    pub DateTimeUtc,
    remote = DateTime<Utc>,
    repr = i64,
    into_ffi = |value: &DateTime<Utc>| value.timestamp(),
    try_from_ffi = |value: i64| DateTime::<Utc>::from_timestamp(value, 0)
        .ok_or(boltffi::CustomTypeConversionError),
}
