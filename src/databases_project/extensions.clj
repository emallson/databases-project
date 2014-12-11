(ns databases-project.extensions
  (:require [clojure.data.json :as json]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]))

(extend-type java.sql.Timestamp
  json/JSONWriter
  (-write [time out]
    (.print out (str "\"" (tf/unparse (tf/formatters :rfc822)
                                      (tc/from-sql-time time)) "\""))))
