use std::sync::LazyLock;
use boltffi::{data, export};
use chrono::{DateTime, Utc};
use crate::{Money, db::{DbError, on_fake_or_real_db}, items::{Item, Tax}, location::LocationDbEntry, tags::Tag};

static FAKE_TRANSACTIONS: LazyLock<[Transaction; 0]> = LazyLock::new(|| [
    // TODO:
]);
/// Returns a slice containing sample [`Locations`][Location] that are used for demos and App Tests.
///
/// NOTE: This function should only be called *ONCE* in the entire lifetime of the program.
/// The returned list should be cached in the memory of the native application running.
#[export]
pub fn get_fake_transactions() -> &'static [Transaction] { &*FAKE_TRANSACTIONS }

#[derive(Debug, Clone)]
#[data]
pub struct Transaction {
    pub date: DateTime<Utc>,
    pub location: Option<LocationDbEntry>,
    pub price: Money,
    pub items: Vec<Item>,
    pub tax: Tax,
    pub tags: Vec<Tag>,
    pub description: String,
}
impl PartialEq for Transaction {
    #[inline]
    fn eq(&self, other: &Self) -> bool {
        self.date.eq(&other.date)
    }
}
impl Eq for Transaction { }
impl Ord for Transaction {
    #[inline]
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        self.date.cmp(&other.date)
    }
}
impl PartialOrd for Transaction {
    #[inline]
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}

#[export]
pub fn get_transactions_page(query: Option<String>, start: usize, len: usize) -> Result<Vec<TransactionDbEntry>, DbError> {
    on_fake_or_real_db(
        |fake_db| fake_db.get_transactions_page(query.as_ref().map(String::as_str), start, len),
        || {
            todo!("Search DB with {query:?}, {start:?}, {len:?}")
        }
    )
}

#[derive(Debug, Clone)]
#[data]
pub struct TransactionDbEntry {
    pub id: u64,
    pub data: Transaction,
}
#[data(impl)]
impl TransactionDbEntry {
    /// Create a new [`Transaction`] *Database item* with the given data.
    pub fn insert_new(data: Transaction) -> Result<Self, DbError> {
        on_fake_or_real_db(
            |fake_db| Ok(fake_db.insert_transaction(data.clone())),
            || {
                todo!("Insert {data:?} into DB")
            }
        )
    }

    /// Modifies the existing [`Transaction`] item's data in the Database.
    pub fn edit(&self, new_data: Transaction) -> Result<(), DbError> {
        on_fake_or_real_db(
            |fake_db| fake_db.edit_transaction(self.id, new_data.clone()),
            || {
                todo!("Replace {:?} with {new_data:?}", self.id)
            }
        )
    }

    /// Deletes the [`Transaction`] entry from the Database.
    pub fn delete(self) -> Result<(), DbError> {
        on_fake_or_real_db(
            |fake_db| fake_db.delete_transaction(self.id),
            || {
                todo!()
            }
        )
    }

    /// Deletes all [`Transaction`] entries from the Database.
    pub fn clear_all() -> Result<(), DbError> {
        on_fake_or_real_db(
            |fake_db| Ok(fake_db.transactions.clear()),
            || {
                todo!()
            }
        )
    }
}
impl PartialEq for TransactionDbEntry {
    #[inline]
    fn eq(&self, other: &Self) -> bool {
        self.data.eq(&other.data)
    }
}
impl Eq for TransactionDbEntry { }
impl Ord for TransactionDbEntry {
    #[inline]
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        self.data.cmp(&other.data)
    }
}
impl PartialOrd for TransactionDbEntry {
    #[inline]
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}
