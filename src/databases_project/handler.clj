(ns databases-project.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [databases-project.auctions :as auctions]
            [databases-project.character :as character]
            [databases-project.item :as item]
            [databases-project.templates :as templates]))

(defroutes app-routes
  (GET "/items/:page" [page]
    (let [items (item/list-active-with-prices {"start" (* (- (Integer/parseInt page) 1) 100)})]
      (templates/item-list [] items)))
  (GET "/character/:page" [page]
       (let [characters (character/get-characters {"start"(*(-(Integer/parseInt page) 1) 100)})]
         (templates/character-list [] characters)))
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
