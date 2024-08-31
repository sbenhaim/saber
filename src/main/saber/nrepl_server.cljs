(ns saber.nrepl-server
  (:require [nrepl-cljs-sci.core :as nrepl]
            [saber.sci :as sci]))

(defn start [port]
  (let [opts {:port port
              :ctx (sci/get-ctx)
              :app js/app}]
    (nrepl/start-server opts)))
