(ns saber.utils
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [promesa.core :as p]))

(defn jsify [clj-thing]
  (clj->js clj-thing))

(defn cljify [js-thing]
  (js->clj js-thing :keywordize-keys true))

#_(defn path-or-uri-exists?+ [path-or-uri]
  (-> (p/let [uri (if (= (type "") (type path-or-uri))
                    (vscode/Uri.file path-or-uri)
                    path-or-uri)
              _stat (vscode/workspace.fs.stat uri)])
      (p/handle
       (fn [_r, e]
         (if e
           false
           true)))))

#_(defn vscode-read-uri+ [^js uri-or-path]
  (let [uri (if (string? uri-or-path)
              (vscode/Uri.file uri-or-path)
              uri-or-path)]
    (-> (p/let [_ (vscode/workspace.fs.stat uri)
                data (vscode/workspace.fs.readFile uri)
                decoder (js/TextDecoder. "utf-8")
                code (.decode decoder data)]
          code))))

#_(defn vault-root []
  vscode/workspace.rootPath)

(defn info [& xs]
  (js/console.log "INFO:" (str/join " " (mapv str xs))))

(defn warn [& xs]
  (js/console.log "WARN:" (str/join " " (mapv str xs))))

(defn error [& xs]
  (js/console.log "ERROR:" (str/join " " (mapv str xs))))
