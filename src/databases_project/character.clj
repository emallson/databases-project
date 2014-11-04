(ns databases-project.character
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [databases-project.config :refer [api-key locale db-info]]))


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

