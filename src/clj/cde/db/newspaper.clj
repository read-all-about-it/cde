(ns cde.db.newspaper
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [kebab->snake nil-fill-default-params]]
   [java-time.api :as jt]
   ))


(defn- date? [s]
  (if (re-matches #"^\d{4}-\d{2}-\d{2}$" s)
    true
    false))

(defn- parse-date [s]
  (jt/local-date "yyyy-MM-dd" s))

(defn- parse-start-end-dates [params]
  (let [start-date (:start-date params)
        end-date (:end-date params)]
    (if (and start-date end-date)
      (if (and (date? start-date) (date? end-date))
        (assoc params :start-date (parse-date start-date)
                      :end-date (parse-date end-date))
        (throw (ex-info "Invalid date format" {:cde/error-id ::invalid-date-format
                                               :error "Date must be in the format YYYY-MM-DD"})))
      params)))

(defn create-newspaper! [params]
  (let [missing (filter #(nil? (params %)) [:title :trove-newspaper-id])
        optional-keys [:common-title :location :start-year :end-year :details
                       :newspaper-type :colony-state :start-date :end-date :issn :added-by]]
    (if (empty? missing)
      (let [existing (jdbc/with-transaction [conn db/*db*]
                       (db/get-newspaper-by-trove-newspaper-id* conn {:trove_newspaper_id (:trove-newspaper-id params)}))]
        (if-not (empty? existing)
          (throw (ex-info "A newspaper already exists with this Trove Newspaper ID!"
                          {:cde/error-id ::duplicate-newspaper-trove-newspaper-id
                           :error "Newspaper already exists with this Trove Newspaper ID!"}))
          (try
            (->> params
                 (parse-start-end-dates)
                 (nil-fill-default-params optional-keys)
                 (kebab->snake)
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

(defn get-titles-in-newspaper [newspaper-id]
  (let [titles (db/get-all-titles-by-newspaper-table-id* {:newspaper_table_id newspaper-id})]
    (if (empty? titles)
      (throw (ex-info "No titles found in that newspaper!"
                      {:cde/error-id ::no-titles-found
                       :error "No titles found in that newspaper!"}))
      titles)))


(defn get-newspaper-list 
  "Get a list of all newspapers, ordered by common title.
   Only return: id, trove_newspaper_id, title, and common_title."
  []
  (let [newspapers (db/get-newspaper-terse-list* {})]
    (if (empty? newspapers)
      (throw (ex-info "No newspapers found!"
                      {:cde/error-id ::no-newspapers-found
                       :error "No newspapers found!"}))
      newspapers)))