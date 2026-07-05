use std::{ffi::OsString, fs, io, path::{Path, PathBuf}};
use serde::{Deserialize, Serialize};
use serde_xml_rs::SerdeXml;
use sha2::{Digest as _, Sha256};
use xml::EmitterConfig;
use super::{DRAWABLE_DIR, ICONS_DIR, USER_ICON_DRAWABLE_PREFIX};
use crate::{Error, Errors, TARGET_DIR, command_output, static_path, utils::{IterResultExt as _, checksum, read_dir}};

/// Copy and **reformat** `SVG` files in [`ICONS_DIR`] to the `res/drawable` directory in the Android application.
///
/// If **`verbose`** is `true`, prints information about an opperation to *stderr*.
/// If **`dry`** is `true`, doesn't write to any files.
///
/// The files are converted to Android's proprietary *Drawable Resource (XML)* format and have `"userIcon_"` prefixed to their file name.
pub fn copy_icons(verbose: bool, dry: bool) -> Result<(), Errors<Error>> {
    let tmp_dir = &svg_to_bad_drawable(ICONS_DIR.as_path(), verbose, dry)?;

    if verbose {
        eprintln!("\nConverted SVGs to Vector Drawable; now fixing output files...\n");
    }

    // The converted Vector Drawables are missing some attributes, add those here.
    read_dir(tmp_dir).map(|result| result.and_then(|entry| {
        // The entry's name + a prefix for drawable distinction in Android `res/drawable`.
        let android_file = {
            let mut file_name = OsString::from(USER_ICON_DRAWABLE_PREFIX);
            file_name.push(entry.file_name());
            DRAWABLE_DIR.join(file_name)
        };

        if android_file.try_exists()
            .map_err(|err| Error::with_prefix(err, format!("Error checking if file \"{}\" exists", android_file.display())))?
        {
            if verbose {
                eprintln!("Vector Drawable \"{}\" already exists; skipping", android_file.display());
            }
            return Ok(());
        }

        // Deserialize Vector Drawable with bad data.
        let drawable = BadDrawable::read_file(entry.path())?;

        // Correct the data in the Drawable to a file in the android drawables directory.
        drawable.write_to_file(if dry { Path::new("/dev/null") } else { &android_file })?;

        // Delete the temporary, bad drawable file.
        fs::remove_file(entry.path())
            .map_err(|err| Error::with_prefix(err, format!("Error deleting file \"{}\"", entry.path().display())))?;

        if verbose {
            if dry { eprint!("Dry run: "); }
            eprint!("Converted SVG file \"{}\"", ICONS_DIR.join(entry.file_name()).with_extension("svg").display());
            if !dry {
                eprint!("; wrote to \"{}\"", android_file.display());
            }
            eprintln!("");
        }

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
pub fn svg_to_bad_drawable(path: impl AsRef<Path>, verbose: bool, dry: bool) -> Result<PathBuf, Error> {
    static_path! { VD_TOOL_PATH = TARGET_DIR.join("bin/vd-tool/bin/vd-tool") }

    let path = path.as_ref();
    let tmp_dir = TARGET_DIR.as_path()
        .join("tmp")
        .join(path.file_name()
            .ok_or_else(|| Error::new(format!("Path to SVG[s] must be a file or directory")))?
        );

    let input_is_file = path.metadata()
        .map_err(|err| Error::with_prefix(err, format!("Error checking if \"{}\" is a file", path.display())))?
        .is_file();
    let return_path = if input_is_file {
        // Won't panic because a path to a file that exists will not terminate in '..'.
        tmp_dir.join(path.file_name().unwrap()).with_extension("xml")
    } else {
        tmp_dir.clone()
    };

    if !dry {
        fs::create_dir_all(&tmp_dir)
            .map_err(|err| Error::with_prefix(err, format!("Error creating directory \"{}\"", tmp_dir.display())))?;

        // Check if this operation was already done by checking that ALL files in path exist in tmp_dir.
        let files_exist = read_dir(path)
            .map(|entry| entry.and_then(|entry| {
                let path = entry.path();
                let name = path.file_stem()
                    .ok_or_else(|| Error::new(format!("Invalid SVG file found: \"{}\"", entry.path().display())))?;

                // Check if "{path}/{name}.svg" also exists as "{tmp_dir}/{name}.xml"
                tmp_dir.join(name).with_extension("xml")
                    .try_exists()
                    .map_err(|err| Error::with_prefix(err, format!("Error checking if file \"{}\" exists", tmp_dir.join(name).with_extension("xml").display())))
            }))
            .collect::<Result<Box<[_]>, _>>()?;

        if files_exist.iter().all(|exist| *exist) {
            if verbose {
                eprintln!("SVGs in \"{}\" were already converted to *bad* Drawables; skipping", path.display())
            }
            return Ok(return_path);
        }

        // Operation was not done, so do it here.
        unpack_vd_tool(verbose)?;
        command_output!(VD_TOOL_PATH.as_path(), "-c", "-in", path, "-out", tmp_dir)?;
        if verbose {
            eprint!("Converted all SVG files in \"{}\" to *bad* Drawable files in \"{}\"", path.display(), tmp_dir.display());
            eprintln!("");
        }
    } else if verbose {
        eprintln!("Dry run: Skipping converting SVGs to Vector drawable")
    }

    Ok(return_path)
}

/// `vd-tool` stands for ***Vector Drawable Tool***,
/// and has the functionality to convert **SVG to Vector Drawable** with all the features of both formats.
///
// ---
// The tool is NOT packaged in this repo, it must be downloaded from the repackager repo and then that downloads the actual code that we compile here.
// Both the source code and the java binary must be in the `/target` directory of the project.
// Scratch that... I was going to do this but its just gonna make CI run slower, so just download the built package in the github.
fn unpack_vd_tool(verbose: bool) -> Result<(), Error> {
    static PKG_NAME: &str = "vd-tool";
    static PKG_URL: &str = "https://github.com/rharter/vd-tool/releases/download/v1/vd-tool.zip";
    static PKG_SHA256: &str = "9bc7b2046b51e22c62663a93c9e91c3b29b053a36a5484a0d73d8c54def3e595";

    let zip_file = TARGET_DIR.join(PKG_NAME).with_extension("zip");
    let unpkg_dir = TARGET_DIR.join("bin").join(PKG_NAME);

    if unpkg_dir.try_exists()
        .map_err(|err| Error::with_prefix(err, format!("Error checking if file \"{}\" exists", unpkg_dir.display())))?
    {
        if verbose {
            eprintln!("vd-tool already downloaded; Skipping")
        }
        return Ok(());
    }

    if verbose {
        eprintln!("Downloading {PKG_NAME}...");
    }
    command_output!("curl", "--location", PKG_URL, "--output", zip_file)?;
    let sha256 = Sha256::digest(fs::read(&zip_file)
        .map_err(|err| Error::with_prefix(err, format!("Error reading file \"{}\"", zip_file.display())))?
    );
    checksum::<Sha256>(PKG_SHA256, sha256)
        .map_err(|err| Error::with_prefix(err, format!("SHA256 checksum of \"{PKG_NAME}\" failed")))?;
    if verbose {
        eprintln!("Download completed");
    }

    command_output!("unzip", zip_file, "-d", unpkg_dir.parent().unwrap())?;
    if verbose {
        eprintln!("Unpacked {PKG_NAME} to \"{}\"", unpkg_dir.display());
    }

    fs::remove_file(&zip_file)
        .map_err(|err| Error::with_prefix(err, format!("Error deleting file \"{}\"", zip_file.display())))?;

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
                .map_err(|err| Error::with_prefix(err, format!("Error opening file \"{}\" for reading", path.display())))?
        ).map_err(|err| Error::with_prefix(io::Error::other(err), format!("Error deserializing file \"{}\" as XML", path.display())))
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
            .map_err(|err| Error::with_prefix(err, format!("Error opening file \"{}\" for writing", path.display())))?;

        config.to_writer(&mut drawable_file, self)
            .map_err(|err| Error::with_prefix(io::Error::other(err), format!("Error serializing Drawable data for \"{}\"", path.display())))
    }
}
