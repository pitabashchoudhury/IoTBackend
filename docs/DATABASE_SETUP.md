# Database Setup Guide

This guide covers PostgreSQL setup for the IoT Backend project. The project requires **two separate databases**:

| Database | Used By | Port (Docker) | Port (Local) |
|----------|---------|---------------|--------------|
| `auth_db` | auth-service | 5434 | 5432 |
| `device_db` | device-service | 5433 | 5432 |

---

## Option A: Fresh PostgreSQL Installation

Follow this if you **do not** have PostgreSQL installed on your machine.

### Step 1 — Install PostgreSQL

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib -y
```

**Fedora/RHEL:**
```bash
sudo dnf install postgresql-server postgresql-contrib -y
sudo postgresql-setup --initdb
```

**macOS (Homebrew):**
```bash
brew install postgresql@16
brew services start postgresql@16
```

### Step 2 — Start PostgreSQL Service

```bash
sudo systemctl start postgresql
sudo systemctl enable postgresql    # auto-start on boot
```

Verify it is running:
```bash
sudo systemctl status postgresql
```

### Step 3 — Create User and Databases

Switch to the `postgres` superuser and open psql:
```bash
sudo -u postgres psql
```

Run the following SQL commands:
```sql
-- Create the application user with password
CREATE USER iot_user WITH PASSWORD 'iot_pass';

-- Create the two databases
CREATE DATABASE auth_db OWNER iot_user;
CREATE DATABASE device_db OWNER iot_user;

-- Grant full privileges
GRANT ALL PRIVILEGES ON DATABASE auth_db TO iot_user;
GRANT ALL PRIVILEGES ON DATABASE device_db TO iot_user;

-- Exit psql
\q
```

### Step 4 — Grant Schema Permissions

```bash
sudo -u postgres psql -d auth_db -c "GRANT ALL ON SCHEMA public TO iot_user;"
sudo -u postgres psql -d device_db -c "GRANT ALL ON SCHEMA public TO iot_user;"
```

### Step 5 — Enable Password Authentication

By default, PostgreSQL on Linux uses `peer` authentication (OS username must match DB username). To allow password-based login for `iot_user`, edit `pg_hba.conf`:

```bash
# Find the file location
sudo -u postgres psql -c "SHOW hba_file;"
```

Open the file (typically `/etc/postgresql/16/main/pg_hba.conf`):
```bash
sudo nano /etc/postgresql/16/main/pg_hba.conf
```

Find the line:
```
local   all   all   peer
```

Change it to (or add above it):
```
local   all   iot_user   md5
host    all   iot_user   127.0.0.1/32   md5
host    all   iot_user   ::1/128        md5
```

Restart PostgreSQL:
```bash
sudo systemctl restart postgresql
```

### Step 6 — Verify Connection

```bash
psql -U iot_user -d auth_db -h localhost -c "SELECT current_database();"
psql -U iot_user -d device_db -h localhost -c "SELECT current_database();"
```

Both commands should return without errors.

---

## Option B: Existing PostgreSQL Installation

Follow this if PostgreSQL is **already running** on your machine.

### Step 1 — Check PostgreSQL is Running

```bash
sudo systemctl status postgresql
```

Or check if port 5432 is in use:
```bash
ss -tlnp | grep 5432
```

### Step 2 — Connect as Superuser

```bash
sudo -u postgres psql
```

### Step 3 — Check if `iot_user` Already Exists

```sql
\du
```

Look for `iot_user` in the list.

**If user does NOT exist**, create it:
```sql
CREATE USER iot_user WITH PASSWORD 'iot_pass';
```

**If user already exists** and you need to reset the password:
```sql
ALTER USER iot_user WITH PASSWORD 'iot_pass';
```

### Step 4 — Check if Databases Already Exist

```sql
\l
```

Look for `auth_db` and `device_db` in the list.

**If databases do NOT exist**, create them:
```sql
CREATE DATABASE auth_db OWNER iot_user;
CREATE DATABASE device_db OWNER iot_user;
GRANT ALL PRIVILEGES ON DATABASE auth_db TO iot_user;
GRANT ALL PRIVILEGES ON DATABASE device_db TO iot_user;
\q
```

Then grant schema permissions:
```bash
sudo -u postgres psql -d auth_db -c "GRANT ALL ON SCHEMA public TO iot_user;"
sudo -u postgres psql -d device_db -c "GRANT ALL ON SCHEMA public TO iot_user;"
```

**If databases already exist** but are owned by a different user:
```sql
ALTER DATABASE auth_db OWNER TO iot_user;
ALTER DATABASE device_db OWNER TO iot_user;
GRANT ALL PRIVILEGES ON DATABASE auth_db TO iot_user;
GRANT ALL PRIVILEGES ON DATABASE device_db TO iot_user;
\q
```

```bash
sudo -u postgres psql -d auth_db -c "GRANT ALL ON SCHEMA public TO iot_user;"
sudo -u postgres psql -d device_db -c "GRANT ALL ON SCHEMA public TO iot_user;"
```

### Step 5 — Verify Connection

```bash
psql -U iot_user -d auth_db -h localhost -c "SELECT current_database();"
psql -U iot_user -d device_db -h localhost -c "SELECT current_database();"
```

> If you get a `peer authentication failed` error, follow [Step 5 in Option A](#step-5--enable-password-authentication) to enable `md5` authentication.

---

## Option C: Use Docker (No Local PostgreSQL Needed)

If you prefer not to install PostgreSQL locally, Docker Compose can handle everything:

```bash
# Start only the databases
docker compose up auth-db device-db -d

