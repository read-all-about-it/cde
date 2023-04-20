# cde

generated using Luminus version "4.47"

FIXME

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run 

Note: for local development, make sure you have postgres running (remember to check `brew services restart postgresql` and `createdb` if you're on a mac!), and ensure that the dev `database-url` is properly specified in `./dev-config.edn`.

## Deployment

The github actions currently include a `deploy.yml` workflow, which builds the uberjar & docker container and deploys to heroku automatically. This includes all database migrations for the postgres addon, so deploying from local should be unnecessary.

However, if you don't have a heroku instance running for the project yet, try:

    heroku create
    heroku addons:create heroku-postgresql:mini

Then you can deploy from local with

    git push heroku master

## License

Copyright © 2023 FIXME
