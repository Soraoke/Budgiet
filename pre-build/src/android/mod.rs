pub mod svg2drawable;

use crate::Error;

/// Crates an `array` in the `res/values` directory with values corresponing to the *userIcon* drawables.
/// This array can later be used to dynamically get the icons in Kotlin code.
/// 
/// If **dry** is `true`, doesn't write to any files.
/// 
/// Must run [`copy_icons()`] before this.
/// 
/// See [this stackoverflow post](https://stackoverflow.com/a/51824649) for more details.
pub fn create_icons_array(dry: bool) -> Result<(), Error> {
    todo!()
}