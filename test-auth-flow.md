# Authentication Flow Testing Guide

## Test Setup

1. Start the backend server:
```bash
lein run
```

2. Start the frontend dev server:
```bash
npx shadow-cljs watch app
```

## Test Scenarios

### 1. Test Unauthenticated Access (Should Fail)
```bash
# Test protected endpoint without token - should return 401
curl -X POST http://localhost:3000/api/v1/create/author \
  -H "Content-Type: application/json" \
  -d '{"common_name": "Test Author"}'
```

Expected: 401 Unauthorized with message about missing JWT token

### 2. Test Login Flow

1. Navigate to http://localhost:3000
2. Click "Login" button
3. Complete Auth0 authentication
4. Check browser console for:
   - "User logged in: [user object]"
   - "Got tokens: [token]"
   - "Storing tokens in db: [token]"

### 3. Test Authenticated API Calls

After login, open browser console and run:
```javascript
// Dispatch a test auth request
re_frame.core.dispatch(["auth/test-with-auth"])
```

Expected: Successful response logged to console

### 4. Test Protected Operations

After login, try creating records through the UI:
- Create a new author
- Create a new title  
- Update an existing chapter

All should succeed with JWT token automatically included.

### 5. Test Token Persistence

1. Login successfully
2. Refresh the page
3. Check console for "User already authenticated, getting user info and tokens"
4. Verify user stays logged in

### 6. Test Logout

1. Click "Logout" button
2. Try accessing protected endpoint
3. Should fail with 401

## Debugging

### Check Token in Browser
Open browser console and run:
```javascript
// Get current token
re_frame.core.subscribe(["auth/tokens"])
```

### Check Auth Headers
Monitor Network tab in browser DevTools:
- Look for `Authorization: Bearer [token]` header on API requests
- Verify token is present on POST/PUT requests to protected endpoints

### Common Issues

1. **Token not included**: Check if `:auth/get-auth0-tokens` was dispatched after login
2. **401 on all requests**: Verify JWT validation middleware is active on backend
3. **Token expired**: Implement token refresh (not yet added)

## Backend Verification

Check backend logs for:
- JWT validation attempts
- User extraction from tokens
- Authorization success/failure messages

## Security Checklist

✅ JWT tokens validated on backend  
✅ Cookie-based auth deprecated
✅ All mutation endpoints protected
✅ Token included in Authorization header
✅ Token stored in Auth0 localStorage
✅ User info extracted from validated tokens
❌ Token refresh not yet implemented
❌ Rate limiting not yet implemented