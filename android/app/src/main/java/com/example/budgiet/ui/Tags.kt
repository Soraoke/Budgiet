@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.budgiet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.example.budgiet.R
import com.example.budgiet.UserIcons
import com.example.budgiet.ui.theme.BudgietTheme
import com.example.budgiet.ui.theme.UserColorPalette
import com.example.budgiet.ui.utils.ActionDialog
import com.example.budgiet.ui.utils.ActionDialogPadding
import com.example.budgiet.ui.utils.BorderStyle
import com.example.budgiet.ui.utils.ColorPickerButton
import com.example.budgiet.ui.utils.ColorPickerDialog
import com.example.budgiet.ui.utils.FilledTextIconButton
import com.example.budgiet.ui.utils.PlainSearchBar
import com.example.budgiet.ui.utils.PlainToolTipBox
import com.example.budgiet.ui.utils.border
import com.example.budgiet.ui.utils.parentDialogOffset

val TAG_GRID_MAX_HEIGHT = 400.dp
val TAG_SHAPE
    @Composable get() = MaterialTheme.shapes.medium
val SELECTED_TAG_BORDER_COLOR
    @Composable get() = MaterialTheme.colorScheme.tertiaryFixedDim

private val FAKE_TAGS = listOf(
    Tag("Groceries", "shopping_cart", UserColorPalette.Green),
    Tag("Take-out", "fast_food_restaurant", UserColorPalette.Orange),
    Tag("Transportation", "rail_subway_train_transport", UserColorPalette.Blue),
    Tag("School", "education_school_cap", UserColorPalette.Brown),
    Tag("Utility", "domain_infrastructure", UserColorPalette.Yellow),
    Tag("Trips", "hiking_person", UserColorPalette.Turquoise),
)

@Composable
fun TagsPickerDialog(
    modifier: Modifier = Modifier,
    selectedTags: List<Tag>,
    @Suppress("LocalVariableName") _useFakeTags: Boolean = false,
    onSubmit: (List<Tag>) -> Unit,
    onDismiss: () -> Unit,
) {
    val gridPadding = 4.dp
    val searchState = rememberTextFieldState()
    val allTags = if (_useFakeTags) FAKE_TAGS else NewTransactionViewModel.getAllTags()

    var showTagCreator by remember { mutableStateOf(false) }
    val innerSelectedTags = remember { mutableStateSetOf<Tag>() }
    innerSelectedTags.addAll(selectedTags)

    if (showTagCreator) {
        TagCreatorDialog(
            modifier = modifier,
            onSubmit = { NewTransactionViewModel.createNewTag(it) },
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                showTagCreator = false
            },
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
                            onSubmit(innerSelectedTags.toList())
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

            if (allTags.isEmpty()) {
                Box(modifier) {
                    Column(
                        modifier = Modifier.padding(ActionDialogPadding.Default.dialogEdges),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("There are no tags.", textAlign = TextAlign.Center)
                        Text("Press \"New Tag\" to create one.", textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyVerticalGrid(
                    modifier = modifier,
                    contentPadding = PaddingValues(gridPadding),
                    columns = GridCells.Adaptive(minSize = 1.dp),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    this.items(
                        items = allTags,
                        key = { it.name },
                        span = { _ -> GridItemSpan(this.maxLineSpan) },
                    ) { tag ->
                        TagFrame(
                            tag = tag,
                            modifier = Modifier.clickable { innerSelectedTags.add(tag) },
                            isSelected = innerSelectedTags.contains(tag),
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

@Composable
fun TagCreatorDialog(
    modifier: Modifier = Modifier,
    onSubmit: (Tag) -> Unit,
    onDismiss: () -> Unit,
) {
    var icon by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(UserColorPalette.random()) }

    var showIconPickerDialog by remember { mutableStateOf(false) }

    val iconResource = if (icon == null) {
        R.drawable.add_box_24px
    } else {
        UserIcons[icon]
    }?.let { painterResource(it) }
    var nameError by remember { mutableStateOf<String?>(null) }

    fun checkNameError() {
        nameError = if (name.isEmpty()) {
            "Tag name must not be empty."
        } else if (name.length > NewTransactionViewModel.tagNameCharLimit) {
            "Tag name must be 9 characters or less."
        } else {
            null
        }
    }

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
            title = { Text("Create new tag") },
            actions = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                PlainToolTipBox("Submit new tag") {
                    FilledTextIconButton(
                        onClick = {
                            checkNameError()

                            if (nameError == null) {
                                onSubmit(Tag(name, icon, color))
                                onDismiss()
                            }
                        },
                        enabled = nameError == null && iconResource != null,
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
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh
                                    .copy(alpha = itemBackgroundOpacity)
                            )
                            .circleContainer(
                                borderStyle = if (icon == null) BorderStyle.Dashed() else BorderStyle.Solid,
                            ),
                        onClick = { showIconPickerDialog = true }
                    ) {
                        // FIXME: icon does not show when using .circleContainer()
                        iconResource?.let { Icon(it, "Select tag icon") }
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
                        checkNameError()
                    },
                )

                ColorPickerButton(
                    modifier = Modifier.circleContainer(),
                    color = color,
                    onColorChange = { color = it },
                )
            }

            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))

            if (iconResource == null) {
                Text("FATAL Error: Icon \"$icon\" does not exist.",
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

// TODO: doc: onRemove shows an X button if not null
@Composable
fun TagFrame(
    modifier: Modifier = Modifier,
    tag: Tag,
    isSelected: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .background(color = tag.color, shape = TAG_SHAPE)
            .run { if (isSelected) {
                border(width = 2.dp, shape = TAG_SHAPE, color = MaterialTheme.colorScheme.tertiary)
            } else this },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // TODO: show dash-bordered circle if icon does not exist
        tag.icon
            ?.let { UserIcons[it] }
            ?.let { res ->
                Icon(painterResource(res), null)
            }

        // TODO: contrast icon and text color
        Text(tag.name)

        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(painterResource(R.drawable.close_24px), "Remove tag")
            }
        }
    }
}

@Composable
fun IconPickerDialog(
    modifier: Modifier = Modifier,
    initiallySelectedIcon: String? = null,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
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
                items = UserIcons.toList().filter { it.first.contains(searchState.text) },
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
private fun TagsPickerPreview() {
    BudgietTheme {
        TagsPickerDialog(
            selectedTags = listOf(),
            onSubmit = { },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagsPickerWithContentPreview() {
    BudgietTheme {
        TagsPickerDialog(
            selectedTags = listOf(FAKE_TAGS[0], FAKE_TAGS[2]),
            _useFakeTags = true,
            onSubmit = { },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagCreatorPreview() {
    BudgietTheme {
        TagCreatorDialog(
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
