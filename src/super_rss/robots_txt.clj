(ns super-rss.robots-txt
  (:require [clojure.string :as string]
            [super-rss.http :as http]))

(defn get-robots-txt
  "If found return it as a vector of key map like:
    [{:key \"Sitemap\", :value \"https://yolo.cc/sitemap/sitemap.xml\"}
     {:key \"License\", :value \"https://medium.com/license.xml\"}])"
  [base-url]
  (let [{:keys [status body]} (http/get (str base-url "/robots.txt") {:throw-on-error false})]
    (when (= 200 status)
      (->> (string/split-lines body)
           (remove string/blank?)
           (map (fn [line]
                  (let [[k v] (string/split line #": ")]
                    {:key k :value v})))))))

(defn medium? [robots-vec]
  (some (fn [robots-line] (= {:key "License" :value "https://medium.com/license.xml"}
                               robots-line))
          robots-vec))

;; (def resp (get-robots-txt "https://uxdesign.cc"))
;; (medium? resp)

;; (get-robots-txt "https://mastra.ai/")