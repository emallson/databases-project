(ns databases-project.handler
  (:require [clojure.data.json :as json]
            [ring.util.response :refer [redirect]]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [databases-project.extensions]
            [databases-project.auctions :as auctions]
            [databases-project.character :as character]
            [databases-project.realm :as realm]
            [databases-project.item :as item]
            [databases-project.templates :as templates]))

(defn page-start
  "Computes the starting record of a page (given size 100)."
  [n]
  (* (- n 1) 100))

(defn sql-time
  "Converts a Joda Time object to a string suitable for MySQL."
  [t]
  (tf/unparse (tf/formatters :mysql) t))

(defn prev-period
  "Creates a MySQL-ready time that is one `p' prior to `t`."
  [t p]
  (sql-time (time/minus t (p 1))))

(defroutes app-routes
  (GET "/realm/:realm/items/:page" [realm page]
    (let [items (item/list-active-with-prices {"start" (page-start (Integer/parseInt page)), "realm" realm})]
      (templates/item-list realm [] items)))

  (GET "/realm/:realm" [realm]
    (templates/realm-overview
     realm
     (first (realm/get-counts {"realm" realm}))
     (first (realm/get-top-auctioneers-listings {"realm" realm, "count" 1}))
     (first (realm/get-top-auctioneers-value {"realm" realm, "count" 1}))))

  ;; item details
  (GET "/realm/:realm/item/:item-id" [realm item-id]
    (let [item (item/get-item-stats {"item" item-id,
                                     "realm" realm,
                                     "count" 200})]
      (templates/item-details
       realm
       item
       (item/get-buyout-over-time
        {"item" item-id, "realm" realm,
         "start" (prev-period (time/now) time/weeks), "end" (sql-time (time/now))})
       (item/get-auctions-for-item {"item" item-id}))))

  ;; deal-finding
  (GET "/realm/:realm/deals/:page" [realm page]
    (when-let [deals (item/get-deals {"realm" realm,
                                      "ratio" 0.5,
                                      "start" (page-start (Integer/parseInt page))})]
      (templates/realm-deals realm deals)))

  (GET "/realm/:realm/characters/:page" [realm page]
    (let [characters (map (partial character/id->Race-name :race)
                          (character/get-characters {"start" (page-start (Integer/parseInt page))}))]
         (templates/character-list realm [] characters)))

  (GET "/home" []
    (let [realms (realm/get-realms-with-data {})]
      (templates/home realms)))
  (GET "/" []
    (redirect "/home"))

  (route/resources "/resources")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
