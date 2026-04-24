@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.budgiet.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgiet.DbEntry
import com.example.budgiet.R
import com.example.budgiet.Result
import com.example.budgiet.rememberListPager
import com.example.budgiet.rememberQueryListPager
import com.example.budgiet.rememberWork
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.ActionDialog
import com.example.budgiet.ui.utils.ActionDialogPadding
import com.example.budgiet.ui.utils.Corner
import com.example.budgiet.ui.utils.FilledTextIconButton
import com.example.budgiet.ui.utils.ItemActionsMenu
import com.example.budgiet.ui.utils.ListColumn
import com.example.budgiet.ui.utils.ListColumnItemScope
import com.example.budgiet.ui.utils.MenuErrorItem
import com.example.budgiet.ui.utils.MenuLoadingItem
import com.example.budgiet.ui.utils.PlainSearchBar
import com.example.budgiet.ui.utils.PlainToolTipBox
import com.example.budgiet.ui.utils.halfRoundedCornerShape
import com.example.budgiet.ui.utils.hideDropdownMenuPadding
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.util.Objects
import kotlin.math.ceil
import kotlin.math.min

val FAKE_LOCATIONS = mapOf(
    0u  to Location("Chipotle", "123 Main Street, Bronx NY"),
    1u  to Location("Aldi", "456 IsNuts Lane, Los Angeles CA"),
    2u  to Location("Bowling Alley", "789 Trampoline Street, Detroit MI"),
    3u  to Location("Six Flags Great Adventure", "1 Six Flags Blvd, Jackson Township, NJ 08527"),
    4u  to Location("Reading Terminal Market", "1136 Arch St, Philadelphia, PA 19107"),
    5u  to Location("Angie's Seafood", "1727 E Pratt St, Baltimore, MD 21231"),
    6u  to Location("Ichiran", "132 W 31st St, New York, NY 10001"),
    7u  to Location("Frugal Bookstore", "57 Warren St, Roxbury, MA 02119"),
    8u  to Location("Sonic Boom", "215 Spadina Ave., Toronto, ON M5T 2C7, Canada"),
    9u  to Location("The Little Grand Market", "710 Grandview Xing Wy Suite 112, Columbus, OH 43215"),
    10u to Location("Five Guys", "3273 Steelyard Dr, Cleveland, OH 44109"),
)

data class Location(
    val name: String,
    val address: String? = null,
    // TODO: remove this when we have a real db
    val lastUsed: LocalTime? = null,
) {
    override fun equals(other: Any?) = when (other) {
        is Location -> this.name == other.name && this.address == other.address
        else -> false
    }
    override fun toString() = "$name${address?.let { " at $it" } ?: ""}"
    override fun hashCode() = Objects.hash(name, address)
}

class LocationViewModel: ViewModel() {
    private val fakeDb = mutableStateMapOf<UInt, Location>()
    /** Makes the [ViewModel] ignore the internal database, and instead will hold the [Location]s data in a [MutableList] in memory.
     *
     * Don't use in production :D */
    internal fun useAlternativeLocations(locations: Map<UInt, Location>) {
        this.fakeDb.clear()
        locations.forEach { this.fakeDb[it.key] = it.value }
    }

    var selectedLocation by mutableStateOf<DbEntry<Location>?>(null)

    // TODO: doc, throws
    fun locationsPage(query: CharSequence?, start: UInt, len: UInt): List<DbEntry<Location>> {
        val list = this.fakeDb
            .entries.toList()
            .map { DbEntry(it.key, it.value) }
            .run { if (query != null) {
                filter {
                    it.data.name.contains(query, ignoreCase = true)
                    || it.data.address?.contains(query, ignoreCase = true) ?: false
                }
            } else this }

        val toIdx = min((start + len).toInt(), list.size)
        val fromIdx = min(start.toInt(), toIdx)
        return list.subList(fromIdx, toIdx)
            .sortedByDescending { it.data.lastUsed }

        // TODO: real impl
    }

    // TODO: doc
    fun nearbyLocations(context: Context, searchRadius: UInt): List<DbEntry<Location>> {
        TODO()
    }

