(ns databases-project.realm
  (:require [clj-http.client :as http]
            [korma.core :as korma :refer :all]
            [databases-project.entities :as ents]))

(defn realm-name->id
  "Given a realm name, returns its id."
  [name]
  (-> (select ents/realm
              (fields :RealmID)
              (where {:RName name}))
    first :RealmID))
