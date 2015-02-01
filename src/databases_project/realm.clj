(ns databases-project.realm
  (:require [clj-http.client :as http]
            [korma.core :as korma :refer :all]
            [databases-project.entities :as ents]))

(defn get-realm-id
  "Given a realm name, returns its id."
  [name]
  (-> (select ents/realm
              (fields :RealmID)
              (where {:RName name})
              (limit 1))
      first :RealmID))

(defn realm-name->id
  [auction]
  (assoc auction :ownerRealm
         (get-realm-id (:ownerRealm auction))))
