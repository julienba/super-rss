(defproject com.github.julienba/super-rss "0.1.0"
  :description "A library to read RSS feed"
  :url "http://github.com/julienba/super-rss"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/core.cache "1.0.207"]
                 [org.clojure/tools.logging "1.0.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.29"]
                 [log4j/log4j "1.2.17"]
                 [clojure.java-time "0.3.2"]
                 [enlive "1.1.6"]
                 [remus "0.2.1"]]
  :repl-options {:init-ns super-rss.core})
