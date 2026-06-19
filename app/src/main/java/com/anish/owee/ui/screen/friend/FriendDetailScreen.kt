package com.anish.owee.ui.screen.friend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.data.model.User
import com.anish.owee.ui.screen.friend.components.BalanceState
import com.anish.owee.ui.screen.friend.components.FriendAvatar
import com.anish.owee.ui.screen.friend.components.color
import com.anish.owee.ui.screen.friend.components.rememberMockBalance
import com.anish.owee.ui.screen.friend.components.sentenceLabel
import com.anish.owee.ui.theme.OweeTheme
import com.anish.owee.ui.theme.Surface
import com.anish.owee.ui.theme.SurfaceVariant
import com.anish.owee.ui.theme.TextSecondary
import com.anish.owee.viewmodel.FriendshipViewModel

@Composable
fun FriendDetailScreen(
    friendId: String,
    onBackClick: () -> Unit = {},
    friendshipViewModel: FriendshipViewModel = viewModel()
) {

    val uiState by friendshipViewModel
        .uiState
        .collectAsState()

    val friend: User? = uiState.friends
        .firstNotNullOfOrNull { friendship ->
            when {
                friendship.sender?.id == friendId -> friendship.sender
                friendship.receiver?.id == friendId -> friendship.receiver
                else -> null
            }
        }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && friend == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            friend == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "We couldn't find this friend",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                FriendDetailContent(
                    friend = friend,
                    onBackClick = onBackClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun FriendDetailContent(
    friend: User,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val balance = rememberMockBalance(friendId = friend.id)
    val firstName = friend.displayName.trim().split(" ").firstOrNull() ?: friend.displayName

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {

        // Top Header with Back button
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Text(
                text = "Friend Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Profile header
        Column(
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
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "@${friend.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Balance summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (balance is BalanceState.SettledUp) {
                        "Balance"
                    } else if (balance is BalanceState.YouOwe) {
                        "You owe"
                    } else {
                        "Owes you"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = balance.sentenceLabel(firstName),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = balance.color(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Primary actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { /* hook for future Request Money flow */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Request Money",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            OutlinedButton(
                onClick = { /* hook for future Add Expense flow */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Add Expense",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Recent activity
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            RecentActivityEmptyState()

            TextButton(
                onClick = { /* hook for future settlement history view */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "View Settlement History",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RecentActivityEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = TextSecondary
            )
            Text(
                text = "No activity yet",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
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

@Preview(showBackground = true)
@Composable
private fun FriendDetailContentYouOwePreview() {
    OweeTheme {
        FriendDetailContent(
            friend = User(
                id = "owes_you_seed_1",
                email = "rahul@example.com",
                displayName = "Rahul Mehta",
                username = "rahulm",
                photoUrl = null
            ),
            onBackClick = {}
        )
    }
}