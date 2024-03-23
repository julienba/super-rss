(ns super-rss.util
  (:require [clojure.string :as string]))

(defn get-base-url [website-url]
  (let [host   (.getHost (java.net.URI. website-url))
        prefix (if (string/starts-with? website-url "https://")
                 "https://"
                 "http://")]
    (str prefix host)))

(defn url->absolute-url [root-url url]
  (cond
    (string/starts-with? url "http")
    url

    (not= "/" (str (first url)))
    (str root-url url)

    (= "/" (str (first url)))
    (str (subs root-url 0 (dec (count root-url))) url)))
