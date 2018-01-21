(ns district.ui.web3-tx.queries)

(def db-key :district.ui.web3-tx)

(defn txs
  ([db]
   (-> db db-key :txs))
  ([db filter-opts]
   (if filter-opts
     (into {} (filter (fn [[_ tx]]
                        (= filter-opts (select-keys tx (keys filter-opts))))
                      (txs db)))
     (txs db))))

(defn tx [db tx-hash]
  (get (txs db) tx-hash))

(defn localstorage-disabled? [db]
  (-> db db-key :disable-using-localstorage?))

(defn merge-tx-data [db tx-hash tx-data]
  (update-in db [db-key :txs tx-hash] merge tx-data))

(defn remove-tx [db tx-hash]
  (update-in db [db-key :txs] dissoc tx-hash))

(defn merge-txs [db txs]
  (update-in db [db-key :txs] merge txs))

(defn assoc-opt [db key value]
  (assoc-in db [db-key key] value))

(defn dissoc-web3-tx [db]
  (dissoc db db-key))

