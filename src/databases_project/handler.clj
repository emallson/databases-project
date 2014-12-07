(ns databases-project.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [overtone.at-at :refer [mk-pool every after]]
            [taoensso.timbre :as timbre]
            [databases-project.auctions :as auctions]
            [databases-project.item :as item]
            [databases-project.templates :as templates]))

(defroutes app-routes
  (GET "/items/:page" [page]
    (let [items (item/list-active-with-prices {"start" (* (- (Integer/parseInt page) 1) 100)})]
      (templates/item-list [] items)))
  (route/not-found "Not Found"))

(timbre/set-level! :info)

(def fetch-pool (mk-pool))
(def update-times (atom {}))

(defn update-loop []
  (swap! update-times (partial auctions/update-realm! "Korgath"))
  (after 180000 update-loop fetch-pool))

(def app
  (handler/site app-routes))

(after 10000 update-loop fetch-pool)
