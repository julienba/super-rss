(ns super-rss.date-test
  (:require [clojure.test :refer [deftest testing are]]
            [super-rss.date :as sut]))

(deftest parse-test
  (testing "Most common data format are parsed"
    (are [x y] (= x (sut/local-date->date (sut/str->date y)))
         #inst "2021-01-19T00:00:00.000-00:00" "January 19, 2021"
         #inst "2020-10-14T00:00:00.000-00:00" "2020/10/14"
         #inst "2017-08-14T00:00:00.000-00:00" "14 August, 2017"
         #inst "2021-02-04T00:00:00.000-00:00" "4 February, 2021"
         #inst "2021-01-21T00:00:00.000-00:00" "Jan 21 2021"
         #inst "2019-09-10T00:00:00.000-00:00" "Sep 10 2019"
         #inst "2020-10-05T00:00:00.000-00:00" "October 5, 2020"
         #inst "2017-11-27T00:00:00.000-00:00" "Nov 27, 2017")))
