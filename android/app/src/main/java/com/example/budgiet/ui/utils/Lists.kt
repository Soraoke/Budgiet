package com.example.budgiet.ui.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.window.PopupProperties
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.compose.itemKey
import com.example.budgiet.PagerController
import com.example.budgiet.R
import com.example.budgiet.rememberListPager

/** When a [LazyColumn]'s [ListItem]'s **height** can't be determined because it has no content,
 * use this value for the **height** instead. */
private val LIST_ITEM_DEFAULT_HEIGHT = 70.5.dp
private val MENU_ITEM_DEFAULT_WIDTH = 125.dp
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

    /** Adds a [List] of items to the List Composable, but only a sublist of that exists at a time.
     * Items are *created* and *destroyed* on the fly as needed, depending on the scroll position of the Composable.
     *
     * @param pager The [Pager] is responsible for *loading* and *unloading* the data.
     *   Use [rememberListPager] to create a [Pager] in a composable.
     * @param itemKey A Callback that generates an *unique key* for every **item**. See [LazyListScope.items].
     * @param itemContent The [Composable] that will be called for *each item* in the [Pager]'s data.
     *   The caller *should* use [ListColumnItemScope.DataItem], but it is not required.
     * @param loadingContent The [Composable] that will be called when the list can't show an item yet because it is still loading.
     * @param errorContent The [Composable] that will be called when the **pager** loads a page that throws an [Exception].
     *   Takes a **type**, which is the *full class name* of the [Exception], and the exception's [**message**][Throwable.message]. */
    fun <T: Any> pagedItems(
        pager: PagerController<T>,
        itemKey: (T) -> Any,
        loadingContent: @Composable ItemScope.() -> Unit = { this.LoadingItem() },
        errorContent: @Composable ItemScope.(type: String, message: String?) -> Unit
            = { type, message -> this.ErrorItem(type = type, message = message) },
        itemContent: @Composable ItemScope.(T) -> Unit,
    ) = this.pagedItemsIndexed(
        pager = pager,
        itemKey = { _, item -> itemKey(item) },
        loadingContent = { loadingContent() },
        errorContent = { _, type, message -> errorContent(type, message) },
        itemContent = { _, item -> itemContent(item) },
    )

    /** Same as [pagedItems], but provides an additional **index** argument to each callback.
     *
     * The **index** is based of the [pager] items.
     * The [List][ListColumn] Composable can display a *loading item* as the first item.
     * When this is the case, [itemContent] will receive an **index** of `0`,
     * even though it is actually the *second* item. */
    fun <T: Any> pagedItemsIndexed(
        pager: PagerController<T>,
        itemKey: (idx: Int, T) -> Any,
        loadingContent: @Composable ItemScope.(idx: Int) -> Unit = { this.LoadingItem() },
        errorContent: @Composable ItemScope.(idx: Int, type: String, message: String?) -> Unit
            = { idx, type, message -> this.ErrorItem(type = type, message = message) },
        itemContent: @Composable ItemScope.(idx: Int, T) -> Unit,
    ) {
        /** Renders the **LoadingItem**, **ErrorItem**, or **onLoaded** composables depending on the **status**. */
        fun statusItems(idx: Int, status: LoadState, onLoaded: (ListScope<ItemScope>.(idx: Int) -> Unit)? = null) {
            when (status) {
                is LoadState.Error -> this.item {
                    errorContent(idx, status.error.javaClass.name, status.error.message)
                }
                is LoadState.Loading -> this.item {
                    loadingContent(idx)
                }
                is LoadState.NotLoading -> if (onLoaded != null) {
                    this.onLoaded(idx)
                }
            }
        }

        statusItems(0, pager.refreshStatus) {
            statusItems(0, pager.prependStatus)

            val items = pager.items
            this.items(
                count = items.itemCount,
                key = { idx -> items.itemKey { itemKey(idx, it) }(idx) },
            ) { idx ->
                items[idx]?.let { item ->
                    itemContent(idx, item)
                } ?: run {
                    // This will never be null as long as enablePlaceholders = false in the Pager.
                    // Leave it here tho, in case we change it to true and forget about it.
                    loadingContent(idx)
                }
            }

            statusItems(items.itemCount, pager.appendStatus)
        }
    }
}

