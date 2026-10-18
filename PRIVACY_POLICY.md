# Privacy Policy for Pay Tracker

**Last updated:** October 18, 2026

Pay Tracker ("we", "our", or "the app") is committed to protecting your privacy. This Privacy Policy explains our practices regarding data collection, processing, and storage when using the Pay Tracker Android application.

---

## 1. 100% Offline-First Architecture

Pay Tracker operates on a **strict local-only, offline architecture**:
- **Zero Cloud Servers:** We do not operate any external web servers, databases, or cloud infrastructure.
- **Zero Analytics / Telemetry:** Pay Tracker does not integrate any third-party tracking libraries, analytics SDKs (e.g., Firebase, Google Analytics), or crash reporters.
- **Zero Third-Party Data Sharing:** Your financial information never leaves your personal device under any circumstances.

---

## 2. Information Accessed & Processed

To deliver its core expense tracking functionality, Pay Tracker accesses specific on-device information strictly with your explicit consent:

### A. Notification Listener Access (`NotificationListenerService`)
* **Purpose:** To detect and record transaction alerts from authorized payment applications (such as Google Pay, PhonePe, Paytm, and bank apps) in real time.
* **Processing:** Notifications are parsed strictly on-device using local regular expression heuristics. Only the payment amount, merchant name/UPI handle, date, and status are extracted.
* **Non-Payment Messages:** Any notification not matching financial debit/credit patterns is immediately ignored and discarded. No notification content is ever transmitted off your device.

### B. SMS Permissions (`READ_SMS`, `RECEIVE_SMS`)
* **Purpose:** To allow users to optionally import historical bank transactions and parse incoming bank transactional debit alerts.
* **Processing:** SMS reading is strictly performed on-device. Personal conversations, OTPs (One-Time Passwords), and non-financial SMS messages are ignored and never logged. You can use Pay Tracker without enabling SMS permissions.

### C. Biometric Authentication (`USE_BIOMETRIC`)
* **Purpose:** If you choose to enable the Biometric & PIN App Lock in Settings, Pay Tracker uses standard Android BiometricPrompt APIs to verify your identity before showing financial logs.
* **Storage:** Biometric data (fingerprints/face scans) is processed exclusively by your device's secure hardware (TEE/Titan M chip). The app receives only a cryptographic success or failure signal; it never accesses or stores your raw biometric data.

---

## 3. Data Storage & Retention

* **Storage Location:** All transactions, categories, and settings are stored locally on your device in a private SQLite database managed by Android Room.
* **User Control & Deletion:** You retain complete control over your data at all times. You can permanently erase all transactions and reset the database at any moment by navigating to **Settings > Data Management > Clear All Transactions**, or by uninstalling the app.

---

## 4. Children’s Privacy

Pay Tracker is not directed to children under the age of 13, and we do not knowingly collect any personal information from children.

---

## 5. Changes to This Privacy Policy

If we make updates to this Privacy Policy, the revised policy will be posted within our GitHub repository and application release notes with an updated revision date.

---

## 6. Contact Us

If you have any questions or feedback regarding this Privacy Policy or Pay Tracker's privacy practices, please open an issue on GitHub:
- **Repository:** https://github.com/Edric2412/PayTracker
- **Developer:** Edric
