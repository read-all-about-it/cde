(ns cde.config
  "Application configuration constants.

  Contains configuration values that are compiled into the ClojureScript
  application. Currently holds Auth0 authentication settings.

  Note: For sensitive configuration that should not be in source control,
  consider using environment variables injected at build time.")

(def auth0-details
  "Auth0 authentication configuration map.

  Contains all settings required for Auth0 integration:
  - `:client-id` - Auth0 application client ID
  - `:domain` - Auth0 tenant domain
  - `:redirect-uri` - URL to redirect after authentication
  - `:audience` - API identifier for access token audience claim
  - `:response-type` - OAuth response type (token and id_token)
  - `:scope` - OAuth scopes (openid, profile, email, custom API scopes)

  Used by [[cde.events]] for Auth0 WebAuth client initialization."
  {:client-id "brtvKymeXTTht8IwrQdhwYirSxt65K95"
   :domain "read-all-about-it.au.auth0.com"
   :redirect-uri "https://readallaboutit.com.au"
  ;;  :audience "https://read-all-about-it.au.auth0.com/api/v2/"
   :audience "https://readallaboutit.com.au/api/v1/"
   :response-type "token id_token"
   :scope "openid profile email read:users write:records"})
