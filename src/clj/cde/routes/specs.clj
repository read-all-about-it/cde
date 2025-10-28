(ns cde.routes.specs
  "Shared Clojure specs for API responses and common data types.

  Defines reusable specifications for:
  - Entity responses (author, newspaper, title, chapter)
  - List/pagination responses with next/previous links
  - Search results
  - Trove API integration responses
  - Common field types (IDs, strings, dates)

  These specs serve dual purposes:
  1. Runtime validation via spec coercion in Reitit routes
  2. OpenAPI/Swagger documentation generation

  See also: [[cde.routes.services]] for route composition."
  (:require
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]))

;;;; Common ID spec
(s/def ::pk-id (s/and int? pos?))

;; Platform statistics response
(s/def ::platform-stats-response
  (s/keys :req-un [::chapter-count
                   ::title-count
                   ::author-count
                   ::newspaper-count]))

(s/def ::chapter-count int?)
(s/def ::title-count int?)
(s/def ::author-count int?)
(s/def ::newspaper-count int?)

;; Author response specs
(s/def ::author-response
  (s/keys :req-un [::id ::common_name]
          :opt-un [::other_name ::gender ::nationality
                   ::nationality_details ::author_details]))

(s/def ::author-list-response
  (s/keys :req-un [::results ::limit ::offset]
          :opt-un [::next ::previous]))

(s/def ::author-nationalities-response
  (s/coll-of string?))

(s/def ::author-genders-response
  (s/coll-of string?))

(s/def ::titles-by-author-response
  (s/coll-of ::title-response))

;; Newspaper response specs
(s/def ::newspaper-response
  (s/keys :req-un [::id ::title]
          :opt-un [::location ::details ::newspaper_type
                   ::colony_state ::trove_newspaper_id
                   ::common_title ::start_year ::end_year
                   ::start_date ::end_date ::issn
                   ::added_by ::created_at ::updated_at]))

(s/def ::newspaper-list-response
  (s/keys :req-un [::results ::limit ::offset]
          :opt-un [::next ::previous]))

(s/def ::titles-in-newspaper-response
  (s/coll-of ::title-response))

;; Title response specs
(s/def ::title-response
  (s/keys :req-un [::id]
          :opt-un [::author_id ::newspaper_table_id
                   ::span_start ::span_end
                   ::publication_title ::common_title
                   ::attributed_author_name
                   ::author_of ::additional_info
                   ::inscribed_author_nationality
                   ::inscribed_author_gender
                   ::information_source ::length
                   ::trove_source ::also_published
                   ::name_category ::curated_dataset]))

(s/def ::title-list-response
  (s/keys :req-un [::results ::limit ::offset]
          :opt-un [::next ::previous]))

(s/def ::single-title-response
  (s/merge ::title-response
           (s/keys :opt-un [::author ::newspaper ::chapters])))

;; Chapter response specs
(s/def ::chapter-response
  (s/keys :req-un [::id]
          :opt-un [::chapter_number ::chapter_title
                   ::article_url ::page_references
                   ::page_url ::word_count
                   ::illustrated ::page_sequence
                   ::chapter_html ::chapter_text
                   ::text_title ::export_title
                   ::trove_article_id]))

(s/def ::chapter-list-response
  (s/keys :req-un [::results ::limit ::offset]
          :opt-un [::next ::previous]))

(s/def ::single-chapter-response
  (s/merge ::chapter-response
           (s/keys :opt-un [::title ::newspaper ::author])))

(s/def ::chapters-within-title-response
  (s/coll-of ::chapter-response))

;; Search response specs
(s/def ::search-titles-response
  (s/keys :req-un [::results ::search_type ::limit ::offset]
          :opt-un [::next ::previous ::query]))

(s/def ::search-chapters-response
  (s/keys :req-un [::results ::search_type ::limit ::offset]
          :opt-un [::next ::previous ::query]))

(s/def ::search-newspapers-response
  (s/coll-of ::newspaper-response))

