use std::{fmt::Display, format, str::FromStr, sync::LazyLock};
use boltffi::{data, export};
use rust_decimal::Decimal;
use crate::price::{format_money, validate_money_field_input};
use crate::{Currency, Locale, Money};

/// Returns a slice containing sample [`Items`][Item] that are used for demos and App Tests.
pub fn fake_items() -> &'static [Item] { &*__FAKE_ITEMS }
#[doc(hidden)]
static __FAKE_ITEMS: LazyLock<[Item; 5]> = LazyLock::new(|| [
    Item::new_fake("Ham", 5.99, Amount::Units(1)),
    Item::new_fake("Cheese", 2.59, Amount::new_fake_measured(1.0, "lbs")),
    Item::new_fake("Bread", 4.19, Amount::Units(2)),
    Item::new_fake("Crackers", 1.89, Amount::Units(1)),
    Item::new_fake("Chicken", 4.99, Amount::new_fake_measured(3.5, "lbs")),
]);
/// Returns an array containing sample [`Items`][Item] that are used for demos and App Tests.
///
/// NOTE: This function should only be called *ONCE* in the entire lifetime of the program.
/// The returned list should be cached in the memory of the native application running.
#[export]
pub fn get_fake_items() -> Vec<Item> { fake_items().to_vec() }

/// Calculate the total cost of *all [`Items`][Item]* combined, plus the [`Tax`] amount.
#[export]
pub fn total_price(items: &[Item], tax: Tax) -> Decimal {
    let items_sum: Decimal = items.iter()
        .map(|item| item.total_price())
        .sum();

    let tax_amount = match tax {
        Tax::CurrencyAmount(amount) => amount,
        Tax::Percentage(percent) => items_sum * percent * Decimal::from_f32_retain(0.01).unwrap()
    };

    items_sum + tax_amount
}

/// Produces a message to display in the `NewTransactionForm` `ItemsField`.
/// Includes the number of items, their total cost, and the tax amount/percentage.
#[export]
pub fn display_items_field_info(
    items: &[Item],
    tax: Tax,
    currency: Currency,
    locale: Locale,
) -> String {
    let items_count: u32 = items.iter()
        .map(|item| match &item.amount {
            // Consider item collections with measurements as a single item.
            Amount::Measured { .. } => 1,
            Amount::Units(value) => *value,
        })
        .sum();
    let items_price = items.iter()
        .map(|item| item.total_price())
        .fold(Decimal::ZERO, |acc, amount| acc + amount);

    format!("{items_count} {items_word} ({items_price}){display_tax}",
        items_word = if items_count == 1 { "item" } else { "items" },
        items_price = format_money(&Money::from_decimal(items_price, currency), locale, true),
        display_tax = if tax.value() != Decimal::ZERO && items_count != 0 {
            match tax {
                Tax::CurrencyAmount(tax_amount) => format!(" + {} tax", format_money(&Money::from_decimal(tax_amount, currency), locale, true)),
                Tax::Percentage(percent) => format!(" + {percent}% tax"),
            }
        } else { String::new() },
    )
}

#[data]
#[derive(Debug, Clone)]
pub struct Item {
    pub name: String,
    pub unit_price: Decimal,
    pub amount: Amount,
}
#[data(impl)]
impl Item {
    fn new_fake(name: &str, unit_price: f64, amount: Amount) -> Self {
        Self {
            name: name.to_string(),
            unit_price: Decimal::from_f64_retain(unit_price).unwrap(),
            amount,
        }
    }

    /// Calculate how much *this* single [`Item`] entry costs based on its **`unit_price`** and **`amount`**.
    pub fn total_price(&self) -> Decimal {
        match &self.amount {
            Amount::Measured { value, .. } => self.unit_price * value,
            Amount::Units(value) => self.unit_price * Decimal::from(*value),
        }
    }

    /// Check that the provided **`name`** can be used for a *new or edited [`Item`]*,
    pub fn validate_name(existing_items: &[Item], name: &str, is_new: bool) -> Result<(), String> {
        let name_exists = existing_items.iter()
            .find(|item| item.name == name)
            .is_some();

        if name.is_empty() {
            Err("Name must not be empty".into())
        } else if is_new && name_exists {
            Err("An item with this name already exists. Edit the price/amount of that item instead.".into())
        } else {
            Ok(())
        }
    }
}

#[data]
#[derive(Debug, Clone)]
pub enum Amount {
    Measured { value: Decimal, label: String },
    Units(u32),
}
/// A lone *discriminant* value for the [`Amount`] type.
#[data]
#[derive(Debug, Clone, Copy)]
pub enum AmountType { Measured, Units }

