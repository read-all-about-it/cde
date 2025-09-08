# To Be Continued: Read All About It

To Be Continued is a Collaborative Digital Editing ('CDE') Platform. It's built to enable the discovery, collation, editing, and publishing of serialised fiction found in the [National Library of Australia's Trove](https://trove.nla.gov.au/) newspaper archive.

The CDE platform is built with Clojure & ClojureScript using the [Luminus](http://www.luminusweb.net/) framework. This codebase covers both the backend and frontend of the application. The frontend is a Single-Page Application (SPA) using [Reagent](https://reagent-project.github.io/) (a React wrapper) for the UI. The backend is a RESTful API with a [PostgreSQL](https://www.postgresql.org/) database. It uses [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html) for ClojureScript compilation, [re-frame](https://github.com/Day8/re-frame) for state management, [Selmer](https://github.com/yogthos/Selmer) for templating, and [Auth0](https://auth0.com/) for authentication & authorization.


## Requirements

- [Leiningen](https://leiningen.org/)
- [Node.js](https://nodejs.org/) & [npm](https://www.npmjs.com/)
- [Postgres](https://www.postgresql.org/)
- [Auth0](https://auth0.com/) account and application credentials

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
. # TODO: add project structure
```

## Development

### REPL-Driven Development

To start the REPL, run:

```
lein repl # start the REPL
```

Then:

```clojure
;; Start/stop the server
(start)
(stop)
(restart)

;; Run migrations
(migrate)
(rollback)
```

### Testing

```bash
lein test # Run all backend tests
```


### Database Migrations

If you're making changes to your local dev database, use `lein repl` (instead of `lein run`), then:
1. `(start)` to start the server,
2. `(migrate)` to run all existing migrations, and
3. `(create-migration "migration-name")` to create a new migration.

You can also use `lein run migrate` to run all existing migrations, and `lein run rollback` to rollback the latest migration.

## Configuration

### Environment Variables

TK

### shadow-cljs Configuration

TK

## Deployment

We currently use a Heroku buildpack setup for building & running the application as follows:

### Heroku Deployment with Buildpacks (Current)

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


### Docker Deployment (Planned)

TK #TODO: add Docker deployment instructions

## API Overview

### Authentication Endpoints
- TK

### API Routes
- TK

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


## Contributing

TK

## License

TK

## Acknowledgments

TK
