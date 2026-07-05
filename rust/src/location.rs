use std::{fmt::Display, sync::LazyLock, time::SystemTime};
use boltffi::{data, export};
use crate::db::{DbError, on_fake_or_real_db};

static FAKE_LOCATIONS: LazyLock<[Location; 11]> = LazyLock::new(|| [
    Location::new_fake("Chipotle", "123 Main Street, Bronx NY"),
    Location::new_fake("Aldi", "456 IsNuts Lane, Los Angeles CA"),
    Location::new_fake("Bowling Alley", "789 Trampoline Street, Detroit MI"),
    Location::new_fake("Six Flags Great Adventure", "1 Six Flags Blvd, Jackson Township, NJ 08527"),
    Location::new_fake("Reading Terminal Market", "1136 Arch St, Philadelphia, PA 19107"),
    Location::new_fake("Angie's Seafood", "1727 E Pratt St, Baltimore, MD 21231"),
    Location::new_fake("Ichiran", "132 W 31st St, New York, NY 10001"),
    Location::new_fake("Frugal Bookstore", "57 Warren St, Roxbury, MA 02119"),
    Location::new_fake("Sonic Boom", "215 Spadina Ave., Toronto, ON M5T 2C7, Canada"),
    Location::new_fake("The Little Grand Market", "710 Grandview Xing Wy Suite 112, Columbus, OH 43215"),
    Location::new_fake("Five Guys", "3273 Steelyard Dr, Cleveland, OH 44109"),
]);
/// Returns a slice containing sample [`Locations`][Location] that are used for demos and App Tests.
///
/// NOTE: This function should only be called *ONCE* in the entire lifetime of the program.
/// The returned list should be cached in the memory of the native application running.
#[export]
pub fn get_fake_locations() -> &'static [Location] { &*FAKE_LOCATIONS }

#[export]
pub fn get_locations_page(query: Option<String>, start: usize, len: usize) -> Result<Vec<LocationDbEntry>, DbError> {
    on_fake_or_real_db(
        |fake_db| Ok(fake_db.get_locations_page(query.as_ref().map(String::as_str), start, len)),
        || {
            todo!("Search DB with {query:?}, {start:?}, {len:?}")
        }
    )
}

#[export]
pub fn search_nearby_locations() -> Vec<LocationDbEntry> {
    todo!()
}

#[data]
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LocationDbEntry {
    pub id: u64,
    pub data: Location,
    #[boltffi::default(Instant::EPOCH)]
    pub(super) last_used: SystemTime,
}
#[data(impl)]
impl LocationDbEntry {
    /// Create a new [`Location`] *Database item* with the given data.
    pub fn insert_new(data: Location) -> Result<Self, DbError> {
        let new_last_used = SystemTime::now();

        on_fake_or_real_db(
            |fake_db| Ok(fake_db.insert_location(data.clone(), new_last_used)),
            || {
                todo!("Insert {data:?} into DB")
            }
        )
    }

    pub fn find_by_id(id: u64) -> Result<Option<Self>, DbError> {
        on_fake_or_real_db(
            |fake_db| Ok(fake_db.find_location_by_id(id)),
            || {
                todo!("Find Location Entry by {id:?}")
            }
        )
    }

    /// Modifies the existing [`Location`] item's data in the Database.
    pub fn edit(&self, new_data: Location) -> Result<(), DbError> {
        let new_last_used = SystemTime::now();

        on_fake_or_real_db(
            |fake_db| fake_db.edit_location(self.id, new_data.clone(), new_last_used),
            || {
                todo!("Replace {:?} with {new_data:?}", self.id)
            }
        )
    }

    /// Set the [`Location`]'s **last_used** time to now,
    /// bumping the entry to the top of query results.
    pub fn mark_used(&self) -> Result<(), DbError> {
        let new_last_used = SystemTime::now();

        on_fake_or_real_db(
            |fake_db| fake_db.mark_location_used(self.id, new_last_used),
            || {
                todo!()
            }
        )

    }

    /// Deletes the [`Location`] entry from the Database.
    pub fn delete(self) -> Result<(), DbError> {
        on_fake_or_real_db(
            |fake_db| fake_db.delete_location(self.id),
            || {
                todo!()
            }
        )
    }

    /// Deletes all [`Location`] entries from the Database.
    pub fn clear_all() -> Result<(), DbError> {
        on_fake_or_real_db(
            |fake_db| Ok(fake_db.locations.clear()),
            || {
                todo!()
            }
        )
    }
}
#[data(impl)]
impl PartialOrd for LocationDbEntry {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}
#[data(impl)]
impl Ord for LocationDbEntry {
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        self.last_used.cmp(&other.last_used)
            .reverse() // Greater times means more recent use, so should go first.
            .then(self.id.cmp(&other.id))
            .then(self.data.cmp(&other.data))
    }
}

#[data]
#[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord)]
pub struct Location {
    pub name: String,
    pub address: Option<String>,
}
#[data(impl)]
impl Location {
    fn new_fake(name: &str, address: &str) -> Self {
        Self { name: name.to_string(), address: Some(address.to_string()) }
    }

    // TODO: DOC; old_data is Some if editing a specific location
    // TODO: make unit test (but in rust) for this:
    //   Add new location (name, addr) when location (name, addr) with the same (addr) but diff name exists
    //   Add new location (name, no addr) when location (name, addr) with same (name) exists
    //   Add new location (name, no addr) when location (name, no addr) with same (name) exists
    //   Add new location (name, addr) when location (name, addr) with same (name, addr) exists
    //   Add location A (name, addrA), Add location B (name, addrB), Try add location A and B again (fail)
    //   Edit existing location, repeating all same rules as above.
    pub fn validate(new_data: &Location, old_data: Option<Location>) -> Result<(), String> {
        todo!("Check that {new_data:?} can be inserted into DB, optionally comparing to {old_data:?}")
    }

    #[allow(unused_variables)]
    pub fn validate_name(name: &str, is_new: bool) -> Result<(), String> {
        if name.is_empty() {
            Err("Name must not be empty".into())
        } else {
            Ok(())
        }
    }

    #[allow(unused_variables)]
    pub fn validate_address(address: &str, is_new: bool) -> Result<(), String> {
        Ok(())
    }
}
#[data(impl)]
impl Display for Location {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let address = match &self.address {
            Some(address) => format!(" at {address}"),
            None => String::new(),
        };
        write!(f, "{}{address}", self.name)
    }
}
