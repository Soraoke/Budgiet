use boltffi::export;

#[export]
pub fn grapheme_string_length(s: &str) -> usize {
    todo!("Count the graphemes in {s:?}")
}
#[export]
pub fn grapheme_string_take(s: &str, count: usize) -> &str {
    todo!("Count the graphemes in {s:?}, return only {count:?} graphemes")
}


//class Grapheme(private val inner: CharSequence) {
//    override fun toString(): String = this.inner.toString()
//    override fun equals(other: Any?): Boolean = this.inner == other
//    override fun hashCode(): Int = this.inner.hashCode()
//}
//
// TODO: move this to Rust. The backend should be in charge of processing input. This is here for now to satisfy ui tests.
//** A wrapper around [String] that operates on [**graphemes**](https://en.wikipedia.org/wiki/Grapheme) instead of [characters][Char] or *code points*.
// *
// * This is useful to manage a String where you only care about *visual* character units. */
//class GraphemeString(private val inner: String): Comparable<String>, Iterable<Grapheme> {
//    constructor() : this(String())
//
//    val length: Int = run {
//        // Why forEach does not catch NoSuchElementException?? That's beyond me...
//        @Suppress("SpellCheckingInspection")
//        val iter = this@GraphemeString.charIdxIterator()
//        var count = 0
//        while (iter.next() != BreakIterator.DONE) {
//            count ++
//        }
//        count
//    }
//
//    /** Get the **grapheme** at the **index**.
//     *
//     * This will not work the same as [String]'s *get()*,
//     * as that indexes over [Char]s, but this indexes over **graphemes**.
//     *
//     * @throws IndexOutOfBoundsException */
//    operator fun get(index: Int): Grapheme {
//        // num of chars >= num of graphemes (always)
//        if (index > this.inner.length) {
//            throw IndexOutOfBoundsException("Provided index $index, but the String only has ${this.inner.length} characters.")
//        }
//
//        var count = 0
//        this.iterator().forEach { grapheme ->
//            if (count == index) {
//                return grapheme
//            }
//            count++
//        }
//
//        throw IndexOutOfBoundsException("Provided index $index, but the String only has $count graphemes.")
//    }
//
//    /** Returns a [GraphemeString] containing **graphemes** of the original string at the specified range of **indices**.
//     *
//     * @throws IndexOutOfBoundsException
//     * @throws IllegalArgumentException if [startIndex] > [endIndex] */
//    fun subSequence(startIndex: Int, endIndex: Int): GraphemeString {
//        if (startIndex > endIndex) {
//            throw IllegalArgumentException("startIndex ($startIndex) must not be greater than endIndex ($endIndex).")
//        }
//        // num of chars >= num of graphemes (always)
//        if (startIndex >= this.inner.length) {
//            throw IndexOutOfBoundsException("Provided startIndex $startIndex, but the String only has ${this.inner.length} characters.")
//        }
//        if (endIndex >= this.inner.length) {
//            throw IndexOutOfBoundsException("Provided endIndex $endIndex, but the String only has ${this.inner.length} characters.")
//        }
//
//        val it = this.charIdxIterator()
//        var startCharIndex: Int? = null
//        var endCharIndex: Int? = null
//        var count = 0
//        @Suppress("VariableInitializerIsRedundant")
//        var prev = 0
//        var current = 0
//
//        while (true) {
//            prev = current
//            current = it.next()
//            if (current == BreakIterator.DONE) {
//                break
//            }
//
//            if (count == startIndex) {
//                startCharIndex = prev
//            }
//            if (count == endIndex) {
//                endCharIndex = current
//            }
//
//            if (startCharIndex != null && endCharIndex != null) {
//                return GraphemeString(this.inner.slice(startCharIndex..endCharIndex))
//            }
//
//            count++
//        }
//
//        if (startCharIndex == null) {
//            throw IndexOutOfBoundsException("Provided startIndex $startIndex, but the String only has $count graphemes.")
//        }
//        @Suppress("KotlinConstantConditions")
//        if (endCharIndex == null) {
//            throw IndexOutOfBoundsException("Provided endIndex $endIndex, but the String only has $count graphemes.")
//        }
//
//        throw InternalError("Neither startCharIndex or endCharIndex were null, but the function did not return the string slice.")
//    }
//
//    fun take(count: Int): GraphemeString {
//        return if (this.length <= count) {
//            this
//        } else {
//            this.subSequence(0, count)
//        }
//    }
//
//    private fun charIdxIterator(): BreakIterator {
//        val it = BreakIterator.getCharacterInstance()
//        it.setText(this@GraphemeString.inner)
//        return it
//    }
//
//    override fun iterator(): Iterator<Grapheme> = object: Iterator<Grapheme> {
//        @Suppress("SpellCheckingInspection")
//        val iter = this@GraphemeString.charIdxIterator()
//        var prevBoundary = 0
//
//        override fun next(): Grapheme {
//            this.prevBoundary = this.iter.current()
//            val currentBoundary = this.iter.next()
//
//            if (currentBoundary == BreakIterator.DONE) {
//                throw NoSuchElementException()
//            }
//
//            return Grapheme(this@GraphemeString.inner.subSequence(this.prevBoundary, currentBoundary))
//        }
//
//        override fun hasNext(): Boolean = (this.iter.clone() as BreakIterator).next() == BreakIterator.DONE
//    }
//
//    override fun compareTo(other: String): Int = this.inner.compareTo(other)
//
//    override fun toString(): String = this.inner
//
//    override fun equals(other: Any?): Boolean = this.inner == other
//    override fun hashCode(): Int = this.inner.hashCode()
//}
