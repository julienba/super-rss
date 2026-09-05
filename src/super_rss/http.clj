(ns super-rss.http
  (:require [exoscale.ok-http :as client])
  (:refer-clojure :exclude [get])
  (:import [java.io File]
           [okhttp3 Cache OkHttpClient OkHttpClient$Builder]))

(defn- create-temp-dir [prefix]
  (let [temp-file (File/createTempFile prefix "")
        temp-dir-path (.getAbsolutePath temp-file)]
    (.delete temp-file)
    (let [temp-dir (File. temp-dir-path)]
      (.mkdirs temp-dir)
      (.getAbsolutePath temp-dir))))

(defn create-cached-client
  "Creates an OkHttp client with a cache."
  [{:keys [cache-path cache-size]
    :or {cache-path (create-temp-dir "http-cache")
         ; 10 MB
         cache-size (* 10 1024 1024)}}]
  (let [cache-dir (File. cache-path)
        cache (Cache. cache-dir cache-size)
        builder (doto (OkHttpClient$Builder.)
                  (.cache cache))]
    (.build builder)))

(def ^OkHttpClient default-client
  (delay (create-cached-client {})))

(def default-user-agent
  "Identifies the library and carries a contact URL. Anonymous tokens are what
   Cloudflare Bot Fight Mode and origin WAFs score as an unverified bot, and they
   leave a site owner with no one to contact and no reason not to ban."
  "super-rss/1.0 (+https://github.com/julienba/super-rss)")

(def accept
  "Tell servers doing content negotiation that a feed is wanted."
  "application/rss+xml, application/atom+xml, application/xml;q=0.9, */*;q=0.8")

(defn headers
  "Headers for one call.
   `context` is the per-call breadcrumb that used to be the whole User-Agent
   (\"sitemap-reader\", \"rss-validator\", ...); it is kept as a suffix so it stays
   visible in a server's logs. `:user-agent` lets a library user say who they are."
  ([context] (headers context nil))
  ([context {:keys [user-agent]}]
   {"User-Agent" (str (or user-agent default-user-agent)
                      (when context (str " " context)))
    "Accept" accept}))

(def default-opts
  {:response-body-decoder :string
   :throw-on-error false})

(defn- request [{:keys [client] :as req}]
  (let [client (or client @default-client)
        req (-> (merge default-opts req)
                ; A call that sets no headers of its own still identifies itself
                (update :headers #(merge (headers nil) %)))]
    (client/request client (dissoc req :client))))

(defn get
  "Convenience wrapper for `request` with method `:get`"
  ([url] (get url nil))
  ([url opts]
   (let [opts (assoc opts :url url :method :get)]
     (request opts))))

(defn delete
  "Convenience wrapper for `request` with method `:delete`"
  ([url] (delete url nil))
  ([url opts]
   (let [opts (assoc opts :url url :method :delete)]
     (request opts))))

(defn head
  "Convenience wrapper for `request` with method `:head`"
  ([url] (head url nil))
  ([url opts]
   (let [opts (assoc opts :url url :method :head)]
     (request opts))))

(defn post
  "Convenience wrapper for `request` with method `:post`"
  ([url] (post url nil))
  ([url opts]
   (let [opts (assoc opts :url url :method :post)]
     (request opts))))

(defn patch
  "Convenience wrapper for `request` with method `:patch`"
  ([url] (patch url nil))
  ([url opts]
   (let [opts (assoc opts :url url :method :patch)]
     (request opts))))

(defn put
  "Convenience wrapper for `request` with method `:put`"
  ([url] (put url nil))
  ([url opts]
   (let [opts (assoc opts :url url :method :put)]
     (request opts))))
