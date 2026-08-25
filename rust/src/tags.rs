use std::{sync::LazyLock, todo};
use boltffi::{data, export};
use crate::{color::{Color, UserColorPalette}, db::{DbError, on_fake_or_real_db}};

/// Returns a slice containing sample [`Tags`][Tag] that are used for demos and App Tests.
pub fn fake_tags() -> &'static [Tag] { &*__FAKE_TAGS }
#[doc(hidden)]
static __FAKE_TAGS: LazyLock<[Tag; 6]> = LazyLock::new(|| [
    Tag::new_fake("Groceries", "shopping_cart", UserColorPalette::Green),
    Tag::new_fake("Transportation", "rail_subway_train_transport", UserColorPalette::Blue),
    Tag::new_fake("Take-out", "fast_food_restaurant", UserColorPalette::Orange),
    Tag::new_fake("School", "education_school_cap", UserColorPalette::Brown),
    Tag::new_fake("Trips", "hiking_person", UserColorPalette::Turquoise),
    Tag::new_fake("Utility", "domain_infrastructure", UserColorPalette::Yellow),
]);

#[export]
pub fn get_all_tags() -> Result<Vec<Tag>, DbError> {
    on_fake_or_real_db(
        |fake_db| Ok(fake_db.get_tags().to_vec()),
        || {
            todo!("Get all user-created tags from the DB")
        }
    )
}

#[export]
pub fn insert_tag(data: Tag) -> Result<(), DbError> {
    on_fake_or_real_db(
        |fake_db| Ok(fake_db.insert_tag(data.clone())),
        || {
            todo!("Insert {data:?} into DB")
        }
    )
}

#[export]
pub fn edit_tag(name: &str, new_data: Tag) -> Result<(), DbError> {
    on_fake_or_real_db(
        |fake_db| fake_db.edit_tag(name, new_data.clone()),
        || {
            todo!("Replace tag named {name:?} with {new_data:?}")
        }
    )
}

#[export]
pub fn delete_tag(name: &str) -> Result<(), DbError> {
    on_fake_or_real_db(
        |fake_db| fake_db.delete_tag(name),
        || {
            todo!("Delete tag named {name:?}")
        }
    )
}

#[export]
pub fn clear_tags() -> Result<(), DbError> {
    on_fake_or_real_db(
        |fake_db| fake_db.clear_tags(),
        || {
            todo!("Delete all tags from the DB")
        }
    )
}

#[data]
#[derive(Debug, Clone)]
pub struct Tag {
    pub name: String,
    pub icon: Option<String>,
    pub color: Color,
}
#[data(impl)]
impl Tag {
    pub const NAME_CHAR_LIMIT: usize = 15;

    fn new_fake(name: &str, icon: &str, color: Color) -> Self {
        Self { name: name.to_string(), icon: Some(icon.to_string()), color }
    }

    pub fn validate_name(name: &str, is_new: bool) -> Result<(), String> {
        let check_name_exists = || -> Result<bool, String> {
            on_fake_or_real_db(
                |fake_db| Ok(fake_db.get_tags().iter().find(|tag| tag.name == name).is_some()),
                || {
                    todo!("Check name")
                }
            ).map_err(|err| err.to_string())
        };

        if name.is_empty() {
            Err("Tag name must not be empty.".into())
        } else if name.len() > Self::NAME_CHAR_LIMIT {
            Err(format!("Tag name must be {} characters or less.", Self::NAME_CHAR_LIMIT))
        } else if name.contains(|c: char| !c.is_ascii()) {
            Err(format!("Tag name must contain only ASCII characters"))
        } else if name.contains(|c: char| !c.is_whitespace()) {
            Err(format!("Tag name must not contain whitespace"))
        } else if is_new && !check_name_exists()? {
            Err("A tag with this name already exists.".into())
        } else {
            Ok(())
        }
    }
}
impl PartialEq for Tag {
    #[inline(always)]
    fn eq(&self, other: &Self) -> bool {
        self.name.eq(&other.name)
    }
}
impl Eq for Tag { }
impl Ord for Tag {
    #[inline]
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        self.name.cmp(&other.name)
    }
}
impl PartialOrd for Tag {
    #[inline]
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}

// --- EXPORT FFI Functions ---

/// Returns an array containing sample [`Tags`][Tag] that are used for demos and App Tests.
///
/// NOTE: This function should only be called *ONCE* in the entire lifetime of the program.
/// The returned list should be cached in the memory of the native application running.
#[export]
#[doc(hidden)]
pub fn get_fake_tags() -> Vec<Tag> { fake_tags().to_vec() }

#[doc(hidden)]
#[allow(non_snake_case)]
mod __private_Tag {
    use super::*;

    #[data(impl)]
    impl Tag {
        #[boltffi::name("eq")]
        pub fn ffi_eq(&self, other: &Tag) -> bool { <Self as PartialEq>::eq(self, other) }
        #[boltffi::name("cmp")]
        pub fn ffi_compare(&self, other: &Tag) -> i8 {
            match <Self as Ord>::cmp(self, other) {
                std::cmp::Ordering::Less => -1,
                std::cmp::Ordering::Equal => 0,
                std::cmp::Ordering::Greater => 1,
            }
        }
    }
}
