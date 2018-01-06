(ns district.ui.web3-tx
  (:require
    [akiroz.re-frame.storage :as re-frame-storage]
    [cljs.spec.alpha :as s]
    [cljsjs.web3]
    [district.ui.web3-tx.events :as events]
    [mount.core :as mount :refer [defstate]]
    [re-frame.core :refer [dispatch-sync]]))

(declare start)
(declare stop)
(defstate web3
  :start (start (:web3-tx (mount/args)))
  :stop (stop))

(s/def ::disable-using-localstorage? boolean?)
(s/def ::opts (s/nilable (s/keys :opt-un [::disable-using-localstorage?])))

(defn start [opts]
  (s/assert ::opts opts)
  (when-not (:disable-using-localstorage? opts)
    (re-frame-storage/reg-co-fx! :district.ui.web3-tx {:fx :web3-tx-localstorage :cofx :web3-tx-localstorage}))
  (dispatch-sync [::events/start opts])
  opts)


(defn stop []
  (dispatch-sync [::events/stop]))

