#[doc(hidden)]
pub mod classes;
pub mod db;
pub mod color;
pub mod description;
pub mod items;
pub mod location;
pub mod price;
pub mod tags;
pub mod transaction;
pub mod utils;

use std::sync::RwLock;
use rusty_money::iso::USD;
pub use num_format::Locale;

uniffi::setup_scaffolding!();

pub type Currency = &'static rusty_money::iso::Currency;
pub type Money = rusty_money::Money<'static, rusty_money::iso::Currency>;

static CURRENT_LANGUAGE: RwLock<&str> = RwLock::new("EN_US");
pub fn current_language() -> &'static str {
    *CURRENT_LANGUAGE.read()
        // Is doing this slop??
        .unwrap_or_else(|lock| {
            CURRENT_LANGUAGE.clear_poison();
            lock.into_inner()
        })
}

static CURRENT_LOCALE: RwLock<Locale> = RwLock::new(Locale::en_US_POSIX);
pub fn current_locale() -> Locale {
    *CURRENT_LOCALE.read()
        // Is doing this slop??
        .unwrap_or_else(|lock| {
            CURRENT_LOCALE.clear_poison();
            lock.into_inner()
        })
}

static CURRENT_CURRENCY: RwLock<Currency> = RwLock::new(USD);
pub fn current_currency() -> Currency {
    *CURRENT_CURRENCY.read()
        // Is doing this slop??
        .unwrap_or_else(|lock| {
            CURRENT_CURRENCY.clear_poison();
            lock.into_inner()
        })
}
