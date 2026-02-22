-- ============================================
-- auth_db: Schema + Seed Data
-- ============================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    profile_image_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Refresh tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- ============================================
-- Seed Data
-- ============================================
-- Password for all users: "password123" (BCrypt hash)
-- BCrypt hash of "password123"

INSERT INTO users (id, name, email, password_hash, profile_image_url, created_at, updated_at) VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Pitabash Admin', 'pitabash@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, NOW(), NOW()),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'John Doe', 'john@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'https://i.pravatar.cc/150?u=john', NOW(), NOW()),
    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Jane Smith', 'jane@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'https://i.pravatar.cc/150?u=jane', NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- Refresh tokens for the users
INSERT INTO refresh_tokens (id, user_id, token, expires_at, created_at) VALUES
    ('d4e5f6a7-b8c9-0123-defa-234567890123', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'refresh-token-pitabash-001', NOW() + INTERVAL '7 days', NOW()),
    ('e5f6a7b8-c9d0-1234-efab-345678901234', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', 'refresh-token-john-001', NOW() + INTERVAL '7 days', NOW()),
    ('f6a7b8c9-d0e1-2345-fabc-456789012345', 'c3d4e5f6-a7b8-9012-cdef-123456789012', 'refresh-token-jane-001', NOW() + INTERVAL '7 days', NOW())
ON CONFLICT (token) DO NOTHING;

-- Verify
SELECT 'auth_db seeded successfully' AS status;
SELECT 'Users:' AS info, count(*) AS count FROM users;
SELECT 'Refresh Tokens:' AS info, count(*) AS count FROM refresh_tokens;
