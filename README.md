# 17-Track Order Tracking Service MVP

A comprehensive Java-based order tracking service that integrates with the 17-Track API to provide real-time shipment tracking, webhook notifications, and intelligent retry mechanisms.

## 🚀 Features

### Core Functionality
- **Single & Batch Tracking Registration** - Register one or multiple tracking numbers
- **CSV Import** - Bulk import tracking numbers from CSV files
- **Real-time Status Updates** - Query current tracking status and events
- **Webhook Integration** - Receive push notifications from 17-Track
- **Carrier Auto-detection** - Automatically identify carriers
- **Full Event History** - Complete tracking timeline with all events

### Technical Features
- **Redis Caching** - Token caching, rate limiting, and status caching
- **Rate Limit Protection** - Respect 17-Track API limits (5000 register/day, 10000 query/day)
- **Intelligent Retry** - Exponential backoff retry mechanism
- **MySQL Persistence** - Store tracking data, events, and history
- **RESTful API** - Complete CRUD operations
- **Scheduled Sync** - Automatic background synchronization
- **Error Handling** - Comprehensive exception handling and logging

## 📋 Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 7.0+**
- **17-Track API Key** (Get from https://api.17track.net)

## 🛠️ Installation & Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd track-17-service
```

### 2. Configure Application

Edit `src/main/resources/application.yml`:

```yaml
tracking:
  api:
    key: YOUR_17TRACK_API_KEY_HERE
  webhook:
    secret: YOUR_WEBHOOK_SECRET_HERE
```

### 3. Setup Database

```bash
# Connect to MySQL
mysql -u root -p

# Run schema.sql
mysql -u root -p < schema.sql
```

### 4. Build & Run

#### Option A: Maven
```bash
mvn clean install
mvn spring-boot:run
```

#### Option B: Docker Compose
```bash
# Set environment variables
export TRACK_17_API_KEY=your_api_key
export WEBHOOK_SECRET=your_webhook_secret

# Start all services
docker-compose up -d
```

### 5. Access the Application

- **API**: http://localhost:8080/api
- **Web UI**: Open `index.html` in browser
- **Health Check**: http://localhost:8080/api/actuator/health

## 📚 API Documentation

### Register Single Tracking

```http
POST /api/tracking/register
Content-Type: application/json

{
  "tracking_number": "1234567890",
  "carrier": 101,
  "description": "Order #12345"
}
```

### Register Batch

```http
POST /api/tracking/batch
Content-Type: application/json

{
  "trackings": [
    {
      "tracking_number": "1234567890",
      "carrier": 101,
      "description": "Order #1"
    },
    {
      "tracking_number": "0987654321",
      "carrier": 102,
      "description": "Order #2"
    }
  ]
}
```

### Import CSV

```http
POST /api/tracking/import-csv
Content-Type: multipart/form-data

file: tracking_list.csv
```

**CSV Format:**
```csv
tracking_number,carrier_code,description
1234567890,101,Order #1
0987654321,102,Order #2
```

### Query Tracking by ID

```http
GET /api/tracking/{id}
```

### Query Tracking by Number

```http
GET /api/tracking/number/{trackingNumber}
```

### Get All Trackings

```http
GET /api/tracking/all
```

### Sync Tracking

```http
POST /api/tracking/{id}/sync
```

### Get All Carriers

```http
GET /api/carriers
```

### Sync Carriers

```http
POST /api/carriers/sync
```

### Webhook Endpoint

```http
POST /api/webhook/17track
Content-Type: application/json
X-Webhook-Signature: <signature>

{
  "event": "InTransit",
  "accepted": [
    {
      "number": "1234567890",
      "carrier": 101,
      "track": { ... }
    }
  ]
}
```

## 🗂️ Project Structure

```
track-17-service/
├── src/main/java/com/tracking/
│   ├── TrackingServiceApplication.java
│   ├── client/
│   │   └── SeventeenTrackClient.java
│   ├── config/
│   │   ├── RedisConfig.java
│   │   ├── WebClientConfig.java
│   │   ├── CorsConfig.java
│   │   └── TrackingProperties.java
│   ├── controller/
│   │   ├── TrackingController.java
│   │   ├── WebhookController.java
│   │   └── CarrierController.java
│   ├── dto/
│   │   ├── RegisterTrackingRequest.java
│   │   ├── TrackingResponse.java
│   │   ├── WebhookPayload.java
│   │   └── ... (other DTOs)
│   ├── entity/
│   │   ├── TrackingNumber.java
│   │   ├── TrackingEvent.java
│   │   ├── Carrier.java
│   │   └── WebhookEvent.java
│   ├── exception/
│   │   └── GlobalExceptionHandler.java
│   ├── repository/
│   │   ├── TrackingNumberRepository.java
│   │   ├── TrackingEventRepository.java
│   │   ├── CarrierRepository.java
│   │   └── WebhookEventRepository.java
│   └── service/
│       ├── TrackingService.java
│       ├── WebhookService.java
│       ├── CarrierService.java
│       ├── RedisCacheService.java
│       └── SchedulerService.java
├── src/main/resources/
│   └── application.yml
├── schema.sql
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── index.html (Frontend)
└── README.md
```

## 🔄 Background Jobs

### Retry Job
- **Frequency**: Every 5 minutes
- **Purpose**: Retry failed tracking syncs with exponential backoff
- **Max Attempts**: 5

### Sync Job
- **Frequency**: Every 5 minutes
- **Purpose**: Sync active trackings not updated in last 5 minutes
- **Limit**: 50 trackings per run

### Cleanup Job
- **Frequency**: Daily at 2 AM
- **Purpose**: 
  - Delete webhook events older than 30 days
  - Mark trackings older than 90 days as expired

### Health Check
- **Frequency**: Every 10 minutes
- **Purpose**: Monitor system health and log statistics

## 📊 Database Schema

### tracking_numbers
- `id` (PK)
- `tracking_number` (UNIQUE)
- `carrier_code`
- `carrier_name`
- `description`
- `status` (PENDING, IN_TRANSIT, DELIVERED, etc.)
- `substatus`
- `created_at`
- `updated_at`
- `last_synced_at`
- `retry_count`
- `next_retry_at`
- `track_info` (JSON)

### tracking_events
- `id` (PK)
- `tracking_id` (FK)
- `tracking_number`
- `event_time`
- `event_description`
- `event_location`
- `event_code`
- `stage`
- `substatus`
- `event_data` (JSON)
- `created_at`

### carriers
- `carrier_code` (PK)
- `carrier_name`
- `carrier_key`
- `country`
- `phone`
- `website`
- `logo_url`
- `is_active`

### webhook_events
- `id` (PK)
- `event_id` (UNIQUE)
- `event_type`
- `tracking_number`
- `payload` (JSON)
- `processed`
- `processed_at`
- `created_at`

## 🎯 Tracking Status Codes

| Status | Description |
|--------|-------------|
| PENDING | Registered, not yet tracked |
| NOT_FOUND | Not found by carrier |
| IN_TRANSIT | In transit |
| PICK_UP | Ready for pickup |
| UNDELIVERED | Delivery attempt failed |
| DELIVERED | Successfully delivered |
| ALERT | Exception/Alert |
| EXPIRED | Tracking expired |
| ERROR | System error |

## 🔧 Configuration

### Rate Limits (from application.yml)
```yaml
tracking:
  rate-limit:
    register-per-day: 5000
    query-per-day: 10000
```

### Cache TTL
```yaml
tracking:
  cache:
    token-ttl: 3600      # 1 hour
    status-ttl: 300      # 5 minutes
    carrier-ttl: 86400   # 24 hours
```

### Retry Configuration
```yaml
tracking:
  retry:
    max-attempts: 5
    initial-delay: 60000      # 1 minute
    max-delay: 3600000        # 1 hour
    multiplier: 2.0           # Exponential
```

## 🚀 Deployment

### Docker Deployment

```bash
# Build and start
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

### Manual Deployment

```bash
# Build JAR
mvn clean package

# Run application
java -jar target/track-17-service-1.0.0-SNAPSHOT.jar
```

## 🧪 Testing

### Test Tracking Registration
```bash
curl -X POST http://localhost:8080/api/tracking/register \
  -H "Content-Type: application/json" \
  -d '{
    "tracking_number": "9400100000000000000000",
    "carrier": 101,
    "description": "Test Package"
  }'
```

### Test Webhook
```bash
curl -X POST http://localhost:8080/api/webhook/17track \
  -H "Content-Type: application/json" \
  -d '{
    "event": "InTransit",
    "accepted": [
      {
        "number": "9400100000000000000000",
        "carrier": 101,
        "track": {
          "status": 20,
          "substatus": 2001
        }
      }
    ]
  }'
```

## 📝 Logging

Logs are stored in:
- **Console**: INFO level and above
- **File**: `logs/tracking-service.log`

Debug logging for package:
```yaml
logging:
  level:
    com.tracking: DEBUG
```

## 🔒 Security

- Webhook signature validation (HMAC-SHA256)
- CORS configuration for frontend access
- Rate limiting to prevent abuse
- Input validation on all endpoints

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

For issues and questions:
- Create an issue on GitHub
- Check 17-Track API documentation: https://api.17track.net/en/doc

## 🙏 Acknowledgments

- 17-Track API for shipment tracking services
- Spring Boot framework
- Redis for caching
- MySQL for data persistence

---

**Note**: Remember to keep your API key secure and never commit it to version control!