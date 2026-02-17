(ns biff_repl.repl
  "Web REPL routes and handlers for sandboxed Clojure evaluation."
  (:require [com.biffweb :as biff]
            [biff_repl.ui :as ui]
            [biff_repl.sandbox :as sandbox]
            [biff_repl.repl-commands :as commands]
            [clojure.string :as str]
            [ring.middleware.anti-forgery :as csrf]
            [cheshire.core :as cheshire]
            [rum.core :as rum]))

;; Rate limiting
(defonce rate-limit-store (atom {}))
(def ^:private max-evals-per-minute 30)
(def ^:private rate-window-ms 60000)

(defn- get-session-id [req]
  (or (get-in req [:session :uid])
      (get-in req [:cookies "ring-session" :value])
      (:remote-addr req)
      "anonymous"))

(defn- cleanup-old-timestamps [timestamps now]
  (filterv #(> % (- now rate-window-ms)) timestamps))

(defn- check-rate-limit
  "Returns true if the request is within rate limits, false otherwise."
  [session-id]
  (let [now (System/currentTimeMillis)]
    (swap! rate-limit-store
           (fn [store]
             (let [timestamps (get store session-id [])
                   cleaned (cleanup-old-timestamps timestamps now)]
               (assoc store session-id cleaned))))
    (let [count (count (get @rate-limit-store session-id []))]
      (< count max-evals-per-minute))))

(defn- record-eval [session-id]
  (let [now (System/currentTimeMillis)]
    (swap! rate-limit-store
           update session-id
           (fn [timestamps]
             (conj (cleanup-old-timestamps (or timestamps []) now) now)))))

;; SCI context (created once, shared across requests)
(defonce sci-ctx (sandbox/create-context commands/commands-ns))

(defn- result-html
  "Renders the evaluation result as HTML."
  [{:keys [result error]} code]
  (if error
    [:div.result-entry
     [:div.code-input.text-gray-400.text-sm.mb-1
      [:span.text-blue-400 "> "] code]
     [:div.error.text-red-400.font-mono.whitespace-pre-wrap
      error]]
    [:div.result-entry
     [:div.code-input.text-gray-400.text-sm.mb-1
      [:span.text-blue-400 "> "] code]
     [:div.result.text-green-300.font-mono.whitespace-pre-wrap
      (sandbox/format-result result)]]))

(defn eval-handler
  "Handles POST /repl/eval requests."
  [{:keys [params] :as req}]
  (let [session-id (get-session-id req)
        code (str/trim (or (:code params) ""))]
    (cond
      (str/blank? code)
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body ""}

      (not (check-rate-limit session-id))
      {:status 429
       :headers {"Content-Type" "text/html"}
       :body (rum/render-static-markup
              [:div.result-entry
               [:div.code-input.text-gray-400.text-sm.mb-1
                [:span.text-blue-400 "> "] code]
               [:div.error.text-red-400.font-mono
                "Rate limit exceeded. Please wait before trying again."]])}

      :else
      (do
        (record-eval session-id)
        (let [eval-result (sandbox/eval-string sci-ctx code)]
          {:status 200
           :headers {"Content-Type" "text/html"}
           :body (rum/render-static-markup (result-html eval-result code))})))))

(defn repl-page
  "Renders the main REPL page with CodeMirror editor."
  [req]
  (ui/base
   (assoc req :base/title "Clojure REPL")
   [:div.min-h-screen.bg-gray-900.text-gray-100
    [:div.max-w-4xl.mx-auto.p-4

     ;; Header
     [:div.mb-6
      [:h1.text-3xl.font-bold.text-green-400 "Clojure REPL"]
      [:p.text-gray-400.mt-2
       "Interactive sandboxed Clojure environment. Try "
       [:code.text-yellow-300 "(help)"]
       " to get started."]]

     ;; Output area
     [:div#repl-output.bg-gray-800.rounded-lg.p-4.mb-4.min-h-64.max-h-96.overflow-y-auto.font-mono.text-sm
      [:div.result-entry
       [:div.text-gray-500 ";; Welcome to the Clojure REPL!"]
       [:div.text-gray-500 ";; Type (help) for available commands."]
       [:div.text-gray-500 ";; Press Ctrl+Enter or click Eval to run code."]]]

     ;; Input area
     [:form {:hx-post "/repl/eval"
             :hx-target "#repl-output"
             :hx-swap "beforeend scroll:#repl-output:bottom"
             :hx-headers (cheshire/generate-string
                          {:x-csrf-token csrf/*anti-forgery-token*})}
      [:div.bg-gray-800.rounded-lg.overflow-hidden
       [:div#editor-container.border-b.border-gray-700
        {:data-placeholder "(+ 1 2 3)"}]
       [:textarea#code-input.hidden {:name "code"}]
       [:div.flex.justify-between.items-center.p-2.bg-gray-750
        [:div.text-gray-500.text-sm
         [:span.mr-4 "Ctrl+Enter to evaluate"]
         [:span "30 evals/min"]]
        [:button.bg-green-600.hover:bg-green-500.text-white.px-4.py-2.rounded.font-medium.transition-colors
         {:type "submit"}
         "Eval"]]]]

     ;; Footer
     [:div.mt-6.text-center.text-gray-500.text-sm
      [:p "Powered by "
       [:a.text-blue-400.hover:underline
        {:href "https://github.com/babashka/sci" :target "_blank"}
        "SCI"]
       " (Small Clojure Interpreter)"]
      [:p.mt-1
       [:a.text-blue-400.hover:underline
        {:href "/" :target "_self"}
        "Back to Home"]]]]]))

(def routes
  [["/repl" {:get repl-page}]
   ["/repl/eval" {:post eval-handler}]])

(def module
  {:routes routes})
