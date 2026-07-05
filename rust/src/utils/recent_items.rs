use std::{io, path::{Path, PathBuf}, str::FromStr, sync::{RwLock, atomic::{AtomicBool, Ordering}}};
use boltffi::{custom_type, export};
use itertools::Itertools;
use rusty_money::Findable;
use crate::{Currency, color::{Color, UserColorPalette}, utils::IterResultExt as _};

static RECENT_CURRENCIES: RecentCurrencies = RecentCurrencies::new();
static RECENT_COLORS: RecentColors = RecentColors::new();

trait RecentItems: Sized + 'static {
    type T: PartialEq + Clone;
    const ENTRY_NAME: &'static str;
    const MAX_ITEMS: usize = usize::MAX;
    const ENTRIES_FILE_PATH: &'static RwLock<Option<PathBuf>>;
    const ENTRIES_LIST: &'static RwLock<Vec<Self::T>>;

    fn item_from_str(s: &str) -> Result<Self::T, String>;
    fn item_to_string(item: &Self::T) -> String;

    /// Populates the [`ENTRIES_LIST`] of **recent items** with type `T`, sorted by *most recent use*.
    /// This is done by reading the file off of storage.
    ///
    /// This method should only be called if [`Self`] is *NOT* initialized.
    ///
    /// > Note: This function ***blocks***, only run it in the **worker thread**.
    fn load_storage(files_dir: &Path) -> io::Result<()> {
        let file_path = create_file_path(files_dir, Self::ENTRIES_FILE_PATH, Self::ENTRY_NAME)?;

        // Read the entirety of the file
        let items = std::fs::read_to_string(file_path)?
            .split('\n')
            // Last element will always be empty because the file always ends with newLine (unless it is empty).
            .dropping_back(1)
            .map(|s| Self::item_from_str(s)
                .map_err(|err| format!("Error parsing {} from {s:?}: {err}", Self::ENTRY_NAME))
            )
            .collect_results::<Vec<_>>()
            .map_err(|errs| io::Error::other(errs.into_iter().collect::<String>()))?;

        // Write entries to instance memory.
        let mut dest = Self::ENTRIES_LIST.write()
            .unwrap_or_else(|lock| {
                Self::ENTRIES_LIST.clear_poison();
                lock.into_inner()
            });
        let _ = std::mem::replace(&mut *dest, items);

        Ok(())
    }

    /// Removes (clears) all items from the file in storage.
    ///
    /// > Note: This function ***blocks***, only run it in the **worker thread**.
    fn clear(&self) -> io::Result<()> {
        std::fs::write(get_file_path::<Self>()?, "")
    }

    /// Marks an **item** as recently used (i.e. it was just selected),
    /// moving it to the front of the [`List`][Vec] of recent items,
    /// which is **sorted** by latest use.
    ///
    /// This function *only* writes to the [File] in storage.
    ///
    /// Returns a new [`List`][Vec] with the sorted items.
    ///
    /// > Note: This function ***blocks***, only run it in the **worker thread**.
    fn move_to_front(&self, item: &Self::T) -> io::Result<Vec<Self::T>> {
        #![allow(unstable_name_collisions)]
        let mut items_list = Self::ENTRIES_LIST.write()
            .map_err(|_| io::Error::other(format!("Could not read/write to in-memory list of RecentItems {}, RwLock is poisoned", Self::ENTRY_NAME)))?;

        // Apply to mutable list in memory.
        match items_list.iter().position(|element| element == item) {
            // The item was already first in the list; do nothing.
            Some(0) => { },
            // Swap the target item with the element in the front.
            Some(idx) => items_list.swap(0, idx),
            // The item was not found in the List, so it must be prepended.
            None => {
                items_list.insert(0, item.clone());
                // Truncate list if it reached max-size.
                let len = items_list.len();
                if Self::MAX_ITEMS != usize::MAX && len > Self::MAX_ITEMS {
                    items_list.drain(len..);
                }
            },
        }

        // Apply to file in storage.
        // !TODO: dispatch work to avoid IO from delaying updating the items in the App.
        std::fs::write(get_file_path::<Self>()?,
            items_list.iter()
                .map(|item| Self::item_to_string(item))
                .intersperse("\n".to_string())
                .collect::<String>()
        )?;

        Ok(items_list.to_vec())
    }
}

struct RecentCurrencies {
    is_init: AtomicBool,
    file_path: RwLock<Option<PathBuf>>,
    ordered_items: RwLock<Vec<<Self as RecentItems>::T>>,
}
impl RecentCurrencies {
    const fn new() -> Self {
        Self {
            is_init: AtomicBool::new(false),
            file_path: RwLock::new(None),
            ordered_items: RwLock::new(Vec::new()),
        }
    }
}
impl RecentItems for RecentCurrencies {
    type T = Currency;
    const ENTRY_NAME: &'static str = "Currency";
    const ENTRIES_FILE_PATH: &'static RwLock<Option<PathBuf>> = &RECENT_CURRENCIES.file_path;
    const ENTRIES_LIST: &'static RwLock<Vec<Self::T>> = &RECENT_CURRENCIES.ordered_items;

