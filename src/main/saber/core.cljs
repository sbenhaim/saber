(ns saber.core
  (:require
   [clojure.pprint :refer [pprint]]
   [nrepl-cljs-sci.core :as nrepl]
   [reagent.dom :as rdom]
   [saber.obsidian :as obs]
   [sci.ctx-store :as store]
   [saber.sci :as sci]
   [promesa.core :as p]))


(defn load-init
  []
  (let [path "Saber/init.cljs"]
    (sci/saber-load-file path)
    (println "Saber/init.cljs loaded.")))


(defn start-nrepl
  [port]
  (let [opts {:port port
              :app js/app
              :host "127.0.0.1"
              :ctx (store/get-ctx)}]
    (nrepl/start-server opts)
    (obs/msg (str "nREPL server started on port " port) 5000)))



(defn code-block-processor
  [source el ctx]
  (try
    (p/let [mode el.className
            result (sci/eval-string source)
            result-str (with-out-str (pprint result))]
      (case mode
        "block-language-clojure-reagent" (rdom/render [result] el)
        "block-language-clojure-md"      (obs/render-md result el ctx)
        "block-language-clojure-eval"    (obs/render-md
                                        (str "```clojure\n" result-str "\n```")
                                        el ctx)
        "block-language-clojure-source"  (obs/render-md
                                          (str "```clojure\n"
                                               source
                                               "\n => "
                                               result-str
                                               "\n```")
                                          el ctx)))
    (catch js/Error e
      (obs/msg (str "Error evaluating code block: " e))
      (println "Error evaluating code block: " e))))


(defn main
  [^obsidian.Plugin plugin obsidian]

  (sci/init)
  (obs/init! plugin obsidian)

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
  (obs/cb-processor "clojure-reagent" code-block-processor))


(comment
  (obs/read
    (obs/file "Saber/init.cljs")))
