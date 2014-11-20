(ns databases-project.macros
  (:require [clojure.java.jdbc :as jdbc]))

(defmacro defstmt
  "Define a prepared statement with a name which acts like a function. Use
  {field-key} rather than ? in your SQL definition."
  [stmt-name db-info stmt-value & {:keys [query? docstring]
                                   :or {query? false, docstring ""}}]
  (let [stmt-parameters (map #(let [p (second %)]
                                (if (= (first p) \:)
                                  (keyword (apply str (rest p)))
                                  p))
                             (re-seq #"\{(:?\w+)\}" stmt-value))
        fn-parameters (map #(if (keyword? %)
                              (symbol (apply str (rest (str %))))
                              (symbol %)) stmt-parameters)
        stmt (clojure.string/replace stmt-value #"\{:?\w+\}" "?")
        db-fn (if query?
                `(jdbc/query
                  ~db-info [~'pstmt ~@fn-parameters])
                `(jdbc/db-do-prepared ~db-info true ~'pstmt [~@fn-parameters]))]
    `(let [~'pstmt (jdbc/prepare-statement (or (jdbc/db-find-connection ~db-info)
                                              (jdbc/get-connection ~db-info))
                                          ~stmt)]
       (defn ~stmt-name
         ~docstring
         [~(zipmap fn-parameters stmt-parameters)]
         ~db-fn))))
