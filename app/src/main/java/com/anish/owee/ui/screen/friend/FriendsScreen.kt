package com.anish.owee.ui.screen.friend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.viewmodel.FriendshipViewModel

@Composable
fun FriendsScreen(
    friendshipViewModel: FriendshipViewModel = viewModel()
) {

    val uiState by friendshipViewModel
        .uiState
        .collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {

            Text(
                text = "Find Friends",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = {
                    friendshipViewModel.updateSearchQuery(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Search username")
                }
            )

            Button(
                onClick = {
                    friendshipViewModel.searchUsers()
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Search")
            }
        }

        items(uiState.searchResults) { user ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {

                    Text(user.displayName)

                    Text("@${user.username}")
                }

                Button(
                    onClick = {
                        friendshipViewModel
                            .sendFriendRequest(user.id)
                    }
                ) {
                    Text("Add")
                }
            }
        }

        item {

            HorizontalDivider()

            Text(
                text = "Incoming Requests",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(uiState.incomingRequests) { request ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {
                    Text(
                        text = request.sender?.displayName ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "@${request.sender?.username ?: "unknown"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row {

                    Button(
                        onClick = {
                            friendshipViewModel
                                .acceptFriendRequest(
                                    request.id
                                )
                        }
                    ) {
                        Text("Accept")
                    }

                    Button(
                        onClick = {
                            friendshipViewModel
                                .rejectFriendRequest(
                                    request.id
                                )
                        }
                    ) {
                        Text("Reject")
                    }
                }
            }
        }

        item {

            HorizontalDivider()

            Text(
                text = "Friends",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(uiState.friends) { friendship ->

            val friend = if (friendship.senderId == uiState.currentUserId) {
                friendship.receiver
            } else {
                friendship.sender
            }

            Column {
                Text(
                    text = friend?.displayName ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "@${friend?.username ?: "unknown"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}