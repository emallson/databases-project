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

(defn insert-all-realms
  []
  (map insert-realm (get-realms)))
