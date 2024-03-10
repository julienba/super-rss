(ns super-rss.date
  (:require [java-time :as jt])
  (:import [java.time ZoneId]))

(def common-format ["yyyy/MM/dd" "dd/MM/yyyy" "MM/dd/yyyy" "MMMM d, yyyy"
                    "MMM dd yyyy" "mmm dd yyyy" "MMM dd, yyyy"
                    "d MMMM, yyyy" "yyyy-MM-dd" "dd MMM yyyy"])

(defn str->date* [s format]
  (try
    (jt/local-date format s)
    (catch Exception _ nil)))

(defn str->date
  "Try multiple date format on a string until it match a date"
  [s]
  (some #(when (str->date* s %) (str->date* s %))
        common-format))

;; TODO find the format that work better for the list of date candidate and reparse all with it

(def zoneid-utc (ZoneId/of "UTC"))

(defn- local-date->instant
  "Converts a LocalDate to a instant using UTC"
  [ld]
  (-> ld
      (.atStartOfDay zoneid-utc)
      (.toInstant)))

(defn local-date->date [ld]
  (-> ld
      local-date->instant
      java.util.Date/from))
