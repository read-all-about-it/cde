(ns cde.jwt-auth
  "JWT authentication with RS256 signature verification"
  (:require
   [clojure.tools.logging :as log]
   [buddy.sign.jwt :as buddy-jwt]
   [buddy.core.codecs.base64 :as b64]
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str])
  (:import [java.security KeyFactory]
           [java.security.spec RSAPublicKeySpec]
           [java.math BigInteger]))

(def jwks-url "https://read-all-about-it.au.auth0.com/.well-known/jwks.json")

;; Cache for JWKS to avoid fetching on every request
(def jwks-cache (atom {:keys nil :fetched-at 0}))

;; Cache duration in milliseconds (10 minutes)
(def cache-duration-ms (* 10 60 1000))

(defn base64url-decode
  "Decode base64url string (different from regular base64)"
  [s]
  (-> s
      (str/replace "-" "+")
      (str/replace "_" "/")
      (as-> s (str s (apply str (repeat (mod (- 4 (mod (count s) 4)) 4) "="))))
      (b64/decode)))

(defn jwk->public-key
  "Convert a JWK to a Java PublicKey for RS256 verification"
  [jwk]
  (try
    (let [n-bytes (base64url-decode (:n jwk))
          e-bytes (base64url-decode (:e jwk))
          n (BigInteger. 1 n-bytes)
          e (BigInteger. 1 e-bytes)
          spec (RSAPublicKeySpec. n e)
          factory (KeyFactory/getInstance "RSA")]
      (.generatePublic factory spec))
    (catch Exception e
      (log/error e "Failed to convert JWK to public key")
      nil)))

(defn fetch-jwks
  "Fetch JWKS from Auth0 endpoint with caching"
  []
  (let [now (System/currentTimeMillis)
        {:keys [keys fetched-at]} @jwks-cache]
    (if (and keys (< (- now fetched-at) cache-duration-ms))
      (do
        (log/debug "Using cached JWKS, valid for"
                   (/ (- cache-duration-ms (- now fetched-at)) 1000) "more seconds")
        keys)
      (try
        (log/info "Fetching JWKS from" jwks-url)
        (let [response (http/get jwks-url {:socket-timeout 5000
                                           :connection-timeout 5000
                                           :accept :json
                                           :as :json})
              status (:status response)
              _ (log/info "JWKS fetch response status:" status)
              _ (when (not= 200 status)
                  (log/error "Non-200 status from JWKS endpoint:" status
                             "body:" (:body response))
                  (throw (ex-info "Failed to fetch JWKS"
                                  {:status status
                                   :body (:body response)})))

              ;; Response body is already parsed as JSON when using :as :json
              jwks (:body response)
              _ (log/debug "Parsed JWKS structure:" (clojure.core/keys jwks))
              jwks-keys (:keys jwks)]

          (if (and jwks-keys (pos? (count jwks-keys)))
            (do
              (reset! jwks-cache {:keys jwks-keys :fetched-at now})
              (log/info "Successfully fetched and cached" (count jwks-keys) "JWKS keys")
              (doseq [key jwks-keys]
                (log/debug "Cached key:" {:kid (:kid key) :alg (:alg key) :use (:use key)}))
              jwks-keys)
            (do
              (log/error "No keys found in JWKS response. Full response:" jwks)
              (throw (ex-info "No keys in JWKS" {:jwks jwks})))))

        (catch Exception e
          (log/error e "Failed to fetch JWKS from" jwks-url
                     "Error:" (.getMessage e))
          ;; Return cached keys if available
          (if-let [cached-keys (:keys @jwks-cache)]
            (do
              (log/warn "Using stale cached JWKS due to fetch failure")
              cached-keys)
            (do
              (log/error "No cached JWKS available, authentication will fail")
              nil)))))))

(defn verify-jwt
  "Verify JWT signature and validate claims"
  [token]
  (try
    ;; Parse JWT parts
    (let [parts (str/split token #"\.")
          _ (when-not (= 3 (count parts))
              (log/error "Invalid JWT format. Expected 3 parts, got" (count parts))
              (throw (ex-info "Invalid JWT format" {:parts (count parts)})))

          ;; Decode header to get kid and algorithm
          header-str (first parts)
          header-bytes (base64url-decode header-str)
          header (json/parse-string (String. header-bytes "UTF-8") true)
          kid (:kid header)
          alg (:alg header)
          _ (log/debug "JWT header:" {:kid kid :alg alg})]

      ;; Verify algorithm is RS256
      (when-not (= alg "RS256")
        (log/error "Invalid JWT algorithm:" alg "expected RS256")
        (throw (ex-info "Invalid algorithm" {:alg alg :expected "RS256"})))

      ;; Fetch JWKS and find the matching key
      (let [jwks (fetch-jwks)]

        (when-not jwks
          (log/error "No JWKS available for verification")
          (throw (ex-info "JWKS unavailable" {})))

        (let [jwk (first (filter #(= (:kid %) kid) jwks))]

          (when-not jwk
            (log/error "No matching key found in JWKS for kid:" kid
                       "Available kids:" (map :kid jwks))
            (throw (ex-info "No matching key found in JWKS"
                            {:kid kid
                             :available-kids (map :kid jwks)})))

          ;; Convert JWK to public key
          (let [public-key (jwk->public-key jwk)]
            (when-not public-key
              (log/error "Failed to create public key from JWK" {:kid kid :jwk jwk})
              (throw (ex-info "Failed to create public key from JWK" {:kid kid})))

            ;; Verify the JWT signature using buddy
            (let [claims (buddy-jwt/unsign token public-key {:alg :rs256})
                  _ (log/debug "JWT claims verified for user:" (:sub claims))]

              ;; Additional claim validations
              (let [now (quot (System/currentTimeMillis) 1000)
                    exp (:exp claims)
                    iss (:iss claims)
                    aud (:aud claims)]

                ;; Check expiration
                (when (and exp (< exp now))
                  (log/warn "JWT expired by" (- now exp) "seconds")
                  (throw (ex-info "JWT expired"
                                  {:exp exp
                                   :now now
                                   :expired-by (- now exp)})))

                ;; Check issuer
                (when-not (= iss "https://read-all-about-it.au.auth0.com/")
                  (log/error "Invalid JWT issuer:" iss)
                  (throw (ex-info "Invalid issuer" {:iss iss})))

                ;; Check audience contains our API
                (when-not (or (= aud "https://readallaboutit.com.au/api/v1/")
                              (and (sequential? aud)
                                   (some #(= % "https://readallaboutit.com.au/api/v1/") aud)))
                  (log/error "Invalid JWT audience:" aud)
                  (throw (ex-info "Invalid audience" {:aud aud})))

                ;; Return validated claims
                (log/info "JWT validated successfully for user:" (:sub claims))
                claims))))))

    (catch clojure.lang.ExceptionInfo e
      (log/warn "JWT validation failed:" (.getMessage e) (ex-data e))
      nil)
    (catch Exception e
      (log/error e "Unexpected error during JWT validation")
      nil)))
