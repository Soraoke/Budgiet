package com.example.budgiet

import android.annotation.SuppressLint
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.chrono.ChronoLocalDate
import java.time.chrono.ChronoPeriod
import java.time.chrono.Chronology
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.Temporal
import java.time.temporal.TemporalField
import java.time.temporal.TemporalUnit
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@SuppressLint("ExperimentalAnnotationRetention")
@RequiresOptIn(message = "This part of the API is visible only for testing.")
internal annotation class UsableInTestsOnly


/** An [Executor][java.util.concurrent.Executor] containing the *single thread* that will run *blocking tasks*.
 *
 * A normal [Thread] could be used here, but it's better to use an [Executor][java.util.concurrent.Executor]
 * for ease of pushing tasks to the thread. */
private val WORKER_THREAD = Executors.newSingleThreadExecutor()
/** The **ID** of the [Thread] in the *single-threaded executor* [WORKER_THREAD].
 *
 * After it is first initialized, the **ID** will not change,
 * because the code it runs will never *throw* an [Exception],
 * so the thread will not terminate until the end of the program.
 *
 * The value does not need to be put in a [Mutex][kotlinx.coroutines.sync.Mutex],
 * as only the worker thread can modify this value. */
private var WORKER_THREAD_ID: Long? = null
private fun isWorkerThread(): Boolean = WORKER_THREAD_ID != null && Thread.currentThread().id == WORKER_THREAD_ID

/** A *sum type* that represents an operation's **result**.
 *
 * A [Result] can be [Ok] if the operation was *successful* and provides a good value,
 * or [Err] if the operation *failed* and threw an [Exception] ([Throwable]).
 *
 * ## Example
 *
 * ```kotlin
 * when (val result = ...) {
 *     is Result.Ok -> useValue(result.value)
 *     is Result.Err -> useError(result.error)
 * }
 * ``` */
sealed class Result<out T> {
    class Ok<out T>(val value: T) : Result<T>()
    class Err(val error: Throwable) : Result<Nothing>()

    /**
     * Unwraps the Result to retrieve it's Ok value
     * If the Result is Err, it will return null
     *
     * @return Ok Result value or null
     */
    fun getOkOrNull(): T? {
        return when (this) {
            is Ok -> this.value
            is Err -> null
        }
    }

    /**
     * Unwraps the Result to retrieve it's Err throwable
     * If the Result is Ok, it will return null
     *
     * @return Err Result throwable or null
     */
    fun getErrOrNull(): Throwable? {
        return when (this) {
            is Ok -> null
            is Err -> this.error
        }
    }
}

/** Run a **task** in a *single-threaded* work Executor,
 * and [remember] the value in a [Composable].
 *
 * This function adds the **task** to the executor and immediately returns a `mutableStateOf(null)`.
 * While the task waits to be executed (and while it is being executed),
 * the *UI* thread can continue the rendering process without having to wait for work to be done.
 *
 * Optionally, the caller can pass a custom [Executor] to run the work in instead of the default **worker thread**.
 *
 * After the **task** is finished, the returned [MutableState] is updated to contain a [Result]:
 * either the *success* value produced by the **task** Callback,
 * or an *error value* if the **task** threw an [Exception] ([Throwable]).
 * Throwing an [Exception] in a [Composable] is not ideal since it will crash the program if not caught,
 * so this function will automatically catch [Exception]s and put it in the [Result] instead.
 *
 * > Note: If this function detects that it is being called from the *default* **worker thread**,
 * > it will just run the *task* in the same thread without first pushing it to the Executor and waiting its turn.
 * > This optimizes the order of running *tasks* in case the caller calls [rememberWork] without knowing it is in the worker thread,
 * > Although this should be extremely rare. */
@Composable
fun <T> rememberWork(executor: Executor = WORKER_THREAD, task: suspend () -> T): MutableState<Result<T>?> = remember {
    // Run on the current thread if it is the worker thread
    if (isWorkerThread()) {
        runBlocking {
            // Don't allow an exception to terminate the worker thread; gotta catch em all.
            mutableStateOf(try {
                Result.Ok(task())
            } catch (e: Throwable) {
                Result.Err(e)
            })
        }
    } else {
        val state = mutableStateOf<Result<T>?>(null)

        executor.execute {
            if (executor == WORKER_THREAD && WORKER_THREAD_ID == null) {
                WORKER_THREAD_ID = Thread.currentThread().id
            }

            runBlocking {
                // Don't allow an exception to terminate the worker thread; gotta catch em all.
                state.value = try {
                    Result.Ok(task())
                } catch (e: Throwable) {
                    Result.Err(e)
                }
            }
        }

        state
    }
}

