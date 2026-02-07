package com.example.budgiet.ui.utils

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.budgiet.ListPager
import com.example.budgiet.R
import com.example.budgiet.rememberListPager

/** When a [LazyColumn]'s [ListItem]'s **height** can't be determined because it has no content,
 * use this value for the **height** instead. */
private val LIST_ITEM_DEFAULT_HEIGHT = 70.5.dp
val LIST_SHAPE = RoundedCornerShape(16.dp)
val LIST_ITEM_SHAPE = RoundedCornerShape(4.dp)
const val LIST_DEFAULT_VISIBLE_ITEMS = 3.5f

/** Receiver scope for *Lists* (i.e. [ListColumn], [LazyDropdownMenu]).
 *
 * Emulates the same interface as [LazyListScope],
 * but instead exposes a custom [ListColumnItemScope],
 * which itself exposes the correct composable items to use in [ListColumn]. */
@Suppress("unused")
class ListScope<ItemScope: ListItemScope> internal constructor(
    private val innerScope: LazyListScope,
    private val newItemScope: (LazyItemScope) -> ItemScope,
) {
    /** See [LazyListScope.item]. */
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable ItemScope.() -> Unit,
    ) = this.innerScope.item(key, contentType) {
        this@ListScope.newItemScope(this)
            .content()
    }

    /** See [LazyListScope.items] (overload with **count**). */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable ItemScope.(index: Int) -> Unit,
    ) = this.innerScope.items(count, key, contentType) { i ->
        this@ListScope.newItemScope(this)
            .itemContent(i)
    }

    /** See [LazyListScope.items] (overload with [List]). */
    fun <T> items(
        items: List<T>,
        key: ((item: T) -> Any)? = null,
        contentType: (item: T) -> Any? = { null },
        itemContent: @Composable ItemScope.(item: T) -> Unit,
    ) = this.innerScope.items(items, key, contentType) { item ->
        this@ListScope.newItemScope(this)
            .itemContent(item)
    }

    /** See [LazyListScope.itemsIndexed]. */
    fun <T> itemsIndexed(
        items: List<T>,
        key: ((Int, T) -> Any)? = null,
        contentType: (Int, T) -> Any? = { _, _ -> null },
        itemContent: @Composable ItemScope.(Int, T) -> Unit,
    ) = this.innerScope.itemsIndexed(items, key, contentType) { i, item ->
        this@ListScope.newItemScope(this)
            .itemContent(i, item)
    }
}

/** A custom improvement of the [LazyItemScope] interface.
 * Implementors of the [LazyItemScope] in this codebase should use *this class* instead.
 *
 * This class exposes [itemWidth] and [itemHeight] to composables that are called in the context of a [ListItemScope] implementor.
 * These 2 properties must be passed as [MutableState] to this constructor by the implementor.
 *
 * This class provides default implementations the [Modifier] methods that are not implemented in [LazyItemScope]. */
