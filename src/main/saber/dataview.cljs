(ns saber.dataview
  (:require [saber.obsidian :as obs]
            [applied-science.js-interop :as j]
            [clojure.string :as s]
            [datascript.core :as d]))


(def plugin (obs/plugin "dataview"))


(defn get-page
  [path]
  (.get (.. plugin -index -pages) path))


(defn page->datom
  [p]
  {:page/path (j/get p :path)
   :page/name (.name p)
   :page/frontmatter (js->clj (j/get p :frontmatter) :keywordize-keys true)})


(def db (d/create-conn
          {:page/path        {:db/unique :db.unique/identity}
           :page/aliases     {:db/valueType   :db.type/ref
                              :db/cardinality :db.cardinality/many}
           :page/ctime       {}
           :page/day         {}
           :page/fields      {}
           :page/frontmatter {}
           :page/links       {:db/valueType   :db.type/ref
                              :db/cardinality :db.cardinality/many}
           :page/lists       {:db/valueType   :db.type/ref
                              :db/cardinality :db.cardinality/many}
           :page/mtime       {}
           :page/name        {}
           :page/size        {}
           :page/tags        {:db/valueType :db.type/ref
                              :db/cardinality :db.cardinality/many}}))

(defn load
  []
  (let [pages (.. plugin -index -pages)
        pages (map second pages)
        data  (map page->datom pages)]
    (d/transact! db data)))


(comment (load))


(defn dv-update
  [type file old-path]
  (let [f (get-page (j/get file :path))
        datom (page->datom f)]
    (d/transact! db [datom])))



(comment
  (d/pull @db '[*] [:page/path "Journals/2023-11-22.md"])

  (d/transact! db [{:page/path "Journals/2023-11-22.md"
                    :page/name "2023-11-22"
                    :page/frontmatter {:bleep :bloop}}]))


(defn init []
  (.on obs/md "dataview:index-ready" #'load)
  (.on obs/md "dataview:metadata-change" #'dv-update))


(comment (init))


(comment

  (require '[portal-dev.core :as p])


  (d/pull @db '[*] [:page/path "Saber/init.cljs"])

  (d/q '[:find ?path
         :where
         [?p :page/path ?path]
         [?p :page/frontmatter ?fm]
         [(re-find #"Tasks/" ?path)]
         ;; [(get ?fm "class") ?class]
         ;; [(= ?class "Track")]
         ]
       @db)

  (s/includes? "string" "x")
  (re-find #"s" "saber"))

;; {:aliases #{}
;;   :closure_uid_336060418 54
;;   :ctime #DateTime "1700002197682"
;;   :day nil
;;   :fields {:class "Track"
;;            :priority nil
;;            :created #DateTime "2023-11-14"
;;            :link "https://security.microsoft.com/"}
;;   :frontmatter {:class "Track"
;;                 :priority nil
;;                 :created "2023-11-14"
;;                 :link "https://security.microsoft.com/"}
;;   :links []
;;   :lists []
;;   :mtime #DateTime "1700002201208"
;;   :path "Tasks/M$ Security.md"
;;   :size 90
;;   :tags #{}}



;;     "path": "Journals/2023-11-21.md",
;;     "fields": {},
;;     "frontmatter": {},
;;     "tags": {},
;;     "aliases": {},
;;     "links": [
;;         {
;;             "path": "Tasks/Tableau write-back lambda function.md",
;;             "type": "file",
;;             "display": "Tableau write-back lambda function",
;;             "embed": false
;;          }
;;     ],
;;     "lists":
;;         {
;;             "symbol": "-",
;;             "link": {
;;                 "path": "Journals/2023-11-21.md",
;;                 "type": "header",
;;                 "subpath": "Open"
;;             },
;;             "links": [],
;;             "section": {
;;                 "path": "Journals/2023-11-21.md",
;;                 "type": "header",
;;                 "subpath": "Open"
;;             },
;;             "text": "love Kimberly Marie Steele Ben-Haim soooooo much. ❤️❤️❤️💕♥️😘😍🥰",
;;             "tags": {},
;;             "line": 1,
;;             "lineCount": 1,
;;             "list": 1,
;;             "position": {
;;                 "start": {
;;                     "line": 1,
;;                     "col": 0,
;;                     "offset": 7
;;                 },
;;                 "end": {
;;                     "line": 1,
;;                     "col": 72,
;;                     "offset": 79
;;                 }
;;             },
;;             "children": [],
;;             "fields": {},
;;             "task": {
;;                 "status": "x",
;;                 "checked": true,
;;                 "completed": true,
;;                 "fullyCompleted": true
;;             }
;;         }
;; "ctime": "2023-11-20T16:34:22.959-06:00",
;;     "mtime": "2023-11-21T21:23:00.433-06:00",
;;     "size": 1417,
;;     "day": "2023-11-21T00:00:00.000-06:00"}

