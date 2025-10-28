(ns cde.middleware.formats
  "Muuntaja configuration for request/response content negotiation.

   Configures the Muuntaja instance with custom Transit handlers for
   Java time types (java.time.Instant, LocalDate, etc.). This enables
   seamless serialisation of dates between the Clojure backend and
   ClojureScript frontend via Transit+JSON format."
  (:require
   [luminus-transit.time :as time]
   [muuntaja.core :as m]))

(def instance
  "Muuntaja instance configured with Transit time handlers.

  Extends Muuntaja's default configuration with custom Transit handlers
  for Java time types, enabling serialisation of:
  - `java.time.Instant`
  - `java.time.LocalDate`
  - `java.time.LocalDateTime`
  - Other java.time types

  Used by [[cde.middleware/wrap-formats]] for request/response encoding.

  Supported formats: Transit+JSON, JSON, EDN (Muuntaja defaults)."
  (m/create
   (-> m/default-options
       (update-in
        [:formats "application/transit+json" :decoder-opts]
        (partial merge time/time-deserialization-handlers))
       (update-in
        [:formats "application/transit+json" :encoder-opts]
        (partial merge time/time-serialization-handlers)))))
