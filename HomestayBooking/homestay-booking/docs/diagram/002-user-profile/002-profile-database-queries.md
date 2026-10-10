# 002 - User Profile: Database Queries

This document provides the detailed SQL used in implementing the User Profile functions (`GET /users/me` and `PUT /users/me`).

## 1. View Profile (`GET /users/me`)

### Find User by ID (from JWT token)
```sql
SELECT id, full_name, email, phone, avatar_url, role, status, created_at, updated_at
FROM users 
WHERE id = :userId;
```

## 2. Update Profile (`PUT /users/me`)

### Find User by ID (Load before update)
```sql
SELECT id, full_name, email, phone, avatar_url, role, status, created_at, updated_at
FROM users 
WHERE id = :userId;
```

### Update User Profile Fields
*(Note: JPA typically updates all fields or configured dynamic update fields, but the effective SQL for the partial update logic is below)*

```sql
UPDATE users
SET 
    full_name = :fullName, 
    phone = :phone, 
    avatar_url = :avatarUrl, 
    updated_at = CURRENT_TIMESTAMP
WHERE id = :userId;
```

*Note on Validation: The `email`, `role`, and `status` fields are deliberately excluded from the UPDATE query in this feature to comply with the read-only constraints defined in the specifications.*