abstract class ListItemScope internal constructor(
    private val innerScope: LazyItemScope,
    /** This is only used with [LazyDropdownMenu], as it is the only case where we need to manually set the width and height. */
    private val _itemWidth: MutableState<Dp?>?,
    private val _itemHeight: MutableState<Dp?>?,
): LazyItemScope {
    /** The declared **width** of an item in the *List*.
     *
     * The **width** of the *whole List* is set to this value.
     * Other items that don't know what their **width** should be should have it set to this value. */
    @Suppress("unused")
    val itemWidth: Dp?
        get() = this._itemWidth?.value

    /** The declared **height** of an item in the *List*.
     *
     * Determines the **height** of the whole list,
     * and also sets the height of *list items* that don't know what their height should be. */
    val itemHeight: Dp?
        get() = this._itemHeight?.value

    /** Makes the [Composable] that this [Modifier] is used on to be able to control the [ListColumn]'s *width*.
     * Even if multiple items call this *modifier*, only the first item will be able to set the value.
     *
     * This sets the **itemWidth** value in [ListColumnItemScope] to the actual width of the *first item* that is rendered on the list.
     *
     * This [Modifier] will only have an effect if the **List** Composable allows an *item* to set the **width**.*/
    @Composable
    fun Modifier.applyWidthToList(): Modifier {
        var modifier = this
        this@ListItemScope.run {
            if (this._itemWidth != null) {
                val density = LocalDensity.current

                modifier = modifier.onGloballyPositioned { coords ->
                    // Only set the height for the first rendered element
                    if (this._itemWidth.value == null) {
                        this._itemWidth.value = with(density) { coords.size.width.toDp() }
                    }
                }
            }
        }

        return modifier
    }
    /** Makes the [Composable] that this [Modifier] is used on to be able to control the [ListColumn]'s *height*.
     * Even if multiple items call this [Modifier], only the first item will be able to set the value.
     *
     * This sets the **itemHeight** value in [ListColumnItemScope] to the actual height of the *first item* that is rendered on the list.
     *
     * This [Modifier] will only have an effect if the **List** Composable allows an *item* to set the **height**. */
    @Composable
    fun Modifier.applyHeightToList(): Modifier {
        var modifier = this
        this@ListItemScope.run {
            if (this._itemHeight != null) {
                val density = LocalDensity.current

                modifier = modifier.onGloballyPositioned { coords ->
                    // Only set the height for the first rendered element
                    if (this._itemHeight.value == null) {
                        this._itemHeight.value = with(density) { coords.size.height.toDp() }
                    }
                }
            }
        }

        return modifier
    }

    override fun Modifier.fillParentMaxWidth(fraction: Float): Modifier {
        val modifier = this
        return this@ListItemScope.innerScope.run {
            modifier.fillParentMaxWidth(fraction)
        }
    }
    override fun Modifier.fillParentMaxHeight(fraction: Float): Modifier {
        val modifier = this
        return this@ListItemScope.innerScope.run {
            modifier.fillParentMaxHeight(fraction)
        }
    }
    override fun Modifier.fillParentMaxSize(fraction: Float): Modifier {
        val modifier = this
        return this@ListItemScope.innerScope.run {
            modifier.fillParentMaxSize(fraction)
        }
    }
}

/** A custom [LazyItemScope], which exposes the composables that should be used in [ListColumn].
 * All composables in here are implemented with [ListItem].
 *
 * * **[DataItem]**: Represents *good data* in the list.
 *
 * * **[LoadingItem]**: Renders an item with a **progress indicator** composable as the content,
 *     indicating that the [ListColumn] is waiting for more items to **load**.
 *
 *     This is primarily used in the [PagedListColumn].
 *
 * * **[ErrorItem]**: Represents *bad data* in the list.
 *
 *     This occurs when an **Exception** is thrown when an item is being fetched.
 *     For example, when the [Pager] attempts to load a page, but the loader throws. */
class ListColumnItemScope internal constructor(
    innerScope: LazyItemScope,
    itemHeight: MutableState<Dp?>,
    val itemShape: Shape,
) : ListItemScope(innerScope, _itemWidth = null, _itemHeight = itemHeight) {
    /** Represents *good data* in the [ListColumn]. */
    @Composable
    fun DataItem(
        headlineContent: @Composable (() -> Unit),
        modifier: Modifier = Modifier,
        overlineContent: @Composable (() -> Unit)? = null,
        supportingContent: @Composable (() -> Unit)? = null,
        leadingContent: @Composable (() -> Unit)? = null,
        trailingContent: @Composable (() -> Unit)? = null,
        colors: ListItemColors = ListItemDefaults.colors(),
        tonalElevation: Dp = ListItemDefaults.Elevation,
        shadowElevation: Dp = ListItemDefaults.Elevation
    ) {
        ListItem(
            headlineContent = headlineContent,
            modifier = modifier.applyHeightToList()
                .clip(this.itemShape),
            overlineContent = overlineContent,
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            colors = colors,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
        )
    }
    /** Renders an item with a **progress indicator** composable as the content,
     * indicating that the [ListColumn] is waiting for more items to **load**.
     *
     * This is primarily used in the [PagedListColumn]. */
    @Composable
    fun LoadingItem(
        modifier: Modifier = Modifier,
        progressIndicator: @Composable () -> Unit = { CircularProgressIndicator() },
    ) {
        ListItem(
            modifier = modifier
                .heightIn(min = this.itemHeight ?: LIST_ITEM_DEFAULT_HEIGHT)
                .clip(this.itemShape),
            headlineContent = { Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                progressIndicator()
            } }
        )
    }
    /** Represents *bad data* in the [ListColumn].
     *
     * This occurs when an **Exception** is thrown when an item is being fetched.
     * For example, when the [Pager] attempts to load a page, but the loader throws. */
    @Composable
    fun ErrorItem(modifier: Modifier = Modifier, type: String, message: String? = null) {
        val color = MaterialTheme.colorScheme.error
        ListItem(
            // This item does not need to be resized,
            // but it should also not set the List height because it has an irregular size due to the error message.
            modifier = modifier.clip(this.itemShape),
            leadingContent = { Icon(
                painterResource(R.drawable.info_24px),
                "Error",
                tint = color,
            ) },
            headlineContent = { Text("Error: $type", color = color) },
            supportingContent = message?.let { { Text(message, color = color) } }
        )
    }
}

