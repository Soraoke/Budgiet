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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgiet.R
import com.example.budgiet.Result
import com.example.budgiet.into
import com.example.budgiet.rememberListPager
import com.example.budgiet.rememberQueryListPager
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.ActionDialog
import com.example.budgiet.ui.utils.ActionDialogPadding
import com.example.budgiet.ui.utils.Corner
import com.example.budgiet.ui.utils.FilledTextIconButton
import com.example.budgiet.ui.utils.ListColumn
import com.example.budgiet.ui.utils.ListColumnItemScope
import com.example.budgiet.ui.utils.PlainSearchBar
import com.example.budgiet.ui.utils.PlainToolTipBox
import com.example.budgiet.ui.utils.halfRoundedCornerShape
import com.example.budgiet.ui.utils.hideDropdownMenuPadding
import kotlinx.coroutines.delay
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
    val address: String,
)

data class DbEntry<T>(
    val id: UInt,
    val data: T,
)

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
                    it.data.name.contains(query)
                            || it.data.address.contains(query)
                }
            } else this }

        return list.subList(start.toInt(), min((start + len).toInt(), list.size))

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
        var id = 0u
        while (this.fakeDb.keys.contains(id)) {
            id++
        }

        this.fakeDb[id] = data
        return DbEntry(id, data)

        // TODO: real impl
    }

    fun editLocation(id: UInt, newData: Location) {
        // TODO: update locations loaded in pager
        TODO()
        // TODO: real impl
    }

    fun deleteLocation(id: UInt) {
        this.fakeDb.remove(id)
        // TODO: real impl
    }

    fun validateName(name: String, isNewLocation: Boolean = true): Result<Unit> {
        val msg = if (name.isEmpty()) {
            "Name must not be empty"
        } else {
            null
        }

        return msg?.let { Result.Err(Exception(msg)) }
            ?: Result.Ok(Unit)
    }
    fun validateAddress(address: String, isNewLocation: Boolean = true): Result<Unit> {
        val msg = if (address.isEmpty()) {
            "Address must not be empty"
        } else {
            null
        }

        return msg?.let { Result.Err(Exception(msg)) }
            ?: Result.Ok(Unit)
    }
}

/** Displays the [Location] selected by the user to be assigned to the [NewTransaction][NewTransactionForm].
 *
 * This displays 2 buttons:
 *  1. Displays the *currently selected [Location]* and opens the [LocationPickerDialog].
 *  2. To search from [nearbyLocations][LocationViewModel.nearbyLocations].
 *
 * @param onClick The action that runs when the first button is *clicked*.
 *   This action should open the [LocationPickerDialog]. */
@Suppress("UnusedReceiverParameter")
@Composable
fun RowScope.LocationField(
    viewModel: LocationViewModel,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = Modifier.semantics {
            contentDescription = "Select location"
            stateDescription = viewModel.selectedLocation?.data?.name ?: "None selected"
        },
        onClick = onClick,
        // Modify the shape on the left-side of the button to connect with the auto-select location button.
        shape = halfRoundedCornerShape(Corner.Right),
    ) {
        Text(
            if (viewModel.selectedLocation != null) {
                viewModel.selectedLocation!!.data.name
            } else {
                "Select Location"
            }
        )
    }
    PlainToolTipBox("Auto-select Location") {
        FilledIconButton(
            onClick = { TODO() },
            shape = halfRoundedCornerShape(Corner.Left)
        ) {
            Icon(painterResource(R.drawable.location_on_24px), null)
        }
    }
}

@Composable
fun LocationPickerDialog(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel,
    onDismiss: () -> Unit,
) {
    var showNewLocationDialog by remember { mutableStateOf(false) }
    var newLocationAdded by remember { mutableStateOf(false) }

    @Suppress("AssignedValueIsNeverRead")
    if (showNewLocationDialog) {
        NewLocationDialog(
            modifier = modifier,
            viewModel = viewModel,
            onDismiss = { showNewLocationDialog = false },
            onSubmit = { newLocationAdded = true },
        )
    } else {
        LocationSearchDialog(
            modifier = modifier,
            viewModel = viewModel,
            onDismiss = onDismiss,
            onSubmit = { viewModel.selectedLocation = it },
            onNewClick = { showNewLocationDialog = true },
            newLocationAdded = newLocationAdded,
        )
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
 * @param newLocationAdded When a new [Location] item was added to the list of locations,
 *   The first item in the list of *Recents* will have an *animated scrim* to indicate that that item was just added.
 * @param onNewClick The action to run when the `"New Location"` button is clicked.  */
@Composable
private fun LocationSearchDialog(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel,
    newLocationAdded: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (DbEntry<Location>) -> Unit,
    onNewClick: () -> Unit,
) {
    val searchColumnSize = 3.5f
    val scrimAnimationSpeed = 250 // In millis
    val scrimAnimationDuration = scrimAnimationSpeed * 5 // Repeat n times; In millis
    val scrimColor = MaterialTheme.colorScheme.secondaryContainer
    val pageSize = ceil(searchColumnSize).toUInt() * 3u
    val searchState = rememberTextFieldState()

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
            @Composable
            fun ListColumnItemScope.LocationItem(item: DbEntry<Location>, animateScrim: Boolean = false) {
                var runAnimation by remember { mutableStateOf(animateScrim) }
                val containerColor = if (runAnimation) {
                    LaunchedEffect(Unit) {
                        delay(scrimAnimationDuration.toLong())
                        runAnimation = false
                    }
                    rememberInfiniteTransition()
                        .animateColor(
                            initialValue = ListItemDefaults.containerColor,
                            targetValue = scrimColor,
                            animationSpec = infiniteRepeatable(
                                animation = tween(scrimAnimationSpeed),
                                repeatMode = RepeatMode.Reverse,
                            ),
                        )
                        .value
                } else {
                    ListItemDefaults.containerColor
                }

                this.DataItem(
                    modifier = Modifier
                        .semantics {
                            contentDescription = "${item.data.name} at address ${item.data.address}"
                        }
                        .clickable(onClick = {
                            onSubmit(item)
                            close()
                        }),
                    colors = ListItemDefaults.colors(
                        containerColor = containerColor,
                    ),
                    headlineContent = { Text(item.data.name) },
                    supportingContent = { Text(item.data.address) },
                )
            }

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
                            this.pagedItemsIndexed(
                                pager = recentsPager,
                                itemKey = { _, item -> item.id.toInt() }, // Why can't use UInt ....
                            ) { idx, item ->
                                if (idx == 0) {
                                    LocationItem(item, newLocationAdded)
                                } else {
                                    LocationItem(item)
                                }
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
                            ) { item -> this.LocationItem(item) }
                        }
                    }
                }
            }
        }
    }
}

