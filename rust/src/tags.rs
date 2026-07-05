use std::sync::LazyLock;
use boltffi::{data, export};
use crate::{color::{Color, UserColorPalette}, db::{DbError, on_fake_or_real_db}};

static FAKE_TAGS: LazyLock<[Tag; 6]> = LazyLock::new(|| [
    Tag::new_fake("Groceries", "shopping_cart", UserColorPalette::Green),
    Tag::new_fake("Transportation", "rail_subway_train_transport", UserColorPalette::Blue),
    Tag::new_fake("Take-out", "fast_food_restaurant", UserColorPalette::Orange),
    Tag::new_fake("School", "education_school_cap", UserColorPalette::Brown),
    Tag::new_fake("Trips", "hiking_person", UserColorPalette::Turquoise),
    Tag::new_fake("Utility", "domain_infrastructure", UserColorPalette::Yellow),
]);
/// Returns a slice containing sample [`Tags`][Tag] that are used for demos and App Tests.
///
/// NOTE: This function should only be called *ONCE* in the entire lifetime of the program.
/// The returned list should be cached in the memory of the native application running.
#[export]
pub fn get_fake_tags() -> &'static [Tag] { &*FAKE_TAGS }

pub fn insert_tag(data: Tag) -> Result<(), DbError> {
    on_fake_or_real_db(
        |fake_db| Ok(fake_db.insert_tag(data.clone())),
        || {
            todo!("Insert {data:?} into DB")
        }
    )
}

pub fn edit_tag(name: &str, new_data: Tag) -> Result<(), DbError> {
    on_fake_or_real_db(
        |fake_db| fake_db.edit_tag(name, new_data.clone()),
        || {
            todo!("Replace tag named {name:?} with {new_data:?}")
        }
    )
}

pub fn delete_tag(name: &str) -> Result<(), DbError> {
    on_fake_or_real_db(
        |fake_db| fake_db.delete_tag(name),
        || {
            todo!("Delete tag named {name:?}")
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
impl Tag {
    const NAME_CHAR_LIMIT: usize = 15;

    fn new_fake(name: &str, icon: &str, color: Color) -> Self {
        Self { name: name.to_string(), icon: Some(icon.to_string()), color }
    }

    pub fn validate_name(name: &str, is_new: bool) -> Result<(), String> {
        let check_name_exists = || -> bool { todo!() }; // check db

        if name.is_empty() {
            Err("Tag name must not be empty.".into())
        } else if name.len() > Self::NAME_CHAR_LIMIT {
            Err(format!("Tag name must be {} characters or less.", Self::NAME_CHAR_LIMIT))
        } else if name.contains(|c: char| !c.is_ascii()) {
            Err(format!("Tag name must contain only ASCII characters"))
        } else if name.contains(|c: char| !c.is_whitespace()) {
            Err(format!("Tag name must not contain whitespace"))
        } else if is_new && !check_name_exists() {
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
