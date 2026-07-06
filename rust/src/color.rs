#![allow(unstable_name_collisions)]

use std::{fmt::Display, str::FromStr};
use boltffi::{data, export};
use serde::{Deserialize, Serialize};
use thiserror::Error;
use itertools::Itertools as _;
use crate::utils::{IterResultExt as _, StringVisitor};

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
    /// Creates a color from a number with the format `0xRRGGBB`.
    pub const fn new_rgb(code: u32) -> Self {
        Self {
            red:   ((code & 0xFF0000) >> 16) as u8,
            green: ((code & 0x00FF00) >> 8) as u8,
            blue:  (code & 0x0000FF) as u8,
            alpha: 0xFF,
        }
    }
    /// Creates a color from a number with the format `0xRRGGBBAA`.
    pub const fn new_rgba(code: u32) -> Self {
        Self {
            red:   ((code & 0xFF000000) >> 24) as u8,
            green: ((code & 0x00FF0000) >> 16) as u8,
            blue:  ((code & 0x0000FF00) >> 8) as u8,
            alpha: (code & 0x000000FF) as u8,
        }
    }
    /// Creates a color from a number with the format `0xAARRGGBB`.
    pub const fn new_argb(code: u32) -> Self {
        Self {
            alpha:   ((code & 0xFF000000) >> 24) as u8,
            green: ((code & 0x00FF0000) >> 16) as u8,
            blue:  ((code & 0x0000FF00) >> 8) as u8,
            red: (code & 0x000000FF) as u8,
        }
    }

    /// Converts the [`Color`] to a [`String`] with the format `"RRGGBB"`.
    pub fn format_rgb(&self) -> String {
        fn component_to_hex(n: u8) -> String { format!("{n:02X}") }
        let mut buf = String::new();

        buf.push_str(&component_to_hex(self.red));
        buf.push_str(&component_to_hex(self.green));
        buf.push_str(&component_to_hex(self.blue));

        buf
    }
    /// Converts the [`Color`] to a [`String`] with the format `"RRGGBBAA"`.
    pub fn format_rgba(&self) -> String {
        fn component_to_hex(n: u8) -> String { format!("{n:02X}") }
        let mut buf = String::new();

        buf.push_str(&component_to_hex(self.red));
        buf.push_str(&component_to_hex(self.green));
        buf.push_str(&component_to_hex(self.blue));
        buf.push_str(&component_to_hex(self.alpha));

        buf
    }

    /// Parse a *Hexadecimal string* to obtain an `RGB[A]` [`Color`].
    pub fn from_hex(hex: &str, allow_alpha: bool) -> Result<Self, ColorParseError> {
        let len = hex.len();
        if (len == 4 || len == 8) && !allow_alpha {
            return Err(ColorParseError::AlphaNotAllowed);
        }

        let hex_digits = hex.chars()
            .map(|c| match c {
                '0'..='9' => Ok(c as u8 - b'0'),
                'a'..='f' => Ok(c as u8 - b'a' + 10),
                'A'..='F' => Ok(c as u8 - b'A' + 10),
                _ => Err(c),
            })
            .collect_results::<Box<[_]>>()
            .map_err(|errs| ColorParseError::InvalidChars { chars: errs.into_iter().collect::<String>() })?;
        let hex_digits = hex_digits.iter().as_slice();

        // Place 2 *Hexadecimal digit* from **`hex_digits`** (of choice) into the returned number.
        // The 2 digits are obtained from the provided **hex_digits indices**.
        let merge_digits = |idx1: usize, idx2: usize| -> u8 {
            hex_digits[idx1] << 4 | hex_digits[idx2]
        };

        match hex.len() {
            0..=2 => Err(ColorParseError::InvalidHexLength { len }),
            3..=4 => Ok(Self {
                red:   merge_digits(0, 0),
                green: merge_digits(1, 1),
                blue:  merge_digits(2, 2),
                alpha: if len > 3 { merge_digits(3, 3) } else { 0xFF },
            }),
            6 | 8 => Ok(Self {
                red:   merge_digits(0, 1),
                green: merge_digits(2, 3),
                blue:  merge_digits(4, 5),
                alpha: if len > 6 { merge_digits(6, 7) } else { 0xFF },
            }),
            _ => Err(ColorParseError::InvalidHexLength { len }),
        }
    }
}
#[data(impl)]
impl Display for Color {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&if self.alpha == 0xFF {
            self.format_rgb()
        } else {
            self.format_rgba()
        })
    }
}
#[data(impl)]
impl FromStr for Color {
    type Err = ColorParseError;

