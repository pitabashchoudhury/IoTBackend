# IoT Backend - Server Deployment Guide

## Network Scenario

```
                    ┌──────────────┐
                    │   Router     │
                    │ 192.168.x.1  │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │                         │
     ┌────────┴────────┐     ┌─────────┴────────┐
     │  Ubuntu Server   │     │  Client PC        │
     │  192.168.x.100   │     │  192.168.x.101    │
     │                  │     │                   │
     │  Runs:           │     │  Connects via:    │
     │  - All services  │     │  - SSH (setup)    │
     │  - PostgreSQL    │     │  - Browser :3000  │
     │  - Frontend      │     │  - API     :8080  │
     └──────────────────┘     └───────────────────┘
```

> Replace `192.168.x.100` with your actual server IP throughout this guide.

---

## Prerequisites

- Ubuntu Server machine connected to your local network (router)
- Client PC connected to the same network/router
- Both machines can reach each other over the network

---

## PHASE 1: Initial Server Setup (One-Time, Physical Access)

> You need physical access to the server (keyboard + monitor) only for this phase.
> After this, everything is done via SSH from your client PC.

### Step 1: Setup SSH on the Server

Login to the server physically (keyboard + monitor) and run:

```bash
# Install OpenSSH server
sudo apt update
sudo apt install -y openssh-server

# Start and enable SSH service
sudo systemctl start ssh
sudo systemctl enable ssh

# Verify SSH is running
sudo systemctl status ssh
```

You should see `active (running)` in the output.

### Step 2: Find the Server IP Address

```bash
ip addr show | grep "inet " | grep -v 127.0.0.1
```

Note down the IP address (e.g., `192.168.1.100`). You will use this for everything from now on.

### Step 3: Create a Deployment User (Optional but Recommended)

```bash
# Create a dedicated user for deployment
sudo adduser deployer

# Give sudo access
sudo usermod -aG sudo deployer
```

> Or you can use your existing user account. From here on, we'll use `deployer` as the username — replace it with your actual username.

After this step, you can disconnect the keyboard/monitor from the server. Everything else is done remotely via SSH from your client PC.

---

## PHASE 2: Connect from Client PC via SSH

### Step 4: SSH into the Server

Open a terminal on your **Client PC**:

**Linux / macOS:**
```bash
ssh deployer@192.168.x.100
```

**Windows (PowerShell or CMD):**
```powershell
ssh deployer@192.168.x.100
```

**Windows (PuTTY):**
1. Open PuTTY
2. Host Name: `192.168.x.100`
3. Port: `22`
4. Connection type: SSH
5. Click "Open"
6. Login as: `deployer`

> Type `yes` when prompted about the host fingerprint (first time only).
> Enter your password when prompted.

### Step 5: (Optional) Setup SSH Key Authentication

This avoids typing your password every time.

**On your Client PC**, run:

```bash
# Generate SSH key pair (press Enter for all prompts)
ssh-keygen -t ed25519 -C "iot-deployment"

# Copy the public key to the server
ssh-copy-id deployer@192.168.x.100
```

Now you can SSH without a password:

```bash
ssh deployer@192.168.x.100
```

---

## PHASE 3: Install Prerequisites on Server (via SSH)

> All commands from here are run **inside the SSH session** on the server.

### Step 6: Update System

```bash
sudo apt update && sudo apt upgrade -y
```

### Step 7: Install Git

```bash
sudo apt install -y git
git --version
```

### Step 8: Install Docker and Docker Compose

```bash
# Install dependencies
sudo apt install -y ca-certificates curl gnupg

# Add Docker's official GPG key
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Add Docker repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Allow Docker commands without sudo
sudo usermod -aG docker $USER
```

> **IMPORTANT:** After adding yourself to the docker group, you must logout and login again for it to take effect:

```bash
exit
```

Then SSH back in:

```bash
ssh deployer@192.168.x.100
```

Verify Docker:

```bash
docker --version
docker compose version
```

### Step 9: Install Java 17 and Maven

```bash
sudo apt install -y openjdk-17-jdk maven

# Verify
java -version
mvn -version
```

