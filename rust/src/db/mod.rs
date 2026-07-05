mod fake;

use fake::{FakeDb, FAKE_DB};

#[boltffi::error]
#[derive(Debug, Clone)]
pub enum DbError {
    // TODO: thiserror
    InsertError,
    EntryNotFound,
    IndexOutOfBounds,
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
