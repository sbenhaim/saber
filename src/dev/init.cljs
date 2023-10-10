(ns dev.init
  (:require [saber.obsidian :as obs]))

(js/console.log "(Saber) Loading init.cljs")

(obs/define-command "sort-selection" "Sort Selection" obs/sort-selection)