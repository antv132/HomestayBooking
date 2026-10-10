# 002 - User Profile: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities

### User Class (Profile Subset)
The User entity is primarily managed by `001-user-auth`, but the User Profile feature reads and updates specific fields of this entity.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class, no complex business methods. Standard getters/setters for Profile data.) |

---

## 2. Controllers

### ProfileController Class
Exposes REST API endpoints for user profile management.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getCurrentUser | GET `/api/v1/users/me`. Retrieves the profile information of the currently authenticated user. Returns 200 OK with `UserProfileResponse`. Security: Requires valid JWT, checks ACTIVE status. |
| 02 | updateCurrentUser | PUT `/api/v1/users/me`. Updates the profile of the currently authenticated user. Takes `UpdateProfileRequest` containing `fullName`, `phone`, and `avatarUrl`. Returns 200 OK with updated `UserProfileResponse`. Security: Requires valid JWT, RBAC Self scope. |

---

## 3. Services (Core Logic)

### UserService Class
Implements the core business logic for user profile retrieval and partial updates.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getCurrentUserProfile | Fetches the user by ID (extracted from JWT). Returns `UserProfileResponse` mapped from the `User` entity. Throws `UserNotFoundException` if the user no longer exists. |
| 02 | updateProfile | Applies partial updates to the user's profile. Loads `User` from repository. Updates `fullName` (validates not blank), `phone` (validates VN format), and `avatarUrl` (validates HTTP/HTTPS URL) if provided. Saves the updated entity via `UserRepository`. Returns updated `UserProfileResponse`. |
