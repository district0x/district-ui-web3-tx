(ns district.ui.web3-tx.queries)

(defn txs [db]
  (-> db :district.ui.web3-tx :txs))

(defn tx [db tx-hash]
  (get (txs db) tx-hash))

(defn txs-with-status [db tx-status]
  (into {} (filter (fn [[_ tx]]
                     (= tx-status (:status tx)))
                   (txs db))))

(defn localstorage-disabled? [db]
  (-> db :district.ui.web3-tx :disable-using-localstorage?))

(defn merge-tx-data [db tx-hash tx-data]
  (update-in db [:district.ui.web3-tx :txs tx-hash] merge tx-data))

(defn merge-txs [db txs]
  (update-in db [:district.ui.web3-tx :txs] merge txs))

(defn assoc-opt [db key value]
  (assoc-in db [:district.ui.web3-tx key] value))

(defn dissoc-web3-tx [db]
  (dissoc db :district.ui.web3-tx))

