@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.budgiet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgiet.R
import com.example.budgiet.Result
import com.example.budgiet.UserIcons
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.theme.UserColorPalette
import com.example.budgiet.ui.utils.ActionDialog
import com.example.budgiet.ui.utils.ActionDialogPadding
import com.example.budgiet.ui.utils.BorderStyle
import com.example.budgiet.ui.utils.ColorPickerButton
import com.example.budgiet.ui.utils.ColorPickerDialog
import com.example.budgiet.ui.utils.Corner
import com.example.budgiet.ui.utils.FilledTextIconButton
import com.example.budgiet.ui.utils.ItemActionsMenu
import com.example.budgiet.ui.utils.PlainSearchBar
import com.example.budgiet.ui.utils.PlainToolTipBox
import com.example.budgiet.ui.utils.border
import com.example.budgiet.ui.utils.correctContentContrast
import com.example.budgiet.ui.utils.halfRoundedCornerShape
import com.example.budgiet.ui.utils.parentDialogOffset

val TAG_GRID_MAX_HEIGHT = 250.dp
val TAG_GRID_PADDING = PaddingValues(4.dp)
val TAG_FRAME_SPACING = 3.dp
val TAG_SHAPE
    @Composable get() = MaterialTheme.shapes.small
val SELECTED_TAG_BORDER_COLOR
    @Composable get() = MaterialTheme.colorScheme.tertiaryFixedDim

val FAKE_TAGS = listOf(
    Tag("Groceries", "shopping_cart", UserColorPalette.Green),
    Tag("Transportation", "rail_subway_train_transport", UserColorPalette.Blue),
    Tag("Take-out", "fast_food_restaurant", UserColorPalette.Orange),
    Tag("School", "education_school_cap", UserColorPalette.Brown),
    Tag("Trips", "hiking_person", UserColorPalette.Turquoise),
    Tag("Utility", "domain_infrastructure", UserColorPalette.Yellow),
)

data class Tag(
    val name: String,
    val icon: String?,
    val color: Color,
)

class TagsViewModel: ViewModel() {
    /** A fake "database" containing tag data in memory. will be removed once the real database is implemented. */
    // TODO:
    private val fakeTagsDb = mutableStateListOf<Tag>()

    val tagNameCharLimit = 15

    val allTags: List<Tag> = this.fakeTagsDb

    /** Stores the **ID** of the [Tag]s that have been selected by the user.
     *
     * Since [Tag] names are unique, the name itself can be used as the ID. */
    var selectedTags = mutableStateSetOf<String>()

    /** Makes the [ViewModel] ignore the internal database, and instead will hold the [Tag]s data in a [MutableList] in memory.
     *
     * Don't use in production :D */
    internal fun useAlternativeTags(allTags: List<Tag>)
        = this.fakeTagsDb
            .apply { clear() }
            .addAll(allTags)

    fun createNewTag(tag: Tag) {
        this.fakeTagsDb.add(tag)
    }
    fun editTag(name: String, newTag: Tag) {
        this.fakeTagsDb
            .indexOfFirst { it.name == name }
            .also { idx -> if (idx == -1) {
                throw IllegalArgumentException("Attempting to edit non-existent tag with name \"$name\"")
            } }
            .also { idx ->
                this.fakeTagsDb.removeAt(idx)
                this.fakeTagsDb.add(idx, newTag)
            }
        // TODO: also replace from selected tags
    }
    fun deleteTag(tag: Tag) {
        this.fakeTagsDb.remove(tag)
        // TODO: also remove from selected tags
    }

    /** Check if the provided **`name`** can be used for a new Tag.
     * Otherwise, returns an **Error** message. */
    fun validateTagName(name: String, isNewTag: Boolean = true): Result<Unit> {
        // TODO: only allow ascii and dont allow whitespace
        val msg = if (name.isEmpty()) {
            "Tag name must not be empty."
        } else if (name.length > this.tagNameCharLimit) {
            "Tag name must be ${this.tagNameCharLimit} characters or less."
        } else if (isNewTag && this.allTags.find { it.name == name } != null) {
            "A tag with this name already exists."
        } else {
            null
        }

        return msg?.let { Result.Err(Exception(msg)) }
            ?: Result.Ok(Unit)
    }
}

/** Displays the [Tag]s selected by the user to be assigned to the [NewTransaction][NewTransactionForm].
 *
 * @param viewModel Contains the **`selectedTags`** that will be displayed.
 * @param onButtonClick The action to run when the button that opens the [TagsPickerDialog] is clicked. */
