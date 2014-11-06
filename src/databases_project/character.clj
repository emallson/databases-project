(ns databases-project.character
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [databases-project.config :refer [api-key locale db-info]]
            [databases-project.macros :refer [defstmt]]))

(defstmt get-cached-character-by-name db-info
  "SELECT CName, Faction, RealmID FROM PCharacter WHERE CName = {owner}"
  :docstring "Pass in an auction object and this will return matching
  characters (either 0 or 1)."
  :query? true)

(defn get-character-info
  "Get list of files containing auction data for a realm."
  [realm pname]
  (http/get (str "https://us.api.battle.net/wow/character/" realm "/" pname)
            {:query-params {:apikey api-key,
                            :locale locale}}))
(defn get-character-att
  [realm pname patt]
  (-> @(get-character-info realm pname)
      :body json/read-str (get patt)))
