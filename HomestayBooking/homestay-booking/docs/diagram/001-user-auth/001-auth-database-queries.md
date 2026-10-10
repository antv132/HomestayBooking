# 001 - User Auth: Database Queries

This document provides the detailed SQL (SELECT, INSERT, UPDATE, etc.) used in implementing the Authentication functions. Note that in Spring Boot / Spring Data JPA, these are generated dynamically via Hibernate/JPQL, but below are the underlying native SQL queries equivalent to the operations performed.

## 1. Registration Flow

### Find User by Email (Check if exists)
```sql
SELECT id, full_name, email, password_hash, role, status, google_id, email_verified, created_at, updated_at
FROM users 
WHERE email = :email;
```

### Insert New User (INACTIVE)
```sql
INSERT INTO users (id, full_name, email, password_hash, role, status, email_verified, created_at, updated_at) 
VALUES (:id, :fullName, :email, :passwordHash, 'CUSTOMER', 'INACTIVE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

### Save OTP for Verification
```sql
INSERT INTO otp_verification (id, user_id, otp_hash, expires_at)
VALUES (:id, :userId, :otpHash, :expiresAt);
```

## 2. OTP Verification Flow

### Find OTP by Hash (or User + OTP)
```sql
SELECT id, user_id, otp_hash, expires_at
FROM otp_verification
WHERE user_id = :userId 
  AND otp_hash = :otpHash 
  AND expires_at > CURRENT_TIMESTAMP;
```

### Update User Status to ACTIVE
```sql
UPDATE users 
SET status = 'ACTIVE', email_verified = true, updated_at = CURRENT_TIMESTAMP
WHERE id = :id;
```

## 3. Login Flow (Standard)

### Find User by Email
```sql
SELECT id, full_name, email, password_hash, role, status, google_id, email_verified, created_at, updated_at
FROM users 
WHERE email = :email;
```

### Save Refresh Token
```sql
INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at)
VALUES (:id, :userId, :tokenHash, :expiresAt);
```

## 4. Google OAuth Flow

### Find User by Email
```sql
SELECT id, full_name, email, password_hash, role, status, google_id, email_verified, created_at, updated_at
FROM users 
WHERE email = :email;
```

### Insert New User (If not found via Google)
```sql
INSERT INTO users (id, full_name, email, google_id, role, status, email_verified, created_at, updated_at) 
VALUES (:id, :fullName, :email, :googleId, 'CUSTOMER', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

### Link Existing User to Google ID (Update)
```sql
UPDATE users
SET google_id = :googleId, email_verified = true, updated_at = CURRENT_TIMESTAMP
WHERE id = :id;
```

## 5. Token Management & Password Reset

### Revoke All Existing User Refresh Tokens
```sql
DELETE FROM refresh_tokens
WHERE user_id = :userId;
```

### Insert Password Reset Token
```sql
INSERT INTO password_reset_tokens (id, user_id, token_hash, expires_at)
VALUES (:id, :userId, :tokenHash, :expiresAt);
```

### Find Password Reset Token
```sql
SELECT id, user_id, token_hash, expires_at
FROM password_reset_tokens
WHERE token_hash = :tokenHash AND expires_at > CURRENT_TIMESTAMP;
```

### Update Password
```sql
UPDATE users
SET password_hash = :newPasswordHash, updated_at = CURRENT_TIMESTAMP
WHERE id = :userId;
```
