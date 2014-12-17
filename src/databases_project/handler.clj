(ns databases-project.handler
  (:require [clojure.data.json :as json]
            [ring.util.response :refer [redirect]]
            [taoensso.timbre.profiling :refer [profile]]
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
  "Creates a MySQL-ready time that is one `p` prior to `t`."
  [t p]
  (sql-time (time/minus t (p 1))))

(defroutes realm-routes
  (context "/realm/:realm" [realm]

    (GET "/" []
      (templates/realm-overview
       realm
       (first (realm/get-counts {"realm" realm}))
       (first (realm/get-top-auctioneers-listings {"realm" realm, "count" 1}))
       (first (realm/get-top-auctioneers-value {"realm" realm, "count" 1}))))

    (GET "/items/:page" [page]
      (let [items (item/list-active-with-prices {"start" (page-start (Integer/parseInt page)), "realm" realm})]
        (templates/item-list realm [] items page item/max-pages)))

    ;; item details
    (GET "/item/:item-id" [item-id]
      (let [item (item/get-item-stats {"item" item-id,
                                       "realm" realm,
                                       "count" 200})]
        (templates/item-details
         realm
         item
         (item/get-buyout-over-time
          {"item" item-id, "realm" realm,
           "start" (prev-period (time/now) time/weeks), "end" (sql-time (time/now))})
         (item/get-auctions-for-item {"item" item-id, "realm" realm}))))

    ;; deal-finding
    (GET "/deals/:page" [page]
      (when-let [deals (item/get-deals {"realm" realm,
                                        "ratio" 0.5,
                                        "start" (page-start (Integer/parseInt page))})]
        (templates/realm-deals realm deals)))

    (GET "/characters/:page" [page]
      (let [characters (map (partial character/id->Race-name :race)
                            (character/get-characters {"realm" realm,
                                                       "start" (page-start (Integer/parseInt page))}))]
        (templates/character-list realm [] characters page)))

    (GET "/character/:name" [name]
      (let [overview-data (first (character/get-character-overview {"realm" realm,
  "cname" name})) listing-data (character/get-character-listings{    "realm" realm,
  "cname" name}) ]
        (templates/character realm name overview-data listing-data)))))

(defroutes app-routes
  realm-routes
  (GET "/home" []
    (let [realms (realm/get-realms-with-data {})]
      (templates/home realms)))
  (GET "/" []
    (redirect "/home"))

  (route/resources "/resources")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
