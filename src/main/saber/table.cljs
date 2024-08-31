(ns main.saber.table
  (:require
            [datascript.core :as d]
            ;; ["@mui/x-data-grid" :refer [DataGrid]]
            [rum.core :as rum]
            [saber.obsidian :as obs]
            [saber.query :as query]))


(comment
  (d/q '[:find [?path ...]
         :in $
         :where
         [?p :page/path ?path]
         [?p :page/frontmatter ?fm]
         [(re-find #"Tasks/" ?path)]
         [(get ?fm :class) ?class]
         [(= ?class "Track")]
         (not [(contains? ?fm :completed)])]
       @query/db))


(rum/defc scratch-list < rum/reactive [db]
  (let [db    (rum/react db)
        items (d/q '[:find [?path ...]
                     :in $
                     :where
                     [?p :page/path ?path]
                     [?p :page/frontmatter ?fm]
                     [(re-find #"Tasks/" ?path)]
                     [(get ?fm :class) ?class]
                     [(= ?class "Track")]
                     (not [(contains? ?fm :completed)])]
                   db)]
    [:ul
     (for [item items]
       [:li {:key item} item])]))

(def db query/db)


(obs/cb-processor "scratch" (fn [_s e _c] (rum/mount (scratch-list db) e)))


(swap! db conj 10)


(comment

  (def some-data (r/atom [:one :two :three]))


  (defn scratch-list
    []
    (fn []
      [:ul
       (for [item @some-data]
         ^{:key item}
         [:li item])]))


  (defn render-list [source el etx]
    (rdom/render [scratch-list] el))

  (obs/cb-processor "scratch" (fn [s e c] (render-list s e c)))

  (swap! some-data conj :four)
  (swap! some-data concat [1 2 3 4 5 6])
  (reset! some-data [])

  (query/load)

  (def rules
    `[[(path-match ?p ?rx)
       [?p :page/path ?path]
       (re-find ?rx ?path)]])

  (d/q '[:find ?path ?fm
         :in $ %
         :where
         [?p :page/path ?path]
         [?p :page/frontmatter ?fm]
         ;; [?rx #"Tasks/"]
         ;; (path-match ?p #"Tasks/")
         ;; [(re-find #"Tasks/" ?path)]
         [(get ?fm :class) ?class]
         [(= ?class "Track")]
         (not [(contains? ?fm :completed)])
         ]
       @query/db rules)

  )



(defn md-list
  [rows cols]
  (fn []
    [:> DataGrid {:rows rows :columns cols}]))


(defn render-table [source el etx]
  (js/console.log "Hi")
  (let [rows [{:id 1 :col1 "Hello" :col2 "World"}
              {:id 2 :col1 "DataGridPro" :col2 "is Awesome"}
              {:id 3 :col1 "MUI" :col2 "is Amazing"}]
        columns [{:field "col1" :headerName "Column 1" :width 150}
                 {:field "col2" :headerName "Column 2" :width 150}]]
    (js/console.log "Hi")
    (rdom/render [md-list rows columns] el)))


(obs/cb-processor "table" (fn [s e c] (render-table s e c)))