    // TODO: doc
    fun nearbyAddresses(context: Context, searchRadius: UInt): List<String> {
        TODO()
    }

    fun newLocation(data: Location): DbEntry<Location> {
        val data = data.copy(lastUsed = LocalTime.now())

        var id = 0u
        while (this.fakeDb.keys.contains(id)) {
            id++
        }

        this.fakeDb[id] = data
        return DbEntry(id, data)

        // TODO: real impl
    }

    fun editLocation(id: UInt, newData: Location) {
        val newData = newData.copy(lastUsed = LocalTime.now())

        this.fakeDb.replace(id, newData)
        // TODO: real impl

        // Update selected location.
        if (this.selectedLocation?.let { id == it.id } ?: false) {
            this.selectedLocation = DbEntry(id, newData)
        }
    }

    fun deleteLocation(id: UInt) {
        this.fakeDb.remove(id)
        // TODO: real impl

        // Update selected location.
        if (this.selectedLocation?.let { id == it.id } ?: false) {
            this.selectedLocation = null
        }
    }

    /** Returns an Error if the [Location] could not be submitted because of collisions with *other* [Location]s.
     *
     *  This function should only check the **`data`** against *other distinct* items,
     *  so it has to avoid checking it against the original [Location] data it is editing from.
     *  If the function is being called for ***editing*** an existing [Location] item,
     *  the **`ogData`** argument should have that data, otherwise the argument should be `null`.
     *
     *  Note that this function *does not* check if the [**name**][Location.name] or [**address**][Location.address] are valid data that can be put in the database.
     *  This should be checked with [validateName] and [validateAddress] respectively. */
    // TODO: make unit test (but in rust) for this:
    //   Add new location (name, addr) when location (name, addr) with the same (addr) but diff name exists
    //   Add new location (name, no addr) when location (name, addr) with same (name) exists
    //   Add new location (name, no addr) when location (name, no addr) with same (name) exists
    //   Add new location (name, addr) when location (name, addr) with same (name, addr) exists
    //   Add location A (name, addrA), Add location B (name, addrB), Try add location A and B again (fail)
    //   Edit existing location, repeating all same rules as above.
    fun validateData(data: Location, ogData: Location? = null): Result<Unit> {
        this.fakeDb.values
            // Don't check data against original item data.
            .run { ogData?.let {
                filter { !(it.name == ogData.name && it.address == ogData.address) }
            } ?: this }
            .forEach { other ->
                val msg = if (data.name == other.name) {
                    when (Pair(data.address == null, other.address == null)) {
                        Pair(true, true),
                        Pair(false, true) -> "A location with this name and no address already exists.\nTry using another name."
                        Pair(true, false) -> "A location with this name already exists.\nTry adding an address (or use another name) to differentiate them."
                        Pair(false, false) -> if (data.address == other.address) {
                            "A location with this name and address already exists."
                        } else {
                            null
                        }
                        else -> null
                    }
                } else {
                    null
                }

                msg?.let {
                    return Result.Err(Exception(msg))
                }
            }

        return Result.Ok(Unit)
    }
    fun validateName(name: String): Result<Unit> {
        val msg = if (name.isEmpty()) {
            "Name must not be empty"
        } else {
            null
        }

        return msg?.let { Result.Err(Exception(msg)) }
            ?: Result.Ok(Unit)
    }
    fun validateAddress(address: String) = Result.Ok(Unit)
}

/** Displays the [Location] selected by the user to be assigned to the [NewTransaction][NewTransactionForm].
 *
 * This displays 2 buttons:
 *  1. Displays the *currently selected [Location]* and opens the [LocationPickerDialog].
 *  2. To search from [nearbyLocations][LocationViewModel.nearbyLocations].
 *
 * @param onClickSelect The action that runs when the *"Select location"* button is *clicked*.
 *   This action should open the [LocationPickerDialog].
 * @param onClickNearby The action that runs when the *"Nearby locations"* button is *clicked*.
 *   This action should open the [NearbyLocationsDialog]. */
