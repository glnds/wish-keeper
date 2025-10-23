# Wish Keeper

A Java-based REST API application for managing wishes and people through a PostgreSQL database. The application uses a custom HTTP server implementation with Jackson for JSON processing.

## Features

- **Wish Management**: Create, retrieve, and replace wishes for beneficiaries
- **People Management**: Register and update people with optimistic locking
- **Wish Fulfillment**: Proof-of-work based wish fulfillment system using SHA-256 mining
- **Geographic Distance**: Calculate distance to North Pole using Haversine formula
- **Business Rules**: Enforce constraints (max 3 wishes per beneficiary, validation, etc.)

## Prerequisites

- **Java**: JDK 8 or higher (configured for Java 16)
- **Maven**: 3.6+ for building and running
- **PostgreSQL**: 9.5+ with database `webapp_db`
- **Database Access**: User `geert` with appropriate permissions

## Quick Start

### 1. Database Setup

Create the database and tables:

```bash
# Create database
createdb webapp_db

# Run schema files
psql -d webapp_db -f src/main/resources/People.pgsql
psql -d webapp_db -f src/main/resources/wishes.pgsql
```

### 2. Build and Run

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

Database connection is configured in:
- `WishStorePostgres.java` (lines 13-15)
- `PeopleStorePostgres.java` (lines 12-14)

Default configuration:
```
URL: jdbc:postgresql://localhost:5432/webapp_db
User: geert
Password: gman
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
- Database credentials hardcoded in source code
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