/** Run a **task** in a *single-threaded* work Executor,
 * returning the value that the **task** produced.
 *
 * This function adds the **task** to the Executor and *suspends* while waiting for the **task** to produce a result.
 * Unlike [rememberWork], this function will *rethrow* any [Exception]s thrown by the **task**.
 * It is up to the caller to *catch* those [Exception]s.
 *
 * Optionally, the caller can pass a custom [Executor] to run the work in instead of the default **worker thread**.
 *
 * > Note: If this function detects that it is being called from the *default* **worker thread**,
 * > it will just run the *task* in the same thread without first pushing it to the Executor and waiting its turn.
 * > This optimizes the order of running *tasks* in case the caller calls [runWork] without knowing it is in the worker thread,
 * > Although this should be extremely rare. */
suspend fun <T> runWork(executor: Executor = WORKER_THREAD, task: suspend () -> T): Result<T> {
    return if (isWorkerThread()) {
        // Don't allow an exception to terminate the worker thread; gotta catch em all.
        try {
            Result.Ok(task())
        } catch (e: Throwable) {
            Result.Err(e)
        }
    } else {
        val channel = Channel<Result<T>>(capacity = 1)

        executor.execute {
            if (executor == WORKER_THREAD && WORKER_THREAD_ID == null) {
                WORKER_THREAD_ID = Thread.currentThread().id
            }

            // Don't know why it's complaining about this if the Runnable is not suspend, so it wouldn't compile anyways.
            @Suppress("RunBlockingInSuspendFunction")
            runBlocking {
                // Don't allow an exception to terminate the worker thread; gotta catch em all.
                channel.send(try {
                    Result.Ok(task())
                } catch (e: Throwable) {
                    Result.Err(e)
                })
            }
        }

        channel.receive()
    }
}

class Date private constructor(private val localDate: LocalDate): ChronoLocalDate {
    constructor(utcMillis: Long) : this(
        Instant.ofEpochMilli(utcMillis)
            // NOTE: This does not set the timezone of the Date to UTC,
            // but instead interprets the millis as set in UTC, which is what the DatePicker provides.
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
    )

    val utcMillis: Long
        get() = this.localDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    override fun toString(): String {
        val now = LocalDate.now()

        return when (localDate) {
            // I know this arm is unreachable, but wanted to include it for completeness.
            // Maybe we'll want to use it for something else later.Expand commentComment on line R209
            now.plusDays(1) -> "Tomorrow"
            now -> "Today"
            now.minusDays(1) -> "Yesterday"
            else -> DateTimeFormatter
                .ofLocalizedDate(FormatStyle.LONG)
                .let { formatter ->
                    localDate.format(formatter)
                }
        }
    }

    override fun equals(other: Any?): Boolean = this.localDate == other
    override fun hashCode(): Int = this.localDate.hashCode()

    override fun getChronology(): Chronology? = this.localDate.chronology
    override fun lengthOfMonth(): Int = this.localDate.lengthOfMonth()
    override fun until(endDateExclusive: ChronoLocalDate?): ChronoPeriod? = this.localDate.until(endDateExclusive)
    override fun until(
        endExclusive: Temporal?,
        unit: TemporalUnit?
    ): Long = this.localDate.until(endExclusive, unit)
    override fun getLong(field: TemporalField?): Long = this.localDate.getLong(field)

    companion object {
        /** Get the **current** [Date].
         *
         * Calls [LocalDate.now] under the hood. */
        fun now(): Date {
            return Date(LocalDate.now())
        }

        /** Only allow [Date]s that occurred.
         * That is, dates that are in the *past or present*.
         *
         * @return a singleton that can be passed to [rememberDatePickerState][androidx.compose.material3.rememberDatePickerState],
         * which will disable any *future* dates in the [DatePicker][androidx.compose.material3.DatePicker],
         * only allowing *past or present* dates to be selected. */
        @OptIn(ExperimentalMaterial3Api::class)
        fun pastOrPresentDates(): SelectableDates {
            // Taken from https://stackoverflow.com/a/77678547/32115191
            @Suppress("RemoveRedundantQualifierName")
            return object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return Date(utcTimeMillis) <= Date.now()
                }
                override fun isSelectableYear(year: Int): Boolean {
                    return year <= Date.now().localDate.year
                }
            }
        }
    }
}

// TODO: Actual implementation in Rust
fun graphemeStringLength(string: String): Int = string.length
// TODO: Actual implementation in Rust
fun graphemeStringTake(string: String, count: Int): String = string.take(count)

//class Grapheme(private val inner: CharSequence) {
//    override fun toString(): String = this.inner.toString()
//    override fun equals(other: Any?): Boolean = this.inner == other
//    override fun hashCode(): Int = this.inner.hashCode()
//}
//
//// TODO: move this to Rust. The backend should be in charge of processing input. This is here for now to satisfy ui tests.
///** A wrapper around [String] that operates on [**graphemes**](https://en.wikipedia.org/wiki/Grapheme) instead of [characters][Char] or *code points*.
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
