use std::{cell::RefCell, time::SystemTime};
use boltffi::export;
use crate::{location::{Location, LocationDbEntry}, tags::Tag, transaction::{Transaction, TransactionDbEntry}};
use super::DbError;

thread_local! {
    /// A *fake "Database"* store, useful for **demos** and **UI tests**.
    ///
    /// Each thread has its own "fake database".
    pub(super) static FAKE_DB: RefCell<Option<FakeDb>> = RefCell::new(None);
}

/// Activates the *fake "database"* for the current thread.
#[export]
pub fn use_fake_db() {
    FAKE_DB.set(Some(FakeDb::default()));
}

#[derive(Debug, Default)]
pub(crate) struct FakeDb {
    pub locations: Vec<LocationDbEntry>,
    pub transactions: Vec<TransactionDbEntry>,
    pub tags: Vec<Tag>,
}
impl FakeDb {
    fn find_idx_in_array<T>(array: &[T], mut predicate: impl FnMut(&T) -> bool) -> Result<usize, DbError> {
        array.iter()
            .enumerate()
            .find(|(_, entry)| predicate(*entry))
            .map(|(idx, _)| idx)
            .ok_or(DbError::EntryNotFound)
    }
}

impl FakeDb {
    pub fn get_locations_page(&self, query: Option<&str>, start: usize, len: usize) -> Vec<LocationDbEntry> {
        // TODO: use query
        self.locations[start..start+len].iter()
            .map(LocationDbEntry::clone)
            .collect::<Vec<_>>()
    }
    pub fn insert_location(&mut self, data: Location, new_last_used: SystemTime) -> LocationDbEntry {
        let entry = LocationDbEntry {
            id: self.locations
                .last()
                .map(|entry| entry.id + 1)
                .unwrap_or(0),
            data: data.clone(),
            last_used: new_last_used,
        };
        self.locations.push(entry.clone());
        // Sort fake Db.
        self.locations.sort();

        entry
    }
    pub fn find_location_by_id(&self, id: u64) -> Option<LocationDbEntry> {
        self.locations.iter()
            .find(|entry| entry.id == id)
            .map(|entry| entry.clone())
    }
    pub fn edit_location(&mut self, id: u64, new_data: Location, new_last_used: SystemTime) -> Result<(), DbError> {
        // Find index in Array by Entry's id.
        let idx = Self::find_idx_in_array(&self.locations, |entry| entry.id == id)?;
        // Replace entry.
        let entry = &mut self.locations[idx];
        *entry = LocationDbEntry {
            id: entry.id,
            data: new_data,
            last_used: new_last_used,
        };
        // Sort fake Db.
        self.locations.sort();

        Ok(())
    }
    pub fn mark_location_used(&mut self, id: u64, new_last_used: SystemTime) -> Result<(), DbError> {
        // Find index in Array by Entry's id.
        let idx = Self::find_idx_in_array(&self.locations, |entry| entry.id == id)?;
        // Update timestamp
        self.locations[idx].last_used = new_last_used;
        // Sort fake Db.
        self.locations.sort();

        Ok(())
    }
    pub fn delete_location(&mut self, id: u64) -> Result<(), DbError> {
        // Find index in Array by Entry's id.
        let idx = Self::find_idx_in_array(&self.locations, |entry| entry.id == id)?;
        // Remove entry.
        self.locations.remove(idx);

        Ok(())
    }
}

impl FakeDb {
    pub fn get_transactions_page(&self, query: Option<&str>, start: usize, len: usize) -> Vec<TransactionDbEntry> {
        todo!()
    }
    pub fn insert_transaction(&mut self, data: Transaction) -> TransactionDbEntry {
        let entry = TransactionDbEntry {
            id: self.transactions
                .last()
                .map(|entry| entry.id + 1)
                .unwrap_or(0),
            data: data.clone(),
        };
        self.transactions.push(entry.clone());
        // Sort fake Db.
        self.transactions.sort();

        entry
    }
    pub fn edit_transaction(&mut self, id: u64, new_data: Transaction) -> Result<(), DbError> {
        // Find index in Array by Entry's id.
        let idx = Self::find_idx_in_array(&self.transactions, |entry| entry.id == id)?;
        // Replace entry.
        let entry = &mut self.transactions[idx];
        *entry = TransactionDbEntry {
            id: entry.id,
            data: new_data,
        };
        // Sort fake Db.
        self.transactions.sort();

        Ok(())
    }
    pub fn delete_transaction(&mut self, id: u64) -> Result<(), DbError> {
        // Find index in Array by Entry's id.
        let idx = Self::find_idx_in_array(&self.transactions, |entry| entry.id == id)?;
        // Remove entry.
        self.transactions.remove(idx);

        Ok(())
    }
}

impl FakeDb {
    pub fn insert_tag(&mut self, data: Tag) {
        self.tags.push(data)
    }
    pub fn edit_tag(&mut self, name: &str, new_data: Tag) -> Result<(), DbError> {
        // Find index in Array by Entry's id.
        let idx = Self::find_idx_in_array(&self.tags, |entry| entry.name == name)?;
        // Replace entry.
        let entry = &mut self.tags[idx];
        *entry = new_data;
        // Sort fake Db.
        self.tags.sort();

        Ok(())
    }
    pub fn delete_tag(&mut self, name: &str) -> Result<(), DbError> {
        // Find index in Array by Entry's id.
        let idx = Self::find_idx_in_array(&self.tags, |entry| entry.name == name)?;
        // Remove entry.
        self.tags.remove(idx);

        Ok(())
    }
}
