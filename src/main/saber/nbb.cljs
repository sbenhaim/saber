(ns saber.nbb
  (:require [nbb.impl.nrepl-server :as nrepl]))

(defn nrepl-server [port]
  (nrepl/start-server! {:port port}))