#[data(impl)]
impl Amount {
    /// The maximum number of *characters* that the **`label`** field can have.
    const LABEL_CHAR_LIMIT: usize = 7;

    fn new_fake_measured(value: f64, label: &str) -> Self {
        Self::Measured { value: Decimal::from_f64_retain(value).unwrap(), label: label.to_string() }
    }

    /// Check that the provided **`label`** can be submitted to the database.
    pub fn validate_label(label: String) -> Result<(), String> {
        if label.is_empty() {
            Err("label must not be empty.".into())
        } else if label.len() > Self::LABEL_CHAR_LIMIT {
            Err(format!("The length of the label for an Amount must not exceed {} characters.", Self::LABEL_CHAR_LIMIT))
        } else {
            Ok(())
        }
    }

    /// Parse the *numeric value* that will be used in the [`Amount`].
    ///
    /// Parses differently depending on the [`AmountType`].
    pub fn parse_value(s: &str, ty: AmountType) -> Result<Decimal, String> {
        match ty {
            AmountType::Units => <u32 as FromStr>::from_str(s)
                .map_err(|err| err.to_string())
                .map(|num| Decimal::new(num as i64, 0)),
            AmountType::Measured => Decimal::from_str(s)
                .map_err(|err| err.to_string()),
        }
    }

    pub fn text_value(&self) -> String {
        match self {
            Self::Measured { value, .. } => value.to_string(),
            Self::Units(value) => value.to_string(),
        }
    }
    pub fn ty(&self) -> AmountType {
        match self {
            Self::Measured { .. } => AmountType::Measured,
            Self::Units(_) => AmountType::Units,
        }
    }
}
impl Display for Amount {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Measured { value, label } => write!(f, "{value} {label}"),
            Self::Units(value) => f.write_str(&value.to_string()),
        }
    }
}
impl Display for AmountType {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(match self {
            Self::Measured => "Measured",
            Self::Units => "Units",
        })
    }
}
#[doc(hidden)]
#[allow(non_snake_case)]
mod __private_Amount {
    use super::*;

    #[data(impl)]
    impl Amount {
        pub fn to_string(&self) -> String { <Self as ToString>::to_string(self) }
    }
    #[data(impl)]
    impl AmountType {
        pub fn to_string(&self) -> String { <Self as ToString>::to_string(self) }
    }
}

#[data]
#[derive(Debug, Clone, Copy)]
pub enum Tax {
    CurrencyAmount(Decimal),
    Percentage(Decimal),
}
/// A lone *discriminant* value for the [`Tax`] type.
#[data]
#[derive(Debug, Clone, Copy)]
pub enum TaxType { CurrencyAmount, Percentage }

#[data(impl)]
impl Tax {
    pub fn new(ty: TaxType, value: Decimal) -> Self {
        match ty {
            TaxType::CurrencyAmount => Self::CurrencyAmount(value),
            TaxType::Percentage => Self::Percentage(value),
        }
    }

    pub fn parse(ty: TaxType, s: &str, currency: Currency, locale: Locale) -> Result<Tax, String> {
        match ty {
            TaxType::CurrencyAmount => validate_money_field_input(s, currency, locale)
                .map(|money| Self::CurrencyAmount(*money.amount()))
                .map_err(|err| err.to_string()),
            TaxType::Percentage => if s.is_empty() {
                Ok(Tax::Percentage(Decimal::ZERO))
            } else {
                Decimal::from_str(s)
                    .map(|val| Self::Percentage(val))
                    .map_err(|err| err.to_string())
            }
        }
    }

    pub fn value(&self) -> Decimal {
        match self {
            Self::CurrencyAmount(amount) => *amount,
            Self::Percentage(value) => *value,
        }
    }
    pub fn ty(&self) -> TaxType {
        match self {
            Self::CurrencyAmount { .. } => TaxType::CurrencyAmount,
            Self::Percentage(_) => TaxType::Percentage,
        }
    }
}
impl Display for TaxType {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(match self {
            Self::CurrencyAmount => "CurrencyAmount",
            Self::Percentage => "Percentage",
        })
    }
}
#[doc(hidden)]
#[allow(non_snake_case)]
mod __private_Tax {
    use super::*;

    #[data(impl)]
    impl TaxType {
        pub fn to_string(&self) -> String { <Self as ToString>::to_string(self) }
    }
}
