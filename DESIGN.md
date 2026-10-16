# Design System Specification: Pay Tracker
**Origin:** Stitch Taste Standard & Material Design 3 (Nothing OS Aesthetic)

---

## Configuration & Style Dials

| Dial | Level | Description |
| :--- | :--- | :--- |
| **Creativity** | `8` | Clean, bold typography, tactile feedback, monospace transaction numbers, dot-matrix subtle branding. |
| **Density** | `6` | Balanced financial cockpit. High information density without visual clutter or overwhelming cards. |
| **Variance** | `7` | Asymmetric summary stats, hero balance card, contextual action banners, staggered list choreography. |
| **Motion Intent**| `7` | Smooth Compose transitions (`spring(dampingRatio = 0.8f, stiffness = 400f)`), pulse badges, animated progress bars. |

---

## 1. Visual Atmosphere
An intentional, distraction-free financial cockpit inspired by Nothing OS and Google Material Design 3. Dark-first matte surfaces, crisp 1px borders, high-contrast monospace typography for numbers and currency, and an iconic singular red accent (`#D71921`) that calls attention only to critical pending actions and alerts.

---

## 2. Color Palette & Semantic Roles

### Neutral System
- **Dark Background:** `#0C0C0E` — Deep charcoal canvas (never pure `#000000`).
- **Dark Surface:** `#141416` — Elevated cards and containers.
- **Dark Surface Variant:** `#1F1F24` — Secondary interactive chips and inputs.
- **Dark Border (Whisper):** `#2A2A30` — 1px crisp outline for structural definition.
- **Text Primary:** `#F4F4F6` — High legibility.
- **Text Secondary:** `#A1A1AA` — Subtle labels and timestamps.
- **Text Tertiary:** `#71717A` — Supporting transaction metadata.

### Accent System
- **Primary Accent (Nothing Red):** `#D71921` — Badges, primary CTA highlights, pending notification signal.
- **Primary Container:** `#2D1214` — Soft dark container for red elements.
- **Success:** `#22C55E` — Confirmed transactions and healthy budget states.
- **Warning/Pending:** `#F59E0B` — Transactions awaiting review in queue.

### Category Palette (Muted & Distinctive)
- **Food & Dining:** `#FF7043`
- **Groceries:** `#66BB6A`
- **Shopping:** `#AB47BC`
- **Travel & Commute:** `#42A5F5`
- **Bills & Utilities:** `#FFA726`
- **Transfers:** `#26A69A`
- **Entertainment:** `#EC407A`
- **Health:** `#26C6DA`
- **General / Other:** `#78909C`

---

## 3. Typography Architecture
- **Numbers / Currencies:** Monospace font family (`FontFamily.Monospace`) for amounts, transaction counts, dates, and percentages. Strict alignment.
- **Headlines / Display:** SemiBold / Bold Sans with tight letter spacing (`-0.5.sp`).
- **Body & Captions:** Clean Sans-serif with comfortable line height and muted secondary color for subtitles.

---

## 4. Component Standards

### Stat & Hero Cards
- **Corners:** 20.dp rounded corners with 1.dp border (`#2A2A30`).
- **Content:** Large balance headline in monospace, subtitle comparison, and visual trend pill.
- **Interactive:** Subtle ripple or tactile scale on touch.

### Transaction Items
- **Leading Icon:** 42.dp rounded squircle (not simple circle) with subtle tinted category background.
- **Title & Subtitle:** Merchant name in `titleMedium`, timestamp and source badge (e.g., "Google Pay") in `labelSmall`.
- **Trailing Amount:** Bold monospace amount with minus sign (`-₹240.00`) and confirmation status badge.

### Review Queue Cards
- **Actionable:** Clear suggestion pill ("Suggested: Food"), single-tap confirm button with checkmark, and quick-edit category chips.
- **Empty State:** Illustrated card with subtle pulse icon celebrating "All caught up!".

---

## 5. Motion & Compose Guidelines
- Compose `AnimatedVisibility` with fade + slide-in for transaction state changes.
- `animateFloatAsState` or `animateContentSize()` for expanding category pickers.
- Spring physics: `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)`.
