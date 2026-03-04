(ns biff_repl.home)

(defn redirect-to-repl [_req]
  {:status 302
   :headers {"Location" "/repl"}})

(def module
  {:routes [["/" {:get redirect-to-repl}]]})
