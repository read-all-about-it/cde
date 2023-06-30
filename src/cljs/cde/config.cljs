(ns cde.config)


(def auth0-details
  {:client-id "brtvKymeXTTht8IwrQdhwYirSxt65K95"
   :domain "read-all-about-it.au.auth0.com"
  ;;  :redirect-uri "http://localhost:3000"
   :redirect-uri "https://readallaboutit.com.au"
   :audience "https://read-all-about-it.au.auth0.com/api/v2/"
   :response-type "token id_token"
   :scope "openid profile email"})

