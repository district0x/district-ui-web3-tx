(ns tests.all
  (:require
   [tests.smart-contracts-test :refer [smart-contracts]]
   [district.ui.web3]
   [district.ui.smart-contracts]
   [mount.core :as mount]
   [re-frame.core :refer [reg-event-fx dispatch-sync subscribe reg-cofx reg-sub dispatch trim-v]]
   [cljs-web3.core :as web3]
   [cljs.test :refer [deftest is testing run-tests async use-fixtures]]
   [day8.re-frame.test :refer [run-test-async run-test-sync wait-for]]
   [district.ui.smart-contracts.events :as contracts-events]
   [district.ui.smart-contracts.subs :as contracts-subs]

   ;; [cljsjs.web3]

   ;; [district.ui.smart-contracts.deploy-events :as deploy-events]
   ;; [district.ui.web3-accounts.events :as accounts-events]
   ;; [district.ui.web3-accounts.subs :as accounts-subs]
   ;; [district.ui.web3-accounts]
   ;; [district.ui.web3-tx.events :as events]
   ;; [district.ui.web3-tx.subs :as subs]
   ;; [district.ui.web3-tx]
   ;; [tests.constants :refer [mintable-token]]



   ))

;; (reg-event-fx
;;  ::clear-localstorage
;;  (fn [{:keys [:db]}]
;;    {:dispatch [::events/clear-localstorage]
;;     ;; Hack so the storage gets actually cleared before tests cut off execution
;;     :dispatch-later [{:ms 50 :dispatch [::localstorage-cleared]}]}))

;; (reg-event-fx
;;  ::localstorage-cleared
;;  (constantly nil))

;; (reg-event-fx
;;  ::tx-hash
;;  (constantly nil))

;; (reg-event-fx
;;  ::tx-success
;;  (constantly nil))

;; (reg-event-fx
;;  ::tx-success-n
;;  (constantly nil))

;; (reg-event-fx
;;  ::tx-error
;;  (constantly nil))

;; (reg-event-fx
;;  ::tx-error-n
;;  (constantly nil))

(use-fixtures
  :each
  {:after
   (fn []
     (mount/stop))})

(deftest tx-success
  (let [instance (subscribe [::contracts-subs/instance :mintable-token])]
    (-> (mount/with-args
          {:web3 {:url "http://localhost:8545"}
           ;; :web3-tx {:recommended-gas-price-option :safe-low}
           :smart-contracts {;;:disable-loading-at-start? true
                             ;; :contracts-path "./"
                             :contracts-path "base/resources/public/contracts/build/"
                             :format :truffle-json
                             :contracts smart-contracts
                             }})
        (mount/start))

    (run-test-async

     (wait-for [::contracts-events/contracts-loaded]
       (prn "@@@ " @instance)
       )

     (is (= 1 1))



     )




    )


  )


;; #_(deftest tx-error
;;   (run-test-async
;;     (let [instance (subscribe [::contracts-subs/instance :mintable-token])
;;           txs (subscribe [::subs/txs])
;;           pending-txs (subscribe [::subs/txs {:status :tx.status/pending}])
;;           failed-txs (subscribe [::subs/txs {:status :tx.status/error}])
;;           accounts (subscribe [::accounts-subs/accounts])
;;           recommended-gas-price (subscribe [::subs/recommended-gas-price])]

;;       (-> (mount/with-args
;;             {:web3 {:url "http://localhost:8549"}
;;              :web3-tx {:disable-loading-recommended-gas-prices? true}
;;              :smart-contracts {:disable-loading-at-start? true
;;                                :contracts {:mintable-token mintable-token}}})
;;         (mount/start))

;;       (wait-for [::accounts-events/accounts-changed]
;;         (dispatch [::deploy-events/deploy-contract :mintable-token {:from (first @accounts)}])
;;         (wait-for [::contracts-events/set-contract ::deploy-events/contract-deploy-failed]

;;           (dispatch [::events/send-tx {:instance @instance
;;                                        :fn :mint
;;                                        :args [(second @accounts) (web3/to-wei 1 :ether)]
;;                                        :tx-opts {:from (second @accounts)}
;;                                        :on-tx-hash [::tx-hash]
;;                                        :on-tx-error [::tx-error]
;;                                        :on-tx-error-n [[::tx-error-n]]}])

;;           (wait-for [::events/tx-hash ::events/tx-hash-error]
;;             (wait-for [::events/add-tx]
;;               (wait-for [::tx-hash]
;;                 (is (= 1 (count @txs)))
;;                 (is (= @pending-txs @txs))
;;                 (wait-for [::events/tx-error ::events/tx-success]
;;                   (wait-for [::events/set-tx]
;;                     (wait-for [::tx-error]
;;                       (wait-for [::tx-error-n]
;;                         (wait-for [::events/tx-loaded]
;;                           (wait-for [::events/set-tx]
;;                             (is (= @failed-txs @txs))
;;                             (is (= 0 (count @pending-txs)))

;;                             (let [{:keys [:transaction-hash :gas-used :created-on :status :gas-price]} (last (vals @txs))]
;;                               (is (string? transaction-hash))
;;                               (is (pos? gas-used))
;;                               (is (instance? js/Date created-on))
;;                               (is (= status :tx.status/error))
;;                               (is (number? gas-price))
;;                               (is (nil? @recommended-gas-price))

;;                               (dispatch [::events/remove-tx transaction-hash])
;;                               (wait-for [::events/remove-tx]
;;                                 (is (= 0 (count @txs)))

;;                                 (dispatch [::clear-localstorage])
;;                                 (wait-for [::localstorage-cleared])))))))))))))))))