;; Trove integration response specs
(s/def ::trove-newspaper-response
  (s/keys :req-un [::title]
          :opt-un [::id ::state ::issn ::trove_url]))

(s/def ::trove-article-response
  (s/keys :opt-un [::trove_article_id ::chapter_title
                   ::chapter_number ::article_url ::final_date
                   ::page_number ::page_url ::corrections
                   ::word_count ::illustrated ::last_corrected
                   ::page_sequence ::chapter_html ::chapter_text
                   ::trove_newspaper_id]))

;; Common field specs
(s/def ::id ::pk-id)
(s/def ::common_name string?)
(s/def ::other_name (s/nilable string?))
(s/def ::gender (s/nilable string?))
(s/def ::nationality (s/nilable string?))
(s/def ::nationality_details (s/nilable string?))
(s/def ::author_details (s/nilable string?))
(s/def ::newspaper_title (s/nilable string?))
(s/def ::location (s/nilable string?))
(s/def ::details (s/nilable string?))
(s/def ::newspaper_type (s/nilable string?))
(s/def ::colony_state (s/nilable string?))
(s/def ::publication_title (s/nilable string?))
(s/def ::attributed_author_name (s/nilable string?))
(s/def ::author_of (s/nilable string?))
(s/def ::additional_info (s/nilable string?))
(s/def ::inscribed_author_nationality (s/nilable string?))
(s/def ::inscribed_author_gender (s/nilable string?))
(s/def ::information_source (s/nilable string?))
(s/def ::length (s/nilable int?))
(s/def ::trove_source (s/nilable string?))
(s/def ::also_published (s/nilable string?))
(s/def ::name_category (s/nilable string?))
(s/def ::curated_dataset (s/nilable boolean?))
(s/def ::chapter_number (s/nilable string?))
(s/def ::chapter_title (s/nilable string?))
(s/def ::article_url (s/nilable string?))
(s/def ::page_references (s/nilable int?))
(s/def ::page_url (s/nilable string?))
(s/def ::word_count (s/nilable int?))
(s/def ::illustrated (s/nilable boolean?))
(s/def ::page_sequence (s/nilable string?))
(s/def ::chapter_html (s/nilable string?))
(s/def ::chapter_text (s/nilable string?))
(s/def ::final_date (s/nilable string?))
(s/def ::page_number (s/nilable int?))
(s/def ::corrections (s/nilable int?))
(s/def ::last_corrected (s/nilable string?))
(s/def ::text_title (s/nilable string?))
(s/def ::export_title (s/nilable string?))
(s/def ::trove_article_id (s/nilable int?))
(s/def ::trove_newspaper_id (s/nilable int?))
(s/def ::common_title (s/nilable string?))
(s/def ::author_id (s/nilable int?))
(s/def ::newspaper_table_id (s/nilable int?))
(s/def ::span_start (s/nilable any?))  ;; Can be string or LocalDate
(s/def ::span_end (s/nilable any?))    ;; Can be string or LocalDate
(s/def ::start_year (s/nilable int?))
(s/def ::end_year (s/nilable int?))
(s/def ::start_date (s/nilable any?))  ;; Can be string or LocalDate
(s/def ::end_date (s/nilable any?))    ;; Can be string or LocalDate
(s/def ::added_by (s/nilable int?))
(s/def ::created_at any?)
(s/def ::updated_at any?)
(s/def ::title string?)
(s/def ::state (s/nilable string?))
(s/def ::issn (s/nilable string?))
(s/def ::trove_url (s/nilable string?))
(s/def ::date (s/nilable string?))
(s/def ::page (s/nilable int?))
(s/def ::snippet (s/nilable string?))
(s/def ::results coll?)
(s/def ::limit int?)
(s/def ::offset int?)
(s/def ::next (s/nilable string?))
(s/def ::previous (s/nilable string?))
(s/def ::search_type string?)
(s/def ::query map?)
(s/def ::author map?)
(s/def ::newspaper map?)
(s/def ::chapters coll?)
