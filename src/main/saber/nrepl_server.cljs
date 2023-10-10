(ns saber.nrepl-server
  (:require [nrepl-cljs-sci.core :as nrepl]))

(defn start []
  (let [opts {:port 8703
              :app js/app}]
    (nrepl/start-server opts)))
