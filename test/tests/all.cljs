(ns tests.all
  (:require [cljs-web3-next.core :as web3]
            [cljs.test :refer [deftest is testing run-tests async use-fixtures]]
            [day8.re-frame.test :refer [run-test-async run-test-sync wait-for]]
            [district.ui.smart-contracts.events :as contracts-events]
            [district.ui.smart-contracts.subs :as contracts-subs]
            [district.ui.smart-contracts]
            [district.ui.web3-accounts.events :as accounts-events]
            [district.ui.web3-accounts.subs :as accounts-subs]
            [district.ui.web3-accounts]
            [district.ui.web3-tx.events :as events]
            [district.ui.web3-tx.subs :as subs]
            [district.ui.web3-tx]
            [district.ui.web3.subs :as web3-subs]
            [district.ui.web3]
            [mount.core :as mount]
            [re-frame.core :refer [reg-event-fx dispatch-sync subscribe reg-cofx reg-sub dispatch trim-v]]
            [tests.smart-contracts-test :refer [smart-contracts]]))

(reg-event-fx
 ::clear-localstorage
 (fn [{:keys [:db]}]
   {:dispatch [::events/clear-localstorage]
    ;; Hack so the storage gets actually cleared before tests cut off execution
    :dispatch-later [{:ms 50 :dispatch [::localstorage-cleared]}]}))

(reg-event-fx
 ::localstorage-cleared
 (constantly nil))

(reg-event-fx
 ::tx-hash
 (constantly nil))

(reg-event-fx
 ::tx-success
 (constantly nil))

(reg-event-fx
 ::tx-success-n
 (constantly nil))

(reg-event-fx
 ::tx-error
 (constantly nil))

(reg-event-fx
 ::tx-error-n
 (constantly nil))

(use-fixtures
  :each
  {:after
   (fn []
     (mount/stop))})

(defn all-events
  [events]
  (let [triggered (atom (set []))]
  (fn [event]
    (swap! triggered conj (first event))
    (every? @triggered events))))


;; IMPORTANT, for these tests to pass make sure your ganache config specifies a blocktime greater than zero
;; E.g., launching ganache with the params "-b 10" or "--blocktime 10"
;; Otherwise, not all expected events might be triggered. Automatic mining makes the block to be committed "too fast"
;; and the on-success event might come before the on-hash, which wouldn't happen in a real setup.

(deftest tx-success
  (let [instance (subscribe [::contracts-subs/instance :mintable-token])
        accounts (subscribe [::accounts-subs/accounts])
        web3 (subscribe [::web3-subs/web3])
        txs (subscribe [::subs/txs])
        pending-txs (subscribe [::subs/txs {:status :tx.status/pending}])
        success-txs (subscribe [::subs/txs {:status :tx.status/success}])]

    (-> (mount/with-args
          {:web3 {:url "http://localhost:8549"}
           :web3-accounts {:eip55? true}
           :web3-tx {:disable-loading-recommended-gas-prices? true}
           :smart-contracts {:disable-loading-at-start? false
                             :contracts-path "contracts/build/"
                             :format :truffle-json
                             :contracts smart-contracts}})
        (mount/start))

    (run-test-async
     (wait-for [::contracts-events/contracts-loaded]
       (is (not (nil? @instance)))
       (wait-for [::accounts-events/accounts-changed]
         (is (not (empty? @accounts)))
         (is (not (nil? @web3)))
         (let [initial-txs-count (count @txs)]
           (dispatch [::events/send-tx {:instance @instance
                                        :fn :mint
                                        :args [(first @accounts) (web3/to-wei "1" :ether)]
                                        :tx-opts {:from (first @accounts)}
                                        :on-tx-hash [::tx-hash]
                                        :on-tx-success [::tx-success]
                                        :on-tx-success-n [[::tx-success-n]]}])
           (wait-for [(all-events
                        [::events/tx-hash
                         ::events/add-tx
                         ::tx-hash]) ::events/tx-hash-error]
             (is (= (inc initial-txs-count) (count @txs)))
             (is (= (last @pending-txs) (last @txs)))

             (wait-for [(all-events
                          [::events/tx-success
                           ::events/tx-receipt
                           ::tx-success-n
                           ::events/set-tx
                           ::events/tx-loaded
                           ]) ::events/tx-load-failed]
                       (wait-for [::events/set-tx]
                         (is (= (last @success-txs) (last @txs)))
                         (is (= 0 (count @pending-txs)))
                         (let [{:keys [:transaction-hash :gas-used :created-on :status :gas-price] :as tx} (first (vals @txs))]
                           (is (string? transaction-hash))
                           (is (pos? gas-used))
                           (is (= status :tx.status/success))
                           (is (= tx @(subscribe [::subs/tx transaction-hash])))
                           (is (number? gas-price))
                           (dispatch [::clear-localstorage])
                           (wait-for [::localstorage-cleared])))))))))))

