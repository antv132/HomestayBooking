# 008 - Room Management: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities

### Room Class
Represents a room in a property.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Managed via `deleted_at` timestamp for soft delete. Default `status` is `AVAILABLE`. `room_number` must be UNIQUE among active rooms in a property. `floor_id` must reference a floor in the same property.) |

### RoomImage Class
Represents images associated with a room.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Enforces limits: Maximum 10 images per `room_id`. Exactly 1 primary image expected per room if images exist via `is_primary` flag.) |

---

## 2. Controllers

### RoomController Class
Handles room management operations.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getRooms | Manager sees rooms for their property. Employee sees read-only list. Excludes soft-deleted rooms. Returns paginated room list. |
| 02 | createRoom | Takes room creation request. Defaults status to `AVAILABLE`. Returns created room details. |
| 03 | updateRoom | Modifies room metadata. Validates `floorId` scope. Changing `pricePerNight` does not alter the total of existing bookings. Returns updated room. |
| 04 | deleteRoom | Takes `UUID roomId`. Executes a soft-delete after checking delete guards. Returns 200 OK. |

### RoomGalleryController Class
Handles image uploads and gallery management.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | upload | Uploads a new image. Validates file types (`jpeg`, `png`, `webp`) and size (<= 10MB). Returns image details. |
| 02 | setPrimary | Sets an image as the primary image for a room. Returns 200 OK. |
| 03 | reorder | Reorders images based on sort order. Returns 200 OK. |
| 04 | delete | Deletes an image. Returns 200 OK. |

---

## 3. Services

### RoomService Class
Handles business logic for room operations.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | softDelete | The core operation must query `BookingRepository` to ensure `countActiveOrBlockingBookings == 0`. If valid, it updates `deleted_at = NOW()`. Returns void. |
| 02 | updateRoom | If the `floorId` changes, validates that the new floor belongs to the same property. Moving rooms between properties is strictly prohibited. Returns updated Room. |

### RoomImageService Class
Handles business logic for room images.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | setPrimary | Must run transactionally. First updates all images for the room to `is_primary = false`, then sets the target image to `true`. Returns void. |
| 02 | uploadImage | Validates the count limit (10) before persisting to storage. Returns saved `RoomImage`. |
