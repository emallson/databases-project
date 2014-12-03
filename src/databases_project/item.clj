(ns databases-project.item
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as timbre]
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

(defn get-contextual-items
  "Fetches all context-dependent items listed in a pseudo-item object. If the
  object is not a pseudo-item, returns a vector holding the object."
  [item-object]
  (let [id (get item-object "id")
        contexts (get item-object "availableContexts")
        no-contexts (or
                     (nil? contexts)
                     (empty? (first contexts)))]
    (if no-contexts
      [item-object]
      (map
       #(-> @(http/get (str "https://us.api.battle.net/wow/item/" id "/" %)
                       {:query-params {:apikey api-key,
                                       :locale locale}})
            :body json/read-str)
       contexts))))

(defn get-new-item-data
  [auction-data]
  (let [new-items (->> auction-data
                       (map #(get % "item"))
                       distinct
                       (filter #(empty? (get-cached-item {"item" %}))))]
    (timbre/info (str (count new-items) " new items"))
    (->> new-items
         (map #(get-item-info %))
         (map #(-> @% :body json/read-str))
         (filter #(not (= (get % "status") "nok")))
         (map get-contextual-items)
         (reduce into []))))
