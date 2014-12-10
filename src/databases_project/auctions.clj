(ns databases-project.auctions
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as timbre]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [databases-project.config :refer [api-key locale db-info]]
            [databases-project.macros :refer [defstmt]]
            [databases-project.realm :refer [realm-name->id get-realm]]
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
  (try
    (->> file-list
         (map (fn [{url "url", post-date "lastModified"}]
                (map
                 #(assoc % "postDate" post-date)
                 (-> @(http/get url)
                     :body json/read-str
                     (get "auctions")
                     (get "auctions")))))
         (reduce into []))
    (catch Exception e (timbre/errorf "Files are moving! Try again soon..."))))

(def time-left-ids (zipmap ["SHORT" "MEDIUM" "LONG" "VERY_LONG"] (range 4)))
(defn time-left->id
  [key auction]
  (assoc auction "timeLeft"
         (time-left-ids (get auction key))))

(defn post-date->fmt
  [key auction]
  (assoc auction "postDate"
         (tf/unparse (tf/formatters :mysql) (tc/from-long (get auction key)))))

(defstmt insert-auction db-info
  "INSERT INTO Listing (ListID, Quantity, BuyPrice, OriginalBidPrice, BidPrice, StartLength, TimeLeft, PostDate, CName, RealmID, ItemID, AContext, Active)
                VALUES ({auc}, {quantity}, {buyout}, {bid}, {bid}, {timeLeft}, {timeLeft}, {postDate}, {owner}, {realmID}, {item}, {context}, 1)
                ON DUPLICATE KEY UPDATE
                   BidPrice = VALUES(BidPrice),
                   TimeLeft = VALUES(TimeLeft),
                   Active = 1;")

(defstmt deactivate-auctions! db-info
  "UPDATE Listing SET Active = 0 WHERE RealmID = {realmid};"
  :docstring "Mark all auctions for a realm inactive. Auctions will be reactivated afterwards if they are still up.")

(defn update-auctions!
  [auction-data]
  (apply insert-auction (map #(timbre/spy (->> %
                                               (realm-name->id "ownerRealm")
                                               (time-left->id "timeLeft")
                                               (post-date->fmt "postDate"))) auction-data)))

(defn update-realm!
  "Checks to see if a realm needs updating and, if so, updates it."
  [update-times realm]
  (if-let [file-list (get-updated-files-for realm (get update-times realm 0))]
    (let [last-update (apply max (map #(get % "lastModified") file-list)),
          auction-data (get-auction-data-from file-list)]
      (if-not (empty? auction-data)
        (do
          (timbre/infof "File List: %s" file-list)
          (timbre/infof "Beginning update...")
          (update-characters! auction-data)
          (update-items! auction-data)
          (deactivate-auctions! {"realmid" (get-realm {:name realm})})
          (update-auctions! auction-data)
          (timbre/infof "Done updating %s" realm)
          (assoc update-times realm
                 (max last-update (get update-times realm 0))))
        (do (timbre/infof "Unable to load auctions for %s" realm)
            update-times)))
    (do
      (timbre/infof "No new files for %s" realm)
      update-times)))
