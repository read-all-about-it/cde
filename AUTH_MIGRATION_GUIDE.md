# Authentication Security Migration Guide

## Critical Security Issues Fixed

### 1. JWT Token Validation Now Enforced
**Previous Issue**: JWT middleware (`wrap-auth0`) was defined but never applied to routes
**Fix Applied**: JWT middleware is now active in the API middleware stack

### 2. Cookie-Based Auth Replaced
**Previous Issue**: Routes only checked for client-side Auth0 cookie (`auth0.{client-id}.is.authenticated=true`) which could be easily forged
**Fix Applied**: New `check-auth0-jwt` middleware properly validates JWT signatures and claims

### 3. All Protected Routes Updated
**Fixed Endpoints**:
- PUT `/api/v1/author/:id` - Update author
- PUT `/api/v1/title/:id` - Update title  
- PUT `/api/v1/chapter/:id` - Update chapter
- POST `/api/v1/create/newspaper` - Create newspaper
- POST `/api/v1/create/author` - Create author
- POST `/api/v1/create/title` - Create title
- POST `/api/v1/create/chapter` - Create chapter (was previously unprotected!)

## Frontend Changes Required

### Authorization Header
All API requests to protected endpoints must now include the JWT token:

```javascript
// Example for frontend requests
fetch('/api/v1/create/author', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${auth0Token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify(authorData)
})
```

### ClojureScript/re-frame Update
Update your AJAX requests to include the token:

```clojure
(defn authenticated-request [method uri params]
  (let [token (get-auth0-token)] ; Get token from Auth0 SDK
    (ajax/request
      {:method method
       :uri uri
       :headers {"Authorization" (str "Bearer " token)}
       :params params
       ; ... other options
       })))
```

## Testing the Changes

### 1. Test Unauthorized Access
```bash
# Should return 401 Unauthorized
curl -X POST http://localhost:3000/api/v1/create/author \
  -H "Content-Type: application/json" \
  -d '{"common_name": "Test Author"}'
```

### 2. Test Authorized Access
```bash
# Should succeed with valid token
curl -X POST http://localhost:3000/api/v1/create/author \
  -H "Authorization: Bearer YOUR_VALID_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"common_name": "Test Author"}'
```

## Deprecated Functions

- `check-auth0-cookie` - Now marked as DEPRECATED, use `check-auth0-jwt` instead
- Cookie-based authentication should be completely removed in future releases

## User Information Extraction

Use the new helper function to get user details from validated tokens:

```clojure
(defn my-handler [request]
  (let [user (mw/extract-user-from-jwt request)]
    ; user contains {:user-id "auth0|xxx" :email "user@example.com" :name "User Name"}
    ; ... your logic here
    ))
```

## Security Best Practices

1. **Never trust client-side cookies** for authentication
2. **Always validate JWT signatures** using the Auth0 JWKS endpoint
3. **Include user context** from JWT claims when creating/updating records
4. **Implement token refresh** on the frontend to handle expired tokens
5. **Add rate limiting** to prevent abuse of authenticated endpoints

## Rollback Plan

If issues arise, you can temporarily revert by:
1. Commenting out `mw/wrap-auth0` in the middleware stack
2. Replacing `mw/check-auth0-jwt` with `mw/check-auth0-cookie` in routes
3. **WARNING**: This will restore the security vulnerability!

## Next Steps

1. Update frontend to send JWT tokens in Authorization headers
2. Remove all cookie-based auth code once JWT auth is working
3. Add proper user association using JWT claims for created/updated records
4. Implement refresh token handling
5. Add comprehensive authentication tests