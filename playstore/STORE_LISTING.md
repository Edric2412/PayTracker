# Google Play Console Store Listing & Data Safety Pack

Use the text and answers in this document directly inside the [Google Play Console](https://play.google.com/console).

---

## 1. Store Listing Metadata

### App Name (max 30 characters)
```text
Pay Tracker: Expense Tracker
```

### Short Description (max 80 characters)
```text
100% offline personal finance tracker with real-time UPI & bank expense alerts.
```

### Full Description (max 4000 characters)
```text
Pay Tracker is a privacy-first, 100% offline personal finance and expense tracker built with modern Android Jetpack Compose and our signature Botanical Glass aesthetic.

Track every rupee automatically without ever uploading your personal financial records to third-party cloud servers.

🌿 ZERO CLOUD. ZERO TRACKERS. 100% OFFLINE PRIVACY
• Your money, your device: All transactions are stored exclusively on your local device in an encrypted Room SQLite database.
• No accounts, no sign-ins: Never hand over your phone number, email address, or banking credentials.
• Zero advertising, zero analytics SDKs: No telemetry, no third-party data brokers, and zero internet server requirements.

⚡ INSTANT PAYMENT DETECTION
• Real-Time UPI Alerts: Automatically detects and parses payments made via Google Pay, PhonePe, Paytm, and Indian bank apps via on-device notification parsing.
• One-Tap Category Review: Review incoming debits, assign custom categories (Food, Groceries, Shopping, Travel, Bills), or mark transfers directly from notifications.
• Historical SMS Import: Optionally scan local bank debit SMS messages to auto-populate your monthly expenses and past spending history in seconds.

✨ CRAFTED FOR SPEED & ELEGANCE
• Botanical Glass & Obsidian Design: Premium translucent glass cards, emerald accents, and fluid 120 FPS list virtualization.
• Cash Flow Insights: Visual breakdowns of your monthly spend, money received, net cash flow, top spending categories, and peak expense.
• Biometric & PIN App Lock: Secure your financial logs with your device’s fingerprint scanner, face unlock, or device PIN.

🔒 PERMISSIONS EXPLAINED
• Notification Listener: Required to detect transaction notifications from payment apps locally in real time.
• SMS (Optional): Used solely to import historical bank transaction alerts on your request.
• Biometrics: Used to lock and unlock the application securely.

Take control of your spending with complete peace of mind. Download Pay Tracker today!
```

---

## 2. Google Play Data Safety Questionnaire Answers

When filling out the **Data Safety** section in Google Play Console, use these exact answers:

1. **Does your app collect or share any of the required user data types?**
   👉 **No** (Select "No").
   *Rationale: All notifications, SMS, and transactions are processed and stored 100% locally on the device SQLite database. Zero bytes leave the phone.*

2. **Is all of the user data collected by your app encrypted in transit?**
   👉 **N/A** (Since no data is collected or transmitted over the internet).

3. **Do you provide a way for users to request that their data be deleted?**
   👉 **Yes**
   *Add description: "Users can permanently delete all stored transactions and reset the local database at any time inside Settings > Data Management > Clear All Transactions, or by uninstalling the app."*

---

## 3. Financial Features Declaration

When prompted in the **Financial Features** policy declaration in Play Console:
- Select **Personal Finance Management (PFM)** / **Expense Tracking**.
- Feature justification: *"The app allows users to log and categorize their personal expenditures using on-device heuristics from transaction notifications and local SMS alerts."*

---

## 4. Content Rating Questionnaire

- Category: **Utility / Productivity / Finance**
- Violence / Sexual content / Language: **No**
- Shares physical location: **No**
- Enables users to interact/message each other: **No**
- Collects personal info from children: **No**
👉 Resulting rating will be: **Everyone / PEGI 3 (All Ages)**.
