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

    (and (= "/" (str (last root-url)))
         (= "/" (str (first url))))
    (str root-url (subs url 1))


    (and (not= "/" (str (last root-url)))
         (not= "/" (str (first url))))
    (str root-url "/" url)

    ;; suffix of root-url and url are the same
    (= (last (string/split root-url #"/"))
       (second (string/split url #"/")))
    (str root-url (subs url (inc (count (second (string/split url #"/"))))))

    (and (not= "/" (str (last root-url)))
         (= "/" (str (first url))))
    (str root-url url)

    :else (str root-url url)))
