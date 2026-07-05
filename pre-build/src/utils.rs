use std::{ffi::OsStr, fs::{self, DirEntry}, io, mem::ManuallyDrop, path::Path, process::Command, str::FromStr as _};
use chrono::{DateTime, Utc};
use sha2::digest::{Digest, Output};
use crate::{Error, Errors};

/// Run a system command, streaming **stdout** and **stderr**.
#[macro_export]
macro_rules! command {
    (ENV => [ $($env_key:expr => $env_val:expr),* ], $command:expr, $($args:expr),*) => {
        crate::utils::_command(
            ::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$command),
            &[ $( (::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$env_key), ::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$env_val))),* ],
            &[ $(::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$args)),* ],
        )
    };
    (ENV => $env:expr, $command:expr, $($args:expr),*) => {
        crate::utils::_command(
            ::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$command),
            $env,
            &[ $(::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$args)),* ],
        )
    };
    ($command:expr, $($args:expr),*) => {
        crate::utils::_command(
            ::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$command),
            &[],
            &[ $(::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$args)),* ],
        )
    };
}
/// Run a system command, returning the output of **stdout** or **stderr** in the [`Result`].
#[macro_export]
macro_rules! command_output {
    ($command:expr, $($args:expr),*) => {
        crate::utils::_command_output(
            ::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$command),
            &[],
            &[ $(::std::convert::AsRef::<::std::ffi::OsStr>::as_ref(&$args)),* ],
        )
    };
}
#[doc(hidden)]
pub fn _command<'a>(command: &'a OsStr, env: &[(&'a OsStr, &'a OsStr)], args: &[&'a OsStr]) -> Result<(), Error> {
    let mut command = &mut Command::new(command);
    for (key, val) in env {
        command = command.env(key, val);
    }

    let status = command
        .args(args)
        .stderr(std::io::stderr())
        .stdout(std::io::stdout())
        .status()
        .map_err(|err| Error::with_prefix(err, format!("Error spawning {command:?} command")))?;

    if status.success() {
        Ok(())
    } else {
        Err(Error::new(io::Error::other(format!("Command {command:?} exited with error code '{:?}'", status.code()))))
    }
}
#[doc(hidden)]
pub fn _command_output<'a>(command: &'a OsStr, env: &[(&'a OsStr, &'a OsStr)], args: &[&'a OsStr]) -> Result<String, Error> {
    let mut command = &mut Command::new(command);
    for (key, val) in env {
        command = command.env(key, val);
    }

    let output = command
        .args(args)
        .output()
        .map_err(|err| Error::with_prefix(err, format!("Error spawning {command:?} command")))?;
    if !output.status.success() {
        let err = String::from_utf8_lossy(&output.stderr);
        return Err(Error::with_prefix(io::Error::other(err.as_ref()), format!("Command {command:?} exited with error code '{:?}'", output.status.code())));
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

/// Creates a *runtime-initialized* ([`LazyLock`][std::sync::lazy_lock::LazyLock]) [`PathBuf`].
///
/// The point of using this is writing less code :D
#[macro_export]
macro_rules! static_path {
    (pub $name:ident = $val:expr) => {
        pub static $name: ::std::sync::LazyLock<std::path::PathBuf> = ::std::sync::LazyLock::new(|| $val);
    };
    ($name:ident = $val:expr) => {
        static $name: ::std::sync::LazyLock<std::path::PathBuf> = ::std::sync::LazyLock::new(|| $val);
    };
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
        .map_err(|err| Error::with_prefix(err, format!("Error opening directory \"{}\"", path.display())))
        .map(|entries| entries.map(|entry| entry
            .map_err(|err| Error::with_prefix(err, format!("Error reading entry of directory \"{}\"", path.display())))
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
                .map_err(|err| Error::with_prefix(err, format!("Error getting metadata for \"{}\"", entry.path().display())));
            match meta {
                Ok(meta) => meta.is_file().then_some(Ok(entry)),
                Err(err) => Some(Err(err)),
            }
        },
        result => Some(result),
    })
}

/// Read the *Modified time (mtime)* of a directory tree.
///
/// Returns the **mtime** of the file within the *directory tree* that was last modified.
pub fn recursive_dir_mtime(path: &Path) -> Result<DateTime<Utc>, Error> {
    if !path.metadata()
        .map_err(|err| Error::with_prefix(err, format!("Error getting file metadata of \"{}\"", path.display())))?
        .is_dir()
    {
        return Err(Error::with_prefix(io::Error::from(io::ErrorKind::NotADirectory), format!("Error getting recursive modified times of \"{}\"", path.display())));
    }

    let mut mtimes = command_output!("find", path, "-type", "f", "-printf", "%T@\\n")?
        .split('\n')
        .filter(|line| !line.is_empty())
        .map(|mtime| {
            let unix_time = mtime.split_once('.')
                .ok_or_else(|| Error::new(format!("Error parsing UNIX Timestamp: expected format \"{{unix_time}}.{{nsecs}}\", but got \"{mtime}\"")))
                .and_then(|(unix_time, _)| Ok(
                    u64::from_str(unix_time)
                        .map_err(|err| Error::with_prefix(io::Error::other(err), "Error parsing u64 from <unix_time>"))?
                ))?;
            DateTime::from_timestamp(unix_time as i64, 0)
                .ok_or_else(|| Error::with_prefix(io::Error::new(io::ErrorKind::InvalidData, "seconds and/or nanoseconds are invalid/out-of-range"), "Error parsing DateTime from UNIX Timestamp \"{unix_time}.{nsecs}\""))
        })
        .collect_results::<Box<[_]>>()
        .map_err(|errors| Error::new(errors.to_string()))?;

    mtimes.sort();
    mtimes.last()
        .map(DateTime::clone)
        .ok_or(())
        .or_else(|()| Ok({
            // The directory tree contained no files, check the mtime of the directory file itself.
            path.metadata()
                .map_err(|err| Error::with_prefix(err, format!("Could not get metadata of directory \"{}\"", path.display())))?
                .modified()
                .map_err(|err| Error::with_prefix(err, format!("Could not get modified time of directory \"{}\"", path.display())))?
                .into()
        }))
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
