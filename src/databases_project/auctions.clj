(ns databases-project.auctions
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [databases-project.config :refer [api-key locale db-info]]
            [databases-project.macros :refer [defstmt]]))

(defn get-auction-files-for
  "Get list of files containing auction data for a realm."
  [realm]
  (http/get (str "https://us.api.battle.net/wow/auction/data/" realm)
            {:query-params {:apikey api-key,
                            :locale locale}}))

(defn get-auction-data-from
  [url]
  (-> @(http/get url)
      :body json/read-str (get "auctions")))

(defstmt insert-auction db-info
  "INSERT INTO Listing (ListID, Quantity, BuyPrice, BidPrice, StartLength, TimeLeft, PostDate, CName, RealmID, ItemID)
                VALUES ({auc}, {quantity}, {buyout}, {bid}, 0, 0, 0, {owner}, 0, {item});")

(defn update-realm!
  "Checks to see if a realm needs updating and, if so, updates it."
  [realm update-times]
  (let [file-list
        (-> @(get-auction-files-for realm)
            :body json/read-str (get "files")),
        last-update (apply max (map #(get % "lastModified") file-list))]
    (->> file-list
         (filter #(> (get % "lastModified") (get update-times realm)))
         (map #(get-auction-data-from (get % "url")))
         (map #(map insert-auction %)))
    (assoc update-times realm
           (max last-update (get update-times realm)))))
