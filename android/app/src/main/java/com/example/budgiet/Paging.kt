package com.example.budgiet

import android.annotation.SuppressLint
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import org.jetbrains.annotations.Range
import java.util.concurrent.Executor
import java.util.concurrent.Executors

const val DEFAULT_MAX_PAGES = 4u

// NOTE: key is NOT a Location ID, but an index in the pagination
typealias PagingKey = UInt
typealias ListPager<T> = Pager<PagingKey, T>

/** The **function** that is in charge of getting the **data** that will be loaded by the [ListPagingSource].
 * ```kotlin
 * suspend (query: CharSequence, startIndex: UInt, length: UInt) -> List<T>
 * ```
 *
 * This function *must be* ***pure*** and ***consecutive***.
 * That is, it must always return the same [List] if given the same arguments,
 * and must always return items in order,
 * such that calling it with consecutive **startIndices** yields *consecutive* items.
 *
 * Here is an example of how it should behave:
 * ```kotlin
 * getPage("", 0, 5) -> [ 1, 2, 3, 4, 5 ]
 * getPage("", 1, 5) -> [ 2, 3, 4, 5, 6 ]
 * ```
 *
 * ### Suspend
 *
 * A [QueryPageGetter] runs in a **worker thread** and can adopt any *suspend* behavior they wish.
 * This allows the *UI* thread to keep rendering without stutters while the [QueryPageGetter] is producing a result.
 *
 * ### Parameters
 *
 *  * **query**: This is the string that the [SearchBar][androidx.compose.material3.SearchBar] linked to the [ListPagingSource]
 *     wants the getter to *query* for in a Database or an API endpoint.
 *
 *     If the getter does not need a query, use [rememberListPager] with [PageGetter] instead.
 *
 *  * **startIndex**: The index of the item that will be placed at the beginning of the *page*.
 *
 *  * **length**: The amount of items that the *pager* is requesting.
 *     This value only serves as a guideline, because the returned [List] can have any *size*,
 *     but the size of the [List] tells the *pager* whether there are more pages to load or not.
 *
 *     More info about this in the return.
 *
 * @return The *data* that will populate the **page**.
 *
 *   The **size** of the [List] should not exceed the **length** that the Pager requested.
 *   If it does exceed it, the Pager will [List] take a slice with only the required elements.
 *
 *   If the **size** of the [List] is less than the requested **length**,
 *   the *pager* will assume that there are no more items in the dataset and will not request any further pages. */
typealias QueryPageGetter<T> = suspend (CharSequence, UInt, UInt) -> List<T>
/** The same as [QueryPageGetter], but does not have a **query** parameter. */
typealias PageGetter<T> = suspend (UInt, UInt) -> List<T>

/** Create a [Pager] for a **query** result that persists in a [Composable].
 *
 * If the getter does not need a query, use [rememberListPager] with [PageGetter] instead.
 *
 * @param getPage See [ListPagingSource].
 * @param queryState See [ListPagingSource].
 * @param pageSize The number of *items* that a **page** can hold (i.e. how many items are requested at a time).
 * @param maxPages The number of **pages** that can be loaded. Loading more pages than this will cause previous pages to get *unloaded*. */
@Composable
fun <T: Any> rememberQueryListPager(
    /**```kotlin
     * suspend (query: CharSequence, startIndex: UInt, length: UInt) -> List<T>
     * ```
     * See [QueryPageGetter]. */
    getPage: QueryPageGetter<T>,
    queryState: TextFieldState,
    pageSize: UInt,
    maxPages: @Range(from = 4, to = Int.MAX_VALUE.toLong()) UInt = DEFAULT_MAX_PAGES,
): PagerController<T> = PagerController.new(Pager(defaultPageConfig(pageSize.toInt(), maxPages.toInt())) {
    ListPagingSource.withQuery(getPage, queryState)
})
/** Create a [Pager] for a **list** that persists in a [Composable].
 *
 * Same as [rememberQueryListPager], but does not use a **query** for getting pages.
 *
 * @param getPage See [ListPagingSource].
 * @param pageSize The number of *items* that a **page** can hold (i.e. how many items are requested at a time).
 * @param maxPages The number of **pages** that can be loaded. Loading more pages than this will cause previous pages to get *unloaded*.*/
