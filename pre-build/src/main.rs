mod android;
mod ffi;
mod utils;
mod locale_currency;

use std::{error::Error as StdError, fs, path::{Path, PathBuf}, process::exit};
use clap::{Parser, Subcommand};
use common::{Error, IterResultExt as _};
use crate::{android::svg2drawable::{BadDrawable, svg_to_bad_drawable}, ffi::pack_rust_lib, locale_currency::gen_locale_currency_map_src, utils::read_dir};

static_path! { pub PROJECT_ROOT = project_root::get_project_root().expect("Could not find root directory of the Rust project") }
static_path! { pub TARGET_DIR = PROJECT_ROOT.join("target") }

#[derive(Parser)]
#[command(version, about, long_about = None)]
struct Cli {
    /// Output to stderr information about the operations performed by the program.
    #[arg(long, short)]
    verbose: bool,
    /// Dry run: processes input, but does not write to any files.
    #[arg(short, long)]
    dry: bool,
    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand)]
#[clap(rename_all = "lower")]
enum Commands {
    /// Run pre-build for the android application source.
    Android { },
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
fn _main() -> Result<(), Box<dyn StdError>> {
    let Cli { verbose, dry, command } = Cli::parse();
    match command {
        Commands::Android { } => {
            android::svg2drawable::copy_icons(verbose, dry)?;
            if verbose {
                eprintln!("\nConverted SVG files to usable Vector Drawables; now adding array with icon names...\n");
            }
            android::create_icons_array(verbose, dry)?;
            // android::create_gitignore(verbose, dry)?;
            gen_locale_currency_map_src(verbose, dry)?;
            if verbose {
                eprintln!("\nDone!");
            }
            pack_rust_lib(ffi::BoltFfiPlatform::Android, verbose, dry)?;
        },
        Commands::Svg2Drawable { input, output } => {
            // Don't have to worry about symlinks here, metadata follows them.
            let input_is_file = input.metadata()
                .map_err(|err| Error::with_prefix(err, format!("Error checking if \"{}\" is a file", input.display())))?
                .is_file()
                // consider stdin to be a file.
                || input.as_os_str() == "-";
            let output_is_file = match &output {
                Some(output) => output.metadata()
                    .map_err(|err| Error::with_prefix(err, format!("Error checking if \"{}\" is a file", input.display())))?
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

            let tmp_output = &svg_to_bad_drawable(input, verbose, dry)?;
            fn delete_tmp_file(path: &Path) -> Result<(), Error> {
                // Delete the temporary, bad drawable file.
                fs::remove_file(path)
                    .map_err(|err| Error::with_prefix(err, format!("Error deleting file \"{}\"", path.display())))
            }

            if output_is_file {
                // tmp file must be removed regardless of result.
                let result = BadDrawable::read_file(tmp_output)
                    .and_then(|drawable| drawable.write_to_file(output));

                delete_tmp_file(&tmp_output)?;
                result?
            } else {
                read_dir(tmp_output)
                    .map(|result| result.and_then(|entry| {
                        // Deserialize Vector Drawable with bad data.
                        let result = BadDrawable::read_file(entry.path())
                            .and_then(|drawable| drawable.write_to_file(output.join(entry.file_name())));

                        delete_tmp_file(&entry.path())?;
                        result
                    }))
                    .collect_results::<()>()?;
            }
        }
    }

    Ok(())
}
