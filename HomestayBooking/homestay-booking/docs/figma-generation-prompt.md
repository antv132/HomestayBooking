# Figma Generation Prompt
## Homestay / Resort Booking Management System — Complete UI Design System

> **Docs sync 2026-07-23:** Canonical business rules = `sdd/services/homestay/features/*/spec.md` (esp. `004`, `012`) + `docs/screen.md` for **SCR IDs**.  
> - Customer cancel: refund **deposit by tier** ≥7d 100% / 3–7d 50% / &lt;3d 0% — **not** flat 50% of paid.  
> - Payment edu: **VNPay only** (TD-011). Hold timeout default **10 minutes**.  
> - SCR-19 = Cancellation; SCR-20 = Order Review & Payment; SCR-37 = Check-in/Check-out (`screen.md`). Legacy SCR numbers in older sections below must **not** override `screen.md`.

---

## Context

You are a **Senior Product Designer** and **Senior Frontend Architect**.

Analyze and use the following documents as the **single source of truth** (priority when conflict):

1. `sdd/services/homestay/features/<feature>/spec.md` — feature rules  
2. `docs/screen.md` — SCR catalog / IDs  
3. `Specification_v2.md`, `screendesign.md`, `entity-ui-mapping.md`, `api-spec-by-screen.md`  
4. `DESIGN.md`, `component-library.md` — visual system only  

Also: `AGENTS.md` — Architecture, roles, and coding standards.

**Project:** Homestay / Resort Booking Management System
**Actors:** Guest (unauthenticated) · Customer · Manager · Admin · Employee
**Total Screens:** 65 (see `screen.md`)

---

## Core Objective

Generate a complete and consistent UI design system and screen set for **all 65 screens** defined in `screen.md`.

The generated UI must:
- Follow `screendesign.md` exactly for layout and component placement
- Follow `component-library.md` exactly for component anatomy and states
- Follow `docs/DESIGN.md` exactly for all design tokens (colors, typography, spacing, radius)
- Follow `entity-ui-mapping.md` exactly — every field, column, and KPI must map to a real entity attribute
- Follow cancel / payment / hold rules from **sdd** (tiers, VNPay-only, 10-min hold)

> **Do not invent** business fields, entities, or attributes that do not exist in `entity-ui-mapping.md`.
> Every form field, table column, detail section, search filter, and dashboard metric must map to a documented entity attribute.

---

## GLOBAL RULES

### Rule 1 — Entity-Driven UI (Critical)

All screens must be derived from the entity data model. UI fields map **directly** to entity attributes.

**Example:**
```
Entity: Booking
Attributes: customerId · roomId · checkInDate · checkOutDate · guestCount · totalAmount · status

Booking Form MUST contain:   Room selector · Check-in date · Check-out date · Guest count · Special requests
Booking Form MUST NOT contain: Coupon code · Loyalty points · AI recommendation score
```

---

### Rule 2 — Form Generation Rules

- **Create screen:** Generate form controls from entity attributes only.
- **Edit screen:** Only include **mutable** attributes. Never show `id (UUID)`, `createdAt`, `updatedAt`, auto-generated fields.
- **Detail screen:** Show all important entity attributes including related entity data.

CRUD screens for the same entity must use the **same field order**:
```
Room: 1. roomNumber  2. roomType  3. floorId  4. pricingRule.basePrice  5. pricingRule.weekendSurcharge  6. capacity  7. area  8. description  9. status
```

---

### Rule 3 — Table Column Rules

All table columns must map to entity attributes or aggregated relationship data.

```
Booking Table columns: bookingId · Customer name (User.fullName) · Room (Room.roomNumber) · Check-in · Check-out · Total · Status
Booking Table MUST NOT have: Happiness score · AI match score
```

---

### Rule 4 — Search and Filter Rules

Search filters must correspond to entity attributes only.

```
Room Search: roomType · pricingRule.basePrice range (min/max) · capacity · status · propertyId · floorId
Room Search MUST NOT have: Popularity score · AI ranking
```

---

### Rule 5 — Dashboard KPI Rules

Dashboard metrics must come from entity aggregations only.

```
Manager Dashboard KPIs:
  - Total Properties      (COUNT properties)
  - Total Rooms           (COUNT rooms)
  - Available Rooms       (COUNT rooms WHERE status=AVAILABLE)
  - Occupied Rooms        (COUNT rooms WHERE status=OCCUPIED)
  - Bookings Today        (COUNT bookings WHERE checkInDate=today)
  - Check-ins Today       (COUNT bookings WHERE checkInDate=today AND status=CONFIRMED)
  - Check-outs Today      (COUNT bookings WHERE checkOutDate=today AND status=CHECKED_IN)
  - Monthly Revenue       (SUM Payment.amount WHERE status=PAID AND month=current)

Customer Dashboard KPIs:
  - Active Bookings       (COUNT bookings WHERE status in [CONFIRMED, CHECKED_IN])
  - Upcoming Check-in     (nearest Booking.checkInDate)
  - Pending Payments      (COUNT payments WHERE status=PENDING)
  - Open Tickets          (COUNT maintenanceTickets WHERE status=OPEN)
```

---

### Rule 6 — Relationship-Aware UI

Use entity relationships for nested information display:

```
Booking Detail shows: Booking -> Room (roomNumber, roomType) -> Property (name) -> Floor (floorNumber)
                       Booking -> Customer (fullName, email, phone)
                       Booking -> Payment (type, amount, status)
                       Booking -> Contract (pdfUrl, status)
```

---

### Rule 7 — Reusable Component Rule

Never create a new component if an existing one from `component-library.md` can be reused.

Available components:
```
Foundation:    Button.* · Input.* · Select.* · DatePicker.* · Checkbox · Toggle · Badge · Alert · Tooltip · Modal · Drawer · Tabs.* · Pagination · Avatar · FileUpload.*
Business:      RoomCard · BookingCard · ContractCard · PaymentCard · MaintenanceTicketCard · ReviewCard · NotificationCard · ComplaintCard · AvailabilityCalendar · StarRating
Dashboard:     KpiCard · RevenueChart · OccupancyChart · BookingTrendChart
Data Display:  DataTable · Timeline · ActivityFeed · StatisticsPanel · EmptyState
Layout:        PublicLayout · CustomerLayout · ManagerLayout
Form Patterns: CreateForm · EditForm · DetailView · SearchFilterForm · ConfirmationDialog
```

---

### Rule 8 — Design Consistency Rule

All 65 screens must use the **same** token set — no screen-specific overrides:
- Spacing: always from `spacing.*` tokens
- Typography: always from the defined type scale
- Border radius: `rounded.full` on interactive elements, `rounded.md`/`rounded.lg` on cards
- Table pattern: identical header, row, hover, selected state
- Form pattern: identical label, input, helper text, error text layout

---

## DESIGN SYSTEM TOKEN REFERENCE

```
COLORS (from docs/DESIGN.md):

  Brand:
    primary:          #ea2804   (CTAs, hero band, inline links — one per viewport max)
    primary-deep:     #c01f00   (pressed/active state of primary)
    on-primary:       #ffffff   (text on primary surfaces)

  Surface:
    canvas:           #f9f7f3   (EVERY page background — warm cream, never pure white)
    surface-bone:     #f3f0e8   (inset groups, sidebar bands, summary boxes)
    surface-card:     #ffffff   (individual cards, inputs, modals — only white surface)
    surface-dark:     #202020   (Manager sidebar, code wells, dark bands)
    surface-deep:     #000000   (footer only)

  Border:
    hairline:         rgba(32,32,32,0.12)   (1px dividers on cream)
    hairline-strong:  #202020               (focused inputs, structural separators)
    divider-dark:     rgba(255,255,255,0.2) (dividers on dark surfaces)

  Text:
    ink:              #202020   (primary text — warm black)
    body:             #3a3a3a   (body copy)
    charcoal:         #575757   (captions, metadata, secondary nav)
    mute:             #646464   (supporting text, inactive labels)
    ash:              #8d8d8d   (placeholder, tertiary)
    stone:            #bbbbbb   (disabled foreground)
    on-dark:          #fcfcfc   (text on dark surfaces)

  Semantic (status badges):
    success:          #2b9a66   (Available, Paid, Confirmed, Active, Resolved)
    warning:          #F59E0B   (Pending Deposit, In Progress, Pending)
    error:            #DC2626   (Cancelled, Failed, Rejected, Closed)
    info:             #2563EB   (Reserved, Checked-in)
    neutral:          #6B7280   (Maintenance, Occupied)
    purple:           #7C3AED   (Checked-out, Completed)

  Focus:
    ring-focus:       rgba(59,130,246,0.5)  (focus ring on all interactive elements)

TYPOGRAPHY (from docs/DESIGN.md):

  Display (rb-freigeist-neue — substitute: Bricolage Grotesque):
    display-xxl:   128px / 700w / lineHeight 1.0 / tracking -3px    (hero only)
    display-xl:    72px  / 700w / lineHeight 1.0 / tracking -1.8px   (section openers)
    display-lg:    48px  / 700w / lineHeight 1.0 / tracking -1px     (KPI values, page titles)
    display-md:    30px  / 600w / lineHeight 1.2 / tracking -0.5px   (feature card titles)

  Heading (basier-square — substitute: Inter / Geist):
    heading-lg:    38.4px / 600w / lineHeight 0.83 / tracking -0.5px
    heading-md:    24px   / 600w / lineHeight 1.33 / tracking -0.35px  (card titles, modal headers)
    heading-sm:    20px   / 600w / lineHeight 1.4  / tracking -0.3px   (section headers)

  Body (basier-square):
    body-lg:       18px / 400w / lineHeight 1.56    (marketing prose)
    body-md:       16px / 400w / lineHeight 1.5     (default body, form fields)
    body-sm:       14px / 400w / lineHeight 1.43    (captions, metadata)

  Button (basier-square):
    button-md:     16px / 600w / lineHeight 1.0     (default button label)
    button-sm:     14px / 600w / lineHeight 1.0     (compact buttons, pills)

  Other:
    caption:       12px / 400w / lineHeight 1.33    (footer, badge labels)
    caption-tight: 14px / 600w / tracking -0.35px   (emphatic small labels)
    code-md:       14px jetbrains-mono              (Contract IDs, Payment IDs)
    code-sm:       11px jetbrains-mono              (compact code labels)

BORDER RADIUS (from docs/DESIGN.md):
    rounded.none:  0px     (hero bands, full-bleed sections, footer)
    rounded.xs:    4px     (code tabs, small chips)
    rounded.sm:    6px     (calendar cells, small callouts)
    rounded.md:    10px    (cards, code wells, form containers)
    rounded.lg:    16px    (large cards, modals, pricing panels)
    rounded.full:  9999px  (ALL buttons, inputs, badges, avatars, pills)

SPACING (from docs/DESIGN.md):
    xxs: 2px   xs: 4px   sm: 8px   md: 12px   lg: 16px
    xl: 24px   xxl: 32px   xxxl: 48px   section: 96px   band: 160px

SHADOWS:
    shadow-xs:      0 1px 2px rgba(32,32,32,0.06)
    shadow-sm:      0 2px 8px rgba(32,32,32,0.08)     (cards on white)
    shadow-md:      0 4px 16px rgba(32,32,32,0.10)    (dropdowns, popovers)
    shadow-lg:      0 8px 32px rgba(32,32,32,0.12)    (modals, drawers)
    shadow-float:   0 8px 24px rgba(32,32,32,0.08)    (booking panel)
    shadow-primary: 0 4px 16px rgba(234,40,4,0.30)    (focused primary CTA)

LAYOUT:
    Nav height (Public):   60px, background: canvas
    Nav height (Manager):  60px, background: canvas
    Sidebar width:         240px (expanded) / 64px (icon-only), background: surface-dark (#202020)
    Content max-width:     1280px (centered)
    Page background:       ALWAYS canvas (#f9f7f3)

STATUS BADGE MAPPING:
    AVAILABLE          -> Badge.Success  (bg: #dcfce7, text: #2b9a66)
    PENDING_DEPOSIT    -> Badge.Warning  (bg: #fef3c7, text: #d97706)
    CONFIRMED          -> Badge.Success
    RESERVED           -> Badge.Info    (bg: #dbeafe, text: #2563EB)
    CHECKED_IN         -> Badge.Info
    CHECKED_OUT        -> Badge.Purple  (bg: #ede9fe, text: #7C3AED)
    OCCUPIED           -> Badge.Neutral (bg: #f3f4f6, text: #6B7280)
    MAINTENANCE        -> Badge.Neutral
    CANCELLED          -> Badge.Error   (bg: #fee2e2, text: #DC2626)
    PAID               -> Badge.Success
    PENDING (payment)  -> Badge.Warning
    FAILED             -> Badge.Error
    ACTIVE (contract)  -> Badge.Success
    COMPLETED          -> Badge.Purple
    OPEN (ticket)      -> Badge.Warning
    IN_PROGRESS        -> Badge.Info
    RESOLVED           -> Badge.Success
    CLOSED             -> Badge.Neutral
```