@Composable
fun <T: Any> rememberListPager(
    /**```kotlin
     * suspend (startIndex: UInt, length: UInt) -> List<T>
     * ```
     * See [PageGetter]. */
    getPage: PageGetter<T>,
    pageSize: UInt,
    maxPages: @Range(from = 4, to = Int.MAX_VALUE.toLong()) UInt = DEFAULT_MAX_PAGES,
): PagerController<T> = PagerController.new(Pager(defaultPageConfig(pageSize.toInt(), maxPages.toInt())) {
    ListPagingSource.withoutQuery(getPage)
})

/** Same as [rememberListPager], but allows passing a custom executor to run [getPage] on.
 * The executor should be [Executors.newSingleThreadExecutor].
 *
 * This was specifically made to be called ***ONLY*** from semantic tests.
 *
 * @param getPage See [ListPagingSource].
 * @param pageSize The number of *items* that a **page** can hold (i.e. how many items are requested at a time).
 * @param maxPages The number of **pages** that can be loaded. Loading more pages than this will cause previous pages to get *unloaded*.*/
@UsableInTestsOnly
@Composable
internal fun <T: Any> rememberTestListPager(
    getPage: PageGetter<T>,
    pageSize: UInt,
    maxPages: @Range(from = 4, to = Int.MAX_VALUE.toLong()) UInt = DEFAULT_MAX_PAGES,
    executor: Executor,
): PagerController<T> = PagerController.new(Pager(defaultPageConfig(pageSize.toInt(), maxPages.toInt())) {
    ListPagingSource.forTest(getPage, executor)
})

private fun defaultPageConfig(pageSize: Int, maxPages: @Range(from = 4, to = Int.MAX_VALUE.toLong()) Int) = PagingConfig(
    pageSize = pageSize,
    initialLoadSize = pageSize,
    // Must be > pageSize * 3
    maxSize = pageSize * maxPages,
    // Don't let the pager return a bunch of unloaded items, we are going to show a single unloaded item at a time.
    enablePlaceholders = false,
)

/** Contains methods that allow *checking* and *controlling* the state of a [Pager].
 *
 * The controller is *bound* to a [Pager] after it is passed to a [Composable] that uses a [Pager].
 * Before the controller is *bound* none of its methods will have any effect and will return *default values*. */
class PagerController<T: Any> private constructor(
    val items: LazyPagingItems<T>,
) {
    /** Trigger a **refresh* in the [Pager]'s data,
     * invalidating *all loaded items* in the [Pager].
     *
     * Wrapper for [LazyPagingItems.refresh]. */
    fun refresh() = this.items.refresh()

    /** Check the **status** of the *Page* of items that are being **prepended** to the list. */
    val prependStatus: LoadState
        get() = this.items.loadState.prepend
    /** Check the **status** of *Pages* after a [refresh]. */
    val refreshStatus: LoadState
        get() = this.items.loadState.refresh
    /** Check the **status** of the *Page* of items that are being **appended** to the list. */
    val appendStatus: LoadState
        get() = this.items.loadState.append

    /** Returns whether the [Pager] is currently loading any items. */
    fun isLoading()
        = this.prependStatus is LoadState.Loading
        || this.refreshStatus is LoadState.Loading
        || this.appendStatus is LoadState.Loading

    companion object {
        /** Initializes the controller to operate on the provided [Pager] **items**. */
        @SuppressLint("ComposableNaming")
        @Composable
        internal fun <T: Any> new(pager: ListPager<T>): PagerController<T> {
            val pager = remember { pager }
            return PagerController(pager.flow.collectAsLazyPagingItems())
        }
    }
}

/** A generic [PagingSource] over a list of items.
 *
 * This class was mainly designed to be used for *text searches* that query a *database*,
 * but the implementation allows it to be *general purpose*,
 * as long as **getPage** conforms to [QueryPageGetter]'s expected behavior.
 *
 * Use the [withQuery] and [withoutQuery] constructors.
 *
 * ### Parameters
 *
 *  * **getPage**: Callback that gets the data for a Page. See [QueryPageGetter].
 *
 *  * **queryState**: The *state* value of the [androidx.compose.material3.SearchBar],
 *     which holds the **query** text.
 *
 *     Ideally, only a reference to the **query** text should be passed in here,
 *     but I don't think this is possible, so the whole *state* must be passed in. */
