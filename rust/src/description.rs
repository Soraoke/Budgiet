use uniffi::export;
use unicode_segmentation::UnicodeSegmentation;

/// Counts the number of [`graphemes`][UnicodeSegmentation::graphemes] in the provided **`string`**.
pub fn grapheme_string_length(s: &str) -> usize {
    s.grapheme_indices(true).count()
}
#[export]
#[doc(hidden)]
fn __ffi_grapheme_string_length(s: &str) -> u64 {
    grapheme_string_length(s) as u64
}

/// Divides the **`string`** into [`graphemes`][UnicodeSegmentation::graphemes],
/// **`counts`** *up to* the specified amount of graphemes,
/// and returns the *cut-off* string.
pub fn grapheme_string_take(s: &str, count: usize) -> &str {
    let offset = s.grapheme_indices(true)
        .map(|(offset, _)| offset)
        .take(count)
        .last()
        .unwrap_or(s.len());

    &s[0..offset]
}
#[export]
#[doc(hidden)]
fn __ffi_grapheme_string_take(s: &str, count: u64) -> String {
    grapheme_string_take(s, count as usize).to_string()
}
