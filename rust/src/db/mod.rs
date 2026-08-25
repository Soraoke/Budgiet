mod fake;

use std::time::SystemTime;
use boltffi::export;
use fake::{FakeDb, FAKE_DB};
use crate::{location::fake_locations, tags::fake_tags, transaction::fake_transactions};

#[boltffi::error]
#[derive(Debug, Clone, thiserror::Error)]
pub enum DbError {
    #[error("Error inserting data into Database")]
    InsertError,
    #[error("Could not find entry in Database that matches the given query")]
    EntryNotFound,
    // IndexOutOfBounds,
    #[error("{_0}")]
    Other(String),
}

/// Activates the *fake "database"* for the current thread.
///
/// If **`add_items`** is `true`, the FakeDb will be populated with *fake items* from each category (e.g. [fake tags][crate::tags::fake_tags], [fake locations][crate::location::fake_locations], etc),
/// otherwise it will start out empty.
#[export]
pub fn use_fake_db(add_items: bool) {
    let mut fake_db = FakeDb::default();

    if add_items {
        let now = SystemTime::now();

        for location in fake_locations() {
            fake_db.insert_location(location.clone(), now);
        }
        for tag in fake_tags() {
            fake_db.insert_tag(tag.clone());
        }
        for transaction in fake_transactions() {
            fake_db.insert_transaction(transaction.clone());
        }
    }

    FAKE_DB.set(Some(fake_db));
}

/// Runs the operation on the **fake database**,
/// but if the thread has *no [`FAKE_DB`]*, then runs the operation on the **real database**.
pub(crate) fn on_fake_or_real_db<T>(
    on_fake: impl FnOnce(&mut FakeDb) -> Result<T, DbError>,
    on_real: impl FnOnce() -> Result<T, DbError>,
) -> Result<T, DbError> {
    let fake_rtrn = FAKE_DB.with_borrow_mut(|fake_db| match fake_db {
        Some(fake_db) => Some(on_fake(fake_db)),
        None => None,
    });

    match fake_rtrn {
        Some(val) => val,
        None => on_real(),
    }
}
