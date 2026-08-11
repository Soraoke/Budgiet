package com.example.budgiet

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.trySendBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@SuppressLint("ExperimentalAnnotationRetention")
@RequiresOptIn(message = "This part of the API is visible only for testing.")
internal annotation class UsableInTestsOnly

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

    /** Unwraps the Result to retrieve it's Ok value
     * If the Result is Err, it will return null
     *
     * @return Ok Result value or null */
    fun getOkOrNull(): T? {
        return when (this) {
            is Ok -> this.value
            is Err -> null
        }
    }

    /** Unwraps the Result to retrieve it's Ok value
     * If the Result is Err, the exception value will be *thrown*.
     *
     * @return Ok Result value */
    fun unwrap(): T {
        return when (this) {
            is Ok -> this.value
            is Err -> throw this.error
        }
    }

    /** Unwraps the Result to retrieve it's Err throwable
     * If the Result is Ok, it will return null
     *
     * @return Err Result throwable or null */
    fun getErrOrNull(): Throwable? {
        return when (this) {
            is Ok -> null
            is Err -> this.error
        }
    }

    /** Unwraps the Result to retrieve it's Err throwable
     * If the Result is Ok, an exception will be *thrown*.
     *
     * @return Ok Result value */
    fun unwrapErr(): Throwable {
        return when (this) {
            is Ok -> throw RuntimeException("Expected error value, found Ok(${this.value})")
            is Err -> this.error
        }
    }

    /** Transforms the value of `this` if it is [Ok].
     * Does nothing otherwise. */
    fun <U> map(transform: (T) -> U): Result<U> {
        return when(this) {
            is Ok -> Ok(transform(this.value))
            is Err -> this
        }
    }

    fun isOkAnd(predicate: (T) -> Boolean): Boolean = this is Ok && predicate(this.value)
    fun isErrAnd(predicate: (Throwable) -> Boolean): Boolean = this is Err && predicate(this.error)

    companion object {
        /** Converts a [kotlin.Result] to this app's custom [Result]. */
        fun <T> fromKt(result: kotlin.Result<T>): Result<T> {
            return if (result.isSuccess) {
                Ok(result.getOrNull()!!)
            } else {
                Err(result.exceptionOrNull()!!)
            }
        }
    }
}
/** Converts a [kotlin.Result] to this app's custom [Result]. */
fun <T> kotlin.Result<T>.into(): Result<T> = Result.fromKt(this)

/** Unwrap a **nullable** value, or *throw* and [Exception][Throwable] if it was `null`.
 *
 * Can pass a function that generates a custom [exception] to *throw* when the value is `null`.*/
fun <T> T?.unwrap(exception: (() -> Throwable)? = null): T
    = if (exception == null) this!! else this ?: throw exception()

fun dispatchWork(task: suspend () -> Unit) = dispatchWork(task as AyncCallbackInterface as AyncCallback)

/** Similar to [runWork], but can be called from a [Composable] context instead of a *suspend* context.
 *
 * Upon calling, the **`task`** is immediately spawned in the Runtime,
 * and this function returns a [State][androidx.compose.runtime.State] object containing `null`.
 * When the **`task`** is done, the [State] will be populated with the [Result] of running the **`task`**,
 * which will trigger *recomposition* and update the UI.
 *
 * @param key Value that triggers a *re-run* if changed. */
@Composable
fun <T> rememberWork(
    key: Any = Unit,
    task: suspend () -> T
): MutableState<Result<T>?> {
    val state = remember(key) { mutableStateOf<Result<T>?>(null) }
    LaunchedEffect(key) {
        state.value = runWork(task)
    }
    return state
}

/** Run a **task** in the *default* single worker thread, returning the value that the **`task`** produced.
 *
 * > Note: If this function is called from the same thread it's supposed to run on (i.e. *default* worker thread),
 * > it will push the **`task`** to the back of the queue instead of executing it immediately.
 *
 * Returns [Result.Err] if the **`task`** is *canceled* or throws an [Exception]. */
suspend fun <T> runWork(task: suspend () -> T): Result<T> {
    suspend fun runTask()
        // Don't allow an exception to terminate the worker thread; gotta catch em all.
        = try {
            Result.Ok(task())
        } catch (e: Throwable) {
            Result.Err(e)
        }

    val channel = Channel<Result<T>>()
    dispatchWork {
        channel.trySend(runTask())
    }
    return channel.receive()
}

fun localDateFromUtcMillis(utcMillis: Long): LocalDate {
    return Instant.ofEpochMilli(utcMillis)
        // NOTE: This does not set the timezone of the Date to UTC,
        // but instead interprets the millis as set in UTC, which is what the DatePicker provides.
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
}
fun LocalDate.formatRelativeToPresent(): String {
    val now = LocalDate.now()

    return when (this) {
        // I know this arm is unreachable, but wanted to include it for completeness.
        // Maybe we'll want to use it for something else later.
        now.plusDays(1) -> "Tomorrow"
        now -> "Today"
        now.minusDays(1) -> "Yesterday"
        // Formatting method taken from https://stackoverflow.com/a/56668796/32115191.
        else -> DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .let { formatter ->
                this.format(formatter)
            }
    }
}

/** Maps a **[Currency]** to one of our imported *drawable resources*.
 *
 * This will take the [Currency]'s [symbol][Currency.symbol] and try to match it with a resource.
 *
 * @returns `null` if we do not recognize the currency's *symbol*.
 * In this case, the caller should omit the currency icon,
 * or default to using the **[dollar][R.drawable.currency_dollar_24px]** icon if an icon is required. */
@Composable
fun getCurrencyIcon(currency: Currency): Painter? {
    val map = hashMapOf(
        '$' to R.drawable.currency_dollar_24px,
        '₱' to R.drawable.currency_peso_24px,
        '£' to R.drawable.currency_pound_24px,
        '¥' to R.drawable.currency_yen_24px,
        '₩' to R.drawable.currency_won_24px,
        '₪' to R.drawable.currency_shekel_24px,
        '₽' to R.drawable.currency_ruble_24px,
        '€' to R.drawable.currency_euro_24px,
        '₠' to R.drawable.currency_euro_24px,
        '₹' to R.drawable.currency_rupee_24px,
        'र' to R.drawable.currency_rupee_24px,
        '₨' to R.drawable.currency_rupee_24px,
        '₣' to R.drawable.currency_franc_24px,
        'Ꞙ' to R.drawable.currency_franc_24px,
        '₺' to R.drawable.currency_lira_24px,
        '₤' to R.drawable.currency_lira_24px,
        '₿' to R.drawable.currency_bitcoin_24px,
    )

    // Check if the symbol string contains an actual symbol at all
    for (c in currency.symbol()) {
        map[c]?.let { res ->
            return painterResource(res)
        }
    }

    return null
}
