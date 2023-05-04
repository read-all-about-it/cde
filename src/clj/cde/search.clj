(ns cde.search
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]))

(defn prepare-query-params [query]
  (let [{:keys [length nationality author newspaper-title common-title]} query]
    {:length (when length (Integer/parseInt length))
     :nationality (when nationality (str "%" nationality "%"))
     :author (when author (str "%" author "%"))
     :newspaper_title (when newspaper-title (str "%" newspaper-title "%"))
     :common_title (when common-title (str "%" common-title "%"))}))

(defn search-titles [query limit offset]
  (let [query-params (prepare-query-params query)]
    (db/search-titles* (assoc query-params :limit limit :offset offset))))
