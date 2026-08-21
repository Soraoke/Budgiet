use std::{error::Error as StdError, fmt::{Debug, Display, Write as _}, io};

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

/// An error packing one or multiple other [`IoError`]s.
pub struct Errors<E>(Box<[E]>);
impl<E> IntoIterator for Errors<E> {
    type Item = E;
    type IntoIter = <Box<[Self::Item]> as IntoIterator>::IntoIter;

    #[inline(always)]
    fn into_iter(self) -> Self::IntoIter {
        self.0.into_iter()
    }
}
impl<E> From<E> for Errors<E> {
    fn from(value: E) -> Self {
        Self(Vec::from([value]).into_boxed_slice())
    }
}
impl<E> From<Box<[E]>> for Errors<E> {
    #[inline(always)]
    fn from(value: Box<[E]>) -> Self {
        Self(value)
    }
}
impl<E> From<Vec<E>> for Errors<E> {
    #[inline(always)]
    fn from(value: Vec<E>) -> Self {
        Self(value.into_boxed_slice())
    }
}
impl<E> FromIterator<E> for Errors<E> {
    fn from_iter<T: IntoIterator<Item = E>>(iter: T) -> Self {
        Self(iter.into_iter().collect())
    }
}
impl<E> Debug for Errors<E>
where E: Debug {
    #[inline(always)]
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        <Box<[_]> as Debug>::fmt(&self.0, f)
    }
}
impl<E> Display for Errors<E>
where E: Display {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        for (i, error) in self.0.iter().enumerate() {
            <E as Display>::fmt(error, f)?;

            if i != self.0.len() - 1 {
                f.write_char('\n')?;
            }
        }
        Ok(())
    }
}
impl<E> StdError for Errors<E>
where E: StdError { }

#[derive(Debug)]
pub struct Error {
    pub prefix: String,
    pub error: Box<dyn StdError>,
}
impl Error {
    pub fn new(err: impl Into<Box<dyn StdError>>) -> Self {
        Self { prefix: "".into(), error: err.into() }
    }
    pub fn with_prefix(err: impl Into<Box<dyn StdError>>, msg: impl Display) -> Self {
        Self { prefix: msg.to_string(), error: err.into() }
    }

    pub fn io_error_kind(&self) -> Option<io::ErrorKind> {
        self.error.downcast_ref::<io::Error>()
            .map(|err| err.kind())
    }
}
impl<E: serde::ser::Error + 'static> From<E> for Error {
    fn from(value: E) -> Self {
        Self::new(Box::new(value))
    }
}
impl StdError for Error {
    #[inline(always)]
    fn source(&self) -> Option<&(dyn StdError + 'static)> {
        self.error.source()
    }
}
impl Display for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        if !self.prefix.is_empty() {
            f.write_str(&self.prefix)?;
            f.write_str(": ")?;
        }
        (&self.error as &dyn Display).fmt(f)
    }
}
