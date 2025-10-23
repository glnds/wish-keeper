# Wish Keeper

A Java-based REST API application for managing wishes and people through a PostgreSQL database. The application uses a custom HTTP server implementation with Jackson for JSON processing.

## Features

- **Wish Management**: Create, retrieve, and replace wishes for beneficiaries
- **People Management**: Register and update people with optimistic locking
- **Wish Fulfillment**: Proof-of-work based wish fulfillment system using SHA-256 mining
- **Geographic Distance**: Calculate distance to North Pole using Haversine formula
- **Business Rules**: Enforce constraints (max 3 wishes per beneficiary, validation, etc.)

## Prerequisites

### Option 1: Docker/Finch (Recommended)
- **Docker** or **AWS Finch**: Container runtime for running the application
- No need for local Java, Maven, or PostgreSQL installation

### Option 2: Local Development
- **Java**: JDK 8 or higher (configured for Java 16)
- **Maven**: 3.6+ for building and running
- **PostgreSQL**: 9.5+ with database `webapp_db`
- **Database Access**: User `geert` with appropriate permissions

## Quick Start

### Option A: Using Docker/Finch (Recommended)

The easiest way to run the application is with Docker or AWS Finch:

```bash
# Using Docker Compose
docker compose up

# Or using AWS Finch
finch compose up
```

This will:
- Build the Java application in a container
- Start a PostgreSQL database container
- Automatically initialize the database schema
- Start the application on http://localhost:8000

To stop the containers:
```bash
# Using Docker
docker compose down

# Or using AWS Finch
finch compose down
```

To rebuild after code changes:
```bash
# Using Docker
docker compose up --build

# Or using AWS Finch
finch compose up --build
```

### Option B: Local Development

#### 1. Database Setup

Create the database and tables:

```bash
# Create database
createdb webapp_db

# Run schema files
psql -d webapp_db -f src/main/resources/People.pgsql
psql -d webapp_db -f src/main/resources/wishes.pgsql
```

#### 2. Build and Run

```bash
# Build the project
mvn compile
# or
make build

# Run the application
mvn exec:java
# or
make run

# Run tests
mvn test
# or
make test
```

The server will start on `http://localhost:8000`

## API Endpoints

### Health Check
```http
GET /api/hello
```
Returns: `"Hello, World!"`

### Wishes

#### Get All Wishes
```http
GET /api/wish
```

#### Create Wish
```http
POST /api/wish
Content-Type: application/json

{
  "id": "wish123",
  "productName": "Red Bicycle",
  "quantity": 1,
  "beneficiaryId": 1
}
```

**Validation:**
- Beneficiary must exist
- Max 3 wishes per beneficiary
- Product name cannot be empty
- Quantity cannot be negative

#### Replace Wish
```http
PUT /api/wishreplace
Content-Type: application/json

{
  "id": "wish456",
  "productName": "Blue Bicycle",
  "quantity": 1,
  "beneficiaryId": 1,
  "idOfWishToBeReplaced": "wish123"
}
```

### People

#### Register Person
```http
POST /api/people
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "2010-05-15",
  "addressLocation": {
    "latitude": 45.0,
    "longitude": 0.0
  },
  "behavior": "nice"
}
```

**Note:** Do not include `id`, `version`, or `timeOfRegistration` - these are auto-generated.

#### Get All People
```http
GET /api/people
```

#### Update Person
```http
PUT /api/people
Content-Type: application/json

{
  "id": 1,
  "version": 1,
  "firstName": "Jane",
  "lastName": "Doe",
  "dateOfBirth": "2010-05-15",
  "addressLocation": {
    "latitude": 50.0,
    "longitude": 5.0
  },
  "behavior": "nice"
}
```

**Note:** Uses optimistic locking - must provide correct `version` number.

### Wish Fulfillment

#### Fulfill Wish
```http
POST /api/wishfulfill
Content-Type: application/json

{
  "id": "wish123"
}
```

This endpoint performs proof-of-work mining:
- Calculates distance from beneficiary's location to North Pole
- Sets mining difficulty based on distance
- Finds valid SHA-256 hash meeting difficulty requirement
- Can take significant time for distant locations

## Database Schema

### People Table
```sql
CREATE TABLE people (
    id SERIAL PRIMARY KEY,
    firstName VARCHAR(255),
    lastName VARCHAR(255),
    dateOfBirth DATE,
    timeOfRegistration TIMESTAMP,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    version INTEGER,
    behavior behavior_enum DEFAULT 'nice'
);
```

### Wishes Table
```sql
CREATE TABLE wishes (
    id VARCHAR(30) PRIMARY KEY,
    productName VARCHAR(120) NOT NULL,
    quantity INTEGER NOT NULL,
    beneficiaryId INTEGER NOT NULL,
    CONSTRAINT fk_beneficiary FOREIGN KEY (beneficiaryId)
        REFERENCES people (id) ON DELETE CASCADE
);
```

## Architecture

