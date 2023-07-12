# To Be Continued / Read All About It: Collaborative Digital Editing Platform

To Be Continued is a platform for

- discovering,
- collating,
- adding bibliographic information to,
- editing, &
- publishing

fiction from the National Library of Australia's "Trove" Newspaper archive. This codebase covers both the backend and frontend of the platform. It's written in Clojure & ClojureScript using the [Luminus](http://www.luminusweb.net/) framework.

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run 

Note: for local development, make sure you have postgres running (remember to check `brew services restart postgresql` and `createdb` if you're on a mac!), and ensure that the dev `database-url` is properly specified in `./dev-config.edn`.

To start hot-reloading of the frontend via shadow-cljs, run:

    npx shadow-cljs watch app

We sometimes see issues with the hot-reloading when using Safari, so we recommend using Chrome for development. Recommended process is:

1. Open a terminal and run `npx shadow-cljs watch app`
2. Open a second terminal window and run `lein run`
3. Open `localhost:3000` in Chrome

If you're making changes to the testing database, instead use `lein repl`, then `(start)` and `(migrate)` to start the server and run migrations, and `(create-migration "migration-name")` to create a new migration.

## Deployment

We currently deploy to heroku. If you don't have a heroku instance running for the project yet, try:

    heroku create
    heroku addons:create heroku-postgresql:mini

Then you can deploy from local with

    git push heroku master

A common problem (causing a build failure & rejected push from heroku) occurs when heroku tries to run the Clojure buildpack *before* the NodeJS buildpack. You can address this in the settings for your Heroku app: explicitly add both buildpacks, and ensure that NodeJS is placed above Clojure.

## License

Copyright © 2023 FIXME
