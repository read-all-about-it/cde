# To Be Continued: Read All About It

To Be Continued is a Collaborative Digital Editing ('CDE') Platform. It's built to enable the discovery, collation, editing, and publishing of serialised fiction found in the [National Library of Australia's Trove](https://trove.nla.gov.au/) newspaper archive.

The CDE platform is built with Clojure & ClojureScript using the [Luminus](http://www.luminusweb.net/) framework. This codebase covers both the backend and frontend of the application. The frontend is a Single-Page Application (SPA) using [Reagent](https://reagent-project.github.io/) (a React wrapper) for the UI. The backend is a RESTful API with a [PostgreSQL](https://www.postgresql.org/) database. It uses [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html) for ClojureScript compilation, [re-frame](https://github.com/Day8/re-frame) for state management, [Selmer](https://github.com/yogthos/Selmer) for templating, and [Auth0](https://auth0.com/) for authentication & authorization.


## Requirements

- [Leiningen](https://leiningen.org/)
- [Node.js](https://nodejs.org/) & [npm](https://www.npmjs.com/)
- [Postgres](https://www.postgresql.org/)
- [Auth0](https://auth0.com/) account and application credentials
- [Trove API key](https://trove.nla.gov.au/about/create-something/using-api)

## Quick Start

### Development Setup

1. Clone the repository & install dependencies
```bash
git clone https://github.com/read-all-about-it/cde
cd cde
npm install # frontend dependencies
```

2. Set up your database
```bash
createdb cde_dev # create a development database
```

3. Configure your environment variables
```bash
cp dev-config.edn.example dev-config.edn
# edit 'dev-config.edn' with your database & Auth0 credentials, and Trove API keys
```

4. Migrate your database
```bash
lein run migrate
```

5. Start the Backend Web Server for the application
```bash
lein run # start the backend web server
```

6. Start the Frontend Hot-Reloader for the application
```bash
npx shadow-cljs watch app # compilation and hot-reload for ClojureScript
```

7. Open your browser to `http://localhost:3000`

## Project Structure

```
cde/
├── src/
│   ├── clj/cde/                    # Backend (Clojure)
│   │   ├── core.clj                # Application entry point, server lifecycle
│   │   ├── handler.clj             # Main request router, route composition
│   │   ├── config.clj              # Configuration loading (cprop)
│   │   ├── middleware.clj          # Request/response middleware, auth wrappers
│   │   ├── jwt_auth.clj            # JWT RS256 verification with JWKS caching
│   │   ├── layout.clj              # Selmer template rendering
│   │   ├── trove.clj               # Trove API integration
│   │   ├── utils.clj               # Parameter handling, HTML processing
│   │   ├── nrepl.clj               # nREPL server configuration
│   │   ├── middleware/
│   │   │   └── formats.clj         # Muuntaja request/response formatting
│   │   ├── db/                     # Database layer (domain-driven)
│   │   │   ├── core.clj            # Connection management, type conversions
│   │   │   ├── author.clj          # Author CRUD operations
│   │   │   ├── chapter.clj         # Chapter CRUD with Trove sync
│   │   │   ├── title.clj           # Title CRUD with date parsing
│   │   │   ├── newspaper.clj       # Newspaper CRUD operations
│   │   │   ├── user.clj            # User get-or-create operations
│   │   │   ├── search.clj          # Full-text search with fuzzy matching
│   │   │   └── platform.clj        # Platform-level statistics
│   │   └── routes/                 # HTTP route handlers
│   │       ├── services.clj        # Route composition, Swagger setup
│   │       ├── specs.clj           # Shared response specifications
│   │       ├── home.clj            # Home page, static content
│   │       ├── auth.clj            # Authentication endpoints
│   │       ├── author.clj          # Author API endpoints
│   │       ├── chapter.clj         # Chapter API endpoints
│   │       ├── title.clj           # Title API endpoints
│   │       ├── newspaper.clj       # Newspaper API endpoints
│   │       ├── search.clj          # Search API endpoints
│   │       ├── platform.clj        # Platform statistics endpoints
│   │       └── trove.clj           # Trove proxy endpoints
│   │
│   ├── cljc/cde/                   # Shared code (Clojure & ClojureScript)
│   │   └── validation.cljc         # Shared validation logic
│   │
│   └── cljs/cde/                   # Frontend (ClojureScript)
│       ├── core.cljs               # SPA entry point, Reitit routing
│       ├── events.cljs             # re-frame event handlers
│       ├── subs.cljs               # re-frame subscriptions
│       ├── ajax.cljs               # HTTP client, auth header injection
│       ├── config.cljs             # Auth0 configuration
│       ├── utils.cljs              # UI utilities, field metadata
│       ├── views/                  # Page components
│       │   ├── home.cljs           # Landing page
│       │   ├── about.cljs          # About, Team, FAQ pages
│       │   ├── author.cljs         # Author detail/create/edit
│       │   ├── chapter.cljs        # Chapter detail/create/edit
│       │   ├── title.cljs          # Title detail/create/edit
│       │   ├── newspaper.cljs      # Newspaper detail/create/edit
│       │   ├── search.cljs         # Search interface
│       │   ├── contribute.cljs     # Contribution guide
│       │   └── settings.cljs       # User settings
│       └── components/             # Reusable UI components
│           ├── nav.cljs            # Navigation bar
│           ├── forms.cljs          # Form utilities, tabbed forms
│           ├── modals.cljs         # Modal dialogs
│           ├── metadata.cljs       # Metadata display
│           ├── search.cljs         # Search UI component
│           ├── login.cljs          # Login UI
│           └── forms/              # Entity form components
│               ├── creation.cljs   # All entity creation forms
│               └── editing.cljs    # All entity editing forms
│
├── resources/
│   ├── migrations/                 # SQL migration files (up/down pairs)
│   ├── sql/
│   │   └── queries.sql             # HugSQL query definitions
│   ├── html/                       # HTML templates (home.html, error.html)
│   ├── public/                     # Static assets (CSS, JS, images)
│   └── docs/                       # Markdown documentation (about, faq, etc.)
│
├── test/
│   ├── clj/cde/                    # Backend tests
│   │   ├── handler_test.clj        # HTTP handler tests
│   │   ├── middleware_test.clj     # Middleware unit tests
│   │   ├── db/                     # Database layer tests
│   │   └── routes/                 # Route handler tests
│   └── cljs/cde/                   # Frontend tests
│
├── env/                            # Environment-specific configurations
│   ├── dev/                        # Development environment
│   ├── prod/                       # Production environment
│   └── test/                       # Test environment
│
├── project.clj                     # Leiningen project configuration
├── shadow-cljs.edn                 # shadow-cljs build configuration
├── package.json                    # Node.js dependencies
├── dev-config.edn                  # Local development config (git-ignored)
└── test-config.edn                 # Test configuration
```

## Development

### REPL-Driven Development

REPL-driven development is the preferred workflow. To start the REPL, run:

```bash
lein repl # start the REPL
```

Then:

```clojure
;; Start/stop the server
(start)
(stop)
(restart)

;; Database operations
(migrate)                        ; Run all migrations
(rollback)                       ; Rollback latest migration
(create-migration "name")        ; Create a new migration
(reset-db)                       ; Reset database
(restart-db)                     ; Restart database connection
```

### Testing

```bash
# Run all backend tests
lein test

# Run a specific test
lein test :only cde.db.author-test/test-create-author

# Run ClojureScript tests
npx shadow-cljs compile test
node target/test/test.js
```

### Documentation

We use `codox` to generate documentation for the Clojure codebase. To generate documentation, run:

```bash
lein codox # generate documentation
```

### Database Migrations

If you're making changes to your local dev database, use `lein repl` (instead of `lein run`), then:
1. `(start)` to start the server,
2. `(migrate)` to run all existing migrations, and
3. `(create-migration "migration-name")` to create a new migration.

You can also use `lein run migrate` to run all existing migrations, and `lein run rollback` to rollback the latest migration.

## Configuration

### Environment Variables

Configuration is managed via EDN files. Create a `dev-config.edn` file in the project root (this file is git-ignored):

```clojure
{:dev true
 :port 3000
 
 ;; nREPL server port for REPL connections
 :nrepl-port 7000

 ;; PostgreSQL database connection URL
 :database-url "jdbc:postgresql://localhost/cde_dev?user=YOUR_USER&password=YOUR_PASSWORD"

 ;; Trove API keys (get yours at https://trove.nla.gov.au/about/create-something/using-api)
 :trove-api-keys ["YOUR_TROVE_API_KEY"]

 ;; Auth0 configuration
 :auth0-details {:client-id "YOUR_AUTH0_CLIENT_ID"
                 :domain "YOUR_DOMAIN.auth0.com"
                 :redirect-uri "http://localhost:3000"
                 :audience "https://your-api-identifier/"
                 :response-type "token id_token"
                 :scope "openid profile email read:users write:records"}}
```

| Key | Description |
|-----|-------------|
| `:port` | HTTP server port (default: 3000) |
| `:nrepl-port` | nREPL server port for REPL connections |
| `:database-url` | JDBC connection string for PostgreSQL |
| `:trove-api-keys` | Vector of Trove API keys (multiple for rotation) |
| `:auth0-details` | Auth0 SPA configuration map |

### shadow-cljs Configuration

The `shadow-cljs.edn` file configures ClojureScript compilation:

```clojure
{:nrepl {:port 7002}                    ; nREPL port for ClojureScript REPL
 :builds
 {:app
  {:target     :browser                 ; Browser target for SPA
   :output-dir "target/cljsbuild/public/js"
   :asset-path "/js"
   :modules    {:app {:entries [cde.app]}}
   :devtools   {:watch-dir "resources/public"
                :preloads  [re-frisk.preload]}  ; re-frisk for state inspection
   :dev        {:closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}}
   :release    {:compiler-options {:optimizations :simple}}}
  :test
  {:target :node-test
   :output-to "target/test/test.js"
   :autorun true}}}
```

Key builds:
- **`:app`** - Main application build with hot-reloading and re-frisk devtools
- **`:test`** - Node.js test runner for ClojureScript tests

## Deployment

We currently use a Heroku buildpack setup for building & running the application as follows:

### Heroku Deployment with Buildpacks

For initial setup, run:

```bash
heroku create your-app-name # create a new Heroku app
heroku addons:create heroku-postgresql:essential-2 # create a new Heroku Postgres database
```

To deploy from local, run:
```bash
git push heroku master
```

A common problem (causing a build failure & rejected push from heroku) occurs when heroku tries to run the Clojure buildpack *before* the NodeJS buildpack. You can address this in the settings for your Heroku app: explicitly add both buildpacks, and ensure that NodeJS is placed above Clojure.

### Production Build

To create a production build locally:

```bash
# Build the ClojureScript release bundle
npx shadow-cljs release app

# Create the uberjar (includes compiled ClojureScript)
lein uberjar

# Run the uberjar
java -jar target/uberjar/cde.jar
```

## API Overview

The API is served under `/api/v1`. Swagger documentation is available at `/api/api-docs/` when running in development.

### Authentication

Authentication uses Auth0 with JWT (RS256) tokens. Protected endpoints require a valid `Authorization: Bearer <token>` header. The backend validates tokens against Auth0's JWKS endpoint with a 10-minute cache.

### Entity Endpoints

| Entity | Endpoint | Methods | Auth Required |
|--------|----------|---------|---------------|
| **Authors** | `/api/v1/authors` | GET | No |
| | `/api/v1/author/:id` | GET, PUT | PUT requires auth |
| | `/api/v1/author/:id/titles` | GET | No |
| | `/api/v1/create/author` | POST | Yes |
| **Titles** | `/api/v1/titles` | GET | No |
| | `/api/v1/title/:id` | GET, PUT | PUT requires auth |
| | `/api/v1/title/:id/chapters` | GET | No |
| | `/api/v1/create/title` | POST | Yes |
| **Chapters** | `/api/v1/chapters` | GET | No |
| | `/api/v1/chapter/:id` | GET, PUT | PUT requires auth |
| | `/api/v1/create/chapter` | POST | Yes |
| **Newspapers** | `/api/v1/newspapers` | GET | No |
| | `/api/v1/newspaper/:id` | GET, PUT | PUT requires auth |
| | `/api/v1/newspaper/:id/titles` | GET | No |
| | `/api/v1/create/newspaper` | POST | Yes |

### Search Endpoints

| Endpoint | Description |
|----------|-------------|
| `/api/v1/search/titles` | Search titles by text, author, newspaper, nationality, gender |
| `/api/v1/search/chapters` | Search chapter content with fuzzy matching |
| `/api/v1/search/newspapers` | Search newspapers by Trove ID |

### Platform Endpoints

| Endpoint | Description |
|----------|-------------|
| `/api/v1/platform/statistics` | Get counts of all entities in the database |
| `/api/v1/platform/search-options` | Get available filter values (nationalities, genders) |
| `/api/v1/options/newspapers` | Get newspaper list for dropdowns |
| `/api/v1/options/authors` | Get author list for dropdowns |

### Trove Proxy Endpoints

These endpoints proxy requests to the National Library of Australia's Trove API:

| Endpoint | Description |
|----------|-------------|
| `/api/v1/trove/newspaper/:trove_id` | Fetch newspaper metadata from Trove |
| `/api/v1/trove/chapter/:trove_id` | Fetch article content from Trove |
| `/api/v1/trove/exists/newspaper/:trove_id` | Check if newspaper exists in TBC database |
| `/api/v1/trove/exists/chapter/:trove_id` | Check if chapter exists in TBC database |

### Pagination

List endpoints support pagination via query parameters:
- `limit` - Maximum number of results (default: 50, max: 500)
- `offset` - Number of results to skip (default: 0)

Example: `/api/v1/titles?limit=20&offset=40`

## Troubleshooting

Some common issues & tips on how to fix them (mostly in local development):

### Hot-reloading isn't hot-reloading
We sometimes see issues with the hot-reloading when using Safari, so we recommend using Chrome for development. Recommended process is:
1. Open a terminal and run `npx shadow-cljs watch app`
2. Open a second terminal window and run `lein run`
3. Open `localhost:3000` in Chrome to see the application

### Shadow-cljs compilation errors

```bash
# Clear build cache
rm -rf .shadow-cljs/
npx shadow-cljs compile app
```

### Database Connection Issues

```bash
# Verify PostgreSQL is running
pg_isready

# Check connection database-url string in your config.edn
```

(Remember to check `brew services restart postgresql` and `createdb` if you're on a mac!)

### Auth0 Redirect Issues

- Verify callback URL in Auth0 dashboard matches your configuration
- Check CORS settings if using different ports for development
- Ensure the `redirect-uri` in `dev-config.edn` matches your Auth0 application settings

### Trove API Issues

- Verify your Trove API key is valid at [Trove API Console](https://trove.nla.gov.au/about/create-something/using-api)
- Check that your API key is correctly configured in `dev-config.edn`
- Note: Trove rate limits requests; the platform supports multiple API keys for rotation

### Database Migration Errors

```bash
# If migrations are out of sync, check current state
lein run migrate

# To rollback and retry
lein run rollback
lein run migrate
```

