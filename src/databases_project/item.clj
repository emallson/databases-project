(ns databases-project.item
  (:require [clojure.core.async :as async :refer [go go-loop <! >!]]
            [taoensso.timbre :as timbre]
            [databases-project.entities :as ents]
            [databases-project.api :as api]
            [korma.core :as korma :refer :all]
            [clj-http.client :as http]
            [databases-project.realm :refer [realm-name->id]]))

;; (defstmt insert-item db-info
;;   "INSERT INTO Item (ItemID, Context, MaxStack, IName)
;;              VALUES ({id}, {context}, {stackable}, {name});"
;;   :docstring "Insert an item received from the B.net API.")

(def item-contexts-url (partial format "https://us.api.battle.net/wow/item/%d"))
(def item-url (partial format "https://us.api.battle.net/wow/item/%d/%s"))

(def unobtainium-template
  {:ItemID 0,
   :Context "",
   :IName "Unobtainium",
   :MaxStack 0})

(defn reformat-item
  [api-item]
  {:IName (:name api-item)
   :MaxStack (:maxCount api-item)
   :Context (:context api-item)
   :ItemID (:id api-item)})

(defn get-item-contexts
  ([item-id]
   (let [{status :status, body :body} (api/get (item-contexts-url item-id))]
     (condp = status
       200 (:availableContexts body)
       404 nil))))

(defn get-item
  ([item-id context]
   (let [{status :status, body :body} (api/get ((if (= context "")
                                                  item-contexts-url
                                                  item-url)
                                                item-id context))]
     (reformat-item body))))

(defn get-cached-item
  [item-id context]
  (first (select ents/item
                 (where {:ItemID item-id,
                         :Context context})
                 (limit 1))))

(defn get-items
  ([item-id]
   (if-let [contexts (get-item-contexts item-id)]
     (map (partial get-item item-id) contexts)
     (assoc unobtainium-template
            :ItemID item-id))))

(defn get-cached-items
  [item-id]
  (select ents/item
          (where {:ItemID item-id})))

(defn insert-item!
  [item]
  (timbre/debugf "Attempting insertion of %s" item)
  (insert ents/item
          (values item)))

(def without-existing
  (partial filter (fn [{item-id :id}]
                    (empty? (get-cached-items item-id)))))

(defn fetch-items
  [items]
  (let [c (async/chan (async/buffer (count items)))]
    (go (doseq [{item-id :id} items]
          (>! c (get-items item-id)))
        (async/close! c))
    c))

(defn insert-items!
  [channel]
  (go-loop [c channel]
    (when-let [item (<! c)]
      (insert-item! item)
      (recur c))))

(def context-numbers?
  {3 "raid-normal",
   5 "raid-heroic",
   6 "raid-mythic",
   13 "trade-skill"})

(defn acontext->context
  "Translates the context field on an auction into a value matching the foreign
  key to Item. This is done with a best guess and may not be 100% accurate."
  [{item-id :item, acontext :context, :as auction}]
  (if-let [contexts (->> (get-cached-items item-id)
                      (map :Context)
                      (into #{}))]
    (cond
      (= (count contexts) 1) (first contexts)

      (and (contains? context-numbers? acontext)
           (contains? contexts (context-numbers? acontext))) (context-numbers? acontext)

      :else (first contexts))))

(defn auctions->item-info
  [auctions]
  (map (fn [{item-id :item, acontext :context}]
         {:id item-id}) auctions))

(defn update-items!
  [auctions]
  (-> auctions
    auctions->item-info
    distinct
    without-existing
    fetch-items
    insert-items!))

;; (defstmt list-active-with-prices db-info
;;   "SELECT ItemID, IName, Context, MaxStack, MIN(BuyPricePerItem) AS MinBuyPrice, AVG(BuyPricePerItem) AS AvgBuyPrice
;;    FROM Listing
;;    NATURAL JOIN Item
;;    WHERE RealmID = (SELECT RealmID FROM Realm WHERE RName = {realm})
;;      AND Active = 1
;;    GROUP BY IName, Context
;;    LIMIT {start}, 100;"
;;   :docstring "Get an item list with normalized prices."
;;   :query? true)

;; (defstmt -get-item-stats db-info
;;   "SELECT ItemID, IName,
;;        AVG(BuyPricePerItem) AS AvgBuyPrice,
;;        MIN(BuyPricePerItem) AS MinBuyPrice
;;    FROM Listing
;;    NATURAL JOIN Item
;;    NATURAL JOIN Realm
;;    WHERE ItemID = {item} and RName = {realm}
;;      AND BuyPricePerItem > 0
;;    ORDER BY PostDate DESC
;;    LIMIT {count};"
;;   :query? true)

;; (defn get-item-stats
;;   [m]
;;   (let [item (first (-get-item-stats m))]
;;     (when (not-any? nil? (vals item))
;;       item)))

;; (defstmt get-auctions-for-item db-info
;;   "SELECT IName, Quantity, BidPrice, BuyPricePerItem * Quantity AS BuyPrice,
;;           BuyPricePerItem, CName, TimeLeft
;;    FROM Listing
;;    NATURAL JOIN Item
;;    WHERE ItemID = {item}
;;      AND RealmID = (SELECT RealmID FROM Realm WHERE RName = {realm})
;;      AND Active = 1 AND BuyPricePerItem > 0
;;    ORDER BY BuyPricePerItem ASC
;;    LIMIT 200;"
;;   :docstring "Get the first 200 listings of an item and sort by buyout per item."
;;   :query? true)

;; (defstmt get-buyout-over-time db-info
;;   "SELECT AVG(BuyPricePerItem) AS AvgBuyPrice,
;;           MIN(BuyPricePerItem) MinBuyPrice,
;;           PostDate
;;    FROM Listing
;;    NATURAL JOIN Realm
;;    WHERE ItemID = {item} AND RName = {realm}
;;      AND PostDate >= {start} AND PostDate <= {end}
;;      AND BuyPricePerItem > 0
;;    GROUP BY HOUR(PostDate)
;;    ORDER BY PostDate ASC;"
;;   :docstring "Collects hourly Min and Mean data for an item on a realm."
;;   :query? true)

;; (defstmt get-deals db-info
;;   "SELECT IName,
;;           Listing.ItemID,
;;           Quantity,
;;           BuyPricePerItem,
;;           AvgBuyPrice,
;;           BuyPricePerItem / AvgBuyPrice AS PriceRatio
;;    FROM Listing
;;    NATURAL JOIN Item
;;    INNER JOIN (SELECT ItemID, AVG(BuyPricePerItem) AS AvgBuyPrice
;;                FROM Listing
;;                NATURAL JOIN Realm
;;                WHERE RName = {realm} AND BuyPricePerItem > 0
;;                GROUP BY ItemID, RealmID)
;;          AS MarketValues
;;          ON (MarketValues.ItemID = Listing.ItemID
;;              AND BuyPricePerItem <= AvgBuyPrice * {ratio})
;;    WHERE RealmID = (SELECT RealmID FROM Realm WHERE RName = {realm}) AND Active = 1 AND BuyPricePerItem > 0
;;    ORDER BY PriceRatio ASC
;;    LIMIT {start},100;"
;;   :docstring "Gets auctions which are RATIO * market value or below."
;;   :query? true)

;; (defstmt max-pages db-info
;;   "SELECT COUNT(*)/100 FROM LISTING;"
;;   :docstring "Gets total number of listings divided by 100"
;;   :query? "true")
