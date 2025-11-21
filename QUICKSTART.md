# Quick Start Guide

Get the Distributed Rate Limiter running in 5 minutes!

## Prerequisites Checklist

- [ ] Java 17+ installed (`java -version`)
- [ ] Maven 3.8+ installed (`mvn -version`)
- [ ] Redis installed and running (`redis-cli ping`)
- [ ] Git installed

## Step-by-Step Setup

### 1. Start Redis (Choose One)

**Option A: Docker (Recommended)**
```bash
docker run -d --name redis -p 6379:6379 redis:latest
```

**Option B: Local Redis**
```bash
redis-server
```

**Verify Redis:**
```bash
redis-cli ping
# Expected: PONG
```

---

### 2. Build and Run Application

```bash
# Navigate to backend
cd backend

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

**Expected Output:**
```
Started RateLimiterBackendApplication in 3.5 seconds
```

---

### 3. Test the API

#### Get JWT Token
```bash
curl -X POST http://localhost:8080/token \
  -u user1:password
```

**Copy the token from response**

#### Use the Token
```bash
# Replace YOUR_TOKEN with actual token
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/info
```

---

## Quick Test Commands

Save this as `test.sh`:

```bash
#!/bin/bash

# Get token
echo "Getting JWT token..."
TOKEN=$(curl -s -X POST http://localhost:8080/token \
  -u user1:password | jq -r '.token')

echo "Token received: ${TOKEN:0:20}..."

# Make requests
echo -e "\nMaking 10 requests..."
for i in {1..10}; do
  echo "Request $i:"
  curl -s -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/api/v1/info | jq '.totalRequestsThisMinute'
done

echo -e "\nTest complete!"
```

Run:
```bash
chmod +x test.sh
./test.sh
```

---

## Postman Collection

### 1. Create Token Request
- **Method:** POST
- **URL:** `http://localhost:8080/token`
- **Auth Type:** Basic Auth
  - Username: `user1`
  - Password: `password`
- **Save** the token from response

### 2. Create Info Request
- **Method:** GET
- **URL:** `http://localhost:8080/api/v1/info`
- **Headers:**
  - Key: `Authorization`
  - Value: `Bearer {{token}}`

---

## Docker Quick Start

```bash
# Start both Redis and App
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

---

## Verify Installation

Run this command:
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

---

## Next Steps

1. Application is running
2. Read [README.md](README.md) for detailed documentation
3. Run tests: `mvn test`
4. Customize [rules.json](backend/src/main/resources/rules.json)
5. Deploy to production

---

## Troubleshooting

### Redis Connection Error
```bash
# Check Redis status
redis-cli ping

# If not running, start Redis
redis-server

# Or use Docker
docker run -d -p 6379:6379 redis
```

### Port Already in Use
```bash
# Change port in application.properties
server.port=8081

# Or kill process on port 8080
# Windows:
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac:
lsof -ti:8080 | xargs kill -9
```

### JWT Token Error
- Ensure keys exist in `backend/src/main/resources/`
- Regenerate keys if needed:
```bash
java -cp target/classes com.distributed.rate.limiter.KeyGenerator
```

---

## Available Test Users

The application comes with 10 pre-configured users:

- user1 / password
- user2 / password
- user3 / password
- user4 / password
- user5 / password
- user6 / password
- user7 / password
- user8 / password
- user9 / password
- user10 / password

Each user has independent rate limits.

---

## Rate Limit Rules

The default configuration includes:

| Rule | Limit | Window | Algorithm |
|------|-------|--------|-----------|
| Per Minute | 15 requests | 60 seconds | Fixed Window |
| Per Hour | 300 requests | 3600 seconds | Fixed Window |
| Per Day | 7200 requests | 86400 seconds | Fixed Window |
| Per Minute (Sliding) | 5 requests | 60 seconds | Sliding Window |

Edit `backend/src/main/resources/rules.json` to customize.

---

## Quick Test Scenarios

### Scenario 1: Test Fixed Window Rate Limit

```bash
# Get token
TOKEN=$(curl -s -X POST http://localhost:8080/token \
  -u user1:password | jq -r '.token')

# Make 20 requests quickly
for i in {1..20}; do
  echo "Request $i:"
  curl -s -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/api/v1/info | jq '.totalRequestsThisMinute'
done
```

You should see the rate limit kick in after 15 requests.

### Scenario 2: Test Multiple Users

```bash
# User 1
TOKEN1=$(curl -s -X POST http://localhost:8080/token \
  -u user1:password | jq -r '.token')

# User 2
TOKEN2=$(curl -s -X POST http://localhost:8080/token \
  -u user2:password | jq -r '.token')

# Each user can make requests independently
curl -H "Authorization: Bearer $TOKEN1" \
  http://localhost:8080/api/v1/info

curl -H "Authorization: Bearer $TOKEN2" \
  http://localhost:8080/api/v1/info
```

### Scenario 3: Monitor Redis Keys

```bash
# Watch rate limit keys being created
redis-cli --scan --pattern "rate:*"

# Monitor in real-time
redis-cli monitor
```

---

## Performance Tips

1. **Use Connection Pooling** - Already configured in the application
2. **Monitor Redis Memory** - Use `redis-cli info memory`
3. **Adjust Rate Limits** - Modify `rules.json` based on your needs
4. **Scale Horizontally** - Run multiple instances with shared Redis

---

## Common Commands

```bash
# Build without tests
mvn clean package -DskipTests

# Run specific test
mvn test -Dtest=FixedWindowLimiterTest

# Check application logs
tail -f backend/logs/application.log

# View Redis data
redis-cli
> KEYS rate:*
> GET rate:user1:12345
```

---

## Environment Variables

For production deployment, set these variables:

```bash
export REDIS_HOST=your-redis-host
export REDIS_PORT=6379
export REDIS_PASSWORD=your-secure-password
export JWT_TOKEN_EXPIRY_SECONDS=86400
```

---

## Need Help?

- Check [README.md](README.md) for detailed documentation
- Check [Troubleshooting](#troubleshooting) section above
- View application logs in `backend/logs/`
- Test Redis connection: `redis-cli ping`
- Verify application health: `curl http://localhost:8080/actuator/health`

---

