(ns cde.handler
  "Main request router and route composition.

   This namespace assembles all application routes into a single Ring handler,
   combining the home routes (SPA serving) with API service routes. It also
   configures Swagger UI for API documentation and handles default responses
   for 404/405/406 errors.

   Key components:
   - `app-routes`: Mount state containing the compiled Reitit router
   - `app`: Returns the fully-wrapped Ring handler with all middleware"
  (:require
   [cde.middleware :as middleware]
   [cde.layout :refer [error-page]]
   [cde.routes.home :refer [home-routes]]
   [cde.routes.services :refer [service-routes]]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring :as ring]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.webjars :refer [wrap-webjars]]
   [cde.env :refer [defaults]]
   [mount.core :as mount]))

(mount/defstate init-app
  "Mount state for environment-specific initialisation.

  Executes `:init` and `:stop` functions from [[cde.env/defaults]],
  which differ between development and production builds."
  :start ((or (:init defaults) (fn [])))
  :stop  ((or (:stop defaults) (fn []))))

(defn- ^:no-doc async-aware-default-handler
  "Default handler that supports both sync and async Ring request patterns.

  Returns nil for unmatched routes, allowing fallback to subsequent handlers."
  ([_] nil)
  ([_ respond _] (respond nil)))

(mount/defstate app-routes
  "Mount state containing the compiled Reitit router.

  Combines:
  - [[cde.routes.home/home-routes]]: SPA serving and static pages
  - [[cde.routes.services/service-routes]]: RESTful API endpoints
  - Swagger UI at `/swagger-ui`
  - Static resource handling
  - Default error handlers (404, 405, 406)"
  :start
  (ring/ring-handler
   (ring/router
    [(home-routes)
     (service-routes)])
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path   "/swagger-ui"
      :url    "/api/swagger.json"
      :config {:validator-url nil}})
    (ring/create-resource-handler
     {:path "/"})
    (wrap-content-type
     (wrap-webjars async-aware-default-handler))
    (ring/create-default-handler
     {:not-found
      (constantly (error-page {:status 404, :title "404 - Page not found"}))
      :method-not-allowed
      (constantly (error-page {:status 405, :title "405 - Not allowed"}))
      :not-acceptable
      (constantly (error-page {:status 406, :title "406 - Not acceptable"}))}))))

(defn app
  "Returns the fully-wrapped Ring handler for the application.

   Wraps the compiled routes with base middleware (authentication, error
   handling, CORS, etc.) using a var reference to support REPL reloading."
  []
  (middleware/wrap-base #'app-routes))
