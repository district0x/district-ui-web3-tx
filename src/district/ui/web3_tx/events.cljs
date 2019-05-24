(ns district.ui.web3-tx.events
  (:require
    [ajax.core :as ajax]
    [bignumber.core :as bn]
    [camel-snake-kebab.core :as cs]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [cljs-web3.eth :as web3-eth]
    [cljs.spec.alpha :as s]
    [day8.re-frame.forward-events-fx]
    [day8.re-frame.http-fx]
    [district.cljs-utils :as cljs-utils]
    [district.ui.web3-tx.queries :as queries]
    [district.ui.web3.events :as web3-events]
    [district.ui.web3.queries :as web3-queries]
    [district.ui.window-focus.queries :as w-focus-queries]
    [district0x.re-frame.interval-fx]
    [district0x.re-frame.spec-interceptors :refer [validate-first-arg validate-args]]
    [district0x.re-frame.web3-fx]
    [re-frame.core :refer [reg-event-fx trim-v inject-cofx]]))

(def interceptors [trim-v])
(s/def ::tx-hash string?)
(s/def ::tx-data map?)


(reg-event-fx
  ::start
  [interceptors (inject-cofx :web3-tx-localstorage)]
  (fn [{:keys [:db :web3-tx-localstorage]} [{:keys [:disable-using-localstorage?
                                                    :recommended-gas-price-option
                                                    :recommended-gas-prices-load-interval
                                                    :disable-loading-recommended-gas-prices?]}]]
    (let [txs (if disable-using-localstorage? {} (queries/txs web3-tx-localstorage))]
      (merge
       {:db (-> db
              (queries/merge-txs txs)
              (queries/assoc-opt :disable-using-localstorage? disable-using-localstorage?)
              (queries/assoc-opt :recommended-gas-prices-load-interval (or recommended-gas-prices-load-interval 30000))
              (queries/assoc-recommended-gas-price-option (or recommended-gas-price-option :average)))
        :forward-events {:register ::web3-created
                         :events #{::web3-events/web3-created}
                         :dispatch-to [::watch-pending-txs]}}
       (when-not disable-loading-recommended-gas-prices?
         {:dispatch [::watch-recommended-gas-prices]})))))


(reg-event-fx
  ::watch-pending-txs
  interceptors
  (fn [{:keys [:db]}]
    (let [pending-txs (queries/txs db {:status :tx.status/pending})]
      (when (seq pending-txs)
        {:web3/watch-transactions {:web3 (web3-queries/web3 db)
                                   :transactions (for [tx-hash (keys pending-txs)]
                                                   {:id (str :district.ui.web3-tx tx-hash)
                                                    :tx-hash tx-hash
                                                    :on-tx-success [::tx-success {}]
                                                    :on-tx-error [::tx-error {}]
                                                    :on-tx-receipt [::tx-receipt {}]})}}))))


(reg-event-fx
  ::watch-recommended-gas-prices
  interceptors
  (fn [{:keys [:db]}]
    (let []
      {:dispatch [::load-recommended-gas-prices]
       :dispatch-interval {:dispatch [::load-recommended-gas-prices]
                           :id ::recommended-gas-prices
                           :ms (queries/opt db :recommended-gas-prices-load-interval)}})))


(reg-event-fx
  ::stop-watching-recommended-gas-prices
  interceptors
  (fn []
    {:clear-interval {:id ::recommended-gas-prices}}))


(reg-event-fx
  ::load-recommended-gas-prices
  interceptors
  (fn [{:keys [:db]}]
    (when (w-focus-queries/focused? db)
     {:http-xhrio {:method :get
                   :uri "https://ethgasstation.info/json/ethgasAPI.json"
                   :timeout 30000
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::set-recommended-gas-prices]
                   :on-failure [::recommended-gas-prices-load-failed]}})))


(reg-event-fx
  ::set-recommended-gas-prices
  interceptors
  (fn [{:keys [:db]} [recommended-gas-prices]]
    {:db (->> recommended-gas-prices
           (transform-keys cs/->kebab-case)
           (cljs-utils/map-vals-at-keys (partial * 1e8) [:fastest :fast :average :safe-low])
           (queries/merge-recommended-gas-prices db))}))


(reg-event-fx
  ::set-recommended-gas-price-option
  [interceptors (validate-first-arg :district.ui.web3-tx/recommended-gas-price-option)]
  (fn [{:keys [:db]} [recommended-gas-price-option]]
    {:db (queries/assoc-recommended-gas-price-option db recommended-gas-price-option)}))


(reg-event-fx
  ::recommended-gas-prices-load-failed
  (constantly nil))


(reg-event-fx
  ::send-tx
  interceptors
  (fn [{:keys [:db]} [{:keys [:instance :fn :tx-opts :args] :as opts}]]
    {:web3/call
     {:web3 (web3-queries/web3 db)
      :fns [(merge opts
                   {:instance instance
                    :fn fn
                    :args args
                    :tx-opts (merge
                               {:gas-price (queries/recommended-gas-price db)}
                               tx-opts)
                    :on-tx-hash [::tx-hash opts]
                    :on-tx-hash-error [::tx-hash-error opts]
                    :on-tx-receipt [::tx-receipt opts]
                    :on-tx-success [::tx-success opts]
                    :on-tx-error [::tx-error opts]})]}}))


