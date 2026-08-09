# Implementation Plan - Offline Mode Enhancement

This plan addresses several issues related to the app's behavior when the internet is unavailable, including the "black screen" on dashboard, pull-to-refresh indicators, and disabling payment requests.

## User Review Required

> [!IMPORTANT]
> I will be adding a `ConnectivityObserver` to the `MainActivity` or a global location to monitor network status. This will be used to show a red banner at the top of the screen when offline.

> [!NOTE]
> We will cache the user profile in `PreferenceManager` to avoid forcing the user into the "Username Setup" screen when the network is down but a session exists.

## Proposed Changes

### 1. Connectivity Monitoring

#### [NEW] [ConnectivityObserver.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/utils/ConnectivityObserver.kt)
Implemented a flow-based network connectivity observer to track real-time internet status.

### 2. Session & Profile Caching

#### [MODIFY] [PreferenceManager.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/data/local/PreferenceManager.kt)
Add methods to save and retrieve the user profile (displayName, username, etc.) to/from SharedPreferences.

#### [MODIFY] [AuthRepositoryImpl.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/data/repository/AuthRepositoryImpl.kt)
Update `getCurrentUser` to return cached user data if network fetch fails.
Update `needsUsernameSetup` to be smarter about offline states.

#### [MODIFY] [SessionViewModel.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/viewmodel/SessionViewModel.kt)
Handle offline scenarios in `checkSession` to prevent stuck "Loading" or incorrect "UsernameRequired" states.

### 3. UI Enhancements

#### [NEW] [OfflineBanner.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/ui/components/OfflineBanner.kt)
Create a reusable Composable that shows a premium "You are offline" red banner at the top of the screen, sliding down when connectivity is lost.

#### [MODIFY] [MainScreen.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/MainScreen.kt)
Integrate the `OfflineBanner` into the `Scaffold` so it shows globally when needed.

#### [MODIFY] [HomeScreen.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/ui/screen/home/HomeScreen.kt)
Update the `LinearProgressIndicator` to change color from primary (blue) to error (red) when refreshing while offline.

### 4. Disabling Actions Offline

#### [MODIFY] [CreateExpenseScreen.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/ui/screen/group/CreateExpenseScreen.kt)
#### [MODIFY] [CreateFriendRequestScreen.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/ui/screen/friend/CreateFriendRequestScreen.kt)
#### [MODIFY] [GroupDetailScreen.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/ui/screen/group/GroupDetailScreen.kt)
#### [MODIFY] [FriendDetailScreen.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/ui/screen/friend/FriendDetailScreen.kt)
Disable the "Create" or "Add" buttons when offline and show a tooltip or message explaining why.

## Verification Plan

### Manual Verification
- Launch the app with internet OFF: Verify dashboard opens with cached data and doesn't show a black screen.
- Pull down to refresh with internet OFF: Verify the red loading line appears and the "You are offline" banner slides down.
- Navigate to "Create Expense" with internet OFF: Verify the "Create" button is disabled and an offline message is shown.
- Turn internet ON: Verify the banner disappears and buttons become enabled.
