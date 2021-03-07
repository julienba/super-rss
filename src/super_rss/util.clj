(ns super-rss.util
  (:require [clojure.string :as string]))

(defn get-base-url [website-url]
  (let [host   (.getHost (java.net.URI. website-url))
        prefix (if (string/starts-with? website-url "https://")
                 "https://"
                 "http://")]
    (str prefix host)))

(defn url->absolute-url [base-url url]
  (if (string/starts-with? url "/")
    (str base-url url)
    url))
