(ns super-rss.util
  (:require [clojure.string :as string])
  (:import (java.net URI
                     URL)))

(defn get-base-url [website-url]
  (let [host   (.getHost (URI. website-url))
        prefix (if (string/starts-with? website-url "https://")
                 "https://"
                 "http://")]
    (str prefix host)))

(defn url->absolute-url [root-url url]
  (if (or (string/starts-with? url "javascript:")
          (string/starts-with? url "mailto:"))
    url
    (let [root-url (URL. root-url)
          url (URL. root-url url)]
      (str url))))
