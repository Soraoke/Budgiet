package com.example.budgiet.ui.utils

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.budgiet.Result
import com.example.budgiet.formatPrice
import com.example.budgiet.into
import com.example.budgiet.ui.FIELD_TIMEOUT
import com.example.budgiet.validateFieldInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale
import kotlin.time.Duration

/** Required specification for a **state object** of a [TextField][androidx.compose.material3.TextField]. */
interface FieldState<T> {
    var fieldText: String
    val parseResult: Result<T>

    val isError get() = this.parseResult is Result.Err

    /** Delays imposed upon the *validation/parsing/formatting* callbacks when *auto validating* is enabled (or `null` if it is not enabled).
     *
     * You can perform the *validation/parsing/formatting* manually at any time with [doValidate] (which does all 3). */
    var autoValidateTimings: AutoValidateTimings?

    /** If **`autoValidate`** is *disabled*, this performs all 3 of the *validation/parsing/formatting* manually, without an [delay]s. */
    fun doValidate()
}

/** See [FieldState.autoValidateTimings] for details.
 *
 * Call [rememberScope] to create a timings object in a [Composable]. */
class AutoValidateTimings(
    internal val coroutineScope: CoroutineScope,
    val parseDelay: Duration = Duration.ZERO,
    val formatDelay: Duration = FIELD_TIMEOUT,
) {
    companion object {
        @Composable
        fun rememberScope(
            parseDelay: Duration = Duration.ZERO,
            formatDelay: Duration = FIELD_TIMEOUT,
        ): AutoValidateTimings {
            val coroutineScope = rememberCoroutineScope()
            return remember { AutoValidateTimings(coroutineScope, parseDelay, formatDelay) }
        }
    }
}

/** Holds the [state][FieldState] for a [TextField][androidx.compose.material3.TextField]
 * whose value is meant to represent a simple [String].
 *
 * Note that, by default, the *validator* is called automatically.
 * See [autoValidateTimings] to change this behavior. */
class StringTextFieldState @RememberInComposition constructor(
    initialValue: String,
    /** Checks whether the value is valid. */
    private val validator: ((String) -> Result<Unit>)? = null,
    override var autoValidateTimings: AutoValidateTimings? = null,
): FieldState<String> {
    private var _fieldText by mutableStateOf(initialValue)
    // Only validate initialValue if autoValidation is enabled.
    private var _parseResult by mutableStateOf(if (autoValidateTimings == null) {
        Result.Ok(Unit)
    } else {
        this.validateValue(initialValue)
    })

    private var autoValidateJob: Job? = null

    override var fieldText: String
        get() = this._fieldText
        set(value) {
            this._fieldText = value

            // Cancel autoValidate Job when fieldText.set() is called.
            this.autoValidateJob?.let { job ->
                job.cancel()
                this.autoValidateJob = null
            }
            this.autoValidateTimings?.let { timings ->
                // Don't call the delay at all if there is no delay.
                if (timings.parseDelay == Duration.ZERO) {
                    doValidate()
                } else {
                    this.autoValidateJob = timings.coroutineScope.launch {
                        delay(timings.parseDelay)
                        doValidate()
                        autoValidateJob = null
                    }
                }
            } ?: run {
                // If autoValidate is disabled, still reset the parseResult so that the new value can be validated later.
                this._parseResult = Result.Ok(Unit)
            }
        }
    override val parseResult: Result<String> get() = this._parseResult.map { this.fieldText }

    override fun doValidate() { this._parseResult = this.validateValue(this.fieldText) }

    private fun validateValue(value: String): Result<Unit>
        = this.validator?.invoke(value) ?: Result.Ok(Unit)
}

/** A Generic implementation of [FieldState], which holds the state for a [TextField][androidx.compose.material3.TextField].
 *
 * The actual value (`T`) remains untouched until the [fieldText] can be successfully parsed.
 *
 * > if `T` is [String], use [StringTextFieldState] instead.
 * > All other classes that wish to implement [FieldState] should extend this class instead.
 *
 * Note that, by default, the *parser and formatter* are called automatically.
 * See [autoValidateTimings] to change this behavior. */