---

## ENTITY ATTRIBUTE REFERENCE

> Validate every form field, table column, filter, KPI, and detail view against this reference.

```
User:
  id (UUID) · fullName · email · phone · passwordHash · avatarUrl
  role (GUEST / CUSTOMER / MANAGER / ADMIN / EMPLOYEE) · status (ACTIVE / INACTIVE / SUSPENDED)
  createdAt · updatedAt

Property:
  id (UUID) · name · address · description
  status (ACTIVE / INACTIVE) · createdAt · updatedAt
  Relations: floors[] · rooms[]

Floor:
  id (UUID) · propertyId · floorNumber · description
  createdAt · updatedAt
  Relations: rooms[]

PricingRule:
  id (UUID) · roomId · basePrice · weekendSurcharge
  createdAt · updatedAt

Room:
  id (UUID) · propertyId · floorId · roomNumber · roomType
  area · capacity · description
  status (AVAILABLE / PENDING_DEPOSIT / RESERVED / OCCUPIED / MAINTENANCE)
  createdAt · updatedAt
  Relations: pricingRule · attachments[] · bookings[] · reviews[] · maintenanceTickets[]

Attachment:                                   (replaces old RoomImage / photoUrls fields)
  id (UUID) · entityId · entityType (ROOM / MAINTENANCE_TICKET / DAMAGE_REPORT / PAYMENT_RECEIPT)
  url · type (IMAGE / DOCUMENT) · sortOrder · isPrimary · uploadedAt

Booking:
  id (UUID) · customerId · roomId · checkInDate · checkOutDate · guestCount
  totalAmount · specialRequests
  status (PENDING_DEPOSIT / CONFIRMED / CHECKED_IN / CHECKED_OUT / CANCELLED / NO_SHOW)
  createdAt · updatedAt
  Relations: payments[] · contract · maintenanceTickets[]

Payment:
  id (UUID) · bookingId · customerId
  type (DEPOSIT / REMAINING_BALANCE)
  amount · method (BANK_TRANSFER / CASH / VNPAY)
  status (PENDING / PAID / FAILED)
  paidAt · verifiedBy · verifiedAt · createdAt · updatedAt
  Relations: attachments[] (receipts)

Contract:
  id (UUID) · bookingId · customerId · roomId
  checkInDate · checkOutDate · depositAmount · totalAmount
  pdfUrl · status (ACTIVE / COMPLETED / CANCELLED)
  sentAt · generatedAt · createdAt · updatedAt
  Generated via: OutboxEvent (async PDF generation after deposit confirmed)

MaintenanceTicket:
  id (UUID) · customerId · roomId · title · description
  status (OPEN / ASSIGNED / IN_PROGRESS / RESOLVED / CLOSED)
  assigneeId · createdAt · updatedAt
  Relations: attachments[]

HousekeepingTask:
  id (UUID) · roomId · assigneeId · status (PENDING / IN_PROGRESS / COMPLETED)
  createdAt · updatedAt

RoomInspection:
  id (UUID) · roomId · inspectorId · status (PASS / FAIL)
  checklist (JSON) · notes · createdAt

DamageReport:
  id (UUID) · roomId · reporterId · status (PENDING_REVIEW / APPROVED / ESCALATED / REJECTED)
  totalFee · notes · createdAt · updatedAt
  Relations: items[] (DamageItem) · attachments[]
  Escalation Rule: if totalFee > 5,000,000 VND → status = ESCALATED, requires Admin co-approval

DamageItem:
  id (UUID) · damageReportId · name · estimatedCost

Review:
  id (UUID) · bookingId · customerId · roomId
  rating (1-5) · comment · status (PUBLISHED / HIDDEN)
  createdAt · updatedAt
  Constraint: only 1 review per booking; only after Booking.status=CHECKED_OUT

Notification:
  id (UUID) · userId · isRead
  type (BOOKING_CONFIRMED / CONTRACT_GENERATED / PAYMENT_CONFIRMED / MAINTENANCE_UPDATED / SYSTEM)
  title · content · relatedEntityId · relatedEntityType · createdAt

Complaint:
  id (UUID) · customerId · subject · description
  status (OPEN / INVESTIGATING / RESOLVED / CLOSED)
  resolutionNotes · resolvedAt · createdAt · updatedAt

Promotion:
  id (UUID) · code · discountPercent · startDate · endDate
  Relations: attachments[] (banner images)

ActivityLog:
  id (UUID) · userId · action · entityType · entityId
  description · ipAddress · userAgent · createdAt

SystemSetting:
  id (UUID) · key · value · description · updatedBy · updatedAt
  Keys: DEPOSIT_PERCENTAGE · SYSTEM_NAME · SUPPORT_EMAIL
        SMTP_HOST · SMTP_PORT · SMTP_SENDER
        PAYMENT_METHODS · BANK_ACCOUNT_NUMBER · BANK_ACCOUNT_NAME · BANK_NAME

RefreshToken:
  id · userId · token · expiresAt · revokedAt · createdAt
```

---

## SCREEN PROMPTS

---

## PUBLIC PORTAL (SCR-01 to SCR-10)

---

### SCR-01 — Landing / Home Page

```
Screen: SCR-01 — Landing / Home Page
Actor: Guest
Layout: PublicLayout (PublicHeader + Hero Band + Content sections + Footer)
Entity data: Room (featured) · Property (featured) · Attachment (Room images)

TOP NAVIGATION (nav-bar, 60px, canvas #f9f7f3, 1px hairline bottom):
  Left:   Logo — rb-freigeist-neue wordmark + homestay icon
  Center: Nav links — "Rooms" | "Properties" | "About" (button-sm, charcoal)
  Right:  "Log In" Button.Ghost | "Register" Button.Primary (rounded.full, #ea2804)

HERO BAND (full-bleed, background: hero-warm #ea2804, rounded.none, padding 96px 32px):
  Headline: "Find Your Perfect Stay" — display-xl (72px), rb-freigeist-neue, on-dark #fcfcfc
  Subline:  "Discover and book premium homestay & resort rooms across Vietnam." — body-lg, on-dark
  SEARCH BAR (inline, surface-card bg, rounded.full, shadow-float):
    [Property/Location text input, rounded.full]
    [Check-in DatePicker, rounded.full]
    [Check-out DatePicker, rounded.full]
    [Guests Input.Number, rounded.full]
    ["Search" Button.Primary, rounded.full, #ea2804]

FEATURED ROOMS (canvas bg, padding-top: section 96px):
  Section title: "Featured Rooms" — heading-md, ink
  Room Cards grid — 4-col desktop / 2-col tablet / 1-col mobile:
    RoomCard fields from entity:
      imageUrl:     Attachment.url (isPrimary=true, entityType=ROOM), 1:1 square, rounded.md top
      status badge: Room.status (Badge.Success/Warning/Info/Neutral)
      property:     Property.name — body-sm, charcoal
      name:         Room.roomNumber + Room.roomType — heading-sm, ink
      stats:        Room.capacity | Room.area (m²) — body-sm, charcoal
      price:        Room.pricingRule.basePrice + "/night" — heading-sm, primary #ea2804
      button:       "View Detail" Button.Outline, rounded.full
  Card hover: translateY(-2px), shadow-md

FEATURED PROPERTIES (surface-bone #f3f0e8 bg band, padding 64px 0):
  Section title: "Our Properties" — heading-md, ink
  3-col card grid: Property.name · Property.address · room count

FOOTER (surface-deep #000000, rounded.none, padding 64px 32px):
  4-col: About | Quick Links | Contact | Social
  Bottom divider: divider-dark rgba(255,255,255,0.2)
  Copyright: caption, on-dark-mute
```

---

### SCR-02 — Login

```
Screen: SCR-02 — Login
Actor: Guest
Layout: Auth (centered on canvas #f9f7f3, no sidebar)
Entity: User (read by email for auth) · RefreshToken (created on success)

CARD (surface-card, rounded.lg 16px, shadow-lg, 480px wide, padding: xxl 32px):
  Logo centered
  Title:    "Welcome back" — display-md, rb-freigeist-neue
  Subtitle: "Sign in to your account" — body-md, charcoal

  FORM:
    Email    input -> User.email    (rounded.full, 44px height)
    Password input -> User.passwordHash (show/hide Button.Icon, rounded.full)
    Row: "Remember me" Checkbox left | "Forgot password?" link (color-primary) right
    "Sign In" Button.Primary full-width (rounded.full, 44px, #ea2804)

  Footer: "Don't have an account?" + "Register" link (color-primary) -> SCR-03

  VALIDATION STATES:
    Empty field:          input border error #DC2626 + body-sm error text below
    Wrong credentials:    Alert.Error "Invalid email or password"
    Account suspended:    Alert.Error "Your account has been suspended"
    Email not verified:   Alert.Warning "Please verify your email first"
```

---

### SCR-03 — Register

