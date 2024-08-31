(ns saber.query
  (:require [saber.obsidian :as obs]
            [applied-science.js-interop :as j]
            [clojure.string :as s]
            [clojure.pprint :refer [pprint]]
            [datascript.core :as d]
            [rum.core :as rum]))


(def plugin (obs/plugin "dataview"))


(defn get-page
  [path]
  (.get (.. plugin -index -pages) path))


(comment
  (js/console.log
    (obs/resolve-link "Pages/Karla.md" "Journals/2024-03-22.md"))
  (js/console.log
    (get-page "Journals/2023-08-30.md")))


(defn page-path
  [p]
  (j/get p :path))


(defn page-name
  [p]
  (let [n (j/get p :name)]
    (if (string? n) n
        (.name p))))


(comment
  (page-name (get-page "Journals/2023-08-30.md")))


(defn page->datom
  [p]
  (let [path       (page-path p)
        link-paths (map page-path (j/get p :links))
        links      (map #(assoc {} :page/path %) link-paths)]
    {:page/path        path
     :page/name        (page-name p)
     :page/link        links
     :page/frontmatter (js->clj (j/get p :frontmatter) :keywordize-keys true)
     :page/deleted? false}))


(comment (-> (get-page "Journals/2024-03-22.md") page->datom))


(def schema
  {:page/path        {:db/unique :db.unique/identity}
   :page/alias       {:db/valueType   :db.type/ref
                      :db/cardinality :db.cardinality/many}
   :page/ctime       {}
   :page/day         {}
   :page/fields      {}
   :page/frontmatter {}
   :page/link        {:db/valueType   :db.type/ref
                      :db/cardinality :db.cardinality/many}
   :page/list        {:db/valueType   :db.type/ref
                      :db/cardinality :db.cardinality/many}
   :page/mtime       {}
   :page/name        {}
   :page/size        {}
   :page/deleted     {}
   :page/tag         {:db/valueType   :db.type/ref
                      :db/cardinality :db.cardinality/many}})


(defonce db (d/create-conn schema))

(comment
  (def db (d/create-conn schema)))


(defn get-index-datoms []
  (let [pages (.. plugin -index -pages)
        pages (map second pages)]
    (map page->datom pages)))


(comment (get-index-datoms))


(defn load
  [& {:keys [async?] :or {async? true}}]
  (println "(SaberDB): Loading Index...")
  (let [data  (get-index-datoms)]
    (if async?
      (d/transact-async db data)
      (d/transact! db data))))


(defn native-load
  "TODO: Doesn't work"
  []
  (let [files (.getMarkdownFiles obs/vault)
        data (map page->datom files)]
    (d/transact-async db data)))


(comment (load :async? false))


(defn delete-tx
  [path]
  (let [retractions
        (for [attr (keys (dissoc schema :page/path :page/deleted?))]
          [:db/retract [:page/path path] attr])]
    (conj (vec retractions) [:db/add [:page/path path] :page/deleted? true])))


(defn update-tx
  [page]
  (let [path (page-path page)
        update (page->datom page)
        attrs [:page/link :page/tag :page/list :page/alias]
        retractions (for [attr attrs] [:db/retract [:page/path path] attr])
        tx-data (conj (vec retractions) update)]
    tx-data))


(defn rename-tx
  [page old-path]
  (let [new-path (page-path page)
        new-name (page-name page)
        tx [[:db/add [:page/path old-path] :page/path new-path]
            [:db/add [:page/path new-path] :page/name new-name]]]
    tx))


(defn md-update
  [type file old-path]
  (let [path (page-path file)
        page (get-page path)]
    (case type
      "delete" (d/transact-async db (delete-tx path))
      "update" (d/transact-async db (update-tx page))
      "rename" (d/transact-async db (rename-tx page old-path))
      (js/console.log type "Unimplemented"))))


(defn init []
  (.on obs/md "dataview:index-ready" #'load)
  (.on obs/md "dataview:metadata-change" #'md-update))


(def rules

  '[[(dir ?dir ?page)
     [?page :page/path ?path]
     [(clojure.string/starts-with? ?path ?dir)]]

    [(path-match ?p ?rx)
     [?p :page/path ?path]
     (re-find ?rx ?path)]

    [(attr ?page ?attr-name ?attr)
     [?page :page/frontmatter ?fm]
     [(get ?fm ?attr-name) ?attr]]

    [(attr= ?page ?attr-name ?val)
     (attr ?page ?attr-name ?attr)
     [(= ?attr ?val)]]

    ])



(comment

  (def db (d/create-conn schema))

  (load :async? false)


  (js/console.log
    (get-page "Journals/2024-03-24.md"))

  (d/pull @db '[*] [:page/path "Pages/Test File.md"])
  (d/pull @db '[*] [:page/path "Pages/Tested File.md"])
  (d/pull @db '[:page/name {:page/link [:page/path]}] [:page/path "Journals/2024-03-24.md"])

  (d/transact! db [{:page/path        "Pages/Test File.md"
                    :page/name        "Test File.md"
                    :page/frontmatter {:one :two}}])
  (d/transact! db [[:db/retract [:page/path "Pages/Test File.md"] :page/name]
                   [:db/retract [:page/path "Pages/Test File.md"] :page/frontmatter]])


  (def query '[:find [(pull ?p [:page/name :page/path]) ...]
               :in $
               :where
               [?p :page/path ?path]
               [?p :page/frontmatter ?fm]
               [(re-find #"Tasks/" ?path)]
               [(get ?fm :class) ?class]
               [(= ?class "Track")]
               (not [(contains? ?fm :completed)])])


  (defn db->txt
    [db]
    (let [results (d/q query @db)]
      (s/join "\n" (for [p results] (str "- [[" (:page/name p) "]]")))))


  (defn render-query
    [db source el ctx]
    (set! (.-innerHTML el) "")
    (let [md (db->txt db)]
      (obs/render-md md el ctx)))


  (defn scratch-list
    [source el ctx]
    (add-watch db :watcher
               (fn [_key atom _old-state _new-state]
                 (render-query db source el ctx)))
    (render-query db source el ctx))



  (obs/cb-processor "saberq" (fn [s e c] (scratch-list s e c)))



  (defn static [s e c]
    (let [items (d/q '[:find [(pull ?p [:page/name :page/path]) ...]
                       :in $
                       :where
                       [?p :page/path ?path]
                       [?p :page/frontmatter ?fm]
                       [(re-find #"Tasks/" ?path)]
                       [(get ?fm :class) ?class]
                       [(= ?class "Track")]
                       (not [(contains? ?fm :completed)])]
                     @db)
          links (for [i items] (str "[" (:page/name i) "](" (:page/path i) ")"))
          links (for [i items] (str "[[" (:page/name i) "]]"))
          ]
      (obs/render-md
        (s/join "\n" links)
        e c)))

  (obs/cb-processor "saber-static" (fn [s e c] (static s e c)))

  (obs/cb-processor "saber" (fn [_s e _c] (rum/mount (scratch-list db) e)))



  ;; Todo Query
  (->
    (d/q '[:find [(pull ?p [:page/name :page/path]) ...]
           :in $ % ?class ?priority
           :where
           (attr ?p :class ?class)
           (attr ?p :priority ?priority)
           (not (attr ?p :completed ?completed))]
         @db rules "Track" "later")
    first
    :page/name)

  ;; Inbox Query
  (d/q '[:find (pull ?p [:page/name :page/path])
         :in $ % ?class
         :where
         (attr ?p :class ?class)
         (not (attr ?p :priority ?priority))
         (not (attr ?p :completed ?completed))]
       @db rules "Track")


)
