(ns databases-project.auctions
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as timbre]
            [databases-project.config :refer [api-key locale db-info]]
            [databases-project.macros :refer [defstmt]]
            [databases-project.realm :refer [realm-name->id]]
            [databases-project.character :refer [update-characters!]]
            [databases-project.item :refer [update-items!]]))

(defn get-auction-files-for
  "Get list of files containing auction data for a realm."
  [realm]
  (timbre/info (str "Fetching files for " realm))
  (http/get (str "https://us.api.battle.net/wow/auction/data/" realm)
            {:query-params {:apikey api-key,
                            :locale locale}}))

(defn get-updated-files-for
  [realm update-time]
  (seq (filter #(> (get % "lastModified") update-time)
               (-> @(get-auction-files-for realm)
                   :body json/read-str (get "files")))))

(defn get-auction-data-from
  [file-list]
  (timbre/info (str "Aggregating auctions from files..."))
  (->> file-list
       (map #(get % "url"))
       (map (fn [url]
              (-> @(http/get url)
                  :body json/read-str
                  (get "auctions")
                  (get "auctions"))))
       (reduce into [])))

(defstmt insert-auction db-info
  "INSERT INTO Listing (ListID, Quantity, BuyPrice, BidPrice, StartLength, TimeLeft, PostDate, CName, RealmID, ItemID)
                VALUES ({auc}, {quantity}, {buyout}, {bid}, 0, 0, 0, {owner}, {realmID}, {item});")

(defn update-auctions!
  [auction-data]
  (doseq [auction auction-data]
    (->> auction
         (realm-name->id "ownerRealm")
         insert-auction)))

(defn update-realm!
  "Checks to see if a realm needs updating and, if so, updates it."
  [realm update-times]
  (if-let [file-list (get-updated-files-for realm (get update-times realm 0))]
    (let [last-update (apply max (map #(get % "lastModified") file-list)),
          auction-data (get-auction-data-from file-list)]
      (update-characters! auction-data)
      (update-items! auction-data)
      (update-auctions! auction-data)
      (assoc update-times realm
             (max last-update (get update-times realm 0))))
    update-times))
