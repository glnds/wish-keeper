#!/bin/bash

# Migration script to deploy schema to AWS RDS PostgreSQL
# This script extracts the database schema from the Docker setup and applies it to RDS

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Check required environment variables
check_prerequisites() {
    print_info "Checking prerequisites..."
    
    # Check for AWS CLI
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI is not installed. Please install it first."
        exit 1
    fi
    
    # Check for psql
    if ! command -v psql &> /dev/null; then
        print_error "psql is not installed. Please install PostgreSQL client."
        exit 1
    fi
    
    # Check for jq (optional but helpful)
    if ! command -v jq &> /dev/null; then
        print_warn "jq is not installed. Some features may be limited."
    fi
    
    print_info "Prerequisites check passed!"
}

# Function to get RDS endpoint from CloudFormation stack
get_rds_endpoint() {
    local stack_name=$1
    print_info "Retrieving RDS endpoint from stack: $stack_name"
    
    aws cloudformation describe-stacks \
        --stack-name "$stack_name" \
        --query 'Stacks[0].Outputs[?OutputKey==`DBEndpoint`].OutputValue' \
        --output text
}

# Function to get secret value from Secrets Manager
get_secret() {
    local secret_name=$1
    print_info "Retrieving database credentials from Secrets Manager..."
    
    aws secretsmanager get-secret-value \
        --secret-id "$secret_name" \
        --query 'SecretString' \
        --output text
}

# Function to apply schema to RDS
apply_schema() {
    local db_host=$1
    local db_user=$2
    local db_name=$3
    local db_password=$4
    
    print_info "Applying database schema to RDS..."
    
    # Set password for psql
    export PGPASSWORD="$db_password"
    
    # Check connection
    print_info "Testing database connection..."
    if ! psql -h "$db_host" -U "$db_user" -d postgres -c '\q' 2>/dev/null; then
        print_error "Cannot connect to database at $db_host"
        exit 1
    fi
    print_info "Connection successful!"
    
    # Apply People table schema
    print_info "Creating people table..."
    psql -h "$db_host" -U "$db_user" -d "$db_name" -f "../../src/main/resources/People.pgsql"
    
    # Apply Wishes table schema
    print_info "Creating wishes table..."
    psql -h "$db_host" -U "$db_user" -d "$db_name" -f "../../src/main/resources/wishes.pgsql"
    
    # Verify tables were created
    print_info "Verifying tables..."
    table_count=$(psql -h "$db_host" -U "$db_user" -d "$db_name" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';")
    
    if [ "$table_count" -ge 2 ]; then
        print_info "Schema migration completed successfully! Created $table_count tables."
    else
        print_error "Schema migration may have failed. Only $table_count tables found."
        exit 1
    fi
    
    # Clean up password
    unset PGPASSWORD
}

# Function to backup existing data (if migrating from local)
backup_local_data() {
    print_info "Backing up local PostgreSQL data..."
    
    local backup_file="wish-keeper-backup-$(date +%Y%m%d-%H%M%S).sql"
    
    # Check if local database is running
    if docker ps | grep -q wish-keeper-db; then
        docker exec wish-keeper-db pg_dump -U wishkeeper webapp_db > "$backup_file"
        print_info "Backup saved to: $backup_file"
        echo "$backup_file"
    else
        print_warn "Local database container not running. Skipping backup."
        echo ""
    fi
}

# Function to restore data to RDS
restore_data() {
    local backup_file=$1
    local db_host=$2
    local db_user=$3
    local db_name=$4
    local db_password=$5
    
    if [ -z "$backup_file" ] || [ ! -f "$backup_file" ]; then
        print_warn "No backup file found. Skipping data restore."
        return
    fi
    
    print_info "Restoring data to RDS from: $backup_file"
    export PGPASSWORD="$db_password"
    
    psql -h "$db_host" -U "$db_user" -d "$db_name" -f "$backup_file"
    
    print_info "Data restore completed!"
    unset PGPASSWORD
}

# Main execution
main() {
    print_info "=== Wish Keeper Database Migration to AWS RDS ==="
    echo ""
    
    # Check prerequisites
    check_prerequisites
    echo ""
    
    # Get parameters
    if [ $# -lt 1 ]; then
        print_error "Usage: $0 <rds-stack-name> [--with-data]"
        print_error "Example: $0 wish-keeper-rds"
        print_error "         $0 wish-keeper-rds --with-data"
        exit 1
    fi
    
    RDS_STACK_NAME=$1
    WITH_DATA=${2:-}
    
    # Get RDS endpoint
    DB_HOST=$(get_rds_endpoint "$RDS_STACK_NAME")
    if [ -z "$DB_HOST" ]; then
        print_error "Could not retrieve RDS endpoint from stack: $RDS_STACK_NAME"
        exit 1
    fi
    print_info "RDS Endpoint: $DB_HOST"
    echo ""
    
    # Get credentials from Secrets Manager
    SECRET_NAME="${RDS_STACK_NAME}-db-credentials"
    SECRET_JSON=$(get_secret "$SECRET_NAME")
    
    DB_USER=$(echo "$SECRET_JSON" | jq -r '.username')
    DB_PASSWORD=$(echo "$SECRET_JSON" | jq -r '.password')
    DB_NAME=$(echo "$SECRET_JSON" | jq -r '.dbname')
    
    print_info "Database: $DB_NAME"
    print_info "User: $DB_USER"
    echo ""
    
    # Backup local data if requested
    BACKUP_FILE=""
    if [ "$WITH_DATA" = "--with-data" ]; then
        BACKUP_FILE=$(backup_local_data)
        echo ""
    fi
    
    # Apply schema
    apply_schema "$DB_HOST" "$DB_USER" "$DB_NAME" "$DB_PASSWORD"
    echo ""
    
    # Restore data if backup exists
    if [ -n "$BACKUP_FILE" ]; then
        restore_data "$BACKUP_FILE" "$DB_HOST" "$DB_USER" "$DB_NAME" "$DB_PASSWORD"
        echo ""
    fi
    
    print_info "=== Migration Complete ==="
    print_info "Database is ready at: $DB_HOST:5432/$DB_NAME"
    print_info ""
    print_info "Update your application environment variables:"
    echo "  DB_HOST=$DB_HOST"
    echo "  DB_PORT=5432"
    echo "  DB_NAME=$DB_NAME"
    echo "  DB_USER=$DB_USER"
    echo "  DB_PASSWORD=<from-secrets-manager>"
}

# Run main function
main "$@"
