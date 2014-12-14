(ns databases-project.realm
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [databases-project.config :refer [api-key locale db-info]]
            [databases-project.macros :refer [defstmt]]))

(defn get-realm-basic
  "Get list of files containing auction data for a realm."
  []
  (http/get (str "https://us.api.battle.net/wow/realm/status")
            {:query-params {:apikey api-key,
                            :locale locale}}))

(defn get-realms
  []
  (map-indexed (fn [id body] {:id id, :name (get body "name")})
               (-> @(get-realm-basic)
                   :body json/read-str
                   (get "realms"))))

(defstmt insert-realm db-info
  "INSERT INTO Realm (RealmID, RName) VALUES ({:id}, {:name});")

(defn insert-all-realms [] (map insert-realm (get-realms)))

(defstmt get-realm db-info
  "SELECT RealmID, RName FROM Realm WHERE RName = {:name}"
  :query? true)

(defstmt get-realms-with-data db-info
  "SELECT RealmID, RName FROM Realm
   NATURAL JOIN (SELECT DISTINCT RealmID FROM Listing) AS UniqueRealmIDs;"
  :query? true
  :docstring "Produces a list of realms which have listings in the database.")

(defstmt get-counts db-info
  "SELECT COUNT(ListID) AS NumListings, COUNT(DISTINCT CName) AS NumCharacters FROM Listing
   NATURAL JOIN Realm
   WHERE Active = 1 AND RName = {realm};"
  :query? true
  :docstring "Get the number of listings and number of characters who posted them.")

(defstmt get-top-auctioneers-listings db-info
  "SELECT CName, COUNT(ListID) AS ListCount FROM Listing
   NATURAL JOIN Realm
   WHERE RName = {realm} AND Active = 1
   GROUP BY CName
   ORDER BY ListCount DESC
   LIMIT {count};"
  :query? true
  :docstring "Gets the top N auctioneers by the number of listings.")

(defstmt get-top-auctioneers-value db-info
  "SELECT CName, SUM(BuyPrice) AS ListValue FROM Listing
   NATURAL JOIN Realm
   WHERE RName = {realm} AND Active = 1
   GROUP BY CName
   ORDER BY ListValue DESC
   LIMIT {count};"
   :query? true
   :docstring "Gets the top N auctioneers by the buyout value of their auctions.")

(defn realm-name->id
  "Replaces the given field on the object `data` with the id of the realm name
  stored in the field."
  ([src-field data]
     (realm-name->id src-field "realmID" data))
  ([src-field dest-field data]
     (let [realm-name (get data src-field)]
       (-> data
           (dissoc src-field)
           (assoc dest-field (-> (get-realm {:name realm-name})
                                 first :realmid))))))
