use std::{ffi::OsStr, fs::{self, DirEntry}, io, path::Path, process::Command};
use sha2::digest::{Digest, Output};

use crate::{Error, IoError};

/// Run a system command.
#[macro_export]
macro_rules! command {
    ($command:literal, $($args:expr),*) => {
        crate::utils::_command($command, &[
            $(::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$args)),*
        ])
    };
}
#[doc(hidden)]
pub fn _command<'a>(command: &str, args: &[&'a OsStr]) -> Result<String, IoError> {
    let output = Command::new(command)
        .args(args)
        .output()
        .map_err(|err| IoError::from(err, format!("Error spawning {command:?} command")))?;
    if !output.status.success() {
        let err = String::from_utf8_lossy(&output.stderr);
        return Err(IoError::other(err.as_ref(), format!("Command {command:?} exited with error code '{:?}'", output.status.code())));
    }
    Ok(String::from_utf8_lossy(&output.stdout).to_string())
}

/// Checks that the resulting [`Output`] **hash** matches an **`expected`** *hexadecimal* string. 
pub fn checksum<D: Digest>(expected: impl AsRef<str>, actual: Output<D>) -> Result<(), io::Error> {
    let expected_str = expected.as_ref();
    let expected = hex::decode(expected_str)
        .map_err(|err| io::Error::other(err))?;

    if AsRef::<[u8]>::as_ref(&actual) != &expected {
        return Err(io::Error::other(format!("Expected hash \"{expected_str}\", but was \"{actual}\"",
            actual = hex::encode(actual),
        )))
    }
    Ok(())
}

/// Opens a directory and reads all entries, calling **`f`** for every entry.
/// 
/// Collects all **errors** produced by **`f`** and 
pub fn read_dir_with(path: impl AsRef<Path>, mut f: impl FnMut(DirEntry) -> Result<(), IoError>) -> Result<(), Error> {
    let path = path.as_ref();
    let results = fs::read_dir(path)
        .map_err(|err| IoError::from(err, format!("Error opening directory \"{}\"", path.display())))?
        .map(|entry| entry
            .map_err(|err| IoError::from(err, format!("Error reading entry of directory \"{}\"", path.display())))
            .and_then(|entry| f(entry))
        );

    let mut errors = Vec::new();
    results.for_each(|result| { result
        .map_err(|err| errors.push(err))
        .unwrap_or(())
    });
    if errors.is_empty() {
        Ok(())
    } else {
        Err(errors.into())
    }
}
