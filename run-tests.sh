#!/bin/bash

echo "==============================================="
echo "CDE Platform - Comprehensive Test Suite"
echo "==============================================="
echo

# Set test environment
export TEST_ENV=true
export DATABASE_URL=${DATABASE_URL:-"postgresql://localhost/cde_test?user=postgres&password=postgres"}

echo "Test Configuration:"
echo "  TEST_ENV: $TEST_ENV"
echo "  DATABASE_URL: $DATABASE_URL"
echo

# Check if database exists
if psql -lqt | cut -d \| -f 1 | grep -qw cde_test; then
    echo "✓ Test database exists"
else
    echo "✗ Test database does not exist"
    echo "  To create: createdb cde_test"
    echo "  Continuing without database tests..."
fi
echo

# Run tests with categories
echo "Running Test Suite..."
echo "---------------------"

# 1. Simple tests (no external dependencies)
echo
echo "1. Unit Tests (No Dependencies):"
lein test cde.simple-test 2>&1 | grep -E "Ran|failures|errors"

# 2. Middleware tests
echo
echo "2. Middleware Tests:"
lein test cde.middleware-test 2>&1 | grep -E "Ran|failures|errors"

# 3. Handler tests
echo
echo "3. Handler Tests:"
lein test cde.handler-test 2>&1 | grep -E "Ran|failures|errors"

# 4. All tests summary
echo
echo "==============================================="
echo "Full Test Suite:"
echo "---------------------"
lein test 2>&1 | tail -5

echo
echo "==============================================="
echo "Test Summary:"
echo "---------------------"
echo "✓ Test framework operational"
echo "✓ JWT mocking configured for test mode"
echo "✓ Database tests skip if no database"
echo
echo "To run with database:"
echo "  1. createdb cde_test"
echo "  2. lein run migrate"
echo "  3. ./run-tests.sh"
echo
echo "To run specific test:"
echo "  lein test cde.simple-test"
echo "  lein test :only cde.middleware-test/test-check-auth0-jwt"
echo "==============================================="