/** A custom improvement of the [LazyItemScope] interface.
 * Implementors of the [LazyItemScope] in this codebase should use *this class* instead.
 *
 * This class exposes [itemWidth] and [itemHeight] to composables that are called in the context of a [ListItemScope] implementor.
 * These 2 properties must be passed as [MutableState] to this constructor by the implementor.
 *
 * This class also exposes [Composables][Composable] that should be used to display certain types of data in the list:
 * * **[LoadingItem]**: Renders an item with a **progress indicator** composable as the content,
 *     indicating that the [ListColumn] is waiting for more items to **load**.
 *
 * * **[ErrorItem]**: Represents *bad data* in the list.
 *
 *     This occurs when an **Exception** is thrown when an item is being fetched.
 *     For example, when the [Pager] attempts to load a page, but the loader throws an **Exception**.
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

    /** Renders an item with a **progress indicator** composable as the content,
     * indicating that the [ListColumn] is waiting for more items to **load**.
     *
     * This is primarily used in the [ListScope.pagedItems]. */
    @Composable
    abstract fun LoadingItem(
        modifier: Modifier = Modifier,
        progressIndicator: @Composable () -> Unit = { CircularProgressIndicator() },
    )

    /** Represents *bad data* in the [ListColumn].
     *
     * This occurs when an **Exception** is thrown when an item is being fetched.
     * For example, when the [Pager] attempts to load a page, but the loader throws. */
    @Composable
    abstract fun ErrorItem(modifier: Modifier = Modifier, type: String, message: String? = null)

    /** Represents *bad data* in the [ListColumn].
     *
     * This occurs when an **Exception** is thrown when an item is being fetched.
     * For example, when the [Pager] attempts to load a page, but the loader throws. */
    @Composable
    fun ErrorItem(error: Throwable, modifier: Modifier = Modifier) = ErrorItem(modifier, error.javaClass.name, error.message)
}

