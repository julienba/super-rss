(defproject com.github.julienba/super-rss "0.1.6-SNAPSHOT"
  :description "A library to read RSS feed"
  :url "http://github.com/julienba/super-rss"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.cache "1.1.234"]
                 [org.clojure/tools.logging "1.3.0"]
                 [org.slf4j/slf4j-log4j12 "2.0.17"]
                 [log4j/log4j "1.2.17"]
                 [clojure.java-time "1.4.3"]
                 [enlive "1.1.6" :exclusions [org.jsoup/jsoup]]
                 [remus "0.2.2"]
                 [org.clj-commons/hickory "0.7.7"]]
  :repl-options {:init-ns super-rss.core}
  :repositories [["releases" {:url "https://repo.clojars.org"
                              :creds :gpg}]]

  :profiles {:test {:plugins [[lein-cloverage "1.2.2"]]
                    :resource-paths ["test/resources"]
                    :source-paths ["test"]}})