@Composable
fun RowScope.LocationField(
    viewModel: LocationViewModel,
    onClickSelect: () -> Unit,
    onClickNearby: () -> Unit,
) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.TopEnd,
    ) {
        FilledTonalButton(
            modifier = Modifier
                .semantics {
                    contentDescription = "Select location"
                    stateDescription = viewModel.selectedLocation?.data?.toString() ?: "None selected"
                },
            onClick = onClickSelect,
            // Modify the shape on the left-side of the button to connect with the auto-select location button.
            shape = halfRoundedCornerShape(Corner.Right),
        ) {
            Text(viewModel.selectedLocation?.data?.toString() ?: "Select Location")
        }
    }
    PlainToolTipBox("Nearby locations") {
        FilledIconButton(
            onClick = onClickNearby,
            shape = halfRoundedCornerShape(Corner.Left)
        ) {
            Icon(painterResource(R.drawable.location_on_24px), null)
        }
    }
}

/** State structure for [LocationEditorDialog].
 *
 * Can be one of **[Search]**, **[Nearby]**, **[New]**, and **[Edit]**. */
sealed class LocationPickerState {
    /** Select one of the [Location]s that already exist in the *database* from a list.
     * Also has a *search bar* for filtering **name** and **address**. */
    object Search: LocationPickerState()
    /** Select one of the [Location]s that already exist in the *database* from a **Map View**. */
    object Nearby: LocationPickerState()
    /** Form to *create* a new [Location] item. */
    object New: LocationPickerState()
    /** Form to *edit* an existing [Location] item. */
    class Edit(val location: DbEntry<Location>): LocationPickerState()
}
/** Shows a [Dialog][ActionDialog] that allows the user to select a [Location].
 * Shows different content depending on the [LocationPickerState]. */
@Composable
fun LocationPickerDialog(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel,
    state: LocationPickerState,
    onStateChange: (LocationPickerState) -> Unit,
    onDismiss: () -> Unit,
) {
    var newLocationId by remember { mutableStateOf<UInt?>(null) }

    @Suppress("AssignedValueIsNeverRead")
    when (state) {
        LocationPickerState.Search -> {
            LocationSearchDialog(
                modifier = modifier,
                viewModel = viewModel,
                onDismiss = onDismiss,
                onSubmit = { viewModel.selectedLocation = it },
                onNewClick = { onStateChange(LocationPickerState.New) },
                onEditClick = { onStateChange(LocationPickerState.Edit(it)) },
                newLocationId = newLocationId,
            )
        }
        LocationPickerState.Nearby -> {
            NearbyLocationsDialog(
                modifier = modifier,
                viewModel = viewModel,
                onDismiss = onDismiss,
            )
        }
        LocationPickerState.New,
        is LocationPickerState.Edit -> {
            val editLocation = if (state is LocationPickerState.Edit) {
                state.location
            } else null

            LocationEditorDialog(
                modifier = modifier,
                location = editLocation?.data,
                viewModel = viewModel,
                onDismiss = { onStateChange(LocationPickerState.Search) },
                onSubmit = { newLocationId = editLocation?.let { editLoc ->
                    viewModel.editLocation(editLoc.id, it)
                    editLoc.id
                } ?: run {
                    viewModel.newLocation(it).id
                } },
            )
        }
    }
}

/** Display a [Dialog][ActionDialog] that contains a [List][ListColumn] of [Location]s
 * and a [SearchBar][PlainSearchBar] to filter through all the locations in the list.
 *
 * The user can click a [Location] item to select it.
 *
 * @param onSubmit The action that runs when the user clicks the `"Submit"` button.
 *   Passes the [Location] and its **ID** as the argument.
 *   This action should set the [selectedLocation][LocationViewModel.selectedLocation].
 * @param newLocationId When a new [Location] item was added to the list of locations,
 *   that item (by ID) in the list of *Recents* will have an *animated scrim* to indicate that it was just added.
 * @param onNewClick The action to run when the `"New Location"` button is clicked.
 *   This should show the [LocationEditorDialog] for creating a *new* [Location].
 * @param onEditClick The action to run when the `"Edit"` button is clicked from a Location's [ItemActionsMenu].
 *   Like [onNewClick], this should show the [LocationEditorDialog] for editing the [Location] in the argument. */
