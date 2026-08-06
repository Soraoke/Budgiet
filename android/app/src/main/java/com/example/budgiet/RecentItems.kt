package com.example.budgiet

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import java.io.File
import com.example.budgiet.Currency as CCurrency
import com.example.budgiet.Color as CColor

/** TODO: doc
 *
 * Instances: [Currency], [Color]. */
sealed class RecentItems<T> {
    /** A provider of [Currencies][com.example.budgiet.Currency] **recently used** by the user.
     *
     * See [RecentItems] for more details. */
    object Currency: RecentItems<CCurrency>() {
        override val instance = object : FfiRecentItems<CCurrency> {
            override suspend fun loadStorage(context: Context): Result<List<CCurrency>> = runCatching { FfiRecentCurrencies.loadStorage(context.filesDir.absolutePath) }.into()
            override fun clear() = FfiRecentCurrencies.clear()
            override suspend fun moveToFront(item: CCurrency): Result<List<CCurrency>> = runCatching { FfiRecentCurrencies.moveToFront(item) }.into()
        }
    }
    /** A provider of [Colors][com.example.budgiet.Color] **recently used** by the user.
     *
     * See [RecentItems] for more details. */
    object Color: RecentItems<CColor>() {
        override val instance = object : FfiRecentItems<CColor> {
            override suspend fun loadStorage(context: Context): Result<List<CColor>> = runCatching { FfiRecentColors.loadStorage(context.filesDir.absolutePath) }.into()
            override fun clear() = FfiRecentColors.clear()
            override suspend fun moveToFront(item: CColor): Result<List<CColor>> = runCatching { FfiRecentColors.moveToFront(item) }.into()
        }
    }

    private lateinit var state: MutableState<Result<List<T>>?>

    /** Returns an ordered [List] of **recent items** with type `T`, sorted by *most recent use*.
     *
     * The return value tells the state of the data:
     *  * **`null`**: The data is still being loaded.
     *  * **[Result.Err]**: There was an error loading the data.
     *  * **[Result.Ok]**: The data finished loading successfully.
     *
     *  Since this is a [Composable] with an internal [MutableState],
     *  changes in the state will propagate to the caller and it will be recomposed. */
    @Composable
    fun items(): State<Result<List<T>>?> {
        val context = LocalContext.current
        this.state = rememberWork { this.instance.loadStorage(context).unwrap() }

        return this.state
    }

    /** Removes (clears) all items from the *ordered list* in memory and from the file in storage. */
    fun clear() {
        if (this.state.value != null) {
            this.state.value = Result.Ok(listOf())
        }
        this.instance.clear()
    }

    /** Marks an **item** as recently used (i.e. it was just selected),
     * moving it to the front of the [List] of recent items,
     * which is **sorted** by latest use.
     *
     * This function will also write to the [File] in storage the same content as the [List] in memory.
     *
     * See [RecentItems.items] to read from this [List]. */
    fun moveToFront(item: T) {
        dispatchWork {
            this.state.value = this.instance.moveToFront(item)
        }
    }

    /** Instance of a *Rust* type that implements the `budgietlib::utils::recent_items::RecentItems` trait. */
    protected abstract val instance: FfiRecentItems<T>
}

interface FfiRecentItems<T> {
    suspend fun loadStorage(context: Context): Result<List<T>>
    fun clear()
    suspend fun moveToFront(item: T): Result<List<T>>
}