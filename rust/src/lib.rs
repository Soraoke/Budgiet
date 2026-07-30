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

pub type Currency = &'static rusty_money::iso::Currency;
pub type Money = rusty_money::Money<'static, rusty_money::iso::Currency>;

pub static CURRENT_LANGUAGE: RwLock<&str> = RwLock::new("EN_US");
pub static CURRENT_LOCALE: RwLock<Locale> = RwLock::new(Locale::en_US_POSIX);
pub static CURRENT_CURRENCY: RwLock<Currency> = RwLock::new(USD);
