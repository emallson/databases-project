(ns databases-project.character
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as timbre]
            [databases-project.config :refer [api-key locale db-info]]
            [databases-project.macros :refer [defstmt]]
            [databases-project.realm :refer [realm-name->id]]))

(def Race-ids
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
   -1 "Scrublord"
   })
(defn id->Race-name
  [key character]
  (assoc character :race
      (Race-ids (get character key))))

(defstmt insert-character db-info
  "INSERT INTO PCharacter (CName, Race, RealmID) VALUES ({name}, {race}, {realmID});"
  :docstring "Inserts a character into the database. Need to transform RName -> RealmID prior to insertion.")

(defstmt get-cached-character db-info
  "SELECT CName, Race, RealmID FROM PCharacter
   NATURAL JOIN Realm
   WHERE CName = {owner} AND RName = {ownerRealm};"
  :docstring "Pass in an auction object and this will return matching
  characters (either 0 or 1). Transforming RName -> RealmID is not necessary."
  :query? true)

(defstmt get-characters db-info
  "SELECT CName, Race, RName FROM PCharacter
  NATURAL JOIN Realm ORDER BY CName LIMIT {start}, 100;"
  :query? true)

(defstmt get-character-listings db-info
  "SELECT IName, Quantity, TimeLeft, BidPrice, BuyPricePerItem, BuyPrice
  FROM Listing
  NATURAL JOIN Item
  NATURAL JOIN Realm
  NATURAL JOIN PCharacter
  WHERE CName = {cname} AND RName = {realm} AND Active = 1;"
  :query? true
  :docstring "Returns a character's current auctions")
(defstmt get-character-overview db-info
  "SELECT CName, Race, RName,
          COUNT(ListID) AS NumListings,
          SUM(BuyPrice) AS Valuation
   FROM Listing
   NATURAL JOIN Realm
   NATURAL JOIN PCharacter
   WHERE CName = {cname} AND RName = {realm} AND Active = 1;"
  :query? true
  :docstring "Gets values for the character overview panel.")

(defn get-character-info
  "Get character info from the B.net API"
  ([{realm "ownerRealm", pname "owner"}]
     (get-character-info realm pname))
  ([realm pname]
     (http/get (str "https://us.api.battle.net/wow/character/" realm "/" pname)
               {:query-params {:apikey api-key,
                               :locale locale}})))

(defn character-info-or-scrublord
  "Fetches character data (already deref'd) or returns a map with race -1 to
  indicate scrublord."
  [{realm "ownerRealm", pname "owner"}]
  (timbre/debug (str "trying: " pname "-" realm))
  (let [response @(get-character-info realm pname)
        char-info (-> response :body json/read-str)]
    (timbre/debug char-info)
    (cond
     (empty? char-info) {"realm" realm, "name" pname, "race" -2} ; missingno
     (= (get char-info "status") "nok") {"realm" realm, "name" pname, "race" -1} ; scrublord
     (not= (get char-info "name") pname) (assoc char-info "name" pname) ; character has been renamed but auction still has old name
     :else char-info)))

(defn get-new-character-data
  [auction-data]
  (let [unique-characters (->> auction-data
                               (map #(select-keys % ["owner" "ownerRealm"]))
                               distinct)
        new-characters (filter #(empty? (get-cached-character %)) unique-characters)]
    (timbre/infof "%d new characters" (count new-characters))
    (->> new-characters
         (map character-info-or-scrublord)
         (map #(realm-name->id "realm" %)))))

(defn update-characters!
  [auction-data]
  (doseq [character (get-new-character-data auction-data)]
    (timbre/debugf "Inserting: %s" character)
    (insert-character character)))
