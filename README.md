# Pay Tracker 🌿

> **Native, 100% Offline-First Personal Finance & Expense Tracker for Android**  
> Designed with the **Botanical Glass & Obsidian** design system. Engineered for high performance, zero cloud tracking, and automatic real-time transaction detection.

---

## 📖 Overview

**Pay Tracker** is an Android application that automatically tracks personal expenses and income in real time. It captures transaction notifications and bank SMS messages from UPI apps (Google Pay, PhonePe, Paytm, CRED, BHIM, etc.), banking institutions (HDFC, SBI, ICICI, Axis, Kotak, etc.), and food/commerce services (Swiggy, Zomato, Blinkit, Zepto, Uber) directly on your device.

- **Zero Cloud / 100% Private**: Your financial data never leaves your device. No analytics SDKs, no external trackers, no remote servers.
- **Real-Time Detection**: Automatically parses transaction amount, merchant name, UPI reference ID, debit/credit direction, and transaction type as notifications arrive.
- **Heads-Up Action Notifications**: Categorize expenses or adjust records directly from system notifications without opening the app.
- **Botanical Glass UI**: High-contrast, dark-mode visual interface with frosted glass containers, specular borders, and emerald accents.
- **Silky 120 FPS List Virtualization**: Granular, typed list recycling optimized for high-refresh rate displays (Nothing OS, Pixel, Samsung AMOLED).

---

## 🏗️ System Architecture

Pay Tracker follows the **Clean Architecture** pattern combined with **MVVM (Model-View-ViewModel)** and unidirectional reactive data flow via **Kotlin Coroutines Flow**.

```mermaid
graph TD
    subgraph UI ["UI Layer (Jetpack Compose)"]
        DS[DashboardScreen]
        HS[HistoryScreen]
        AS[AnalyticsScreen]
        ST[SettingsScreen]
        BG[Botanical Glass Components]
    end

    subgraph Presentation ["Presentation Layer"]
        DVM[DashboardViewModel]
        HVM[HistoryViewModel]
        AVM[AnalyticsViewModel]
        SVM[SettingsViewModel]
    end

    subgraph Domain ["Domain / Ingestion Engine"]
        PNL[PaymentNotificationListenerService]
        SMS[SmsTransactionImporter]
        GPP[GPayParser & Regex Engine]
        CS[CategorySuggester]
        NH[NotificationHelper]
    end

    subgraph Data ["Local Data Layer (Room & SharedPreferences)"]
        TR[TransactionRepository]
        TDAO[TransactionDao]
        RDB[(Room Database: pay_expenses.db)]
        PREF[Encrypted / App Preferences]
    end

    %% Notification / SMS Stream
    OS_NOTIF[Android Notification Subsystem] -->|StatusBarNotification| PNL
    OS_SMS[Android Telephony SMS Provider] -->|Cursor| SMS

    %% Parser Flow
    PNL --> GPP
    SMS --> GPP
    GPP --> CS
    PNL -->|Expense Heads-Up Alert| NH

    %% Repository Flow
    PNL -->|Save Transaction| TR
    SMS -->|Bulk Import| TR
    TR --> TDAO
    TDAO --> RDB

    %% ViewModel Reactive Flow
    TR -->|Flow<List<Transaction>>| DVM
    TR -->|Flow<List<Transaction>>| HVM
    TR -->|Flow<List<Transaction>>| AVM

    %% UI Observation Flow
    DVM -->|StateFlow| DS
    HVM -->|StateFlow| HS
    AVM -->|StateFlow| AS
    SVM -->|StateFlow| ST
```

---

## ⚡ Transaction Detection & Parsing Pipeline

The pipeline processes financial notifications with deduplication, regex matching, and multi-field extraction for Android 12–16:

```mermaid
flowchart LR
    A["Incoming Notification / SMS"] --> B{"Package Filter"}
    B -->|Finance, Bank, SMS, or Delivery| C["Extract Content"]
    B -->|Ignored App| X["Drop"]

    C --> D["Multi-Field Extractor\n- MessagingStyle Messages\n- EXTRA_TEXT & EXTRA_BIG_TEXT\n- EXTRA_TEXT_LINES\n- Ticker"]
    D --> E["Regex Ingestion Engine\n(GPayParser)"]

    E --> F{"Valid Transaction?"}
    F -->|No / Spam / OTP| X
    F -->|Yes: Debit / Credit / Refund| G["Deduplication Engine\nSHA-256 Fingerprint & UPI Ref"]

    G --> H{"Already Recorded?"}
    H -->|Duplicate| X
    H -->|New| I["Persist to SQLite Database"]

    I --> J["Category Suggester\n(Local Keyword & Merchant Rule Engine)"]
    J --> K["Post Heads-Up Notification\nwith Quick Category Pills"]
    K --> L["Reactive UI Update\n(Kotlin Flow 120 FPS)"]
```

---

## 🗄️ Database Schema

All transactions are stored locally on-device in a SQLite database managed via Android Room.

```mermaid
erDiagram
    TRANSACTIONS {
        INTEGER id PK "Auto-incrementing primary key"
        REAL amount "Transaction amount in INR"
        TEXT merchant "Extracted merchant or recipient name"
        TEXT category "Assigned category (Food, Groceries, Shopping, etc.)"
        INTEGER timestamp "Epoch milliseconds of transaction"
        TEXT sourceApp "Source app package name or SMS sender address"
        TEXT status "CONFIRMED | PENDING | REJECTED"
        TEXT notes "Optional user notes"
        TEXT referenceId "UPI Reference ID / RRN / Bank Ref (Unique)"
        TEXT fingerprint "SHA-256 content hash for duplicate prevention"
        TEXT direction "DEBIT | CREDIT"
        TEXT classification "EXPENSE | INCOME | REFUND | CASHBACK | TRANSFER"
        TEXT rawNotification "Original raw message text for re-parsing/audit"
    }

    CATEGORIES {
        TEXT name PK "Category name"
        INTEGER iconRes "Vector icon identifier"
        INTEGER colorHex "Hex color value for botanical badges"
        INTEGER monthlyBudget "Budget target in INR"
    }

    TRANSACTIONS }|..|| CATEGORIES : "categorized as"
```

