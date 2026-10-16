# Project: Notification-First Expense Tracker

## 1. Overview

Build a personal Android expense-tracking app that reduces the effort required to record everyday spending. The app observes notifications from supported banking and payment apps, extracts available transaction details, suggests a spending category, and asks the user to confirm or correct the suggestion.

The first release is for personal use on an Android phone. It will be installed directly on the device rather than published to Google Play. The app should work locally and avoid paid services wherever practical.

## 2. Problem Statement

Manually entering every purchase into a budgeting app is tedious, so transactions often go unrecorded. Bank and payment notifications can provide useful transaction details, but they usually do not explain the user's actual purpose for a payment.

The app will use transaction notifications to create an expense record automatically, then request only the missing information from the user.

## 3. Goals

- Detect supported payment or banking notifications on Android.
- Extract available details such as amount, merchant or recipient, transaction time, source app, and reference.
- Suggest a likely expense category.
- Ask the user to confirm the suggestion or correct it.
- Support short text descriptions and, where feasible, speech-to-text input.
- Persist transactions locally before presenting a follow-up notification.
- Keep uncategorized or unanswered transactions in a pending-review list.
- Avoid duplicate transactions when the same alert is delivered more than once.
- Keep financial and notification data on the device for the initial version.
- Use no paid API for core transaction capture and categorization.

## 4. Core User Flow

1. The user makes a cashless payment.
2. A supported bank or payment app posts a notification.
3. The app's notification listener checks whether the notification is relevant.
4. A parser extracts the details available in the notification.
5. A categorization component suggests a category based on rules, merchant mappings, and optionally Gemini.
6. The app saves the transaction to its local database with a pending status.
7. A follow-up notification asks the user to confirm the suggested category.
8. If the user confirms, the transaction is marked confirmed.
9. If the user rejects the suggestion, the app offers category choices and a text field; speech-to-text may be used to enter a description.
10. If the user ignores or dismisses the prompt, the transaction remains pending and is visible in the app's review queue.

The app must not assume that the notification contains the exact items purchased. If the purpose is not known, it should ask rather than invent details.

## 5. Planned Features

### Initial MVP

- Android-native application.
- Notification access onboarding and source-app filtering.
- Notification parsing for a small number of tested source formats.
- Transaction persistence using a local database.
- Basic rule-based category suggestions.
- Follow-up notification with confirm and correct actions.
- Pending-review screen for unanswered transactions.
- Transaction history with category, amount, source, and timestamp.
- Duplicate detection.
- Manual correction of category and description.
- Local-only operation without a backend.

### Follow-up Improvements

- Gemini-assisted categorization for ambiguous transaction descriptions.
- Merchant-category preferences learned from user corrections.
- Speech-to-text for descriptions.
- Monthly summaries and category totals.
- Better handling of refunds, failed payments, transfers between the user's own accounts, and reversals.
- Export and backup options.
- Additional supported notification formats.

Features will be added only after the core notification-to-expense flow works reliably.

## 6. Planned Architecture

The initial architecture is a single Android application with local persistence and no server.

### Notification Listener

Use Android's `NotificationListenerService` to observe notifications after the user explicitly grants notification access. Initially, process only notifications from user-selected, supported payment or banking apps.

The listener must ignore unrelated notifications and should not collect or store the contents of unrelated messages.

### Transaction Parser

Convert notification text into a structured candidate transaction containing the available amount, merchant or recipient, transaction timestamp, source package, reference, and original parsing confidence.

Parsing should be conservative. Unknown or ambiguous notifications should be ignored or surfaced for review instead of being treated as confirmed expenses.

### Categorization Engine

Start with deterministic rules and merchant-to-category mappings. This is inexpensive, fast, and works offline. Keep the suggested category separate from the user's final category so corrections can be tracked.

Gemini may be used as an optional enhancement for ambiguous descriptions. It must not be treated as a source of missing facts; it can classify only from information provided to it.

### Transaction Repository and Local Database

Use Room over SQLite as the persistent source of truth. Save a transaction before issuing a follow-up notification. Store confirmation status and corrections so missed notifications do not cause data loss.

Use deduplication based on a reliable transaction reference when available, with a fallback fingerprint for notifications that lack one. Amount and timestamp alone should not be assumed to uniquely identify a transaction.

### Notification Interaction

Use Android notifications for the follow-up prompt and supported actions such as confirming or correcting a category. The app UI provides the full review experience. A dismissed notification must not delete the underlying pending transaction.

### User Interface

Use Jetpack Compose for onboarding, permissions guidance, dashboard, transaction history, pending review, category correction, and settings.

## 7. Planned Technology Stack

