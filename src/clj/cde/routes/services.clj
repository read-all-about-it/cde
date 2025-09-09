(ns cde.routes.services
  "Main service routes composition - combines all domain-specific route modules"
  (:require
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [cde.middleware.formats :as formats]
   [cde.middleware :as mw]
   ;; Import all domain route namespaces
   [cde.routes.auth :as auth]
   [cde.routes.platform :as platform]
   [cde.routes.search :as search]
   [cde.routes.newspaper :as newspaper]
   [cde.routes.author :as author]
   [cde.routes.title :as title]
   [cde.routes.chapter :as chapter]
   [cde.routes.trove :as trove]
   ;; Specs for shared response types
   [cde.routes.specs :as specs]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]))

;; Re-export shared response specs for backward compatibility
;; These specs are used across multiple route namespaces
(s/def ::platform-stats-response ::specs/platform-stats-response)
(s/def ::author-nationalities-response ::specs/author-nationalities-response)
(s/def ::author-genders-response ::specs/author-genders-response)
(s/def ::newspaper-response ::specs/newspaper-response)
(s/def ::newspaper/list-response ::specs/newspaper-list-response)
(s/def ::author-response ::specs/author-response)
(s/def ::author/list-response ::specs/author-list-response)
(s/def ::title-response ::specs/title-response)
(s/def ::title/list-response ::specs/title-list-response)
(s/def ::single-title-response ::specs/single-title-response)
(s/def ::chapter-response ::specs/chapter-response)
(s/def ::chapter/list-response ::specs/chapter-list-response)
(s/def ::single-chapter-response ::specs/single-chapter-response)
(s/def ::chapters-within-title-response ::specs/chapters-within-title-response)
(s/def ::titles-by-author-response ::specs/titles-by-author-response)
(s/def ::titles-in-newspaper-response ::specs/titles-in-newspaper-response)
(s/def ::search/titles-response ::specs/search-titles-response)
(s/def ::search/chapters-response ::specs/search-chapters-response)
(s/def ::search/newspapers-response ::specs/search-newspapers-response)
(s/def ::trove-newspaper-response ::specs/trove-newspaper-response)
(s/def ::trove-article-response ::specs/trove-article-response)

(defn service-routes
  "Compose all domain-specific routes into a single service router"
  []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:info {:title "To Be Continued API"
                     :description "API for the Collaborative Digital Editing platform"}}
    :middleware [;; swagger feature
                 swagger/swagger-feature
                 ;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response body
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; Swagger documentation endpoints
   ["/swagger.json"
    {:get (swagger/create-swagger-handler)}]

   ["/api-docs/*"
    {:get (swagger-ui/create-swagger-ui-handler
           {:url "/api/swagger.json"
            :config {:validator-url nil}})}]

   ;; API v1 routes - composed from domain modules
   (vec (concat
         ["/v1"]
         (auth/auth-routes)
         (platform/platform-routes)
         (search/search-routes)
         (newspaper/newspaper-routes)
         (author/author-routes)
         (title/title-routes)
         (chapter/chapter-routes)
         (trove/trove-routes)))])
