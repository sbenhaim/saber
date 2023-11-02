(ns saber.obsidian
  (:require [clojure.string :as s]
            [applied-science.js-interop :as j]))


(defn js-obs
  "In dev mode, we set a global obsidian variable. In release, we can require it."
  []
  (try (js/require "obsidian")
       (catch js/Error e js/obsidian)))

(comment
  (js-obs))


(def obsidian (js-obs))


(defn ^obsidian.Editor editor []
  (when-let [editor js/app.workspace.activeEditor]
    (.-editor editor)))


(comment (editor))

(defn cursor []
  (.getCursor (editor)))

(comment (cursor))

(defn current-line []
  (j/get (cursor) :line))

(comment (current-line))

(defn current-line-text []
  (let [s (.getLine (editor) (current-line))]
    (when-not (= s "") s)))

(comment (current-line-text))

(defn selection []
  (let [s (.getSelection (editor))]
    (when-not (= s "") s)))

(comment (selection))


(defn current-text []
  (.. (editor) -cm -state -doc toString))


(defn current-context []
  (or (selection) (current-line-text) (current-text)))


;; (comment (current-context))


(defn replace-selection
  [f]
  (.replaceSelection (editor) (f (selection))))


(defn plugin
  [plugin]
  (js/app.plugins.getPlugin plugin))


(def ^obsidian.Plugin this (plugin "saber"))

(def ^obsidian.App app (j/get this :app))

(def ^obsidian.Vault vault (j/get app :vault))

(def ^obsidian.Workspace workspace (j/get app :workspace))

(def ^obsidian.MetadataCache md (j/get app :metadataCache))


(defn define-command
  [id name f]
  (.addCommand this #js {:id id :name name :callback f}))


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
  ([s] (msg s 2000))
  ;; Todo: Notice
  ([s t]
   (let [obs (js-obs)]
     (obs.Notice. s t))))


(defn render-md
  [md el ctx]
  (let [js-obs (js-obs)]
    (js-obs.MarkdownRenderer.renderMarkdown
      md el "" ctx)))


(comment
  (msg "Hello, world!")
  (msg "Hello, world!" 5000))


(comment
  (replace-regexp-current-line #"[aeiou]" "x"))


(defn cb-processor
  [lang callback]
  (.registerMarkdownCodeBlockProcessor this lang callback))


(defn current-file
  []
  (.getActiveFile workspace))


(comment (current-file))



(defn current-path
  []
  (j/get (current-file) :path))


(comment (current-path))

(comment (current-line-text))

(defn current-word
  []
  (try
    (let [editor (editor)
          w (.wordAt editor (cursor))]
      (.getRange editor (j/get w :from) (j/get w :to)))
    (catch js/Error e nil)))


(comment (current-word))


(defn current-link
  []
  (let [line (current-line-text)
        links (re-seq #"\[\[(.+?)\]\]" line)]
    (-> links first second)))


(defn resolve-link
  [link-text source-path]
  (.getFirstLinkpathDest md link-text source-path))


(defn file
  [path]
  (.getAbstractFileByPathInsensitive vault path))


(defn filecache
  [tfile]
  (.getFileCache md tfile))


(defn frontmatter
  [tfile]
  (j/get (filecache tfile) :frontmatter))


(defn file-class
  [tfile]
  (j/get (frontmatter tfile) :class))


(comment
  (file-class (file "Tasks/Send NR Invoice.md")))


(comment
  (current-path)
  (current-link)
  (js/console.log
    (resolve-link (current-link) (current-path)))
  (-> (current-link)
      (resolve-link (current-path))
      (frontmatter)))


(defn read
  [tfile]
  (.read vault tfile))
