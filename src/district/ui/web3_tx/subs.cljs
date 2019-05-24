(ns district.ui.web3-tx.subs
  (:require
    [district.ui.web3-tx.queries :as queries]
    [re-frame.core :refer [reg-sub]]))

(defn- sub-fn [query-fn]
  (fn [db [_ & args]]
    (apply query-fn db args)))

(reg-sub
  ::txs
  (sub-fn queries/txs))

(reg-sub
  ::tx
  (sub-fn queries/tx))

(reg-sub
  ::recommended-gas-prices
  (sub-fn queries/recommended-gas-prices))

(reg-sub
  ::recommended-gas-price-option
  (sub-fn queries/recommended-gas-price-option))

(reg-sub
  ::recommended-gas-price
  (sub-fn queries/recommended-gas-price))