```
Screen: SCR-03 — Register
Actor: Guest
Layout: Auth, 520px card
Entity: User (created) — fields: fullName · email · phone · passwordHash · role · status

CARD (surface-card, rounded.lg, shadow-lg, padding: xxl):
  Title: "Create your account" — display-md, rb-freigeist-neue

  FORM:
    Full Name input    -> User.fullName  (required, rounded.full)
    Email input        -> User.email     (required, unique, email format, rounded.full)
    Phone input        -> User.phone     (rounded.full)
    Password input     -> User.passwordHash (strength meter: Weak / Fair / Strong, rounded.full)
    Confirm Password input (rounded.full)
    Terms checkbox (required to enable submit)

  "Create Account" Button.Primary full-width (rounded.full, #ea2804)

  On success: User.status = INACTIVE -> redirect to SCR-04 (OTP verification)
```

---

### SCR-04 — OTP / Email Verification

```
Screen: SCR-04 — OTP Verification
Actor: Guest (just registered)
Layout: Auth, 440px card
Entity: User — updates User.status from INACTIVE -> ACTIVE on success

CARD (surface-card, rounded.lg, shadow-lg, padding: xxl):
  Icon: email icon (48px, lighter primary bg, rounded.full)
  Title:    "Verify your email" — heading-md
  Subtitle: "We sent a 6-digit code to [User.email]" — body-md, charcoal

  6 OTP input boxes in a row (52x60px each, jetbrains-mono font, auto-focus)
  "Verify Email" Button.Primary full-width (rounded.full, #ea2804)
  "Resend code" Button.Ghost with 60s countdown

  SUCCESS: Alert.Success "Email verified! You can now sign in."
  ERROR:   Alert.Error   "Invalid or expired code. Please request a new one."
```

---

### SCR-05 — Forgot Password

```
Screen: SCR-05 — Forgot Password
Actor: Guest
Layout: Auth, 440px card
Entity: User (read by User.email)

CARD (surface-card, rounded.lg, shadow-lg, padding: xxl):
  Icon: lock icon (48px, lighter primary bg, rounded.full)
  Title:    "Forgot your password?" — heading-md
  Subtitle: "Enter your registered email to receive a reset link." — body-md, charcoal

  FORM:
    Email input -> User.email (rounded.full)
  "Send Reset Link" Button.Primary full-width (rounded.full, #ea2804)
  "Back to Login" Button.Ghost (left arrow icon)

  SUCCESS: Checkmark icon (green circle) + "Check your inbox" + "Resend in 5 min" countdown
```

---

### SCR-06 — Reset Password

```
Screen: SCR-06 — Reset Password
Actor: Guest
Layout: Auth, 440px card
Entity: User (updates User.passwordHash) · RefreshToken (all revoked on success)

CARD (surface-card, rounded.lg, shadow-lg, padding: xxl):
  Icon: shield icon (lighter primary bg, rounded.full)
  Title: "Set new password" — heading-md

  FORM:
    New Password input     -> User.passwordHash (strength meter, rounded.full)
    Confirm Password input (rounded.full)
    Checklist: "At least 8 characters" | "One uppercase letter" | "One number"

  "Reset Password" Button.Primary full-width (rounded.full, #ea2804)
  SUCCESS: Alert.Success "Password reset successfully. Redirecting to login..."
  NOTE:    All RefreshToken records for this user have revokedAt set on success.
```

---

### SCR-07 — Room Listing

```
Screen: SCR-07 — Room Listing
Actor: Guest / Customer
Layout: PublicLayout, 2-column (sidebar + main)
Entity: Room · Property · Floor · Attachment

FILTER SIDEBAR (280px, surface-card, rounded.md, shadow-sm, padding: xl):
  Title: "Filter Rooms" — heading-sm
  All filters map directly to entity attributes:
    Property         -> Select.Searchable (Property.name/id)
    Room Type        -> Checkbox group (Room.roomType: Studio / Standard / Deluxe / Suite / Villa)
    Price Range      -> dual Input.Number Min/Max (Room.pricingRule.basePrice)
    Guests           -> Input.Number (Room.capacity)
    Check-in Date    -> DatePicker.Single
    Check-out Date   -> DatePicker.Single
  "Search" Button.Primary (rounded.full, #ea2804)
  "Clear All" Button.Ghost

MAIN CONTENT (flex-1):
  Toolbar: "X rooms found" | Sort Select (pricingRule.basePrice ASC/DESC | newest)
  RoomCard grid (4-col desktop / 2-col tablet / 1-col mobile):
    Entity fields per card: Attachment.url (isPrimary, entityType=ROOM) · Room.status badge · Property.name
    Room.roomNumber · Room.roomType · Room.capacity · Room.area · Room.pricingRule.basePrice
    Button: "View Detail" Button.Outline (rounded.full) -> SCR-08

  Pagination (centered, bottom)
  EmptyState: magnifier icon + "No rooms match your filters" + "Clear Filters" Button.Outline
```

---

### SCR-08 — Room Detail

```
Screen: SCR-08 — Room Detail
Actor: Guest / Customer
Layout: PublicLayout, max-width 1280px
Entity: Room · Property · Floor · Attachment · Review · Booking (availability)

BREADCRUMB: Home > Rooms > [Property.name] > [Room.roomNumber] (body-sm, primary links)

IMAGE GALLERY:
  Primary: Attachment.url (isPrimary=true, entityType=ROOM), large, rounded.md, 2/3 width
  Thumbnails: remaining Attachments sorted by Attachment.sortOrder (2x2 grid)
  "View all photos" overlay Button.Ghost

TWO COLUMNS (60% left / 40% right):

LEFT COLUMN:
  Title:         Room.roomNumber + " - " + Room.roomType — display-md, rb-freigeist-neue
  Property row:  Property.name · Property.address — body-sm, charcoal
  Status badge:  Room.status (Badge.*)
  Floor:         Floor.floorNumber — body-sm, charcoal

  Stats row: Room.capacity (people) | Room.area (m²) | Room.pricingRule.basePrice/night — body-md

  Description: Room.description — body-lg, body #3a3a3a
  Divider (hairline)

  AVAILABILITY CALENDAR (AvailabilityCalendar component):
    Source data: Booking.checkInDate/checkOutDate by status:
      PENDING_DEPOSIT -> Amber cells
      CONFIRMED/CHECKED_IN -> Blue cells
      OCCUPIED -> Red cells
      MAINTENANCE -> Gray cells
      Available dates -> Green cells
    Legend: Badge.Tag chips (Available | Pending | Reserved | Occupied | Maintenance)

  REVIEWS (Review.status = PUBLISHED only):
    Average Rating: aggregated Review.rating — display-md, ink
    StarRating display (5 stars, primary #ea2804 filled)
    Review count — body-sm, charcoal
    ReviewCard list:
      Avatar (User avatarUrl, rounded.full, 32px) | User.fullName | Review.createdAt (relative)
      StarRating (5 stars, 16px)
      Review.comment — body-md, 4-line truncate

RIGHT COLUMN (sticky, surface-card, rounded.lg, shadow-float, padding: xxl):
  Price: Room.pricingRule.basePrice + "/night" — display-md, primary #ea2804
  Weekend surcharge note: if applicable, Room.pricingRule.weekendSurcharge/night (body-sm, warning)
  Stats: Room.area | Room.capacity
  Check-in DatePicker (rounded.full)
  Check-out DatePicker (rounded.full)
  Guests Input.Number (rounded.full)
  Price breakdown: nights x pricingRule.basePrice (+weekendSurcharge if applicable) = totalAmount
  Deposit preview: 40% of total (from SystemSetting.DEPOSIT_PERCENTAGE)
  "Book Now" Button.Primary full-width (rounded.full, #ea2804) -> SCR-17
  Room.status != AVAILABLE: show Alert.Warning "This room is currently unavailable"
```

---

### SCR-09 — Search Results

```
Screen: SCR-09 — Search Results
Actor: Guest / Customer
Layout: PublicLayout, same structure as SCR-07
Entity: Room · Property · Floor · Attachment

Active filter chips above results (Badge.Tag with x):
  Each chip: entity attribute + value: "Deluxe x" | "< 500K/night x"
  "Clear all" Button.Ghost

Results grid identical to SCR-07 RoomCard grid.
EmptyState: "No rooms found for your search. Try adjusting your filters."
```

---

### SCR-10 — Availability Calendar

```
Screen: SCR-10 — Availability Calendar
Actor: Guest / Customer
Layout: PublicLayout
Entity: Room · Booking (occupancy dates)

Page title: "[Room.roomNumber] — [Property.name]" — heading-md
Room info bar: Room.roomType | Room.pricingRule.basePrice/night | Room.capacity | Room.area

FULL-SIZE CALENDAR (AvailabilityCalendar component, read-only):
  Displays 2 months side-by-side on desktop, 1 month on mobile
  Cell colors by Booking.status (same mapping as SCR-08)
  Legend: Available | Pending Deposit | Reserved | Occupied | Maintenance

"Book This Room" Button.Primary (rounded.full, #ea2804) -> SCR-17 (if Room.status=AVAILABLE)
```

---

## SHARED SCREENS (SCR-11 to SCR-15)

---

### SCR-11 — User Profile

```
Screen: SCR-11 — User Profile
Actor: Customer / Manager
Layout: CustomerLayout or ManagerLayout (role-determined)
Entity: User (read own record)

PROFILE HEADER CARD (surface-card, rounded.lg, shadow-sm, padding: xxl):
  Left:  Avatar (User.avatarUrl, 80px, rounded.full) + fallback initials (lighter primary bg)
  Right:
    User.fullName — heading-md, ink
    User.role Badge (Customer: Badge.Info | Manager: Badge.Warning)
    "Member since " + User.createdAt — body-sm, charcoal
  Top-right: "Edit Profile" Button.Outline (rounded.full) -> SCR-12

INFO CARDS (surface-card, rounded.md, shadow-sm, 24px gap):
  PERSONAL INFO:
    Full Name: User.fullName
    Email:     User.email
    Phone:     User.phone
  ACCOUNT INFO:
    Role:         User.role (Badge)
    Status:       User.status (Badge)
    Member Since: User.createdAt
    Last Updated: User.updatedAt
  SECURITY:
    "Change Password" Button.Ghost -> SCR-13
```

---

### SCR-12 — Edit Profile

```
Screen: SCR-12 — Edit Profile
Actor: Customer / Manager
Layout: CustomerLayout or ManagerLayout
Entity: User — mutable fields only

FORM CARD (surface-card, rounded.md, shadow-sm, padding: xxl, max-width 640px):
  Avatar: User.avatarUrl preview (80px, rounded.full) + FileUpload.Button "Upload photo"
  Full Name input  -> User.fullName (required, rounded.full)
  Phone input      -> User.phone (rounded.full)
  Email (read-only display, "Email cannot be changed" caption)
  Role  (read-only Badge display)

  NOT EDITABLE HERE: id · passwordHash · status · createdAt · updatedAt

  ACTION BAR:
    "Save Changes" Button.Primary (rounded.full, #ea2804)
    "Cancel" Button.Ghost -> SCR-11
```

---

### SCR-13 — Change Password

