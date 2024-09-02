(ns saber.commands
  (:require [saber.obsidian :as obs]
            [applied-science.js-interop :as j]
            [promesa.core :as p]
            [tick.core :as tick]
            [tick.locale-en-us]))


(obs/define-command
  :complete-inline
  "Complete Task on Line"
  #(let [link (obs/current-link)
         path (obs/current-path)
         file (obs/resolve-link link path)
         fm   (.-fileManager obs/app)]
     (.processFrontMatter fm file
                          (fn [fm] (j/assoc! fm :completed (tick/zoned-date-time))))))

