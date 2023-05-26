(ns cde.search
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [kebab->snake nil-fill-default-params]]
   [clojure.string :as str]))

(defn- set-limit-offset-defaults [params]
  (let [limit (or (:limit params) 50)
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
                      ;; :gender
                      ;; :length
                      :author]
        clean-params (-> (nil-fill-default-params default-keys query-params)
                         (set-limit-offset-defaults)
                         (select-keys [:common-title
                                       :newspaper-title
                                       :nationality
                                       :author
                                       ;; :gender
                                       ;; :length
                                       :limit
                                       :offset])
                         (kebab->snake)
                         (update :common_title prep-for-string-match)
                         (update :newspaper_title prep-for-string-match)
                         (update :nationality prep-for-string-match)
                         (update :author prep-for-string-match)
                         ;; length must be 0, 1, or 8 (or nil)
                         (update :length
                                 (fn [x] (cond (nil? x) nil
                                               (empty? x) nil
                                               (str/blank? x) nil
                                               (= x "0") 0
                                               (= x "1") 1
                                               (= x "8") 8
                                               (= x 0) 0
                                               (= x 1) 1
                                               (= x 8) 8
                                               :else nil)))
                         ;; (update :gender prep-for-string-match)
                         )
        search-results (db/search-titles* clean-params)]
    (println "Search for titles: " clean-params)
    {:results search-results
     :limit (:limit clean-params)
     :offset (:offset clean-params)
     :search_type "title"
     ;; TODO: Add next link to search results, eg:
     ;; :next (if (< (count search-results) (:limit clean-params))
     ;;          nil
     ;;          (assoc clean-params :offset (+ (:offset clean-params) (:limit clean-params))))
     }))


(defn search-chapters
  "Search within chapters for a given string, optionally filtered by ID"
  [query-params]
  (let [default-keys [:chapter-text :common-title :newspaper-title :gender :nationality]
        clean-params (-> (nil-fill-default-params default-keys query-params)
                         (set-limit-offset-defaults)
                         (select-keys [:chapter-text
                                       :common-title
                                       :newspaper-title
                                       :nationality
                                       :author
                                       :limit
                                       :offset])
                         (kebab->snake)
                         (update :chapter_text prep-for-string-match)
                         (update :common_title prep-for-string-match)
                         (update :newspaper_title prep-for-string-match)
                         (update :nationality prep-for-string-match)
                         (update :author prep-for-string-match)
                         ;; length must be 0, 1, or 8 (or nil)
                         (update :length
                                 (fn [x] (cond (nil? x) nil
                                               (empty? x) nil
                                               (str/blank? x) nil
                                               (= x "0") 0
                                               (= x "1") 1
                                               (= x "8") 8
                                               (= x 0) 0
                                               (= x 1) 1
                                               (= x 8) 8
                                               :else nil))))
        search-results (db/search-chapters* clean-params)]
    (println "Search for chapters: " clean-params)
    {:results search-results
     :limit (:limit clean-params)
     :offset (:offset clean-params)
     :search_type "chapter"}))