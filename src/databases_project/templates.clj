(ns databases-project.templates
  (:require [net.cgrand.enlive-html :refer :all]))

(defn pretty-price [value]
  (str
   (int (/ value 10000)) "<span class=\"gold\">g</span> "
   (int (mod (/ value 100) 100)) "<span class=\"silver\">s</span> "
   (int (mod value 100)) "<span class=\"copper\">c</span>"))

(defsnippet wowhead-link "public/wowhead-link.html" [:a] [item]
  [:a] (set-attr :href (format "http://www.wowhead.com/item=%d" (get item :itemid)))
  [:a] (content (str "[" (get item :iname) "]")))

(defsnippet list-item "public/list-item.html" [:tr] [item]
  [:.name] (content (wowhead-link item))
  [:.stack-size] (html-content (str (get item :maxstack)))
  [:.min-buyout] (html-content (pretty-price (get item :minbuyprice)))
  [:.med-buyout] (html-content (pretty-price (get item :avgbuyprice))))

(deftemplate item-list "public/list.html" [headers contents]
  [:.table :tbody] (clone-for [el contents] (content (list-item el))))
