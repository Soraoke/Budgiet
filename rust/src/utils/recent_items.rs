use std::{eprintln, fs, io, path::{Path, PathBuf}, str::FromStr as _, sync::{RwLock, atomic::{AtomicBool, Ordering}}};
use common::IterResultExt as _;
use itertools::Itertools;
use rusty_money::Findable;
use boltffi::export;
use crate::{Currency, color::{Color, UserColorPalette}, utils::dispatch::dispatch_blocking_work};

static RECENT_CURRENCIES: RecentCurrencies = RecentCurrencies::new();
static RECENT_COLORS: RecentColors = RecentColors::new();

trait RecentItems: Sized + 'static {
    type T: PartialEq + Clone + Sync + Send + 'static;
    const ENTRY_NAME: &'static str;
    const MAX_ITEMS: usize = usize::MAX;
    const ENTRIES_FILE_PATH: &'static RwLock<Option<PathBuf>>;
    const ENTRIES_LIST: &'static RwLock<Vec<Self::T>>;

    fn item_from_str(s: &str) -> Result<Self::T, String>;
    fn item_to_string(item: &Self::T) -> String;
    /// Checks that [`Self`] has been initialized (i.e. it has been given the filesDir path by the Application).
    fn check_init(&self) -> io::Result<()>;

    /// Populates the [`ENTRIES_LIST`] of **recent items** with type `T`, sorted by *most recent use*.
    /// This is done by reading the file off of storage.
    ///
    /// This method should only be called if [`Self`] is *NOT* initialized.
    ///
    /// > Note: This function ***blocks***, only run it in the **worker thread**.
    fn load_storage(files_dir: &Path) -> io::Result<()> {
        let file_path = create_file_path(files_dir, Self::ENTRIES_FILE_PATH, Self::ENTRY_NAME)?;

        // Read the entirety of the file
        let items = fs::read_to_string(file_path)?
            .split('\n')
            // Last element will always be empty because the file always ends with newLine (unless it is empty).
            .dropping_back(1)
            .map(|s| Self::item_from_str(s)
                .map_err(|err| format!("Error parsing {} from {s:?}: {err}", Self::ENTRY_NAME))
            )
            .collect_results::<Vec<_>>()
            .map_err(|errs| io::Error::other(errs.into_iter().collect::<String>()))?;

        // Write entries to instance memory.
        let mut entries = Self::ENTRIES_LIST.write()
            .map_err(|_| io::Error::other(format!("RwLock ENTRIES_LIST for {} is poisoned", Self::ENTRY_NAME)))?;
        let _ = std::mem::replace(&mut *entries, items);

        Ok(())
    }

    /// Removes (clears) all items from the file in storage.
    /// The caller must remove all items from the *List* in memory in the platform language.
    ///
    /// > Note: This function ***blocks***, only run it in the **worker thread**.
    fn clear(&self) -> io::Result<()> {
        self.check_init()?;
        fs::write(get_file_path(Self::ENTRIES_FILE_PATH, Self::ENTRY_NAME)?, "")
    }

    /// Marks an **item** as recently used (i.e. it was just selected),
    /// moving it to the front of the [`List`][Vec] of recent items,
    /// which is **sorted** by latest use.
    ///
    /// Returns a new [`List`][Vec] with the sorted items.
    ///
    /// This function *only* writes to the [File] in storage.
    /// The caller mustreplace the *List* in memory in the platform language.
    ///
    /// > Note: This function ***blocks***, only run it in the **worker thread**.
    fn move_to_front(&self, item: &Self::T) -> io::Result<Vec<Self::T>> {
        self.check_init()?;
        let mut items_list = Self::ENTRIES_LIST.write()
            .map_err(|_| io::Error::other(format!("RwLock ENTRIES_LIST for {} is poisoned", Self::ENTRY_NAME)))?;

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
        let dispatch_list = items_list.clone();
        dispatch_blocking_work(move || {
            let path = get_file_path(Self::ENTRIES_FILE_PATH, Self::ENTRY_NAME)
                .unwrap_or_else(|err| panic!("{err}"));
            fs::write(&path,
                #[allow(unstable_name_collisions)]
                dispatch_list.iter()
                    .map(|item| Self::item_to_string(item))
                    .intersperse("\n".to_string())
                    .collect::<String>(),
            )
            .unwrap_or_else(|err| panic!("Error writing to file \"{}\": {err}", path.display()));
        });

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

    fn check_init(&self) -> io::Result<()> {
        if !self.is_init.load(Ordering::SeqCst) {
            Err(io::Error::other("RecentItems.Currency has not yet been initialized by the Application."))
        } else {
            Ok(())
        }
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

    fn check_init(&self) -> io::Result<()> {
        if !self.is_init.load(Ordering::SeqCst) {
            Err(io::Error::other("RecentItems.Currency has not yet been initialized by the Application."))
        } else {
            Ok(())
        }
    }
}

/// > Note: This function ***blocks***, only run it in the **worker thread**.
fn create_file_path(files_dir: &Path, entries_file_path: &RwLock<Option<PathBuf>>, entry_name: &str) -> io::Result<PathBuf> {
    let path = files_dir.join("RecentItems").join(entry_name).with_extension("txt");

    // Create file if it does not exist
    match fs::metadata(&path) {
        // Path exists, check it's a file.
        Ok(meta) => if !meta.is_file() {
            return Err(io::Error::other(format!("Entries path for {} must be a regular file", entry_name)));
        }
        // Path does not exist, create the file
        Err(err) if err.kind() == io::ErrorKind::NotFound => {
            let parent = path.parent()
                .ok_or_else(|| io::Error::other(format!("Path \"{}\" does not contain a parent component", path.display())))?;
            fs::create_dir_all(parent)?;
            fs::File::create_new(&path)?;
        },
        Err(err) => return Err(err),
    }

    // Save path to the static globals so clear() and move_to_front() can be called later.
    let _ = entries_file_path.write()
        .map_err(|_| io::Error::other(format!("RwLock ENTRIES_FILE_PATH for {} is poisoned", entry_name)))?
        .insert(path.clone());

    Ok(path)
}

/// > Note: This function ***blocks***, only run it in the **worker thread**.
fn get_file_path(entries_file_path: &RwLock<Option<PathBuf>>, entry_name: &str) -> io::Result<PathBuf> {
    let path = entries_file_path.read()
        .map_err(|_| io::Error::other(format!("RwLock ENTRIES_FILE_PATH for {} is poisoned", entry_name)))?;
    match &*path {
        Some(path) => Ok(path.clone()),
        None => Err(io::Error::other(format!("Could not get RecentItems {} entries: path not initialized", entry_name)))
    }
}

// --- EXPORT Associated functions

#[doc(hidden)]
pub struct FfiRecentCurrencies;
#[export]
#[doc(hidden)]
impl FfiRecentCurrencies {
    /// > Note: This function ***blocks***, only run it in the **worker thread**.
    pub async fn load_storage(files_dir: String) -> Result<Vec<Currency>, String> {
        if !RECENT_CURRENCIES.is_init.load(Ordering::Acquire) {
            <RecentCurrencies as RecentItems>::load_storage(Path::new(&files_dir))
                .map_err(|err| err.to_string())?;

            RECENT_CURRENCIES.is_init.store(true, Ordering::Release);
        }

        Ok(RECENT_CURRENCIES.ordered_items.read()
            .map_err(|_| io::Error::other(format!("RwLock ENTRIES_LIST for {} is poisoned", <RecentCurrencies as RecentItems>::ENTRY_NAME)))
            .map_err(|err| err.to_string())?
            .clone()
        )
    }
    pub fn clear() {
        dispatch_blocking_work(|| {
            <RecentCurrencies as RecentItems>::clear(&RECENT_CURRENCIES)
                .unwrap_or_else(|err| eprintln!("{err}"));
        });
    }
    /// > Note: This function ***blocks***, only run it in the **worker thread**.
    pub async fn move_to_front(item: Currency) -> Result<Vec<Currency>, String> {
        <RecentCurrencies as RecentItems>::move_to_front(&RECENT_CURRENCIES, &item)
            .map_err(|err| err.to_string())
    }
}

#[doc(hidden)]
pub struct FfiRecentColors;
#[export]
#[doc(hidden)]
impl FfiRecentColors {
    /// > Note: This function ***blocks***, only run it in the **worker thread**.
    pub async fn load_storage(files_dir: String) -> Result<Vec<Color>, String> {
        if !RECENT_COLORS.is_init.load(Ordering::Acquire) {
            <RecentColors as RecentItems>::load_storage(Path::new(&files_dir))
                .map_err(|err| err.to_string())?;

            RECENT_COLORS.is_init.store(true, Ordering::Release);
        }

        Ok(RECENT_COLORS.ordered_items.read()
            .map_err(|_| io::Error::other(format!("RwLock ENTRIES_LIST for {} is poisoned", <RecentColors as RecentItems>::ENTRY_NAME)))
            .map_err(|err| err.to_string())?
            .clone()
        )
    }
    pub fn clear() {
        dispatch_blocking_work(|| {
            <RecentColors as RecentItems>::clear(&RECENT_COLORS)
                .unwrap_or_else(|err| eprintln!("{err}"));
        });
    }
    /// > Note: This function ***blocks***, only run it in the **worker thread**.
    pub async fn move_to_front(item: Color) -> Result<Vec<Color>, String> {
        <RecentColors as RecentItems>::move_to_front(&RECENT_COLORS, &item)
            .map_err(|err| err.to_string())
    }
}
