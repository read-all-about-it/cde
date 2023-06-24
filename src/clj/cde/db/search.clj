(ns cde.db.search
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [nil-fill-default-params]]
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
  (let [default-keys [:title_text
                      :newspaper_title_text
                      :author_nationality
                      ;; :gender
                      ;; :length
                      :author]
        clean-params (-> (nil-fill-default-params default-keys query-params)
                         (set-limit-offset-defaults)
                         (select-keys [:title_text
                                       :newspaper_title_text
                                       :author_nationality
                                       :author_name
                                       ;; :gender
                                       ;; :length
                                       :limit
                                       :offset])
                         (update :title_text prep-for-string-match)
                         (update :newspaper_title_text prep-for-string-match)
                         (update :author_nationality prep-for-string-match)
                         (update :author_name prep-for-string-match)
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
  (let [default-keys [:chapter_text
                      :title_text
                      :newspaper_title_text
                      :author_gender
                      :author_nationality]
        clean-params (-> (nil-fill-default-params default-keys query-params)
                         (set-limit-offset-defaults)
                         (select-keys [:chapter_text
                                       :title_text
                                       :newspaper_title_text
                                       :author_nationality
                                       :author_name
                                       :limit
                                       :offset])
                         (update :chapter_text prep-for-string-match)
                         (update :title_text prep-for-string-match)
                         (update :newspaper_title_text prep-for-string-match)
                         (update :author_nationality prep-for-string-match)
                         (update :author_name prep-for-string-match)
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