/** A [LazyColumn] that uses custom composables for the **items**,
 * giving the [LazyColumn] a more proper *"list" look*.
 *
 * See [ListColumnItemScope] for details on these composables.
 *
 * @param state The state object to be used to control or observe the list's state.
 *   May be omitted if the caller is not interested in handling the list's state.
 * @param contentPadding Apply padding to the **content** of the list as a whole,
 *   *not* each individual item.
 *   This essentially allows adding padding to the *sides* of all items,
 *   *top* of the *first* item, and *bottom* of the *last* item.
 * @param reverseLayout See [LazyColumn].
 * @param visibleItems How many *list items* should be visible at a time.
 *   Essentially sets the [ListColumn]'s **height** in terms of its **items**'s heights.
 * @param shape The [Shape] around the list widget.
 *   This allows setting the **roundness** of the list's corners.
 * @param itemShape The [Shape] around individual **items** in the list.
 *   This allows setting the **roundness** of the corners of all items.
 *   This value only has an effect if an **item** is using one of the composables in [ListColumnItemScope].
 * @param dividerThickness How much **spacing** should be applied between each item in the list.
 * @param content The space to declare the items in the list.
 *   Use [LazyListScope.item] or [LazyListScope.items],
 *   and within those call one of the composables in [ListColumnItemScope]. */
@Composable
fun ListColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    visibleItems: Float = LIST_DEFAULT_VISIBLE_ITEMS,
    shape: Shape = LIST_SHAPE,
    itemShape: Shape = LIST_ITEM_SHAPE,
    dividerThickness: Dp = DividerDefaults.Thickness,
    content: ListScope<ListColumnItemScope>.() -> Unit
) {
    val itemHeight = remember { mutableStateOf<Dp?>(null) }
    // Get the height of the first item in the list to determine the size of the whole List widget.
    val listMaxHeight = (itemHeight.value ?: LIST_ITEM_DEFAULT_HEIGHT) * visibleItems + dividerThickness * 3
    val listMinHeight = (itemHeight.value ?: LIST_ITEM_DEFAULT_HEIGHT) * 1.25f + dividerThickness

    LazyColumn(
        // List's height should be conscious of it's items' and dividers' heights.
        modifier = modifier
            .heightIn(min = listMinHeight, max = listMaxHeight)
            .clip(shape),
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = Arrangement.spacedBy(dividerThickness),
    ) {
        ListScope(this) { innerScope ->
            ListColumnItemScope(innerScope, itemHeight, itemShape)
        }.content()
    }
}

/** Contains methods that allow *checking* and *controlling* the state of a [Pager].
 *
 * The controller is *bound* to a [Pager] after it is passed to a [Composable] that uses a [Pager].
 * Before the controller is *bound* none of its methods will have any effect and will return *default values*. */
class PagerController internal constructor() {
    private lateinit var _items: LazyPagingItems<*>
    private lateinit var pager: ListPager<*>

    /** Initializes the controller to operate on the provided [Pager] **items**. */
    @SuppressLint("ComposableNaming")
    @Composable
    internal fun <T: Any> bind(pager: ListPager<T>) {
        if (itemsNullable == null) {
            this.pager = pager
            this._items = pager.flow.collectAsLazyPagingItems()
        }
    }

    /** Don't forget to call [bind] before reading from this value!
     *
     * @throws IllegalArgumentException if the caller does not use the same pager that this controller was bound to.
     * @throws UninitializedPropertyAccessException if this controller has not been bound to a pager yet. */
    internal fun <T: Any> items(pager: ListPager<T>): LazyPagingItems<T> {
        if (this.pager !== pager) {
            throw IllegalArgumentException("Used a different pager than the one that this controller was bound to")
        }
        /* SAFETY: This cast will always work because _items is tied to the pager,
         * so they're going to have the same `T`,
         * and the values never change after they're initialized.
         * */
        @Suppress("UNCHECKED_CAST")
        return this._items as LazyPagingItems<T>
    }

