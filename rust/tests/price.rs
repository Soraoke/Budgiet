use num_format::Locale;
use rust_decimal::{Decimal, prelude::FromPrimitive as _};
use budgietlib::{Currency, Money, price::{ParseMoneyError, ParseMoneyErrorKind}, utils::{CurrencyExt as _, MoneyExt as _}};

/// Basic style.
static EN_US: Locale = Locale::en_001;
/// Uses 3 decimal places, and space-like group separator.
static FR_TN: Locale = Locale::fr_TN;
// val bhd: Locale = Locale.of("ar", "BH")
/// Uses period for digit separator, and comma for decimal separator (reversed).
static ITL: Locale = Locale::it;

/// Extension methods for convenience.
trait LocaleExt {
    fn parse_price(self, price: &str) -> Result<f64, ParseMoneyError>;
    fn format_price(self, price: f64) -> String;
}
impl LocaleExt for Locale {
    fn parse_price(self, price: &str) -> Result<f64, ParseMoneyError> {
        Money::parse_value(price, Currency::locale_default(self), self)
            .map(|money| money.amount().as_f64())
    }
    fn format_price(self, price: f64) -> String {
        Money::from_decimal(Decimal::from_f64(price).unwrap(), Currency::locale_default(self))
            .format(self, false)
    }
}

#[test]
fn parse_price() {
    // Parse Integer.
    assert_eq!(EN_US.parse_price("1").unwrap(), 1.0);
    assert_eq!(FR_TN.parse_price("1").unwrap(), 1.0);
    assert_eq!(ITL.parse_price("1").unwrap(), 1.0);

    // Parse decimal point.
    assert_eq!(EN_US.parse_price("1.1").unwrap(), 1.1);
    assert_eq!(ITL.parse_price("1,1").unwrap(), 1.1);

    // Parse empty decimal places.
    assert_eq!(EN_US.parse_price("1.").unwrap(), 1.0);
    assert_eq!(ITL.parse_price("1,").unwrap(), 1.0);

    // Parse empty unit places.
    assert_eq!(EN_US.parse_price(".1").unwrap(), 0.1);
    assert_eq!(ITL.parse_price(",1").unwrap(), 0.1);

    // Parse digit separator.
    assert_eq!(EN_US.parse_price("1,234,567.8").unwrap(), 1234567.8);
    assert_eq!(FR_TN.parse_price("1 234 567,8").unwrap(), 1234567.8);
    assert_eq!(ITL.parse_price("1.234.567,8").unwrap(), 1234567.8);
    // Parse WITHOUT digit separator.
    assert_eq!(EN_US.parse_price("1234567.8").unwrap(), 1234567.8);
    assert_eq!(FR_TN.parse_price("1234567,8").unwrap(), 1234567.8);
    assert_eq!(ITL.parse_price("1234567,8").unwrap(), 1234567.8);

    // Error on invalid Characters.
    assert_eq!(EN_US.parse_price("(100)").unwrap_err().kind,
        ParseMoneyErrorKind::InvalidChars { list: vec!['(', ')'] },
    );
    assert_eq!(FR_TN.parse_price("a100").unwrap_err().kind,
        ParseMoneyErrorKind::InvalidChars { list: vec!['a'] },
    );

    // Error on more than one decimal point.
    assert_eq!(EN_US.parse_price("1..0").unwrap_err().kind,
        ParseMoneyErrorKind::MultipleDecimalSeps,
    );
    assert_eq!(ITL.parse_price("1,,0").unwrap_err().kind,
        ParseMoneyErrorKind::MultipleDecimalSeps,
    );

    // Error on incorrect group separator placement
    assert_eq!(EN_US.parse_price("1,23,4").unwrap_err().kind,
        ParseMoneyErrorKind::InvalidGroupSize,
    );
    assert_eq!(ITL.parse_price("1.2345.6").unwrap_err().kind,
        ParseMoneyErrorKind::InvalidGroupSize,
    );

    // Error on too many decimal digits.
    assert_eq!(EN_US.parse_price("1.000").unwrap_err().kind,
        ParseMoneyErrorKind::TooManyFractionalDigits { found_digits: 3 },
    );
    assert_eq!(FR_TN.parse_price("1,0000").unwrap_err().kind,
        ParseMoneyErrorKind::TooManyFractionalDigits { found_digits: 4 },
    );
    assert_eq!(ITL.parse_price("1,000").unwrap_err().kind,
        ParseMoneyErrorKind::TooManyFractionalDigits { found_digits: 3 },
    );

    // Error on leading 0 digits.
    assert_eq!(EN_US.parse_price("0100").unwrap_err().kind,
        ParseMoneyErrorKind::LeadingZeroes,
    );
    assert_eq!(EN_US.parse_price("00100").unwrap_err().kind,
        ParseMoneyErrorKind::LeadingZeroes,
    );
    assert_eq!(FR_TN.parse_price("000100").unwrap_err().kind,
        ParseMoneyErrorKind::LeadingZeroes,
    );

    // Error on incorrect decimal separators.
    assert_eq!(EN_US.parse_price("1,00").unwrap_err().kind,
        ParseMoneyErrorKind::InvalidGroupSize,
    );
    assert_eq!(FR_TN.parse_price("1.000").unwrap_err().kind,
        ParseMoneyErrorKind::InvalidGroupSize,
    );
    assert_eq!(ITL.parse_price("1.00").unwrap_err().kind,
        ParseMoneyErrorKind::InvalidGroupSize,
    );

    // TODO: test ParseMoneyError::IncorrectGroupSep
    // TODO: test ParseMoneyError::IncorrectDecimalSep
    todo!()

    // No need to test this, will fail with too many decimal places error (its more efficient).
    // // Error group separator in decimal places
    // assert_eq!(EN_US.parse_price("1.00,000").unwrap_err(),
    //     ParseMoneyError::GroupSepInFractionalSection { group_sep: ",".to_string(), decimal_sep: ".".to_string() },
    // );
    // assert_eq!(ITL.parse_price("1,00.000").unwrap_err(),
    //     ParseMoneyError::GroupSepInFractionalSection { group_sep: ".".to_string(), decimal_sep: ",".to_string() },
    // );
}

#[test]
fn format_price() {
    // Integer value
    assert_eq!(EN_US.format_price(10.0), "10.00");

    // Decimal places
    assert_eq!(EN_US.format_price(10.11), "10.11");
    assert_eq!(FR_TN.format_price(10.111), "10,111");
    assert_eq!(ITL.format_price(10.11), "10,11");

    // Digit separator
    assert_eq!(EN_US.format_price(1234.0), "1,234.00");
    assert_eq!(FR_TN.format_price(1234.0), "1 234,000");
    assert_eq!(ITL.format_price(1234.0), "1.234,00");

    // Digit separator AND decimal places
    assert_eq!(EN_US.format_price(1234.11), "1,234.11");
    assert_eq!(FR_TN.format_price(1234.111), "1 234,111");
    assert_eq!(ITL.format_price(1234.11), "1.234,11");

    // Too many decimal digits (round up)
    assert_eq!(EN_US.format_price(10.116), "10.12");
    assert_eq!(FR_TN.format_price(10.1116), "10,112");
    assert_eq!(ITL.format_price(10.116), "10,12");
    // (round down)
    assert_eq!(EN_US.format_price(10.113), "10.11");
    assert_eq!(FR_TN.format_price(10.1113), "10,111");
    assert_eq!(ITL.format_price(10.113), "10,11");
}
