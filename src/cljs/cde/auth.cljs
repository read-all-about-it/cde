(ns cde.auth
  (:require [re-frame.core :as re-frame]
            [cde.config :as config]
            ["@auth0/auth0-spa-js" :as auth0]))

(defonce auth0-client (atom nil))


