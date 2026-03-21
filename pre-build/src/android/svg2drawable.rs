use std::{ffi::OsString, fs, path::{Path, PathBuf}};
use serde::{Deserialize, Serialize};
use serde_xml_rs::SerdeXml;
use sha2::{Digest as _, Sha256};
use xml::EmitterConfig;
use super::{DRAWABLE_DIR, ICONS_DIR, USER_ICON_DRAWABLE_PREFIX};
use crate::{Error, Errors, command, utils::{IterResultExt as _, checksum, read_dir}};

static TARGET_DIR: &str = "../target";

/// Copy and **reformat** `SVG` files in [`ICONS_DIR`] to the `res/drawable` directory in the Android application.
/// 
/// If **`dry`** is `true`, doesn't write to any files.
/// 
/// The files are converted to Android's proprietary *Drawable Resource (XML)* format and have `"userIcon_"` prefixed to their file name. 
pub fn copy_icons(dry: bool) -> Result<(), Errors<Error>> {
    let tmp_dir = &svg_to_bad_drawable(ICONS_DIR)?;

    // The converted Vector Drawables are missing some attributes, add those here.
    read_dir(tmp_dir).map(|result| result.and_then(|entry| {
        // The entry's name + a prefix for drawable distinction in Android `res/drawable`.
        let android_file = {
            let mut file_name = OsString::from(USER_ICON_DRAWABLE_PREFIX);
            file_name.push(entry.file_name());
            DRAWABLE_DIR.join(file_name)
        };

        if android_file.try_exists()
            .map_err(|err| Error::io(err, format!("Error checking if file \"{}\" exists", android_file.display())))?
        {
            println!("Vector Drawable \"{}\" already exists; skipping", android_file.display());
            return Ok(());
        }

        // Deserialize Vector Drawable with bad data.
        let drawable = BadDrawable::read_file(entry.path())?;

        // Correct the data in the Drawable to a file in the android drawables directory.
        drawable.write_to_file(if dry { Path::new("/dev/null") } else { &android_file })?;

        // Delete the temporary, bad drawable file.
        fs::remove_file(entry.path())
            .map_err(|err| Error::io(err, format!("Error deleting file \"{}\"", entry.path().display())))?;

        if dry { print!("Dry run: ") }
        print!("Converted SVG file \"{}\"", Path::new(ICONS_DIR).join(entry.file_name()).with_extension("svg").display());
        if !dry {
            print!("; wrote to \"{}\"", android_file.display())
        }
        println!("");

        Ok(())
    }))
    .collect_results()
}

/// Convert an **SVG** file to Android's proprietary **Vector Drawable** format.
/// 
/// The Vector Drawable Tool, however, does not produce a valid Drawable that the Android build can use,
/// so the file must be converted *once again* by **deserializing** and **serializing** a [`BadDrawable`].
/// 
/// **`path`** is the SVG file, *or* a directory containing SVG files.
/// 
/// Returns the **path** to the converted **Vector Drawable** file (or directory, if **`path`** was a directory).
pub fn svg_to_bad_drawable(path: impl AsRef<Path>) -> Result<PathBuf, Error> {
    let path = path.as_ref();
    let tmp_dir = Path::new(TARGET_DIR).join("tmp").join("user-icons");

    fs::create_dir_all(&tmp_dir)
        .map_err(|err| Error::io(err, format!("Error creating directory \"{}\"", tmp_dir.display())))?;

    unpack_vd_tool()?;
    command!("../target/bin/vd-tool/bin/vd-tool", "-c", "-in", path, "-out", tmp_dir)?;

    Ok(if path.metadata()
        .map_err(|err| Error::io(err, format!("Error checking if \"{}\" is a file", path.display())))?
        .is_file()
    {
        // Won't panic because a path to a file that exists will not terminate in '..'.
        tmp_dir.join(path.file_name().unwrap()).with_extension("xml")
    } else {
        tmp_dir
    })
}

