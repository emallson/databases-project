(ns databases-project.item
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as timbre]
            [databases-project.config :refer [api-key locale db-info]]
            [databases-project.macros :refer [defstmt]]))

(defstmt insert-item db-info
  "INSERT INTO Item (ItemID, Context, MaxStack, IName)
             VALUES ({id}, {context}, {stackable}, {name});"
  :docstring "Insert an item received from the B.net API.")

(defstmt get-cached-item db-info
  "SELECT ItemID, Context, MaxStack, IName FROM Item
   WHERE ItemID = {item};"
  :docstring "Get an item by its ID. Pass in a auction data object. Returns the
  Item in each available context."
  :query? true)

(defstmt list-active-with-prices db-info
  "SELECT ItemID, IName, Context, MaxStack, MIN(BuyPricePerItem) AS MinBuyPrice, AVG(BuyPricePerItem) AS AvgBuyPrice
   FROM Listing
   NATURAL JOIN Item
   WHERE RealmID = (SELECT RealmID FROM Realm WHERE RName = {realm})
     AND Active = 1
   GROUP BY IName, Context
   LIMIT {start}, 100;"
  :docstring "Get an item list with normalized prices."
  :query? true)

(defstmt -get-item-stats db-info
  "SELECT ItemID, IName,
       AVG(BuyPricePerItem) AS AvgBuyPrice,
       MIN(BuyPricePerItem) AS MinBuyPrice
   FROM Listing
   NATURAL JOIN Item
   NATURAL JOIN Realm
   WHERE ItemID = {item} and RName = {realm}
     AND BuyPricePerItem > 0
   ORDER BY PostDate DESC
   LIMIT {count};"
  :query? true)

(defn get-item-stats
  [m]
  (let [item (first (-get-item-stats m))]
    (when (not-any? nil? (vals item))
      item)))

(defstmt get-auctions-for-item db-info
  "SELECT IName, Quantity, BidPrice, BuyPricePerItem * Quantity AS BuyPrice,
          BuyPricePerItem, CName, TimeLeft
   FROM Listing
   NATURAL JOIN Item
   WHERE ItemID = {item}
     AND RealmID = (SELECT RealmID FROM Realm WHERE RName = {realm})
     AND Active = 1 AND BuyPricePerItem > 0
   ORDER BY BuyPricePerItem ASC
   LIMIT 200;"
  :docstring "Get the first 200 listings of an item and sort by buyout per item."
  :query? true)

(defstmt get-buyout-over-time db-info
  "SELECT AVG(BuyPricePerItem) AS AvgBuyPrice,
          MIN(BuyPricePerItem) MinBuyPrice,
          PostDate
   FROM Listing
   NATURAL JOIN Realm
   WHERE ItemID = {item} AND RName = {realm}
     AND PostDate >= {start} AND PostDate <= {end}
     AND BuyPricePerItem > 0
   GROUP BY HOUR(PostDate)
   ORDER BY PostDate ASC;"
  :docstring "Collects hourly Min and Mean data for an item on a realm."
  :query? true)

(defstmt get-deals db-info
  "SELECT IName,
          Listing.ItemID,
          Quantity,
          BuyPricePerItem,
          AvgBuyPrice,
          BuyPricePerItem / AvgBuyPrice AS PriceRatio
   FROM Listing
   NATURAL JOIN Item
   INNER JOIN (SELECT ItemID, AVG(BuyPricePerItem) AS AvgBuyPrice
               FROM Listing
               NATURAL JOIN Realm
               WHERE RName = {realm} AND BuyPricePerItem > 0
               GROUP BY ItemID, RealmID)
         AS MarketValues
         ON (MarketValues.ItemID = Listing.ItemID
             AND BuyPricePerItem <= AvgBuyPrice * {ratio})
   WHERE RealmID = (SELECT RealmID FROM Realm WHERE RName = {realm}) AND Active = 1 AND BuyPricePerItem > 0
   ORDER BY PriceRatio ASC
   LIMIT {start},100;"
  :docstring "Gets auctions which are RATIO * market value or below."
  :query? true)

(defn get-item-info
  ([item-id]
     (http/get (str "https://us.api.battle.net/wow/item/" item-id)
               {:query-params {:apikey api-key,
                               :locale locale}})))

(defn get-contextual-items
  "Fetches all context-dependent items listed in a pseudo-item object. If the
  object is not a pseudo-item, returns a vector holding the object.

  Note that right now we have NO IDEA how to relate Auction.AContext to Item.Context"
  [item-object]
  (let [id (get item-object "id")
        contexts (get item-object "availableContexts")
        no-contexts (or
                     (nil? contexts)
                     (empty? (first contexts)))]
    (if no-contexts
      ; note: looks like misplaced fn arglist, but is intended: vector containing single object
      [item-object]
      (map
       #(-> @(http/get (str "https://us.api.battle.net/wow/item/" id "/" %)
                       {:query-params {:apikey api-key,
                                       :locale locale}})
            :body json/read-str)
       contexts))))

(def unobtainium-template
  {"id" 0,
   "context" "",
   "name" "Unobtainium",
   "stackable" 0})

(defn get-item-or-unobtainium
  "Total hack to get around non-existent (no longer available) items."
  [item-id]
  (let [response @(get-item-info item-id)
        item-info (-> response :body json/read-str)]
    (cond
     (= (get item-info "status") "nok") (assoc unobtainium-template
                                          "id" item-id) ; unobtainable item
     :else item-info)))

(defn get-new-item-data
  "Returns a LIST OF LISTS of items."
  [auction-data]
  (let [new-items (->> auction-data
                       (map #(get % "item"))
                       distinct
                       (filter #(empty? (get-cached-item {"item" %}))))]
    (timbre/info (str (count new-items) " new items"))
    (->> new-items
         (map #(get-item-or-unobtainium %))
         (map get-contextual-items))))

(defn update-items!
  [auction-data]
  (doseq [item-group (get-new-item-data auction-data)]
    (doseq [item item-group]
      (timbre/debugf "Inserting: %s" item)
      (insert-item item))))
(defstmt max-pages db-info
  "SELECT COUNT(*)/100 FROM LISTING;"
  :docstring "Gets total number of listings divided by 100"
  :query? "true")