---

## 🛠️ Tech Stack & Libraries

| Layer / Concern | Technology / Library | Description |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0.21 | 100% native modern Kotlin |
| **UI Framework** | Jetpack Compose + Material 3 | Declarative UI with hardware-accelerated Compose runtime |
| **Design System** | Botanical Glass System | Frosted obsidian surfaces, specular gradient borders, Nothing OS aesthetic |
| **Architecture** | Clean Architecture + MVVM | Unidirectional Data Flow, separation of concerns, testable modules |
| **Concurrency** | Kotlin Coroutines & Flow | Non-blocking asynchronous I/O, reactive state pipelines (`StateFlow`) |
| **Local Persistence** | Android Room 2.6.1 + SQLite | Embedded relational database with schema migration support |
| **Code Generation** | KSP (Kotlin Symbol Processing) | High-performance annotation processor for Room |
| **Lifecycle** | AndroidX Lifecycle 2.8.6 | ViewModel, `LifecycleOwner`, and `compose.runtime` lifecycle integration |
| **System Services** | `NotificationListenerService` | Real-time notification interception without polling |
| **Testing** | JUnit 4 + Kotlin Test | Comprehensive unit test suite for regex parsers and flow calculators |
| **Build System** | Gradle 8.9 + AGP 8.7.0 | Modular Gradle build setup targeting Android API 26 through 35+ |

---

## 📱 Use Cases & Features

### 1. Automated Real-Time Expense & Income Capture
- Intercepts and parses real-time transaction alerts from:
  - **UPI Apps**: Google Pay (`com.google.android.apps.nbu.paisa.user`), PhonePe, Paytm, CRED, BHIM, Amazon Pay, WhatsApp Pay.
  - **Indian Banks**: HDFC, SBI, ICICI, Axis, Kotak Mahindra, Bank of Baroda, Canara, IDFC FIRST, IndusInd, Jupiter, Fi Money.
  - **Quick-Commerce & Delivery**: Swiggy, Zomato, Blinkit, Zepto, Uber, Ola, Rapido, Flipkart, Myntra.
- Supports debit messages (`"Paid ₹450 to Swiggy"`, `"debited for INR 1,200"`), credit messages (`"Received ₹2,000 from Amit"`, `"credited with Rs 500"`), refunds, and cashbacks.

### 2. High-Performance Dashboard
- **Hero Spending Velocity**: Tracks month-to-date spending, received income, and net cash flow at a glance.
- **Key Performance Indicators (KPIs)**:
  - **Transaction Count**: Total verified transactions this month.
  - **Top Category**: Dominant expense bucket with percentage share and category color badge.
  - **Peak Expense**: Largest single purchase of the current month with merchant details.

### 3. Silky 120 FPS History & Search
- Completely virtualized transaction list utilizing typed `HistoryListItem` headers and rows.
- Individual row recycling in `LazyColumn` eliminates layout lag and memory spikes.
- Search and filter by:
  - Query (merchant name, notes, reference ID)
  - Transaction Type (All, Expenses Only, Income Only)
  - Category (Food, Groceries, Shopping, Travel, Bills, etc.)
  - Date Range (Today, 7 Days, This Month, Last Month, Custom Calendar Range)

### 4. 1-Click Historical SMS Import
- Reads historical banking and financial SMS messages directly from the device's inbox.
- Automatic spam filtering excludes telemarketing, promotional messages, and OTPs.
- Automatic deduplication checks UPI reference IDs and SHA-256 transaction fingerprints to prevent duplicate entries.

### 5. Interactive Notifications
- Receive an instant heads-up alert when a transaction occurs with pre-selected category suggestion chips.
- Re-categorize or dismiss directly from the notification shade without unlocking or opening the app.

---

## 🔒 Permissions & Privacy

Pay Tracker operates under a strict **local-first privacy model**:

| Permission | Purpose |
| :--- | :--- |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Required to detect incoming transaction alerts from banking and UPI apps in real time. |
| `READ_SMS` | Optional. Used only when explicitly triggering historical SMS import from the dashboard. |
| `POST_NOTIFICATIONS` | Required on Android 13+ to show transaction confirmation heads-up alerts with quick actions. |

> **Privacy Guarantee**: Pay Tracker does **NOT** request or use the `android.permission.INTERNET` permission for expense tracking. All data is stored locally in an encrypted sandbox database on your phone.

---

## 🚀 Building & Installing

### Pre-built APK
The ready-to-install release APK is signed with the debug keystore for easy sideloading:
- **Direct APK**: [`PayTracker-v1.0.apk`](file:///home/edric/Documents/Pay/PayTracker-v1.0.apk) (13 MB)
- **Release Build Output**: `app/build/outputs/apk/release/app-release.apk`

### Building from Source

```bash
# 1. Clone repository
git clone https://github.com/Edric2412/PayTracker.git
cd PayTracker

# 2. Run unit tests
./gradlew testDebugUnitTest

# 3. Assemble Release APK
./gradlew assembleRelease

# 4. Install onto connected Android device via ADB
./gradlew installDebug
```

---

## 📄 License
This project is licensed under the MIT License — see the repository root for details.
