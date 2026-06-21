package com.anish.owee.ui.screen.friend

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PeopleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.anish.owee.navigation.Route
import com.anish.owee.ui.screen.friend.components.FriendCard
import com.anish.owee.ui.screen.friend.components.FriendRequestCard
import com.anish.owee.ui.screen.friend.components.SearchResultCard
import com.anish.owee.ui.theme.TextSecondary
import com.anish.owee.viewmodel.FriendshipViewModel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.input.ImeAction
import com.anish.owee.data.model.Friendship
import com.anish.owee.ui.screen.friend.components.SearchResultStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FriendsScreen(
    navController: NavHostController,
    friendshipViewModel: FriendshipViewModel = viewModel()
) {
    val uiState by friendshipViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    var friendshipToRemove by remember { mutableStateOf<Friendship?>(null) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    if (showRemoveDialog && friendshipToRemove != null) {
        val isSettled = uiState.validationBalance != null && kotlin.math.abs(uiState.validationBalance!!) < 0.01
        val isOwed = (uiState.validationBalance ?: 0.0) > 0.01
        
        val friendName = if (friendshipToRemove?.senderId == uiState.currentUserId) {
            friendshipToRemove?.receiver?.displayName
        } else {
            friendshipToRemove?.sender?.displayName
        } ?: "this friend"

        AlertDialog(
            onDismissRequest = { 
                showRemoveDialog = false
                friendshipViewModel.resetValidation()
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isSettled && uiState.validationBalance != null) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Action Locked", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    } else {
                        Text("Remove Friend", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            },
            text = {
                if (uiState.validationBalance == null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else if (!isSettled) {
                    val statusText = if (isOwed) "owes you" else "you owe"
                    val amount = kotlin.math.abs(uiState.validationBalance!!)
                    Text(
                        "You cannot remove '$friendName' yet. There is an outstanding balance where $friendName $statusText ₹${String.format("%.2f", amount)}. Please settle up first.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        "Are you sure you want to remove '$friendName'? You will no longer see each other's activity.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                if (uiState.validationBalance != null) {
                    if (isSettled) {
                        Button(
                            onClick = {
                                friendshipToRemove?.let { friendshipViewModel.removeFriend(it.id) }
                                showRemoveDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Remove", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                val friend = if (friendshipToRemove?.senderId == uiState.currentUserId) friendshipToRemove?.receiver else friendshipToRemove?.sender
                                if (friend != null) {
                                    navController.navigate("${Route.FriendDetail.route}/${friend.id}")
                                }
                                showRemoveDialog = false
                                friendshipViewModel.resetValidation()
                            },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Settle Up", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showRemoveDialog = false 
                        friendshipViewModel.resetValidation()
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
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { 
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 96.dp) // Lift above floating nav bar
                ) 
            }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { friendshipViewModel.loadData() },
                modifier = Modifier.fillMaxSize().padding(padding),
                indicator = { } // Remove default spinner
            ) {
                // --- Status Bar Loading ---
                AnimatedVisibility(
                    visible = uiState.isLoading || uiState.isSearching,
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
                    // Fixed Header
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Friends",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1.5).sp,
                            fontSize = 34.sp
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Fixed Search Bar
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = friendshipViewModel::updateSearchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        placeholder = {
                            Text(
                                text = "Search by username",
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
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { friendshipViewModel.updateSearchQuery("") }) {
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { friendshipViewModel.searchUsers() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )

                    // Search Results
                    AnimatedVisibility(visible = uiState.hasSearched || uiState.searchResults.isNotEmpty() || uiState.isSearching) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            if (uiState.searchResults.isEmpty() && !uiState.isSearching && uiState.hasSearched) {
                                Text(
                                    text = "No user found with this username",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                uiState.searchResults.forEach { user ->
                                    val status = when {
                                        uiState.friends.any { it.senderId == user.id || it.receiverId == user.id } -> SearchResultStatus.Added
                                        uiState.outgoingRequests.any { it.receiverId == user.id } -> SearchResultStatus.Sent
                                        else -> SearchResultStatus.Add
                                    }

                                    SearchResultCard(
                                        displayName = user.displayName,
                                        username = user.username,
                                        photoUrl = user.photoUrl,
                                        status = status,
                                        onAddClick = {
                                            friendshipViewModel.sendFriendRequest(user.id)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        if (uiState.incomingRequests.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Pending Requests",
                                    count = uiState.incomingRequests.size
                                )
                            }

                            items(
                                items = uiState.incomingRequests,
                                key = { "req_${it.id}" }
                            ) { request ->
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                                    FriendRequestCard(
                                        senderDisplayName = request.sender?.displayName ?: "Unknown",
                                        senderUsername = request.sender?.username ?: "unknown",
                                        senderPhotoUrl = request.sender?.photoUrl,
                                        onAccept = {
                                            friendshipViewModel.acceptFriendRequest(request.id)
                                        },
                                        onReject = {
                                            friendshipViewModel.rejectFriendRequest(request.id)
                                        }
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }

                        item {
                            SectionHeader(
                                title = "Your Friends",
                                count = uiState.friends.size.takeIf { it > 0 }
                            )
                        }

                        if (uiState.isLoading && uiState.friends.isEmpty()) {
                            items(5) {
                                FriendItemShimmer()
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        } else if (uiState.friends.isEmpty()) {
                            item { FriendsEmptyState() }
                        } else {
                            items(
                                items = uiState.friends,
                                key = { it.id }
                            ) { friendship ->
                                val friend = if (friendship.senderId == uiState.currentUserId) {
                                    friendship.receiver
                                } else {
                                    friendship.sender
                                }

                                friend?.let {
                                    var showMenu by remember { mutableStateOf(false) }

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
                                                onClick = {
                                                    navController.navigate("${Route.FriendDetail.route}/${it.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    showMenu = true
                                                }
                                            )
                                    ) {
                                        Column {
                                            FriendCard(friend = it)
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 20.dp),
                                                thickness = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        }

                                        // Menu aligned to the right side
                                        Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)) {
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false },
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                shape = MaterialTheme.shapes.medium
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Remove Friend", color = MaterialTheme.colorScheme.error) },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Delete,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    },
                                                    onClick = {
                                                        showMenu = false
                                                        friendshipToRemove = friendship
                                                        showRemoveDialog = true
                                                        friendshipViewModel.validateFriendRemoval(friendship.id)
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
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int? = null
) {
    Text(
        text = if (count != null) "$title ($count)" else title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        ),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun FriendItemShimmer() {
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(brush)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth(0.4f).height(20.dp).background(brush))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.2f).height(14.dp).background(brush))
        }
    }
}

@Composable
private fun FriendsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                imageVector = Icons.Rounded.PeopleOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "No friends yet",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Search for a username above to add your first friend and start splitting.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
    }
}
