(ns databases-project.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [databases-project.auctions :as auctions]
            [databases-project.character :as character]
            [databases-project.item :as item]
            [databases-project.templates :as templates]))

(defn prev-day [t]
  (tf/unparse (tf/formatters :mysql)
              (time/minus t (time/days 1))))

(defroutes app-routes
  (GET "/items/:page" [page]
    (let [items (item/list-active-with-prices {"start" (* (- (Integer/parseInt page) 1) 100)})]
      (templates/item-list [] items)))
  (GET "/realm/:realm/item/:item-id" [realm item-id]
    (when-let [item (item/get-item-stats {"item" item-id,
                                     "realm" realm,
                                     "queryDate" (prev-day (time/now))})]
      (templates/item-details item)))
  (GET "/character/:page" [page]
       (let [characters (map (partial character/id->Race-name :race) (character/get-characters {"start"(*(-(Integer/parseInt page) 1) 100)}))]
         (templates/character-list [] characters)))
  (route/resources "/resources")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
