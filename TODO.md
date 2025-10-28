# TODO

Tasks are organised by priority (highest first).

---

## High Priority (Code Quality / Architecture)

- [ ] Extract `with-defaults` helper to shared utility
  - Notes:
    - Identical function duplicated across 4 route files: `routes/author.clj`, `routes/newspaper.clj`, `routes/title.clj`, `routes/chapter.clj`
    - Move to `cde.routes.specs` or create `cde.routes.utils` namespace
    - Each file has: `(defn- ^:no-doc with-defaults [limit offset] [(if (nil? limit) 50 limit) (if (nil? offset) 0 offset)])`
  - Complexity: Simple

- [ ] Split large `events.cljs` file (1500+ lines)
  - Notes:
    - Current file at `src/cljs/cde/events.cljs` is unwieldy
    - Split by domain: `events/author.cljs`, `events/title.cljs`, `events/chapter.cljs`, `events/newspaper.cljs`, `events/auth.cljs`, `events/common.cljs`
    - Maintain backwards compatibility by re-exporting from main `events.cljs`
  - Complexity: Medium

- [ ] Implement validation in `validation.cljc`
  - Notes:
    - File at `src/cljc/cde/validation.cljc` is essentially empty (only requires `struct.core`)
    - Should contain shared validation specs for entities (author, title, chapter, newspaper)
    - Use struct library already imported, or migrate to clojure.spec
    - Can be shared between frontend and backend (cljc)
  - Complexity: Medium

- [ ] Move Auth0 config from hardcoded values to environment config
  - Notes:
    - `src/cljs/cde/config.cljs` has hardcoded Auth0 client-id, domain, redirect-uri
    - These should be injected at build time via closure-defines or read from a config endpoint
    - Dev config exists in `dev-config.edn` but frontend doesn't use it
    - Consider: API endpoint that returns public config, or shadow-cljs closure-defines
  - Complexity: Medium

---

## Medium Priority (Test Coverage)

- [ ] Add tests for `db/user.clj`
  - Notes:
    - No tests exist for user operations
    - Test `get-or-create-user!` function
    - Use transaction rollback pattern from existing db tests
  - Complexity: Simple

- [ ] Add tests for `db/platform.clj`
  - Notes:
    - Platform statistics functions untested
    - Test `get-stats` function returns expected structure
  - Complexity: Simple

- [ ] Add tests for `db/title.clj`
  - Notes:
    - Only basic tests exist; needs more comprehensive coverage
    - Test date parsing logic, validation, relationships
  - Complexity: Medium

- [ ] Add tests for `db/chapter.clj`
  - Notes:
    - Needs tests for Trove sync functionality
    - Test chapter text/html handling
  - Complexity: Medium

- [ ] Add tests for `db/newspaper.clj`
  - Notes:
    - Basic CRUD operations need test coverage
  - Complexity: Simple

- [ ] Add tests for `routes/author.clj`
  - Notes:
    - Route handler tests with ring-mock
    - Test auth middleware integration
  - Complexity: Medium

- [ ] Add tests for `routes/title.clj`
  - Notes:
    - Route handler tests with ring-mock
    - Test query parameter handling
  - Complexity: Medium

- [ ] Add tests for `routes/chapter.clj`
  - Notes:
    - Route handler tests with ring-mock
    - Test Trove integration endpoints
  - Complexity: Medium

- [ ] Add tests for `routes/newspaper.clj`
  - Notes:
    - Route handler tests with ring-mock
  - Complexity: Medium

- [ ] Add tests for `routes/search.clj`
  - Notes:
    - Test full-text search endpoint
    - Test fuzzy matching functionality
  - Complexity: Medium

- [ ] Add tests for `routes/platform.clj`
  - Notes:
    - Test statistics endpoint
  - Complexity: Simple

- [ ] Add tests for `routes/trove.clj`
  - Notes:
    - Test Trove proxy endpoints
    - Mock external Trove API calls
  - Complexity: Medium

- [ ] Add tests for `jwt_auth.clj`
  - Notes:
    - Test token verification logic
    - Test JWKS caching behaviour
    - Mock HTTP calls to Auth0
  - Complexity: Hard

- [ ] Add tests for `trove.clj`
  - Notes:
    - Test Trove API integration
    - Mock external HTTP calls
    - Test error handling for API failures
  - Complexity: Medium

- [ ] Add tests for `utils.clj`
  - Notes:
    - Test HTML processing functions
    - Test parameter handling utilities
  - Complexity: Simple

- [ ] Add frontend event handler tests
  - Notes:
    - Extract handler functions from `reg-event-db`/`reg-event-fx` calls
    - Test pure handler functions directly
    - Priority: auth events, entity CRUD events
  - Complexity: Medium

- [ ] Add frontend subscription tests
  - Notes:
    - Test derived/computed subscriptions (layer 3)
    - Use `re-frame-test` library
  - Complexity: Medium

---

## Low Priority (Documentation / Polish)

- [ ] Add missing docstrings to route handlers
  - Notes:
    - Some route functions lack comprehensive docstrings
    - Priority files: `routes/chapter.clj`, `routes/title.clj`
  - Complexity: Simple

- [ ] Add namespace docstrings where missing
  - Notes:
    - Some namespaces lack purpose documentation
    - Check: `db/*.clj`, `routes/*.clj`
  - Complexity: Simple

- [ ] Document API endpoints in Swagger/OpenAPI
  - Notes:
    - Swagger is configured in `routes/services.clj`
    - Ensure all endpoints have proper specs and descriptions
    - Review reitit-swagger documentation
  - Complexity: Medium

- [ ] Add error.html styling
  - Notes:
    - `resources/html/error.html` is unstyled
    - Add Bulma CSS and consistent styling with main app
  - Complexity: Simple

---

## Future Enhancements (Backlog)

- [ ] Implement Collections feature
  - Notes:
    - Database tables exist: `collections`, `collection_items`
    - Needs: API endpoints, frontend UI, event handlers
  - Complexity: Hard

- [ ] Implement Posts feature
  - Notes:
    - Database tables exist: `posts`, `versions`, `dependencies`
    - Needs: API endpoints, frontend UI, rich text editing
    - Depends on Collections feature
  - Complexity: Hard

- [ ] Implement Comments feature
  - Notes:
    - Database table exists: `comments`
    - Needs: API endpoints, frontend UI
    - Depends on Posts feature
  - Complexity: Medium

- [ ] Implement Updations/notifications feature
  - Notes:
    - Database table exists: `updations`
    - Track changes to titles, notify interested users
  - Complexity: Medium

- [ ] Add E2E tests for critical user journeys
  - Notes:
    - Use Etaoin (Clojure) or Cypress (JS)
    - Priority: login flow, create title, search functionality
  - Complexity: Hard

- [ ] Switch from Heroku CLI (and Pipeline) to Github Actions CI/CD
  - Notes:
    - Run lein test, shadow-cljs test
    - Enforce coverage thresholds
    - Build uberjar for deployment
  - Complexity: Medium

---

## Notes/Reminders

- Focus on increasing test coverage before adding new features!
- Always run `lein test` and `npx shadow-cljs compile test` to verify changes (don't just rely on the CI pipeline.)
