# Component Library
*Homestay / Resort Booking Management System*

This document serves as the single source of truth for all frontend components used in the project, based on the Modern Zen & Premium Hospitality design system.

---

## 1. Foundations / Tokens

These tokens MUST be configured in the project's TailwindCSS configuration.

### 1.1 Typography
- **Primary Font (Headings)**: `Outfit`
- **Secondary Font (Body/UI)**: `Plus Jakarta Sans`
- **Scale**:
  - `text-display-lg`: 48px, Weight 700, Line Height 1.2
  - `text-display-md`: 36px, Weight 700, Line Height 1.2
  - `text-heading-lg`: 24px, Weight 600, Line Height 1.3
  - `text-heading-md`: 20px, Weight 600, Line Height 1.4
  - `text-body-base`: 16px, Weight 400, Line Height 1.5
  - `text-body-sm`: 14px, Weight 400, Line Height 1.5
  - `text-caption`: 12px, Weight 500, Line Height 1.5

### 1.2 Colors
- **Primary (Brand)**:
  - `color-primary-base`: `#0F766E` (Teal 600)
  - `color-primary-hover`: `#0D9488` (Teal 500)
  - `color-primary-light`: `#CCFBF1` (Teal 50)
- **Surfaces**:
  - `color-surface-canvas`: `#F8FAFC` (Slate 50) - Default page background
  - `color-surface-card`: `#FFFFFF` (White) - Cards, Modals, Drawers
  - `color-surface-inverted`: `#0F172A` (Slate 900) - Sidebar, Footer
- **Text & Ink**:
  - `color-text-primary`: `#1E293B` (Slate 800)
  - `color-text-secondary`: `#64748B` (Slate 500)
  - `color-text-inverted`: `#FFFFFF`
- **Borders**:
  - `color-border-subtle`: `#F1F5F9` (Slate 100)
  - `color-border-base`: `#E2E8F0` (Slate 200)
- **Semantic**:
  - `color-success`: `#10B981` (Emerald 500)
  - `color-warning`: `#F59E0B` (Amber 500)
  - `color-danger`: `#EF4444` (Red 500)
  - `color-info`: `#3B82F6` (Blue 500)

### 1.3 Spacing, Radius & Elevation
- **Spacing**: Base 4px (`space-1` = 4px, `space-2` = 8px, `space-4` = 16px, `space-6` = 24px, `space-8` = 32px)
- **Radius**:
  - `radius-sm`: 4px (Checkboxes, Tags)
  - `radius-md`: 8px (Inputs, Buttons)
  - `radius-lg`: 16px (Cards, Modals)
  - `radius-full`: 9999px (Avatars, Status Badges)
- **Elevation (Shadows)**:
  - `shadow-sm`: Soft subtle shadow for interactive hover states (e.g., Buttons).
  - `shadow-md`: For Dropdowns and elevated small elements.
  - `shadow-lg`: Very soft and diffused for Cards and Modals.

---

## 2. Atoms

### 2.1 Buttons (`<Button />`)
- **Variants**:
  - `button-primary`: `bg-primary-base`, `text-inverted`, `radius-md`, no border.
  - `button-secondary`: `bg-transparent`, border `color-border-base`, `text-primary`.
  - `button-ghost`: No background, no border.
  - `button-success` & `button-danger`: Used for destructive actions or explicit approvals.
- **States**:
  - Hover: `bg-primary-hover` (for primary), `bg-surface-canvas` (for ghost/secondary), `shadow-sm`.
  - Disabled: opacity 50%, `cursor-not-allowed`.
  - Loading: Disables click, replaces content with a spinning SVG.

### 2.2 Inputs (`<Input />`, `<Textarea />`, `<Select />`)
- **Default**: Height 44px, `radius-md`, border `color-border-base`.
- **Focus**: `border-color-primary-base`, box-shadow `0 0 0 3px color-primary-light`. NO default browser outline.
- **Error State**: Border turns `color-danger`. Error message appears below in `text-caption color-danger`.
- **Accessibility**: Must have associated `<label>`.

### 2.3 Status Badge (`<StatusBadge />`)
- **Visuals**: `radius-full`, padding `4px 12px`, `text-caption` uppercase tracking-wide.
- **Colors**: ALWAYS uses a 10% opacity background of the semantic color, with 100% opacity text.
  - **Success**: `bg-success/10 text-success` (Available, Paid, Clean).
  - **Warning**: `bg-warning/10 text-warning` (Pending, Maintenance).
  - **Danger**: `bg-danger/10 text-danger` (Failed, Cancelled, Dirty).

