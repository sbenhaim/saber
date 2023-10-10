(ns saber.obsidian
  (:require [clojure.string :as s]
            [applied-science.js-interop :as j]
            ["obsidian" :as obs]))


(defn editor []
  (.-editor js/app.workspace.activeEditor))


(comment (editor))

(defn cursor []
  (.getCursor (editor)))

(comment (cursor))

(defn current-line []
  (j/get (cursor) :line))

(comment (current-line))

(defn current-line-text []
  (.getLine (editor) (current-line)))

(comment (current-line-text))

(defn selection []
  (.getSelection (editor)))

(comment (selection))

(defn replace-selection
  [f]
  (.replaceSelection (editor) (f (selection))))


(defn plugin
  [plugin]
  (js/app.plugins.getPlugin plugin))


(defn define-command
  [id name f]
  (.addCommand (plugin "saber") #js {:id id :name name :callback f}))


(defn replace-current-line
  [f]
  (.setLine (editor) (current-line) (f (current-line-text))))


;; TODO: Use replace-current-line
(defn replace-regexp-current-line [regexp replacement]
  (let [txt (current-line-text)
        rep (s/replace txt regexp replacement)]
    (.setLine (editor) (current-line) rep)))


(defn insert [text]
  (.replaceRange (editor) text (cursor)))


(comment
  (insert "But better yet, I can do it programmatically!")
  )


(defn msg
  ([s] (msg s 0))
  ([s t] (obs/Notice. s t)))


(comment
  (replace-regexp-current-line #"[aeiou]" "x"))
