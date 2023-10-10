(ns saber.sci
  (:require
   ["fs" :as fs]
   ["module" :as module]
   ["path" :as path]
   ["obsidian" :as obsidian]
   [clojure.string :as str]
   [clojure.zip]
   [portal.api]
   [saber.repl-utils :as repl-utils]
   [saber.obsidian]
   [sci.configs.cljs.test :as cljs-test-config]
   [sci.configs.cljs.pprint :as cljs-pprint-config]
   [sci.configs.funcool.promesa :as promesa-config]
   [sci.configs.applied-science.js-interop :as js-interop-config]
   [sci.configs.tonsky.datascript :as datascript-config]
   [sci.configs.reagent.reagent :as reagent-config]
   [sci.core :as sci]
   [sci.ctx-store :as store]
   [rewrite-clj.node]
   [rewrite-clj.parser]
   [rewrite-clj.zip]))

(sci/enable-unrestricted-access!) ;; allows mutating and set!-ing all vars from inside SCI
(sci/alter-var-root sci/print-fn (constantly *print-fn*))
(sci/alter-var-root sci/print-err-fn (constantly *print-err-fn*))

(def saber-ns (sci/create-ns 'saber.core nil))

(defn ns->path [namespace]
  (-> (str namespace)
      (munge)
      (str/replace  "." "/")
      (str ".cljs")))

(defn source-script-by-ns [namespace]
  (let [ns-path (ns->path namespace)
        path-if-exists (fn [search-path]
                         (let [file-path (path/join search-path ns-path)]
                           (when (fs/existsSync file-path)
                             file-path)))
        ;; workspace first, then user - the and is a nil check for no workspace
        path-to-load (first (keep #(and % (path-if-exists %))
                                  ["."]
                                  #_[(conf/workspace-abs-src-path)
                                   (conf/workspace-abs-scripts-path)
                                   (conf/user-abs-src-path)
                                   (conf/user-abs-scripts-path)]))]
    (when path-to-load
      {:file ns-path
       :path-to-load path-to-load
       :source (str (fs/readFileSync path-to-load))})))


(defn require* [from-ns lib {:keys [reload]}]
  (let [from-path (if (.startsWith lib "/")
                    ""
                    (:path-to-load (source-script-by-ns from-ns)))
        req (module/createRequire (path/resolve (or from-path "./script.cljs")))
        resolved (.resolve req lib)]
    (when reload
      (aset (.-cache req) resolved js/undefined))
    (js/require resolved)))


(def zns (sci/create-ns 'clojure.zip nil))

(def zip-namespace
  (sci/copy-ns clojure.zip zns))

(def rzns (sci/create-ns 'rewrite-clj.zip))
(def rewrite-clj-zip-ns (sci/copy-ns rewrite-clj.zip rzns))

(def rpns (sci/create-ns 'rewrite-clj.parser))
(def rewrite-clj-parser-ns (sci/copy-ns rewrite-clj.parser rpns))

(def rnns (sci/create-ns 'rewrite-clj.node))
(def rewrite-clj-node-ns (sci/copy-ns rewrite-clj.node rnns))

(def core-namespace (sci/create-ns 'clojure.core nil))

(def pns (sci/create-ns 'portal.api))
(def portal-namespace (sci/copy-ns portal.api pns))

(def sons (sci/create-ns 'saber.obsidian))
(def saber-obsidian-namespace (sci/copy-ns saber.obsidian sons))

(def saber-core
  {'*file* sci/file
   ;; 'extension-context (sci/copy-var db/extension-context joyride-ns)
   ;; 'invoked-script (sci/copy-var db/invoked-script joyride-ns)
   ;; 'output-channel (sci/copy-var db/output-channel joyride-ns)
   'js-properties repl-utils/instance-properties})



(store/reset-ctx!
  (let [config {:classes {'js (doto goog/global
                                (aset "require" js/require))
                          :allow :all}
                :namespaces {'clojure.core {'IFn (sci/copy-var IFn core-namespace)}
                             'clojure.zip zip-namespace
                             'saber.core saber-core
                             'rewrite-clj.zip rewrite-clj-zip-ns
                             'rewrite-clj.parser rewrite-clj-parser-ns
                             'rewrite-clj.node rewrite-clj-node-ns
                             'portal.api portal-namespace
                             'saber.obsidian saber-obsidian-namespace}
                :ns-aliases {'clojure.test 'cljs.test}
                :js-libs {"fs" fs
                          "obsidian" obsidian}
                :load-fn (fn [{:keys [ns libname opts]}]
                           (cond
                             (symbol? libname)
                             (source-script-by-ns libname)
                             :else ;; (string? libname) ;; node built-in or npm library
                             (let [mod (require* ns libname opts)
                                   ns-sym (symbol libname)]
                               (sci/add-class! (store/get-ctx) ns-sym mod)
                               (sci/add-import! (store/get-ctx) ns ns-sym
                                                (or (:as opts)
                                                    ns-sym))
                               {:handled true})))}]
    (doto
        (sci/init config)
        (sci/merge-opts js-interop-config/config)
        (sci/merge-opts cljs-test-config/config)
        (sci/merge-opts cljs-pprint-config/config)
        (sci/merge-opts promesa-config/config)
        (sci/merge-opts datascript-config/config)
        (sci/merge-opts reagent-config/config))))

(def !last-ns (volatile! @sci/ns))

(defn eval-string [s]
  (sci/binding [sci/ns @!last-ns]
    (let [rdr (sci/reader s)]
      (loop [res nil]
        (let [form (sci/parse-next (store/get-ctx) rdr)]
          (if (= :sci.core/eof form)
            (do
              (vreset! !last-ns @sci/ns)
              res)
            (recur (sci/eval-form (store/get-ctx) form))))))))
