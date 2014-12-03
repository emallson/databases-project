(ns databases-project.character
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
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
  (let [response @(get-character-info realm pname)
        char-info (-> response :body json/read-str)]
    (if (= (get char-info "status") "nok")
      {"realm" realm, "name" pname, "race" -1}
      char-info)))

(defn get-new-character-data
  [auction-data]
  (->> auction-data
       (map #(select-keys % ["owner" "ownerRealm"]))
       distinct
       (filter #(empty? (get-cached-character %)))
       (map character-info-or-scrublord)
       (map #(realm-name->id "realm" %))))