```
Screen: SCR-13 — Change Password
Actor: Customer / Manager
Layout: CustomerLayout or ManagerLayout
Entity: User.passwordHash · RefreshToken (all revoked on success)

FORM CARD (surface-card, rounded.md, padding: xxl, max-width 480px):
  Title: "Change Password" — heading-md

  Current Password input  -> validates against User.passwordHash (rounded.full)
  New Password input      -> User.passwordHash (strength meter, rounded.full)
  Confirm Password input  (rounded.full)

  "Update Password" Button.Primary (rounded.full, #ea2804)

  SUCCESS: Alert.Success "Password updated. Other sessions have been signed out."
  ERROR:   Alert.Error   "Current password is incorrect"
  NOTE:    All RefreshToken records revoked on success (security requirement).
```

---

### SCR-14 — Notification Center

```
Screen: SCR-14 — Notification Center
Actor: Customer / Manager
Layout: CustomerLayout or ManagerLayout
Entity: Notification (Notification.userId = current user)

HEADER ROW:
  "Notifications" — heading-md
  Unread count Badge.Error (pill, rounded.full) if > 0
  "Mark all as read" Button.Ghost -> sets all Notification.isRead = true

TABS (Tabs.Pill):
  "All" | "Unread" (filter by Notification.isRead = false)

NOTIFICATION LIST (surface-card, rounded.md, divide-y hairline):
  Each NotificationCard row:
    Left:   Category icon circle (24px, rounded.full, colored by Notification.type)
    Center: Notification.title (body-md, weight 600 if isRead=false)
            Notification.content (2-line truncate, body-sm, charcoal)
    Right:  Relative timestamp from Notification.createdAt (caption, mute)
            Unread dot (8px, primary #ea2804, rounded.full) if isRead=false
  Unread card bg: surface-bone, 4px left border primary #ea2804
  Click -> SCR-15 + set Notification.isRead = true

EmptyState: bell icon + "You are all caught up"
Pagination (bottom, if > 10)
```

---

### SCR-15 — Notification Detail

```
Screen: SCR-15 — Notification Detail
Actor: Customer / Manager
Layout: CustomerLayout or ManagerLayout
Entity: Notification — Notification.isRead set to true on view

Breadcrumb: Notifications > [Notification.title]

DETAIL CARD (surface-card, rounded.md, shadow-sm, padding: xxl):
  Category icon (40px, rounded.full)
  Title:    Notification.title — heading-md
  Received: Notification.createdAt — body-sm, charcoal
  Content:  Notification.content — body-lg, full text

  Context actions based on Notification.type:
    BOOKING_CONFIRMED  -> "View Booking" Button.Outline -> SCR-19
    CONTRACT_GENERATED -> "View Contract" Button.Outline -> SCR-26
    PAYMENT_CONFIRMED  -> "View Payment" Button.Outline -> SCR-23
    MAINTENANCE_UPDATED -> "View Ticket" Button.Outline -> SCR-29

"Back to Notifications" Button.Ghost (left arrow) -> SCR-14
```

---

## CUSTOMER PORTAL (SCR-16 to SCR-31)

---

### SCR-16 — Customer Dashboard

```
Screen: SCR-16 — Customer Dashboard
Actor: Customer
Layout: CustomerLayout (topbar + content, max-width 1280px, canvas bg)
Entity: Booking · Payment · MaintenanceTicket · Notification

GREETING: "Good morning, [User.fullName]" — heading-md, ink

KPI ROW (4 KpiCards, 1/4 width, surface-card, rounded.lg, shadow-sm):
  1. Active Bookings
     Value: COUNT Booking WHERE status IN (CONFIRMED, CHECKED_IN)
     Color: primary #ea2804 if > 0, success if > 0
  2. Upcoming Check-in
     Value: nearest Booking.checkInDate WHERE status=CONFIRMED — formatted date
     Warning: if < 3 days away (Badge.Warning)
  3. Pending Payments
     Value: COUNT Payment WHERE status=PENDING
     Color: error if > 0, success if 0
  4. Open Tickets
     Value: COUNT MaintenanceTicket WHERE status IN (OPEN, IN_PROGRESS)
     Color: warning if > 0, success if 0

RECENT BOOKINGS (surface-card, rounded.md, shadow-sm):
  Title: "My Bookings" + "View All" Button.Ghost -> SCR-18
  Last 3 BookingCards:
    Booking.id (code-md) | Room.roomNumber | checkInDate -> checkOutDate | Booking.status Badge | Booking.totalAmount

RECENT PAYMENTS (surface-card, rounded.md):
  Title: "Recent Payments" + "View All" Button.Ghost -> SCR-23
  Last 3: Payment.type badge | Payment.amount | Payment.status badge | Payment.createdAt

MAINTENANCE TICKETS (surface-card, rounded.md):
  Title: "My Tickets" + "View All" Button.Ghost -> SCR-27
  Last 3 MaintenanceTicketCards: title | Room.roomNumber | status badge | createdAt

NOTIFICATIONS (surface-card, rounded.md):
  Title: "Notifications"
  5 NotificationCards (compact): title (bold if unread) | relative timestamp
```

---

### SCR-17 — Booking Form

```
Screen: SCR-17 — Booking Form
Actor: Customer
Layout: CustomerLayout
Entity: Booking (created) · Room (read)
Fields created: customerId · roomId · checkInDate · checkOutDate · guestCount · totalAmount · specialRequests

PAGE TITLE: "Book a Room" — heading-md
Breadcrumb: Home > [Property.name] > [Room.roomNumber] > Book

TWO COLUMNS (60% form / 40% summary):

LEFT — FORM CARD (surface-card, rounded.md, padding: xxl):
  Room.roomNumber + Room.roomType (read-only display at top)
  Check-in DatePicker.Single  -> Booking.checkInDate  (rounded.full, required)
  Check-out DatePicker.Single -> Booking.checkOutDate (rounded.full, required)
  Validation: checkOutDate must be after checkInDate
  Number of Guests Input.Number -> Booking.guestCount (max: Room.capacity, rounded.full)
  Special Requests Textarea -> Booking.specialRequests (rounded.md, optional, max 500 chars)

RIGHT — PRICE SUMMARY CARD (surface-card, rounded.lg, shadow-float, padding: xxl):
  Room.roomNumber + Room.roomType
  Room.pricingRule.basePrice + "/night"
  Nights: (checkOutDate - checkInDate) days
  Room Rate: nights x pricingRule.basePrice (+weekendSurcharge if applicable)
  Total Amount: Booking.totalAmount
  Deposit (40%): Booking.totalAmount x 0.40 (from SystemSetting.DEPOSIT_PERCENTAGE)
  Balance (60%): Booking.totalAmount x 0.60

  "Confirm Booking" Button.Primary full-width (rounded.full, #ea2804)
  Alert.Info: "You will pay the 40% deposit to confirm your booking."
  Alert.Warning: "If you cancel before check-in, deposit refund depends on days left: ≥7d 100% / 3–7d 50% / <3d 0%."

On success: Booking.status = PENDING_DEPOSIT -> redirect to SCR-21 (Deposit Payment)
```

---

### SCR-18 — Booking List

```
Screen: SCR-18 — Booking List
Actor: Customer
Layout: CustomerLayout
Entity: Booking (Customer's own)

PAGE TITLE: "My Bookings" — heading-md

TABS (Tabs.Pill):
  "All" | "Pending" | "Confirmed" | "Checked-in" | "Checked-out" | "Cancelled"
  Filter by Booking.status

BookingCard list (stacked):
  Each card: Booking.id (code-md, caption) | Room thumbnail (60px, rounded.md)
  Room.roomNumber | Property.name | checkInDate -> checkOutDate
  Booking.totalAmount | Booking.status Badge
  "View Detail" Button.Ghost -> SCR-19

Pagination (bottom)
EmptyState: calendar icon + "No bookings yet" + "Browse Rooms" Button.Outline -> SCR-07
```

---

### SCR-19 — Booking Detail

```
Screen: SCR-19 — Booking Detail
Actor: Customer
Layout: CustomerLayout
Entity: Booking · Room · Property · Payment · Contract

Breadcrumb: My Bookings > [Booking.id]

TWO COLUMNS (65% main / 35% sidebar):

LEFT — DETAILS:
  Title: "Booking #" + Booking.id (code-md) — heading-md
  Status Badge (prominent, large): Booking.status

  INFO SECTION (key-value pairs, body-md):
    Room:      Room.roomNumber + " - " + Room.roomType
    Property:  Property.name
    Check-in:  Booking.checkInDate (formatted)
    Check-out: Booking.checkOutDate (formatted)
    Guests:    Booking.guestCount
    Requests:  Booking.specialRequests

  PAYMENT SECTION:
    Deposit (40%): Payment where type=DEPOSIT — amount | status Badge | paidAt
    Balance (60%): Payment where type=REMAINING_BALANCE — amount | status Badge | paidAt
    Total:         Booking.totalAmount

  CONTRACT SECTION (if exists):
    Contract.id (code-md) | Contract.status Badge
    "Download PDF" Button.Outline -> GET /api/contracts/{id}/pdf
    "View Contract" Button.Ghost -> SCR-26

  TIMELINE (Timeline component):
    Booking Created | Deposit Paid | Booking Confirmed | Contract Sent | Checked-in | Checked-out

RIGHT SIDEBAR CARD (surface-card, rounded.lg, shadow-sm, padding: xl):
  Context-aware actions based on Booking.status:
    PENDING_DEPOSIT: "Pay Deposit Now" Button.Primary -> SCR-21
    CONFIRMED: "Pay Balance" Button.Primary -> SCR-22 | "Cancel Booking" Button.Danger -> SCR-20
    CHECKED_IN: no action
    CHECKED_OUT: "Write Review" Button.Primary -> SCR-30 (if no review yet)
    CANCELLED: Alert.Error "Booking cancelled. Deposit refund: by tier if cancelled by you (≥7d/3–7d/<3d); 100% deposit if cancelled by the property."
```

---

### SCR-20 — Booking Cancellation

> **ID note:** Canonical cancel screen is **SCR-19** (`screen.md`). Treat this block as cancellation UX; do not invent a second cancel ID.

```
Screen: SCR-19 — Booking Cancellation (legacy label SCR-20 in older drafts)
Actor: Customer
Layout: CustomerLayout
Entity: Booking (update status -> CANCELLED)

ConfirmationDialog (Modal.sm, 400px):
  Warning icon (40px, error bg, rounded.full)
  Title: "Cancel Booking?" — heading-md
  Description:
    "Are you sure you want to cancel booking #[Booking.id]?"
    "Check-in: [Booking.checkInDate] | Room: [Room.roomNumber]"
    Alert.Warning: "Deposit refund by days until check-in: ≥7d 100% / 3–7d 50% / <3d 0%. This cannot be undone."
    Refund Preview: "Refund Amount: [depositAmount x tierPercent]"
  Confirm: "Yes, Cancel Booking" Button.Danger (rounded.full, #DC2626)
  Cancel:  "Go Back" Button.Outline (rounded.full)

Only available when Booking.status = CONFIRMED or PENDING_DEPOSIT (before check-in)
On success: Booking.status -> CANCELLED | Room inventory released
```
---

### SCR-21 — Deposit Payment

