package com.anish.owee.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anish.owee.viewmodel.HomeViewModel
import com.anish.owee.viewmodel.state.UserTotalBalance
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anish.owee.ui.theme.Success
import com.anish.owee.ui.theme.Error
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onUserClick: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh data silently in background when returning to home
                viewModel.loadHomeData(isSilent = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            // Fixed Header
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp,
                    fontSize = 34.sp
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.loadHomeData() },
                modifier = Modifier.fillMaxSize(),
                indicator = { }
            ) {
                // Only show skeleton on first load if we have no data
                // If we already have data (from cache or previous load), just show the list
                if (uiState.isLoading && uiState.userBalances.isEmpty()) {
                    HomeSkeleton()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        // Total Balance Card
                        item {
                            TotalBalanceCard(uiState.totalBalance)
                        }

                        // Section: Quick Settle (Instagram Story Style)
                        if (uiState.userBalances.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Quick Settle",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(uiState.userBalances) { userBalance ->
                                        PeopleSummaryStory(
                                            userBalance = userBalance,
                                            onClick = { onUserClick(userBalance.user.id) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        // Section: Breakdown
                        item {
                            Text(
                                text = "Balance Breakdown",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BalanceSummarySmall(
                                    label = "Groups",
                                    amount = uiState.groupBalance,
                                    icon = Icons.Rounded.Groups,
                                    modifier = Modifier.weight(1f)
                                )
                                BalanceSummarySmall(
                                    label = "Friends",
                                    amount = uiState.friendBalance,
                                    icon = Icons.Rounded.Person,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Branding Footer
                        item {
                            Spacer(modifier = Modifier.height(64.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp, bottom = 8.dp),
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
            }
        }
    }
}

@Composable
fun PeopleSummaryStory(
    userBalance: UserTotalBalance,
    onClick: () -> Unit
) {
    val isOwed = userBalance.balance > 0.01
    val borderColor = if (isOwed) Success else Error
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .size(68.dp)
                    .border(2.dp, borderColor, CircleShape)
                    .padding(4.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (userBalance.user.photoUrl != null) {
                    AsyncImage(
                        model = userBalance.user.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userBalance.user.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = borderColor
                        )
                    }
                }
            }
            
            // Amount Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp),
                color = borderColor,
                shape = CircleShape,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.background)
            ) {
                Text(
                    text = "₹${abs(userBalance.balance).roundToInt()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = userBalance.user.displayName.split(" ").first(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TotalBalanceCard(totalBalance: Double) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = MaterialTheme.shapes.large,
        color = if (totalBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Total Net Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "₹${"%.2f".format(abs(totalBalance))}",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
            
            val statusText = when {
                totalBalance > 0.01 -> "You are owed in total"
                totalBalance < -0.01 -> "You owe in total"
                else -> "You're all settled up!"
            }
            
            Text(
                text = statusText.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun BalanceSummarySmall(
    label: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "₹${"%.2f".format(abs(amount))}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = when {
                    amount > 0.01 -> MaterialTheme.colorScheme.primary
                    amount < -0.01 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = if (amount >= 0) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                    contentDescription = null,
                    tint = if (amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (amount >= 0) "Owed" else "Owe",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun HomeSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        // Balance Card Skeleton
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(vertical = 20.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {}

        // Quick Settle Skeleton
        Text(
            text = "Quick Settle",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(68.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {}
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.width(48.dp).height(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {}
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Breakdown Skeleton
        Text(
            text = "Balance Breakdown",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                Surface(
                    modifier = Modifier.weight(1f).height(100.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}
            }
        }
    }
}
