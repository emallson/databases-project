;;; Stolen from: https://github.com/korma/Korma/issues/112
(ns jmglov.upsert
  (:require [korma.sql.engine :as korma-engine]
            [korma.core :refer :all]
            [robert.hooke :refer [add-hook]]))

(defn map->upsert-string
  [map]
  (clojure.string/join
   ", "
   (reduce (fn [l [k v]]
             (conj l (format "%s = %s"
                             (name k)
                             (if (keyword? v)
                               (format "VALUES(%s)" (name v))
                               v))))
           [] map)))

(defn upsert
  [f & [q :as args]]
  (let [result (apply f args)]
    (if-let [upsert-map (:upsert q)]
      (assoc result :sql-str
             (format "%s ON DUPLICATE KEY UPDATE %s"
                     (:sql-str result)
                     (map->upsert-string upsert-map)))
      result)))

(add-hook #'korma-engine/sql-insert #'upsert)

;; (defentity bar)

;; (defn upsert-bar! [id timestamp]
;;   (-> (insert* bar)
;;       (values [{:id id, :timestamp timestamp}])
;;       (assoc :upsert [:timestamp timestamp])
;;       (insert)))