(deftest tx-error
  (let [instance (subscribe [::contracts-subs/instance :mintable-token])
        txs (subscribe [::subs/txs])
        pending-txs (subscribe [::subs/txs {:status :tx.status/pending}])
        failed-txs (subscribe [::subs/txs {:status :tx.status/error}])
        accounts (subscribe [::accounts-subs/accounts])
        recommended-gas-price (subscribe [::subs/recommended-gas-price])]

    (-> (mount/with-args
          {:web3 {:url "http://localhost:8549"}
           :web3-accounts {:eip55? true}
           :web3-tx {:disable-loading-recommended-gas-prices? true}
           :smart-contracts {:disable-loading-at-start? false
                             :contracts-path "contracts/build/"
                             :format :truffle-json
                             :contracts smart-contracts}})
        (mount/start))

    (run-test-async
     (wait-for [::contracts-events/contracts-loaded]
       (wait-for [::accounts-events/accounts-changed]
         (let [initial-txs-count (count @txs)]
           (dispatch [::events/send-tx {:instance @instance
                                        :fn :mint
                                        :args [(first @accounts) (web3/to-wei "1" :ether)]
                                        :tx-opts {:from (second @accounts)}
                                        :on-tx-hash [::tx-hash]
                                        :on-tx-error [::tx-error]
                                        :on-tx-error-n [[::tx-error-n]]}])

           (wait-for [(all-events
                        [::events/tx-hash
                         ::events/add-tx
                         ::tx-hash]) ::events/tx-hash-error]
             (is (= (inc initial-txs-count) (count @txs)))
             (is (= (last @pending-txs) (last @txs)))
             (wait-for [(all-events
                          [::events/tx-error
                           ::tx-error
                           ::tx-error-n
                           ::events/set-tx
                           ::events/tx-loaded
                           ]) ::events/tx-success]
                       (wait-for [::events/set-tx]
                         (is (= @failed-txs @txs))
                         (is (= 0 (count @pending-txs)))
                         (let [{:keys [:transaction-hash :gas-used :created-on :status :gas-price]} (last (vals @txs))]
                           (is (string? transaction-hash))
                           (is (pos? gas-used))
                           (is (instance? js/Date created-on))
                           (is (= status :tx.status/error))
                           (is (number? gas-price))
                           (is (nil? @recommended-gas-price))
                           (dispatch [::events/remove-tx transaction-hash])
                           (wait-for [::events/remove-tx]
                             (is (= 0 (count @txs)))
                             (dispatch [::clear-localstorage])
                             (wait-for [::localstorage-cleared]))))))))))))

(deftest tx-hash-error
  (let [instance (subscribe [::contracts-subs/instance :mintable-token])
        txs (subscribe [::subs/txs])
        accounts (subscribe [::accounts-subs/accounts])]

    (-> (mount/with-args
          {:web3 {:url "http://localhost:8549"}
           :web3-accounts {:eip55? true}
           :web3-tx {:disable-loading-recommended-gas-prices? true}
           :smart-contracts {:disable-loading-at-start? false
                             :contracts-path "contracts/build/"
                             :format :truffle-json
                             :contracts smart-contracts}})
        (mount/start))

    (run-test-async
     (wait-for [::contracts-events/contracts-loaded]
       (wait-for [::accounts-events/accounts-changed]
         (let [initial-txs-count (count @txs)]
         (dispatch [::events/send-tx {:instance @instance
                                      :fn :mint
                                      :args [(first @accounts) (web3/to-wei "1" :ether)]
                                      :tx-opts {:from "0x0000000000000000000000000000000000000000"}
                                      :on-tx-hash [::tx-hash]
                                      :on-tx-hash-error [::tx-error]
                                      :on-tx-hash-error-n [[::tx-error-n]]}])

         (wait-for [(all-events
                      [::events/tx-hash-error
                       ::tx-error
                       ::tx-error-n])
                    #{::events/tx-hash
                      ::events/tx-success
                      ::events/tx-receipt
                      ::events/tx-error}]
           (is (= initial-txs-count (count @txs))))))))))
