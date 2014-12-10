(ns databases-project.templates
  (:require [net.cgrand.enlive-html :refer :all]
            [databases-project.macros :refer [defstmt]]
            [databases-project.config :refer [api-key locale db-info]]))

(defn pretty-price [value]
  (str
   (int (/ value 10000)) "<span class=\"gold\">g</span> "
   (int (mod (/ value 100) 100)) "<span class=\"silver\">s</span> "
   (int (mod value 100)) "<span class=\"copper\">c</span>"))

(defsnippet wowhead-link "public/wowhead-link.html" [:a] [item]
  [:a] (set-attr :href (format "http://www.wowhead.com/item=%d" (get item :itemid)))
  [:a] (content (str "[" (get item :iname) "]")))

(defsnippet list-item "public/list-item.html" [:tr] [item]
  [:.name] (content (wowhead-link item))
  [:.stack-size] (html-content (str (get item :maxstack)))
  [:.min-buyout] (html-content (pretty-price (get item :minbuyprice)))
  [:.med-buyout] (html-content (pretty-price (get item :avgbuyprice))))

(deftemplate item-list "public/list.html" [headers contents]
  [:.table :tbody] (clone-for [el contents] (content (list-item el))))

(defstmt get-player-listings db-info
    "SELECT * FROM Listings
    NATURAL JOIN Realm
    WHERE CName = {seller} and RName = {sellerRealm} and Active = 1;"
    :docstring "Return all listings a player has currently."
    :query? true)

(defstmt get-player-items db-info
    "SELECT * FROM Listings
    NATURAL JOIN Realm
    WHERE CName = {Seller} and RName = {sellerRealm}
    GROUP BY ItemID, Context;"
    :docstring "Returns a list of items a player has put up for auction."
    :query? true)

(defstmt get-item-listings db-info
    "SELECT * FROM Listings
    NATURAL JOIN Realm
    WHERE ItemID = {item} and RName = {itemRealm} and Active = 1;"
    :docstring "Returns all current auctions for an item."
    :query? true)

(defstmt get-item-stats db-info
    "SELECT AVG(BuyPrice), MIN(BuyPrice), IName FROM Listings
    NATURAL JOIN Item, Realm
    WHERE ItemId = {item} and RName = {itemRealm} and PostDate >= {queryDate};"
    :query? true)

(defstmt get-item-plots db-info
    "SELECT AVG(BuyPrice), MIN(BuyPrice), IName FROM Listings
    NATURAL JOIN Item
    NATURAL JOIN Realm
    WHERE ItemId = {item} and RName = {itemRealm} and PostDate >= {queryDate}
    GROUP BY PostDate;"
    :query? true)

(defstmt get-item-deals db-info
    "SELECT * FROM Listings
    NATURAL JOIN Item
    NATURAL JOIN Realm
    WHERE ItemId = {item} and RName = {itemRealm} and Active = 1 and BuyPrice = MIN(BuyPrice);"
    :docstring "Finds all items currently under the minimum buyprice."
    :query? true)


