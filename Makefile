.PHONY: build test test-unit test-with-db db-start db-stop db-status run clean help

# Default target - show help
.DEFAULT_GOAL := help

# Show available make targets
help:
	@echo "Available targets:"
	@echo "  make build              - Compile the application"
	@echo "  make run                - Run the application locally (without Docker)"
	@echo "  make clean              - Clean build artifacts"
	@echo ""
	@echo "Testing:"
	@echo "  make test               - Run all tests (requires database)"
	@echo "  make test-unit          - Run unit tests only (no database required)"
	@echo "  make test-with-db       - Start DB, run all tests, keep DB running"
	@echo "  make test-with-db-clean - Start DB, run all tests, stop DB afterwards"
	@echo ""
	@echo "Database:"
	@echo "  make db-start           - Start PostgreSQL database"
	@echo "  make db-stop            - Stop the database"
	@echo "  make db-status          - Check database status"

# Build the application
build:
	mvn compile

# Run the application locally (without Docker)
run:
	mvn exec:java

# Clean build artifacts
clean:
	mvn clean

# Run all tests (will fail if database is not running)
test:
	mvn test

# Run only unit tests (no database required)
test-unit:
	mvn test -Dtest=WishTest,PeopleHandlerUtilsTest,LocationTest,HashCollisionTest

# Start PostgreSQL database only (not the app)
db-start:
	finch compose up -d postgres
	@echo "Waiting for database to be ready..."
	@sleep 3
	@echo "Database is ready!"

# Stop the database
db-stop:
	finch compose down

# Check database status
db-status:
	finch compose ps

# Run tests with database (starts DB, runs tests, keeps DB running)
test-with-db: db-start
	@echo "Running tests with database..."
	mvn test
	@echo "Tests complete. Database is still running. Use 'make db-stop' to stop it."

# Run all tests and stop database afterwards
test-with-db-clean: db-start
	@echo "Running tests with database..."
	mvn test || (finch compose down && exit 1)
	finch compose down
	@echo "Tests complete. Database stopped."
