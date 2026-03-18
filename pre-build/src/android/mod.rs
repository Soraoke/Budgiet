use serde::{Deserialize, Deserializer, Serialize, Serializer, ser::SerializeStruct as _};
use serde_xml_rs::SerdeXml;
use xml::EmitterConfig;

use crate::{ErrBuf, Error, IoError};
use std::{ffi::OsString, fs, path::Path};

const DRAWABLE_DIR: &str = "../android/app/src/main/res/drawable";
const ICONS_DIR: &str = "../res/userIcons";

/// Copy and **reformat** `SVG` files in [`ICONS_DIR`] to the `res/drawable` directory in the Android application.
/// 
/// If **dry** is `true`, doesn't write to any files.
/// 
/// The files are converted to Android's proprietary *Drawable Resource (XML)* format and have `"userIcon_"` prefixed to their file name. 
pub fn copy_icons(dry: bool) -> Result<(), Error> {
    let err_buf = ErrBuf::new();

    for entry in fs::read_dir(ICONS_DIR)
        .map_err(|err| IoError::from(err, format!("Error opening directory: {ICONS_DIR}")))?
        .filter_map(|entry| entry.map_err(|err|
            err_buf.push_with_prefix(err, format!("Error reading entry of directory {ICONS_DIR}"))
        ).ok())
    {
        if !entry.metadata()
            .map_err(|err| err_buf.push_with_prefix(err, format!("Error reading metadata of \"{}\"", entry.path().display())))
            .is_ok_and(|meta| meta.is_file())
        {
            continue;
        }

        // Deserialize SVG data
        let svg = match Svg::read_file(entry.path()) {
            Ok(svg) => svg,
            Err(err) => {
                err_buf.push(err);
                continue;
            },
        };

        // The entry's name + a prefix for drawable distinction in Android `res/drawable`.
        let android_file = {
            let mut file_name = OsString::from("userIcon_");
            file_name.push(entry.file_name());
            Path::new(DRAWABLE_DIR).join(file_name)
        };

        if android_file.try_exists()
            .map_err(|err| err_buf.push(IoError::from(err, format!("Error checking if file \"{}\" exists", android_file.display()))))
            .is_ok_and(|exists| exists)
        {
            println!("Vector Drawable \"{}\" already exists; skipping", android_file.display());
            continue;
        }

        // Convert SVG file to Android's proprietatry Drawable format.
        if svg.write_drawable_to_file(if dry { Path::new("/dev/null") } else { &android_file })
            .map_err(|err| err_buf.push(err))
            .is_err()
        {
            continue;
        }

        if dry {
            println!("Dry run: Serialized SVG file \"{}\"", entry.path().display())
        } else {
            println!("Serialized SVG file \"{}\"; wrote to \"{}\"", entry.path().display(), android_file.display())
        }
    }

    if err_buf.is_empty() {
        Ok(())
    } else {
        Err(err_buf.into())
    }
}

#[derive(Deserialize)]
pub struct Svg {
    #[serde(rename = "@width", deserialize_with = "Svg::px")]
    width: u32,
    #[serde(rename = "@height", deserialize_with = "Svg::px")]
    height: u32,
    #[serde(rename = "@viewBox")]
    view_box: ViewBox,
    #[serde(rename = "path")]
    paths: Box<[SvgPath]>,
}
pub struct ViewBox {
    min_x: i32,
    min_y: i32,
    width: u32,
    height: u32,
}
#[derive(Deserialize)]
pub struct SvgPath {
    #[serde(rename = "@fill")]
    fill: Option<String>,
    #[serde(rename = "@d")]
    data: String,
}
impl Svg {
    /// Strip the `"px"` suffix from **width** and **height** attributes.
    fn px<'de, D: Deserializer<'de>>(deserializer: D) -> Result<u32, D::Error> {
        String::deserialize(deserializer)?
            .strip_suffix("px")
            .ok_or( serde::de::Error::custom("Expected value to have \"px\" suffix"))?
            .parse::<u32>()
            .map_err(|err| serde::de::Error::invalid_type(
                serde::de::Unexpected::Other(&format!("Error parsing integer value: {err}")),
                &"unsigned integer",
            ))
    }

