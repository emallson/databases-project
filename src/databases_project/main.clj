(ns databases-project.main
  (:require [databases-project.auctions :refer [update-realm!]]))

(def update-times (atom {}))

(defn -main
  [realm & rest]
  (while true
    (swap! update-times (partial update-realm! realm))
    (.sleep java.util.concurrent.TimeUnit/MINUTES 3)))
