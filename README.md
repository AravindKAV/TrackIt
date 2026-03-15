# TrackIt – Expense Tracker

TrackIt is a modern, offline-first Android expense tracker built with Jetpack Compose and Hilt. It combines manual entry with automatic SMS/notification parsing, tracks multiple bank accounts, and renders a fintech-style dashboard with live analytics.

## Feature highlights
- **Guided onboarding** – Splash + onboarding flow explains core features and walks through SMS/notification permissions.
- **Multi-account ledger** – Create bank accounts, tag every transaction, and monitor per-account totals alongside overall spend.
- **Dashboard analytics** – Monthly total hero, account cards with unique gradients, category pie chart with sweep gradients, weekly bar chart, and recent transactions list.
- **Credit & Debit Support** – Track both spending (Debits) and income (Credits). Automatically detects income from SMS/Notifications using smart keyword parsing.
- **Money Transfers** – Move funds between your added bank accounts with a dedicated "Transfer" mode that handles double-entry automatically.
- **App Lock Security** – Protect your sensitive financial data with Biometric (Fingerprint/Face) or Device PIN/Password authentication.
- **Manual + automatic capture** – Add transactions with validation, or let the app parse UPI SMS/notifications (Google Pay, PhonePe, Paytm, BHIM, etc.).
- **Expense History** – Dedicated History tab showing a beautiful month-by-month breakdown of your earnings vs. spending with full transaction records.
- **Dark & Light Mode** – Full support for system themes with a custom Light/Dark/System preference toggle in Settings.
- **Premium UI** – Modern "Glassmorphism" aesthetic with vibrant color gradients, decorative geometric overlays, and smooth navigation transitions.
- **Transactions hub** – Full ledger with category and account filters, tap-to-edit, and swipe-to-delete gestures.
- **Settings & sample data** – Toggle detectors, reset seeded demo data, manage bank accounts, and customize app appearance.

## Architecture
```
SMS/Notifications -> Parser -> ExpenseRepository -> Room (TransactionDao/AccountDao/CategoryDao)
                                    |
                               Use cases
                                    |
                          Compose ViewModels + UI
```
- **Data layer**: Room entities (transactions, categories, accounts) with Flow-based DAO queries, plus DataStore preferences for detector toggles, theme settings, and lock state.
- **Domain layer**: Use cases (`ObserveDashboardAnalytics`, `ObserveTransactions`, `UpsertTransaction`, `ObserveAccounts`, `UpdateTheme`, etc.) hide repository details from the UI.
- **Presentation layer**: Jetpack Compose + Navigation + Hilt ViewModels drive Splash, Onboarding, Dashboard, Transactions, History, and Settings screens.
- **Services**: BroadcastReceiver for SMS and NotificationListenerService for supported UPI apps feed the parser and repository.

## Getting started
1. **Prereqs** – Android Studio Iguana (or newer) with JDK 17 and Android SDK 26–34.
2. **Sync & build** – `./gradlew :app:assembleDebug` (or press *Sync Project* in Android Studio).
3. **Install** – `adb install app/build/outputs/apk/debug/app-debug.apk` or simply hit *Run*.
4. **First launch** – Splash seeds sample accounts + categories, then onboarding walks through feature education and permission prompts. 
5. **Grant permissions** – Approve SMS + POST_NOTIFICATIONS when prompted, then open the notification-listener settings screen to allow "TrackIt".
6. **Populate Data** – Use the **Reset Sample Data** option in Settings to quickly preview the Dashboard and History features with rich dummy data.

## Dashboard analytics
- **Monthly hero** – High-contrast gradient card surfaces the month-to-date spend.
- **Account insights** – Each bank card uses a dynamic horizontal gradient matching the account's color and shows spent/earned totals and live balance.
- **Category + weekly charts** – Custom Compose pie + bar charts with vibrant gradients that react instantly to Room updates.
- **Universal Add Button** – A central, floating "Add" button at the bottom of the screen for quick manual entry from any tab.

## Manual transaction entry
1. Tap the central floating action button on the bottom bar to open the form.
2. Choose between **Debit**, **Credit**, or **Transfer** modes.
3. Select the accounts involved, enter amount + merchant, and pick from context-aware categories (e.g., Salary/Cashback for Credits, Travel/Dining for Debits).
4. For **Transfers**, simply select the source and destination bank accounts to move funds.
5. Hit **Save Entry** to update Room; dashboard and history widgets refresh immediately.

## Automatic parsing
### SMS detection
- `SmsTransactionReceiver` listens to `Telephony.Sms.Intents.SMS_RECEIVED`.
- Incoming bodies flow through `UpiDetectionParser`, which intelligently detects amounts, merchants, and transaction types (Credit vs Debit).
- Parsed transactions land in the ledger with `source = SMS`.

### Notification listener
- `UpiNotificationListenerService` watches Google Pay, PhonePe, Paytm, BHIM, Amazon Pay, etc.
- Notifications reuse the same parser and land in the ledger with `source = NOTIFICATION`.

## Security & Privacy
- **App Lock**: Optional Biometric/PIN lock can be enabled in Settings to secure the app launch.
- **Privacy First**: All computation and data storage happen strictly on-device. TrackIt never requests banking credentials or sends your data off-device.

## Screenshots
- Dashboard – Modern analytics with glassmorphism effects.
- History – Comprehensive monthly financial timeline.
- Transactions – Filterable record ledger with swipe gestures.
- Settings – Full customization for themes, security, and account management.
