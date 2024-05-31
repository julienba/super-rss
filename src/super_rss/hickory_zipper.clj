(ns super-rss.hickory-zipper
  (:require [clojure.zip :as z]
            [hickory.core :as h]
            [hickory.zip :as hz]
            [super-rss.util :as util]))

(defn string->zipper [s]
  (hz/hickory-zip (h/as-hickory (h/parse s))))

(defn- anchor? [node]
  (and (map? node)
       (= :a (:tag node))
       (get-in node [:attrs :href])))

(defn cleanup-anchors [html-zipper root-url]
  (loop [zp (z/next html-zipper)]
    (if (z/end? zp)
      (z/node zp)
      (let [node (z/node zp)]
        (if (anchor? node)
          (recur (-> zp
                     (z/edit (fn [node]
                               (update-in node [:attrs :href] #(util/url->absolute-url root-url %))))
                     z/next))
          (recur (z/next zp)))))))
