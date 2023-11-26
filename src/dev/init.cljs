(ns dev.init
  (:require [saber.obsidian :as obs]
            [clojure.string :as s]))



(defn cycle-line
  [l]
  (let [[_ whitespace _bullet current content] (re-find #"^(\s*)(-)? ?(\[.\])? ?(.*)$" l)
        nxt (case current
              "[ ]" "[/]"
              "[x]" "[ ]"
              nil "[ ]"
              "[x]")]
    (str whitespace "- " nxt " " content )))


(defn cycle-lines
  [text]
  (->> text
       s/split-lines
       (map cycle-line)
       (s/join "\n")))


(obs/define-command
  :cycle-lines
  "Cycle Lines"
  #(obs/replace-selection-or-line cycle-lines))
