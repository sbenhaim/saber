(ns saber.utils
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [promesa.core :as p]))

(defn jsify [clj-thing]
  (clj->js clj-thing))

(defn cljify [js-thing]
  (js->clj js-thing :keywordize-keys true))

(defn info [& xs]
  (js/console.log "INFO:" (str/join " " (mapv str xs))))

(defn warn [& xs]
  (js/console.log "WARN:" (str/join " " (mapv str xs))))

(defn error [& xs]
  (js/console.log "ERROR:" (str/join " " (mapv str xs))))
