/** General utilities for both Unit tests and Integration tests. */
@file:Suppress("unused")

package com.example.budgiet

/** See [org.junit.Assert.assertEquals].
 *
 * This does not return the *receiver `T`* because the assertion is meant to be the last method of the chain. */
fun <T> T.assertEquals(expected: T) = org.junit.Assert.assertEquals(expected, this)

/** See [kotlin.assert].
 *
 * This does not return the *receiver `T`* because the assertion is meant to be the last method of the chain.
 *
 * @param lazyMessage The error message that will be displayed. */
fun <T> T.assert(condition: (T) -> Boolean, lazyMessage: (() -> String)? = null) {
    if (lazyMessage == null) {
        assert(condition(this))
    } else {
        assert(condition(this), lazyMessage)
    }
}
