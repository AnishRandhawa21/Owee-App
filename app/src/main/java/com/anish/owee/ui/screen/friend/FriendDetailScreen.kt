package com.anish.owee.ui.screen.friend

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.data.model.User
import com.anish.owee.viewmodel.state.BalanceState
import com.anish.owee.ui.screen.friend.components.FriendAvatar
import com.anish.owee.viewmodel.state.color
import com.anish.owee.viewmodel.state.sentenceLabel
import com.anish.owee.ui.theme.*
import com.anish.owee.viewmodel.FriendRequestViewModel
import com.anish.owee.viewmodel.FriendshipViewModel
import com.anish.owee.ui.screen.friend.components.FriendRequestActivityCard
import com.anish.owee.ui.screen.friend.components.SettlementBottomSheet
import com.anish.owee.viewmodel.state.toBalanceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FriendDetailScreen(
    friendId: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit = {},
    onRequestMoneyClick: (String, String) -> Unit,
    onSettlementClick: (String, String, Double) -> Unit,
    onViewHistoryClick: (String, String) -> Unit = { _, _ -> },
    friendshipViewModel: FriendshipViewModel = viewModel(),
    friendRequestViewModel: FriendRequestViewModel = viewModel()
) {
    val uiState by friendshipViewModel.uiState.collectAsStateWithLifecycle()
    val requestUiState by friendRequestViewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(friendId) {
        friendRequestViewModel.loadCachedFriendData(friendId)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                friendRequestViewModel.loadRequests(friendId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val friendship = uiState.friends.firstOrNull { 
        it.sender?.id == friendId || it.receiver?.id == friendId 
    }
    
    val friend: User? = friendship?.let { 
        if (it.sender?.id == friendId) it.sender else it.receiver 
    }

    var showSettlementSheet by remember { mutableStateOf(false) }

    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    rememberSharedContentState(key = "friend_$friendId"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    zIndexInOverlay = 1f,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(0.dp)),
                    boundsTransform = { _, _ ->
                        tween(com.anish.owee.animations.NavAnimations.DURATION, easing = EaseInOutQuart)
                    }
                )
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- Status Bar Loading ---
            AnimatedVisibility(
                visible = uiState.isLoading || requestUiState.isLoading,
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

            if (friend == null && !uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Friend not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (friend != null) {
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading || requestUiState.isLoading,
                    onRefresh = { friendRequestViewModel.loadRequests(friendId) },
                    modifier = Modifier.fillMaxSize(),
                    indicator = { } // Remove default spinner
                ) {
                    val firstName = friend.displayName.trim().split(" ").firstOrNull() ?: friend.displayName
                    val balance = requestUiState.balance.toBalanceState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        // Fixed Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 8.dp, end = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = "Friend Details",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 40.dp)
                        ) {
                            // Profile header
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FriendAvatar(
                                        photoUrl = friend.photoUrl,
                                        displayName = friend.displayName,
                                        size = 88.dp
                                    )
                                    Text(
                                        text = friend.displayName,
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                    Text(
                                        text = "@${friend.username}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Balance summary card
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    shape = MaterialTheme.shapes.large,
                                    color = balance.color().copy(alpha = 0.1f)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (balance is BalanceState.SettledUp) "All Caught Up" 
                                                   else if (balance is BalanceState.YouOwe) "You owe" 
                                                   else "Owes you",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = balance.color().copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = balance.sentenceLabel(firstName),
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = (-1).sp
                                            ),
                                            color = balance.color(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Primary actions
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { onRequestMoneyClick(friend.id, friend.displayName) },
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        shape = MaterialTheme.shapes.medium,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                    ) {
                                        Text("Request", fontWeight = FontWeight.Bold)
                                    }

                                    if (requestUiState.balance < -0.01) {
                                        OutlinedButton(
                                            onClick = { showSettlementSheet = true },
                                            modifier = Modifier.weight(1f).height(52.dp),
                                            shape = MaterialTheme.shapes.medium,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Text("Settle Up", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }

                            // Recent activity
                            item {
                                Text(
                                    text = "Recent Activity",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 20.dp, top = 32.dp, bottom = 8.dp)
                                )
                            }

                            if (requestUiState.activities.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                        RecentActivityEmptyState()
                                    }
                                }
                            } else {
                                items(
                                    items = requestUiState.activities.take(5),
                                    key = { it.id }
                                ) { activity ->
                                    Box(modifier = Modifier.animateItem()) {
                                        Column {
                                            FriendRequestActivityCard(
                                                title = activity.title,
                                                note = activity.note,
                                                amount = activity.amount,
                                                status = if (activity.type == "settlement") "paid" else activity.status
                                            )
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 20.dp),
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                                
                                item {
                                    TextButton(
                                        onClick = { onViewHistoryClick(friend.id, friend.displayName) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        Text("View Full History", fontWeight = FontWeight.Bold, color = TextSecondary)
                                    }
                                }
                            }

                            // Branding Footer
                            item {
                                Spacer(modifier = Modifier.height(40.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, bottom = 24.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "OWEE",
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 2.sp,
                                            fontSize = 64.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                    )
                                    Text(
                                        text = "SPLIT SMART • LIVE BETTER",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                        modifier = Modifier.offset(y = (-10).dp)
                                    )
                                }
                            }
                        }
                    }

                    if (showSettlementSheet && friendship != null && friend != null) {
                        SettlementBottomSheet(
                            balance = requestUiState.balance,
                            requestedByMe = requestUiState.requestedByMe,
                            requestedByFriend = requestUiState.requestedByFriend,
                            friendName = friend.displayName,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onDismiss = { showSettlementSheet = false },
                            onSettleNow = { amount ->
                                onSettlementClick(
                                    friendship.id,
                                    friend.id,
                                    amount
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentActivityEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = TextSecondary.copy(alpha = 0.5f)
            )
            Text(
                text = "No activity yet",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Expenses you add together will show up here",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
