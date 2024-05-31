(ns super-rss.hickory-zipper-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [super-rss.hickory-zipper :as sut]))

(deftest string->zipper
  (is (= [{:type :document,
           :content
           [{:type :element,
             :attrs nil,
             :tag :html,
             :content
             [{:type :element, :attrs nil, :tag :head, :content nil}
              {:type :element,
               :attrs nil,
               :tag :body,
               :content
               [{:type :element,
                 :attrs {:href "foo"},
                 :tag :a,
                 :content ["bar" {:type :element, :attrs nil, :tag :br, :content nil}]}]}]}]}
          nil]
         (sut/string->zipper "<a href=foo>bar<br></a>"))))

(deftest anchors?
  (let [nodes [{:type :element, :attrs nil, :tag :html, :content [{:type :element, :attrs nil, :tag :head, :content nil} {:type :element, :attrs nil, :tag :body, :content [{:type :element, :attrs {:href "foo"}, :tag :a, :content ["bar" {:type :element, :attrs nil, :tag :br, :content nil}]}]}]}
               {:type :element, :attrs nil, :tag :head, :content nil}
               {:type :element, :attrs nil, :tag :body, :content [{:type :element, :attrs {:href "foo"}, :tag :a, :content ["bar" {:type :element, :attrs nil, :tag :br, :content nil}]}]}
               {:type :element, :attrs {:href "foo"}, :tag :a, :content ["bar" {:type :element, :attrs nil, :tag :br, :content nil}]}
               "bar"
               {:type :element, :attrs nil, :tag :br, :content nil}]]
    (is (= [{:type :element, :attrs {:href "foo"}, :tag :a, :content ["bar" {:type :element, :attrs nil, :tag :br, :content nil}]}]
           (filter #'sut/anchor? nodes)))))

(deftest cleanup-anchors
  (is (= {:type :document,
          :content
          [{:type :element,
            :attrs nil,
            :tag :html,
            :content
            [{:type :element, :attrs nil, :tag :head, :content nil}
             {:type :element,
              :attrs nil,
              :tag :body,
              :content
              [{:type :element,
                :attrs {:href "http://company.com/foo"},
                :tag :a,
                :content ["bar" {:type :element, :attrs nil, :tag :br, :content nil}]}]}]}]}
         (sut/cleanup-anchors (sut/string->zipper "<a href=foo>bar<br></a>") "http://company.com/")
         (sut/cleanup-anchors (sut/string->zipper "<a href=/foo>bar<br></a>") "http://company.com/"))))

(def sample1 (slurp (io/resource "smart_links/sample1.html")))

(deftest cleanup-anchors-on-html-page
  (doseq [path ["smart_links/sample1.html"
                "smart_links/sample2.html"
                "smart_links/sample3.html"]]
    (testing (str "when cleaning anchors for " path)
      (is (sut/cleanup-anchors
           (sut/string->zipper (slurp (io/resource path)))
           "http://company.com/")
          "no exception is thrown when parsing a full file"))))
