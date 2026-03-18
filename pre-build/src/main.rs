mod android;

use std::{cell::RefCell, fmt::{Debug, Display, Write}, io};

use clap::{Parser, Subcommand};

use crate::android::Svg;

#[derive(Parser)]
#[command(version, about, long_about = None)]
struct Cli {
    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand)]
#[clap(rename_all = "lower")]
enum Commands {
    /// Run pre-build for the android application source.
    Android {
        /// Dry run: processes input, but does not write to any files.
        #[arg(short, long)]
        dry: bool,
    },
    /// Convert an SVG file to Android's Vector Drawable proprietary format.
    Svg2Drawable {
        /// The SVG file to convert.
        /// Takes input from **stdin** input is `-`.
        #[arg(short, long)]
        input: String,
        /// The XML file to output to.
        /// Outputs to **stdout** if not passed.
        #[arg(short, long)]
        output: Option<String>,
    }
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    match Cli::parse().command {
        Commands::Android { dry } => {
            // TODO: write gitignore to res/drawable
            android::copy_icons(dry)?;
            android::crate_icons_array(dry)?;
        },
        Commands::Svg2Drawable { input, output } => {
            let svg = Svg::read_file(match input.as_str() {
                "-" => "/dev/stdin",
                path => path,
            })?;
            svg.write_drawable_to_file(output.as_ref().map(String::as_str).unwrap_or("/dev/stdout"))?;
        }
    }

    Ok(())
}

/// An error packing one or multiple other [`IoError`]s.
struct Error(Box<[IoError]>);
impl IntoIterator for Error {
    type Item = IoError;
    type IntoIter = <Box<[Self::Item]> as IntoIterator>::IntoIter;

    #[inline(always)]
    fn into_iter(self) -> Self::IntoIter {
        self.0.into_iter()
    }
}
impl From<ErrBuf> for Error {
    fn from(value: ErrBuf) -> Self {
        Self(value.0.take().into_boxed_slice())
    }
}
impl From<IoError> for Error {
    fn from(value: IoError) -> Self {
        Self(Vec::from([value]).into_boxed_slice())
    }
}
impl Debug for Error {
    #[inline(always)]
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        <Box<[_]> as Debug>::fmt(&self.0, f)
    }
}
impl Display for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        for (i, error) in self.0.iter().enumerate() {
            <IoError as Display>::fmt(error, f)?;

            if i != self.0.len() - 1 {
                f.write_char('\n')?;
            }
        }
        Ok(())
    }
}
impl std::error::Error for Error { }

/// An [`io::Error`] with a proper prefix message.
#[derive(Debug)]
struct IoError {
    message: String,
    error: io::Error,
}
impl IoError {
    pub fn from(err: io::Error, msg: impl Display) -> Self {
        Self {
            message: msg.to_string(),
            error: err,
        }
    }
    pub fn other(err: impl Into<Box<dyn std::error::Error + Send + Sync>>, msg: impl Display) -> Self {
        Self {
            message: msg.to_string(),
            error: io::Error::other(err),
        }
    }
}
impl std::error::Error for IoError {
    #[inline(always)]
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        self.error.source()
    }
}
impl Display for IoError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.message)?;
        <std::io::Error as Display>::fmt(&self.error, f)
    }
}

struct ErrBuf(RefCell<Vec<IoError>>);
#[allow(unused)]
impl ErrBuf {
    pub fn new() -> Self {
        Self(RefCell::new(Vec::new()))
    }

    pub fn push(&self, err: IoError) {
        self.0.borrow_mut().push(err)
    }
    /// Push to the buffer an [`io::Error`] with a propper message prefix.
    pub fn push_with_prefix(&self, err: io::Error, msg: impl Display) {
        self.push(IoError::from(err, msg));
    }

    pub fn extend(&self, iter: impl IntoIterator<Item = IoError>) {
        self.0.borrow_mut().extend(iter);
    }

    pub fn is_empty(&self) -> bool {
        self.0.borrow().is_empty()
    }
}
