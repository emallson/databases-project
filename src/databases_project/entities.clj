(ns databases-project.entities
  (:require [korma.core :refer :all]
            [korma.db :refer :all]))

(declare realm character item listing)

(defdb local-db (mysql {:db "wow-ah"
                        :user "root"
                        :password ""}))

(defentity realm
  (pk :RealmID)
  (table :Realm)
  (has-many character)
  (has-many listing))

(defentity character
  (pk :CName :RealmID)
  (table :PCharacter)
  (belongs-to realm)
  (has-many listing))

(defentity item
  (pk :ItemID :Context)
  (table :Item)
  (has-many listing))

(defentity listing
  (pk :ListID)
  (table :Listing)
  (belongs-to character)
  (belongs-to realm)
  (belongs-to item))
