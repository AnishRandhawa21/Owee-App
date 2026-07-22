# Walkthrough - Settlement and Notification Fixes

I have resolved the issues with the settlement flow and notifications, ensuring they work correctly with your new separated Supabase schema.

## Changes Made

### 1. Settlement Logic Alignment
The main issue was that "Overpayments" (paying more than the debt) were being recorded using the **User ID** instead of the **Friendship ID**. This caused the balance calculators to miss those payments when recalculating the net balance.

- **[SettlementPlanner.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/domain/SettlementPlanner.kt):** Updated the `plan` function to accept a `fallbackFriendshipId`. If an overpayment occurs in a friendship context, it now strictly uses the Friendship ID as the `source_id`.
- **ViewModels:** Updated `SettlementViewModel`, `PendingPaymentViewModel`, and `CustomSettlementViewModel` to correctly pass the `friendshipId` to the planner during settlement execution.

### 2. Notification System Stability
The notification system was likely failing because it was trying to send `null` values for `id` and `created_at` fields, which are handled by the database.

- **[OweeNotification.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/data/model/OweeNotification.kt):** Used `@EncodeDefault(Mode.NEVER)` on `id` and `createdAt` properties. This ensures that when these fields are null, they are not sent in the JSON payload, allowing the Supabase defaults (`gen_random_uuid()` and `now()`) to function correctly.

### 3. Repository Cleanup
- **[SettlementRepositoryImpl.kt](file:///E:/Android-Projects/Owee/app/src/main/java/com/anish/owee/data/repository/SettlementRepositoryImpl.kt):** Updated the `deleteSettlement` method to attempt deletion from both the new `settlement_sessions` table and the legacy `settlements` table, ensuring backward compatibility while prioritizing the new structure.

## Verification Results

> [!TIP]
> **Friend Settlement Fix:** Settlements made from the Friend Detail screen now correctly update the net balance because the `source_id` is consistently mapped to the Friendship ID.

> [!IMPORTANT]
> **Data Integrity:** By using `@EncodeDefault`, we avoid potential `null` constraint violations in the `notifications` table while keeping the Kotlin model simple.

## How to Test
1. **Swipe to Settle:** Try settling a debt with a friend. After the swipe completes, the balance should update instantly, and you should no longer see the old debt.
2. **Overpayment Test:** Try paying ₹100 when you only owe ₹50. The extra ₹50 should now correctly show up as a "credit" (negative debt) in that friend's relationship.
3. **Notifications:** Check the "Notifications" section (or the DB directly) to verify that settlement notifications are now being recorded correctly.
