(ns databases-project.main
  (:require [databases-project.auctions :refer [update-realm!]]))

(defn -main
  [realm & rest]
  (update-realm! realm {}))