@Suppress("UnusedReceiverParameter")
@Composable
fun RowScope.TagsField(
    modifier: Modifier = Modifier,
    viewModel: TagsViewModel,
    onButtonClick: () -> Unit,
) {
    val selectedTags = viewModel.allTags
        .filter { viewModel.selectedTags.contains(it.name) }

    if (selectedTags.isNotEmpty()) {
        val shape = RoundedCornerShape(
            topStart = MaterialTheme.shapes.medium.topStart,
            bottomStart = MaterialTheme.shapes.medium.bottomStart,
            topEnd = MaterialTheme.shapes.extraSmall.topEnd,
            bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd,
        )

        Row(modifier
            .widthIn(max = FIELD_MAX_WIDTH)
            .clip(shape)
            .border(
                width = OutlinedTextFieldDefaults.UnfocusedBorderThickness,
                shape = shape,
                color = MaterialTheme.colorScheme.outline,
            )
            .padding(vertical = TAG_GRID_PADDING.calculateTopPadding())
            .horizontalScroll(rememberScrollState())
            .wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(TAG_FRAME_SPACING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(TAG_GRID_PADDING.calculateStartPadding(LocalLayoutDirection.current) / 2))
            selectedTags.forEach { tag ->
                TagFrame(tag,
                    viewModel = viewModel,
                    onRemove = { viewModel.selectedTags.remove(tag.name) },
                    longPress = true,
                )
            }
            Spacer(Modifier.width(TAG_GRID_PADDING.calculateEndPadding(LocalLayoutDirection.current) / 2))
        }
    }

    PlainToolTipBox("Attach a Tag to this transaction") {
        val tagIcon = @Composable {
            Icon(painterResource(R.drawable.label_24px), "Attach tag")
        }

        if (selectedTags.isEmpty()) {
            FilledTextIconButton(
                onClick = onButtonClick,
                icon = tagIcon,
                text = { Text("Attach tag") },
                colors = ButtonDefaults.filledTonalButtonColors(),
            )
        } else {
            FilledIconButton(
                onClick = onButtonClick,
                shape = halfRoundedCornerShape(Corner.Left),
                colors = IconButtonDefaults.filledTonalIconButtonColors(),
                content = tagIcon,
            )
        }
    }
}

/** Shows a dialog containing a list of *all available [Tag]s*, allowing the user to select from those tags.
 *
 * [Tag]s that were initially selected (defined by **`selectedTags`**) will be highlighted.
 *
 * @param onDismiss The action to run when the [Dialog][ActionDialog] needs to be closed. */
@Composable
fun TagsPickerDialog(
    modifier: Modifier = Modifier,
    viewModel: TagsViewModel,
    onDismiss: () -> Unit,
) {
    val searchState = rememberTextFieldState()

    var showTagCreator by rememberSaveable { mutableStateOf(false) }
    val innerSelectedTags = remember(viewModel.selectedTags) {
        mutableStateSetOf<String>()
            .apply { addAll(viewModel.selectedTags) }
    }

    if (showTagCreator) {
        @Suppress("AssignedValueIsNeverRead")
        TagEditorDialog(
            modifier = modifier,
            tag = null,
            validateNewName = { viewModel.validateTagName(it, isNewTag = true) },
            onSubmit = { viewModel.createNewTag(it) },
            onDismiss = { showTagCreator = false },
        )
    } else {
        ActionDialog(
            modifier = modifier,
            onDismiss = onDismiss,
            title = {
                PlainSearchBar(
                    state = searchState,
                    placeholderText = "Search tags",
                    onQueryChange = { },
                )
            },
            actions = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                PlainToolTipBox("Submit selected tags") {
                    FilledTextIconButton(
                        onClick = {
                            viewModel.selectedTags.addAll(innerSelectedTags)
                            onDismiss()
                        },
                        icon = { Icon(painterResource(R.drawable.check_24px), "Submit") },
                        text = { Text("Done") },
                    )
                }
            },
        ) {
            val modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TextFieldDefaults.MinHeight, max = TAG_GRID_MAX_HEIGHT)
                .border(width = 1.dp, shape = TAG_SHAPE, color = MaterialTheme.colorScheme.outline)

            if (viewModel.allTags.isEmpty()) {
                Column(
                    modifier = modifier.padding(ActionDialogPadding.Default.dialogEdges),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("There are no tags.", textAlign = TextAlign.Center)
                    Text("Press \"New Tag\" to create one.", textAlign = TextAlign.Center)
                }
            } else {
                FlowRow(
                    modifier = modifier
                        .padding(TAG_GRID_PADDING)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(TAG_FRAME_SPACING),
                    verticalArrangement = Arrangement.spacedBy(TAG_FRAME_SPACING),
                ) {
                    viewModel.allTags
                        .filter { it.name.contains(searchState.text, ignoreCase = true) }
                        .forEach { tag ->
                            TagFrame(
                                tag = tag,
                                viewModel = viewModel,
                                isSelected = innerSelectedTags.contains(tag.name),
                                onClick = {
                                    if (innerSelectedTags.contains(tag.name)) {
                                        innerSelectedTags.remove(tag.name)
                                    } else {
                                        innerSelectedTags.add(tag.name)
                                    }
                                },
                                longPress = true,
                            )
                        }
                }
            }

            FilledTextIconButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = ActionDialogPadding.Default.titleSpacerHeight),
                colors = ButtonDefaults.filledTonalButtonColors(),
                icon = { Icon(painterResource(R.drawable.add_24px), "New tag") },
                text = { Text("New tag") },
                onClick = {
                    @Suppress("AssignedValueIsNeverRead")
                    showTagCreator = true
                },
            )
        }
    }
}

