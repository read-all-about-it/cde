(ns cde.utils
  (:require
   [camel-snake-kebab.core :as csk]))

(defn kebab->snake
  "Convert all :key-words in a params map to snake_case :key_words."
  [params]
  (->> params
       (map (fn [[k v]]
              [(csk/->snake_case k) v]))
       (into {})))

(defn nil-fill-default-params
  "Fill in missing values in a params map with nil"
  [default-keys params]
  (let [defaults (into {} (map (fn [k] [k nil]) default-keys))]
    (merge defaults params)))