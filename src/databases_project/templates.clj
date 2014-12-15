(ns databases-project.templates
  (:require [net.cgrand.enlive-html :refer :all]
            [clojure.data.json :as json]
            [clojure.set :refer [map-invert]]
            [clojure.string :refer [capitalize]]
            [databases-project.auctions :refer [time-left-ids]]
            [databases-project.character :refer [Race-ids]]
            [databases-project.macros :refer [defstmt]]
            [databases-project.config :refer [api-key locale db-info]]))

(defsnippet header-base "public/header-base.html" [:head] [])

(defsnippet navbar "public/navbar.html" [:nav] [realm]
  [:#realm-link] (do->
                  (set-attr :href (format "/realm/%s" realm))
                  (remove-attr :id)
                  (content (format "%s Overview" realm)))
  [:#items-link] (do->
                  (set-attr :href (format "/realm/%s/items/1" realm))
                  (remove-attr :id))
  [:#deals-link] (do->
                  (set-attr :href (format "/realm/%s/deals/1" realm))
                  (remove-attr :id))
  [:#chars-link] (do->
                  (set-attr :href (format "/realm/%s/characters/1" realm))
                  (remove-attr :id)))

(defsnippet pretty-price "public/pretty-price.html" [:.price :> :span] [price]
  [:.gold] (prepend (-> price
                     (/ 10000) int str))
  [:.silver] (prepend (-> price
                          (/ 100) (mod 100) int str))
  [:.copper] (prepend (-> price
                          (mod 100) int str)))

(defn pretty-time-left
  [time-left]
  "Converts a time-left id to a pretty string."
  (-> time-left
      ((map-invert time-left-ids))
      (clojure.string/replace #"_" " ")
      (capitalize)))

(defsnippet item-link "public/item-link.html" [:a] [realm item]
  [:a] (do->
        (set-attr :href (format "/realm/%s/item/%d" realm (get item :itemid)))
        (set-attr :rel (format "item=%d" (get item :itemid)))
        (content (str "[" (get item :iname) "]"))))

(defsnippet character-link "public/item-link.html" [:a] [realm character]
  [:a] (do->
        (set-attr :href (format "/realm/%s/character/%s" realm (get character :cname)))
        (content (get character :cname))))

(defsnippet wowhead-link "public/wowhead-link.html" [:a] [item]
  [:a] (do->
        (set-attr :href (format "https://wowhead.com/item=%d" (get item :itemid)))
        (set-attr :rel (format "item=%d" (get item :itemid)))
        (content (str "[" (get item :iname) "]"))))

(defsnippet list-item "public/list-item.html" [:tr] [realm item]
  [:.name] (content (item-link realm item))
  [:.stack-size] (content (str (get item :maxstack)))
  [:.min-buyout] (content (pretty-price (get item :minbuyprice)))
  [:.med-buyout] (content (pretty-price (get item :avgbuyprice))))

(defsnippet get-auctions-for-item "public/item-auction-data.html" [:tr] [realm item]
  [:.IName] (content (str (get item :iname)))
  [:.Quant] (html-content (get item :quantity))
  [:.BidPr] (content (pretty-price (get item :bidprice)))
  [:.BuyPr] (content (pretty-price (get item :buyprice)))
  [:.BuyPrItem] (content (pretty-price (get item :buypriceperitem)))
  [:.CName] (content (character-link realm item))
  [:.TimeLeft] (content (pretty-time-left (get item :timeleft))))

(deftemplate item-list "public/list.html" [realm headers contents]
  [:head] (append (header-base))
  [:div.navbar] (substitute (navbar realm))
  [:.table :tbody] (clone-for [el contents] (content (list-item realm el))))

(defsnippet realm-link "public/realm-link.html" [:li] [realm]
  [:a] (do->
        (set-attr :href (format "/realm/%s" (:rname realm)))
        (content (:rname realm))))

(deftemplate home "public/home.html" [realms]
  [:head] (append (header-base))
  ;; todo: add homepage navbar
;;  [:div.navbar] (substitute (navbar realm))
  [:ul#realm-list] (clone-for [realm realms] (content (realm-link realm))))

(defsnippet character-overview-panel "public/character-overview-panel.html" [:.panel] [overview]
  [:#name] (do->
            (remove-attr :id)
            (content (get overview :cname)))

  [:#race] (do->
            (remove-attr :id)
            (content (get Race-ids (get overview :race))))

  [:#realm] (do->
             (remove-attr :id)
             (content (get overview :rname)))

  [:#num-listings] (do->
                    (remove-attr :id)
                    (content (str (get overview :numlistings))))

  [:#valuation] (do->
                 (remove-attr :id)
                 (content (pretty-price (get overview :valuation)))))

(deftemplate character "public/character.html" [realm character overview-data]
  [:head] (append (header-base))
  [:title] (content (format "%s-%s" character realm))
  [:.navbar] (substitute (navbar realm))
  [:#overview-panel] (do->
                      (add-class "col-md-4")
                      (content (character-overview-panel overview-data))))

(defsnippet character-row "public/get-character.html" [:tr] [realm character]
  [:.Character] (content (character-link realm character))
  [:.Race] (html-content (str (get character :race)))
  [:.Realm] (html-content (str (get character :rname))))

(deftemplate character-list "public/characters.html" [realm headers contents]
  [:head] (append (header-base))
  [:div.navbar] (substitute (navbar realm))
  [:.table :tbody] (clone-for [el contents] (content (character-row realm el))))

(deftemplate item-details "public/item.html" [realm item prices auction]
  [:head] (append (header-base))
  [:div.navbar] (substitute (navbar realm))
  [:#item-name] (content (wowhead-link item))
  [:#min-buyout] (append (pretty-price (get item :minbuyprice)))
  [:#mean-buyout] (append (pretty-price (get item :avgbuyprice)))
  [:#chart-price-time-line] (set-attr "data-prices" (json/write-str prices))
  [:#IBody] (clone-for [el auction] (content (get-auctions-for-item realm el))))

(defsnippet deal-row "public/deal-row.html" [:tr] [realm deal]
  [:.name] (content (item-link realm deal))
  [:.quantity] (content (str (get deal :quantity)))
  [:.price] (content (pretty-price (get deal :buyperitem)))
  [:.market-price] (content (pretty-price (get deal :avgbuyprice)))
  [:.ratio] (content (str (* (get deal :priceratio) 100))))

(defsnippet realm-info-panel "public/realm-info-panel.html" [:dl] [realm counts top-listings top-value]
  [:#num-auctioneers] (prepend (str (:numcharacters counts)))
  [:#num-listings] (prepend (str (:numlistings counts)))
  [:#top-auctioneer-listings] (content (character-link realm top-listings) " (" (str (:listcount top-listings)) ")")
  [:#top-auctioneer-value] (content (character-link realm top-value) " (" (pretty-price (:listvalue top-value)) ")"))

(deftemplate realm-overview "public/realm-overview.html" [realm counts top-listings top-value]
  [:head] (append (header-base))
  [:div.navbar] (substitute (navbar realm))
  [:#info-panel :.panel-body] (content (realm-info-panel realm counts top-listings top-value)))

(deftemplate realm-deals "public/deals.html" [realm deals]
  [:head] (append (header-base))
  [:title] (append realm)
  [:div.navbar] (substitute (navbar realm))
  [:.table :tbody] (clone-for [deal deals] (content (deal-row realm deal))))