# Verify containers are running
docker ps

# Connect to auth_db
docker exec -it auth-db psql -U iot_user -d auth_db

# Connect to device_db
docker exec -it device-db psql -U iot_user -d device_db
```

Docker port mapping:
- `auth-db` is accessible at `localhost:5434`
- `device-db` is accessible at `localhost:5433`

> If using Docker databases with locally-run services, update environment variables:
> ```bash
> # For auth-service
> export DB_HOST=localhost DB_PORT=5434 DB_NAME=auth_db
>
> # For device-service
> export DB_HOST=localhost DB_PORT=5433 DB_NAME=device_db
> ```

---

## Table Auto-Creation

You do **NOT** need to create tables manually. Hibernate will auto-create them on first startup because `ddl-auto: update` is configured in each service's `application.yml`.

The following tables are created automatically:

**auth_db:**
| Table | Columns |
|-------|---------|
| `users` | id (UUID PK), name, email (UNIQUE), password_hash, profile_image_url, created_at, updated_at |
| `refresh_tokens` | id (UUID PK), user_id (FK → users), token (UNIQUE), expires_at, created_at |

**device_db:**
| Table | Columns |
|-------|---------|
| `devices` | id (UUID PK), user_id (UUID), name, type (ENUM), is_online, latitude, longitude, address, location_label, mqtt_topic_prefix, created_at, updated_at |
| `device_controls` | id (UUID PK), device_id (FK → devices), name, control_type (ENUM), current_value, min_value, max_value, step, options (TEXT), mqtt_topic |

---

## Seed Data

After the services have started at least once (so tables exist), seed sample data.

### Seed Users (auth_db)

```bash
psql -U iot_user -d auth_db -h localhost
```

```sql
-- All users have password: password123
-- BCrypt hash for "password123"
INSERT INTO users (id, name, email, password_hash, profile_image_url, created_at, updated_at)
VALUES
  ('a1b2c3d4-e5f6-7890-abcd-ef1234567890',
   'John Doe', 'john@example.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   NULL, NOW(), NOW()),

  ('b2c3d4e5-f6a7-8901-bcde-f12345678901',
   'Jane Smith', 'jane@example.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   NULL, NOW(), NOW()),

  ('c3d4e5f6-a7b8-9012-cdef-123456789012',
   'Admin User', 'admin@example.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   NULL, NOW(), NOW());
```

### Seed Devices (device_db)

```bash
psql -U iot_user -d device_db -h localhost
```

```sql
-- Devices for John Doe
INSERT INTO devices (id, user_id, name, type, is_online, latitude, longitude, address, location_label, mqtt_topic_prefix, created_at, updated_at)
VALUES
  ('d4e5f6a7-b8c9-0123-defa-234567890123',
   'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
   'Living Room Light', 'LIGHT', true,
   28.6139, 77.2090, 'New Delhi, India', 'Living Room',
   'home/livingroom/light', NOW(), NOW()),

  ('e5f6a7b8-c9d0-1234-efab-345678901234',
   'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
   'Bedroom Thermostat', 'THERMOSTAT', true,
   28.6139, 77.2090, 'New Delhi, India', 'Bedroom',
   'home/bedroom/thermostat', NOW(), NOW()),

  ('f6a7b8c9-d0e1-2345-fabc-456789012345',
   'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
   'Front Door Lock', 'LOCK', false,
   28.6139, 77.2090, 'New Delhi, India', 'Front Door',
   'home/frontdoor/lock', NOW(), NOW());

-- Devices for Jane Smith
INSERT INTO devices (id, user_id, name, type, is_online, latitude, longitude, address, location_label, mqtt_topic_prefix, created_at, updated_at)
VALUES
  ('a7b8c9d0-e1f2-3456-abcd-567890123456',
   'b2c3d4e5-f6a7-8901-bcde-f12345678901',
   'Kitchen Fan', 'FAN', true,
   19.0760, 72.8777, 'Mumbai, India', 'Kitchen',
   'home/kitchen/fan', NOW(), NOW()),

  ('b8c9d0e1-f2a3-4567-bcde-678901234567',
   'b2c3d4e5-f6a7-8901-bcde-f12345678901',
   'Garden Sensor', 'SENSOR', true,
   19.0760, 72.8777, 'Mumbai, India', 'Garden',
   'home/garden/sensor', NOW(), NOW());
```

### Seed Device Controls (device_db)

```sql
INSERT INTO device_controls (id, device_id, name, control_type, current_value, min_value, max_value, step, options, mqtt_topic)
VALUES
  -- Living Room Light: power toggle + brightness slider
  ('11111111-1111-1111-1111-111111111111',
   'd4e5f6a7-b8c9-0123-defa-234567890123',
   'Power', 'TOGGLE', 'true', NULL, NULL, NULL, NULL,
   'home/livingroom/light/power'),

  ('22222222-2222-2222-2222-222222222222',
   'd4e5f6a7-b8c9-0123-defa-234567890123',
   'Brightness', 'SLIDER', '75', 0, 100, 1, NULL,
   'home/livingroom/light/brightness'),

  -- Bedroom Thermostat: temperature slider + mode dropdown
  ('33333333-3333-3333-3333-333333333333',
   'e5f6a7b8-c9d0-1234-efab-345678901234',
   'Temperature', 'SLIDER', '22', 16, 30, 0.5, NULL,
   'home/bedroom/thermostat/temp'),

  ('44444444-4444-4444-4444-444444444444',
   'e5f6a7b8-c9d0-1234-efab-345678901234',
   'Mode', 'DROPDOWN', 'cool', NULL, NULL, NULL, 'cool,heat,auto,dry',
   'home/bedroom/thermostat/mode'),

  -- Front Door Lock: lock toggle
  ('55555555-5555-5555-5555-555555555555',
   'f6a7b8c9-d0e1-2345-fabc-456789012345',
   'Lock', 'TOGGLE', 'true', NULL, NULL, NULL, NULL,
   'home/frontdoor/lock/state'),

  -- Kitchen Fan: power toggle + speed slider
  ('66666666-6666-6666-6666-666666666666',
   'a7b8c9d0-e1f2-3456-abcd-567890123456',
   'Power', 'TOGGLE', 'true', NULL, NULL, NULL, NULL,
   'home/kitchen/fan/power'),

  ('77777777-7777-7777-7777-777777777777',
   'a7b8c9d0-e1f2-3456-abcd-567890123456',
   'Speed', 'SLIDER', '3', 1, 5, 1, NULL,
   'home/kitchen/fan/speed'),

  -- Garden Sensor: refresh button
  ('88888888-8888-8888-8888-888888888888',
   'b8c9d0e1-f2a3-4567-bcde-678901234567',
   'Refresh', 'BUTTON', NULL, NULL, NULL, NULL, NULL,
   'home/garden/sensor/refresh');
```

---

## Seed Data Login Credentials

| Name | Email | Password |
|------|-------|----------|
| John Doe | john@example.com | password123 |
| Jane Smith | jane@example.com | password123 |
| Admin User | admin@example.com | password123 |

---

## Accessing Swagger UI

After services are running, open Swagger to test endpoints interactively:

| Service | Direct URL | Via API Gateway |
|---------|-----------|-----------------|
| Auth Service | http://localhost:8081/auth/swagger-ui.html | http://localhost:8080/auth/swagger-ui.html |
| Device Service | http://localhost:8082/devices/swagger-ui.html | http://localhost:8080/devices/swagger-ui.html |
| Weather Service | http://localhost:8084/weather/swagger-ui.html | http://localhost:8080/weather/swagger-ui.html |

### Testing Login via Swagger

1. Open Auth Service Swagger: `http://localhost:8081/auth/swagger-ui.html`
2. Expand **POST /auth/login**
3. Click **Try it out**
4. Enter the request body:
   ```json
   {
     "email": "john@example.com",
     "password": "password123"
   }
   ```
5. Click **Execute**
6. Copy the `token` from the response to use in other service endpoints

---

## Troubleshooting

### Port 5432 already in use
Your local PostgreSQL is using port 5432. Either:
- **Stop local PostgreSQL:** `sudo systemctl stop postgresql`
- **Or use Docker** which maps to different ports (5434, 5433)

### peer authentication failed
Edit `pg_hba.conf` to allow `md5` authentication for `iot_user`. See [Step 5 in Option A](#step-5--enable-password-authentication).

### Connection refused on localhost
Make sure PostgreSQL is listening on `localhost`. Check `postgresql.conf`:
```bash
sudo -u postgres psql -c "SHOW listen_addresses;"
```
If it shows `''`, update `postgresql.conf`:
```
listen_addresses = 'localhost'
```
Then restart: `sudo systemctl restart postgresql`

### Tables not created
Tables are created automatically when services start. Make sure:
1. Databases (`auth_db`, `device_db`) exist
2. `iot_user` has ownership or full privileges
3. The service started without errors (check logs)

### Reset everything
To drop and recreate databases from scratch:
```bash
sudo -u postgres psql -c "DROP DATABASE IF EXISTS auth_db;"
sudo -u postgres psql -c "DROP DATABASE IF EXISTS device_db;"
sudo -u postgres psql -c "CREATE DATABASE auth_db OWNER iot_user;"
sudo -u postgres psql -c "CREATE DATABASE device_db OWNER iot_user;"
sudo -u postgres psql -d auth_db -c "GRANT ALL ON SCHEMA public TO iot_user;"
sudo -u postgres psql -d device_db -c "GRANT ALL ON SCHEMA public TO iot_user;"
```
