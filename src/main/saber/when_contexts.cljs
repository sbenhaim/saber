(ns saber.when-contexts)

(defonce ^:private !db (atom {:contexts {::saber.isActive false
                                         ::saber.isNReplServerRunning false}}))

(defn set-context! [k v]
  (swap! !db assoc-in [:contexts k] v)
  ;; (vscode/commands.executeCommand "setContext" (name k) v)
  )

(defn context [k]
  (get-in @!db [:contexts (if (string? k)
                            (keyword (str "saber.when-contexts/" k))
                            k)]))

(comment
  (context "saber.isActive"))
