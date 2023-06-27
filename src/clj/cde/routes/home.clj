(ns cde.routes.home
  (:require
   [cde.layout :as layout]
   [cde.db.core :as db]
   [clojure.java.io :as io]
   [cde.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))



(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
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
                    (-> (response/ok (-> "docs/people.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]
   ])

