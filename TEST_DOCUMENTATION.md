# Test Documentation

## Overview

This document describes the comprehensive test suite for the CDE platform's authentication and authorization system. The tests cover backend JWT validation, frontend authentication flows, and end-to-end integration scenarios.

## Test Structure

```
test/
├── clj/
│   └── cde/
│       ├── middleware_test.clj      # JWT & middleware tests
│       ├── routes/
│       │   └── services_test.clj    # API endpoint tests
│       ├── integration_test.clj     # End-to-end tests
│       ├── test_helpers.clj         # Test utilities
│       └── handler_test.clj         # Existing handler tests
└── cljs/
    └── cde/
        └── auth_test.cljs            # Frontend auth tests
```

## Running Tests

### Backend Tests

#### Run All Backend Tests
```bash
lein test
```

#### Run Specific Test Namespace
```bash
lein test cde.middleware-test
lein test cde.routes.services-test
lein test cde.integration-test
```

#### Run Specific Test
```bash
lein test :only cde.middleware-test/test-check-auth0-jwt
```

#### Run Tests with Coverage
```bash
lein cloverage
```

### Frontend Tests

#### Setup Frontend Test Environment
```bash
npm install --save-dev karma karma-chrome-launcher karma-cljs-test
```

#### Run Frontend Tests
```bash
npx shadow-cljs compile test
npx karma start --single-run
```

#### Watch Mode for Frontend Tests
```bash
npx shadow-cljs watch test
```

### Integration Tests

#### Setup Test Database
```bash
createdb cde_test
lein run migrate
```

#### Run Integration Tests
```bash
TEST_ENV=true lein test cde.integration-test
```

## Test Categories

### 1. Middleware Tests (`middleware_test.clj`)

Tests for JWT validation and authentication middleware:

- **JWT Validation**
  - ✅ Valid JWT tokens are accepted
  - ✅ Missing tokens return 401
  - ✅ Expired tokens are rejected
  - ✅ Malformed tokens are rejected
  - ✅ User info extracted from valid tokens

- **Security Middleware**
  - ✅ HTTPS redirect for HTTP requests
  - ✅ Internal error handling
  - ✅ CSRF protection
  - ✅ Cookie-based auth marked as deprecated

### 2. API Endpoint Tests (`services_test.clj`)

Tests for protected and public API endpoints:

- **Public Endpoints**
  - ✅ Platform statistics accessible without auth
  - ✅ Newspaper/Author/Title listings public
  - ✅ Search functionality public
  - ✅ Individual record retrieval public

- **Protected Endpoints**
  - ✅ Create operations require JWT
  - ✅ Update operations require JWT
  - ✅ Delete operations require JWT
  - ✅ Admin operations require JWT

- **Data Validation**
  - ✅ Required fields enforced
  - ✅ Type validation
  - ✅ Input sanitization
  - ✅ SQL injection prevention

### 3. Frontend Auth Tests (`auth_test.cljs`)

Tests for frontend authentication flow:

- **Auth State Management**
  - ✅ Auth0 client initialization
  - ✅ User state storage
  - ✅ Token management
  - ✅ Login/logout flow
  - ✅ LocalStorage persistence

- **Auth Events**
  - ✅ Login popup handling
  - ✅ Token fetching
  - ✅ User profile updates
  - ✅ Error handling

- **Auth Subscriptions**
  - ✅ User email retrieval
  - ✅ Login status checks
  - ✅ Token access
  - ✅ Email verification status

### 4. Integration Tests (`integration_test.clj`)

End-to-end testing scenarios:

- **Complete Auth Flow**
  - ✅ User creation
  - ✅ Authentication
  - ✅ Authorization
  - ✅ Token refresh

- **CRUD Operations**
  - ✅ Create with auth
  - ✅ Read without auth
  - ✅ Update with auth
  - ✅ Delete with auth

- **Security Tests**
  - ✅ SQL injection protection
  - ✅ XSS prevention
  - ✅ Token expiration
  - ✅ Concurrent requests
  - ✅ Rate limiting (future)

## Test Helpers

### Mock JWT Creation
```clojure
(create-signed-jwt {:sub "auth0|123"
                    :email "test@example.com"
                    :exp (+ (System/currentTimeMillis) 3600000)})
```

### Authenticated Request
```clojure
(authenticated-request :post "/api/v1/create/author"
                       {:common_name "Test Author"})
```

### Mock Auth0 Client
```clojure
(mock-auth0-client) ; Returns mock client for frontend tests
```

## Test Data Fixtures

### Test User
```clojure
{:id 1
 :email "test@example.com"
 :auth0-id "auth0|test-user-123"}
```

### Test Author
```clojure
{:id 1
 :common_name "Test Author"
 :gender "Unknown"
 :nationality "Unknown"
 :added_by 1}
```

## Continuous Integration

### GitHub Actions Configuration
```yaml
name: Tests
on: [push, pull_request]
jobs:
  backend-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:13
        env:
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '11'
      - run: lein test
      
  frontend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-node@v2
        with:
          node-version: '16'
      - run: npm ci
      - run: npx shadow-cljs compile test
      - run: npx karma start --single-run
```

## Test Coverage Goals

- **Backend**: Minimum 80% code coverage
- **Frontend**: Minimum 70% code coverage
- **Integration**: All critical user paths tested

## Troubleshooting

### Common Issues

1. **401 Errors in Tests**
   - Ensure mock JWT middleware is active
   - Check test token expiration
   - Verify Auth0 configuration

2. **Database Connection Errors**
   - Create test database: `createdb cde_test`
   - Run migrations: `lein run migrate`
   - Check database credentials in test-config.edn

3. **Frontend Test Failures**
   - Clear shadow-cljs cache: `npx shadow-cljs release test`
   - Check for missing npm dependencies
   - Verify karma configuration

4. **Flaky Integration Tests**
   - Increase test timeout in test-config.edn
   - Check for race conditions
   - Ensure proper database cleanup

## Security Test Checklist

- [ ] All protected endpoints return 401 without auth
- [ ] Valid JWT tokens grant access
- [ ] Expired tokens are rejected
- [ ] Malformed tokens are rejected
- [ ] SQL injection attempts blocked
- [ ] XSS payloads sanitized
- [ ] CSRF tokens validated
- [ ] Rate limiting enforced (future)
- [ ] User permissions checked
- [ ] Audit logs created

## Performance Benchmarks

Target response times under load:
- Authentication: < 200ms
- Token validation: < 50ms
- Protected endpoint access: < 500ms
- Concurrent requests: 100 req/s

## Future Improvements

1. **Add Property-Based Testing**
   - Use test.check for generative testing
   - Fuzz testing for input validation

2. **Implement Load Testing**
   - Use Gatling or JMeter
   - Test concurrent user scenarios

3. **Add Mutation Testing**
   - Verify test effectiveness
   - Identify untested code paths

4. **Browser Automation Tests**
   - Selenium/Playwright tests
   - Full user journey testing

5. **Security Scanning**
   - OWASP ZAP integration
   - Dependency vulnerability scanning

## Reporting Issues

When reporting test failures:
1. Include full error message
2. Specify test name and file
3. Provide reproduction steps
4. Include relevant logs
5. Note environment details