# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java-based REST API application called "Wish Keeper" that manages wishes and people through a PostgreSQL database. The application uses a simple HTTP server implementation with Jackson for JSON processing.

## Build System

This project uses **Maven** as its build system with Java 17 as the target version.

### Key Commands

```bash
# Build/Compile the project
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

# Clean build artifacts
mvn clean

# Package as JAR
mvn package

# Run a single test class
mvn test -Dtest=ClassName

# Run with debug output
mvn -X exec:java
```

## Architecture

### Core Components

The application follows a simple layered architecture:

1. **API Layer** (`BasicApi.java`): Main entry point, creates HTTP server on port 8000
   - `/api/hello` - Simple health check endpoint
   - `/api/wish` - Wish management (GET all, POST new)
   - `/api/wishreplace` - Wish replacement operations
   - `/api/people` - People management
   - `/api/wishfulfill` - Wish fulfillment operations

2. **Handler Classes**: Each endpoint has a dedicated handler
   - `WishHandler` - Handles wish CRUD operations
   - `PeopleHandler` - Manages person entities
   - `WishReplacementHandler` - Handles wish replacements
   - `WishFulfillmentHandler` - Manages wish fulfillments

3. **Data Access Layer**: PostgreSQL stores with direct JDBC connections
   - `WishStorePostgres` - Persistence for wishes
   - `PeopleStorePostgres` - Persistence for people
   - Database: `webapp_db` on localhost:5432

4. **Domain Models**:
   - `Wish` - Core wish entity
   - `Person` - Person entity
   - `Location` - Location information
   - `WishFulfillment` - Tracks wish fulfillments
   - Various DTOs for data transfer

### Database Configuration

The application connects to PostgreSQL using **password-less authentication by default**.

**Environment Variables (all optional with defaults):**
  - `DB_HOST` - Database host (default: `localhost`)
  - `DB_PORT` - Database port (default: `5432`)
  - `DB_NAME` - Database name (default: `webapp_db`)
  - `DB_USER` - Database username (default: `wishkeeper`)
  - `DB_PASSWORD` - Database password (default: `""` - empty for password-less auth)

**Authentication Methods:**
- **Docker/Finch (Default):** Trust authentication with network isolation
  - No password required
  - Secured by container network isolation
  - Just run `docker compose up` or `finch compose up`

- **Local Development:** Trust or peer authentication
  - No password required by default
  - Configure PostgreSQL for trust authentication if needed

- **Production (Optional):** Password authentication
  - Set `DB_PASSWORD` environment variable
  - Application supports both password and password-less modes

**Security:** Password-less authentication is secure because:
- Docker containers are isolated on a private network
- Database is only accessible from the application container
- No credentials to leak or manage
- Simpler than password-based auth for containerized environments

### Business Rules

- Each beneficiary can have a maximum of 3 wishes
- Wishes must have valid beneficiary IDs (person must exist)
- Product names cannot be empty
- Quantities cannot be negative

## Dependencies

- Jackson Databind 2.20.0 (JSON processing)
- PostgreSQL Driver 42.7.7
- JUnit 3.8.2 (testing)