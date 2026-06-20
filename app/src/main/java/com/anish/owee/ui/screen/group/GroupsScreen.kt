package com.anish.owee.ui.screen.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.viewmodel.GroupViewModel

@Composable
fun GroupsScreen(
    onCreateGroupClick: () -> Unit = {},
    onGroupClick: (String) -> Unit = {}
) {

    val viewModel: GroupViewModel = viewModel()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    android.util.Log.d(
        "OWEE_UI",
        "Groups on screen = ${uiState.groups.size}"
    )

    when {

        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.groups.isEmpty() -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "No groups yet",
                    style = MaterialTheme.typography.titleMedium
                )

                Button(
                    onClick = onCreateGroupClick,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Create Group")
                }
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {

                items(uiState.groups) { group ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGroupClick(group.id) }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    HorizontalDivider()
                }
                item {
                    Button(
                        onClick = onCreateGroupClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Create Group")
                    }
                }
            }
        }
    }
}