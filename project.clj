(defproject com.github.julienba/super-rss "0.1.5"
  :description "A library to read RSS feed"
  :url "http://github.com/julienba/super-rss"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.cache "1.0.225"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.slf4j/slf4j-log4j12 "1.7.36"]
                 [log4j/log4j "1.2.17"]
                 [clojure.java-time "0.3.3"]
                 [enlive "1.1.6"]
                 [remus "0.2.2"]
                 [org.clj-commons/hickory "0.7.4"]]
  :repl-options {:init-ns super-rss.core}
  :repositories [["releases" {:url "https://repo.clojars.org"
                              :creds :gpg}]])