    /** Exposed methods MUST use this instead of [items]. */
    private val itemsNullable: LazyPagingItems<*>?
        get() = if (this::_items.isInitialized) { this._items } else { null }

    /** Trigger a **refresh* in the [Pager]'s data,
     * invalidating *all loaded items* in the [Pager].
     *
     * Wrapper for [LazyPagingItems.refresh]. */
    fun refresh() = this.itemsNullable?.refresh()

    /** Check the **status** of the *Page* of items that are being **prepended** to the list. */
    val prependStatus: LoadState
        get() = this.itemsNullable?.loadState?.prepend ?: LoadState.NotLoading(false)
    /** Check the **status** of *Pages* after a [refresh]. */
    val refreshStatus: LoadState
        get() = this.itemsNullable?.loadState?.refresh ?: LoadState.NotLoading(false)
    /** Check the **status** of the *Page* of items that are being **appended** to the list. */
    val appendStatus: LoadState
        get() = this.itemsNullable?.loadState?.append ?: LoadState.NotLoading(false)
}

/** A [ListColumn] that uses a [Pager] to load items.
 *
 * See [ListColumn] for the rest of the *parameters*.
 *
 * @param pager The [Pager] is responsible for *loading* and *unloading* the data.
 *
 *   Since the [Pager] itself doesn't expose any methods to check state or read items,
 *   the caller must provide a [PagerController] to do any of that.
 *   If the caller is not interested in having any control over the [Pager],
 *   they may omit the **pagerController** argument, and an *inaccessible* [PagerController] will be used.
 *
 *   See [rememberListPager] of how to create a [Pager] in a composable.
 *
 * @param pagerController Allows the caller to trigger a *[refresh][PagerController.refresh]* on the [Pager]'s data.
 * @param itemKey A Callback that generates an *unique key* for every **item**. See [LazyListScope.items].
 * @param itemContent The [Composable] that will be called for *each item* in the [Pager]'s data.
 *   The caller *should* use [ListColumnItemScope.DataItem], but it is not required.
 * @param loadingContent The [Composable] that will be called when the list can't show an item yet because it is still loading.
 * @param errorContent The [Composable] that will be called when the **pager** loads a page that throws an [Exception].
 *   Takes a **type**, which is the *full class name* of the [Exception], and the exception's [**message**][Throwable.message]. */
@Composable
fun <T: Any> PagedListColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    visibleItems: Float = LIST_DEFAULT_VISIBLE_ITEMS,
    shape: Shape = LIST_SHAPE,
    itemShape: Shape = LIST_ITEM_SHAPE,
    dividerThickness: Dp = DividerDefaults.Thickness,
    pager: ListPager<T>,
    pagerController: PagerController = remember { PagerController() },
    itemKey: (T) -> Any,
    itemContent: @Composable ListColumnItemScope.(T) -> Unit,
    loadingContent: @Composable ListColumnItemScope.() -> Unit = { this.LoadingItem() },
    errorContent: @Composable ListColumnItemScope.(type: String, message: String?) -> Unit
        = { type, message -> this.ErrorItem(type = type, message = message) },
) {
    pagerController.bind(pager)

    ListColumn(modifier, state, contentPadding, reverseLayout, visibleItems, shape, itemShape, dividerThickness) {
        /** Renders the **LoadingItem**, **ErrorItem**, or **onLoaded** composables depending on the **status**. */
        fun statusItems(status: LoadState, onLoaded: (ListScope<ListColumnItemScope>.() -> Unit)? = null) {
            when (status) {
                is LoadState.Loading -> this.item {
                    loadingContent()
                }
                is LoadState.Error -> this.item {
                    errorContent(status.error.javaClass.name, status.error.message)
                }
                else -> if (onLoaded != null) {
                    this.onLoaded()
                }
            }
        }

        statusItems(pagerController.prependStatus)
        statusItems(pagerController.refreshStatus) {
            val items = pagerController.items(pager)
            this.items(items.itemCount,
                key = items.itemKey { item -> itemKey(item) }
            ) { item ->
                items[item]?.let { item ->
                    itemContent(item)
                } ?: run {
                    // This will never be null as long as enablePlaceholders = false in the Pager.
                    // Leave it here tho, in case we change it to true and forget about it.
                    loadingContent()
                }
            }
        }
        statusItems(pagerController.appendStatus)
    }
}
