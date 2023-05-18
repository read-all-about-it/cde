(ns cde.newspaper
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [kebab->snake]]
   [java-time.api :as jt]
   ))


(defn- fill-missing-values
  "Fill in missing values in the create-newspaper! params map with nil"
  [params]
  (let [defaults {:common-title nil
                  :location nil
                  :start-year nil
                  :end-year nil
                  :details nil
                  :newspaper-type nil
                  :colony-state nil
                  :start-date nil
                  :end-date nil
                  :issn nil}]
    (merge defaults params)))

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
  (let [missing (filter #(nil? (params %)) [:title :trove-newspaper-id])]
    (if (empty? missing)
      (jdbc/with-transaction [conn db/*db*]
        (if-not (empty? (db/get-newspaper-by-trove-newspaper-id* conn {:trove_newspaper_id (:trove-newspaper-id params)}))
          (throw (ex-info "A newspaper already exists with this Trove newspaper ID!"
                          {:cde/error-id ::duplicate-newspaper-trove-newspaper-id
                           :error "Newspaper already exists with this Trove newspaper ID!"}))
          (try
            (->> params
                 (fill-missing-values)
                 (parse-start-end-dates)
                 (kebab->snake)
                 (db/create-newspaper!* conn))
            (catch Exception e
              (throw (ex-info "Error creating newspaper"
                              {:cde/error-id ::create-newspaper-exception
                               :error (.getMessage e)}))))))
      (throw (ex-info "Missing required parameters" {:missing missing})))))