### 2.4 Alert Box (`<Alert />`)
- **Visuals**: `radius-md`, background 10% opacity of semantic color, text 100% opacity.
- **Usage**: Non-blocking alerts (e.g., Cancellation Policy in SCR-19 / SCR-69, VNPay Discrepancies in SCR-52).

### 2.5 Avatars
- **Visuals**: `radius-full`, image `object-fit: cover`. Default placeholder if no image exists.

---

## 3. Molecules & Organisms

### 3.1 Room Card (`<RoomCard />`)
- **Anatomy**:
  - Top 55%: Image with `object-fit: cover`, `radius-t-lg`. StatusBadge floating at top-right.
  - Bottom 45%: Padding `space-4`. Title (`text-heading-md`), Location/Floor (`text-body-sm`), Price (`text-heading-lg` in Primary Color) aligned bottom-right.
- **Interaction**: `hover:shadow-lg`, `hover:-translate-y-1` (300ms ease-out).
- **Screens**: SCR-01, SCR-07.

### 3.2 Booking Checkout Form (`<BookingForm />`)
- **Anatomy**:
  - Desktop: 2-column layout. Left (70%) Form, Right (30%) Sticky Price Summary.
  - Sticky Summary: `color-surface-card`, `shadow-lg`, `radius-lg`.
- **Screens**: SCR-16.

### 3.3 Data Tables (`<DataTable />`)
- **Anatomy**:
  - Header: `bg-surface-canvas`, `text-caption` uppercase, `color-text-secondary`.
  - Rows: `bg-surface-card`, bottom border `color-border-subtle`.
- **Interaction**: Row hover changes background to `color-surface-canvas`.
- **Actions**: Kebab menu (Three dots "...") dropdown at the end of each row for actions to save horizontal space.
- **Screens**: SCR-21, SCR-22, SCR-29, SCR-34, SCR-36, SCR-46.

### 3.4 Drawers & Modals (`<Drawer />`, `<Modal />`)
- **Visuals**: `bg-surface-card`, `shadow-lg`, `radius-lg`.
- **Drawer**: Slide-in from right. Used for viewing PDFs, detailed forms, and chat responses.
- **Modal**: Centered overlay with a backdrop blur. Never nest a Modal inside another Modal.
- **Screens**: SCR-38 (Contract PDF), SCR-41 (Maintenance Assignment), SCR-43 (Damage Approval).

### 3.5 Image Gallery (`<ImageGallery />`)
- **Anatomy**: 1 large hero image, 4 smaller thumbnails in a grid.
- **Visuals**: `radius-lg`, overflow-hidden.
- **Screens**: SCR-08 (Room Detail).

### 3.6 Tree View (`<TreeView />`)
- **Anatomy**: Expandable lists (Property → Floor → Room).
- **Visuals**: `text-body-base`, indentations mapped with `color-border-base` vertical lines.
- **Screens**: SCR-28 (Structure Management).

### 3.7 KPI Cards & Dashboard Metrics (`<KpiCard />`)
- **Anatomy**: Icon, Title, Large Number Metric.
- **Visuals**: `color-surface-card`, `radius-lg`, `shadow-sm`.
- **Touch-Friendly Variant**: Min-height 100px for Employee Dashboard (Mobile-first).
- **Screens**: SCR-27 (Manager), SCR-45 (Admin), SCR-59 (Employee).

### 3.8 Full Calendar (`<AvailabilityCalendar />`)
- **Anatomy**: Full-size grid of months and days.
- **Visuals**:
  - Available dates: `bg-surface-canvas`.
  - Booked dates: `bg-border-base`, `text-secondary`, strikethrough styling.
- **Screens**: SCR-09.

---

## 4. Usage Patterns & Anti-Patterns

### ✅ Do:
- Wrap `RoomCard` components in semantic `<a>` or `<button>` tags for accessibility.
- Use Toast notifications (bottom-right) for success/error feedback instead of blocking modals.
- Ensure all interactive elements are focusable via keyboard (`Tab`).
- Mobile-first optimization for Employee-facing screens (SCR-59 to SCR-65), using touch-friendly targets (min 48px height) and FABs (Floating Action Buttons).

### 🚫 Don't:
- Never use solid harsh background colors for large areas. `color-primary` should strictly be used for Actions (Buttons) and Accents.
- Never use `window.alert()`. Use custom `<Alert />` or Toasts.
- Never use `outline: none` without providing a fallback focus shadow.
- Avoid nesting modals. Use a wizard/stepper or route to a dedicated page instead.
