use std::{sync::LazyLock, todo};
use chrono::{DateTime, Utc};
use uniffi::{Record, export};
use crate::{Money, db::{DbError, on_fake_or_real_db}, items::{Item, Tax}, location::LocationDbEntry, tags::Tag};

/// Returns a slice containing sample [`Locations`][Location] that are used for demos and App Tests.
pub fn fake_transactions() -> &'static [Transaction] { &*__FAKE_TRANSACTIONS }
#[doc(hidden)]
static __FAKE_TRANSACTIONS: LazyLock<[Transaction; 0]> = LazyLock::new(|| [
    // TODO:
]);

#[derive(Debug, Clone, Record)]
#[export(Eq, Ord)]
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

pub fn get_transactions_page(query: Option<String>, start: usize, len: usize) -> Result<Vec<TransactionDbEntry>, DbError> {
    on_fake_or_real_db(
        |fake_db| fake_db.get_transactions_page(query.as_ref().map(String::as_str), start, len),
        || {
            todo!("Search DB with {query:?}, {start:?}, {len:?}")
        }
    )
}

#[derive(Debug, Clone, Record)]
#[export(Eq, Ord)]
pub struct TransactionDbEntry {
    pub id: u64,
    pub data: Transaction,
}
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
#[export]
impl TransactionDbEntry {
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

// --- EXPORT Associated functions ---

/// Returns a slice containing sample [`Locations`][Location] that are used for demos and App Tests.
///
/// NOTE: This function should only be called *ONCE* in the entire lifetime of the program.
/// The returned list should be cached in the memory of the native application running.
#[export]
#[doc(hidden)]
fn get_fake_transactions() -> Vec<Transaction> { fake_transactions().to_vec() }

#[export]
#[doc(hidden)]
fn __ffi_get_transactions_page(query: Option<String>, start: u64, len: u64) -> Result<Vec<TransactionDbEntry>, DbError> {
    get_transactions_page(query, start as usize, len as usize)
}

#[export]
#[doc(hidden)]
fn insert_new(data: Transaction) -> Result<TransactionDbEntry, DbError> {
    TransactionDbEntry::insert_new(data)
}
#[export]
#[doc(hidden)]
fn clear_all() -> Result<(), DbError> {
    TransactionDbEntry::clear_all()
}
