use std::{cmp::Ordering, collections::HashMap, io::{self, Write as _}, str::FromStr as _};
use chrono::NaiveDate;
use common::{Error, Errors, IterResultExt as _};
use itertools::Itertools;
use serde_json::{Map, Value};
use crate::{PROJECT_ROOT, static_path};

/// Release version of the **Unicode CLDR** respository to use.
///
/// This should not change often, only when the *Locale* or *Currency* data in the repo also changes.
/// How do we check that? I don't know.
static UNICODE_CLDR_VERSION: &str = "48.2.0";
static_path! { MAP_SRC_PATH = PROJECT_ROOT.join("rust/src/utils/locale_currency_map.rs") }

/// Builds a **Locale -> Currency** map from the *Unicode CLDR*, essentially assigning a *"default"* Currency to a Locale.
/// Then generates a *Rust source* file that is written to the project's main library ([`MAP_SRC_PATH`])
/// containing a [*phf map*](https://docs.rs/phf/0.14.0/phf/) of Locale to Currency.
///
/// > Note: The library's `Cargo.toml` should already have [`phf`](https://crates.io/crates/phf) with the *`macros` feature* as a dependency,
/// > and the [`MAP_SRC_PATH`] should be *ignored* in *git*.
///
/// If **`verbose`** is `true`, prints information about an opperation to *stderr*.
/// If **`dry`** is `true`, doesn't write to any files.
pub fn gen_locale_currency_map_src(verbose: bool, dry: bool) -> Result<(), Errors<Error>> {
    // Open Rust src file before trying to generate map.
    let open = std::fs::OpenOptions::new()
        .write(true)
        .create_new(true)
        .truncate(true)
        .open(&*MAP_SRC_PATH);
    let mut file = match open {
        Ok(file) => file,
        Err(err) if err.kind() == io::ErrorKind::AlreadyExists => {
            if verbose { eprintln!("Locale -> Currency src file already exists; Skipping.") };
            return Ok(());
        },
        Err(err) => return Err(Error::with_prefix(err, "Error opening Locale -> Currency map src file").into())
    };

    let map = build_locale_currency_map()?;
    let map = map.iter()
        .sorted_by_key(|(key, _)| *key)
        .collect::<Vec<_>>();

    if verbose {
        eprintln!("LOCALE -> CURRENCY MAP: {{");
        for (key, val) in &map {
            eprintln!("    {key:?}: {val:?},")
        }
        eprintln!("}}")
    }
    if !dry {
        let mut write_fn = || {
            writeln!(file, "pub(super) static LOCALE_CURRENCY_MAP: ::phf::Map<&'static str, &'static str> = ::phf::phf_map! {{")?;
            for (locale, currency) in &map {
                writeln!(file, "    {locale:?} => {currency:?},")?;
            }
            writeln!(file, "}};")
        };
        write_fn().map_err(|err| Error::with_prefix(err, "Error writing to Locale -> Currency src file"))?;
    }
    Ok(())
}

