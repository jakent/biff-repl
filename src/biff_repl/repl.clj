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

;; Paren balance checking
(defn- balanced?
  "Returns true if all parens, brackets, and braces are balanced in s.
   Tracks string context so delimiters inside strings are ignored."
  [s]
  (loop [i 0
         depth {:paren 0 :bracket 0 :brace 0}
         in-string false
         escape false]
    (if (>= i (count s))
      (and (not in-string)
           (zero? (:paren depth))
           (zero? (:bracket depth))
           (zero? (:brace depth)))
      (let [ch (.charAt s i)]
        (cond
          escape
          (recur (inc i) depth in-string false)

          (= ch \\)
          (recur (inc i) depth in-string true)

          (= ch \")
          (recur (inc i) depth (not in-string) false)

          in-string
          (recur (inc i) depth in-string false)

          (= ch \()
          (recur (inc i) (update depth :paren inc) false false)

          (= ch \))
          (recur (inc i) (update depth :paren dec) false false)

          (= ch \[)
          (recur (inc i) (update depth :bracket inc) false false)

          (= ch \])
          (recur (inc i) (update depth :bracket dec) false false)

          (= ch \{)
          (recur (inc i) (update depth :brace inc) false false)

          (= ch \})
          (recur (inc i) (update depth :brace dec) false false)

          :else
          (recur (inc i) depth false false))))))

;; SCI context (created once, shared across requests)
(defonce sci-ctx (sandbox/create-context commands/commands-ns))

(defn- result-html
  "Renders the evaluation result as HTML."
  [{:keys [result error]} code]
  (if error
    [:div.mb-1
     [:div.flex.items-start
      [:span.text-yellow-500.mr-2 "=>"]
      [:span.text-gray-300.whitespace-pre-wrap code]]
     [:div.text-red-400.whitespace-pre-wrap
      error]]
    [:div.mb-1
     [:div.flex.items-start
      [:span.text-yellow-500.mr-2 "=>"]
      [:span.text-gray-300.whitespace-pre-wrap code]]
     [:div.text-green-300.whitespace-pre-wrap
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

      (not (balanced? code))
      {:status 200
       :headers {"Content-Type" "text/html"
                 "HX-Trigger" "unbalanced"}
       :body ""}

      (not (check-rate-limit session-id))
      {:status 429
       :headers {"Content-Type" "text/html"}
       :body (rum/render-static-markup
              [:div.mb-1
               [:div.whitespace-pre-wrap
                [:span.text-yellow-500 "=> "] [:span.text-gray-300 code]]
               [:div.text-red-400.whitespace-pre-wrap.pl-6
                "Rate limit exceeded. Please wait before trying again."]])}

      :else
      (do
        (record-eval session-id)
        (let [eval-result (sandbox/eval-string sci-ctx code)]
          {:status 200
           :headers {"Content-Type" "text/html"}
           :body (rum/render-static-markup (result-html eval-result code))})))))

(defn repl-page
  "Renders the main REPL page as a unified terminal."
  [req]
  (ui/base
   (assoc req :base/title "Clojure REPL")
   [:div.min-h-screen.bg-gray-900.text-gray-100
    [:div.max-w-4xl.mx-auto.p-4

     ;; Header
     [:div.mb-4
      [:h1.text-3xl.font-bold.text-green-400 "Clojure REPL"]
      [:p.text-gray-400.mt-1
       "Interactive sandboxed Clojure environment. Try "
       [:code.text-yellow-300 "(help)"]
       " to get started."]]

     ;; Unified terminal container — natural top-to-bottom flow
     [:div#repl-container.bg-gray-800.rounded-lg.p-4.font-mono.text-sm.overflow-y-auto
      {:style {:min-height "70vh" :max-height "85vh"}}

      ;; History (normal flow, not flex)
      [:div#repl-history
       [:div.text-gray-500.mb-1 ";; Welcome to the Clojure REPL!"]
       [:div.text-gray-500.mb-2 ";; Type (help) for available commands."]]

      ;; Inline prompt + input (sits right after history)
      [:form#repl-form
       {:hx-post "/repl/eval"
        :hx-target "#repl-history"
        :hx-swap "beforeend"
        :hx-headers (cheshire/generate-string
                     {:x-csrf-token csrf/*anti-forgery-token*})}
       [:div.flex.items-start
        [:span.text-yellow-500.mr-2.leading-6.select-none "=>"]
        [:textarea#repl-input.flex-1.bg-transparent.text-green-300.resize-none.leading-6.p-0.m-0.border-0
         {:name "code"
          :rows "1"
          :placeholder "(+ 1 2 3)"
          :autofocus true
          :spellcheck "false"
          :autocomplete "off"
          :autocorrect "off"
          :autocapitalize "off"
          :style {:outline "none" :box-shadow "none"}}]]]]

     ;; Footer
     [:div.mt-4.text-center.text-gray-500.text-sm
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
