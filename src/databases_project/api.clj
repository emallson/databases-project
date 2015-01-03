(ns databases-project.api
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [databases-project.config :as config]
            [throttler.core :refer [throttle-fn]])
  (:refer-clojure :exclude [get]))

(defn- -get
  "Wrapper for getting results from the WoW API"
  [url]
  (http/get url {:query-params {:apikey config/api-key,
                                :locale config/locale}
                 :throw-exceptions false
                 :as :json}))

(def get (throttle-fn -get 36000 :hour 100))
