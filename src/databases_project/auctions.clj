(ns databases-project.auctions
  (:require [clojure.core.async :as async :refer [go go-loop <! >!]]
            [taoensso.timbre :as timbre]
            [databases-project.entities :as ents]
            [databases-project.api :as api]
            [korma.core :as korma :refer :all]
            [clj-http.client :as http]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [databases-project.realm :refer [realm-name->id]]
            [databases-project.character :refer [update-characters!]]
            [databases-project.item :refer [update-items! acontext->context]]
            [jmglov.upsert]))

;; (ns databases-project.auctions
;;   (:require [org.httpkit.client :as http]
;;             [clojure.data.json :as json]
;;             [clojure.java.jdbc :as jdbc]
;;             [taoensso.timbre :as timbre]
;;             [clj-time.core :as time]
;;             [clj-time.format :as tf]
;;             [clj-time.coerce :as tc]
;;             [databases-project.config :refer [api-key locale db-info]]
;;             [databases-project.macros :refer [defstmt]]
;;             [databases-project.realm :refer [realm-name->id get-realm]]
;;             [databases-project.character :refer [update-characters!]]
;;             [databases-project.item :refer [get-cached-item update-items!]]))

;; (defn get-auction-files-for
;;   "Get list of files containing auction data for a realm."
;;   [realm]
;;   (timbre/info (str "Fetching files for " realm))
;;   (http/get (str "https://us.api.battle.net/wow/auction/data/" realm)
;;             {:query-params {:apikey api-key,
;;                             :locale locale}}))

(def auction-files-url
  (partial format "https://us.api.battle.net/wow/auction/data/%s"))

(defn get-auction-files-for
  "Get list of files containing auction data for a realm"
  [realm-name]
  (timbre/infof "Fetching files for %s" realm-name)
  (let [{status :status, body :body} (api/get (auction-files-url realm-name))]
    (condp = status
      200 (:files body))))

;; (defn get-updated-files-for
;;   [realm update-time]
;;   (seq (filter #(> (get % "lastModified") update-time)
;;                (-> @(get-auction-files-for realm)
;;                    :body json/read-str (get "files")))))

(defn get-updated-files-for
  [realm-name update-time]
  (seq (filter #(> (:lastModified %) update-time)
               (get-auction-files-for realm-name))))

;; (defn get-auction-data-from
;;   [file-list]
;;   (timbre/info (str "Aggregating auctions from files..."))
;;   (try
;;     (->> file-list
;;          (map (fn [{url "url", post-date "lastModified"}]
;;                 (map
;;                  #(assoc % "postDate" post-date)
;;                  (-> @(http/get url)
;;                      :body json/read-str
;;                      (get "auctions")
;;                      (get "auctions")))))
;;          (reduce into []))
;;     (catch Exception e (timbre/errorf "Files are moving! Try again soon..."))))

(defn get-auction-data-from
  [file-list]
  (timbre/infof "Aggregating auctions from files...")
  (try
    (->> file-list
         (map (fn [{url :url, post-date :lastModified}]
                (map
                 #(assoc % :postDate post-date)
                 (-> (http/get url {:throw-exceptions false
                                    :as :json}) :body :auctions :auctions))))
         (reduce into []))
    (catch Exception e (timbre/errorf e "Files are moving! Try again soon..."))))

;;; Honestly, I have no idea what I'm doing
(defn fetch-auctions
  [file-list]
  (let [c (async/chan)]
    (go-loop [{url :url, post-date :lastModified} (first file-list)
              remaining (rest file-list)]
      (try
        (async/onto-chan c (map #(assoc % :postDate post-date)
                                (-> (http/get url) :body :auctions :auctions)))
        (catch Exception e (timbre/errorf "Files are moving! Try again soon...")))
      (if-not (empty? remaining)
        (recur (first remaining) (rest remaining))
        (async/close! c)))
    c))

(def time-left-ids (zipmap ["SHORT" "MEDIUM" "LONG" "VERY_LONG"] (range 4)))
;; (defn time-left->id
;;   [key auction]
;;   (assoc auction "timeLeft"
;;          (time-left-ids (get auction key))))

(defn time-left->id
  [time-left]
  (time-left-ids time-left))

;; (defn post-date->fmt
;;   [key auction]
;;   (assoc auction "postDate"
;;          (tf/unparse (tf/formatters :mysql) (tc/from-long (get auction key)))))

(defn post-date->fmt
  [auction]
  (tf/unparse (tf/formatters :mysql) (tc/from-long (:postDate auction))))