```
Screen: SCR-21 — Deposit Payment
Actor: Customer
Layout: CustomerLayout
Entity: Payment (created, type=DEPOSIT) · PaymentReceipt (created) · Booking (read)

PAGE TITLE: "Pay Deposit" — heading-md

TWO COLUMNS:

LEFT — PAYMENT FORM (surface-card, rounded.md, padding: xxl):
  Title: "Select Payment Method"
  Payment Method (Radio.Card, 3 options):
    Bank Transfer  -> Payment.method = BANK_TRANSFER
      Bank name: [SystemSetting.BANK_NAME]
      Account: [SystemSetting.BANK_ACCOUNT_NUMBER] | Name: [SystemSetting.BANK_ACCOUNT_NAME]
      Transfer reference: Booking.id (code-md, copy button)
    E-Wallet       -> Payment.method = E_WALLET
    Cash           -> Payment.method = CASH (Pay at reception)

  RECEIPT UPLOAD (FileUpload.DropZone):
    Title: "Upload Payment Receipt"
    Accepted: image/*, .pdf, max 10MB
    -> creates PaymentReceipt record

  "Confirm Payment" Button.Primary full-width (rounded.full, #ea2804)
  -> creates Payment record (type=DEPOSIT, status=PENDING)

RIGHT — SUMMARY CARD (surface-card, rounded.lg, shadow-float, padding: xl):
  Room.roomNumber + Room.roomType
  Check-in: Booking.checkInDate
  Check-out: Booking.checkOutDate
  Total: Booking.totalAmount
  Deposit Amount (40%): highlighted in primary #ea2804, large — display-md
  Alert.Info: "Your booking will be confirmed after Manager verifies your payment."
```

---

### SCR-22 — Remaining Balance Payment

```
Screen: SCR-22 — Remaining Balance Payment
Actor: Customer
Layout: CustomerLayout
Entity: Payment (created, type=REMAINING_BALANCE) · PaymentReceipt (created)

Same structure as SCR-21 but:
  Title: "Pay Remaining Balance"
  Amount: Booking.totalAmount x 0.60 — display-md, primary #ea2804
  Alert.Info: "Pay before or at check-in to complete your booking."
```

---

### SCR-23 — Payment History

```
Screen: SCR-23 — Payment History
Actor: Customer
Layout: CustomerLayout
Entity: Payment (Customer's own)

PAGE TITLE: "Payment History" — heading-md

TABS (Tabs.Pill): "All" | "Deposit" | "Balance" | "Pending" | "Paid" | "Failed"

PaymentCard list:
  Payment.id (code-md) | Payment.type Badge | Payment.method | Payment.amount (heading-sm, primary)
  Payment.status Badge | Payment.createdAt
  "View Receipt" Button.Ghost (if PaymentReceipt exists)

Pagination
EmptyState: credit card icon + "No payment history yet"
```

---

### SCR-24 — Receipt Upload

```
Screen: SCR-24 — Receipt Upload
Actor: Customer
Layout: CustomerLayout
Entity: PaymentReceipt (create/replace)

PAGE TITLE: "Upload Payment Receipt" — heading-md
Context: [Payment.type] for Booking #[Booking.id]

UPLOAD CARD (surface-card, rounded.md, padding: xxl, max-width 560px):
  Current receipt preview (if exists): thumbnail (100px, rounded.md) + filename + uploadedAt
  FileUpload.DropZone:
    "Drag and drop your receipt here, or click to browse"
    Accepted: JPG, PNG, WebP, PDF — max 10MB
    Shows preview on select
  Alert.Info: "Accepted formats: JPG, PNG, WebP, PDF. Max size: 10MB."

  "Submit Receipt" Button.Primary (rounded.full, #ea2804)
  "Cancel" Button.Ghost -> SCR-23
```

---

### SCR-25 — My Contract List

```
Screen: SCR-25 — My Contract List
Actor: Customer
Layout: CustomerLayout
Entity: Contract (Customer's own)

PAGE TITLE: "My Contracts" — heading-md

TABS (Tabs.Pill): "All" | "Active" | "Completed" | "Cancelled"

ContractCard list:
  Contract.id (code-md) | Contract.status Badge
  Room.roomNumber | Property.name
  Check-in: Contract.checkInDate | Check-out: Contract.checkOutDate
  Deposit: Contract.depositAmount | Total: Contract.totalAmount
  "View Detail" Button.Ghost -> SCR-26
  "Download PDF" Button.Outline -> GET /api/contracts/{id}/pdf

Pagination
EmptyState: document icon + "No contracts yet"
```

---

### SCR-26 — Contract Detail

```
Screen: SCR-26 — Contract Detail
Actor: Customer
Layout: CustomerLayout
Entity: Contract · Room · Property · Booking

Breadcrumb: My Contracts > [Contract.id]

DETAIL CARD (surface-card, rounded.md, shadow-sm, padding: xxl):
  Contract.id (code-md) | Contract.status Badge
  Room:       Room.roomNumber + " - " + Room.roomType
  Property:   Property.name
  Check-in:   Contract.checkInDate
  Check-out:  Contract.checkOutDate
  Deposit:    Contract.depositAmount
  Total:      Contract.totalAmount
  Generated:  Contract.generatedAt
  Sent at:    Contract.sentAt

  PDF VIEWER EMBED or "Download PDF" Button.Outline -> GET /api/contracts/{id}/pdf
  "Print" Button.Ghost (browser print)
```

---

### SCR-27 — Maintenance Ticket List

```
Screen: SCR-27 — Maintenance Ticket List
Actor: Customer
Layout: CustomerLayout
Entity: MaintenanceTicket (Customer's own)

PAGE TITLE: "Maintenance Requests" — heading-md

"New Request" Button.Primary top-right (rounded.full, #ea2804) -> SCR-28

TABS (Tabs.Pill): "All" | "Open" | "In Progress" | "Resolved" | "Closed"

MaintenanceTicketCard list:
  Ticket.id (code-md, caption) | Ticket.title (heading-sm, 2-line max)
  Room.roomNumber | Ticket.status Badge | Ticket.createdAt (body-sm, charcoal)
  "View Detail" Button.Ghost -> SCR-29

Pagination
EmptyState: wrench icon + "No maintenance requests yet" + "New Request" Button.Outline
```

---

### SCR-28 — Create Maintenance Ticket

```
Screen: SCR-28 — Create Maintenance Ticket
Actor: Customer
Layout: CustomerLayout
Entity: MaintenanceTicket (created)
Fields created: customerId · roomId · title · description · status=OPEN
Relations created: attachments[] (optional photos)

PAGE TITLE: "Submit Maintenance Request" — heading-md

FORM CARD (surface-card, rounded.md, padding: xxl, max-width 640px):
  Room selector  -> MaintenanceTicket.roomId
    Select.Searchable (Customer's booked rooms only)
  Title input    -> MaintenanceTicket.title (required, max 200 chars, rounded.full)
  Description Textarea -> MaintenanceTicket.description (required, min 20 chars, rounded.md)
  Photo Upload   -> Attachment (entityType=MAINTENANCE_TICKET)
    FileUpload.DropZone (optional, max 5 images, 10MB each)

  "Submit Request" Button.Primary (rounded.full, #ea2804)
  "Cancel" Button.Ghost -> SCR-27

On success: Ticket.status = OPEN | Alert.Success "Your maintenance request has been submitted."
```

---

### SCR-29 — Maintenance Ticket Detail

```
Screen: SCR-29 — Maintenance Ticket Detail
Actor: Customer
Layout: CustomerLayout
Entity: MaintenanceTicket · Room

Breadcrumb: Maintenance > [Ticket.title]

DETAIL CARD (surface-card, rounded.md, shadow-sm, padding: xxl):
  Ticket.id (code-md) | Ticket.status Badge (large, prominent)
  Title:       Ticket.title — heading-md
  Room:        Room.roomNumber | Property.name
  Description: Ticket.description — body-md
  Submitted:   Ticket.createdAt

  PHOTOS (if attachments exist, entityType=MAINTENANCE_TICKET):
    Photo grid (3-col, 120px thumbnails, rounded.md)

  TIMELINE (status changes):
    OPEN created | IN_PROGRESS started | RESOLVED | CLOSED

"Back" Button.Ghost -> SCR-27
```

---

### SCR-30 — Review & Rating

```
Screen: SCR-30 — Review & Rating
Actor: Customer
Layout: CustomerLayout
Entity: Review (created)
Precondition: Booking.status = CHECKED_OUT | no existing Review for this Booking

PAGE TITLE: "Write a Review" — heading-md

CONTEXT BANNER:
  Room.roomNumber + Room.roomType | Property.name
  Stay: Booking.checkInDate -> Booking.checkOutDate

REVIEW FORM CARD (surface-card, rounded.md, padding: xxl, max-width 640px):
  Star Rating INPUT (5 stars, 40px each, primary #ea2804 filled):
    -> Review.rating (1-5, required)
    Selected label: "1-Terrible | 2-Poor | 3-Average | 4-Good | 5-Excellent"
  Review Comment Textarea -> Review.comment (min 20 chars, max 1000 chars, rounded.md)
  Character counter bottom-right

  "Submit Review" Button.Primary (rounded.full, #ea2804)
  Alert.Info: "You can submit one review per stay."

On success: Review.status = PUBLISHED | Alert.Success "Thank you for your review!"
```

---

### SCR-31 — My Reviews

```
Screen: SCR-31 — My Reviews
Actor: Customer
Layout: CustomerLayout
Entity: Review (Customer's own)

PAGE TITLE: "My Reviews" — heading-md

ReviewCard list:
  Room.roomNumber | Property.name | Stay dates
  StarRating display (5 stars, 20px, primary #ea2804 filled)
  Review.rating numeric | Review.createdAt
  Review.comment — body-md, 4-line max, "Read more" toggle

Pagination
EmptyState: star icon + "No reviews yet. Reviews unlock after check-out."
```

---

## MANAGER PORTAL (SCR-32 to SCR-65)

---

### SCR-32 — Manager Dashboard

```
Screen: SCR-32 — Manager Dashboard
Actor: Manager
Layout: ManagerLayout (surface-dark sidebar + canvas topbar + content)
Entity: Property · Room · Booking · Payment · MaintenanceTicket

GREETING: "Dashboard" — heading-md, ink (topbar breadcrumb)

KPI ROW 1 (4 KpiCards):
  1. Total Properties — COUNT(properties)
  2. Total Rooms      — COUNT(rooms)
  3. Available Rooms  — COUNT(rooms WHERE status=AVAILABLE) — value: success color
  4. Occupied Rooms   — COUNT(rooms WHERE status=OCCUPIED)

KPI ROW 2 (4 KpiCards):
  5. Bookings This Month — COUNT(bookings WHERE createdAt in current month)
  6. Check-ins Today     — COUNT(bookings WHERE checkInDate=today AND status=CONFIRMED)
  7. Check-outs Today    — COUNT(bookings WHERE checkOutDate=today AND status=CHECKED_IN)
  8. Monthly Revenue     — SUM(Payment.amount WHERE status=PAID AND month=current) — value: primary #ea2804

CHARTS ROW (surface-card, rounded.lg, shadow-sm):
  Left (2/3): RevenueChart — Line chart, monthly revenue, primary #ea2804 line
  Right (1/3): OccupancyChart — Donut chart
    Available (success green) | Occupied (primary) | Maintenance (neutral gray)
    Center: Occupancy % — display-md

RECENT BOOKINGS TABLE (surface-card, rounded.lg, shadow-sm):
  Title: "Recent Bookings" + "View All" Button.Ghost -> SCR-45
  Columns: Booking.id (code-md) | Customer (Avatar.xs + fullName) | Room | checkInDate | checkOutDate | totalAmount | status Badge | Actions
  5 most recent rows

BOOKING TREND CHART (surface-card, rounded.lg):
  BookingTrendChart — Bar chart, new bookings per week
  Colors: Bookings primary #ea2804 | Cancellations error #DC2626
```