    #[inline]
    fn from_str(hex: &str) -> Result<Self, Self::Err> {
        Self::from_hex(hex, true)
    }
}
impl Serialize for Color {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where S: serde::Serializer {
        serializer.serialize_str(&self.to_string())
    }
}
impl<'de> Deserialize<'de> for Color {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where D: serde::Deserializer<'de> {
        let hex = deserializer.deserialize_str(StringVisitor::new("A Hexadecimal number between 3 and 8 digits long"))?;
        Self::from_hex(&hex, true)
            .map_err(|err| serde::de::Error::custom(err))
    }
}

#[boltffi::error]
#[derive(Debug, Clone, Error)]
pub enum ColorParseError {
    #[error("Hex code contained invalid characters ['{}']. Hex characters must be decimal digits (0-9) or A-F (case insensitive)",
        chars.chars()
            .map(|c| c.to_string())
            .intersperse(", ".to_string())
            .collect::<String>()
    )]
    InvalidChars { chars: String },
    #[error("Called 'Color::from_hex(allow_alpha = false)', but hex code contained characters for Alpha (opacity) channel")]
    AlphaNotAllowed,
    #[error("Hex code must be {phrase} {limit} characters in length, but was {len}",
        phrase = if *len < 3 { "at least" } else { "at most" },
        limit = if *len < 3 { 3 } else { 8 },
    )]
    InvalidHexLength { len: usize },
}

/// TODO: doc
#[export]
pub fn correct_color_contrast(background: Color, foreground: Color) -> Color {
    // if (background.alpha < 0.35) {
    //     MaterialTheme.colorScheme.onSurface
    // } else {
    //     MaterialTheme.colorScheme.contentColorFor(background)
    //         .takeOrElse {
    //             if (background.luminance() < 0.5f) {
    //                 DarkColorScheme.onPrimaryContainer
    //             } else {
    //                 LightColorScheme.onPrimaryContainer
    //             }
    //         }
    // }
    todo!()
}

pub struct UserColorPalette;
#[allow(non_upper_case_globals)]
#[export]
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
    fn number_parsing() {
        assert_eq!(Color::new_rgb(0xFA0000), Color { red: 0xFA, green: 0x00, blue: 0x00, alpha: 0xFF });
        assert_eq!(Color::new_rgb(0x00FA00), Color { red: 0x00, green: 0xFA, blue: 0x00, alpha: 0xFF });
        assert_eq!(Color::new_rgb(0x0000FA), Color { red: 0x00, green: 0x00, blue: 0xFA, alpha: 0xFF });
        assert_eq!(Color::new_rgba(0xFA000000), Color { red: 0xFA, green: 0x00, blue: 0x00, alpha: 0x00 });
        assert_eq!(Color::new_rgba(0x00FA0000), Color { red: 0x00, green: 0xFA, blue: 0x00, alpha: 0x00 });
        assert_eq!(Color::new_rgba(0x0000FA00), Color { red: 0x00, green: 0x00, blue: 0xFA, alpha: 0x00 });
        assert_eq!(Color::new_rgba(0x000000FA), Color { red: 0x00, green: 0x00, blue: 0x00, alpha: 0xFA });
    }

    #[test]
    fn string_parsing() {
        assert_eq!(Color::from_hex("FA0000", false).unwrap(), Color::new_rgb(0xFA0000));
        assert_eq!(Color::from_hex("00FA00", false).unwrap(), Color::new_rgb(0x00FA00));
        assert_eq!(Color::from_hex("0000FA", false).unwrap(), Color::new_rgb(0x0000FA));
        assert_eq!(Color::from_hex("FA000000", true).unwrap(), Color::new_rgba(0xFA000000));
        assert_eq!(Color::from_hex("00FA0000", true).unwrap(), Color::new_rgba(0x00FA0000));
        assert_eq!(Color::from_hex("0000FA00", true).unwrap(), Color::new_rgba(0x0000FA00));
        assert_eq!(Color::from_hex("000000FA", true).unwrap(), Color::new_rgba(0x000000FA));
    }

    #[test]
    fn format() {
        assert_eq!(Color::new_rgb(0xFA0000).to_string(), "FA0000");
        assert_eq!(Color::new_rgb(0x00FA00).to_string(), "00FA00");
        assert_eq!(Color::new_rgb(0x0000FA).to_string(), "0000FA");
        assert_eq!(Color::new_rgba(0xFA000000).to_string(), "FA000000");
        assert_eq!(Color::new_rgba(0x00FA0000).to_string(), "00FA0000");
        assert_eq!(Color::new_rgba(0x0000FA00).to_string(), "0000FA00");
        assert_eq!(Color::new_rgba(0x000000FA).to_string(), "000000FA");
    }
}