---

## PHASE 4: Transfer Project to Server (via SSH)

### Step 10: Transfer the Project Files

Choose one of these options:

#### Option A: Clone from Git (Recommended)

Run inside your SSH session on the server:

```bash
cd ~
git clone <your-repo-url>/iot-backend.git
git clone <your-repo-url>/iot-frontend.git
```

#### Option B: Copy from Client PC using SCP

Open a **new terminal on your Client PC** (don't close the SSH session) and run:

```bash
# Copy backend project
scp -r /path/to/iot-backend deployer@192.168.x.100:~/iot-backend

# Copy frontend project
scp -r /path/to/iot-frontend deployer@192.168.x.100:~/iot-frontend
```

> **Why both repos?** The `docker-compose.yml` references `../iot-frontend` to build the frontend container.

#### Option C: Copy using rsync (faster for large projects, resumes on failure)

From your **Client PC**:

```bash
rsync -avz --progress /path/to/iot-backend deployer@192.168.x.100:~/
rsync -avz --progress /path/to/iot-frontend deployer@192.168.x.100:~/
```

### Verify folder structure

Back in your SSH session, run:

```bash
ls -la ~/iot-backend/
ls -la ~/iot-frontend/
```

Expected structure:

```
~/
├── iot-backend/
│   ├── docker-compose.yml
│   ├── pom.xml
│   ├── init-auth-db.sql
│   ├── init-device-db.sql
│   ├── api-gateway/
│   ├── auth-service/
│   ├── device-service/
│   ├── weather-service/
│   ├── discovery-server/
│   ├── common-lib/
│   └── ...
└── iot-frontend/
    ├── Dockerfile
    └── ...
```

---

## PHASE 5: Build and Deploy (via SSH)

### Step 11: Build the Maven Project

```bash
cd ~/iot-backend

# Build all modules (this takes 2-5 minutes on first run)
mvn clean package -DskipTests
```

Wait for `BUILD SUCCESS`. If the build fails due to memory, increase swap:

```bash
# Only if build fails with OutOfMemoryError
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
# Then retry: mvn clean package -DskipTests
```

Verify JARs were created:

```bash
ls discovery-server/target/*.jar
ls api-gateway/target/*.jar
ls auth-service/target/*.jar
ls device-service/target/*.jar
ls weather-service/target/*.jar
```

### Step 12: Configure Environment Variables

```bash
cd ~/iot-backend
nano .env
```

Add:

```env
# ==============================
# PRODUCTION CONFIGURATION
# ==============================

# IMPORTANT: Change this to a secure random string (minimum 32 characters)
JWT_SECRET=your-super-secret-production-key-change-this-to-something-random-and-long!!

# Database credentials (change for production)
DB_USERNAME=iot_user
DB_PASSWORD=iot_pass

# Weather API key (get from https://openweathermap.org/api)
WEATHER_API_KEY=your_openweathermap_api_key_here
```

Save: `Ctrl+O` > `Enter` > `Ctrl+X`

### Step 13: Configure Firewall

```bash
# Enable firewall
sudo ufw enable

# IMPORTANT: Allow SSH first (so you don't lock yourself out!)
sudo ufw allow 22/tcp

# Allow application ports for network access
sudo ufw allow 3000/tcp    # Frontend
sudo ufw allow 8080/tcp    # API Gateway (main entry point)
sudo ufw allow 8761/tcp    # Eureka Dashboard (optional, for monitoring)

# Verify
sudo ufw status
```

Expected output:

```
Status: active

To                         Action      From
--                         ------      ----
22/tcp                     ALLOW       Anywhere
3000/tcp                   ALLOW       Anywhere
8080/tcp                   ALLOW       Anywhere
8761/tcp                   ALLOW       Anywhere
```

> Ports 8081, 8082, 8084, 5433, 5434 are internal — all client traffic goes through the API Gateway (8080).

### Step 14: Build Docker Images and Start Containers

```bash
cd ~/iot-backend

# Build images and start all containers in background
docker compose up -d --build
```

This will:
1. Build Docker images for all 6 services + frontend
2. Pull PostgreSQL 16 Alpine images (first time only)
3. Start everything with health checks

### Step 15: Monitor Startup

```bash
# Watch logs (Ctrl+C to stop watching)
docker compose logs -f
```

Or check status:

```bash
docker compose ps
```

Wait **2-3 minutes** for all services to start. The order is:
1. `auth-db`, `device-db` (databases)
2. `discovery-server` (Eureka)
3. `auth-service`, `device-service`, `weather-service`
4. `api-gateway`
5. `iot-frontend`

Expected output when all are ready:

```
NAME                STATUS                   PORTS
auth-db             Up (healthy)             0.0.0.0:5434->5432/tcp
device-db           Up (healthy)             0.0.0.0:5433->5432/tcp
discovery-server    Up (healthy)             0.0.0.0:8761->8761/tcp
api-gateway         Up                       0.0.0.0:8080->8080/tcp
auth-service        Up                       0.0.0.0:8081->8081/tcp
device-service      Up                       0.0.0.0:8082->8082/tcp
weather-service     Up                       0.0.0.0:8084->8084/tcp
iot-frontend        Up                       0.0.0.0:3000->80/tcp
```

### Step 16: Initialize Database (First Time Only)

```bash
cd ~/iot-backend

# Seed auth database (users)
docker exec -i auth-db psql -U iot_user -d auth_db < init-auth-db.sql

# Seed device database (devices)
docker exec -i device-db psql -U iot_user -d device_db < init-device-db.sql
```

### Step 17: Verify Deployment (from SSH session)

```bash
# Check Eureka health
curl -s http://localhost:8761/actuator/health
# Expected: {"status":"UP"}

# Check API Gateway health
curl -s http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

# Test login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"pitabash@example.com","password":"password123"}'
```

If the login returns a JSON with `access_token`, the deployment is successful.

---

## PHASE 6: Access from Client PC (Browser / Postman)

Open a browser on your **Client PC** and navigate to:

### Frontend (Web UI)

```
http://192.168.x.100:3000
```

### API Gateway (Postman / curl)

```
http://192.168.x.100:8080
```

### Eureka Dashboard (Service Monitoring)

```
http://192.168.x.100:8761
```

### Swagger API Docs

```
http://192.168.x.100:8081/swagger-ui.html   # Auth Service
http://192.168.x.100:8082/swagger-ui.html   # Device Service
```

### Test API from Client PC

```bash
# Login
curl -X POST http://192.168.x.100:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"pitabash@example.com","password":"password123"}'

# Copy the access_token from the response, then:
curl http://192.168.x.100:8080/api/devices \
  -H "Authorization: Bearer <paste_access_token_here>"
```

### Default Test Users

| Email                  | Password      |
|------------------------|---------------|
| pitabash@example.com   | password123   |
| john@example.com       | password123   |
| jane@example.com       | password123   |

---

## Managing the Deployment (via SSH)

SSH into the server first:

```bash
ssh deployer@192.168.x.100
cd ~/iot-backend
```

### Stop all services

```bash
docker compose down
```

### Stop and delete all data (including databases)

```bash
docker compose down -v
```

### Restart a specific service

```bash
docker compose restart auth-service
```

### View logs of a specific service

```bash
docker compose logs -f api-gateway
docker compose logs -f auth-service
docker compose logs --tail=100 device-service
```

### Rebuild after code changes

```bash
mvn clean package -DskipTests
docker compose up -d --build
```

### Rebuild a single service

```bash
# Example: only rebuild auth-service
cd ~/iot-backend/auth-service
mvn clean package -DskipTests
cd ~/iot-backend
docker compose up -d --build auth-service
```

### Auto-start on Server Boot

So containers start automatically when the server reboots:

```bash
# Enable Docker to start on boot
sudo systemctl enable docker

# Set restart policy on all running containers
docker update --restart unless-stopped $(docker ps -q)
```

### Check Resource Usage

```bash
# Container resource usage
docker stats --no-stream

# System memory
free -h

# Disk space
df -h
```

---

## Updating the Application

When you have code changes to deploy:

### Option A: Pull from Git

```bash
ssh deployer@192.168.x.100
cd ~/iot-backend
git pull origin main
mvn clean package -DskipTests
docker compose up -d --build
```

### Option B: Push from Client PC via SCP

From your **Client PC**:

```bash
# Sync changes
rsync -avz --progress /path/to/iot-backend/ deployer@192.168.x.100:~/iot-backend/
```

Then in your **SSH session**:

```bash
cd ~/iot-backend
mvn clean package -DskipTests
docker compose up -d --build
```

---

## Troubleshooting

### SSH connection refused

```bash
# On the server (physical access needed):
sudo systemctl status ssh
sudo systemctl start ssh
sudo ufw allow 22/tcp
```

### SSH connection timeout

- Verify both machines are on the same network: `ping 192.168.x.100` from client
- Check the server's IP hasn't changed: `ip addr show` on server
- Check router settings — some routers block device-to-device communication (AP isolation)

### Cannot access app from client browser

1. **Ping test**: `ping 192.168.x.100` from client
2. **Check firewall**: `sudo ufw status` on server
3. **Check Docker ports**: `sudo ss -tlnp | grep 8080` should show `0.0.0.0:8080`
4. **Check containers**: `docker compose ps` — all should be `Up`

### Containers keep restarting

```bash
# Check which container is failing
docker compose ps

# Check its logs
docker compose logs --tail=50 <service-name>

# Common causes:
# - Out of memory → check with: free -h
# - Database not ready → wait 30 seconds and check again
# - Port conflict → sudo ss -tlnp | grep <port>
```

### Build fails with OutOfMemoryError

```bash
# Add swap space
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Make it permanent
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Retry build
mvn clean package -DskipTests
```

### Frontend cannot reach API Gateway

If the frontend calls `localhost:8080`, it won't work from a client browser because `localhost` refers to the client machine, not the server. You need to update the frontend configuration to use the server's IP address or use a relative URL.

### Database connection refused

```bash
docker compose ps auth-db device-db
docker compose logs auth-db
docker compose logs device-db
```

### SSH session disconnects during long builds

Use `tmux` or `screen` to keep processes running after disconnect:

```bash
# Install tmux
sudo apt install -y tmux

# Start a tmux session
tmux new -s deploy

# Run your commands inside tmux...
mvn clean package -DskipTests
docker compose up -d --build

# Detach: press Ctrl+B, then D
# Reattach later:
tmux attach -t deploy
```

---

## Quick Reference Card

### SSH into server
```bash
ssh deployer@192.168.x.100
```

### Full deploy (from SSH session)
```bash
cd ~/iot-backend
git pull origin main
mvn clean package -DskipTests
docker compose up -d --build
```

### Check status
```bash
docker compose ps
docker compose logs -f
```

### Access URLs (from client browser)
```
Frontend:  http://192.168.x.100:3000
API:       http://192.168.x.100:8080
Eureka:    http://192.168.x.100:8761
```

---

## Port Reference

| Port | Service            | Exposed to Network? | Purpose                    |
|------|--------------------|---------------------|----------------------------|
| 22   | SSH                | Yes                 | Remote server access       |
| 3000 | Frontend           | Yes                 | Web UI                     |
| 8080 | API Gateway        | Yes                 | All API requests           |
| 8761 | Eureka Dashboard   | Yes (optional)      | Service monitoring         |
| 8081 | Auth Service       | No (internal)       | Authentication             |
| 8082 | Device Service     | No (internal)       | Device management          |
| 8084 | Weather Service    | No (internal)       | Weather data               |
| 5434 | Auth PostgreSQL    | No (internal)       | Auth database              |
| 5433 | Device PostgreSQL  | No (internal)       | Device database            |

---

## Minimum Server Requirements

| Resource | Minimum   | Recommended |
|----------|-----------|-------------|
| RAM      | 4 GB      | 8 GB        |
| CPU      | 2 cores   | 4 cores     |
| Disk     | 20 GB     | 50 GB       |
| OS       | Ubuntu 20.04+ | Ubuntu 22.04 LTS |
