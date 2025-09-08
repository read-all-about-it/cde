(ns cde.ajax
  (:require
   [ajax.core :as ajax]
   [luminus-transit.time :as time]
   [cognitect.transit :as transit]
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn get-auth-token
  "Gets the Auth0 access token from the re-frame database"
  []
  (let [db @(rf/subscribe [:auth/tokens])]
    db))

(defn default-headers [request]
  (if (local-uri? request)
    (let [token (get-auth-token)
          base-headers {"x-csrf-token" js/csrfToken}
          headers (if token
                    (do
                      (.log js/console "Ajax interceptor - token from db:" token)
                      (.log js/console "Ajax interceptor - token type:" (type token))
                      (let [auth-value (str "Bearer " token)
                            periods (count (filter #(= % \.) auth-value))]
                        (.log js/console "Ajax interceptor - final auth header:" auth-value)
                        (.log js/console "Ajax interceptor - periods in auth header:" periods)
                        (assoc base-headers "Authorization" auth-value)))
                    base-headers)]
      (-> request
          (update :headers #(merge headers %))))
    request))

;; injects transit serialization config into request options

(defn as-transit [opts]
  (merge {:format          (ajax/transit-request-format
                            {:writer (transit/writer :json time/time-serialization-handlers)})
          :response-format (ajax/transit-response-format
                            {:reader (transit/reader :json time/time-deserialization-handlers)})}
         opts))

(defn load-interceptors! []
  (swap! ajax/default-interceptors
         conj
         (ajax/to-interceptor {:name "default headers"
                               :request default-headers})))