(defn- concat-callback-effects [callback callback-n args]
  (let [on-tx-hash-all (concat (when callback [callback]) callback-n)]
    (for [on-tx-hash on-tx-hash-all]
      (vec (concat on-tx-hash args)))))


(reg-event-fx
  ::tx-hash
  interceptors
  (fn [{:keys [:db]} [{:keys [:on-tx-hash :on-tx-hash-n]} tx-hash]]
    (merge
      {:dispatch [::add-tx tx-hash]}
      (when (or on-tx-hash on-tx-hash-n)
        {:dispatch-n (concat-callback-effects on-tx-hash on-tx-hash-n [tx-hash])}))))


(reg-event-fx
  ::tx-hash-error
  interceptors
  (fn [{:keys [:db]} [{:keys [:on-tx-hash-error :on-tx-hash-error-n]} & args]]
    (when (or on-tx-hash-error on-tx-hash-error-n)
      {:dispatch-n (concat-callback-effects on-tx-hash-error on-tx-hash-error-n args)})))


(reg-event-fx
  ::tx-success
  interceptors
  (fn [{:keys [:db]} [{:keys [:on-tx-success :on-tx-success-n]} {:keys [:transaction-hash] :as tx-receipt}]]
    (let [tx-receipt (assoc tx-receipt :status :tx.status/success)]
      (merge
        {:dispatch [::set-tx transaction-hash tx-receipt]}
        (when (or on-tx-success on-tx-success-n)
          {:dispatch-n (concat-callback-effects on-tx-success on-tx-success-n [tx-receipt])})))))


(reg-event-fx
  ::tx-error
  interceptors
  (fn [{:keys [:db]} [{:keys [:on-tx-error :on-tx-error-n]} {:keys [:transaction-hash] :as tx-receipt}]]
    (let [tx-receipt (assoc tx-receipt :status :tx.status/error)]
      (merge
        {:dispatch [::set-tx transaction-hash tx-receipt]}
        (when (or on-tx-error on-tx-error-n)
          {:dispatch-n (concat-callback-effects on-tx-error on-tx-error-n [tx-receipt])})))))


(reg-event-fx
  ::tx-receipt
  interceptors
  (fn [{:keys [:db]} [{:keys [:on-tx-receipt :on-tx-receipt-n]} {:keys [:transaction-hash] :as tx-receipt}]]
    (merge
      {:web3/call {:web3 (web3-queries/web3 db)
                   :fns [{:fn web3-eth/get-transaction
                          :args [transaction-hash]
                          :on-success [::tx-loaded transaction-hash tx-receipt]
                          :on-error [::tx-load-failed]}]}}
      (when (or on-tx-receipt on-tx-receipt-n)
        {:dispatch-n (concat-callback-effects on-tx-receipt on-tx-receipt-n [tx-receipt])}))))


(reg-event-fx
  ::tx-loaded
  interceptors
  (fn [{:keys [:db]} [tx-hash tx-receipt tx-data]]
    {:dispatch [::set-tx tx-hash (-> tx-data
                                   (update :value bn/number)
                                   (update :gas-price bn/number))]}))


(defn- merge-tx-data [db tx-hash tx-data]
  (let [new-db (queries/merge-tx-data db tx-hash tx-data)]
    (merge
      {:db new-db}
      (when-not (queries/localstorage-disabled? db)
        {:web3-tx-localstorage (select-keys new-db [:district.ui.web3-tx])}))))


(reg-event-fx
  ::add-tx
  [interceptors (validate-first-arg ::tx-hash)]
  (fn [{:keys [:db]} [tx-hash]]
    (merge-tx-data db tx-hash {:created-on (js/Date.)
                               :transaction-hash tx-hash
                               :status :tx.status/pending})))


(reg-event-fx
  ::set-tx
  [interceptors (validate-args (s/cat :tx-hash ::tx-hash
                                      :tx-data ::tx-data
                                      :args (s/* any?)))]
  (fn [{:keys [:db]} [tx-hash tx-data]]
    (merge-tx-data db tx-hash tx-data)))


(reg-event-fx
  ::remove-tx
  [interceptors (validate-first-arg ::tx-hash)]
  (fn [{:keys [:db]} [tx-hash]]
    (let [new-db (queries/remove-tx db tx-hash)]
      (merge
        {:db new-db}
        (when-not (queries/localstorage-disabled? db)
          {:web3-tx-localstorage (select-keys new-db [:district.ui.web3-tx])})))))


(reg-event-fx
  ::clear-localstorage
  (fn []
    {:web3-tx-localstorage nil}))


(reg-event-fx
  ::stop
  interceptors
  (fn [{:keys [:db]}]
    {:db (queries/dissoc-web3-tx db)
     :web3/stop-watching {:ids (map (fn [[tx-hash]]
                                      (str :district.ui.web3-tx tx-hash))
                                    (queries/txs db {:status :tx.status/pending}))}
     :forward-events {:unregister ::web3-created}
     :dispatch [::stop-watching-recommended-gas-prices]}))

