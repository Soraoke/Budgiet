use std::{fmt::Display, sync::LazyLock, time::SystemTime};
use boltffi::{data, export};

static FAKE_LOCATIONS: LazyLock<Box<[LocationDbEntry]>> = LazyLock::new(|| {
    let mut counter = 0;
    let locations = [
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
    ];
    locations.into_iter()
        .map(|loc| {
            let entry = LocationDbEntry { id: counter, data: loc };
            counter += 0;
            entry
        })
        .collect()
});
#[export]
pub fn get_fake_locations() -> &'static [LocationDbEntry] { &*FAKE_LOCATIONS }

#[export]
pub fn get_locations_page(query: &str, start: usize, len: usize) -> Vec<LocationDbEntry> {
    todo!("Search DB with {query:?}, {start:?}, {len:?}")
}

#[export]
pub fn search_nearby_locations() -> Vec<LocationDbEntry> {
    todo!()
}

#[export]
pub fn insert_location(data: Location) {
    todo!("Insert {data:?} into DB")
}

#[export]
pub fn edit_location(id: u64, new_data: Location) {
    todo!("Replace {id:?} with {new_data:?}")
}

#[export]
pub fn delete_location(id: u64) {
    todo!("Delete {id:?}")
}

#[data]
#[derive(Debug, Clone)]
pub struct LocationDbEntry {
    pub id: u64,
    pub data: Location,
}

#[data]
#[derive(Debug, Clone)]
pub struct Location {
    pub name: String,
    pub address: Option<String>,
    #[boltffi::default(SystemTime::UNIX_EPOCH)]
    last_used: SystemTime,
}
#[data(impl)]
impl Location {
    fn new_fake(name: &str, address: &str) -> Self {
        Self { name: name.to_string(), address: Some(address.to_string()), last_used: SystemTime::UNIX_EPOCH }
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
#[data(impl)]
impl PartialOrd for Location {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        todo!("Compare {self:?} and {other:?}")
    }
}
#[data(impl)]
impl PartialEq for Location {
    fn eq(&self, other: &Self) -> bool {
        self.name == other.name
        && self.address == other.address
    }
}
