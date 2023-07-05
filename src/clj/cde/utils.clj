(ns cde.utils
  (:require
   [clojure.string :as str])
  (:import
   [org.jsoup Jsoup]))


(defn nil-fill-default-params
  "Fill in missing values in a params map with nil"
  [default-keys params]
  (let [defaults (into {} (map (fn [k] [k nil]) default-keys))]
    (merge defaults params)))

(defn drop-nil-params
  "Take a map and return a map with only the non-nil values"
  [params]
  (into {} (filter (fn [[k v]] (not (nil? v))) params)))

(defn drop-blank-params
  "Take a map and return a map with only the non-blank values (ie, drop empty strings).
   Keep any instances where the value is not a string type."
  [params]
  (into {} (filter (fn [[k v]] (or (not (string? v)) (not (str/blank? v)))) params)))


(defn html->txt
  "A function for using Jsoup to convert a html string to plain text."
  [html-string]
  (-> html-string
      Jsoup/parse
      .text))