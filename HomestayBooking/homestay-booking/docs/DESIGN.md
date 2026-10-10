# Homestay / Resort Booking System - UI Design Guidelines

## 1. Context & Goals
**Mission:** Create an implementation-ready, token-driven UI design system for the Homestay/Resort Booking Management System that prioritizes modern aesthetics, trust, usability, and operational efficiency across Guest, Customer, Manager, Admin, and Employee portals.

**Brand Vibe & Aesthetic:**
- **Theme:** "Modern Zen & Premium Hospitality"
- **Keywords:** Trustworthy, Relaxing, Clean, Premium, Airy.
- **Goal:** Users should feel a sense of calm and premium service the moment they open the booking page. Managers and Employees should experience a clean, distraction-free interface that boosts productivity.

---

## 2. Design Tokens & Foundations

### 2.1. Typography
We use a dual-font system to balance premium aesthetics with data-heavy readability.
- **Primary Font (Headings):** `Outfit` — Geometric, modern, elegant.
- **Secondary Font (Body/UI):** `Plus Jakarta Sans` — Clean, highly legible for tables and forms.
- **Scale:**
  - `text-display-lg`: 48px, Weight 700, Line Height 1.2
  - `text-display-md`: 36px, Weight 700, Line Height 1.2
  - `text-heading-lg`: 24px, Weight 600, Line Height 1.3
  - `text-heading-md`: 20px, Weight 600, Line Height 1.4
  - `text-body-base`: 16px, Weight 400, Line Height 1.5
  - `text-body-sm`: 14px, Weight 400, Line Height 1.5
  - `text-caption`: 12px, Weight 500, Line Height 1.5

### 2.2. Color Palette (Semantic Tokens)
Avoid pure black/white. Use sophisticated slate grays and a calming nature-inspired primary color.
- **Brand Colors:**
  - `color-primary-base`: `#0F766E` (Teal 600 - Calm, trustworthy)
  - `color-primary-hover`: `#0D9488` (Teal 500)
  - `color-primary-light`: `#CCFBF1` (Teal 50 - Used for subtle highlights)
- **Surfaces:**
  - `color-surface-canvas`: `#F8FAFC` (Slate 50 - The default background for all pages)
  - `color-surface-card`: `#FFFFFF` (White - For cards, dropdowns, modals)
  - `color-surface-inverted`: `#0F172A` (Slate 900 - For Sidebar, Footer)
- **Text & Ink:**
  - `color-text-primary`: `#1E293B` (Slate 800 - Headings & primary reading)
  - `color-text-secondary`: `#64748B` (Slate 500 - Metadata, descriptions)
  - `color-text-inverted`: `#FFFFFF`
- **Borders:**
  - `color-border-subtle`: `#F1F5F9` (Slate 100 - Table row dividers)
  - `color-border-base`: `#E2E8F0` (Slate 200 - Input borders, card borders)
- **Semantic (Status & Feedback):**
  - `color-success`: `#10B981` (Emerald 500)
  - `color-warning`: `#F59E0B` (Amber 500)
  - `color-danger`: `#EF4444` (Red 500)
  - `color-info`: `#3B82F6` (Blue 500)

### 2.3. Spacing & Geometry
- **Spacing Scale:** 4px base (`space-1`: 4px, `space-2`: 8px, `space-3`: 12px, `space-4`: 16px, `space-6`: 24px, `space-8`: 32px, `space-12`: 48px, `space-16`: 64px)
- **Border Radius:**
  - `radius-sm`: 4px (Checkboxes, small tags)
  - `radius-md`: 8px (Inputs, Buttons)
  - `radius-lg`: 16px (Room Cards, Modals, Summary Panels)
  - `radius-full`: 9999px (Avatars, Status Badges)
- **Elevation (Shadows):**
  - `shadow-sm`: `0 1px 2px rgba(0,0,0,0.05)` (Buttons, Inputs on hover)
  - `shadow-md`: `0 4px 6px -1px rgba(0,0,0,0.1)` (Dropdowns)
  - `shadow-lg`: `0 10px 15px -3px rgba(0,0,0,0.05)` (Cards, Modals - Very soft and diffused)

---

## 3. Component-Level Rules

### 3.1. Room Card (`RoomCard`)
- **Anatomy:**
  - Top 55%: High-quality image, `object-fit: cover`.
  - Floating `badge` over image (top-right) for Status (e.g., "Available").
  - Bottom 45%: Padding `space-4`. Title (`text-heading-md`), Location/Floor (`text-body-sm`), Price (`text-heading-lg` / Primary Color) aligned bottom-right.