---

### SCR-33 — Property List

```
Screen: SCR-33 — Property List
Actor: Manager
Layout: ManagerLayout
Entity: Property

Breadcrumb: Property > List

TOOLBAR:
  "Add Property" Button.Primary top-right (rounded.full, #ea2804) -> SCR-35
  Input.Search (rounded.full) — search by Property.name, Property.address

DataTable (Property):
  Columns: Property.name (link -> SCR-34) | Property.address | Room count | status Badge | createdAt | Actions
  Actions dropdown: View | Edit (-> SCR-36) | Delete (ConfirmationDialog)
  Sort: name, createdAt
  Filter: status (Active/Inactive)

Pagination
EmptyState: building icon + "No properties yet. Add your first property." + Button.Primary "Add Property"
```

---

### SCR-34 — Property Detail

```
Screen: SCR-34 — Property Detail
Actor: Manager
Layout: ManagerLayout
Entity: Property · Floor · Room

Breadcrumb: Property > [Property.name]

HEADER CARD (surface-card, rounded.md, padding: xxl):
  Property.name — heading-md | Property.status Badge
  Property.address — body-md, charcoal
  Property.description — body-md

  StatisticsPanel (grid 3-col):
    Total Floors: COUNT(floors WHERE propertyId)
    Total Rooms:  COUNT(rooms WHERE propertyId)
    Available:    COUNT(rooms WHERE propertyId AND status=AVAILABLE)

  Actions: "Edit Property" Button.Outline -> SCR-36 | "Manage Floors" Button.Ghost -> SCR-38

FLOORS LIST (surface-card, rounded.md, shadow-sm):
  Each Floor: Floor.floorNumber | Room count on floor | Button.Ghost "View Rooms" -> SCR-39 filtered
```

---

### SCR-35 — Add Property

```
Screen: SCR-35 — Add Property
Actor: Manager
Layout: ManagerLayout
Entity: Property (created)
Fields: name · address · description · status

Breadcrumb: Property > Add

FORM CARD (surface-card, rounded.md, padding: xxl, max-width 720px):
  Property Name input  -> Property.name (required, max 200 chars, rounded.full)
  Address input        -> Property.address (required, max 500 chars, rounded.full)
  Description Textarea -> Property.description (max 2000 chars, rounded.md)
  Status Select.Single -> Property.status (ACTIVE / INACTIVE) (rounded.full)

  ACTION BAR (sticky bottom):
    "Create Property" Button.Primary (rounded.full, #ea2804)
    "Cancel" Button.Ghost -> SCR-33
```

---

### SCR-36 — Edit Property

```
Screen: SCR-36 — Edit Property
Actor: Manager
Layout: ManagerLayout
Entity: Property (update)

Same layout as SCR-35 with:
  Pre-filled values from existing Property
  "Last updated: [Property.updatedAt]" caption in header
  Property.id (code-md, read-only display)
  Action: "Save Changes" Button.Primary (rounded.full, #ea2804)
```

---

### SCR-37 — Structure Tree View

```
Screen: SCR-37 — Structure Tree View
Actor: Manager
Layout: ManagerLayout
Entity: Property · Floor · Room

Breadcrumb: Structure > Tree View

Property selector (Select.Searchable, rounded.full) -> filter tree by Property.id

TREE VIEW (surface-card, rounded.md, shadow-sm):
  Property (root node)
  └── Floor 1 (Floor.floorNumber)
      ├── Room 101 (Room.roomNumber) [status Badge.Success "Available"]
      ├── Room 102 (Room.roomNumber) [status Badge.Warning "Pending"]
  └── Floor 2
      ├── Room 201 [status Badge.Info "Reserved"]

  Node actions:
    Floor: "Add Room" Button.Ghost | "Edit Floor" Button.Ghost | "Delete" Button.Ghost (danger)
    Room:  "View" Button.Ghost -> SCR-40 | "Edit" Button.Ghost -> SCR-42

"Add Floor" Button.Outline (rounded.full) -> opens inline form within tree
```

---

### SCR-38 — Floor Management

```
Screen: SCR-38 — Floor Management
Actor: Manager
Layout: ManagerLayout
Entity: Floor (CRUD)

Breadcrumb: Structure > Floor Management

Property selector (Select.Searchable, rounded.full) -> filter floors by Property.id
"Add Floor" Button.Primary (rounded.full, #ea2804)

DataTable (Floor):
  Columns: Floor.floorNumber | Room count | Description | Actions
  Actions: Edit (inline EditForm) | Delete (ConfirmationDialog)

INLINE CREATE FORM (appears below toolbar on "Add Floor"):
  Floor Number Input.Number -> Floor.floorNumber (required, rounded.full)
  Description Input.Text    -> Floor.description (optional, rounded.full)
  "Save" Button.Primary (rounded.full) | "Cancel" Button.Ghost
```

---

### SCR-39 — Room List

```
Screen: SCR-39 — Room List
Actor: Manager
Layout: ManagerLayout
Entity: Room

Breadcrumb: Rooms > List

TOOLBAR:
  "Add Room" Button.Primary (rounded.full, #ea2804) -> SCR-41
  Input.Search (rounded.full) — search by Room.roomNumber
  Filter inline: Property Select | Floor Select | Status Select | Room Type Select

DataTable (Room):
  Columns: Room.roomNumber (code-md) | Property.name | Floor.floorNumber | Room.roomType Badge | pricingRule.basePrice | Room.capacity | Room.status Badge | Actions
  Actions: View (-> SCR-40) | Edit (-> SCR-42) | Gallery (-> SCR-43) | Status (-> SCR-44)
  Sort: roomNumber, pricingRule.basePrice, status

Pagination
EmptyState: bed icon + "No rooms yet. Add your first room."
```

---

### SCR-40 — Room Detail Management

```
Screen: SCR-40 — Room Detail Management
Actor: Manager
Layout: ManagerLayout
Entity: Room · Attachment · Property · Floor

Breadcrumb: Rooms > [Room.roomNumber]

HEADER ACTIONS: "Edit Room" Button.Outline -> SCR-42 | "Manage Gallery" Button.Ghost -> SCR-43 | "Update Status" Button.Ghost -> SCR-44

IMAGE GALLERY (thumbnail strip, rounded.md images)
  Primary: Attachment (isPrimary=true, entityType=ROOM)
  Others: sorted by Attachment.sortOrder

INFO CARD (surface-card, rounded.md, padding: xxl):
  Room.roomNumber + Room.roomType — heading-md | Room.status Badge

  StatisticsPanel (grid 4-col):
    Base Price:    Room.pricingRule.basePrice/night
    Capacity:      Room.capacity guests
    Area:          Room.area m²
    Photos:        COUNT(Attachment WHERE entityType=ROOM)

  Details (key-value):
    Property: Property.name | Floor: Floor.floorNumber
    Description: Room.description

  BOOKING HISTORY (mini DataTable, last 5):
    Booking.id | Customer | checkInDate | checkOutDate | status Badge
    "View All" -> SCR-45 filtered by roomId
```

---

### SCR-41 — Add Room

```
Screen: SCR-41 — Add Room
Actor: Manager
Layout: ManagerLayout
Entity: Room (created)
Fields: propertyId · floorId · roomNumber · roomType · area · capacity · description · status
Relations created: pricingRule (basePrice · weekendSurcharge)

Breadcrumb: Rooms > Add

FORM CARD (surface-card, rounded.md, padding: xxl, max-width 720px):
  Property      Select.Searchable -> Room.propertyId (required, rounded.full)
  Floor         Select.Single     -> Room.floorId    (required, filtered by property, rounded.full)
  Room Number   Input.Text        -> Room.roomNumber  (required, max 20 chars, rounded.full)
  Room Type     Select.Single     -> Room.roomType    (Studio / Standard / Deluxe / Suite / Villa, rounded.full)
  Capacity      Input.Number      -> Room.capacity     (required, min 1, rounded.full)
  Area          Input.Number      -> Room.area (m², rounded.full)
  Description   Textarea          -> Room.description  (max 2000 chars, rounded.md)
  Status        Select.Single     -> Room.status        (default: AVAILABLE, rounded.full)
  --- PRICING RULE (fieldset, surface-bone bg, rounded.md, padding: lg) ---
  Base Price/Night     Input.Number -> PricingRule.basePrice (required, min 0, rounded.full)
  Weekend Surcharge    Input.Number -> PricingRule.weekendSurcharge (optional, min 0, rounded.full)
  Alert.Info: "Weekend surcharge is added on Fri/Sat/Sun to the base price."

  ACTION BAR:
    "Create Room" Button.Primary (rounded.full, #ea2804)
    "Cancel" Button.Ghost -> SCR-39
```

---

### SCR-42 — Edit Room

```
Screen: SCR-42 — Edit Room
Actor: Manager
Layout: ManagerLayout
Entity: Room (update)

Same form as SCR-41 with:
  Pre-filled values
  Room.id (code-md, read-only)
  "Last updated: [Room.updatedAt]"
  "Save Changes" Button.Primary (rounded.full, #ea2804)
```

---

### SCR-43 — Room Gallery Management

```
Screen: SCR-43 — Room Gallery Management
Actor: Manager
Layout: ManagerLayout
Entity: Attachment (entityType=ROOM · CRUD)

Breadcrumb: Rooms > [Room.roomNumber] > Gallery

UPLOAD ZONE (FileUpload.ImagePicker):
  FileUpload.DropZone: "Upload images" — max 20 images, 10MB each, JPG/PNG/WebP
  Progress bar per image on upload

IMAGE GRID (CSS grid, 3-col, gap 16px):
  Each image tile (surface-card, rounded.md, shadow-sm):
    Image preview (1:1 square, rounded.md) — Attachment.url
    Drag handle (top-left, to reorder -> updates Attachment.sortOrder)
    "Set Primary" Toggle (if Attachment.isPrimary = false -> Button.Ghost)
    Primary badge (Badge.Primary "Main Photo" if isPrimary = true)
    Delete Button.Icon (trash, rounded.full) -> ConfirmationDialog

"Save Changes" Button.Primary (rounded.full, #ea2804) — saves Attachment.sortOrder + isPrimary
```

---

### SCR-44 — Room Status Management

