#!/bin/bash

# Setup script for test database
# Usage: ./scripts/setup-test-db.sh

set -e

DB_NAME="cde_test"
DB_USER="${DB_USER:-g}"  # Use current user by default

echo "Setting up test database with user: $DB_USER..."

# Check if database exists
if psql -U $DB_USER -lqt | cut -d \| -f 1 | grep -qw $DB_NAME; then
    echo "Database $DB_NAME already exists. Dropping..."
    dropdb -U $DB_USER $DB_NAME
fi

# Create database
echo "Creating database $DB_NAME..."
createdb -U $DB_USER $DB_NAME

# Run migrations
echo "Running migrations..."
DATABASE_URL="postgresql://localhost/$DB_NAME?user=$DB_USER" lein run migrate

echo "Test database setup complete!"
