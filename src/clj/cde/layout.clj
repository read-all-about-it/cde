(ns cde.layout
  "HTML template rendering and error page generation using Selmer.

   This namespace provides functions for rendering HTML templates with
   Selmer templating engine. Templates are located in resources/html/.

   Features:
   - CSRF token injection via {% csrf-field %} tag
   - Markdown rendering via |markdown filter
   - Error page generation with customisable status/title/message"
  (:require
   [clojure.java.io]
   [selmer.parser :as parser]
   [selmer.filters :as filters]
   [markdown.core :refer [md-to-html-string]]
   [ring.util.http-response :refer [content-type ok]]
   [ring.util.anti-forgery :refer [anti-forgery-field]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
   [ring.util.response]))

(parser/set-resource-path!  (clojure.java.io/resource "html"))
(parser/add-tag! :csrf-field (fn [_ _] (anti-forgery-field)))
(filters/add-filter! :markdown (fn [content] [:safe (md-to-html-string content)]))

(defn render
  "Renders an HTML template using Selmer templating engine.

  Templates are located in `resources/html/`. The CSRF token is
  automatically injected into the template context.

  Arguments:
  - `request` - Ring request map (currently unused, reserved for future)
  - `template` - Template filename relative to resources/html/
  - `params` - Optional map of template variables

  Returns: Ring response with rendered HTML and UTF-8 content type."
  [request template & [params]]
  (content-type
   (ok
    (parser/render-file
     template
     (assoc params
            :page template
            :csrf-token *anti-forgery-token*)))
   "text/html; charset=utf-8"))

(defn error-page
  "Generates an error response with a rendered error page.

  Renders `error.html` template with the provided error details.

  Arguments:
  - `error-details` - Map containing:
    - `:status` - HTTP status code (required)
    - `:title` - Error title for display (optional)
    - `:message` - Detailed error message (optional)

  Returns: Ring response map with status, HTML body, and content-type header."
  [error-details]
  {:status  (:status error-details)
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file "error.html" error-details)})
