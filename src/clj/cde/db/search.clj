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

(defn- prep-for-leven [query]
  (if (or (str/blank? query) (empty? query) (nil? query))
    nil
    (str/replace query "%" "")))

(defn add-levenshtein-tolerances
  "Take a map of search query params and add tolerances for levenshtein distance.
   :title_text_tolerance = length of title text query / 4 (min 1; max 4)
   :newspaper_title_text_tolerance = 1 
   :author_name_tolerance = length of author name / 4 (min 1; max 4)
   
   THESE MUST BE INTEGERS.
   "
  [params]
  (let [title-text-tolerance (min 4 (max 1 (int (/ (count (:title_text params)) 4))))
        newspaper-title-text-tolerance 1
        author-name-tolerance (min 4 (max 1 (int (/ (count (:author_name params)) 4))))]
    (assoc params
           :title_text_tolerance title-text-tolerance
           :newspaper_title_text_tolerance newspaper-title-text-tolerance
           :author_name_tolerance author-name-tolerance)))


(defn search-titles
  "Perform a complex search within the titles table."
  
  [query-params]
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
                                       :limit
                                       :offset])
                         (update :title_text prep-for-leven)
                         (update :newspaper_title_text prep-for-leven)
                         (update :author_nationality prep-for-leven)
                         (update :author_name prep-for-leven)
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
                                               :else nil))))]
    (println "Search for titles: " clean-params)
    {:results (db/search-titles* clean-params)
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
