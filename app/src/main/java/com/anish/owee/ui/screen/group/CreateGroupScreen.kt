package com.anish.owee.ui.screen.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.viewmodel.CreateGroupViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment

@Composable
fun CreateGroupScreen(
    onBack: () -> Unit = {},
    viewModel: CreateGroupViewModel = viewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text("Create Group")

        OutlinedTextField(
            value = uiState.groupName,
            onValueChange = viewModel::updateGroupName,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Group Name")
            }
        )
        Text(
            text = "Selected: ${uiState.selectedFriendIds.size}"
        )

        Text(
            text = "Friends (${uiState.friends.size})"
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {

            items(uiState.friends) { friendship ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    val friendUser =
                        viewModel.getFriendUser(friendship)

                    Checkbox(
                        checked = uiState.selectedFriendIds.contains(
                            friendUser?.id
                        ),
                        onCheckedChange = {
                            friendUser?.id?.let {
                                viewModel.toggleFriend(it)
                            }
                        }
                    )

                    Column {

                        Text(
                            text = friendUser?.displayName
                                ?: "Unknown User"
                        )

                        Text(
                            text = "@${friendUser?.username ?: ""}"
                        )
                    }
                }
            }
        }
        Button(
            onClick = {
                viewModel.createGroup()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled =
                uiState.groupName.isNotBlank() &&
                        uiState.selectedFriendIds.isNotEmpty() &&
                        !uiState.isLoading
        ) {
            Text(
                if (uiState.isLoading)
                    "Creating..."
                else
                    "Create Group"
            )
        }

        Button(
            onClick = onBack
        ) {
            Text("Back")
        }
    }
}