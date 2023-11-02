(ns saber.core
  (:require
   [promesa.core :as p]
   [saber.nrepl :as nrepl]
   [saber.obsidian :as obs :refer [js-obs]]
   [saber.sci :refer [eval-string]]
   [sci.core :as sci]))


(def obsidian (js-obs))

(def eval-cljs eval-string)

(comment
  (eval-cljs "{:a :b}"))


(defn load-cljs-file
  [path]
  (sci/with-bindings
    {sci/ns   @sci/ns
     sci/file path}
    (p/let [code (obs/read (obs/file path))]
      (eval-string code))))


(defn load-init
  []
  (let [path "Saber/init.cljs"]
    (if-let [_ (obs/file path)]
      (load-cljs-file path)
      (println "No init script found."))))


(defn start-nrepl
  [port]
  (nrepl/start-server+ {:port port})
  (obs/msg (str "nREPL server started on port " port) 5000))



(defn code-block-processor
  [source el ctx]
  (let [mode el.className
        result (eval-string source)]
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
                                        el ctx))))


(defn main
  [^obsidian.Plugin plugin]

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
  (obs/cb-processor "clojure-source" code-block-processor))


(comment
  (obs/read
    (obs/file "Saber/init.cljs")))
