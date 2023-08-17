(ns cde.db.newspaper
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [nil-fill-default-params]]
   [java-time.api :as jt]))


(defn- date? [s]
  (if (re-matches #"^\d{4}-\d{2}-\d{2}$" s)
    true
    false))


(defn- parse-date [s]
  (jt/local-date "yyyy-MM-dd" s))

(defn- parse-start-end-dates [params]
  (let [start-date (:start_date params)
        end-date (:end_date params)]
    (if (and start-date end-date)
      (if (and (date? start-date) (date? end-date))
        (assoc params :start_date (parse-date start-date)
                      :end_date (parse-date end-date))
        (throw (ex-info "Invalid date format" {:cde/error-id ::invalid-date-format
                                               :error "Date must be in the format YYYY-MM-DD"})))
      params)))

(defn- enforce-issn
  "Ensure that the ISSN is a string, if it's present."
  [params]
  (let [issn (:issn params)]
    (if issn
      (assoc params :issn (str issn))
      params)))


(defn create-newspaper! [params]
  (let [missing (filter #(nil? (params %)) [:title :trove_newspaper_id])
        optional-keys [:common_title :location :start_year :end_year :details
                       :newspaper_type :colony_state :start_date :end_date :issn :added_by]]
    (if (empty? missing)
      (let [existing (jdbc/with-transaction [conn db/*db*]
                       (db/get-newspaper-by-trove-newspaper-id* conn {:trove_newspaper_id (:trove_newspaper_id params)}))]
        ;; (println existing)
        (if-not (empty? existing)
          (throw (ex-info "A newspaper already exists with this Trove Newspaper ID!"
                          {:cde/error-id ::duplicate-newspaper-trove-newspaper-id
                           :error "Newspaper already exists with this Trove Newspaper ID!"}))
          (try
            (->> params
                 (parse-start-end-dates)
                 ;; ensure 'issn' is a string if it's present:
                 (enforce-issn)
                 (nil-fill-default-params optional-keys)
                 (db/create-newspaper!*)
                 (:id))
            (catch Exception e
              (throw (ex-info "Error creating newspaper"
                              {:cde/error-id ::create-newspaper-exception
                               :error (.getMessage e)}))))))
      (throw (ex-info (apply str "Missing required parameters: " (interpose " " missing))
                      {:cde/error-id ::missing-required-params
                       :error (apply str "Missing required parameters: " (interpose " " missing))
                       :missing missing})))))

(defn get-newspaper [id]
  (let [newspaper (db/get-newspaper-by-id* {:id id})]
    (if (empty? newspaper)
      (throw (ex-info "No newspaper found with that ID!"
                      {:cde/error-id ::no-newspaper-found
                       :error "No newspaper found with ID!"}))
      newspaper)))

(defn get-newspaper-by-trove-id
  "Get a newspaper record from our database by its Trove ID."
  [id]
  {:pre [(integer? id)]}
  (let [newspaper (db/get-newspaper-by-trove-newspaper-id* {:trove_newspaper_id id})]
    (if (empty? newspaper)
      (throw (ex-info "No newspaper found with that Trove ID!"
                      {:cde/error-id ::no-newspaper-found
                       :error "No newspaper found with Trove ID!"}))
      newspaper)))

(defn get-titles-in-newspaper [newspaper-id]
  (let [titles (db/get-all-titles-by-newspaper-table-id* {:newspaper_table_id newspaper-id})]
    (if (empty? titles)
      (throw (ex-info "No titles found in that newspaper!"
                      {:cde/error-id ::no-titles-found
                       :error "No titles found in that newspaper!"}))
      titles)))


(defn get-terse-newspaper-list 
  "Get a list of all newspapers, ordered by common title.
   Only return: id, trove_newspaper_id, title, and common_title."
  []
  (let [newspapers (db/get-terse-newspaper-list* {})]
    (if (empty? newspapers)
      (throw (ex-info "No newspapers found!"
                      {:cde/error-id ::no-newspapers-found
                       :error "No newspapers found!"}))
      newspapers)))


(defn trove-newspaper-id->newspaper-id
  "Gets the newspaper id from the database that matches a record
   with a given Trove Newspaper ID. Returns nil if no matching newspaper is found
   in our database."
  [trove-newspaper-id]
  (let [newspaper (db/get-newspaper-by-trove-newspaper-id* {:trove_newspaper_id trove-newspaper-id})]
    (if (empty? newspaper)
      nil
      (:id (first newspaper)))))


(defn get-newspapers
  "Get an unfiltered list of newspapers from the db.
   
   Accepts optional limit & offset params (defaulting to 50 & 0 respectively).
   Limit is capped at 500 for performance reasons.

   Returns a map containing a list of newspapers, along with next/previous links."
  ([]
   (get-newspapers 50 0))
  ([limit]
   (get-newspapers limit 0))
  ([limit offset]
   (let [limit (min limit 500)
         newspapers (db/get-newspapers* {:limit limit :offset offset})
         next (if (= limit (count newspapers))
                (str "/newspapers?limit=" limit "&offset=" (+ offset limit))
                nil)
         prev (if (> offset 0)
                (str "/newspapers?limit=" limit "&offset=" (max (- offset limit) 0))
                nil)]
     {:results newspapers
      :next next
      :previous prev})))