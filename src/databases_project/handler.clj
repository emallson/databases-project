(ns databases-project.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [databases-project.item :as item]
            [databases-project.templates :as templates]))

(defroutes app-routes
  (GET "/items" []
    (let [items (item/list-with-prices {})]
      (timbre/debug items)
      (templates/item-list [] items)))
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
