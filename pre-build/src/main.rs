mod android;
mod utils;

use std::{fmt::{Debug, Display, Write}, fs, io, path::{Path, PathBuf}, process::exit};
use clap::{Parser, Subcommand};
use crate::{android::svg2drawable::{BadDrawable, svg_to_bad_drawable}, utils::read_dir_with};

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
        /// Can be a directory.
        /// Takes input from **stdin** input is `-`.
        #[arg(short, long)]
        input: PathBuf,
        /// The XML file to output to.
        /// Must be a file or directory, depening on the value of **input**.
        /// Outputs to **stdout** if not passed.
        #[arg(short, long)]
        output: Option<PathBuf>,
    }
}

fn main() {
    if let Err(err) = _main() {
        eprintln!("{err}");
        exit(1);
    }
}
fn _main() -> Result<(), Box<dyn std::error::Error>> {
    match Cli::parse().command {
        Commands::Android { dry } => {
            // TODO: write gitignore to res/drawable
            android::svg2drawable::copy_icons(dry)?;
            println!("\nConverted SVG files to usable Vector Drawables; now adding array with icon names...\n");
            android::create_icons_array(dry)?;
            println!("\nDone!");
        },
        Commands::Svg2Drawable { input, output } => {
            // Don't have to worry about symlinks here, metadata follows them.
            let input_is_file = input.metadata()
                .map_err(|err| IoError::from(err, format!("Error checking if \"{}\" is a file", input.display())))?
                .is_file()
                // consider stdin to be a file.
                || input.as_os_str() == "-";
            let output_is_file = match &output {
                Some(output) => output.metadata()
                    .map_err(|err| IoError::from(err, format!("Error checking if \"{}\" is a file", input.display())))?
                    .is_file(),
                // Consider stdout to be a file.
                None => true,
            };
            // Unwrap input and output paths.
            let input = &if input.as_os_str() == "-" {
                PathBuf::from("/dev/stdin")
            } else {
                input
            };
            let output = output.as_ref().map(PathBuf::as_path).unwrap_or(Path::new("/dev/stdout"));

            // Check that input and output are BOTH file or directory.
            if input_is_file && !output_is_file {
                return Err(format!("Expected output \"{}\", to be a file", output.display()).into());
            } else if !input_is_file && output_is_file {
                return Err(format!("Expected output \"{}\", to be a directory, but it is a file", output.display()).into());
            }

            let tmp_output = &svg_to_bad_drawable(input)?;
            fn delete_tmp_file(path: &Path) -> Result<(), IoError> {
                // Delete the temporary, bad drawable file.
                fs::remove_file(path)
                    .map_err(|err| IoError::from(err, format!("Error deleting file \"{}\"", path.display())))
            }

            if output_is_file {
                // tmp file must be removed regardless of result.
                let result = BadDrawable::read_file(tmp_output)
                    .and_then(|drawable| drawable.write_to_file(output));

                delete_tmp_file(&tmp_output)?;
                result?
            } else {
                read_dir_with(tmp_output, |entry| {
                    // Deserialize Vector Drawable with bad data.
                    let result = BadDrawable::read_file(entry.path())
                        .and_then(|drawable| drawable.write_to_file(output.join(entry.file_name())));

                    delete_tmp_file(&entry.path())?;
                    result
                })?;
            }
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
impl From<IoError> for Error {
    fn from(value: IoError) -> Self {
        Self(Vec::from([value]).into_boxed_slice())
    }
}
impl From<Vec<IoError>> for Error {
    #[inline(always)]
    fn from(value: Vec<IoError>) -> Self {
        Self(value.into_boxed_slice())
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
        f.write_str(": ")?;
        <std::io::Error as Display>::fmt(&self.error, f)
    }
}