/// Fetches the `"currencyData.json"` file from the **Unicode CLDR** repo, which contains the Currencies used per regional Locale.
/// The data is then transformed into a ***one-to-one map*** of *Locale code to Currency code*.
///
/// Note that some Locales are skipped if the Currency is not *legal tender*.
fn build_locale_currency_map() -> Result<HashMap<String, String>, Errors<Error>> {
    let cldr_src_url = format!("https://raw.githubusercontent.com/unicode-org/cldr-json/refs/tags/{UNICODE_CLDR_VERSION}/cldr-json/cldr-core/supplemental/currencyData.json");
    static FILTER_NOT_TENDER: bool = true;

    let cldr_src = reqwest::blocking::get(cldr_src_url)
        .map_err(|err| Error::with_prefix(err, "Error requesting CLDR data for Locale-Currency map"))?
        .text()
        .map_err(|err| Error::with_prefix(err, "Error decoding CLDR response text"))?;

    // Find the data object that contains the Currency codes that are used for each Locale code.
    let json = serde_json::from_str::<Value>(&cldr_src)
        .map_err(|err| Error::with_prefix(err, "Error deserializing CLDR JSON"))?;

    let data = json.as_object()
        .and_then(|map| map.get("supplemental"))
        .and_then(|map| map.get("currencyData"))
        .and_then(|map| map.get("region"))
        .and_then(|map| map.as_object())
        .ok_or_else(|| Error::new("CLDR data did not contain Map object 'currencyData.region'"))?;

    data.iter()
        .map(|(locale, val)| {
            val.as_array()
                .ok_or_else(|| Error::new(format!("Expected Locale code \"{locale}\" to map to an array")))
                .map(|array| (locale.as_str(), array))
        })
        // Map each Locale to a list of Currencies that have been used for it.
        .map(|result| result.and_then(|(locale, array)| {
            array.iter()
                .map(|val| val.as_object()
                    .ok_or_else(|| Error::new(format!("Expected array for \"{locale}\" contain objects with a Currency code and use dates")))
                    // Value object should contain a single key: the Currency code.
                    .and_then(|map| {
                        let mut pairs = map.iter();
                        pairs.next()
                            // Check that no more than 1 Currency code exists per object.
                            .and_then(|currency_data| match pairs.next() {
                                Some(_) => None,
                                None => Some(currency_data),
                            })
                            .ok_or_else(|| Error::new(format!("object for \"{locale}\" contained multiple Currency codes")))
                            .and_then(|(currency, data)|
                                data.as_object()
                                    .map(|data| (currency, data))
                                    .ok_or_else(|| Error::new(format!("Expected value of currency code \"{currency}\" in \"{locale}\" to be an object")))
                            )
                    })
                    // Try to parse CurrencyData
                    .and_then(|currency_data| CurrencyData::try_from(currency_data))
                )
                .collect_results::<Box<[_]>>()
                .map(|currency| (locale, currency))
                .map_err(|errors| Error::new(errors))
        }))
        // Reduce the list of Currencies into a single one (most recent) for the Locale
        .filter_map(|result| result.map(|(locale, currencies)| {
            let currencies = currencies.into_iter()
                // The most recent currency only has a _from field and no _to or _tender fields.
                .filter(|currency_data| match currency_data {
                    // Don't consider Currencies that are not legal tender (if filter flag enabled).
                    CurrencyData::NotTender { .. } => !FILTER_NOT_TENDER,
                    // Don't consider Currencies that are no longer in use (i.e. have an end date) in the Locale.
                    CurrencyData::Tender { to_date, .. } => to_date.is_none(),
                })
                // Sort Currencies by '_from' date in descending order.
                .sorted_by(|a, b| match (a, b) {
                    (CurrencyData::Tender { from_date: a_date, .. },
                        CurrencyData::Tender { from_date: b_date, .. },
                    ) => a_date.cmp(b_date),
                    _ => Ordering::Equal,
                })
                .map(|currency_data| currency_data.code().to_string())
                .collect::<Box<[String]>>();

            match currencies.len() {
                // No usable Currency found, filter out this Locale.
                0 => None,
                // One or more Currencies remain for the Locale, use the most recent one.
                _ => Some((locale, currencies.into_iter().next().unwrap())),
            }
        }).transpose())
        .map(|result| result.map(|(locale, currency)| (locale.to_string(), currency)))
        .collect_results::<HashMap<String, String>>()
}

enum CurrencyData<'a> {
    NotTender {
        code: &'a str,
    },
    Tender {
        code: &'a str,
        from_date: NaiveDate,
        to_date: Option<NaiveDate>,
    },
}
impl CurrencyData<'_> {
    pub fn code(&self) -> &str {
        match self {
            Self::NotTender { code } => code,
            Self::Tender { code, .. } => code,
        }
    }
}
impl<'a> TryFrom<(&'a String, &'a Map<String, Value>)> for CurrencyData<'a> {
    type Error = Error;

    fn try_from(value: (&'a String, &'a Map<String, Value>)) -> Result<Self, Self::Error> {
        let code = value.0.as_str();
        let data = value.1;

        /// Parse the value of a **date** with format `"YYYY-MM-DD"`.
        fn parse_date(date: &str) -> Result<NaiveDate, Error> {
            date.split_once('-')
                .and_then(|(year, rest)|
                    rest.split_once('-')
                        .map(|(month, day)| (year, month, day))
                )
                // TODO: check that month and day is 2 digits
                .ok_or_else(|| Error::new(format!("Invalid date format: {date:?}")))
                .and_then(|(year, month, day)| {
                    NaiveDate::from_ymd_opt(
                        i32::from_str(year).map_err(|err| Error::with_prefix(err, "Error parsing year"))?,
                        u32::from_str(month).map_err(|err| Error::with_prefix(err, "Error parsing month"))?,
                        u32::from_str(day).map_err(|err| Error::with_prefix(err, "Error parsing day"))?,
                    ).ok_or_else(|| Error::new("Invalid date value: {date:?}"))
                })
        }

        match data.get("_tender") {
            Some(tender) => tender.as_bool()
                .and_then(|tender| (!tender).then_some(Self::NotTender { code }))
                .or_else(|| tender.as_str().and_then(|tender| {
                    (tender == "false").then_some(Self::NotTender { code })
                }))
                .ok_or_else(|| Error::new(format!("'_tender' field must be a 'false' boolean or string, but was {tender:?}"))),
            None => {
                let from_date = data.get("_from")
                    .and_then(|val| val.as_str())
                    .ok_or_else(|| Error::new("'_from' field must be present and be a string"))?;
                let from_date = parse_date(from_date)?;

                let to_date = data.get("_to")
                    .map(|val| {
                        val.as_str()
                            .ok_or_else(|| Error::new(format!("'_to' field must be a string, but was {val:?}")))
                            .and_then(|date| parse_date(date))
                    })
                    .transpose()?;

                Ok(Self::Tender { code, from_date, to_date })
            },
        }
    }
}
