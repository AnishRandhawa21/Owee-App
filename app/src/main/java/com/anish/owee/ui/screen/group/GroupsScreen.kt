package com.anish.owee.ui.screen.group

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anish.owee.viewmodel.GroupViewModel
import com.anish.owee.viewmodel.state.GroupWithMetadata
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GroupsScreen(
    snackbarHostState: SnackbarHostState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCreateGroupClick: () -> Unit = {},
    onGroupClick: (String) -> Unit = {}
) {
    val viewModel: GroupViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    var groupToDelete by remember { mutableStateOf<GroupWithMetadata?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadGroups(isSilent = true)
    }

    LaunchedEffect(uiState.groups.size) {
        if (uiState.groups.isNotEmpty() && listState.firstVisibleItemIndex > 0) {
            kotlinx.coroutines.delay(200)
            listState.animateScrollToItem(0)
        }
    }

    val filteredGroups by remember(uiState.groups, searchQuery) {
        derivedStateOf {
            uiState.groups
                .filter { it.group.name.contains(searchQuery, ignoreCase = true) }
                .sortedByDescending { it.group.createdAt }
        }
    }

    if (showDeleteDialog && groupToDelete != null) {
        val isSettled = uiState.validationAllSettled == true
        
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false 
                viewModel.resetValidation()
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.validationAllSettled == false) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Action Locked", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    } else {
                        Text("Delete Group", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            },
            text = { 
                if (uiState.validationAllSettled == null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else if (!isSettled) {
                    Text(
                        "You cannot delete '${groupToDelete?.group?.name}' yet. All members must be settled up before the group can be removed.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        "Are you sure you want to delete '${groupToDelete?.group?.name}'? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyLarge
                    ) 
                }
            },
            confirmButton = {
                if (uiState.validationAllSettled != null) {
                    if (isSettled) {
                        Button(
                            onClick = {
                                groupToDelete?.let { viewModel.deleteGroup(it.group.id) }
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                groupToDelete?.let { onGroupClick(it.group.id) }
                                showDeleteDialog = false
                                viewModel.resetValidation()
                            },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Settle Group", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false 
                        viewModel.resetValidation()
                    }
                ) {
                    Text(
                        "Cancel", 
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadGroups() },
            modifier = Modifier.fillMaxSize(),
            indicator = { }
        ) {
            // Animated Status Bar Loading
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(tween(400)) + expandVertically(tween(400)),
                exit = fadeOut(tween(400)) + shrinkVertically(tween(400)),
                modifier = Modifier.align(Alignment.TopCenter).zIndex(1f)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Groups",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.5).sp,
                        fontSize = 34.sp
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    placeholder = {
                        Text(
                            text = "Search groups",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    if (uiState.isLoading && uiState.groups.isEmpty()) {
                        items(6) {
                            GroupItemShimmer()
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    } else if (filteredGroups.isEmpty() && !uiState.isLoading) {
                        item {
                            EmptyGroupsState(isSearching = searchQuery.isNotEmpty())
                        }
                    } else {
                        items(
                            items = filteredGroups,
                            key = { it.group.id } 
                        ) { groupMetadata ->
                            var showMenu by remember { mutableStateOf(false) }
                            val isOwner = groupMetadata.group.createdBy == uiState.currentUserId

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        fadeInSpec = tween(800, easing = FastOutSlowInEasing),
                                        fadeOutSpec = tween(500),
                                        placementSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(color = MaterialTheme.colorScheme.primary),
                                        onClick = { onGroupClick(groupMetadata.group.id) },
                                        onLongClick = {
                                            if (isOwner) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showMenu = true
                                            }
                                        }
                                    )
                            ) {
                                with(sharedTransitionScope) {
                                    Box(
                                        modifier = Modifier.sharedBounds(
                                            rememberSharedContentState(key = "group_${groupMetadata.group.id}"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            boundsTransform = { _, _ ->
                                                tween(com.anish.owee.animations.NavAnimations.DURATION, easing = EaseInOutQuart)
                                            },
                                            zIndexInOverlay = 1f,
                                            clipInOverlayDuringTransition = OverlayClip(androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
                                        )
                                    ) {
                                        Column {
                                            GroupItemPremium(groupWithMetadata = groupMetadata)
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 20.dp),
                                                thickness = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)) {
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Delete Group", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                groupToDelete = groupMetadata
                                                showDeleteDialog = true
                                                viewModel.validateGroupDeletion(groupMetadata.group.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        with(sharedTransitionScope) {
            FloatingActionButton(
                onClick = onCreateGroupClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 110.dp)
                    .size(64.dp)
                    .sharedElement(
                        rememberSharedContentState(key = "fab_create_group"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        zIndexInOverlay = 2f
                    ),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun GroupItemShimmer() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(brush)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth(0.5f).height(20.dp).background(brush))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(0.3f).height(14.dp).background(brush))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.width(100.dp).height(24.dp).background(brush))
            Box(modifier = Modifier.width(60.dp).height(24.dp).background(brush))
        }
    }
}

@Composable
fun GroupItemPremium(groupWithMetadata: GroupWithMetadata) {
    val group = groupWithMetadata.group
    val creator = groupWithMetadata.creator
    val members = groupWithMetadata.members

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                if (creator?.photoUrl != null) {
                    AsyncImage(
                        model = creator.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Group,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Created by ${creator?.displayName ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.height(32.dp)) {
                    members.take(3).forEachIndexed { index, member ->
                        Surface(
                            modifier = Modifier
                                .padding(start = (index * 20).dp)
                                .size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.background)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                if (member.photoUrl != null) {
                                    AsyncImage(
                                        model = member.photoUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = member.displayName.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        modifier = Modifier.align(Alignment.Center),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (members.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (members.size > 3) "+${members.size - 3} others" else "${members.size} members",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Settlement Status Badge
            val badgeColor = if (groupWithMetadata.isSettled) 
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            
            val badgeTextColor = if (groupWithMetadata.isSettled)
                MaterialTheme.colorScheme.secondary
            else
                MaterialTheme.colorScheme.primary

            Surface(
                color = badgeColor,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = if (groupWithMetadata.isSettled) "SETTLED" else "ACTIVE",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

@Composable
fun EmptyGroupsState(isSearching: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.extraLarge
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Group,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = if (isSearching) "No groups found" else "Ready to split?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isSearching) "Try searching for something else." else "Create your first group to manage expenses with friends.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
    }
}
