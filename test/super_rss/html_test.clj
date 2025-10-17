(ns super-rss.html-test
  (:require [clojure.test :refer [deftest are]]
            [super-rss.html :as sut]))

(deftest clean-title-test
  (are [x y] (= x (#'sut/clean-title y))
    ; Basic trimming
    "iOS" "iOS "
    "iOS" " iOS"
    "iOS" "  iOS  "
    ; Pipe separator
    "iOS 15 Update Impact on Open Tracking" "iOS 15 Update Impact on Open Tracking | Bird"
    "iOS 15 Update Impact on Open Tracking" "iOS 15 Update Impact on Open Tracking || Bird"
    ; Whitespace around pipes
    "Title" "Title   |   Subtitle"
    "Title" "Title|Subtitle"
    "Article" "Article   |Subtitle"
    "Article" "Article|   Subtitle"
    ; No pipe
    "Simple Title" "Simple Title"
    "Simple Title" "  Simple Title  "
    ; Pipe at end
    "Title" "Title |"
    "Title" "Title|"
    "Title" "Title   |   "
    ; Only non-empty content
    nil nil
    "" ""
    "" "   "
    "" "|"
    "" "  |  "
    ; Non-string inputs
    nil 123
    nil []
    nil {}))

