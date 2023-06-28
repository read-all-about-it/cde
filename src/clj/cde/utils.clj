(ns cde.utils
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


(defn html->txt
  "A function for using Jsoup to convert a html string to plain text."
  [html-string]
  (-> html-string
      Jsoup/parse
      .text))