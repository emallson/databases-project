(ns databases-project.main
  (:gen-class)
  (:require [databases-project.auctions :refer [update-realm!]]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]))

(def update-times (atom {}))


(defn -main
  [& realms]
  (defonce debug-server (start-server :port 3131))
  (while true
    (swap! update-times #(reduce update-realm! % realms))
    (.sleep java.util.concurrent.TimeUnit/MINUTES 3)))
