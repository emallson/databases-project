(require '[clojure.java.jdbc :as j])

(def mysql-db {	:subprotocol "mysql"
		:subname "test"
               	:user "root"
               	:password "root"})

(j/insert! mysql-db :Character
  {:PName "ibprofin" :Realm "korgath" :Faction 1}
  {:PName "claybourne" :Realm "korgath" :Faction 1})
;; ({:generated_key 1} {:generated_key 2})

(j/query mysql-db
  ["select * from PCharacter]
  :row-fn :cost)
;; (24)
