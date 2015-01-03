(ns databases-project.scraper
  (:require [clojure.core.async :as async :refer [go <! >!]]
            [databases-project.entities :refer :all]
            [clj-http.client :as http]
            [taoensso.timbre :as timbre]
            [korma.core :refer :all]))
