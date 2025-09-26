(ns super-rss.robots-txt
  (:require [babashka.http-client :as http]
            [clojure.string :as string]))

(defn get-robots-txt
  "If found return it as a vector of key map like:
    [{:key \"Sitemap\", :value \"https://yolo.cc/sitemap/sitemap.xml\"}
     {:key \"License\", :value \"https://medium.com/license.xml\"}])"
  [base-url]
  (let [{:keys [status body]} (http/get (str base-url "/robots.txt") {:throw false})]
    (when (= 200 status)
      (->> (string/split-lines body)
           (map (fn [line]
                  (let [[k v] (string/split line #": ")]
                    {:key k :value v})))))))

(defn medium? [robots-vec]
  (some (fn [robots-line] (= {:key "License" :value "https://medium.com/license.xml"}
                               robots-line))
          robots-vec))

;; (def X (get-robots-txt "https://uxdesign.cc/"))
;; (medium? X)