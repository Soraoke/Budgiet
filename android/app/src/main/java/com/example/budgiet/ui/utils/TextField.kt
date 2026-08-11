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
import com.example.budgiet.Currency
import com.example.budgiet.Decimal
import com.example.budgiet.Locale
import com.example.budgiet.Money
import com.example.budgiet.Result
import com.example.budgiet.into
import com.example.budgiet.ui.FIELD_TIMEOUT
import com.example.budgiet.validateMoneyFieldInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

/** Required specification for a **state object** of a [TextField][androidx.compose.material3.TextField]. */
interface FieldState<T> {
    var fieldText: String
    val parseResult: Result<T>

    val isError get() = this.parseResult is Result.Err

    /** Enable *auto-validation* (or `null` if it is not enabled).
     *
     * See [AutoValidateTimings] for details. */
    var autoValidateTimings: AutoValidateTimings?

    /** Performs all 3 of the *validation/parsing/formatting* manually, without any [delay]s.
     *
     * Useful when **[auto-validation][autoValidateTimings]** is *disabled*. */
    fun doValidate()

    /** Runs the provided **callback** only if the **[parseResult]** is [Ok][Result.Ok].
     *
     * In case that *[auto-validation][AutoValidateTimings]* is *enabled*,
     * The validator will wait until the **[parseDelay][AutoValidateTimings.parseDelay]**
     * (if any) is over and the **parsing** is completed.
     * Otherwise, the **callback** is run immediately*/
    fun ifParseOk(block: (T) -> Unit)

    @Composable
    fun textFieldSupportingText(prefix: String? = null): (@Composable () -> Unit)? {
        return this.parseResult.let { if (it is Result.Err) {{
            ErrorText(prefix, it.error)
        }} else null }
    }
}

/** A set of *delays* imposed upon the *validation/parsing/formatting* callbacks when *auto validating* is enabled (or `null` if it is not enabled).
 *
 * You can perform the *validation/parsing/formatting* manually at any time with [FieldState.doValidate] (which does all 3).
 *
 * Note that the **[formatDelay]** "timer" (so to speak) starts directly after the **`parseDelay`** timer ends.
 * This means the **`parser`** is called after **[parseDelay]** time has passed,
 * but the **`formatter`** is called after **[parseDelay] + [formatDelay]** time has passed.
 *
 * Call [rememberScope] to create a timings object in a [Composable].
 *
 * @param parseDelay A delay that is applied before calling the **`parser`** callback.
 *   Also acts as the delay for **[StringTextFieldState.validator]**.
 * @param formatDelay A delay that is applied before calling the **`formatter`** callback. */
class AutoValidateTimings @RememberInComposition constructor(
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
                if (this._parseResult is Result.Err) {
                    this._parseResult = Result.Ok(Unit)
                }
            }
        }
    override val parseResult: Result<String> get() = this._parseResult.map { this.fieldText }

    override fun doValidate() { this._parseResult = this.validateValue(this.fieldText) }

    override fun ifParseOk(block: (String) -> Unit) {
        if (this.validator != null) {
            ifParseOkImpl(block, this.autoValidateJob)
        } else {
            block(this.fieldText)
        }
    }

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
open class TextFieldState<T> @RememberInComposition protected constructor(
    initialFieldValue: String,
    private val initialResult: Result<T>,
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
        initialTextValue: String,
        parser: (String) -> Result<T>,
        formatter: ((T) -> String)? = null,
        autoValidateTimings: AutoValidateTimings? = null,
    ): this(
        initialFieldValue = initialTextValue,
        initialResult = parser(initialTextValue),
        parser, formatter, autoValidateTimings
    )

    /** Prevents calling the **[formatter]** callback when the [fieldText] value is an *empty* string. */
    var skipEmptyTextFormat: Boolean = false

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
                        if (parseResult is Result.Ok
                        && (!this.skipEmptyTextFormat || this._fieldText.isNotEmpty())) {
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
                    this.autoValidateJob = timings.coroutineScope.launch { with(this@TextFieldState) {
                        delay(timings.parseDelay)
                        parse()
                        if (format != null) {
                            delay(timings.formatDelay)
                            format()
                        }
                        autoValidateJob = null
                    } }
                }
            } ?: run {
                // If autoValidate is disabled, still reset the parseResult so that the new value can be validated later.
                if (this._parseResult is Result.Err) {
                    this._parseResult = this.initialResult
                }
            }
        }
    override val parseResult: Result<T> get() = this._parseResult

    override fun doValidate() {
        val parseResult = this.parser(this.fieldText)
        this._parseResult = parseResult

        if (this.formatter != null
        && parseResult is Result.Ok
        && (!this.skipEmptyTextFormat || this._fieldText.isNotEmpty())) {
            this._fieldText = this.formatter(parseResult.value)
        }
    }

    override fun ifParseOk(block: (T) -> Unit) = ifParseOkImpl(block, this.autoValidateJob)
}