/** Display a [Dialog][ActionDialog] that prompts the user for information of a [Location] they want to create.
 *
 * @param onSubmit The action that runs when the user clicks the `"Submit"` button and the [Location] is created.
 *   Note that this composable creates the [Location] automatically with the [viewModel][LocationViewModel],
 *   so the function should not try to add the location itself. */
@Composable
private fun NewLocationDialog(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel,
    onDismiss: () -> Unit,
    onSubmit: (DbEntry<Location>) -> Unit
) {
    val menuShape = MaterialTheme.shapes.medium
    val menuItemPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
    val maxMenuItems = 10u
    val menuItemModifier = Modifier
        .widthIn(min = 0.dp)
        .padding(menuItemPadding)
        .clip(menuShape)

    var locationName by remember { mutableStateOf("") }
    var locationAddress by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<Result<Unit>>(Result.Ok(Unit)) }
    var addressError by remember { mutableStateOf<Result<Unit>>(Result.Ok(Unit)) }

    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        title = {
            Text("New location",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            PlainToolTipBox("Submit new location") {
                FilledTextIconButton(
                    onClick = {
                        nameError = viewModel.validateName(locationName)
                        addressError = viewModel.validateAddress(locationName)

                        if (nameError is Result.Ok && addressError is Result.Ok) {
                            onSubmit(viewModel.newLocation(Location(locationName, locationAddress)))
                            onDismiss()
                        }
                    },
                    icon = { Icon(painterResource(R.drawable.check_24px), null) },
                    text = { Text("Submit") },
                )
            }
        }
    ) {
        @Composable
        fun Field(
            modifier: Modifier = Modifier,
            label: String,
            value: String,
            onValueChange: (String) -> Unit,
            error: Result<Unit>,
        ) {
            TextField(
                modifier = modifier.fillMaxWidth(),
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
        }

        val suggestedNames = remember(locationName) { runCatching {
            viewModel.locationsPage(locationName, 0u, maxMenuItems)
                .map { it.data.name }
                // Only allow a single instance of a name to exist.
                // Due to the list becoming a set, some items wil be culled,
                // so the size won't necessarily be the same as maxMenuItems.
                .toSet()
        }.into() }

        var shouldShowMenu by remember { mutableStateOf(false) }
        val expanded = shouldShowMenu
            && locationName.isNotEmpty()
            && suggestedNames.isOkAnd { it.isNotEmpty() }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { shouldShowMenu = it },
        ) {
            Field(
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    .onFocusEvent { if (it.isFocused) shouldShowMenu = true },
                label = "Name",
                value = locationName,
                onValueChange = {
                    locationName = it
                    nameError = viewModel.validateName(it)
                },
                error = nameError,
            )
            ExposedDropdownMenu(
                modifier = Modifier
                    .hideDropdownMenuPadding()
                    .heightIn(max = 150.dp),
                expanded = expanded,
                onDismissRequest = { shouldShowMenu = false },
                matchAnchorWidth = false,
                shape = menuShape,
            ) {
                // This is only expanded if suggestedNames is Ok, so it is safe to unwrap.
                suggestedNames.unwrap().forEach { name ->
                    DropdownMenuItem(
                        modifier = menuItemModifier,
                        text = { Text(name) },
                        onClick = {
                            locationName = name
                            shouldShowMenu = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(ActionDialogPadding.Default.titleSpacerHeight))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Field(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(end = 8.dp),
                label = "Address",
                value = locationAddress,
                onValueChange = {
                    locationAddress = it
                    addressError = viewModel.validateAddress(it)
                },
                error = addressError,
            )
            PlainToolTipBox("Get address from GPS coordinates") {
                FilledIconButton(
                    onClick = { TODO() },
                    modifier = Modifier.size(TextFieldDefaults.MinHeight - 4.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryFixed,
                        contentColor = MaterialTheme.colorScheme.onPrimaryFixed,
                    )
                ) {
                    Icon(painterResource(R.drawable.location_on_24px),
                        null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }
            }
        }
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
            onClick = { },
        )
    } }
}

@Preview(showBackground = true)
@Composable
private fun LocationPickerEmptyPreview() {
    BudgietTheme {
        LocationSearchDialog(
            viewModel = viewModel<LocationViewModel>(),
            onDismiss = { },
            onSubmit = { },
            onNewClick = { }
        )
    }
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
            onNewClick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NewLocationPreview() {
    BudgietTheme {
        NewLocationDialog(
            viewModel = viewModel<LocationViewModel>().apply {
                useAlternativeLocations(FAKE_LOCATIONS)
            },
            onDismiss = { },
            onSubmit = { _ -> }
        )
    }
}