    fn item_from_str(s: &str) -> Result<Self::T, String> {
        rusty_money::iso::Currency::find(s)
            .ok_or_else(|| format!("Could not find Currency from code \"{s}\""))
    }
    fn item_to_string(item: &Self::T) -> String {
        item.iso_alpha_code.to_string()
    }
}

struct RecentColors {
    is_init: AtomicBool,
    file_path: RwLock<Option<PathBuf>>,
    ordered_items: RwLock<Vec<<Self as RecentItems>::T>>,
}
impl RecentColors {
    const fn new() -> Self {
        Self {
            is_init: AtomicBool::new(false),
            file_path: RwLock::new(None),
            ordered_items: RwLock::new(Vec::new()),
        }
    }
}
impl RecentItems for RecentColors {
    type T = Color;
    const ENTRY_NAME: &'static str = "Color";
    const MAX_ITEMS: usize = (UserColorPalette::list().len() as f64 / 2_f64) as usize - 1;
    const ENTRIES_FILE_PATH: &'static RwLock<Option<PathBuf>> = &RECENT_COLORS.file_path;
    const ENTRIES_LIST: &'static RwLock<Vec<Self::T>> = &RECENT_COLORS.ordered_items;

    fn item_from_str(s: &str) -> Result<Self::T, String> {
        Color::from_str(s).map_err(|err| err.to_string())
    }
    fn item_to_string(item: &Self::T) -> String { item.to_string() }
}

struct FfiRecentCurrencies();
#[export]
impl FfiRecentCurrencies {
    pub fn init(files_dir: &str) -> Result<Self, String> {
        if !RECENT_CURRENCIES.is_init.load(Ordering::Acquire) {
            <RecentCurrencies as RecentItems>::load_storage(Path::new(files_dir))
                .map_err(|err| err.to_string())?;

            RECENT_CURRENCIES.is_init.store(true, Ordering::Release);
        }

        Ok(Self())
    }

    pub fn clear(&self) -> Result<(), String> {
        <RecentCurrencies as RecentItems>::clear(&RECENT_CURRENCIES)
            .map_err(|err| err.to_string())
    }
    pub fn move_to_front(&self, item: &Currency) -> Result<Vec<Currency>, String> {
        <RecentCurrencies as RecentItems>::move_to_front(&RECENT_CURRENCIES, item)
            .map_err(|err| err.to_string())
    }
}
custom_type! {
    pub ReceFfiRecentCurrencies,
    remote = &'static RecentCurrencies,
    repr = FfiRecentCurrencies,
    into_ffi = |_| FfiRecentCurrencies(),
    try_from_ffi = |_| Ok(&RECENT_CURRENCIES),
}

struct FfiRecentColors();
#[export]
impl FfiRecentColors {
    pub fn init(files_dir: &str) -> Result<Self, String> {
        if !RECENT_COLORS.is_init.load(Ordering::Acquire) {
            <RecentColors as RecentItems>::load_storage(Path::new(files_dir))
                .map_err(|err| err.to_string())?;

            RECENT_COLORS.is_init.store(true, Ordering::Release);
        }

        Ok(Self())
    }

    pub fn clear(&self) -> Result<(), String> {
        <RecentColors as RecentItems>::clear(&RECENT_COLORS)
            .map_err(|err| err.to_string())
    }
    pub fn move_to_front(&self, item: &Color) -> Result<Vec<Color>, String> {
        <RecentColors as RecentItems>::move_to_front(&RECENT_COLORS, item)
            .map_err(|err| err.to_string())
    }
}
custom_type! {
    pub RecentColors,
    remote = &'static RecentColors,
    repr = FfiRecentColors,
    into_ffi = |_| FfiRecentColors(),
    try_from_ffi = |_| Ok(&RECENT_COLORS),
}

/// > Note: This function ***blocks***, only run it in the **worker thread**.
fn create_file_path(files_dir: &Path, entries_file_path: &RwLock<Option<PathBuf>>, entry_name: &str) -> io::Result<PathBuf> {
    let path = files_dir.join("RecentItems").join(entry_name).with_extension("txt");

    // Create file if it does not exist
    match path.metadata() {
        // Path exists, check it's a file.
        Ok(meta) => if !meta.is_file() {
            return Err(io::Error::other(format!("Entries path for {} must be a regular file", entry_name)));
        }
        // Path does not exist, create the file
        Err(err) if err.kind() == io::ErrorKind::NotFound => {
            std::fs::create_dir_all(path.parent().unwrap())?;
            std::fs::File::create_new(&path)?;
        },
        Err(err) => return Err(err),
    }

    // Save path to the static globals so clear() and move_to_front() can be called later.
    let _ = entries_file_path.write()
        .unwrap_or_else(|lock| {
            entries_file_path.clear_poison();
            lock.into_inner()
        })
        .insert(path.clone());

    Ok(path)
}

/// > Note: This function ***blocks***, only run it in the **worker thread**.
fn get_file_path<I: RecentItems>() -> io::Result<PathBuf> {
    I::ENTRIES_FILE_PATH.read()
        .map_err(|_| format!("Could not clear RecentItems {} entries: path lock is poisoned", I::ENTRY_NAME))
        .and_then(|lock| match &*lock {
            Some(path) => Ok(path.clone()),
            None => Err(format!("Could not clear RecentItems {} entries: path not initialized", I::ENTRY_NAME))
        })
        .map_err(|err| io::Error::other(err))
}
