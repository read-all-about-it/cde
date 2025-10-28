(ns cde.db.platform
  "Platform-level statistics and metrics.

  Provides aggregate statistics about the platform's content,
  including counts of newspapers, authors, titles, and chapters.

  Used by the home page and API to display platform overview data."
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]))

(defn get-platform-statistics
  "Retrieves aggregate counts for all primary entities in the platform.

  Executes count queries for newspapers, authors, titles, and chapters
  within a single transaction.

  Returns: Map with keys:
  - `:newspaper-count` - Total newspapers in database
  - `:author-count` - Total authors in database
  - `:title-count` - Total titles in database
  - `:chapter-count` - Total chapters in database

  Throws: ex-info if any count query fails."
  []
  (jdbc/with-transaction [t-conn db/*db*]
    (let [newspaper-count (db/count-newspapers* t-conn)
          author-count (db/count-authors* t-conn)
          title-count (db/count-titles* t-conn)
          chapter-count (db/count-chapters* t-conn)]
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
            :else {:newspaper-count (:count newspaper-count)
                   :author-count (:count author-count)
                   :title-count (:count title-count)
                   :chapter-count (:count chapter-count)}))))