    /// Serialize [`SVG``] into Android's proprietary Drawable XML format.
    /// 
    /// For now, it only serializes the `fill` and `d` attributes of the path. 
    fn serialize_drawable<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        // FIXME: Omit the parse instruction (<?xml ... ?>).
        let mut vector = serializer.serialize_struct("vector", 7)?;
        vector.serialize_field("@xmlns:android", "http://schemas.android.com/apk/res/android")?;
        vector.serialize_field("@android:width", &format!("{}dp", self.width))?;
        vector.serialize_field("@android:height", &format!("{}dp", self.height))?;
        // TODO: viewBox min_x, min_y
        vector.serialize_field("@android:viewportWidth", &self.view_box.width)?;
        vector.serialize_field("@android:viewportHeight", &self.view_box.height)?;
        vector.serialize_field("@android:tint", "?android:attr/colorControlNormal")?;
        vector.serialize_field("path",
            &self.paths.iter()
                .map(SvgPath::serializable_drawable)
                .collect::<Box<[_]>>()
        )?;
        vector.end()
    }

    /// [`Deserialize`] a file's SVG content.
    pub fn read_file(path: impl AsRef<Path>) -> Result<Self, IoError> {
        let path = path.as_ref();
        serde_xml_rs::from_reader::<Svg, _>(
            fs::File::open(path)
                .map_err(|err| IoError::from(err, format!("Error opening file \"{}\" for reading", path.display())))?
        ).map_err(|err| IoError::other(err, format!("Error deserializing file \"{}\" as SVG", path.display())))
    }

    /// [`Serialize`] (with indentation) the SVG to a Vector Drawable and write the output to a file.
    pub fn write_drawable_to_file(&self, path: impl AsRef<Path>) -> Result<(), IoError> {
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
            .map_err(|err| IoError::from(err, format!("Error opening file \"{}\" for writing", path.display())))?;

        /// Create a Serializable with the custom drawable serialization function.
        struct SvgDrawable<'a>(&'a Svg);
        impl<'a> Serialize for SvgDrawable<'a> {
            #[inline(always)]
            fn serialize<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
                self.0.serialize_drawable(serializer)
            }
        }
        // Serialize into Android's proprietary Drawable XML format.
        config.to_writer(&mut drawable_file, &SvgDrawable(self))
            .map_err(|err| IoError::other(err, format!("Error serializing SVG data into Drawable for \"{}\"", path.display())))
            .map_err(|err| IoError::other(err, format!("Unable to create XML writer")))
    }
}
impl<'de> Deserialize<'de> for ViewBox {
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        let s = &String::deserialize(deserializer)?;

        let iter = {
            let iter = s.split(' ');
            // Check that split with spaces succeeded, or try split with commas.
            if let Some(_) = iter.clone().next() {
                iter
            } else {
                s.split(',')
            }
        };

        let array = iter
            .map(|val| val.parse::<i32>()
                .map_err(|err| serde::de::Error::invalid_type(
                    serde::de::Unexpected::Other(&format!("Error parsing integer value: {err}")),
                    &"unsigned integer",
                ))
            )
            .collect::<Result<Box<[_]>, _>>()
            .and_then(|slice| <[i32; 4]>::try_from(slice.as_ref())
                .map_err(|_| serde::de::Error::invalid_length(slice.len(), &"4"))
            )?;
        
        Ok(Self {
            min_x: array[0], min_y: array[1],
            width: array[2] as u32, height: array[3] as u32,
        })
    }
}
impl SvgPath {
    /// Return a struct that can [`Serialize`] the `<path>` node of a Android Vector Drawable.
    #[inline(always)]
    fn serializable_drawable(&self) -> SvgPathDrawable<'_> { SvgPathDrawable(self) }
}

struct SvgPathDrawable<'a>(&'a SvgPath);
impl<'a> Serialize for SvgPathDrawable<'a> {
    fn serialize<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        let mut path = serializer.serialize_struct("path", 2)?;
        path.serialize_field("@android:fillColor", self.0.fill.as_ref().map(String::as_str).unwrap_or("@android:color/white"))?;
        path.serialize_field("@android:pathData", &self.0.data)?;
        path.end()
    }
}

/// Crates an `array` in the `res/values` directory with values corresponing to the *userIcon* drawables.
/// This array can later be used to dynamically get the icons in Kotlin code.
/// 
/// If **dry** is `true`, doesn't write to any files.
/// 
/// Must run [`copy_icons()`] before this.
/// 
/// See [this stackoverflow post](https://stackoverflow.com/a/51824649) for more details.
pub fn crate_icons_array(dry: bool) -> Result<(), Error> {
    todo!()
}