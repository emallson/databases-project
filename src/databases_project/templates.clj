(ns databases-project.templates
  (:require [net.cgrand.enlive-html :refer :all]
            [clojure.data.json :as json]
            [databases-project.macros :refer [defstmt]]
            [databases-project.config :refer [api-key locale db-info]]))

(defsnippet header-base "public/header-base.html" [:head] [])

(defsnippet pretty-price "public/pretty-price.html" [:.price :> :span] [price]
  [:.gold] (prepend (-> price
                     (/ 10000) int str))
  [:.silver] (prepend (-> price
                          (/ 100) (mod 100) int str))
  [:.copper] (prepend (-> price
                          (mod 100) int str)))

(defsnippet wowhead-link "public/wowhead-link.html" [:a] [item]
  [:a] (do->
        (set-attr :href (format "http://www.wowhead.com/item=%d" (get item :itemid)))
        (content (str "[" (get item :iname) "]"))))

(defsnippet list-item "public/list-item.html" [:tr] [item]
  [:.name] (content (wowhead-link item))
  [:.stack-size] (content (str (get item :maxstack)))
  [:.min-buyout] (content (pretty-price (get item :minbuyprice)))
  [:.med-buyout] (content (pretty-price (get item :avgbuyprice))))

(deftemplate item-list "public/list.html" [headers contents]
  [:head] (append (header-base))
  [:.table :tbody] (clone-for [el contents] (content (list-item el))))

(defsnippet get-characters "public/get-character.html" [:tr] [character]
  [:.Character] (html-content (str (get character :cname)))
  [:.Race] (html-content (str (get character :race)))
  [:.Realm] (html-content (str (get character :rname))))

(deftemplate character-list "public/characters.html" [headers contents]
  [:.table :tbody] (clone-for [el contents] (content(get-characters el))))

(deftemplate item-details "public/item.html" [item prices]
  [:head] (append (header-base))
  [:#item-name] (content (wowhead-link item))
  [:#min-buyout] (append (pretty-price (get item :minbuyprice)))
  [:#mean-buyout] (append (pretty-price (get item :avgbuyprice)))
  [:#chart-price-time-line] (set-attr "data-prices" (json/write-str prices)))

(defsnippet deal-row "public/deal-row.html" [:tr] [deal]
  [:.name] (content (wowhead-link deal))
  [:.quantity] (content (str (get deal :quantity)))
  [:.price] (content (pretty-price (get deal :buyperitem)))
  [:.market-price] (content (pretty-price (get deal :avgbuyprice)))
  [:.ratio] (content (str (get deal :priceratio))))

(deftemplate realm-deals "public/deals.html" [realm deals]
  [:head] (append (header-base))
  [:title] (append realm)
  [:.table :tbody] (clone-for [deal deals] (content (deal-row deal))))

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

(defstmt get-item-plots db-info
    "SELECT AVG(BuyPrice), MIN(BuyPrice), IName FROM Listings
    NATURAL JOIN Item
    NATURAL JOIN Realm
    WHERE ItemId = {item} and RName = {itemRealm} and PostDate >= {queryDate}
    GROUP BY PostDate;"
    :query? true)
