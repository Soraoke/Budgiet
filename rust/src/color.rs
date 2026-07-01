use boltffi::data;

#[data]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Color {
    pub red: u8,
    pub green: u8,
    pub blue: u8,
    pub alpha: u8,
}
#[data(impl)]
impl Color {
    pub const fn new_rgb(code: u32) -> Self {
        Self {
            red:   ((code & 0xFF0000) >> 16) as u8,
            green: ((code & 0x00FF00) >> 8) as u8,
            blue:  (code & 0x0000FF) as u8,
            alpha: 0xFF,
        }
    }
    pub const fn new_rgba(code: u32) -> Self {
        Self {
            red:   ((code & 0xFF000000) >> 24) as u8,
            green: ((code & 0x00FF0000) >> 16) as u8,
            blue:  ((code & 0x0000FF00) >> 8) as u8,
            alpha: (code & 0x000000FF) as u8,
        }
    }

    // #[cfg(target_os = "android")]
    // pub fn from_android_packed_value(val: u64) -> Self {

    // }
    // #[cfg(target_os = "android")]
    // pub fn to_android_packed_value(self) -> u64 {

    // }
}

pub struct UserColorPalette;
#[allow(non_upper_case_globals)]
#[data(impl)]
impl UserColorPalette {
    pub const Red       : Color = Color::new_rgb(0xF3413D);
    pub const Orange    : Color = Color::new_rgb(0xFA7B40);
    pub const Brown     : Color = Color::new_rgb(0xB37200);
    pub const Yellow    : Color = Color::new_rgb(0xF3E248);
    pub const Green     : Color = Color::new_rgb(0x21BF13);
    pub const Forest    : Color = Color::new_rgb(0x00966E);
    pub const Turquoise : Color = Color::new_rgb(0x37FDAD);
    pub const Cyan      : Color = Color::new_rgb(0x34F6FA);
    pub const Blue      : Color = Color::new_rgb(0x3D50F3);
    pub const Purple    : Color = Color::new_rgb(0xAA07FF);
    pub const Lavender  : Color = Color::new_rgb(0xD88FFF);
    pub const Pink      : Color = Color::new_rgb(0xFF84EF);
    pub const Grey      : Color = Color::new_rgb(0xCFCFCF);
    pub const DarkGrey  : Color = Color::new_rgb(0x6B6B6B);

    /// Returns a slice containing the colors of the defined [`UserColorPalette`].
    ///
    /// NOTE: This function should only be called *ONCE* in the entire lifetime of the program.
    /// The returned list should be cached in the memory of the native application running.
    pub const fn list() -> &'static [Color] { USER_COLOR_PALETTE_LIST }
}

// TODO: use a macro instead
static USER_COLOR_PALETTE_LIST: &[Color] = &[
    UserColorPalette::Red,
    UserColorPalette::Orange,
    UserColorPalette::Brown,
    UserColorPalette::Yellow,
    UserColorPalette::Green,
    UserColorPalette::Forest,
    UserColorPalette::Turquoise,
    UserColorPalette::Cyan,
    UserColorPalette::Blue,
    UserColorPalette::Purple,
    UserColorPalette::Lavender,
    UserColorPalette::Pink,
    UserColorPalette::Grey,
    UserColorPalette::DarkGrey,
];

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn color_parsing() {
        assert_eq!(Color::new_rgb(0xFA0000), Color { red: 0xFA, green: 0x00, blue: 0x00, alpha: 0xFF });
        assert_eq!(Color::new_rgb(0x00FA00), Color { red: 0x00, green: 0xFA, blue: 0x00, alpha: 0xFF });
        assert_eq!(Color::new_rgb(0x0000FA), Color { red: 0x00, green: 0x00, blue: 0xFA, alpha: 0xFF });
        assert_eq!(Color::new_rgba(0xFA000000), Color { red: 0xFA, green: 0x00, blue: 0x00, alpha: 0x00 });
        assert_eq!(Color::new_rgba(0x00FA0000), Color { red: 0x00, green: 0xFA, blue: 0x00, alpha: 0x00 });
        assert_eq!(Color::new_rgba(0x0000FA00), Color { red: 0x00, green: 0x00, blue: 0xFA, alpha: 0x00 });
        assert_eq!(Color::new_rgba(0x000000FA), Color { red: 0x00, green: 0x00, blue: 0x00, alpha: 0xFA });
    }
}