/** Shows a [Dialog][ActionDialog] that allows the user to modify the details of a [Tag].
 * This also serves as a **creator dialog** if the **`tag`** argument is `null`.
 *
 * @param onSubmit The action to run when the user clicks the `'Submit'` button.
 *   This provides an argument with the *new* [Tag] data.
 *   This function should call [**createNewTag**][TagsViewModel.createNewTag] or [**editTag**][TagsViewModel.editTag]
 *   respective to the purpose of this dialog (to edit an existing tag or create a new one).
 * @param validateNewName A function that checks if a *new or existing* [Tag]
 *   can use the new **name** that the user is trying to assign to it.
 * @param tag The data of the [Tag] that is being modified.
 *   Pass `null` if the dialog is intended to **create** a *new [Tag]*. */
@Composable
fun TagEditorDialog(
    modifier: Modifier = Modifier,
    tag: Tag?,
    validateNewName: (String) -> Result<Unit>,
    onSubmit: (Tag) -> Unit,
    onDismiss: () -> Unit,
) {
    UserIcons.load(LocalContext.current)
    var icon by rememberSaveable(tag) { mutableStateOf(tag?.icon) }
    var name by rememberSaveable(tag) { mutableStateOf(tag?.name ?: "") }
    var color by remember(tag) { mutableStateOf(tag?.color ?: UserColorPalette.random()) }

    var showIconPickerDialog by remember { mutableStateOf(false) }

    val iconResource = if (icon == null) {
        R.drawable.add_box_24px
    } else {
        UserIcons[icon]
    }?.let { painterResource(it) }
    var nameError by remember(tag) { mutableStateOf<String?>(null) }

    val innerPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    val itemSpacing = 12.dp
    val circleContainerSize = TextFieldDefaults.MinHeight / 1.333f
    val itemBackgroundOpacity = 0.87f

    if (showIconPickerDialog) {
        @Suppress("AssignedValueIsNeverRead")
        IconPickerDialog(
            modifier = modifier,
            initiallySelectedIcon = icon,
            onSubmit = { icon = it },
            onDismiss = { showIconPickerDialog = false },
        )
    } else {
        ActionDialog(
            modifier = modifier.onGloballyPositioned { coords -> parentDialogOffset = coords.positionOnScreen().round() },
            onDismiss = onDismiss,
            title = { if (tag == null) {
                Text("Create new tag")
            } else {
                Text("Edit tag")
            } },
            actions = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                val canSubmit = {
                    nameError == null && iconResource != null
                    // Check that changes have been made if editing an existing tag.
                    && (tag?.let { it != Tag(name, icon, color) } ?: true)
                }
                PlainToolTipBox("Submit new tag") {
                    FilledTextIconButton(
                        onClick = {
                            nameError = validateNewName(name).getErrOrNull()?.message

                            if (canSubmit()) {
                                onSubmit(Tag(name, icon, color))
                                onDismiss()
                            }
                        },
                        enabled = canSubmit(),
                        icon = { Icon(painterResource(R.drawable.check_24px), "Submit") },
                        text = { Text("Submit") },
                    )
                }
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(color)
                    .padding(innerPadding),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                @Composable
                fun Modifier.circleContainer(borderStyle: BorderStyle = BorderStyle.Solid): Modifier
                    = Modifier
                        .clip(IconButtonDefaults.standardShape)
                        .then(this)
                        .border(
                            width = 1.dp,
                            shape = IconButtonDefaults.standardShape,
                            color = MaterialTheme.colorScheme.outline,
                            style = borderStyle,
                        )
                        .size(circleContainerSize)

                @Suppress("AssignedValueIsNeverRead")
                PlainToolTipBox("Select tag icon") {
                    IconButton(
                        modifier = Modifier.semantics { contentDescription = "Select tag icon" }
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh
                                .copy(alpha = itemBackgroundOpacity)
                            )
                            .circleContainer(
                                borderStyle = if (icon == null) BorderStyle.Dashed() else BorderStyle.Solid,
                            ),
                        onClick = { showIconPickerDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    ) {
                        iconResource?.let { Icon(it, null) }
                    }
                }

                TextField(
                    modifier = Modifier.weight(1f, fill = false),
                    colors = run {
                        val color = TextFieldDefaults.colors()
                            .focusedContainerColor
                            .copy(alpha = itemBackgroundOpacity)
                        TextFieldDefaults.colors(
                            focusedContainerColor = color,
                            unfocusedContainerColor = color,
                            errorContainerColor = color,
                            disabledContainerColor = color,
                        )
                    },
                    label = { Text("Tag name") },
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = validateNewName(name).getErrOrNull()?.message
                    },
                )

                ColorPickerButton(
                    modifier = Modifier.circleContainer(),
                    color = color,
                    onColorChange = { color = it },
                )
            }

            if (iconResource == null || nameError != null) {
                Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
            }
            if (iconResource == null) {
                Text("Internal Error: Icon \"$icon\" does not exist.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (nameError != null) {
                Text("Error: $nameError",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Small pill-shaped container that displays a [Tag]'s info.
 * The frame's background is the [Tag]'s [color][Tag.color].
 *
 * Optionally, the frame can have *actions* when interacting with the UI element:
 *  1. **`onClick`**: The user can click anywhere on the frame to trigger this action.
 *       Ideally it should change the **`isSelected`** value.
 *  2. **`onRemove`**: Adds a *clickable* `x` button at the end of the content,
 *       allowing the user to remove the [Tag] from the list of *selected tags*.
 *  3. **`longPress`**: Makes the frame itself *clickable* for a longer duration,
 *       and shows a menu with more actions (i.e. **`onEdit`** and **`onDelete`**). */
@Composable
fun TagFrame(
    tag: Tag,
    modifier: Modifier = Modifier,
    viewModel: TagsViewModel,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    longPress: Boolean = false,
) {
    UserIcons.load(LocalContext.current)
    val padding = PaddingValues(top = 2.dp, bottom = 2.dp, start = 2.dp, end = 4.dp)

    var showActionsMenu by rememberSaveable(longPress) { mutableStateOf(false) }
    var showTagEditor by rememberSaveable(longPress) { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .wrapContentWidth(Alignment.Start)
                .wrapContentHeight()
                .run { if (isSelected) {
                    border(width = 2.dp, shape = TAG_SHAPE, color = MaterialTheme.colorScheme.tertiary)
                    .shadow(3.dp, TAG_SHAPE)
                } else this }
                .clip(TAG_SHAPE)
                .background(color = tag.color, shape = TAG_SHAPE)
                .run { if (longPress || onClick != null) {
                    combinedClickable(
                        onLongClick = if (longPress) {{ showActionsMenu = true }} else null,
                        onClick = onClick ?: { },
                    )
                } else this }
                .then(modifier)
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(
                padding.calculateEndPadding(LocalLayoutDirection.current)
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val contentColor = correctContentContrast(tag.color)

            if (tag.icon != null) {
                when (val icon = UserIcons[tag.icon]) {
                    // Show dash-bordered circle if icon does not exist
                    null -> {
                        Box(Modifier
                            .size(18.dp)
                            .border(
                                width = Dp.Hairline,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = BorderStyle.Dashed(
                                    dashOnLength = 5.dp,
                                    dashOffLength = 1.5.dp,
                                ),
                            )
                        )
                    }
                    else -> Icon(painterResource(icon), null,
                        tint = contentColor,
                    )
                }
            }

            Text(tag.name, color = contentColor)

            if (onRemove != null) {
                val size = with(LocalDensity.current) {
                    LocalTextStyle.current
                        .lineHeight
                        .takeOrElse { 16.sp }
                        .toDp()
                }
                IconButton(modifier = Modifier.size(size), onClick = onRemove) {
                    Icon(painterResource(R.drawable.close_24px), "Remove tag",
                        modifier = Modifier.size(size - 4.dp),
                        tint = contentColor,
                    )
                }
            }
        }

        if (longPress) {
            ItemActionsMenu(
                expanded = showActionsMenu,
                onDismiss = { showActionsMenu = false },
                onEditClick = { showTagEditor = true },
                onDeleteClick = { viewModel.deleteTag(tag) },
            )
        }
    }

    @Suppress("AssignedValueIsNeverRead")
    if (showTagEditor) {
        TagEditorDialog(
            tag = tag,
            validateNewName = { viewModel.validateTagName(it, isNewTag = false) },
            onSubmit = { viewModel.editTag(tag.name, it) },
            onDismiss = { showTagEditor = false },
        )
    }
}

@Composable
fun IconPickerDialog(
    modifier: Modifier = Modifier,
    initiallySelectedIcon: String? = null,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    UserIcons.load(LocalContext.current)
    var selectedIcon by remember { mutableStateOf(initiallySelectedIcon) }
    val searchState = rememberTextFieldState()

    var itemSize by remember(LocalDensity.current) { mutableStateOf<Dp?>(null) }
    val itemPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp)
    /** Padding applied to the entire surface of the grid, aka only visible in the first and last rows. */
    val gridSurfacePadding = 8.dp
    val visibleRows = 5.5f

    ActionDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        title = { Text("Select an icon") },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            PlainToolTipBox("Select icon") {
                FilledTextIconButton(
                    enabled = selectedIcon != null,
                    onClick = { if (selectedIcon != null) {
                        onSubmit(selectedIcon!!)
                        onDismiss()
                    } },
                    icon = { Icon(painterResource(R.drawable.check_24px), "Select") },
                    text = { Text("Select") },
                )
            }
        }
    ) {
        PlainSearchBar(
            modifier = Modifier.padding(bottom = gridSurfacePadding),
            state = searchState,
            onQueryChange = { },
            placeholderText = "Search icons",
        )

        LazyVerticalGrid(
            modifier = Modifier
                .heightIn(max = ((itemSize ?: 0.dp) + itemPadding.calculateTopPadding() + itemPadding.calculateBottomPadding()) * visibleRows + gridSurfacePadding * 2)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            columns = GridCells.FixedSize((itemSize ?: 0.dp)
                    + itemPadding.calculateLeftPadding(LocalLayoutDirection.current)
                    + itemPadding.calculateRightPadding(LocalLayoutDirection.current)),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(vertical = gridSurfacePadding),
        ) {
            this.items(
                items = UserIcons
                    .filter { it.key.contains(searchState.text, ignoreCase = true) }
                    .toList(),
                key = { it.second },
            ) { icon ->
                val density = LocalDensity.current

                PlainToolTipBox(icon.first,
                    modifier = Modifier.padding(itemPadding),
                ) {
                    IconButton(
                        modifier = Modifier.aspectRatio(1f)
                            .run { if (selectedIcon == icon.first) {
                                shadow(5.dp, shape = CircleShape)
                            } else this }
                            .onGloballyPositioned { with(density) {
                                if (itemSize == null) {
                                    itemSize = it.size.width.toDp()
                                }
                            } },
                        colors = IconButtonDefaults.iconButtonColors().let { colors ->
                            if (selectedIcon == icon.first) colors.copy(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                            ) else colors
                        },
                        onClick = { selectedIcon = icon.first },
                    ) {
                        Icon(painterResource(icon.second),
                            tint = if (selectedIcon == icon.first) {
                                MaterialTheme.colorScheme.onTertiary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagsFieldPreview() {
    BudgietTheme { Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagsField(
            viewModel = viewModel<TagsViewModel>(),
            onButtonClick = { },
        )
    } }
}

@Preview(showBackground = true)
@Composable
private fun TagsFieldFilledPreview() {
    BudgietTheme { Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagsField(
            viewModel = viewModel<TagsViewModel>().apply {
                useAlternativeTags(FAKE_TAGS)
                selectedTags.addAll(listOf(FAKE_TAGS[0], FAKE_TAGS[2]).map { it.name })
            },
            onButtonClick = { },
        )
    } }
}

@Preview(showBackground = true)
@Composable
private fun TagsPickerPreview() {
    BudgietTheme {
        TagsPickerDialog(
            viewModel = viewModel<TagsViewModel>().apply {
                useAlternativeTags(FAKE_TAGS)
                selectedTags.addAll(listOf(FAKE_TAGS[0], FAKE_TAGS[2]).map { it.name })
            },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagEditorPreview() {
    BudgietTheme {
        TagEditorDialog(
            tag = null,
            validateNewName = { Result.Ok(Unit) },
            onSubmit = { },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagIconPicker() {
    BudgietTheme {
        IconPickerDialog(
            initiallySelectedIcon = UserIcons.keys.toList()[1],
            onSubmit = { },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagColorPicker() {
    BudgietTheme {
        ColorPickerDialog(
            onSubmit = { },
            onDismiss = { },
        )
    }
}
