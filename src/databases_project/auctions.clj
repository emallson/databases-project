(ns databases-project.auctions
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]))

(def api-key "")
(def locale "en_US")

(defn get-auction-files-for
  "Get list of files containing auction data for a realm."
  [realm]
  (http/get (str "https://us.api.battle.net/wow/auction/data/" realm)
            {:query-params {:apikey api-key,
                            :locale locale}}))

(defn update-realm
  "Checks to see if a realm needs updating and, if so, updates it."
  [realm update-times]
  (doseq [file (-> @(get-auction-files-for realm)
                   :body json/read-str (get "files"))]
    (println file)))