;; (def filter-???
;;   "Filters all auctions with ??? as their realm. These are auctions posted by
;;   deleted characters."
;;   (partial filter #(not= (get % "ownerRealm") "???")))

(def filter-???
  (partial filter #(not= (:ownerRealm %) "???")))

;; (defn buyprice->buyprice-per-item
;;   "Adds the buyoutPerItem field to an auction."
;;   [key auction]
;;   (assoc auction "buyoutPerItem"
;;          (/ (get auction "buyout")
;;             (get auction "quantity"))))

;; (defstmt insert-auction db-info
;;   "INSERT INTO Listing (ListID, Quantity, BuyPricePerItem, OriginalBidPrice, BidPrice, StartLength, TimeLeft, PostDate, CName, RealmID, ItemID, Context, AContext, Active)
;;                 VALUES ({auc}, {quantity}, {buyoutPerItem}, {bid}, {bid}, {timeLeft}, {timeLeft}, {postDate}, {owner}, {realmID}, {item}, {context}, {acontext}, 1)
;;                 ON DUPLICATE KEY UPDATE
;;                    BidPrice = VALUES(BidPrice),
;;                    TimeLeft = VALUES(TimeLeft),
;;                    Active = 1;")

(defn insert-auctions!
  [auctions]
  (-> (insert* ents/listing)
      (values auctions)
      (assoc :upsert {:BidPrice :BidPrice
                      :TimeLeft :TimeLeft
                      :Active 1})
      (insert)))

;; (defstmt deactivate-auctions! db-info
;;   "UPDATE Listing SET Active = 0 WHERE RealmID = {:realmid};"
;;   :docstring "Mark all auctions for a realm inactive. Auctions will be reactivated afterwards if they are still up.")

(defn deactivate-auctions!
  "Mark all auctions for a realm inactive. Auctions will be reactivated afterwards if they are still up."
  [realm-id]
  (update ents/listing
          (set-fields {:active false})
          (where {:realmid realm-id})))

;; (defn update-auctions!
;;   [auction-data]
;;   (apply insert-auction (map #(timbre/spy (->> %
;;                                                (realm-name->id "ownerRealm")
;;                                                (time-left->id "timeLeft")
;;                                                (post-date->fmt "postDate")
;;                                                (acontext->context))) auction-data)))

(defn reformat-auction
  [auction]
  (let [time-left (time-left->id (:timeLeft auction))]
    {:ListID (:auc auction)
     :Quantity (:quantity auction)
     :BuyPrice (:buyout auction)
     :BuyPricePerItem (/ (:buyout auction)
                         (:quantity auction))
     :OriginalBidPrice (:bid auction)
     :BidPrice (:bid auction)
     :StartLength time-left
     :TimeLeft time-left
     :PostDate (post-date->fmt (:postDate auction))
     :CName (:owner auction)
     :RealmID (realm-name->id (:ownerRealm auction))
     :ItemID (:item auction)
     :AContext (:context auction)
     :Active true}))

(defn update-auctions!
  [auction-data]
  (insert-auctions!
   (map #(timbre/spy (reformat-auction %)) auction-data)))

(defn update-realm!
  "Checks to see if a realm needs updating and, if so, updates it."
  [update-times realm]
  (if-let [file-list (get-updated-files-for realm (get update-times realm 0))]
    (let [last-update (apply max (map :lastModified file-list)),
          auction-data (filter-??? (get-auction-data-from file-list))]
      (if-not (empty? auction-data)
        (do
          (timbre/infof "File List: %s" file-list)
          (timbre/infof "Beginning update...")
          ;; this is bad and i feel bad about it
          ;; abusing <!! to block until the operation completes
          (let [char-chan (update-characters! auction-data)
                item-chan (update-items! auction-data)]
            (async/<!! char-chan)
            (async/<!! item-chan))
          (timbre/spy
           (deactivate-auctions! (realm-name->id realm)))
          (update-auctions! auction-data)
          (timbre/infof "Done updating %s" realm)
          (assoc update-times realm
                 (max last-update (get update-times realm 0))))
        (do (timbre/infof "Unable to load auctions for %s" realm)
            update-times)))
    (do
      (timbre/infof "No new files for %s" realm)
      update-times)))

;; (defn update-realm!
;;   [update-times realm]
;;   (if-let [file-list (get-updated-files-for realm (get update-times realm 0))]
;;     (let [last-update (apply max (map :lastModified file-list))
;;           auction-data-chan (get-auction-data-from file-list)]
;;       (go-loop [auction (<! auction-data-chan)]
;;         (cond
;;           (= (:ownerRealm auction) "???") (recur (<! auction-data-chan))
;;           (not (nil? auction)) )))))
