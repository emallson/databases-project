(defproject databases-project "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [silasdavis/at-at "1.2.0"]
                 [compojure "1.1.9"]
                 [enlive "1.1.5"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [mysql/mysql-connector-java "5.1.25"]
                 [http-kit "2.1.16"]
                 [com.taoensso/timbre "3.3.1"]
                 [clj-time "0.5.1"]]
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler databases-project.handler/app}
  :aot :all
  :main databases-project.main
  :source-paths ["src"]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]}})