```
Screen: SCR-44 — Room Status Management
Actor: Manager
Layout: ManagerLayout
Entity: Room (update Room.status)

Breadcrumb: Rooms > [Room.roomNumber] > Update Status

CARD (surface-card, rounded.md, padding: xxl, max-width 480px):
  Current status: Room.status Badge (large)

  Status Radio.Card group (select new Room.status):
    AVAILABLE   — "Mark as Available" (Badge.Success)
    MAINTENANCE — "Set to Maintenance" (Badge.Neutral)
    (Note: PENDING_DEPOSIT/RESERVED/OCCUPIED are auto-managed by booking flow)

  Reason Textarea -> optional note (rounded.md)
  "Update Status" Button.Primary (rounded.full, #ea2804)
  "Cancel" Button.Ghost -> SCR-40

Alert.Warning: "Changing status manually may affect active bookings. Verify before proceeding."
```

---

### SCR-45 — Booking List (Manager)

```
Screen: SCR-45 — Booking List
Actor: Manager
Layout: ManagerLayout
Entity: Booking

Breadcrumb: Bookings > List

TOOLBAR:
  Input.Search (rounded.full) — search by Booking.id, Customer name
  Filter inline: Status Select | Room Select | DatePicker.Range (checkInDate)

TABS (Tabs.Pill): "All" | "Pending Deposit" | "Confirmed" | "Checked-in" | "Checked-out" | "Cancelled"

DataTable (Booking):
  Columns: Booking.id (code-md) | Customer (Avatar.xs + fullName) | Room (roomNumber) | checkInDate | checkOutDate | totalAmount | status Badge | Actions
  Actions: View (-> SCR-46) | Mark Check-in | Mark Check-out
  Sort: checkInDate, checkOutDate, totalAmount, status

Pagination
Export CSV Button.Outline top-right
```

---

### SCR-46 — Booking Detail (Manager)

```
Screen: SCR-46 — Booking Detail
Actor: Manager
Layout: ManagerLayout
Entity: Booking · Room · User · Payment · Contract

Breadcrumb: Bookings > [Booking.id]

Same 2-column layout as SCR-19 (Customer) but:
  Shows Customer info panel: User.fullName | User.email | User.phone | Avatar
  Actions sidebar:
    PENDING_DEPOSIT / CONFIRMED: "Cancel Booking" Button.Danger (-> SCR-69 Manager Booking Cancellation)
    CONFIRMED:   "Mark Checked-in" Button.Primary (-> PATCH /api/bookings/{id}/check-in)
    CHECKED_IN:  "Mark Checked-out" Button.Primary (-> PATCH /api/bookings/{id}/check-out)
    Any status:  "View Contract" Button.Outline -> SCR-51
  Payment verification status shown for both Payment records
```

---

### SCR-69 — Manager Booking Cancellation (canonical screen ID)

```
Screen: SCR-69 — Manager Booking Cancellation
Actor: Manager
Layout: ManagerLayout
Entity: Booking (update status -> CANCELLED) · Payment (Paid -> Refunded)

Breadcrumb: Bookings > [Booking.id] > Cancel

Dedicated page (mirror of Customer SCR-19 / SCR-20 cancel UX):
  Title: "Cancel Booking (Manager)" — heading-md
  Alert.Info: "Manager-initiated cancel → Guest receives 100% refund of paid amount."
  Summary: Booking.id | Guest | Room | Dates | Paid Amount | Refund Amount (= 100% paid)
  Cancel Reason Textarea (required, max 500 chars) — force majeure / system error / other
  Confirm: "Confirm Cancellation (Full Refund)" Button.Danger
    -> POST /api/v1/manager/bookings/{id}/cancel { cancelReason }
  Cancel: "Go Back" Button.Outline -> SCR-35 (or Manager Booking Detail)

Only when Booking.status IN (PENDING_DEPOSIT, CONFIRMED) and before check-in
On success: Booking.status -> CANCELLED | Payments Paid -> REFUNDED | Room.status -> AVAILABLE | Contract.status -> CANCELLED | notify Customer
```

---

### SCR-47 — Payment List (Manager)

```
Screen: SCR-47 — Payment List
Actor: Manager
Layout: ManagerLayout
Entity: Payment

Breadcrumb: Payments > List

TABS (Tabs.Pill): "All" | "Deposit" | "Balance" | "Pending" | "Paid" | "Failed"

FILTER TOOLBAR:
  DatePicker.Range -> Payment.createdAt
  Method Select    -> Payment.method
  Status Select    -> Payment.status

DataTable (Payment):
  Columns: Payment.id (code-md) | Booking.id (link -> SCR-46) | Customer (Avatar + fullName) | type Badge | amount | method Badge | status Badge | createdAt | Actions
  Actions: View (-> SCR-49) | Verify (-> SCR-48, if status=PENDING)

Pagination
Export CSV Button.Outline
```

---

### SCR-48 — Payment Verification

> **Deferred edu (TD-011):** Do not design/ship bank-transfer verification. Canonical edu payment = VNPay auto-confirm. Keep this block only as post-edu reference. Canonical Manager check-in/out screen ID is **SCR-37** (`screen.md`).

```
Screen: SCR-48 — Payment Verification (OUT OF EDU SCOPE — TD-011)
Actor: Manager
Layout: ManagerLayout
Entity: Payment (update status) · PaymentReceipt (read)

Breadcrumb: Payments > [Payment.id] > Verify

TWO COLUMNS:

LEFT — RECEIPT VIEWER:
  PaymentReceipt image (zoomable, rounded.md, shadow-sm)
  Payment.id (code-md) | Payment.type Badge | Payment.amount — heading-md, primary
  Payment.method | Payment.createdAt
  Customer: User.fullName | User.email | User.phone
  Booking: Booking.id (link) | checkInDate -> checkOutDate | Room.roomNumber

RIGHT — VERIFICATION ACTIONS (surface-card, rounded.lg, padding: xl):
  Current Status: Payment.status Badge (large)
  Verification Note Textarea (optional, rounded.md)

  "Approve Payment" Button.Primary (rounded.full, #ea2804, full-width)
    -> PATCH /api/payments/{id}/verify { status: PAID }
    -> triggers: Booking.status=CONFIRMED | Room.status=RESERVED | Contract auto-generated
  "Reject Payment" Button.Danger (rounded.full, full-width)
    -> PATCH /api/payments/{id}/verify { status: FAILED }

  ConfirmationDialog before each action.
  Alert.Warning: "Approving deposit triggers automatic contract generation and email."
```

---

### SCR-49 — Payment Detail (Manager)

```
Screen: SCR-49 — Payment Detail
Actor: Manager
Layout: ManagerLayout
Entity: Payment · PaymentReceipt · Booking · User

Breadcrumb: Payments > [Payment.id]

DETAIL CARD (surface-card, rounded.md, padding: xxl):
  Payment.id (code-md) | type Badge | status Badge

  Key-value pairs:
    Amount:    Payment.amount — heading-sm, primary
    Type:      Payment.type
    Method:    Payment.method
    Paid at:   Payment.paidAt
    Verified by: Payment.verifiedBy (User.fullName)
    Verified at: Payment.verifiedAt
    Booking:   Booking.id (link -> SCR-46) | checkInDate -> checkOutDate
    Customer:  User.fullName | User.email

  RECEIPT (if exists):
    PaymentReceipt image (100% width, rounded.md)
    "Download Receipt" Button.Outline -> GET /api/payments/{id}/receipt

  TIMELINE: Created | Receipt Uploaded | Verified
```

---

### SCR-50 — Contract List (Manager)

```
Screen: SCR-50 — Contract List
Actor: Manager
Layout: ManagerLayout
Entity: Contract

Breadcrumb: Contracts > List

TABS (Tabs.Pill): "All" | "Active" | "Completed" | "Cancelled"

DataTable (Contract):
  Columns: Contract.id (code-md) | Booking.id (link) | Customer (Avatar + fullName) | Room | checkInDate | checkOutDate | depositAmount | totalAmount | status Badge | Actions
  Actions: View (-> SCR-51) | Download PDF | Resend Email (-> SCR-52)

Pagination
```

---

### SCR-51 — Contract Detail Management

```
Screen: SCR-51 — Contract Detail Management
Actor: Manager
Layout: ManagerLayout
Entity: Contract · Booking · Room · User

Breadcrumb: Contracts > [Contract.id]

HEADER ACTIONS:
  "Download PDF" Button.Outline -> GET /api/contracts/{id}/pdf
  "Print" Button.Ghost (browser print)
  "Resend Email" Button.Ghost -> SCR-52

DETAIL CARD (surface-card, rounded.md, padding: xxl):
  Contract.id (code-md) | Contract.status Badge
  Customer: User.fullName | User.email | User.phone
  Room: Room.roomNumber + Room.roomType | Property.name | Floor.floorNumber
  Check-in: Contract.checkInDate | Check-out: Contract.checkOutDate
  Deposit: Contract.depositAmount | Total: Contract.totalAmount
  Generated: Contract.generatedAt | Sent: Contract.sentAt

PDF EMBED (iframe, rounded.md, shadow-sm, full-height)
or "Open PDF" Button.Outline if embed not supported
```

---

### SCR-52 — Resend Contract Email

```
Screen: SCR-52 — Resend Contract Email
Actor: Manager
Layout: ManagerLayout
Entity: Contract (update Contract.sentAt)

Modal.md (560px):
  Title: "Resend Contract Email" — heading-md
  Description: "Send the contract PDF for Booking #[Booking.id] to:"
  Customer Email: User.email (read-only, pre-filled)
  Optional additional email: Input.Email (rounded.full)
  Note Textarea (optional, rounded.md)

  "Send Email" Button.Primary (rounded.full, #ea2804)
    -> POST /api/contracts/{id}/resend-email
    -> updates Contract.sentAt to now
  "Cancel" Button.Ghost
```

---

### SCR-53 — Maintenance Ticket List (Manager)

```
Screen: SCR-53 — Maintenance Ticket List
Actor: Manager
Layout: ManagerLayout
Entity: MaintenanceTicket

Breadcrumb: Maintenance > List

TABS (Tabs.Pill): "All" | "Open" | "In Progress" | "Resolved" | "Closed"

FILTER: Property Select | Room Select | DatePicker.Range (createdAt)

DataTable (Maintenance):
  Columns: Ticket.id (code-md) | Customer (Avatar + fullName) | Room.roomNumber | Title | status Badge | createdAt | Actions
  Actions: View (-> SCR-54) | Update Status (-> SCR-54)

Pagination
```

---

### SCR-54 — Maintenance Ticket Detail (Manager)

```
Screen: SCR-54 — Maintenance Ticket Detail
Actor: Manager
Layout: ManagerLayout
Entity: MaintenanceTicket (update status)

Breadcrumb: Maintenance > [Ticket.title]

TWO COLUMNS:

LEFT — TICKET INFO (surface-card, rounded.md, padding: xxl):
  Ticket.id (code-md) | Ticket.status Badge (large)
  Title: Ticket.title — heading-md
  Room: Room.roomNumber | Property.name
  Customer: User.fullName | User.email
  Submitted: Ticket.createdAt
  Description: Ticket.description — body-md
  Photos: grid (3-col thumbnails, rounded.md) if attachments exist (entityType=MAINTENANCE_TICKET)
  TIMELINE: status change history

RIGHT — UPDATE PANEL (surface-card, rounded.lg, padding: xl):
  Update Status Radio.Card:
    OPEN | IN_PROGRESS | RESOLVED | CLOSED
  Resolution Note Textarea (rounded.md)
  "Update Status" Button.Primary (rounded.full, #ea2804)
```

