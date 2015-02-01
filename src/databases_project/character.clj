(ns databases-project.character
  (:require [clojure.core.async :as async :refer [go go-loop <! >!]]
            [taoensso.timbre :as timbre]
            [databases-project.entities :as ents]
            [databases-project.api :as api]
            [korma.core :as korma :refer :all]
            [clj-http.client :as http]
            [databases-project.realm :refer [realm-name->id]]))

(def race-ids
  {1 "Human"
   2 "Orc"
   3 "Dwarf"
   4 "Night Elf"
   5 "Undead"
   6 "Tauren"
   7 "Gnome"
   8 "Troll"
   9 "Goblin"
   10 "Blood Elf"
   11 "Draenei"
   22 "Worgen"
   25 "Alliance Pandaren"
   26 "Horde Pandaren"
   -1 "Scrublord"})

(defn reformat-character
  "Reformats a character from the value given by the API to a value that can be
  given to Korma."
  [character]
  {:CName (character :name)
   :Race (character :race)
   :RealmID (realm-name->id (character :realm))})

(def character-url
  "Produces a url for the character"
  (partial format "https://us.api.battle.net/wow/character/%s/%s"))

(defn get-character
  [realm-name char-name]
  (let [{status :status, body :body} (api/get (character-url realm-name char-name))]
    (condp = status
        200 (reformat-character body)
        404 (reformat-character {:name char-name, :realm realm-name, :race -1}))))

(defn get-cached-character
  [realm-name char-name]
  (first (select ents/character
                 (with ents/realm)
                 (where {:Realm.RName realm-name,
                         :CName char-name})
                 (limit 1))))

(defn insert-character!
  [character]
  (timbre/debugf "Attempting insertion of %s" character)
  (try
    (insert ents/character
            (values character))

    (catch java.sql.SQLException e ; bad form to just skip like this, but we've
                                   ; had too many problems with it for me to
                                   ; care
      (timbre/debugf e "Skipping insertion of %s" character))))

(def without-existing
  (partial filter (fn [{realm :realm, name :name}]
                    (nil? (get-cached-character realm name)))))

(defn fetch-characters
  [characters]
  (let [c (async/chan (async/buffer (count characters))),
        info-chan (async/onto-chan (async/chan) characters false)]
    (go-loop [{realm :realm, name :name, :as character} (<! info-chan)]
      (when-not (nil? character)
        (try
          (>! c (get-character realm name))
          (catch IllegalArgumentException e  ; get failed with transient error
            (>! info-chan character)))
        (recur (<! info-chan))))
    c))

(defn insert-characters!
  [channel]
  (go-loop [c channel]
    (when-let [character (<! c)]
      (insert-character! character)
      (recur c))))

(defn auctions->character-info
  [auctions]
  (map (fn [{name :owner, realm :ownerRealm}]
         {:name name, :realm realm}) auctions))

(defn update-characters!
  [auctions]
  (-> auctions
    auctions->character-info
    distinct
    without-existing
    fetch-characters
    insert-characters!))

;; (defstmt get-characters db-info
;;   "SELECT CName, Race, RName FROM PCharacter
;;   NATURAL JOIN Realm
;;   WHERE RName = {realm}
;;     AND CName IN (SELECT CName FROM Listing WHERE Active = 1 AND Listing.RealmID = Realm.RealmID)
;;   ORDER BY CName
;;   LIMIT {start}, 100;"
;;   :query? true)

;; (defstmt get-character-listings db-info
;;   "SELECT IName, Quantity, TimeLeft, BidPrice, BuyPricePerItem,
;;           BuyPricePerItem * Quantity AS BuyPrice
;;   FROM Listing
;;   NATURAL JOIN Item
;;   NATURAL JOIN Realm
;;   NATURAL JOIN PCharacter
;;   WHERE CName = {cname} AND RName = {realm} AND Active = 1;"
;;   :query? true
;;   :docstring "Returns a character's current auctions")

;; (defstmt get-character-overview db-info
;;   "SELECT CName, Race, RName,
;;           COUNT(ListID) AS NumListings,
;;           SUM(BuyPricePerItem * Quantity) AS Valuation
;;    FROM Listing
;;    NATURAL JOIN Realm
;;    NATURAL JOIN PCharacter
;;    WHERE CName = {cname} AND RName = {realm} AND Active = 1;"
;;   :query? true
;;   :docstring "Gets values for the character overview panel.")

;; (defn get-new-character-data
;;   [auction-data]
;;   (let [unique-characters (->> auction-data
;;                                (map #(select-keys % ["owner" "ownerRealm"]))
;;                                distinct)
;;         new-characters (filter #(empty? (get-cached-character %)) unique-characters)]
;;     (timbre/infof "%d new characters" (count new-characters))
;;     (->> new-characters
;;          (map character-info-or-scrublord)
;;          (map #(get-realm-id "realm" %)))))

;; (defn update-characters!
;;   [auction-data]
;;   (doseq [character (get-new-character-data auction-data)]
;;     (timbre/debugf "Inserting: %s" character)
;;     (insert-character character)))
