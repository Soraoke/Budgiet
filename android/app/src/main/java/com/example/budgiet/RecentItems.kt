package com.example.budgiet

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import com.example.budgiet.ui.utils.MAX_USER_COLOR_ITEMS
import java.io.File
import java.util.Currency as JCurrency
import androidx.compose.ui.graphics.Color as CColor

sealed class RecentItems<T> {
    object Currency: RecentItems<JCurrency>() {
        override val maxItems = null
        override fun fromString(s: String): JCurrency
                = JCurrency.getInstance(s)

        override fun toString(item: JCurrency): String
                = item.currencyCode
    }
    object Color: RecentItems<CColor>() {
        override val maxItems = MAX_USER_COLOR_ITEMS
        override fun fromString(s: String): CColor
                = CColor(s.toULong())

        override fun toString(item: CColor): String
                = item.value.toString()
    }

    private lateinit var state: MutableState<Result<SnapshotStateList<T>>?>

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
        this.state = rememberWork {
            this.getFile(context)
                // Read the entirety of the file to move around the elements.
                .readText()
                .split('\n')
                // Last element will always be empty because the file always ends with newLine (unless it is empty).
                .dropLast(1)
                .map { s -> this.fromString(s) }
                .toMutableStateList()
                .also { Log.i(instanceName, "Loaded ${this.instanceName} from storage.") }
        }

        if (this.state.value is Result.Err) {
            Log.e(instanceName, "Error reading ${this.instanceName} from storage: ${(this.state.value as Result.Err).error}")
        }

        return this.state
    }

    /** Removes (clears) all items from the *ordered list* in memory and from the file in storage. */
    fun clear(context: Context) {
        if (!this::state.isInitialized) {
            Log.w(instanceName, this.uninitStateMsg)
            return
        }

        // Clear in memory
        this.state.value = Result.Ok(mutableStateListOf())
        // Clear in storage
        dispatchWork {
            this.getFile(context)
                .writeText("")
            Log.i(instanceName, "Cleared ${this.instanceName} in storage.")
        }
    }

    /** Marks an **item** as recently used (i.e. it was just selected),
     * moving it to the front of the [List] of recent items,
     * which is **sorted** by latest use.
     *
     * This function will also write to the [File] in storage the same content as the [List] in memory.
     *
     * See [RecentItems.items] to read from this [List]. */
    fun moveToFront(item: T, context: Context) {
        if (!this::state.isInitialized) {
            Log.w(this.instanceName, this.uninitStateMsg)
            return
        }

        when (this.state.value) {
            null, is Result.Err -> this.state.value = Result.Ok(mutableStateListOf())
            is Result.Ok -> { }
        }
        val orderedItems = this.state.value!!.unwrap() as MutableList<T>

        val itemStr = this.toString(item)
        Log.i(this.instanceName, "Moving item \"$itemStr\" to the front of MutableStateList in memory.")

        // Apply to mutable list in memory
        // Find currency in the argument
        when (val idx = orderedItems.indexOf(item)) {
            // The currency was already first in the list; do nothing.
            0 -> { }
            // Currency was not found in the List, so it must be prepended.
            -1 -> {
                orderedItems.add(0, item)
                if (this.maxItems != null && orderedItems.size > this.maxItems!!) {
                    orderedItems.dropLast(1)
                }
            }
            // Remove target currency (arg) from the List, and put it in the front.
            else -> {
                orderedItems.add(0, orderedItems.removeAt(idx))
            }
        }

        // Apply to storage
        dispatchWork {
            Log.i(this.instanceName, "Moving item \"$itemStr\" to the front of File in storage.")
            this.getFile(context)
                // Write the modified list
                .writeText(orderedItems.joinToString(
                    separator = "",
                    truncated = "",
                    transform = { item -> "${this.toString(item)}\n" },
                ))
        }
    }

    private val instanceName = "RecentItems.${this::class.simpleName!!}"
    private val uninitStateMsg = "${this.instanceName}.state must be initialized before calling clear() or moveToFront()."

    /** Whether this [getFile] has yet to be called. */
    private var fileFirstAccess = true
    /** Returns the file path that contains this instance's data in storage.
     *
     * Creates the file if necessary. */
    private fun getFile(context: Context): File
        = File(context.filesDir, "RecentItems")
            .also { if (this.fileFirstAccess) it.mkdirs() }
            .resolve("${this::class.simpleName!!}.txt")
            .also { if (this.fileFirstAccess) it.createNewFile() }
            .also { this.fileFirstAccess = false }

    protected abstract val maxItems: Int?
    // protected abstract val itemClass: Class<T>
    protected abstract fun fromString(s: String): T
    protected abstract fun toString(item: T): String
}