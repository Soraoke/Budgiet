use boltffi::export;
use unicode_segmentation::UnicodeSegmentation;

/// Counts the number of [`graphemes`][UnicodeSegmentation::graphemes] in the provided **`string`**.
#[export]
pub fn grapheme_string_length(s: &str) -> usize {
    s.grapheme_indices(true).count()
}

/// Divides the **`string`** into [`graphemes`][UnicodeSegmentation::graphemes],
/// **`counts`** *up to* the specified amount of graphemes,
/// and returns the *cut-off* string.
#[export]
pub fn grapheme_string_take(s: &str, count: usize) -> &str {
    let offset = s.grapheme_indices(true)
        .map(|(offset, _)| offset)
        .take(count)
        .last()
        .unwrap_or(s.len());

    &s[0..offset]
}
