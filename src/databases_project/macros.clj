(ns databases-project.macros
  (:require [clojure.java.jdbc :as jdbc]))

(defmacro defstmt
  "Define a prepared statement with a name which acts like a function. Use
  {field-name} rather than ? in your SQL definition."
  [stmt-name db-info stmt-value]
  (let [stmt-parameters (map #(let [p (second %)]
                                (if (= (first p) \:)
                                  (symbol p)
                                  p))
                             (re-seq #"\{(\w+)\}" stmt-value))
        stmt (clojure.string/replace stmt-value #"\{\w+\}" "?")]
    `(let [pstmt# (jdbc/prepare-statement (or (jdbc/db-find-connection ~db-info)
                                              (jdbc/get-connection ~db-info))
                                          ~stmt)]
       (defn ~stmt-name
         [~'param-map]
         (jdbc/db-do-prepared ~db-info true pstmt#
                              (reduce #(conj %1 (get ~'param-map %2)) [] [~@stmt-parameters]))))))
