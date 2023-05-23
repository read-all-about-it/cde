(ns cde.search
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [kebab->snake]]))

(defn- set-limit-offset-defaults [params]
  (let [limit (or (:limit params) 10)
        offset (or (:offset params) 0)]
    (assoc params :limit limit :offset offset)))

(defn- prep-for-string-match [query]
  (str "%" query "%"))

(defn search-titles [query-params]
  (let [clean-params (-> query-params
                         (set-limit-offset-defaults)
                         (select-keys [:common-title :limit :offset])
                         (kebab->snake)
                         (update :common_title prep-for-string-match))
       search-results (db/search-titles* clean-params)
        ]
    ;;(db/search-titles* clean-params)
    {:results search-results
     :limit (:limit clean-params)
     :offset (:offset clean-params)
     ;; TODO: Add next link to search results, eg:
     ;; :next (if (< (count search-results) (:limit clean-params))
     ;;          nil
     ;;          (assoc clean-params :offset (+ (:offset clean-params) (:limit clean-params))))
     }
  ))