### Project Structure
```
src/
├── main/
│   ├── java/cloud/dpgmedia/
│   │   ├── BasicApi.java              # Main entry point & HTTP server
│   │   ├── handlers/
│   │   │   ├── WishHandler            # Wish CRUD operations
│   │   │   ├── PeopleHandler          # People management
│   │   │   ├── WishReplacementHandler # Wish replacement logic
│   │   │   └── WishFulfillmentHandler # Proof-of-work fulfillment
│   │   ├── stores/
│   │   │   ├── WishStorePostgres      # Wish persistence
│   │   │   └── PeopleStorePostgres    # People persistence
│   │   └── models/
│   │       ├── Wish, Person, Location # Domain models
│   │       └── DTOs                    # Data transfer objects
│   └── resources/
│       ├── wishes.pgsql                # Wishes table schema
│       ├── People.pgsql                # People table schema
│       ├── Wishes.http                 # HTTP request examples
│       └── People.http                 # HTTP request examples
└── test/
    └── java/cloud/dpgmedia/
        ├── WishTest                    # Wish validation tests
        ├── LocationTest                # Geographic calculation tests
        ├── HashCollisionTest           # Proof-of-work tests
        └── WishStorePostgresTest       # Database persistence tests
```

### Technology Stack

- **Java**: Core language (Java 8/16)
- **HTTP Server**: Java's `com.sun.net.httpserver.HttpServer`
- **JSON Processing**: Jackson Databind 2.20.0
- **Database**: PostgreSQL 42.7.7 driver
- **Testing**: JUnit 3.8.2
- **Build Tool**: Maven 3.x

## Business Rules

1. **Maximum Wishes**: Each beneficiary can have a maximum of 3 wishes
2. **Wish Validation**:
   - Product name must not be empty
   - Quantity must be non-negative
   - Beneficiary must exist in the database
3. **Optimistic Locking**: Person updates use version-based concurrency control
4. **Behavior Types**: People can be marked as "nice" or "naughty"
5. **Wish Replacement**: Replaces old wish atomically (delete + create)

## Development

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=WishTest

# Run with debug output
mvn -X test
```

### Building JAR

```bash
mvn package
```

The JAR will be created in `target/` directory.

### Database Configuration

Database connection can be configured via environment variables:

| Environment Variable | Default Value | Description |
|---------------------|---------------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `webapp_db` | Database name |
| `DB_USER` | `geert` | Database user |
| `DB_PASSWORD` | `gman` | Database password |

**For Docker/Finch:** Environment variables are automatically configured in `docker-compose.yml`

**For Local Development:** Set environment variables before running:
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=webapp_db
export DB_USER=geert
export DB_PASSWORD=gman

mvn exec:java
```

Database connection implementation:
- `WishStorePostgres.java` (lines 10-16)
- `PeopleStorePostgres.java` (lines 12-18)

## Docker Deployment

The application includes Docker support with a multi-stage build process:

### Architecture

```
┌─────────────────────────────────────┐
│  wish-keeper-app (Java Container)  │
│  Port: 8000                         │
│  Image: Built from Dockerfile      │
└─────────────────┬───────────────────┘
                  │
                  │ Network: wish-keeper-network
                  │
┌─────────────────▼───────────────────┐
│  wish-keeper-db (PostgreSQL)       │
│  Port: 5432                         │
│  Image: postgres:15-alpine         │
│  Volume: postgres_data             │
└─────────────────────────────────────┘
```

### Files

- **`Dockerfile`**: Multi-stage build
  - Stage 1: Maven build (maven:3.9-eclipse-temurin-17)
  - Stage 2: Runtime (eclipse-temurin:17-jre-jammy)
- **`docker-compose.yml`**: Orchestrates both containers with networking and volumes

### Container Features

- **Automatic Database Initialization**: SQL schemas mounted to `/docker-entrypoint-initdb.d/`
- **Health Checks**: PostgreSQL health check ensures database is ready before app starts
- **Persistent Data**: PostgreSQL data stored in named volume `postgres_data`
- **Network Isolation**: Containers communicate on dedicated bridge network
- **Environment Variables**: All database credentials configurable via environment

### Docker Commands

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f

# Stop all services
docker compose down

# Remove all data (including database volume)
docker compose down -v

# Rebuild and start
docker compose up --build

# Run tests (requires local Maven)
mvn test
```

### AWS Finch Commands

AWS Finch is a Docker-compatible container runtime. Commands are identical:

```bash
# Start all services
finch compose up -d

# View logs
finch compose logs -f

# Stop all services
finch compose down

# Rebuild and start
finch compose up --build
```

### Customizing Configuration

To use different database credentials, edit `docker-compose.yml`:

```yaml
services:
  postgres:
    environment:
      POSTGRES_DB: your_database
      POSTGRES_USER: your_user
      POSTGRES_PASSWORD: your_password

  app:
    environment:
      DB_NAME: your_database
      DB_USER: your_user
      DB_PASSWORD: your_password
```

## API Examples

Example HTTP requests are available in:
- `src/main/resources/Wishes.http`
- `src/main/resources/People.http`

These can be used with REST clients that support `.http` files (e.g., IntelliJ IDEA, VS Code with REST Client extension).

## Testing

The project includes unit tests for:
- Wish validation and JSON parsing
- Geographic distance calculations (Haversine formula)
- Proof-of-work hash mining
- Database CRUD operations
- SQL injection protection

Run tests with:
```bash
make test
```

## Known Limitations

- Single-threaded HTTP server (no concurrent request handling)
- No authentication or authorization
- HTTP only (no HTTPS/TLS)
- No rate limiting
- Limited error handling
- Proof-of-work fulfillment can be CPU-intensive

## Contributing

When contributing to this project:
1. Follow existing code structure and naming conventions
2. Add tests for new functionality
3. Update this README if adding new features or endpoints
4. Ensure all tests pass before submitting changes

## License

This project is for educational/demonstration purposes.

## Additional Documentation

- [CLAUDE.md](CLAUDE.md) - AI assistant guidance for working with this codebase