class ListPagingSource<T: Any> private constructor(
    /** Contains the **getPage** callback. */
    private val type: ListPagingSourceType<T>,
    /** An optional custom work [Executor] that runs **getPage**. Only set in tests. */
    private val executor: Executor? = null,
) : PagingSource<PagingKey, T>() {
    companion object {
        /** This constructor is for a [PagingSource] that uses a **query** to get pages.
         *
         * If the getter does not need a query, use [ListPagingSource.withoutQuery] with [PageGetter] instead. */
        fun <T: Any> withQuery(
            /**```kotlin
             * suspend (query: CharSequence, startIndex: UInt, length: UInt) -> List<T>
             * ```
             * See [QueryPageGetter]. */
            getPage: QueryPageGetter<T>,
            queryState: TextFieldState,
        ) = ListPagingSource(ListPagingSourceType.WithQuery(getPage, queryState))

        /** This constructor is for a [PagingSource] that ***does not*** use a **query** to get pages. */
        fun <T: Any> withoutQuery(
            /**```kotlin
             * suspend (startIndex: UInt, length: UInt) -> List<T>
             * ```
             * See [PageGetter]. */
            getPage: PageGetter<T>,
        ) = ListPagingSource(ListPagingSourceType.NoQuery(getPage))

        /** Same as [ListPagingSource.withoutQuery], but allows passing a custom executor to run [getPage] on.
         * The executor should be [Executors.newSingleThreadExecutor].
         *
         * This was specifically made to be called ***ONLY*** from semantic tests. */
        @UsableInTestsOnly
        internal fun <T: Any> forTest(
            getPage: PageGetter<T>,
            executor: Executor,
        ) = ListPagingSource(ListPagingSourceType.NoQuery(getPage), executor)
    }

    /** Return `null` if the **key** is out of bounds.
     *
     * Made the argument type [Int] instead of [UInt] to avoid underflow,
     * but **key** should always be [UInt] everywhere else. */
    private fun ensureValidKey(key: Int): PagingKey? = when {
        key >= 0 -> key.toUInt()
        else -> null
    }

    override suspend fun load(params: LoadParams<PagingKey>): LoadResult<PagingKey, T> {
        // If params.key is null, it is the first load, so we start loading with STARTING_KEY
        val start = params.key ?: 0u

        val getEmptyPage = {
            LoadResult.Page(
                data = listOf<T>(),
                prevKey = null as PagingKey?,
                nextKey = null,
            )
        }

        if (params.loadSize < 0) {
            return getEmptyPage()
        }

        val task = when (this.type) {
            is ListPagingSourceType.NoQuery -> suspend {
                this.type.getPage(start, params.loadSize.toUInt())
            }
            is ListPagingSourceType.WithQuery -> suspend {
                this.type.getPage(this.type.queryState.text, start, params.loadSize.toUInt())
            }
        }
        val result = if (this.executor == null) {
            runWork(task = task)
        } else {
            // This branch only runs on tests!
            runWork(this.executor, task)
        }
        val data = when (result) {
            // Ignore items that don't fit on this page. They should be loaded on the next page
            is Result.Ok -> result.value.take(params.loadSize)
            is Result.Err -> return LoadResult.Error(result.error)
        }

        return LoadResult.Page(
            data = data,
            // This will be the startKey of the next load() if it's going backwards
            prevKey = if (data.isEmpty()) {
                null
            } else {
                ensureValidKey(start.toInt() - data.size)
            },
            // Only provide a nextKey if there are potentially more items to load
            nextKey = if (data.size >= params.loadSize) {
                start + data.size.toUInt()
            } else {
                null
            },
        )
    }

    override fun getRefreshKey(state: PagingState<PagingKey, T>): PagingKey? {
        val anchorPosition = state.anchorPosition ?: return null
        val key = (anchorPosition - state.config.initialLoadSize / 2)
            .coerceAtLeast(0)
        return ensureValidKey(key)
    }
}

/** A *sum type* that represents whether the [ListPagingSource] uses a **query** to get pages. */
private sealed class ListPagingSourceType<out T> {
    class WithQuery<out T>(
        val getPage: QueryPageGetter<T>,
        val queryState: TextFieldState,
    ) : ListPagingSourceType<T>()
    class NoQuery<out T>(val getPage: PageGetter<T>) : ListPagingSourceType<T>()
}