| Area | Planned technology | Purpose |
|---|---|---|
| Language | Kotlin | Native Android implementation |
| IDE and build | Android Studio, Gradle | Development, builds, device installation |
| UI | Jetpack Compose | Native declarative UI |
| Local persistence | Room + SQLite | Transactions, statuses, preferences |
| Async work and state | Kotlin Coroutines + Flow | Database observation and UI updates |
| Notification capture | Android `NotificationListenerService` | Observe supported app notifications |
| Follow-up prompts | Android notification APIs | Confirmation and correction actions |
| Text input | Compose text fields | Correct purpose and category |
| Voice input | Android speech-to-text / device keyboard dictation | Optional description input |
| Categorization baseline | Kotlin rules and merchant mappings | Fast, offline category suggestions |
| Optional AI | Gemini API, proposed model ID: `gemini-3.5-flash-lite` | Categorization of ambiguous descriptions, if the model is available and suitable |
| Version control | Git and GitHub | Source control and backup |
| Deployment | Direct APK installation | Personal testing and use |

No React Native, Flutter, backend service, cloud database, Docker, or Kubernetes is required for the initial personal version.

## 8. Gemini API Plan and Key Safety

The proposed Gemini model identifier is `gemini-3.5-flash-lite`. Confirm that this exact model ID is available for the selected Gemini API endpoint before implementation; model availability and naming can change.

Gemini should be optional and used only when local rules cannot confidently suggest a category. The request should contain the minimum necessary information, such as a merchant or sanitized transaction description and amount if relevant. Do not send full notification contents, account balances, account identifiers, or unrelated personal information.

Never commit an API key to GitHub or hardcode it in the Android app. A key embedded in a distributed APK can be extracted. For a personal prototype, configure the key locally and understand that client-side storage is not a secure secret store. If the app is later shared with other people, move Gemini requests behind a backend or remove the API dependency. Core capture, local storage, confirmation, and manual categorization must continue to work without Gemini.

Check the current Gemini API pricing and usage terms before relying on it. The design should not require a paid API for normal operation.

## 9. Privacy and Security

- Request notification access only after explaining why it is needed.
- Do not request SMS or email access in the first version.
- Filter notifications by selected source apps.
- Process notification content locally wherever possible.
- Do not log full notification contents in release builds.
- Store transaction data in app-private local storage.
- Do not transmit financial data to an external service unless required for an explicitly enabled feature.
- Minimize data sent to Gemini and make AI categorization optional.
- Keep API keys out of source control and never treat an APK-embedded key as secret.
- Provide a way to review, correct, and delete locally stored transactions.

## 10. Known Limitations and Risks

- A payment app may not issue a notification for every transaction.
- Notification text may omit the merchant, exact purpose, transaction timestamp, or reference.
- Different banks and payment apps use different notification formats.
- Android background execution and notification behavior can vary by OS version and device manufacturer.
- Notifications may be duplicated, delayed, dismissed, or removed.
- The app cannot know the exact items purchased unless a suitable receipt or item-level data source is provided.
- A person-to-person payment does not reveal whether it was a repayment, gift, shared bill, or another purpose.
- Gemini may make incorrect category suggestions; the user must remain able to correct them.
- Public distribution would require a separate review of Google Play policies, permissions, privacy disclosures, and release signing.

## 11. Development and Deployment Plan

### Phase 1 — Android Foundation

- Install Android Studio and create a Kotlin/Compose project.
- Run the generated app on the personal Android phone.
- Set up Git.
- Create the Room database and a basic transaction list using synthetic test data.

### Phase 2 — Notification Access Proof of Concept

- Declare the notification listener service.
- Guide the user to Android's notification-access settings.
- Receive and inspect notifications from one selected test source.
- Filter irrelevant notifications.
- Use synthetic notifications first, then compare one real notification with its source details.

### Phase 3 — Transaction Pipeline

- Parse a supported notification format.
- Validate whether it appears to represent a completed debit.
- Deduplicate it.
- Save it to Room as pending.
- Show the follow-up confirmation notification.

### Phase 4 — Confirmation and Corrections

- Implement confirm and correct actions.
- Save user-selected categories and descriptions.
- Keep ignored transactions in the pending-review list.
- Test duplicate, malformed, and incomplete notifications.

### Phase 5 — Optional Gemini and Polish

- Verify model availability and API terms.
- Add optional AI categorization only if rule-based categorization is insufficient.
- Add speech-to-text, transaction filters, and summaries as needed.
- Test privacy, battery behavior, and reliability.

### Personal Deployment

During development, run the app from Android Studio. For standalone personal use, generate and install an APK. A backend and public app-store release are out of scope for the first version.

## 12. First Milestone and Acceptance Criteria

**Milestone 1: Turn one supported notification into a persistent, confirmable expense.**

The milestone is complete when:

- The app runs on the user's Android phone.
- The user can grant and revoke notification access.
- A supported test or real notification can be received and parsed.
- A valid candidate transaction is saved in Room before a follow-up is shown.
- The follow-up offers confirmation and correction.
- Confirmation updates the saved transaction.
- Correction updates the category or description.
- Ignoring the follow-up leaves the transaction pending in the app.
- Re-delivery of the same notification does not create a duplicate.
- The workflow functions without Gemini or any paid API.

The first priority is to prove that the phone exposes enough useful notification data for a reliable transaction pipeline. A polished dashboard is secondary to that proof.

## 13. Definition of Success

The MVP is successful if it reliably captures transactions from the selected, supported sources, reduces manual entry to a quick confirmation or correction, preserves unanswered transactions, avoids duplicate records, and keeps financial data local by default.
