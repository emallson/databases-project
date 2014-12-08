(ns databases-project.character
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as timbre]
            [databases-project.config :refer [api-key locale db-info]]
            [databases-project.macros :refer [defstmt]]
            [databases-project.realm :refer [realm-name->id]]))

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
