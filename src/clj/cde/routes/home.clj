(ns cde.routes.home
  "Home page and static content routes.

  Serves the main SPA entry point and markdown documentation files.
  These routes handle the non-API portion of the application."
  (:require
   [cde.layout :as layout]
   [cde.db.core :as db]
   [clojure.java.io :as io]
   [cde.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page
  "Renders the main SPA entry point (home.html template).

  The rendered page loads the ClojureScript application which handles
  all client-side routing and rendering."
  [request]
  (layout/render request "home.html"))

(defn home-routes
  "Returns route definitions for home page and static content.

  Routes:
  - `/` - Main SPA entry point
  - `/docstxt` - Documentation markdown
  - `/abouttxt` - About page markdown
  - `/faqtxt` - FAQ markdown
  - `/teamtxt` - Team page markdown

  All routes use CSRF protection and HTTPS redirect middleware."
  []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats
                 middleware/wrap-https-redirect]}
   ["/" {:get home-page}]
   ["/docstxt" {:get (fn [_]
                       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                           (response/header "Content-Type" "text/plain; charset=utf-8")))}]
   ["/abouttxt" {:get (fn [_]
                        (-> (response/ok (-> "docs/about.md" io/resource slurp))
                            (response/header "Content-Type" "text/plain; charset=utf-8")))}]
   ["/faqtxt" {:get (fn [_]
                      (-> (response/ok (-> "docs/faq.md" io/resource slurp))
                          (response/header "Content-Type" "text/plain; charset=utf-8")))}]

   ["/teamtxt" {:get (fn [_]
                       (-> (response/ok (-> "docs/team.md" io/resource slurp))
                           (response/header "Content-Type" "text/plain; charset=utf-8")))}]])