/** A custom [LazyItemScope], which exposes the composables that should be used in [ListColumn].
 * All composables in here are implemented with [ListItem]. */
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
    @Composable
    override fun LoadingItem(modifier: Modifier, progressIndicator: @Composable () -> Unit) {
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
    @Composable
    override fun ErrorItem(modifier: Modifier, type: String, message: String?) {
        val color = MaterialTheme.colorScheme.error
        ListItem(
            // This item does not need to be resized,
            // but it should also not set the List height because it has an irregular size due to the error message.
            modifier = modifier.clip(this.itemShape),
            leadingContent = { Icon(
                painterResource(R.drawable.error_24px),
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
 * Adding items in the **content** is very similar to [LazyColumn]:
 * call `this.items()` with the list you want to display,
 * and within that call a Composable in **[ListColumnItemScope]** (e.g. [DataItem][ListColumnItemScope.DataItem]).
 * Using those Composables is important so that the Column element
 * can define a *height* based on the height of its *items* (see the [visibleItems] parameter).
 *
 * See [ListColumnItemScope] for details on these composables.
 *
 * ## Example
 *
 * ```
 * ListColumn(visibleItems = 3.5) {
 *     this.items(
 *         items = (0..100).toList(),
 *         key = { it },
 *     ) { i ->
 *         this.DataItem(headlineContent = {
 *             Text(i.toString())
 *         })
 *     }
 * }
 * ```
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
 *   Use [ListScope.item] or [ListScope.items],
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
//    val listMinHeight = (itemHeight.value ?: LIST_ITEM_DEFAULT_HEIGHT) * 1.25f + dividerThickness

    LazyColumn(
        // List's height should be conscious of its items' and dividers' heights.
        modifier = modifier
            .heightIn(max = listMaxHeight)
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

/** A custom [LazyItemScope], which exposes the composables that should be used in [LazyDropdownMenu].
 * All composables in here are implemented with [DropdownMenuItem].  */
class LazyMenuItemScope internal constructor(
    innerScope: LazyItemScope,
    itemWidth: MutableState<Dp?>,
    itemHeight: MutableState<Dp?>,
): ListItemScope(innerScope, _itemWidth = itemWidth, _itemHeight = itemHeight) {
    /** An item data composable for the [LazyDropdownMenu].
     *
     * Similar to [ListColumnItemScope.DataItem],
     * but renders a [DropdownMenuItem] instead.
     *
     * This is specifically for use with [LazyDropdownMenu] to determine the height. */
    @Composable
    fun MenuItem(
        modifier: Modifier = Modifier,
        headlineContent: @Composable (() -> Unit),
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        onClick: () -> Unit = { },
        enabled: Boolean = true,
        colors: MenuItemColors = MenuDefaults.itemColors(),
        contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
        interactionSource: MutableInteractionSource? = null,
    ) {
        DropdownMenuItem(
            modifier = modifier
                .applyWidthToList()
                .applyHeightToList(),
            text = headlineContent,
            onClick = onClick,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            enabled = enabled,
            colors = colors,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
        )
    }

    @Composable
    override fun LoadingItem(modifier: Modifier, progressIndicator: @Composable () -> Unit)
        = MenuLoadingItem(modifier = modifier
            .heightIn(min = this.itemHeight ?: LIST_ITEM_DEFAULT_HEIGHT)
            .widthIn(min = this.itemWidth ?: MENU_ITEM_DEFAULT_WIDTH)
        )
    @Composable
    override fun ErrorItem(modifier: Modifier, type: String, message: String?)
        = MenuErrorItem(modifier, type, message)
}

@Composable
fun MenuLoadingItem(modifier: Modifier = Modifier, progressIndicator: @Composable () -> Unit = { CircularProgressIndicator() }) {
    DropdownMenuItem(
        modifier = modifier,
        text = { Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            progressIndicator()
        } },
        enabled = false,
        onClick = { },
    )
}

@Composable
fun MenuErrorItem(modifier: Modifier = Modifier, type: String, message: String? = null) {
    val color = MaterialTheme.colorScheme.error
    DropdownMenuItem(
        // This item does not need to be resized,
        // but it should also not set the List height because it has an irregular size due to the error message.
        modifier = modifier,
        leadingIcon = { Icon(
            painterResource(R.drawable.error_24px),
            "Error",
            tint = color,
        ) },
        text = { Column {
            Text("Error: $type" + if (message != null) ": " else "",
                color = color,
                style = MaterialTheme.typography.labelLarge,
            )
            message?.let { Text(message, color = color) }
        } },
        enabled = false,
        onClick = { },
    )
}

/** A [DropdownMenu] that can hold a *list* of items and *lazily* display only the items that will be visible.
 * This is very similar to [ListColumn].
 *
 * Place a call to this Composable ***directly after*** the UI element that you want the [DropdownMenu] to be anchored to.
 *
 * Adding items in the **listContent** is very similar to [ListColumn] (and by extension [LazyColumn]):
 * call `this.items()` with the list you want to display,
 * and within that call a Composable in **[LazyMenuItemScope]** (i.e. [MenuItem][LazyMenuItemScope.MenuItem]).
 * Using those Composables is important so that the Column element
 * can define a *height* based on the height of its *items* (see the [visibleItems] parameter).
 *
 * Not using [MenuItem][LazyMenuItemScope.MenuItem] will result in the app ***crashing***
 * because [DropdownMenu] requires that all of its content have *absolute size* (both width and height).
 *
 * See [ListColumn] for the rest of the *parameters*.
 *
 * @param showDropdown Whether the **dropdown** popup is being displayed.
 *   This includes [leadingContent], [listContent], and [trailingContent].
 * @param onDismiss The action that will be activated when the user clicks *outside* the [DropdownMenu].
 * @param properties Properties to customize the behavior of the [DropdownMenu].
 * @param tonalElevation How much tint the [DropdownMenu]'s *surface color* will receive
 *   (if it uses the default surface color).
 * @param shadowElevation The strength (opacity, size) of the shadow surrounding the [DropdownMenu].
 * @param leadingContent The Composable that is shown **before** the [ListColumn] in the [DropdownMenu].
 * @param trailingContent The Composable that is shown **after** the [ListColumn] in the [DropdownMenu].
 * @param listContent The items that will go in the [ListColumn].
 *   Use [ListScope.item] or [ListScope.items],
 *   and within those call [LazyMenuItemScope.MenuItem]. */
@Composable
fun LazyDropdownMenu(
    modifier: Modifier = Modifier,
    showDropdown: Boolean,
    onDismiss: () -> Unit,
    properties: PopupProperties = POPUP_PROPERTIES,
    state: LazyListState = rememberLazyListState(),
    reverseLayout: Boolean = false,
    visibleItems: Float = LIST_DEFAULT_VISIBLE_ITEMS,
    shape: Shape = MenuDefaults.shape,
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    border: BorderStroke? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    listContent: ListScope<LazyMenuItemScope>.() -> Unit,
) {
    // Get the size of the first item in the list to determine the size of the whole List widget.
    val itemWidth = remember { mutableStateOf<Dp?>(null) }
    val itemHeight = remember { mutableStateOf<Dp?>(null) }
    val menuWidth = itemWidth.value ?: MENU_ITEM_DEFAULT_WIDTH
    val menuHeight = (itemHeight.value ?: LIST_ITEM_DEFAULT_HEIGHT) * visibleItems

    DropdownMenu(
        modifier = modifier.hideDropdownMenuPadding()
            .clip(shape),
        expanded = showDropdown,
        onDismissRequest = onDismiss,
        properties = properties,
        shape = shape,
        containerColor = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
    ) {
        if (leadingContent != null) {
            Box(Modifier.width(menuWidth).heightIn(max = menuHeight)) {
                leadingContent()
            }
        }

        LazyColumn(
            modifier = Modifier
                // FIXME: MenuItem does not set its width (it is intrinsic to the parent), so the List's width stays at the default (125).
                .width(menuWidth)
                .height(menuHeight),
            state = state,
            reverseLayout = reverseLayout,
            contentPadding = contentPadding,
        ) {
            ListScope(this) { innerScope ->
                LazyMenuItemScope(innerScope, itemWidth, itemHeight)
            }.listContent()
        }

        if (trailingContent != null) {
            Box(Modifier.width(menuWidth).heightIn(max = menuHeight)) {
                trailingContent()
            }
        }
    }
}
