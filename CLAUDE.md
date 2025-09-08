# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

To Be Continued is a Collaborative Digital Editing (CDE) Platform for discovering, collating, editing, and publishing serialised fiction from the National Library of Australia's Trove newspaper archive. Built with Clojure/ClojureScript using the Luminus framework.

## Technology Stack

- **Backend**: Clojure with Luminus framework, RESTful API
- **Frontend**: ClojureScript SPA with Reagent (React wrapper) and re-frame for state management
- **Database**: PostgreSQL with Conman for connection management
- **Build Tools**: Leiningen for Clojure, shadow-cljs for ClojureScript compilation
- **Auth**: Auth0 for authentication & authorization
- **Templates**: Selmer for server-side templating

## Essential Development Commands

### Starting Development Environment
```bash
# Install dependencies
npm install

# Start backend server (port 3000)
lein run

# In separate terminal, start ClojureScript compilation with hot-reload
npx shadow-cljs watch app

# Or use REPL for interactive development
lein repl
# Then in REPL:
(start)     # Start server
(stop)      # Stop server
(restart)   # Restart server
```

### Database Operations
```bash
# Run migrations
lein run migrate

# Rollback latest migration
lein run rollback

# From REPL:
(migrate)                        # Run all migrations
(rollback)                       # Rollback latest
(create-migration "name")        # Create new migration
(reset-db)                       # Reset database
(restart-db)                     # Restart database connection
```

### Testing
```bash
# Run backend tests
lein test

# Run specific test
lein test :only namespace.name/test-name
```

### Building for Production
```bash
# Create uberjar
lein uberjar

# Shadow-cljs release build
npx shadow-cljs release app
```

## Architecture Overview

### Directory Structure

**Backend (Clojure)**
- `src/clj/cde/` - Core backend code
  - `handler.clj` - Main request router, combines all routes
  - `core.clj` - Application entry point and server configuration
  - `middleware.clj` - Request/response middleware stack
  - `routes/` - HTTP route definitions
    - `home.clj` - Serves the main SPA
    - `services.clj` - API endpoints
  - `db/` - Database layer with domain-specific namespaces
    - `core.clj` - Database connection and core queries
    - `author.clj`, `chapter.clj`, `title.clj`, etc. - Domain models
  - `trove.clj` - Integration with Trove API

**Frontend (ClojureScript)**
- `src/cljs/cde/` - Core frontend code
  - `core.cljs` - Main SPA entry point, routing setup
  - `events.cljs` - re-frame event handlers for state mutations
  - `subs.cljs` - re-frame subscriptions for state queries
  - `ajax.cljs` - HTTP client configuration
  - `views/` - Page components (home, author, chapter, title, etc.)
  - `components/` - Reusable UI components
    - `forms/` - Form components for creation and editing
    - `modals.cljs`, `nav.cljs`, `metadata.cljs` - UI elements

**Configuration**
- `dev-config.edn` - Development environment configuration (database, Auth0, API keys)
- `project.clj` - Leiningen project configuration and dependencies
- `shadow-cljs.edn` - ClojureScript build configuration

**Database**
- `resources/migrations/` - SQL migration files (up/down pairs)
- `resources/sql/queries.sql` - HugSQL queries (if present)

### Key Architectural Patterns

1. **Mount State Management**: Uses Mount for lifecycle management of stateful components (database, server, etc.)

2. **re-frame Event System**: Frontend state management follows re-frame's unidirectional data flow:
   - Events trigger state changes
   - Subscriptions provide reactive views of state
   - Components subscribe to state and dispatch events

3. **Domain-Driven Database Layer**: Database operations are organized by domain entities (author, chapter, title, newspaper) rather than technical concerns

4. **Reitit Routing**: Both backend (API) and frontend (SPA) use Reitit for routing with route data and controllers

5. **Migration-Based Schema**: Database schema evolves through timestamped migration files

## Important Development Notes

- The application requires Auth0 credentials and Trove API keys configured in `dev-config.edn`
- Frontend runs on shadow-cljs with hot-reloading enabled in development
- REPL-driven development is the preferred workflow - use `lein repl` and the functions in the `user` namespace
- Database must be created manually before first run: `createdb cde_dev`
- Full-text search uses PostgreSQL's tsvector columns with triggers for automatic updates
- The fuzzy match PostgreSQL extension is required for certain search features