(ns cde.utils
  "Utility functions for parameter handling, date parsing, and HTML processing.

  Provides helper functions for:
  - Parameter map manipulation (nil-filling, filtering)
  - Date validation and parsing (YYYY-MM-DD format)
  - HTML to plain text conversion via Jsoup"
  (:require
   [clojure.string :as str]
   [java-time.api :as jt])
  (:import
   [org.jsoup Jsoup]))

(defn nil-fill-default-params
  "Fills missing keys in a params map with nil values.

  Ensures all expected keys are present in the map, defaulting to nil.
  Useful for database operations that expect specific columns.

  Arguments:
  - `default-keys` - Collection of keys that should be present
  - `params` - Map of parameter values

  Returns: Map with all `default-keys` present, merged with `params`."
  [default-keys params]
  (let [defaults (into {} (map (fn [k] [k nil]) default-keys))]
    (merge defaults params)))

(defn drop-nil-params
  "Removes entries with nil values from a map.

  Arguments:
  - `params` - Map to filter

  Returns: Map containing only entries where value is not nil."
  [params]
  (into {} (filter (fn [[k v]] (not (nil? v))) params)))

(defn drop-blank-params
  "Removes entries with blank string values from a map.

  Filters out entries where the value is a blank string (empty or whitespace only).
  Non-string values are preserved regardless of their content.

  Arguments:
  - `params` - Map to filter

  Returns: Map with blank string values removed, non-strings preserved."
  [params]
  (into {} (filter (fn [[k v]] (or (not (string? v)) (not (str/blank? v)))) params)))

(defn html->txt
  "Converts an HTML string to plain text using Jsoup.

  Parses HTML and extracts all text content, stripping tags and formatting.
  Useful for generating plain text versions of chapter content.

  Arguments:
  - `html-string` - HTML content as a string

  Returns: Plain text string with HTML tags removed."
  [html-string]
  (-> html-string
      Jsoup/parse
      .text))

;;;; Date utilities

(defn date?
  "Checks if value is a valid date (YYYY-MM-DD string or LocalDate).

  Validates that the input represents a date in ISO format. Accepts:
  - YYYY-MM-DD formatted strings
  - java.time.LocalDate instances

  Arguments:
  - `s` - Value to check (string, LocalDate, or nil)

  Returns: true if valid date, false otherwise."
  [s]
  (cond
    (nil? s) false
    (string? s) (boolean (re-matches #"^\d{4}-\d{2}-\d{2}$" s))
    (instance? java.time.LocalDate s) true
    :else false))

(defn parse-date
  "Parses a date value to LocalDate. Accepts YYYY-MM-DD string or LocalDate.

  Safely parses date strings to java.time.LocalDate instances. Handles:
  - Valid YYYY-MM-DD strings -> LocalDate
  - Existing LocalDate instances -> returned as-is
  - nil, blank strings, or invalid formats -> nil

  Arguments:
  - `s` - Date value (string or LocalDate)

  Returns: LocalDate instance, or nil if input is invalid/blank."
  [s]
  (cond
    (and (string? s) (not (str/blank? s)) (re-matches #"^\d{4}-\d{2}-\d{2}$" s))
    (jt/local-date "yyyy-MM-dd" s)
    (instance? java.time.LocalDate s) s
    :else nil))
