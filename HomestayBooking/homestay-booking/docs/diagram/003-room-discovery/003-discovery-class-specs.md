# 003 - Room Discovery: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities (Read-Only Context)

### Property Class
Checked for `ACTIVE` status before returning any associated rooms.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Checked for `ACTIVE` status before returning any associated rooms.) |

### Room Class
Core entity for discovery. Must not be `MAINTENANCE` or `OUT_OF_SERVICE`.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Must not be `MAINTENANCE` or `OUT_OF_SERVICE` for discovery.) |

### RoomImage Class
Attached to room responses. `RoomListItem` only uses the `isPrimary=true` image. `RoomDetail` returns the full gallery ordered by `sortOrder`.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. `RoomListItem` only uses the `isPrimary=true` image. `RoomDetail` returns the full gallery ordered by `sortOrder`.) |

### Booking Class
Queried strictly to calculate availability overlap. Never modified in this feature.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Never modified in this feature.) |

### Promotion Class
Used on the landing page for active banners.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Only active promotions are fetched.) |

---

## 2. Controllers

### DiscoveryController Class
Handles public endpoints for searching and viewing rooms. No authentication is strictly required.
- **Methods:**
  - `searchRooms(SearchCriteria criteria, Pageable pageable)`: Returns `Page<RoomListItem>`.
  - `getRoomDetail(UUID id)`: Returns `RoomDetail`. Throws 404 if property/room is inactive.
  - `getRoomAvailability(UUID id, int year, int month)`: Returns `RoomAvailability` showing blocked dates.
  - `getSearchSuggestions(String keyword)`: Returns `List<Suggestion>` (Properties/Locations).
  - `getFeaturedProperties()` / `getActivePromotions()`: For Landing Page (SCR-01).

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | searchRooms | Takes `SearchCriteria` and `Pageable`. Calls `DiscoveryService.searchRooms`. Returns `Page<RoomListItem>`. |
| 02 | getRoomDetail | Takes `UUID id`. Calls `DiscoveryService.getRoomDetail`. Throws 404 if property/room is inactive. Returns `RoomDetail`. |
| 03 | getRoomAvailability | Takes `UUID id, int year, int month`. Calls `DiscoveryService.getRoomAvailability`. Returns `RoomAvailability` showing blocked dates. |
| 04 | getSearchSuggestions | Takes `String keyword`. Returns `List<Suggestion>` (Properties/Locations). |
| 05 | getFeaturedProperties | Fetches featured properties for Landing Page (SCR-01). |
| 06 | getActivePromotions | Fetches active promotions for Landing Page (SCR-01). |

---

## 3. Services

### DiscoveryService Class
Contains the business rules for filtering and aggregating data.
- **Methods:**
  - `searchRooms(SearchCriteria)`: 
    - Queries database for rooms matching capacity, price, location, and property.
    - If `checkIn` and `checkOut` are provided, it performs a secondary check (or SQL JOIN) to ensure no `Booking` overlaps the requested dates with blocking statuses.
  - `getRoomDetail(UUID)`: 
    - Fetches the room, validates property status.
    - Aggregates `RoomImages` and `Amenities`.
  - `getRoomAvailability(UUID, int, int)`:
    - Calculates the start and end of the requested month.
    - Fetches all bookings overlapping that month with statuses `PENDING_DEPOSIT`, `CONFIRMED`, `RESERVED`, `OCCUPIED`.
    - Generates a flat array of `bookedDates`.

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | searchRooms | Takes `SearchCriteria`. Queries database for rooms matching capacity, price, location, and property. If `checkIn` and `checkOut` are provided, performs a secondary check to ensure no overlapping bookings with blocking statuses. Returns `Page<RoomListItem>`. |
| 02 | getRoomDetail | Takes `UUID`. Fetches the room, validates property status. Aggregates `RoomImages` and `Amenities`. Returns `RoomDetail`. |
| 03 | getRoomAvailability | Takes `UUID, int, int`. Calculates the start and end of the requested month. Fetches all bookings overlapping that month with statuses `PENDING_DEPOSIT`, `CONFIRMED`, `RESERVED`, `OCCUPIED`. Generates a flat array of `bookedDates`. Returns `RoomAvailability`. |

---

## 4. DTOs (Data Transfer Objects)

- **`RoomListItem`**: Summarized view for search results. Contains `primaryImageUrl`, `pricePerNight`, `propertyName`.
- **`RoomDetail`**: Extensive view for SCR-08. Contains array of `images`, `amenities`, `description`.
- **`RoomAvailability`**: Contains `List<String> bookedDates` formatted in ISO-8601 (YYYY-MM-DD).
