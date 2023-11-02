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


(comment
  (-> (obs/current-link)
      (obs/resolve-link (obs/current-path)))


  (str (tick/date-time))

  (.toLocaleString js/Date)

  (.toLocaleString (js/Date.))

  (let [link (obs/current-link)
        path (obs/current-path)
        file (obs/resolve-link link path)
        fm (.-fileManager obs/app)
        now (tick/date-time)]

    (.processFrontMatter fm file
                         (fn [fm] (j/assoc! fm :completed (.toISOString (js/Date.)))))))


(comment
  (def md-files (.getMarkdownFiles vault))

  (count md-files)

  (p/let* [f (first md-files)
           stuff (.read vault f)]
          (println stuff))


  (->>
    (.. obs/this -app -vault getMarkdownFiles)
    first
    ))


(def md (.. obs/this -app -metadataCache))
(def links (js->clj (.-resolvedLinks md)))
(def paths (keys links))
