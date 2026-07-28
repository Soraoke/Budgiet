use std::{ffi::OsString, fmt::Display, fs, io, path::PathBuf};
use crate::{Error, PROJECT_ROOT, TARGET_DIR, command, command_output, static_path, utils::{read_dir, recursive_dir_mtime}};

pub enum BoltFfiPlatform {
    Apple, Android, // Java, CSharp, Wasm, Python
}
impl BoltFfiPlatform {
    pub fn add_rust_targets(&self, verbose: bool) -> Result<(), Error> {
        if verbose { println!("Adding Rust platform targets..."); }
        let add = |name: &str| -> Result<(), Error> {
            command!("rustup", "target", "add", name)?;
            if verbose { println!("Added rust target \"{name}\""); }
            Ok(())
        };

        match self {
            Self::Android => {
                add("aarch64-linux-android")?;
                add("armv7-linux-androideabi")?;
                add("x86_64-linux-android")?;
                add("i686-linux-android")?;
            },
            Self::Apple => todo!(),
        }
        Ok(())
    }

    /// The directory where the resulting packed code will be placed in.
    pub fn packed_dir(&self) -> PathBuf {
        match self {
            Self::Android => PROJECT_ROOT.join("android/app/src/main/kotlin"),
            Self::Apple => todo!(),
        }
    }

    pub fn generate_env(&self) -> Result<Box<[(OsString, OsString)]>, Error> {
        match self {
            Self::Android => {
                // Find NDK_HOME.
                let ndk_home = match std::env::var("ANDROID_NDK_HOME") {
                    Ok(val) => PathBuf::from(val),
                    Err(_) => match std::env::var("ANDROID_HOME") {
                        Ok(val) => {
                            let android_home = PathBuf::from(val);

                            // Find the latest version.
                            let mut versions = read_dir(&android_home.join("Sdk/ndk"))
                                .filter_map(Result::ok)
                                .filter_map(|entry| entry.metadata().ok().map(|meta| meta.is_dir().then_some(entry)).flatten())
                                .collect::<Box<[_]>>();
                            versions.sort_by(|a, b| a.file_name().cmp(&b.file_name()));

                            versions.iter()
                                .find(|entry| entry.file_name() == "latest")
                                .or(versions.get(0))
                                .ok_or(Error::with_prefix(io::Error::from(io::ErrorKind::NotFound), "Error: NDK version directory is empty"))?
                                .path()
                        },
                        Err(_) => return Err(Error::new(format!(
                            "Could not find NDK directory. Please set either ANDROID_NDK_HOME or ANDROID_HOME environment variables"
                        ))),
                    },
                };

                Ok(vec![(
                    "ANDROID_NDK_HOME".into(), ndk_home.into(),
                )].into())
            },
            Self::Apple => todo!(),
        }
    }
}
impl Display for BoltFfiPlatform {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(match self {
            Self::Apple => "apple",
            Self::Android => "android",
            // Self::Java => "java",
            // Self::CSharp => "csharp",
            // Self::Wasm => "wasm",
            // Self::Python => "python",
        })
    }
}

pub fn pack_rust_lib(platform: BoltFfiPlatform, verbose: bool, dry: bool) -> Result<(), Error> {
    static_path! { BOLT_FFI = TARGET_DIR.join("bin/boltffi") }

    if let Err(_) = command_output!("command", "-v", BOLT_FFI.as_path()) {
        // Get version of boltffi_cli to install from the Rust native code Cargo.toml.
        // Note: boltffi_cli version must match that of the library crate.
        let version = {
            let path = PROJECT_ROOT.join("rust/Cargo.toml");
            let cargo = toml::from_str::<toml::Value>(
                fs::read_to_string(&path)
                    .map_err(|err| Error::with_prefix(err, format!("Error reading file \"{}\"", path.display())))?
                    .as_str()
            ).map_err(|err| Error::with_prefix(err, format!("Error parsing TOML value from \"{}\"", path.display())))?;

            cargo.as_table()
                .and_then(|cargo| cargo.get("dependencies").and_then(toml::Value::as_table))
                .and_then(|deps| deps.get("boltffi"))
                .and_then(|deps| match deps {
                    // boltffi dependency version is defined within a table (i.e. `boltffi = { version = "<version>" }`).
                    toml::Value::Table(boltffi) => boltffi.get("version")
                        .and_then(toml::Value::as_str)
                        .map(|v| v.to_string()),
                    // boltffi dependency version is defined inline (i.e. `boltffi = "<version>"`).
                    toml::Value::String(version) => Some(version.to_string()),
                    // boltffi dependency does not exist.
                    _ => None,
                })
                .ok_or_else(|| Error::new(format!("Dependency \"boltffi\" is not defined in manifest file \"{}\"", path.display())))?
        };

        let package_id = format!("boltffi_cli@{version}");

        if verbose { eprintln!("Installing {package_id}..."); }
        if !dry { command!("cargo", "install", format!("{package_id}"), "--root", TARGET_DIR.as_path())?; }
        if verbose { eprintln!("Finished installing {package_id}"); }
    }

    if !dry { platform.add_rust_targets(verbose)?; }

    // Check if boltffi.toml exists
    if !PROJECT_ROOT.join("boltffi.toml")
        .try_exists()
        .map_err(|err| Error::with_prefix(err, format!("Error checking if file ./boltffi.toml exists")))?
    {
        return Err(Error::new(format!("File ./boltffi.toml is required to exist at the project root directory, but was not found.\nRun command \"{} init\" and edit the file as required.", BOLT_FFI.display())))
    }

    let env = platform.generate_env()?;
    let env = env.iter()
        .map(|(k, v)| (k.as_os_str(), v.as_os_str()))
        .collect::<Box<[_]>>();

    // Skip packing if the the Rust code has NOT been modified since the last time it was packed.
    let already_packed = {
        let platform_packed_mtime = match recursive_dir_mtime(platform.packed_dir().as_path()) {
            Ok(mtime) => Some(mtime),
            // Platform package has not been packed yet
            Err(err) if err.io_error_kind().is_some_and(|err| err == io::ErrorKind::NotFound) => None,
            Err(err) => return Err(err),
        };
        let rust_code_mtime = recursive_dir_mtime(PROJECT_ROOT.join("rust/src").as_path())?;

        platform_packed_mtime
            .is_some_and(|mtime| mtime > rust_code_mtime)
    };

    if already_packed {
        eprintln!("budgietlib for {platform} is already packed and not modified; Skipping")
    } else {
        if !dry {
            if verbose {
                command!(ENV => &env, BOLT_FFI.as_path(), "pack", &platform.to_string(), "-v")?;
            } else {
                command!(ENV => &env, BOLT_FFI.as_path(), "pack", &platform.to_string())?;
            }
        } else {
            eprintln!("DRY RUN: packed budgietlib for {platform}")
        }
    }

    Ok(())
}
