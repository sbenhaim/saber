(ns saber.core
  (:require
   [promesa.core :as p]
   [saber.nrepl :as nrepl]
   [saber.obsidian :as obs :refer [js-obs]]
   [saber.dataview :as dv]
   [saber.sci :refer [eval-string]]
   [sci.core :as sci]))


(def obsidian (js-obs))


(defn load-cljs-file
  [path]
  (sci/with-bindings
    {sci/ns   @sci/ns
     sci/file path}
    (p/let [code (obs/read (obs/file path))]
      (try
        (js/console.log "Code: " code)
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
  (nrepl/start-server+ {:port port})
  (obs/msg (str "nREPL server started on port " port) 5000))



(defn code-block-processor
  [source el ctx]
  (try
    (let [mode el.className
          result (pr-str (eval-string source))]
      (println mode)
      (case mode
        "block-language-clojure-eval" (obs/render-md
                                        (str "```clojure\n" result "\n```")
                                        el ctx)
        "block-language-clojure-source" (obs/render-md
                                          (str "```clojure\n"
                                               source
                                               "\n => "
                                               result
                                               "\n```")
                                          el ctx)))
    (catch js/Error e
      (obs/msg (str "Error evaluating code block: " e))
      (println "Error evaluating code block: " e))))


(defn main
  [^obsidian.Plugin plugin]

  (dv/init)

  ;; Command to start nREPL
  (obs/define-command
    :nrepl-server
    "Start nREPL server"
    #(start-nrepl plugin.settings.nreplPort))

  ;; Command to reload init file
  (obs/define-command :reload-init "Reload init.cljs" load-init)

  (js/console.log (obs/file "Saber/init.cljs"))
  (js/console.log (js/app.vault.getAbstractFileByPath "Saber/init.cljs"))

  ;; Attempt to load init file
  (try
    (load-init)
    (catch js/Error e
      (obs/msg (str "Error loading init.cljs: " e))
      (println "Error loading init.cljs: " e)))

  ;; Register markdown processors
  (obs/cb-processor "clojure-eval" code-block-processor)
  (obs/cb-processor "clojure-source" code-block-processor))


(comment
  (obs/read
    (obs/file "Saber/init.cljs")))
