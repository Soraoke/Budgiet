use std::{ffi::OsString, fmt::Display, io, path::PathBuf};
use crate::{Error, command, utils::read_dir};

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
                                .ok_or(Error::io(io::Error::new(io::ErrorKind::NotFound, "File not found"), "Error: NDK version directory is empty"))?
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
    let command_path = "./target/bin/boltffi";

    if let Err(_) = command!("command", "-v", command_path) {
        if verbose { println!("Installing boltffi_cli..."); }
        if !dry { command!("cargo", "install", "boltffi_cli", "--root", "target")?; }
        if verbose { println!("Finished installing boltffi_cli"); }
    }

    if !dry { platform.add_rust_targets(verbose)?; }

    // TODO: check if boltffi.toml exists
    // if !dry { command!(command_path, "init")?; }

    let env = platform.generate_env()?;
    let env = env.iter()
        .map(|(k, v)| (k.as_os_str(), v.as_os_str()))
        .collect::<Box<[_]>>();

    // TODO: skip if already packed (also check file last-modified).
    if !dry {
        if verbose {
            command!(ENV => &env, command_path, "pack", &platform.to_string(), "-v")?;
        } else {
            command!(ENV => &env, command_path, "pack", &platform.to_string())?;
        }
    } else {
        println!("DRY RUN: packed budgietlib for {platform}")
    }

    Ok(())
}
