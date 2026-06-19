package com.anish.owee.ui.screen.friend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PeopleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.anish.owee.data.model.Friendship
import com.anish.owee.navigation.Route
import com.anish.owee.ui.screen.friend.components.FriendCard
import com.anish.owee.ui.screen.friend.components.FriendRequestCard
import com.anish.owee.ui.screen.friend.components.FriendSearchBar
import com.anish.owee.ui.screen.friend.components.SearchResultCard
import com.anish.owee.ui.screen.friend.components.rememberMockBalance
import com.anish.owee.ui.theme.TextSecondary
import com.anish.owee.viewmodel.FriendshipViewModel

@Composable
fun FriendsScreen(
    navController: NavHostController,
    friendshipViewModel: FriendshipViewModel = viewModel()
) {

    val uiState by friendshipViewModel
        .uiState
        .collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        item {
            Text(
                text = "Friends",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                FriendSearchBar(
                    value = uiState.searchQuery,
                    onValueChange = friendshipViewModel::updateSearchQuery,
                    onSearch = friendshipViewModel::searchUsers
                )

                if (uiState.searchResults.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        uiState.searchResults.forEach { user ->
                            SearchResultCard(
                                displayName = user.displayName,
                                username = user.username,
                                photoUrl = user.photoUrl,
                                onAddClick = {
                                    friendshipViewModel.sendFriendRequest(user.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.incomingRequests.isNotEmpty()) {

            item {
                SectionHeader(
                    title = "Pending Requests",
                    count = uiState.incomingRequests.size
                )
            }

            items(
                items = uiState.incomingRequests,
                key = { it.id }
            ) { request ->
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

        item {
            SectionHeader(
                title = "Your Friends",
                count = uiState.friends.size.takeIf { it > 0 }
            )
        }

        when {

            uiState.isLoading && uiState.friends.isEmpty() -> {
                item { FriendsLoadingState() }
            }

            uiState.friends.isEmpty() -> {
                item { FriendsEmptyState() }
            }

            else -> {
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
                        FriendCard(
                            friend = it,
                            balance = rememberMockBalance(friendId = it.id),
                            onClick = {
                                navController.navigate(
                                    "${Route.FriendDetail.route}/${it.id}"
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
private fun SectionHeader(
    title: String,
    count: Int? = null
) {
    Text(
        text = if (count != null) "$title ($count)" else title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun FriendsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.PeopleOutline,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = TextSecondary
        )
        Text(
            text = "No friends yet",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Search for a username above to add your first friend",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun FriendsLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
    }
}