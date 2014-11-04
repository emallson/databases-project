(ns databases-project.realm
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [databases-project.config :refer [api-key locale db-info]]))


(defn get-realm-basic
  "Get list of files containing auction data for a realm."
  []
  (http/get (str "https://us.api.battle.net/wow/realm/status")
            {:query-params {:apikey api-key,
                            :locale locale}}))
(defn get-realm-list
  [iterat]
  
	(-> @(get-realm-basic)
		:body json/read-str (get-in ["realms" iterat "name"])))

(defn get-realms
	(for [i (range 10)] (inc i))
		(get-realm-list i))