---

### SCR-55 — Customer List (Manager)

```
Screen: SCR-55 — Customer List
Actor: Manager
Layout: ManagerLayout
Entity: User (CUSTOMER role only)

Breadcrumb: Customers > List

TOOLBAR: Input.Search (rounded.full) — by User.fullName, User.email

DataTable (Customer):
  Columns: User.id (code-md, caption) | Avatar + fullName | email | phone | status Badge | createdAt | Actions
  Actions: View (-> SCR-56) | Suspend (ConfirmationDialog -> PATCH status=SUSPENDED) | Activate (if suspended)
  Filter: status Select

Pagination
```

---

### SCR-56 — Customer Detail (Manager)

```
Screen: SCR-56 — Customer Detail
Actor: Manager
Layout: ManagerLayout
Entity: User · Booking · Payment

Breadcrumb: Customers > [User.fullName]

PROFILE CARD (surface-card, rounded.md, padding: xxl):
  Avatar (User.avatarUrl, 80px, rounded.full) | User.fullName — heading-md | User.status Badge
  Email: User.email | Phone: User.phone | Registered: User.createdAt

BOOKING HISTORY (DataTable, last 10):
  Booking.id | checkInDate | checkOutDate | Room.roomNumber | totalAmount | status Badge
  "View All" Button.Ghost -> SCR-45 filtered

ACTIONS:
  Suspend Account: Button.Danger -> PATCH /api/users/{id}/suspend (ConfirmationDialog)
  Activate Account: Button.Primary -> PATCH /api/users/{id}/activate (if suspended)
```

---

### SCR-57 — Complaint List (Manager)

```
Screen: SCR-57 — Complaint List
Actor: Manager
Layout: ManagerLayout
Entity: Complaint

Breadcrumb: Complaints > List

TABS (Tabs.Pill): "All" | "Open" | "Investigating" | "Resolved" | "Closed"

DataTable (Complaint):
  Columns: Complaint.id (code-md) | Customer (Avatar + fullName) | Subject | status Badge | createdAt | Actions
  Actions: View (-> SCR-58)

Pagination
```

---

### SCR-58 — Complaint Detail (Manager)

```
Screen: SCR-58 — Complaint Detail
Actor: Manager
Layout: ManagerLayout
Entity: Complaint (update status)

Breadcrumb: Complaints > [Complaint.subject]

TWO COLUMNS:

LEFT — COMPLAINT INFO (surface-card, rounded.md, padding: xxl):
  Complaint.id (code-md) | Complaint.status Badge (large)
  Subject: Complaint.subject — heading-md
  Submitted by: User.fullName | User.email | User.createdAt
  Description: Complaint.description — body-md

  Resolution Notes (if resolved): Complaint.resolutionNotes
  Resolved at: Complaint.resolvedAt

RIGHT — UPDATE PANEL (surface-card, rounded.lg, padding: xl):
  Status Radio.Card: OPEN | INVESTIGATING | RESOLVED | CLOSED
  Resolution Notes Textarea -> Complaint.resolutionNotes (rounded.md)
  "Update Status" Button.Primary (rounded.full, #ea2804)
```

---

### SCR-59 — Revenue Report

```
Screen: SCR-59 — Revenue Report
Actor: Manager
Layout: ManagerLayout
Entity: Payment · Booking · Property

Breadcrumb: Reports > Revenue

FILTERS ROW:
  Property Select.Searchable (rounded.full) -> Property.id filter
  DatePicker.Range -> Payment.paidAt range
  "Export CSV" Button.Outline (rounded.full)

KPI ROW (3 KpiCards):
  Total Revenue:    SUM(Payment.amount WHERE status=PAID in range)
  Deposit Revenue:  SUM(Payment.amount WHERE type=DEPOSIT AND status=PAID)
  Balance Revenue:  SUM(Payment.amount WHERE type=REMAINING_BALANCE AND status=PAID)

RevenueChart (surface-card, rounded.lg):
  Line chart: monthly revenue
  X-axis: months | Y-axis: VND (K/M suffix)
  Line: primary #ea2804

DataTable (breakdown by property or month):
  Period | Property | Bookings | Revenue | vs. prev period
```

---

### SCR-60 — Occupancy Report

```
Screen: SCR-60 — Occupancy Report
Actor: Manager
Layout: ManagerLayout
Entity: Room · Booking

Breadcrumb: Reports > Occupancy

FILTERS: Property Select | DatePicker.Range
"Export CSV" Button.Outline

KPI ROW (4 KpiCards):
  Occupancy Rate %: (occupied nights / total available nights) x 100
  Available Rooms: COUNT(rooms WHERE status=AVAILABLE)
  Occupied Rooms:  COUNT(rooms WHERE status=OCCUPIED)
  Maintenance Rooms: COUNT(rooms WHERE status=MAINTENANCE)

OccupancyChart (Donut, surface-card, rounded.lg):
  Available (success) | Occupied (primary) | Maintenance (neutral) | Pending (warning)
  Center: overall occupancy %

StatisticsPanel (per property or room type breakdown)
```

---

### SCR-61 — Booking Trend Report

```
Screen: SCR-61 — Booking Trend Report
Actor: Manager
Layout: ManagerLayout
Entity: Booking

Breadcrumb: Reports > Booking Trend

FILTERS: DatePicker.Range | Property Select
"Export CSV" Button.Outline

BookingTrendChart (Bar chart, surface-card, rounded.lg):
  X-axis: week/month | Y-axis: count
  Series: New Bookings (primary) | Cancellations (error)

DataTable (booking trend per period):
  Period | New Bookings | Cancellations | Net | vs. prev period
```

---

### SCR-62 — Revenue Trend Report

```
Screen: SCR-62 — Revenue Trend Report
Actor: Manager
Layout: ManagerLayout
Entity: Payment

Breadcrumb: Reports > Revenue Trend

FILTERS: DatePicker.Range | Property Select
"Export CSV" Button.Outline

RevenueChart (Line chart trend, surface-card, rounded.lg):
  Month-over-month revenue comparison
  Current period (primary) | Previous period (charcoal dashed)

DataTable: Period | Revenue | vs. prev | Growth %
```

---

### SCR-63 — Activity Logs

```
Screen: SCR-63 — Activity Logs
Actor: Manager
Layout: ManagerLayout
Entity: ActivityLog

Breadcrumb: Administration > Activity Logs

FILTER TOOLBAR:
  Input.Search (rounded.full) — by ActivityLog.description, User.fullName
  DatePicker.Range -> ActivityLog.createdAt
  Action Select.Multi -> ActivityLog.action (multi-select filter pills)
  "Export CSV" Button.Outline

DataTable (ActivityLog):
  Columns: Log.id (code-md, caption) | User (Avatar + fullName) | action (code-md) | entityType | entityId (code-md) | description | createdAt | ipAddress
  No row actions (read-only)
  Sort: createdAt DESC

Pagination
```

---

### SCR-64 — System Settings

```
Screen: SCR-64 — System Settings
Actor: Manager
Layout: ManagerLayout
Entity: SystemSetting

Breadcrumb: Administration > System Settings

TABS.Card (sections):

1. GENERAL:
   System Name Input.Text -> SystemSetting.SYSTEM_NAME (rounded.full)
   Support Email Input.Email -> SystemSetting.SUPPORT_EMAIL (rounded.full)

2. PAYMENT:
   Deposit Percentage Input.Number -> SystemSetting.DEPOSIT_PERCENTAGE (default: 40, rounded.full)
   Payment Methods Select.Multi -> SystemSetting.PAYMENT_METHODS (checkboxes: BANK_TRANSFER / CASH / E_WALLET)
   Bank Account Number Input.Text -> SystemSetting.BANK_ACCOUNT_NUMBER (rounded.full)
   Bank Account Name Input.Text -> SystemSetting.BANK_ACCOUNT_NAME (rounded.full)
   Bank Name Input.Text -> SystemSetting.BANK_NAME (rounded.full)

3. EMAIL / SMTP:
   SMTP Host Input.Text -> SystemSetting.SMTP_HOST (rounded.full)
   SMTP Port Input.Number -> SystemSetting.SMTP_PORT (rounded.full)
   SMTP Sender Email Input.Email -> SystemSetting.SMTP_SENDER (rounded.full)

Each tab: "Save Changes" Button.Primary (rounded.full, #ea2804) + Alert.Warning "Changes take effect immediately."
```

---

### SCR-65 — Content Moderation

```
Screen: SCR-65 — Content Moderation
Actor: Manager
Layout: ManagerLayout
Entity: Review · Room

Breadcrumb: Administration > Content Moderation

TABS (Tabs.Pill): "All Reviews" | "Published" | "Hidden"

REVIEW MODERATION LIST (cards):
  Each ReviewCard (surface-card, rounded.md, padding: xl):
    Avatar (User.avatarUrl, 32px, rounded.full) | User.fullName | Room.roomNumber | Review.createdAt
    StarRating display (5 stars, 16px, primary)
    Review.comment — body-md, full text
    Review.status Badge (PUBLISHED green | HIDDEN gray)
    Actions:
      PUBLISHED: "Hide Review" Button.Danger (rounded.full) -> PATCH status=HIDDEN
      HIDDEN:    "Show Review" Button.Outline (rounded.full) -> PATCH status=PUBLISHED
    ConfirmationDialog for each action.

Pagination
```

---

## DESIGN DO's AND DON'Ts

```
DO:
  - page background ALWAYS canvas #f9f7f3 (never pure white at page level)
  - rounded.full on ALL buttons, inputs, badges, avatars, tags
  - rounded.md (10px) or rounded.lg (16px) on content cards
  - ONE primary #ea2804 CTA per viewport section max
  - rb-freigeist-neue for all display text (30px+)
  - basier-square for all body, UI, button text
  - jetbrains-mono for all IDs, codes (Booking.id, Payment.id, Contract.id)
  - Manager sidebar: surface-dark #202020 background
  - display lineHeight = 1.0 always (never loosen past 1.0)
  - color-blocking for elevation (surface-bone for inset sections)
  - shadow-sm for cards on white, shadow-float for sticky/floating panels

DON'T:
  - never use pure white as page background
  - never use rounded.full on content cards (only buttons/inputs/badges)
  - never add more than one orange CTA per viewport section
  - never bump body text weight to 500 — use family change instead
  - never add drop shadows on cream canvas surfaces
  - never display entity IDs in Inter/Geist — always jetbrains-mono
  - never invent form fields that don't map to entity attributes
  - never show createdAt / updatedAt / UUID in editable form fields
```

---

*Figma Generation Prompt v1.0.0 — Homestay / Resort Booking Management System*
*Updated: 2026-06-11 | 65 screens | 3 portals: Guest / Customer / Manager*
