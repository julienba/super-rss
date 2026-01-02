(ns super-rss.impl.normal-test
  (:require [clojure.test :refer [deftest testing are is]]
            [net.cgrand.enlive-html :as html]
            [super-rss.impl.normal :as sut]))

(defn- response [status content-type body]
  {:status status
   :headers {"content-type" content-type}
   :body body})

(deftest valid-rss-response?-test
  (testing "Valid RSS responses"
    (are [content-type body description] (sut/valid-rss-response? (response 200 content-type body))
      "application/rss+xml; charset=utf-8"
      "<?xml version=\"1.0\"?><rss version=\"2.0\"><channel></channel></rss>"
      "Standard RSS with application/rss+xml content-type"

      "application/xml"
      "<?xml version=\"1.0\"?><rss version=\"2.0\"><channel></channel></rss>"
      "RSS with application/xml content-type"

      "text/xml; charset=utf-8"
      "<rss version=\"2.0\"><channel></channel></rss>"
      "RSS without XML declaration"

      "application/atom+xml"
      "<?xml version=\"1.0\"?><feed xmlns=\"http://www.w3.org/2005/Atom\"></feed>"
      "Atom feed"

      "application/rdf+xml"
      "<RDF xmlns=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"></RDF>"
      "RDF feed"))

  (testing "Valid RSS responses - case insensitivity"
    (are [content-type body description] (sut/valid-rss-response? (response 200 content-type body))
      "APPLICATION/RSS+XML"
      "<?xml version=\"1.0\"?><rss version=\"2.0\"><channel></channel></rss>"
      "Uppercase content-type"

      "Application/Xml"
      "<RSS version=\"2.0\"><channel></channel></rss>"
      "Mixed case content-type and uppercase RSS tag"

      "application/atom+xml"
      "<FEED xmlns=\"http://www.w3.org/2005/Atom\"></FEED>"
      "Uppercase feed tag"

      "application/rss+xml"
      "  <?xml version=\"1.0\"?><rss version=\"2.0\"><channel></channel></rss>"
      "Body with leading whitespace"))

  (testing "Invalid responses - HTML instead of RSS"
    (are [content-type body description] (not (sut/valid-rss-response? (response 200 content-type body)))
      "text/html; charset=UTF-8"
      "<!doctype html><html><head></head><body></body></html>"
      "HTML response with text/html content-type"

      "text/html"
      "<?xml version=\"1.0\"?><html></html>"
      "HTML response even with XML declaration"))

  (testing "Invalid responses - wrong status"
    (are [status content-type body description] (not (sut/valid-rss-response? (response status content-type body)))
      301 "application/rss+xml" nil "Redirect status code"
      404 "text/html" "Not found" "404 status code"
      500 "application/xml" "<?xml version=\"1.0\"?><rss></rss>" "Server error status"))

  (testing "Invalid responses - missing or invalid body"
    (are [content-type body description] (not (sut/valid-rss-response? (response 200 content-type body)))
      "application/rss+xml" nil "Nil body"
      "application/rss+xml" "   " "Whitespace-only body"
      "application/rss+xml" "" "Empty string body"
      "application/rss+xml" "not xml content" "Non-XML body content"))

  (testing "Invalid responses - missing or invalid content-type"
    (are [content-type body description] (not (sut/valid-rss-response? (response 200 content-type body)))
      "" "<?xml version=\"1.0\"?><rss></rss>" "Missing content-type"
      "text/plain" "<?xml version=\"1.0\"?><rss></rss>" "Plain text content-type"
      "application/json" "<?xml version=\"1.0\"?><rss></rss>" "JSON content-type")))

(defn- html->enlive [html-str]
  (html/html-resource (java.io.StringReader. html-str)))

(deftest find-feed-url-from-anchor-test
  (testing "RSS link detection is case-insensitive"
    (are [anchor-text description]
         (= "/feed/"
            (#'sut/find-feed-url' "https://example.com"
                                  (html->enlive (format "<html><body><a href=\"/feed/\">%s</a></body></html>"
                                                        anchor-text))))
      "RSS" "uppercase RSS"
      "Rss" "title case Rss (carbonbrief style)"
      "rss" "lowercase rss"
      "RSS Feed" "RSS with extra text"
      "Our RSS" "RSS with prefix"
      "<i class=\"icon\"></i> Rss" "Rss with icon element (carbonbrief)"))

  (testing "Non-RSS links are not detected"
    (are [anchor-text description]
         (nil? (#'sut/find-feed-url' "https://example.com"
                                     (html->enlive (format "<html><body><a href=\"/other/\">%s</a></body></html>"
                                                           anchor-text))))
      "Subscribe" "subscribe link"
      "News" "news link"
      "R S S" "spaced out letters")))
