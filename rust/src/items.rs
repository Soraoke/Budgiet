use std::{fmt::Display, sync::LazyLock};
use std::str::FromStr;
use boltffi::{data, export};
use rust_decimal::Decimal;
use crate::price::validate_money_field_input;
use crate::{Currency, Locale, Money};

static FAKE_ITEMS: LazyLock<[Item; 5]> = LazyLock::new(|| [
    Item::new_fake("Ham", 5.99, Amount::Units(1)),
    Item::new_fake("Cheese", 2.59, Amount::new_fake_measured(1.0, "lbs")),
    Item::new_fake("Bread", 4.19, Amount::Units(2)),
    Item::new_fake("Crackers", 1.89, Amount::Units(1)),
    Item::new_fake("Chicken", 4.99, Amount::new_fake_measured(3.5, "lbs")),
]);
#[export]
pub fn get_fake_items() -> &'static [Item] { &*FAKE_ITEMS }

/// Calculate the total cost of *all [`Items`][Item]* combined, plus the [`Tax`] amount.
#[export]
pub fn total_price(items: &[Item], tax: Tax) -> Decimal {
    let mut currency = match &tax {
        Tax::CurrencyAmount(money) => Some(money.currency),
        Tax::Percentage(_) => None,
    };

    let items_sum: Decimal = items.iter()
        .map(|item| {
            // Check that all items use the same currency.
            match &currency {
                Some(currency) => if currency != &item.unit_price.currency {
                    panic!("Items have different currencies")
                },
                None => currency = Some(item.unit_price.currency)
            }

            item.total_price().amount
        })
        .sum();

    let tax_amount = match tax {
        Tax::CurrencyAmount(money) => money.amount,
        Tax::Percentage(percent) => items_sum * percent * Decimal::from_f32_retain(0.01).unwrap()
    };

    items_sum + tax_amount
}

/// Produces a message to display in the `NewTransactionForm` `ItemsField`.
/// Includes the number of items, their total cost, and the tax amount/percentage.
#[export]
pub fn display_field_info(
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
        .map(|item| item.total_price().amount)
        .fold(Decimal::ZERO, |acc, amount| acc + amount);

    format!("{items_count} {items_word} ({items_price}){display_tax}",
        items_word = if items_count == 1 { "item" } else { "items" },
        items_price = Money::from_decimal(items_price, currency).format(locale, true),
        display_tax = if tax.value() != Decimal::ZERO && items_count != 0 {
            match tax {
                Tax::CurrencyAmount(tax_money) => format!(" + {} tax", tax_money.format(locale, true)),
                Tax::Percentage(percent) => format!(" + {percent}% tax"),
            }
        } else { String::new() },
    )
}

#[data]
#[derive(Debug, Clone)]
pub struct Item {
    pub name: String,
    pub unit_price: Money,
    pub amount: Amount,
}
impl Item {
    fn new_fake(name: &str, unit_price: f64, amount: Amount) -> Self {
        Self {
            name: name.to_string(),
            unit_price: Money::from_decimal(Decimal::from_f64_retain(unit_price).unwrap(), Currency(rusty_money::iso::USD)),
            amount,
        }
    }

    /// Calculate how much *this* single [`Item`] entry costs based on its **`unit_price`** and **`amount`**.
    pub fn total_price(&self) -> Money {
        let currency = self.unit_price.currency;
        let amount = match &self.amount {
            Amount::Measured { value, .. } => self.unit_price.amount * value,
            Amount::Units(value) => self.unit_price.amount * Decimal::from(*value),
        };
        Money::from_decimal(amount, currency)
    }

    // TODO: doc
    pub fn validate_name(existing_items: &[Self], name: &str, is_new: bool) -> Result<(), String> {
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
    /// Parse the *numeric value* that will be used in [`Amount::Measured`].
    pub fn parse_measured_value(s: &str) -> Result<Decimal, String> {
        Decimal::from_str(s)
            .map_err(|err| err.to_string())
    }
    /// Parse the *numeric value* that will be used in [`Amount::Units`].
    pub fn parse_units_value(s: &str) -> Result<u32, String> {
        <u32 as FromStr>::from_str(s)
            .map_err(|err| err.to_string())
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
#[data(impl)]
impl Display for Amount {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Measured { value, label } => write!(f, "{value} {label}"),
            Self::Units(value) => f.write_str(&value.to_string()),
        }
    }
}

#[data]
#[derive(Debug, Clone, Copy)]
pub enum Tax {
    CurrencyAmount(Money),
    Percentage(Decimal),
}
/// A lone *discriminant* value for the [`Tax`] type.
#[data]
#[derive(Debug, Clone, Copy)]
pub enum TaxType { CurrencyAmount, Percentage }

#[data(impl)]
impl Tax {
    pub fn parse(ty: TaxType, s: &str, currency: Currency, locale: Locale) -> Result<Tax, String> {
        match ty {
            TaxType::CurrencyAmount => validate_money_field_input(s, currency, locale)
                .map(|money| Self::CurrencyAmount(money))
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
            Self::CurrencyAmount(money) => money.amount,
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
