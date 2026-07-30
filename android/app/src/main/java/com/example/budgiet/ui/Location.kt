@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.budgiet.ui

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
import com.example.budgiet.Location
import com.example.budgiet.LocationDbEntry
import com.example.budgiet.R
import com.example.budgiet.Result
import com.example.budgiet.getFakeLocations
import com.example.budgiet.getLocationsPage
import com.example.budgiet.into
import com.example.budgiet.rememberListPager
import com.example.budgiet.rememberQueryListPager
import com.example.budgiet.rememberWork
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.utils.ActionDialog
import com.example.budgiet.ui.utils.ActionDialogPadding
import com.example.budgiet.ui.utils.Corner
import com.example.budgiet.ui.utils.FieldState
import com.example.budgiet.ui.utils.FilledTextIconButton
import com.example.budgiet.ui.utils.ItemActionsMenu
import com.example.budgiet.ui.utils.ListColumn
import com.example.budgiet.ui.utils.ListColumnItemScope
import com.example.budgiet.ui.utils.MenuErrorItem
import com.example.budgiet.ui.utils.MenuLoadingItem
import com.example.budgiet.ui.utils.PlainSearchBar
import com.example.budgiet.ui.utils.PlainToolTipBox
import com.example.budgiet.ui.utils.StringTextFieldState
import com.example.budgiet.ui.utils.TextFieldState
import com.example.budgiet.ui.utils.halfRoundedCornerShape
import com.example.budgiet.ui.utils.hideDropdownMenuPadding
import com.example.budgiet.useFakeDb
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

/** Displays the [Location] selected by the user to be assigned to the [NewTransaction][NewTransactionForm].
 *
 * This displays 2 buttons:
 *  1. Displays the *currently selected [Location]* and opens the [LocationPickerDialog].
 *  2. To search from [NearbyLocationsDialog].
 *
 * @param onClickSelect The action that runs when the *"Select location"* button is *clicked*.
 *   This action should open the [LocationPickerDialog].
 * @param onClickNearby The action that runs when the *"Nearby locations"* button is *clicked*.
 *   This action should open the [NearbyLocationsDialog]. */
@Composable
fun RowScope.LocationField(
    selectedLocation: Location?,
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
                    stateDescription = selectedLocation?.toString() ?: "None selected"
                },
            onClick = onClickSelect,
            // Modify the shape on the left-side of the button to connect with the auto-select location button.
            shape = halfRoundedCornerShape(Corner.Right),
        ) {
            Text(selectedLocation?.toString() ?: "Select Location")
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
    class Edit(val location: LocationDbEntry): LocationPickerState()
}
/** Shows a [Dialog][ActionDialog] that allows the user to select a [Location].
 * Shows different content depending on the [LocationPickerState]. */