/** A specific implementation of [TextFieldState] for a *real number* (aka [Decimal]).
 *
 * Note: Pass **`keyboardOptions`** to the [TextField][androidx.compose.material3.TextField] to make the on-screen keyboard show only numbers (like a calculator). */
class RealNumberFieldState @RememberInComposition private constructor(
    initialFieldValue: String,
    initialResult: Result<Decimal>,
    parser: (String) -> Result<Decimal>,
    formatter: ((Decimal) -> String)?,
    autoValidateTimings: AutoValidateTimings? = null,
): TextFieldState<Decimal>(initialFieldValue, initialResult, parser, formatter, autoValidateTimings) {
    @Suppress("unused")
    @RememberInComposition
    constructor(
        initialValue: Decimal,
        /** Instantiates the state object with an *empty* [fieldText] value if the **`initialValue`** is `0.0`. */
        emptyInitialTextIfZero: Boolean = false,
        parser: (String) -> Result<Decimal> = defaultParser,
        formatter: ((Decimal) -> String)? = null,
        autoValidateTimings: AutoValidateTimings? = null,
    ): this(
        initialFieldValue = if (emptyInitialTextIfZero) { "" } else {
            formatter?.invoke(initialValue) ?: initialValue.toString()
        },
        initialResult = Result.Ok(initialValue),
        parser, formatter, autoValidateTimings
    )
    @Suppress("unused")
    @RememberInComposition
    constructor(
        initialTextValue: String,
        parser: (String) -> Result<Decimal> = defaultParser,
        formatter: ((Decimal) -> String)? = null,
        autoValidateTimings: AutoValidateTimings? = null,
    ): this(
        initialFieldValue = initialTextValue,
        initialResult = if (initialTextValue.isEmpty()) { Result.Ok(Decimal.ZERO) } else { parser(initialTextValue) },
        parser, formatter, autoValidateTimings
    )

    companion object {
        val defaultParser = { s: String -> runCatching { Decimal.fromStr(s) }.into() }
        val keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        )

        /** Constructs a [RealNumberFieldState] specifically to be used with *money* amounts,
         * which uses a specific custom *parser* and *formatter*.
         *
         * @param emptyInitialTextIfZero Instantiates the state object with an *empty* [fieldText] value if the **`initialValue`** is `0.0`. */
        @RememberInComposition
        fun moneyFieldState(
            initialAmount: Decimal = Decimal.ZERO,
            emptyInitialTextIfZero: Boolean = false,
            currency: Currency,
            locale: Locale,
            autoValidateTimings: AutoValidateTimings,
        ): RealNumberFieldState {
            val parser = { s: String -> runCatching { validateMoneyFieldInput(s, currency, locale) }.into().map { it.amount } }
            val formatter = { n: Decimal -> Money(n, currency).format(locale, false) }
            return RealNumberFieldState(
                initialValue = initialAmount,
                emptyInitialTextIfZero,
                parser,
                formatter,
                autoValidateTimings,
            ).apply {
                // Skip formatting if fieldText is empty, but only for moneyFieldState.
                skipEmptyTextFormat = true
            }
        }

        /** Same as [moneyFieldState], but automatically sets the [autoValidateTimings]. */
        @Composable
        fun rememberMoneyFieldState(
            initialAmount: Decimal = Decimal.ZERO,
            emptyInitialTextIfZero: Boolean = false,
            currency: Currency,
            locale: Locale,
        ): RealNumberFieldState {
            val autoValidateTimings = AutoValidateTimings.rememberScope()
            // Don't reset state when currency or locale change.
            return remember { this.moneyFieldState(initialAmount, emptyInitialTextIfZero, currency, locale, autoValidateTimings) }
        }
    }
}

private fun <T> FieldState<T>.ifParseOkImpl(block: (T) -> Unit, autoValidateJob: Job?) {
    val timings = this.autoValidateTimings
    val block = { this.parseResult.let { parseResult ->
        if (parseResult is Result.Ok) { block(parseResult.value) }
    } }

    if (timings != null) {
        timings.coroutineScope.launch {
            if (autoValidateJob != null) {
                autoValidateJob.join()
            } else {
                // Auto-validation Job did not exist, try the delays again and check parseResult.
                if (timings.parseDelay != Duration.ZERO) { delay(timings.parseDelay) }
                if (timings.formatDelay != Duration.ZERO) { delay(timings.formatDelay) }
            }
            block()
        }
    } else {
        block()
    }
}
