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

The application connects to PostgreSQL using environment variables for security:
- **Required Environment Variables:**
  - `DB_USER` - Database username (no default, must be set)
  - `DB_PASSWORD` - Database password (no default, must be set)
- **Optional Environment Variables (with defaults):**
  - `DB_HOST` - Database host (default: `localhost`)
  - `DB_PORT` - Database port (default: `5432`)
  - `DB_NAME` - Database name (default: `webapp_db`)

**For Docker/Finch:** Create a `.env` file from `.env.example` and set credentials
**For Local Development:** Export environment variables before running `mvn exec:java`

The application will fail to start if `DB_USER` or `DB_PASSWORD` are not provided.

### Business Rules

- Each beneficiary can have a maximum of 3 wishes
- Wishes must have valid beneficiary IDs (person must exist)
- Product names cannot be empty
- Quantities cannot be negative

## Dependencies

- Jackson Databind 2.20.0 (JSON processing)
- PostgreSQL Driver 42.7.7
- JUnit 3.8.2 (testing)