@Composable
fun LocationPickerDialog(
    modifier: Modifier = Modifier,
    state: LocationPickerState,
    onStateChange: (LocationPickerState) -> Unit,
    selectedLocation: LocationDbEntry?,
    onSelectLocation: (LocationDbEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var newLocationId by remember { mutableStateOf<ULong?>(null) }

    when (state) {
        LocationPickerState.Search -> {
            LocationSearchDialog(
                modifier = modifier,
                selectedLocation = selectedLocation,
                onDismiss = onDismiss,
                onSubmit = onSelectLocation,
                onNewClick = { onStateChange(LocationPickerState.New) },
                onEditClick = { onStateChange(LocationPickerState.Edit(it)) },
                newLocationId = newLocationId,
            )
        }
        LocationPickerState.Nearby -> {
            NearbyLocationsDialog(
                modifier = modifier,
                selectedLocation = selectedLocation,
                onSelectLocation = onSelectLocation,
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
                editLocation = editLocation?.data,
                onDismiss = { onStateChange(LocationPickerState.Search) },
                onSubmit = { newLocationId = editLocation?.let { editLoc ->
                    editLoc.edit(it)
                    editLoc.id
                } ?: run {
                    LocationDbEntry.insertNew(it).id
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
 *   This action should set the [selectedLocation].
 * @param newLocationId When a new [Location] item was added to the list of locations,
 *   that item (by ID) in the list of *Recents* will have an *animated scrim* to indicate that it was just added.
 * @param onNewClick The action to run when the `"New Location"` button is clicked.
 *   This should show the [LocationEditorDialog] for creating a *new* [Location].
 * @param onEditClick The action to run when the `"Edit"` button is clicked from a Location's [ItemActionsMenu].
 *   Like [onNewClick], this should show the [LocationEditorDialog] for editing the [Location] in the argument. */
@Composable
private fun LocationSearchDialog(
    modifier: Modifier = Modifier,
    selectedLocation: LocationDbEntry?,
    newLocationId: ULong? = null,
    onSubmit: (LocationDbEntry) -> Unit,
    onDismiss: () -> Unit,
    onNewClick: () -> Unit,
    onEditClick: (LocationDbEntry) -> Unit,
) {
    val searchColumnSize = 3.5f
    val newItemAnimationSpeed = 250.milliseconds // In millis
    val newItemAnimationDuration = newItemAnimationSpeed * 5 // Repeat n times; In millis
    val scrimColor = MaterialTheme.colorScheme.secondaryContainer
    val pageSize = ceil(searchColumnSize).toUInt() * 3u
    val searchState = rememberTextFieldState()
    /** Animation is reset when the newLocation value is modified. */
    var animateNewItemScrim by remember(newLocationId) { mutableStateOf(newLocationId != null) }

    val searchPager = rememberQueryListPager(
        queryState = searchState,
        getPage = { query, start, len -> getLocationsPage(query.toString(), start, len) },
        // Page size should have enough items to scroll down several times the number of items showed.
        pageSize = pageSize,
    )
    val recentsPager = rememberListPager(
        getPage = { start, len -> getLocationsPage(null, start, len) },
        pageSize = pageSize,
    )

    fun close() {
        onDismiss()
        // Cancel pending page loading jobs.
        searchState.clearText()
        searchPager.refresh()
    }

    @Composable
    fun ListColumnItemScope.LocationItem(item: LocationDbEntry, query: CharSequence? = null, isNew: Boolean = false) {
        val defaultContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
        val newItemAnimatedColor = if (isNew && animateNewItemScrim) {
            LaunchedEffect(Unit) {
                delay(newItemAnimationDuration)
                animateNewItemScrim = false
            }
            rememberInfiniteTransition()
                .animateColor(
                    initialValue = defaultContainerColor,
                    targetValue = scrimColor,
                    animationSpec = infiniteRepeatable(
                        animation = tween(newItemAnimationSpeed.toInt(DurationUnit.MILLISECONDS)),
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
                            item.markUsed()
                            close()
                        },
                        onLongClick = { showActionsMenu = true },
                    ),
                colors = ListItemDefaults.colors(
                    // Show a scrim
                    containerColor = if (showActionsMenu) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else if (selectedLocation?.let { it.id == item.id } ?: false) {
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
                    item.delete()
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
 * @param editLocation The data of the [Location] that is being modified.
 * @param onSubmit The action that runs when the user clicks the `"Submit"` button and all the data has been validated.
 *   This provides an argument with the *new* [Location] data.
 *   This function should call [LocationDbEntry.insertNew] or [LocationDbEntry.edit]
 *   respective to the purpose of this dialog (to edit an existing tag or create a new one). */
@Composable
private fun LocationEditorDialog(
    modifier: Modifier = Modifier,
    editLocation: Location?,
    onSubmit: (Location) -> Unit,
    onDismiss: () -> Unit,
) {
    val maxMenuItems = 10.toULong()
    val menuMaxHeight = 150.dp
    val menuShape = MaterialTheme.shapes.medium
    val menuItemPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
    val menuItemModifier = Modifier
        .widthIn(min = 0.dp)
        .padding(menuItemPadding)
        .clip(menuShape)

    val isNew = editLocation == null
    var showNearbyDialog by rememberSaveable { mutableStateOf(false) }

    val editLocationState = remember(editLocation) { object {
        val name = StringTextFieldState(
            initialValue = editLocation?.name ?: "",
            validator = { runCatching { Location.validateName(it, isNew) }.into() },
        )
        val address = TextFieldState<String?>(
            initialTextValue = editLocation?.address ?: "",
            parser = { s -> if (s.isEmpty()) { Result.Ok(null) } else {
                runCatching { Location.validateAddress(s, isNew) }.into()
                    .map { s }
            } },
        )
    } }
    /** Disables the "Submit" button if attempting to submit causes an error. */
    var submitError by remember(editLocation) { mutableStateOf<Result<Unit>>(Result.Ok(Unit)) }

    @Composable
    fun TextField(
        label: String,
        modifier: Modifier = Modifier,
        fieldState: FieldState<*>,
        suggestedValues: Result<Set<String>>?,
    ) {
        var shouldShowMenu by remember { mutableStateOf(false) }
        val expanded = shouldShowMenu
            && fieldState.fieldText.isNotEmpty()
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
                value = fieldState.fieldText,
                onValueChange = { fieldState.fieldText = it },
                isError = fieldState.parseResult is Result.Err,
                supportingText = fieldState.textFieldSupportingText(),
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
                                fieldState.fieldText = suggestedValue
                                fieldState.doValidate()
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
        map: (LocationDbEntry) -> String?
    ) = rememberWork(query) {
        // Use Set; only allow a single instance of an item to exist.
        val set = mutableSetOf<String>()
        var start = 0.toULong()

        while (set.size < maxMenuItems.toInt()) {
            val page = getLocationsPage(query, start, maxMenuItems)
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

    if (showNearbyDialog) {
        NearbyAddressesDialog(
            modifier = modifier,
            onSubmit = {
                editLocationState.address.fieldText = it
                editLocationState.address.doValidate()
            },
            onDismiss = { showNearbyDialog = false },
        )
    } else {
        ActionDialog(
            modifier = modifier,
            onDismiss = onDismiss,
            title = {
                Text(if (editLocation != null) "Edit location" else "New location",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            actions = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                val canSubmit = {
                    editLocationState.name.parseResult is Result.Ok
                    && editLocationState.address.parseResult is Result.Ok
                    && submitError is Result.Ok
                    // Check that changes have been made if editing an existing location.
                    && (editLocation?.let { it != Location(editLocationState.name.parseResult.unwrap(), editLocationState.address.parseResult.unwrap()) } ?: true)
                }
                PlainToolTipBox(if (isNew) "Submit new location" else "Save changes") {
                    FilledTextIconButton(
                        onClick = {
                            editLocationState.name.doValidate()
                            editLocationState.address.doValidate()

                            val newData = Location(editLocationState.name.fieldText, editLocationState.address.fieldText)
                            submitError = runCatching { Location.validate(newData, oldData = editLocation) }.into()

                            if (canSubmit()) {
                                onSubmit(newData)
                                onDismiss()
                            }
                        },
                        enabled = canSubmit(),
                        icon = { Icon(painterResource(R.drawable.check_24px), null) },
                        text = { Text(if (editLocation != null) "Save" else "Submit") },
                    )
                }
            }
        ) {
            val suggestedNames by getSuggestedItems(editLocationState.name.fieldText) { item -> item.data.name }
            val suggestedAddresses by getSuggestedItems(editLocationState.address.fieldText) { item -> item.data.address }

            TextField("Name",
                fieldState = editLocationState.name,
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
                    fieldState = editLocationState.address,
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

// TODO: doc
@Composable
fun NearbyLocationsDialog(
    modifier: Modifier = Modifier,
    selectedLocation: LocationDbEntry?,
    onSelectLocation: (LocationDbEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(selectedLocation) }

    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        title = { Text("Nearby locations") },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            PlainToolTipBox("Use selected location") {
                FilledTextIconButton(
                    enabled = selected != null,
                    onClick = { selected?.let {
                        onSelectLocation(it)
                        onDismiss()
                    } },
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

// TODO: doc
@Composable
private fun NearbyAddressesDialog(
    modifier: Modifier = Modifier,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf<String?>(null) }

    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        title = { Text("Nearby addresses") },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            PlainToolTipBox("Use selected address") {
                FilledTextIconButton(
                    enabled = selected != null,
                    onClick = { selected?.let {
                        onSubmit(it)
                        onDismiss()
                    } },
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

private val FAKE_LOCATIONS = getFakeLocations()
/** Setup fake locations for Previews.
 * Returns the [Database Entry][LocationDbEntry] for the passed in [Location] data (if any) */
@Composable
private fun useFakeLocations(selectedLocation: Location? = null): LocationDbEntry? = remember {
    useFakeDb()
    LocationDbEntry.clearAll()
    val entries = mutableListOf<LocationDbEntry>()

    for (data in FAKE_LOCATIONS) {
        entries.add(LocationDbEntry.insertNew(data))
    }
    selectedLocation?.let { location ->
        when (val found = entries.find { it.data.name == location.name && it.data.address == location.address }) {
            null -> throw Exception("Could not find location $location")
            else -> found
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocationFieldPreview() {
    useFakeLocations()
    BudgietTheme { Row {
        LocationField(
            selectedLocation = null,
            onClickSelect = { },
            onClickNearby = { },
        )
    } }
}

@Preview(showBackground = true)
@Composable
private fun LocationSearchPreview() {
    useFakeLocations()
    BudgietTheme {
        LocationSearchDialog(
            selectedLocation = null,
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
    useFakeLocations()
    BudgietTheme {
        LocationEditorDialog(
            editLocation = null,
            onDismiss = { },
            onSubmit = { _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NearbyLocationsPreview() {
    useFakeLocations()
    BudgietTheme {
        NearbyLocationsDialog(
            selectedLocation = null,
            onSelectLocation = { },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationPickerEmptyPreview() {
    useFakeLocations()
    BudgietTheme {
        LocationSearchDialog(
            selectedLocation = null,
            onDismiss = { },
            onSubmit = { },
            onNewClick = { },
            onEditClick = { },
        )
    }
}
