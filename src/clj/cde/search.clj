(ns cde.search
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [kebab->snake nil-fill-default-params]]
   [clojure.string :as str]))

(defn- set-limit-offset-defaults [params]
  (let [limit (or (:limit params) 100)
        offset (or (:offset params) 0)]
    (assoc params :limit limit :offset offset)))

(defn- prep-for-string-match [query]
  (if (or (str/blank? query) (empty? query) (nil? query))
    nil
    (str "%" (str/replace query "%" "") "%")))


(defn search-titles [query-params]
  (let [default-keys [:common-title
                      :newspaper-title
                      :nationality
                      :gender
                      :author]
        clean-params (-> (nil-fill-default-params default-keys query-params)
                         (set-limit-offset-defaults)
                         (select-keys [:common-title
                                       :newspaper-title
                                       :nationality
                                       :author
                                       :gender
                                       :limit
                                       :offset])
                         (kebab->snake)
                         (update :common_title prep-for-string-match)
                         (update :newspaper_title prep-for-string-match)
                         (update :nationality prep-for-string-match)
                         (update :author prep-for-string-match)
                         (update :gender prep-for-string-match)
                         )
        search-results (db/search-titles* clean-params)]
    ;;(db/search-titles* clean-params)
    (println clean-params)
    {:results search-results
     :limit (:limit clean-params)
     :offset (:offset clean-params)
     ;; TODO: Add next link to search results, eg:
     ;; :next (if (< (count search-results) (:limit clean-params))
     ;;          nil
     ;;          (assoc clean-params :offset (+ (:offset clean-params) (:limit clean-params))))
     }))