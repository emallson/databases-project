(ns databases-project.main
  (:require [databases-project.auctions :refer [update-realm!]]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]))

(def update-times (atom {}))

(defonce debug-server (start-server :port 3131))

(defn -main
  [realm & rest]
  (while true
    (swap! update-times (partial update-realm! realm))
    (.sleep java.util.concurrent.TimeUnit/MINUTES 3)))
