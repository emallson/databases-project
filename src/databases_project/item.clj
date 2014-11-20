(ns databases-project.item
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [databases-project.config :refer [api-key locale db-info]]
            [databases-project.macros :refer [defstmt]]))

(defstmt insert-item db-info
  "INSERT INTO Item (ItemID, MaxStack, IName)
             VALUES ({id}, {stackable}, {name});"
  :docstring "Insert an item received from the B.net API.")

(defstmt get-cached-item db-info
  "SELECT ItemID, MaxStack, IName FROM Item
   WHERE ItemID = {item};"
  :docstring "Get an item by its ID. Pass in a auction data object."
  :query? true)

(defn get-item-info
  ([item-id]
     (http/get (str "https://us.api.battle.net/wow/item/" item-id)
               {:query-params {:apikey api-key,
                               :locale locale}})))

(defn get-new-item-data
  [auction-data]
  (->> auction-data
       (filter #(empty? (get-cached-item %)))
       (map #(get-item-info (get % "item")))
       (map #(-> @% :body json/read-str))))