- **Interaction:** `hover:shadow-lg`, `hover:-translate-y-1` (micro-animation 300ms ease-out). Cursor changes to pointer.

### 3.2. Booking Checkout Form (`BookingForm`)
- **Layout:** Two-column on desktop (Left 70% Form, Right 30% Sticky Price Summary).
- **Inputs:** Height 44px, `radius-md`, border `color-border-base`.
- **Focus State:** `border-color-primary-base`, `box-shadow: 0 0 0 3px color-primary-light`. No outline.
- **Validation:** Error states turn border `color-danger`. Error messages appear below input in `text-caption` `color-danger`.

### 3.3. Buttons (`Button`)
- **Primary:** `bg-primary-base`, `text-inverted`, `radius-md`, no border. Hover: `bg-primary-hover`.
- **Secondary (Outline):** `bg-transparent`, border `color-border-base`, `text-primary`. Hover: `bg-surface-canvas`.
- **Ghost:** No background, no border. Hover: `bg-surface-canvas`.
- **States:** `disabled` opacity 50%, cursor-not-allowed. `loading` shows a spinning SVG replacing the icon/text.

### 3.4. Status Badges (`StatusBadge`)
- **Vibe:** Premium "Soft" Badges. Never use harsh solid backgrounds for badges.
- **Style:** `radius-full`, padding `4px 12px`, `text-caption` uppercase tracking-wide.
- **Colors:**
  - Success (Available/Paid): Background `color-success` at 10% opacity. Text `color-success` 100%.
  - Danger (Failed/Cancelled): Background `color-danger` at 10% opacity. Text `color-danger` 100%.
  - Warning (Pending): Background `color-warning` at 10% opacity. Text `color-warning` (darkened for contrast).

### 3.5. Data Tables (Admin/Manager Portals)
- **Anatomy:**
  - Header: `bg-surface-canvas`, `text-caption` uppercase, `color-text-secondary`.
  - Rows: `bg-surface-card`, border-bottom `color-border-subtle`.
- **Interaction:** Hover on row changes background to `color-surface-canvas`.
- **Actions:** Use a "..." (Kebab menu) dropdown at the end of each row for actions (Edit, View, Delete) to save horizontal space.

---

## 4. Accessibility Requirements & Acceptance Criteria
- **Target:** WCAG 2.1 AA.
- **Keyboard Navigation:** All interactive elements (Buttons, Inputs, Table Rows, Dropdowns) MUST be focusable using the `Tab` key.
- **Focus Indicators:** MUST use a visible focus ring (using the `color-primary-light` shadow mentioned in Inputs). Never use `outline: none` without a fallback shadow.
- **Contrast:** Text colors against their backgrounds MUST pass a 4.5:1 contrast ratio. (E.g., `color-text-secondary` `#64748B` on `#FFFFFF` passes).
- **Forms:** Every input MUST have an associated `<label>`. Placeholders do not replace labels.

---

## 5. Tone & Content Standards
- **Voice:** Professional, welcoming, concise, and reassuring.
- **Do:** "Your booking is confirmed. We look forward to hosting you."
- **Don't:** "Success! Data saved to database."
- **Error Messages:** Must be actionable. (e.g., "Please enter a valid phone number" instead of "Invalid input").

---

## 6. Anti-Patterns & Prohibited Implementations
- 🚫 **No Solid Colored Backgrounds for large areas:** Do not use `color-primary` as a page background. Use it strictly for Actions (Buttons) and Accents.
- 🚫 **No Alerts that block the UI:** Avoid `window.alert()`. Use non-blocking Toast notifications (bottom-right) for success/error messages.
- 🚫 **No Modals inside Modals:** If a flow requires multiple steps, use a wizard/stepper within a single Modal, or route to a Dedicated Page.
- 🚫 **No hidden scrollbars:** Always ensure scrollbars are visible in Data Tables and Drawers.

---

## 7. QA Checklist for Frontend Implementation
- [ ] Are all Design Tokens (Colors, Typography) implemented via CSS Variables / Tailwind Config?
- [ ] Do Status Badges use the 10% opacity background rule instead of solid colors?
- [ ] Is the Focus state visible on all inputs and buttons?
- [ ] Do Buttons have a Loading state that disables click?
- [ ] Are Room Cards wrapped in an `<a>` or `<button>` tag for semantic HTML?
- [ ] Does the UI render correctly on Mobile (375px) and Desktop (1440px)?
