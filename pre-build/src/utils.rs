use std::{ffi::OsStr, fs::{self, DirEntry}, io, mem::ManuallyDrop, path::Path, process::Command};
use sha2::digest::{Digest, Output};
use crate::{Error, Errors};

/// Run a system command.
#[macro_export]
macro_rules! command {
    (ENV => [ $($env_key:expr => $env_val:expr),* ], $command:expr, $($args:expr),*) => {
        crate::utils::_command(
            $command,
            &[ $( (::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$env_key), ::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$env_val))),* ],
            &[ $(::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$args)),* ],
        )
    };
    (ENV => $env:expr, $command:expr, $($args:expr),*) => {
        crate::utils::_command(
            $command, $env, &[ $(::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$args)),* ],
        )
    };
    ($command:expr, $($args:expr),*) => {
        crate::utils::_command($command, &[], &[
            $(::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$args)),*
        ])
    };
}
#[doc(hidden)]
pub fn _command<'a>(command: &str, env: &[(&'a OsStr, &'a OsStr)], args: &[&'a OsStr]) -> Result<String, Error> {
    let mut command = Command::new(command);
    let mut command = &mut command;
    for (key, val) in env {
        command = command.env(key, val);
    }

    let output = command
        .args(args)
        .output()
        .map_err(|err| Error::io(err, format!("Error spawning {command:?} command")))?;
    if !output.status.success() {
        let err = String::from_utf8_lossy(&output.stderr);
        return Err(Error::io_other(err.as_ref(), format!("Command {command:?} exited with error code '{:?}'", output.status.code())));
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

/// Same as [`std::fs::read_dir()`], but transforms the errors to this crate's [`Error`].
pub fn read_dir(path: &Path) -> impl Iterator<Item = Result<DirEntry, Error>> {
    enum ResultIterator<T, E, I: Iterator<Item = Result<T, E>>> {
        Ok(I),
        Err(ManuallyDrop<E>),
        None,
    }
    impl<T, E, I> Iterator for ResultIterator<T, E, I>
    where I: Iterator<Item = Result<T, E>> {
        type Item = I::Item;

        fn next(&mut self) -> Option<Self::Item> {
            match self {
                Self::Ok(iter) => iter.next(),
                Self::Err(err) => {
                    // SAFETY: will switch self to None right after.
                    let err = unsafe { ManuallyDrop::take(err) };
                    *self = Self::None;
                    Some(Err(err))
                },
                Self::None => None,
            }
        }
    }

    let result = fs::read_dir(&path)
        .map_err(|err| Error::io(err, format!("Error opening directory \"{}\"", path.display())))
        .map(|entries| entries.map(|entry| entry
            .map_err(|err| Error::io(err, format!("Error reading entry of directory \"{}\"", path.display())))
        ));
    match result {
        Ok(iter) => ResultIterator::Ok(iter),
        Err(err) => ResultIterator::Err(ManuallyDrop::new(err)),
    }
}

/// Same as [`read_dir()`], but *filters out* any filesystem node that is not a **file**.
pub fn read_dir_files(path: &Path) -> impl Iterator<Item = Result<DirEntry, Error>> {
    // Only take actual files.
    read_dir(path).filter_map(|result| match result {
        Ok(entry) => {
            let meta = entry.metadata()
                .map_err(|err| Error::io(err, format!("Error getting metadata for \"{}\"", entry.path().display())));
            match meta {
                Ok(meta) => meta.is_file().then_some(Ok(entry)),
                Err(err) => Some(Err(err)),
            }
        },
        result => Some(result),
    })
}

pub trait IterResultExt<T, E>
where Self: Iterator {
    /// Collects results of all the calls to [`next`] and returns it.
    ///
    /// If [`next`] only returned `T`s, then it returns a **Collection** containing those values.
    /// Buf if [`next`] returns *at least 1* **Error**,
    /// this functions continues calling [`next`] until the end to collect all the **Errors**.
    ///
    /// [`next`]: Iterator::next()
    fn collect_results<C>(self) -> Result<C, Errors<E>>
    where Self: Iterator<Item = Result<T, E>>,
          C: FromIterator<T>;
}
impl<I, T, E> IterResultExt<T, E> for I
where I: Iterator {
    fn collect_results<C>(self) -> Result<C, Errors<E>>
    where Self: Iterator<Item = Result<T, E>>,
          C: FromIterator<T>,
    {
        let mut errors = Vec::new();

        let values = self.filter_map(|result| result
            .map_err(|err| errors.push(err))
            .ok()
        )
        .collect::<C>();

        if errors.is_empty() {
            Ok(values)
        } else {
            Err(Errors::from(errors))
        }
    }
}