@Composable
private fun LocationSearchDialog(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel,
    newLocationId: UInt? = null,
    onDismiss: () -> Unit,
    onSubmit: (DbEntry<Location>) -> Unit,
    onNewClick: () -> Unit,
    onEditClick: (DbEntry<Location>) -> Unit,
) {
    val searchColumnSize = 3.5f
    val newItemAnimationSpeed = 250 // In millis
    val newItemAnimationDuration = newItemAnimationSpeed * 5 // Repeat n times; In millis
    val scrimColor = MaterialTheme.colorScheme.secondaryContainer
    val pageSize = ceil(searchColumnSize).toUInt() * 3u
    val searchState = rememberTextFieldState()
    /** Animation is reset when the newLocation value is modified. */
    var animateNewItemScrim by remember(newLocationId) { mutableStateOf(newLocationId != null) }

    val searchPager = rememberQueryListPager(
        queryState = searchState,
        getPage = { query, start, len -> viewModel.locationsPage(query, start, len) },
        // Page size should have enough items to scroll down several times the number of items showed.
        pageSize = pageSize,
    )
    val recentsPager = rememberListPager(
        getPage = { start, len -> viewModel.locationsPage(null, start, len) },
        pageSize = pageSize,
    )

    fun close() {
        onDismiss()
        // Cancel pending page loading jobs.
        searchState.clearText()
        searchPager.refresh()
    }

    @Composable
    fun ListColumnItemScope.LocationItem(item: DbEntry<Location>, query: CharSequence? = null, isNew: Boolean = false) {
        val defaultContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
        val newItemAnimatedColor = if (isNew && animateNewItemScrim) {
            LaunchedEffect(Unit) {
                delay(newItemAnimationDuration.toLong())
                animateNewItemScrim = false
            }
            rememberInfiniteTransition()
                .animateColor(
                    initialValue = defaultContainerColor,
                    targetValue = scrimColor,
                    animationSpec = infiniteRepeatable(
                        animation = tween(newItemAnimationSpeed),
                        repeatMode = RepeatMode.Reverse,
                    ),
                )
                .value
        } else {
            null
        }

        /** Creates an [AnnotatedString] that highlights (**emboldens**) substring instances of the **`query`** in the **`text`**.
         *
         * Does nothing if the **`query`** is `null`. */
        fun highlightMatches(text: String, query: CharSequence?): AnnotatedString {
            val instances = Regex(query.toString(), RegexOption.IGNORE_CASE)
                .findAll(text)
                .map { it.range.first }

            val spans = if (query != null) {
                instances.map { idx ->
                    AnnotatedString.Range(
                        item = SpanStyle(fontWeight = FontWeight.ExtraBold),
                        start = idx,
                        end = idx + query.length,
                    )
                }.toList()
            } else {
                emptyList()
            }

            return AnnotatedString(text, spanStyles = spans)
        }

        var showActionsMenu by rememberSaveable { mutableStateOf(false) }

        Box {
            DataItem(
                modifier = Modifier
                    .semantics { contentDescription = item.data.toString() }
                    .combinedClickable(
                        onClick = {
                            onSubmit(item)
                            // Put item first in the list (sorted by lastUsed).
                            viewModel.editLocation(item.id, item.data)
                            close()
                        },
                        onLongClick = { showActionsMenu = true },
                    ),
                colors = ListItemDefaults.colors(
                    // Show a scrim
                    containerColor = if (showActionsMenu) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else if (viewModel.selectedLocation?.let { it.id == item.id } ?: false) {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    } else newItemAnimatedColor
                    ?: defaultContainerColor,
                ),
                headlineContent = { Text(highlightMatches(item.data.name, query)) },
                supportingContent = item.data.address?.let {{ Text(highlightMatches(it, query)) }},
            )
            ItemActionsMenu(
                expanded = showActionsMenu,
                onDismiss = { showActionsMenu = false },
                onEditClick = { onEditClick(item) },
                onDeleteClick = {
                    viewModel.deleteLocation(item.id)
                    searchPager.refresh()
                    recentsPager.refresh()
                },
            )
        }
    }

    ActionDialog(
        modifier = modifier.semantics {
            contentDescription = "Select a location"
        },
        onDismiss = { close() },
        padding = ActionDialogPadding.TightlyPacked,
        title = {
            PlainSearchBar(
                placeholderText = "Search existing locations",
                onQueryChange = { searchPager.refresh() },
                state = searchState,
            )
        },
        actions = {
            TextButton(onClick = { close() }) {
                Text("Cancel")
            }

            PlainToolTipBox("Add new location") {
                FilledTextIconButton(
                    onClick = onNewClick,
                    icon = { Icon(painterResource(R.drawable.add_24px), null) },
                    text = { Text("New") },
                )
            }
        }
    ) {
        AnimatedContent(searchState.text.isEmpty()) { queryIsEmpty ->
            val emptyListModifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(ActionDialogPadding.Default.dialogEdges)

            Column {
                // Show search results if the SearchBar has a query,
                // otherwise show recent locations
                if (queryIsEmpty) {
                    Text("Recent", Modifier
                        .fillMaxWidth()
                        .padding(start = ActionDialogPadding.TightlyPacked.dialogEdges.calculateStartPadding(LocalLayoutDirection.current))
                    )

                    ListColumn(visibleItems = searchColumnSize) {
                        if (recentsPager.isLoading().not()
                        && recentsPager.items.itemSnapshotList.isEmpty()) {
                            this.item {
                                Column(
                                    emptyListModifier,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text("There are no existing Locations.", textAlign = TextAlign.Center)
                                    Text("Press \"New\" to create one.", textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            this.pagedItems(
                                pager = recentsPager,
                                itemKey = { item -> item.id.toInt() }, // Why can't use UInt ....
                            ) { item ->
                                LocationItem(item, isNew = item.id == newLocationId)
                            }
                        }
                    }
                } else {
                    ListColumn(visibleItems = searchColumnSize) {
                        if (searchPager.isLoading().not()
                        && searchPager.items.itemSnapshotList.isEmpty()) {
                            this.item {
                                Column(
                                    emptyListModifier,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text("No matching locations", textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            this.pagedItems(
                                pager = searchPager,
                                itemKey = { item -> item.id.toInt() },
                            ) { item ->
                                this.LocationItem(item, query = searchState.text)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Display a [Dialog][ActionDialog] that prompts the user for information of a [Location] they want to *edit or create*.
 *
 * @param location The data of the [Location] that is being modified.
 * @param onSubmit The action that runs when the user clicks the `"Submit"` button and all the data has been validated.
 *   This provides an argument with the *new* [Location] data.
 *   This function should call [**newLocation**][LocationViewModel.newLocation] or [**editLocation**][LocationViewModel.editLocation]
 *   respective to the purpose of this dialog (to edit an existing tag or create a new one). */
@Composable
private fun LocationEditorDialog(
    modifier: Modifier = Modifier,
    location: Location?,
    viewModel: LocationViewModel,
    onDismiss: () -> Unit,
    onSubmit: (Location) -> Unit
) {
    val maxMenuItems = 10u
    val menuMaxHeight = 150.dp
    val menuShape = MaterialTheme.shapes.medium
    val menuItemPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
    val menuItemModifier = Modifier
        .widthIn(min = 0.dp)
        .padding(menuItemPadding)
        .clip(menuShape)

    var showNearbyDialog by rememberSaveable { mutableStateOf(false) }

    var locationName by rememberSaveable(location) { mutableStateOf(location?.name ?: "") }
    var locationAddress by rememberSaveable(location) { mutableStateOf(location?.address) }
    var nameError by remember(location) { mutableStateOf<Result<Unit>>(Result.Ok(Unit)) }
    var addressError by remember(location) { mutableStateOf<Result<Unit>>(Result.Ok(Unit)) }
    /** Disables the "Submit" button if attempting to submit causes an error. */
    var submitError by remember(location) { mutableStateOf<Result<Unit>>(Result.Ok(Unit)) }

    @Composable
    fun TextField(
        label: String,
        modifier: Modifier = Modifier,
        value: String,
        onValueChange: (String) -> Unit,
        error: Result<Unit>,
        suggestedValues: Result<Set<String>>?,
    ) {
        var shouldShowMenu by remember { mutableStateOf(false) }
        val expanded = shouldShowMenu
                && value.isNotEmpty()
                && suggestedValues?.run { this is Result.Err || isOkAnd { it.isNotEmpty() } }
                ?: true // Show a loading menu if null

        ExposedDropdownMenuBox(
            modifier = modifier,
            expanded = expanded,
            onExpandedChange = { shouldShowMenu = it },
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    .onFocusEvent { if (it.isFocused) shouldShowMenu = true },
                singleLine = true,
                maxLines = 1,
                label = { Text(label) },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                value = value,
                onValueChange = onValueChange,
                isError = error is Result.Err,
                supportingText = if (error is Result.Err) {{
                    Text(error.error.message!!)
                }} else null,
            )

            ExposedDropdownMenu(
                modifier = Modifier
                    .hideDropdownMenuPadding()
                    .heightIn(max = menuMaxHeight),
                expanded = expanded,
                onDismissRequest = { shouldShowMenu = false },
                matchAnchorWidth = false,
                shape = menuShape,
            ) {
                when (suggestedValues) {
                    is Result.Ok -> suggestedValues.value.forEach { suggestedValue ->
                        DropdownMenuItem(
                            modifier = menuItemModifier,
                            text = { Text(suggestedValue) },
                            onClick = {
                                onValueChange(suggestedValue)
                                shouldShowMenu = false
                            }
                        )
                    }
                    is Result.Err -> {
                        val type = suggestedValues.error.javaClass.name
                        val msg = suggestedValues.error.message

                        MenuErrorItem(type = type, message = msg)
                    }
                    null -> MenuLoadingItem()
                }
            }
        }
    }

    /** Return a list of suggested *items* (*[Location]s*) that have a property (defined by the **`map`** argument) that matches the **`query`**.
     *
     * The property will ***not*** be matched against the **`query`** if the value returned by **`map`** is `null`. */
    @Composable
    fun getSuggestedItems(
        query: String,
        map: (DbEntry<Location>) -> String?
    ) = rememberWork(query) {
        // Use Set; only allow a single instance of an item to exist.
        val set = mutableSetOf<String>()
        var start = 0u

        while (set.size < maxMenuItems.toInt()) {
            val page = viewModel.locationsPage(query, start, maxMenuItems)
            if (page.isEmpty()) {
                break
            }
            set.addAll(page
                .mapNotNull(map)
                .filter { it.contains(query, ignoreCase = true) }
            )
            start += maxMenuItems
        }

        set as Set<String>
    }

    @Suppress("AssignedValueIsNeverRead")
    if (showNearbyDialog) {
        NearbyLocationsDialog(
            modifier = modifier,
            viewModel = viewModel,
            mode = NearbyDialogMode.Addresses,
            onDismiss = { showNearbyDialog = false },
        )
    } else {
        ActionDialog(
            modifier = modifier,
            onDismiss = onDismiss,
            title = {
                Text(if (location != null) "Edit location" else "New location",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            actions = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                val canSubmit = {
                    nameError is Result.Ok && addressError is Result.Ok && submitError is Result.Ok
                    // Check that changes have been made if editing an existing location.
                    && (location?.let { it != Location(locationName, locationAddress) } ?: true)
                }
                PlainToolTipBox(if (location != null) "Save changes" else "Submit new location") {
                    FilledTextIconButton(
                        onClick = {
                            val newData = Location(locationName, locationAddress)

                            nameError = viewModel.validateName(locationName)
                            addressError = viewModel.validateAddress(locationName)
                            submitError = viewModel.validateData(newData, ogData = location)

                            if (canSubmit()) {
                                onSubmit(newData)
                                onDismiss()
                            }
                        },
                        enabled = canSubmit(),
                        icon = { Icon(painterResource(R.drawable.check_24px), null) },
                        text = { Text(if (location != null) "Save" else "Submit") },
                    )
                }
            }
        ) {
            val suggestedNames by getSuggestedItems(locationName) { item -> item.data.name }
            val suggestedAddresses by getSuggestedItems(locationAddress ?: "") { item -> item.data.address }

            TextField("Name",
                value = locationName,
                onValueChange = {
                    locationName = it
                    nameError = viewModel.validateName(it)
                    submitError = Result.Ok(Unit)
                },
                error = nameError,
                suggestedValues = suggestedNames,
            )

            Spacer(Modifier.height(ActionDialogPadding.Default.titleSpacerHeight))

            Row(verticalAlignment = Alignment.Top) {
                val spacing = 4.dp
                val unevenPadding = PaddingValues(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)

                TextField("Address (optional)",
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(end = spacing),
                    value = locationAddress ?: "",
                    onValueChange = {
                        locationAddress = it.ifEmpty { null }
                        addressError = viewModel.validateAddress(it)
                        submitError = Result.Ok(Unit)
                    },
                    error = addressError,
                    suggestedValues = suggestedAddresses,
                )

                PlainToolTipBox("Select suggested nearby addresses") {
                    FilledIconButton(
                        onClick = { showNearbyDialog = true },
                        modifier = Modifier.size(TextFieldDefaults.MinHeight),
                        shape = halfRoundedCornerShape(Corner.Left),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryFixed,
                            contentColor = MaterialTheme.colorScheme.onPrimaryFixed,
                        )
                    ) {
                        Icon(painterResource(R.drawable.location_on_24px), null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(unevenPadding)
                        )
                    }
                }
            }

            Spacer(Modifier.height(ActionDialogPadding.Default.titleSpacerHeight))

            submitError.let { submitStatus -> if (submitStatus is Result.Err) {
                Text(submitStatus.error.message!!,
                    color = MaterialTheme.colorScheme.error,
                )
            } }
        }
    }
}

enum class NearbyDialogMode {
    FullLocations, Addresses,
}
@Composable
fun NearbyLocationsDialog(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel,
    mode: NearbyDialogMode = NearbyDialogMode.FullLocations,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(viewModel.selectedLocation) }

    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        title = { Text("Nearby ${when (mode) {
            NearbyDialogMode.FullLocations -> "locations"
            NearbyDialogMode.Addresses -> "addresses"
        }}") },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            PlainToolTipBox("Use selected location") {
                FilledTextIconButton(
                    onClick = {
                        viewModel.selectedLocation = selected
                        onDismiss()
                    },
                    icon = { Icon(painterResource(R.drawable.check_24px), null) },
                    text = { Text("Done") },
                )
            }
        },
    ) {
        // TODO:
        Text("TODO: Not yet implemented")
    }
}

@Preview(showBackground = true)
@Composable
fun LocationFieldPreview() {
    BudgietTheme { Row {
        LocationField(
            viewModel = viewModel<LocationViewModel>().apply {
                useAlternativeLocations(FAKE_LOCATIONS)
            },
            onClickSelect = { },
            onClickNearby = { },
        )
    } }
}

@Preview(showBackground = true)
@Composable
private fun LocationSearchPreview() {
    BudgietTheme {
        LocationSearchDialog(
            viewModel = viewModel<LocationViewModel>().apply {
                useAlternativeLocations(FAKE_LOCATIONS)
            },
            onDismiss = { },
            onSubmit = { },
            onNewClick = { },
            onEditClick = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NewLocationPreview() {
    BudgietTheme {
        LocationEditorDialog(
            location = null,
            viewModel = viewModel<LocationViewModel>().apply {
                useAlternativeLocations(FAKE_LOCATIONS)
            },
            onDismiss = { },
            onSubmit = { _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NearbyLocationsPreview() {
    BudgietTheme {
        NearbyLocationsDialog(
            viewModel = viewModel<LocationViewModel>().apply {
                useAlternativeLocations(FAKE_LOCATIONS)
            },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationPickerEmptyPreview() {
    BudgietTheme {
        LocationSearchDialog(
            viewModel = viewModel<LocationViewModel>(),
            onDismiss = { },
            onSubmit = { },
            onNewClick = { },
            onEditClick = { },
        )
    }
}
