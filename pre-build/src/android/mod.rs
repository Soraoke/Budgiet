pub mod svg2drawable;

use std::{fs, io::Write, path::{Path, PathBuf}, sync::LazyLock};
use serde::Serialize;
use serde_xml_rs::SerdeXml;
use xml::EmitterConfig;
use crate::{Error, Errors, utils::{IterResultExt as _, read_dir_files}};

static ANDROID_RESOURCE_DIR: &str = "../android/app/src/main/res";
static DRAWABLE_DIR: LazyLock<PathBuf> = LazyLock::new(||
    Path::new(ANDROID_RESOURCE_DIR).join("drawable")
);
static USER_ICON_DRAWABLE_PREFIX: &str = "usericon_";
static ICONS_DIR: &str = "../res/user-icons";
static USER_ICONS_ARRAY_PATH: &str = "values/usericons.xml";

/// Crates an `array` in the `res/values` directory with values corresponing to the *userIcon* drawables.
/// This array can later be used to dynamically get the icons in Kotlin code.
/// 
/// If **`verbose`** is `true`, prints information about an opperation to *stderr*.
/// If **dry** is `true`, doesn't write to any files.
/// 
/// Must run [`svg2drawable::copy_icons()`] before this.
/// 
/// See [this stackoverflow post](https://stackoverflow.com/a/51824649) for more details.
pub fn create_icons_array(verbose: bool, dry: bool) -> Result<(), Errors<Error>> {
    let res_file_path = &if dry {
        Path::new("/dev/null").to_path_buf()
    } else {
        let path = Path::new(ANDROID_RESOURCE_DIR).join(USER_ICONS_ARRAY_PATH);
        if path.try_exists()
            .map_err(|err| Error::io(err, format!("Error checking if file \"{}\" exists", path.display())))?
        {
            if verbose {
                eprintln!("Vector Drawable \"{}\" already exists; skipping", path.display());
            }
            return Ok(());
        }
        path
    };

    let res_file = &mut fs::File::options()
        .create(true)
        .write(true)
        .truncate(true)
        .open(res_file_path)
        .map_err(|err| Error::io(err, format!("Error opening file \"{}\" for writing", res_file_path.display())))?;

    let array_resource = {
        let items = read_dir_files(if dry { Path::new(ICONS_DIR) } else { DRAWABLE_DIR.as_path() })
            // Only take files with 'usericons_' prefix if reading from the drawables directory.
            .filter(|result| result.as_ref().is_ok_and(|entry|
                entry.file_name()
                    .to_string_lossy()
                    .starts_with(USER_ICON_DRAWABLE_PREFIX)
                || dry
            ))
            // Map entry to its file name
            .map(|result| result.and_then(|entry| {
                let drawable_name = entry.path()
                    .with_extension("");
                let drawable_name = drawable_name.file_name()
                    .unwrap()
                    .to_str()
                    .ok_or_else(|| Error::new(format!("Drawable file name \"{}\" contains non-UTF8 characters", entry.file_name().to_string_lossy()).into()))?
                    .to_string();

                if verbose {
                    eprintln!("Adding \"{drawable_name}\" to {USER_ICONS_ARRAY_PATH}");
                }

                Ok(format!("@drawable/{drawable_name}"))
            }))
            .collect_results::<Vec<_>>()?;

        ArrayResources { array: ArrayResource {
            name: "user_icons".to_string(),
            items,
        } }
    };

    SerdeXml::new()
        .emitter(EmitterConfig::new()
            // Write in multiple lines.
            .perform_indent(true)
            // Resource file must contain the XML declaration.
            .write_document_declaration(true)
        )
        .to_writer(res_file, &array_resource)
        .map_err(|err| Error::io_other(err, format!("Error serializing array resource")))?;

    if verbose {
        eprintln!("");
        if dry { eprint!("Dry run: ") }
        eprint!("Serialized Array Resource referencing '{}' user-icons", array_resource.array.items.len());
        if !dry {
            eprint!("; wrote to \"{}\"", res_file_path.display())
        }
        eprintln!("");
    }

    Ok(())
}

#[derive(Debug, Serialize)]
#[serde(rename = "resources")]
struct ArrayResources {
    #[serde(rename = "array")]
    array: ArrayResource,
}
#[derive(Debug, Serialize)]
#[serde(rename = "array")]
struct ArrayResource {
    #[serde(rename = "@name")]
    name: String,
    #[serde(rename = "item")]
    items: Vec<String>,
}

pub fn create_gitignore(verbose: bool, dry: bool) -> Result<(), Error> {
    let path = &Path::new(ANDROID_RESOURCE_DIR).join(".gitignore");
    // Overwrite file even if it exists, to keep changes in file names
    // if path.try_exists()
    //     .map_err(|err| Error::io(err, format!("Error checking if file \"{}\" exists", path.display())))?
    // {
    //     if verbose {
    //         eprintln!(".gitignore file already exists; Skipping");
    //     }
    //     return Ok(());
    // }

    let gitignore = format!("drawable/{USER_ICON_DRAWABLE_PREFIX}*\n{USER_ICONS_ARRAY_PATH}\n");

    if !dry {
        fs::File::options()
            .create(true)
            .write(true)
            .truncate(true)
            .open(path)
            .map_err(|err| Error::io(err, format!("Error opening file \"{}\" for writing", path.display())))?
            .write_all(gitignore.as_bytes())
            .map_err(|err| Error::io(err, format!("Error writing to file \"{}\"", path.display())))?;
        if verbose {
            eprintln!("Created .gitignore");
        }
    }

    Ok(())
}
