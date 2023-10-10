(ns saber.core
  (:require [saber.sci :refer [eval-string]]
            [sci.core :as sci]
            [saber.nrepl :as nrepl]
            ["fs" :as fs]
            [saber.obsidian :as obs]))

;; (set! *eval* #(sci/eval-form ctx %))


(def eval-cljs eval-string)

(comment
  (eval-cljs "{:a :b}"))


(defn load-file
  [path]
  (let [source (fs/readFileSync path "utf8")]
    (js/console.log source)
    (sci/with-bindings
      {sci/ns   @sci/ns
       sci/file path}
      (eval-string source))))


(defn load-init
  []
  (let [vault-root js/app.vault.adapter.basePath
        init-script (str vault-root "/Saber/init.cljs")]
    (if (fs/existsSync init-script)
      (load-file init-script)
      (println "No init script found."))))


(defn start-nrepl
  [port]
  (nrepl/start-server+ {:port port})
  (obs/msg (str "nREPL server started on port " port) 5000))


#_(defn start-repl
  [port]
  (repl/connect (str "http://localhost:" port "/repl"))
  (obs/msg (str "repl started on port " port)))



;; (defn code-block-processor
;;   [source el ctx]
;;   (let [mode el.className
;;         result (eval-string source)]
;;     (case mode
;;       "block-language-clojure-eval")))


(defn main
  [plugin]

  ;; Command to start nREPL
  (obs/define-command
    :nrepl-server
    "Start nREPL server"
    (fn [& _] (start-nrepl plugin.settings.nreplPort)))

  ;; Command to reload init file
  (obs/define-command :reload-init "Reload init.cljs" load-init)


  #_(obs/define-command :start-repl "Start REPL" (fn [& _] (start-repl plugin.settings.replPort)))


  ;; Attempt to load init file
  (try
    (load-init)
    (catch js/Error e
      (obs/msg (str "Error loading init.cljs: " e))
      (println "Error loading init.cljs: " e)))

  ;; Register markdown processors
  ;; (.registerMarkdownCodeBlockProcessor plugin
  ;;                                      "clojure"
  ;;                                      code-block-procecssor)

  (obs/msg "(Saber online)" 2000))