abstract class TextFieldState<T> @RememberInComposition protected constructor(
    initialFieldValue: String,
    initialResult: Result<T>,
    private val parser: (String) -> Result<T>,
    private val formatter: ((T) -> String)? = null,
    override var autoValidateTimings: AutoValidateTimings? = null,
): FieldState<T> {
    private var _fieldText by mutableStateOf(initialFieldValue)
    private var _parseResult by mutableStateOf(initialResult)

    @Suppress("unused")
    @RememberInComposition
    constructor(
        initialValue: T,
        parser: (String) -> Result<T>,
        // formatter must not be null if value is T so that the textField can be populated.
        formatter: (T) -> String,
        autoValidateTimings: AutoValidateTimings? = null,
    ): this(
        initialFieldValue = formatter(initialValue),
        initialResult = Result.Ok(initialValue),
        parser, formatter, autoValidateTimings
    )
    @Suppress("unused")
    @RememberInComposition
    constructor(
        initialValue: String,
        parser: (String) -> Result<T>,
        formatter: ((T) -> String)? = null,
        autoValidateTimings: AutoValidateTimings? = null,
    ): this(
        initialFieldValue = initialValue,
        initialResult = parser(initialValue),
        parser, formatter, autoValidateTimings
    )

    private var autoValidateJob: Job? = null

    override var fieldText: String
        get() = this._fieldText
        set(value) {
            this._fieldText = value

            // Cancel autoValidate Job when fieldText.set() is called.
            this.autoValidateJob?.let { job ->
                job.cancel()
                this.autoValidateJob = null
            }
            this.autoValidateTimings?.let { timings ->
                val parse = { this._parseResult = this.parser(this.fieldText) }
                val format = this.formatter?.let { formatter -> {
                    this._parseResult.let { parseResult ->
                        if (parseResult is Result.Ok) {
                            this._fieldText = formatter(parseResult.value)
                        }
                    }
                } }

                // Skip launching coroutine if there is no delay
                if (timings.parseDelay == Duration.ZERO) {
                    parse()
                    if (format != null) {
                        if (timings.formatDelay == Duration.ZERO) {
                            format()
                        } else {
                            this.autoValidateJob = timings.coroutineScope.launch {
                                delay(timings.formatDelay)
                                format()
                                autoValidateJob = null
                            }
                        }
                    }
                } else {
                    this.autoValidateJob = timings.coroutineScope.launch {
                        delay(timings.parseDelay)
                        parse()
                        if (format != null) {
                            if (timings.formatDelay != Duration.ZERO) {
                                delay(timings.formatDelay)
                            }
                            format()
                        }
                        autoValidateJob = null
                    }
                }
            }
        }
    override val parseResult: Result<T> get() = this._parseResult

    override fun doValidate() {
        val parseResult = this.parser(this.fieldText)
        this._parseResult = parseResult
        if (this.formatter != null && parseResult is Result.Ok) {
            this._fieldText = this.formatter(parseResult.value)
        }
    }
}

/** A specific implementation of [TextFieldState] for a *real number* (aka [Double]).
 *
 * Note: Pass **`keyboardOptions`** to the [TextField][androidx.compose.material3.TextField] to make the on-screen keyboard show only numbers (like a calculator). */
class RealNumberFieldState @RememberInComposition private constructor(
    initialFieldValue: String,
    initialResult: Result<Double>,
    parser: (String) -> Result<Double>,
    formatter: ((Double) -> String)?,
    autoValidateTimings: AutoValidateTimings? = null,
): TextFieldState<Double>(initialFieldValue, initialResult, parser, formatter, autoValidateTimings) {
    @RememberInComposition
    constructor(
        initialValue: Double,
        parser: (String) -> Result<Double> = defaultParser,
        formatter: ((Double) -> String)? = defaultFormatter,
        autoValidateTimings: AutoValidateTimings? = null,
    ): this(
        initialFieldValue = initialValue.toString(),
        initialResult = Result.Ok(initialValue),
        parser, formatter, autoValidateTimings
    )
    @RememberInComposition
    constructor(
        initialValue: String,
        parser: (String) -> Result<Double> = defaultParser,
        formatter: ((Double) -> String)? = defaultFormatter,
        autoValidateTimings: AutoValidateTimings? = null,
    ): this(
        initialFieldValue = initialValue,
        initialResult = parser(initialValue),
        parser, formatter, autoValidateTimings
    )

    companion object {
        private val defaultParser: (String) -> Result<Double> = { Unit.runCatching { it.toDouble() }.into() }
        private val defaultFormatter: ((Double) -> String)? = null
        val keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        )

        /** Constructs a [RealNumberFieldState] specifically to be used with *money* amounts,
         * which uses a specific custom *parser* and *formatter*.
         *
         * When **`initialAmount`** is `null`, the [fieldText] starts out empty.
         * Otherwise, if passed in a valid [Double], [fieldText] copies it verbatim, even if it is `0.0`. */
        @RememberInComposition
        fun moneyFieldState(
            initialAmount: Double? = null,
            currency: Currency,
            locale: Locale,
            autoValidateTimings: AutoValidateTimings
        ): RealNumberFieldState {
            val parser = { s: String -> currency.validateFieldInput(s, locale) }
            // TODO: skip formatting if fieldText is empty, but only for moneyFieldState.
            val formatter = { n: Double -> currency.formatPrice(n, locale) }
            return if (initialAmount == null) {
                RealNumberFieldState("", parser, formatter, autoValidateTimings)
            } else {
                RealNumberFieldState(initialAmount, parser, formatter, autoValidateTimings)
            }
        }

        /** Same as [moneyFieldState], but automatically sets the [autoValidateTimings]. */
        @Composable
        fun rememberMoneyFieldState(
            initialAmount: Double? = null,
            currency: Currency,
            locale: Locale,
        ): RealNumberFieldState {
            val autoValidateTimings = AutoValidateTimings.rememberScope()
            // Don't reset state when currency or locale change.
            return remember { this.moneyFieldState(initialAmount, currency, locale, autoValidateTimings) }
        }
    }
}
