mod classes;
pub mod color;
pub mod description;
pub mod items;
pub mod location;
pub mod tags;
pub mod price;

pub use classes::*;
pub use rusty_money::Locale;

pub static CURRENT_LANGUAGE: &str = "EN_US";
