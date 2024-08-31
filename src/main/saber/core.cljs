(ns saber.core
  (:require
   ["@mui/x-data-grid" :refer [DataGrid]]
   ["@mui/material/styles" :refer [ThemeProvider createTheme]]
   ;; ["@mui/material/CssBaseline" :refer [CssBaseline]]
   [applied-science.js-interop :as j]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as s]
   [datascript.core :as d]
   [promesa.core :as p]
   [reagent.dom :as rdom]
   [saber.calcula :as calc]
   ;; [saber.nrepl :as nrepl]
   [saber.nrepl-server :as nrepl]
   [saber.obsidian :as obs]
   [saber.query :as q]
   [saber.sci :refer [eval-string]]
   [sci.core :as sci]))


(defn load-cljs-file
  [path]
  (sci/with-bindings
    {sci/ns   @sci/ns
     sci/file path}
    (p/let [code (obs/read (obs/file path))]
      (try
        (eval-string code)
        (catch js/Error e
          (obs/msg (str "Error loading " path ": " e))
          (println "Error loading " path ": " e))))))

(comment (obs/file "Saber/init.cljs"))



(defn load-init
  []
  (let [path "Saber/init.cljs"]
    (load-cljs-file path)
    (println "Saber/init.cljs loaded.")))


(defn start-nrepl
  [port]
  (nrepl/start port)
  (obs/msg (str "nREPL server started on port " port) 5000))



(defn code-block-processor
  [source el ctx]
  (try
    (let [mode el.className
          result (eval-string source)
          result-str (with-out-str (pprint result))]
      (case mode
        "block-language-clojure-reagent" (rdom/render [result] el)
        "block-language-clojure-md" (obs/render-md result el ctx)
        "block-language-clojure-eval" (obs/render-md
                                        (str "```clojure\n" result-str "\n```")
                                        el ctx)
        "block-language-clojure-source" (obs/render-md
                                          (str "```clojure\n"
                                               source
                                               "\n => "
                                               result-str
                                               "\n```")
                                          el ctx)))
    (catch js/Error e
      (obs/msg (str "Error evaluating code block: " e))
      (println "Error evaluating code block: " e))))


(defn calcula-processor
  [source el ctx]
  (let [result (calc/process source)]
    (obs/render-md
      result
      el ctx)))



;; (def theme-provider (rum/adapt-class ThemeProvider))
;; (def css-baseline (rum/adapt-class CssBaseline))
;; (def dark-theme (createTheme (clj->js {:palette {:mode :dark}})))

;; (rum/defc table [cols rows]
;;   (let [rs  (clj->js rows)
;;         cls (clj->js cols)]
;;     (rum/adapt-class ThemeProvider {:theme dark-theme}
;;                      ;; (rum/adapt-class CssBaseline {})
;;                      (rum/adapt-class DataGrid {:rows rs :columns cls}))))


(defn main
  [^obsidian.Plugin plugin obsidian]

  (obs/init! plugin obsidian)

  (q/init)

  ;; Command to start nREPL
  (obs/define-command
    :nrepl-server
    "Start nREPL server"
    #(start-nrepl plugin.settings.nreplPort))

  ;; Command to reload init file
  (obs/define-command :reload-init "Reload init.cljs" load-init)

  ;; Attempt to load init file
  (try
    (load-init)
    (catch js/Error e
      (obs/msg (str "Error loading init.cljs: " e))
      (println "Error loading init.cljs: " e)))

  ;; Register markdown processors
  (obs/cb-processor "clojure-eval" code-block-processor)
  (obs/cb-processor "clojure-source" code-block-processor)
  (obs/cb-processor "clojure-md" code-block-processor)
  (obs/cb-processor "clojure-reagent" code-block-processor)
  (obs/cb-processor "calcula" calcula-processor)

  ;; Init query engine
  (q/init)
  )


(comment
  (obs/read
    (obs/file "Saber/init.cljs")))
