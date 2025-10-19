(ns saber.sci
  (:require
   ["fs" :as fs]
   ["module" :as module]
   ["path" :as path]
   ["mathjs" :as mathjs]
   [clojure.zip]
   [clojure.string :as str]
   [promesa.core :as p]
   [saber.obsidian :as obs]
   [sci.configs.cljs.test :as cljs-test-config]
   [sci.configs.cljs.pprint :as cljs-pprint-config]
   [sci.configs.funcool.promesa :as promesa-config]
   [sci.configs.applied-science.js-interop :as js-interop-config]
   [sci.configs.tonsky.datascript :as datascript-config]
   [sci.configs.reagent.reagent :as reagent-config]
   [sci.core :as sci]
   [sci.async :as scia]
   [sci.ctx-store :as store]

   [instaparse.core]
   [cljs.pprint]
   [tick.core]
   [tick.locale-en-us]

   ))

(sci/enable-unrestricted-access!) ;; allows mutating and set!-ing all vars from inside SCI
(sci/alter-var-root sci/print-fn (constantly *print-fn*))
(sci/alter-var-root sci/print-err-fn (constantly *print-err-fn*))

(defn load-fn
  [{:keys [namespace ctx] :as args}]
  (p/let [path (str/replace namespace "." "/")
          path (str/replace path "-" "_")
          file (str "Saber/" path ".cljs")
          source (obs/slurp file)]
    (sci/eval-string* ctx source)
    {}))


(def zip-namespace (sci/copy-ns clojure.zip (sci/create-ns 'clojure.zip)))
(def core-namespace (sci/create-ns 'clojure.core nil))
(def saber-obsidian-namespace (sci/copy-ns saber.obsidian (sci/create-ns 'saber.obsidian)))
(def tick-core-namespace (sci/copy-ns tick.core (sci/create-ns 'tick.core)))
(def tick-en-namespace (sci/copy-ns tick.locale-en-us (sci/create-ns 'tick.locale-en-us)))
(def instaparse-namespace (sci/copy-ns instaparse.core (sci/create-ns 'instaparse)))
(def pprint-namespace (sci/copy-ns cljs.pprint (sci/create-ns 'pprint)))

(defn get-ctx
  []
  (let [config {:classes    {'js    (doto goog/global
                                   (aset "require" js/require))
                             :allow :all}
                :namespaces {'clojure.core      {'IFn (sci/copy-var IFn core-namespace)}
                             'clojure.zip       zip-namespace
                             'saber.obsidian    saber-obsidian-namespace
                             'tick.core         tick-core-namespace
                             'tick.locale-en-us tick-en-namespace
                             'instaparse.core   instaparse-namespace
                             'clojure.pprint    cljs.pprint}
                :ns-aliases {'clojure.test   'cljs.test
                             'clojure.pprint 'cljs.pprint}
                :js-libs    {"fs" fs "mathjs" mathjs}
                :load-fn    load-fn}]
    (doto
        (sci/init config)
      (sci/merge-opts js-interop-config/config)
      (sci/merge-opts cljs-test-config/config)
      (sci/merge-opts cljs-pprint-config/config)
      (sci/merge-opts promesa-config/config)
      (sci/merge-opts datascript-config/config)
      (sci/merge-opts reagent-config/config))))



(comment (store/reset-ctx! (get-ctx)))

;; (def !last-ns (volatile! @sci/ns))

;; (defn eval-string [s]
;;   (sci/binding [sci/ns @!last-ns]
;;     (let [rdr (sci/reader s)]
;;       (loop [res nil]
;;         (let [form (sci/parse-next (store/get-ctx) rdr)]
;;           (if (= :sci.core/eof form)
;;             (do
;;               (vreset! !last-ns @sci/ns)
;;               res)
;;             (recur (sci/eval-form (store/get-ctx) form))))))))


(defn eval-string
  [s]
  (scia/eval-string* (store/get-ctx) s))


(comment
  (p/let [res (eval-string "(require '[calcula :as c]) c/parser")]
    (println res)))



(defn saber-load-file
  [path]
  (sci/with-bindings
    {sci/ns   @sci/ns
     sci/file path}
    (p/let [code (obs/slurp path)]
      (try
        (eval-string code)
        (catch js/Error e
          (obs/msg (str "Error loading " path ": " e))
          (println "Error loading " path ": " e))))))



(defn init
  []
  (store/reset-ctx!
    (get-ctx)))
