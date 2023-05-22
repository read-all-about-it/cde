(ns cde.platform
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]))

(defn get-platform-statistics
  "Call the database and get the platform statistics, returning a map on success."
  []
  (jdbc/with-transaction [t-conn db/*db*]
    (let [newspaper-count (db/count-newspapers t-conn)
          author-count (db/count-authors t-conn)
          title-count (db/count-titles t-conn)
          chapter-count (db/count-chapters t-conn)]
      (cond (empty? newspaper-count)
            (throw (ex-info "Attempt to count newspapers in db failed!"
                            {:cde/error-id ::newspaper-count-failed
                             :error "Attempt to count newspapers failed!"}))
            (empty? author-count)
            (throw (ex-info "Attempt to count authors in db failed!"
                            {:cde/error-id ::author-count-failed
                             :error "Attempt to count authors failed!"}))
            (empty? title-count)
            (throw (ex-info "Attempt to count titles in db failed!"
                            {:cde/error-id ::title-count-failed
                             :error "Attempt to count titles failed!"}))
            (empty? chapter-count)
            (throw (ex-info "Attempt to count chapters in db failed!"
                            {:cde/error-id ::chapter-count-failed
                             :error "Attempt to count chapters failed!"}))
            :else {:newspaper-count newspaper-count
                   :author-count author-count
                   :title-count title-count
                   :chapter-count chapter-count}))))