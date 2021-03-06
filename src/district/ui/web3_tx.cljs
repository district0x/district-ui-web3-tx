(ns district.ui.web3-tx
  (:require
    [akiroz.re-frame.storage :as re-frame-storage]
    [cljs.spec.alpha :as s]
    [district.ui.web3-tx.events :as events]
    [district.ui.web3]
    [mount.core :as mount :refer [defstate]]
    [re-frame.core :refer [dispatch-sync]]))

(declare start)
(declare stop)
(defstate web3-tx
  :start (start (:web3-tx (mount/args)))
  :stop (stop))


(s/def ::disable-using-localstorage? boolean?)
(s/def ::disable-loading-recommended-gas-prices? boolean?)
(s/def ::recommended-gas-prices-load-interval number?)
(s/def ::recommended-gas-price-option #{:fast :fastest :safe-low :average})

(s/def ::opts (s/nilable (s/keys :opt-un [::disable-using-localstorage?
                                          ::disable-loading-recommended-gas-prices?
                                          ::recommended-gas-prices-load-interval
                                          ::recommended-gas-price-option])))

(defn start [opts]
  (s/assert ::opts opts)
  (re-frame-storage/reg-co-fx! :district.ui.web3-tx {:fx :web3-tx-localstorage :cofx :web3-tx-localstorage})
  (dispatch-sync [::events/start opts])
  opts)


(defn stop []
  (dispatch-sync [::events/stop]))