/// `vd-tool` stands for ***Vector Drawable Tool***,
/// and has the functionality to convert **SVG to Vector Drawable** with all the features of both formats.
///
// ---
// The tool is NOT packaged in this repo, it must be downloaded from the repackager repo and then that downloads the actual code that we compile here.
// Both the source code and the java binary must be in the `/target` directory of the project.
// Scratch that... I was going to do this but its just gonna make CI run slower, so just download the built package in the github.
fn unpack_vd_tool() -> Result<(), Error> {
    static PKG_URL: &str = "https://github.com/rharter/vd-tool/releases/download/v1/vd-tool.zip";
    static PKG_SHA256: &str = "9bc7b2046b51e22c62663a93c9e91c3b29b053a36a5484a0d73d8c54def3e595";

    let zip_file = Path::new(TARGET_DIR).join("vd-tool.zip");
    let bin_dir = Path::new(TARGET_DIR).join("bin/");

    if bin_dir.join("vd-tool")
        .try_exists()
        .map_err(|err| Error::io(err, format!("Error checking if file \"{}\" exists", bin_dir.join("vd-tool").display())))?
    {
        return Ok(());
    }

    eprintln!("Downloading vd-tool...");
    command!("curl", "--location", PKG_URL, "--output", zip_file)?;
    let sha256 = Sha256::digest(fs::read(&zip_file)
        .map_err(|err| Error::io(err, format!("Error reading file \"{}\"", zip_file.display())))?
    );
    checksum::<Sha256>(PKG_SHA256, sha256)
        .map_err(|err| Error::io(err, "SHA256 checksum of \"vd-tool\" failed"))?;

    eprintln!("Unpacking vd-tool...");
    command!("unzip", zip_file, "-d", bin_dir)?;

    Ok(())
}

/// Deserialize and *bad* **Vector Drawable** and Serialize to a *good* **Vector Drawable**.
#[derive(Debug, Deserialize, Serialize)]
#[serde(rename = "vector")]
pub struct BadDrawable {
    #[serde(rename = "@xmlns:android", skip_deserializing, default = "BadDrawable::android_namespace")]
    xmlns: String,
    #[serde(rename = "@android:width")]
    width: String,
    #[serde(rename = "@android:height")]
    height: String,
    #[serde(rename = "@android:viewportWidth")]
    viewport_width: String,
    #[serde(rename = "@android:viewportHeight")]
    viewport_height: String,
    #[serde(rename = "@android:tint", skip_deserializing, default = "BadDrawable::tint_attr")]
    tint: String,
    path: BadDrawablePath,
}
#[derive(Debug, Deserialize, Serialize)]
#[serde(rename = "path")]
struct BadDrawablePath {
    #[serde(rename = "@android:fillColor")]
    fill_color: String,
    #[serde(rename = "@android:pathData")]
    path_data: String,
}
impl BadDrawable {
    /// Returns the value that the `android:tint` attribute must have.
    fn tint_attr() -> String { "?android:attr/colorControlNormal".to_string() }
    /// Returns the value that the `xmlns:android` attribute must have.
    fn android_namespace() -> String { "http://schemas.android.com/apk/res/android".to_string() }

    /// [`Deserialize`] a Vector Drawable file with *bad* content.
    pub fn read_file(path: impl AsRef<Path>) -> Result<Self, Error> {
        let path = path.as_ref();
        serde_xml_rs::from_reader::<Self, _>(
            fs::File::open(path)
                .map_err(|err| Error::io(err, format!("Error opening file \"{}\" for reading", path.display())))?
        ).map_err(|err| Error::io_other(err, format!("Error deserializing file \"{}\" as XML", path.display())))
    }

    /// [`Serialize`] the Vector Drawable with the correct data and write the output to a file.
    pub fn write_to_file(&self, path: impl AsRef<Path>) -> Result<(), Error> {
        let path = path.as_ref();
        let config = SerdeXml::new()
            .emitter(EmitterConfig::new()
                .perform_indent(true)
                .write_document_declaration(false)
            );
        
        // Open the file for writing.
        let mut drawable_file = fs::OpenOptions::new()
            .create(true)
            .write(true)
            .truncate(true)
            .open(path)
            .map_err(|err| Error::io(err, format!("Error opening file \"{}\" for writing", path.display())))?;

        config.to_writer(&mut drawable_file, self)
            .map_err(|err| Error::io_other(err, format!("Error serializing Drawable data for \"{}\"", path.display())))
    }
}
