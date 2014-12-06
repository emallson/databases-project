(ns databases-project.templates
  (:require [net.cgrand.enlive-html :refer :all]))

(deftemplate list "public/list.html" [headers contents])
