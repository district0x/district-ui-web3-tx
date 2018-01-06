# district-ui-web3-tx

[![Build Status](https://travis-ci.org/district0x/district-ui-web3-tx.svg?branch=master)](https://travis-ci.org/district0x/district-ui-web3-tx)

Clojurescript [mount](https://github.com/tolitius/mount) + [re-frame](https://github.com/Day8/re-frame) module for a district UI,
that handles [web3](https://github.com/ethereum/web3.js/) smart-contract transactions.  

## Installation
Add `[district0x/district-ui-web3-tx "1.0.4"]` into your project.clj  
Include `[district.ui.web3-tx]` in your CLJS file, where you use `mount/start`

**Warning:** district0x modules are still in early stages, therefore API can change in a future.

## district.ui.web3-tx
This namespace contains web3-tx [mount](https://github.com/tolitius/mount) module.

You can pass following args to initiate this module: 
* `:disable-using-localstorage?` Pass true if you don't want to store transaction receipts in a browser's localstorage


```clojure
  (ns my-district.core
    (:require [mount.core :as mount]
              [district.ui.web3-tx]))
              
  (-> (mount/with-args
        {:web3 {:url "https://mainnet.infura.io/"}
         :web3-tx {:disable-using-localstorage? true}})
    (mount/start))
```

## district.ui.web3-tx.subs
re-frame subscriptions provided by this module:

#### `::txs [filter-opts]`
Returns all transactions. Optionally, you can provide filter opts if you want to filter only transactions with a specific property in 
tx receipt. For example it can be `:status`, `:from`, `:to`.  
There are 3 possible transaction statuses:  
* `:tx.status/success`
* `:tx.status/pending`
* `:tx.status/error`

```clojure
(ns my-district.core
    (:require [mount.core :as mount]
              [district.ui.web3-tx :as subs]))
  
  (defn home-page []
    (let [pending-txs (subscribe [::subs/txs {:status :tx.status/pending}])]  
      (fn []
        [:div "Your pending transactions: "]
        (for [[tx-hash tx] @pending-txs]
          [:div 
            {:key tx-hash}
            "Transaction hash: " tx-hash]))))
```

#### `::tx [tx-hash]`
Returns transaction with transaction hash `tx-hash`

## district.ui.web3-tx.events
re-frame events provided by this module:

#### `::start [opts]`
Event fired at mount start.

#### `::send-tx [opts]`
Sends Ethereum transaction. Pass same arguments as you'd pass to [web3/call](https://github.com/district0x/re-frame-web3-fx#web3call)
for state changing contract function. 

```clojure
(dispatch [::events/send-tx {:instance MintableToken
                             :fn :mint
                             :args [(first accounts) (web3/to-wei 1 :ether)]
                             :tx-opts {:from (first accounts) :gas 4500000}
                             :on-tx-hash [::tx-hash]
                             :on-tx-hash-error [::tx-hash-error]
                             :on-tx-success [::tx-success]
                             :on-tx-error [::tx-error]}])
```

#### `::watch-pending-txs`
Starts watching currently pending transactions. This event is fired at mount start.

#### `::tx-hash`
Event fired when a transaction was sent and transaction hash was obtained. Use this event to hook into event flow.  

#### `::tx-hash-error`
Event fired when there was an error sending transaction. Use this event to hook into event flow.

#### `::tx-success`
Event fired when transaction was successfully processed. Use this event to hook into event flow.

```clojure
(ns my-district.events
    (:require [district.ui.web3-tx.events :as tx-events]
              [re-frame.core :refer [reg-event-fx]]
              [day8.re-frame.forward-events-fx]))
              
(reg-event-fx
  ::my-event
  (fn []
    {:register :my-forwarder
     :events #{::tx-events/tx-success}
     :dispatch-to [::do-something-after-tx-success]}))
```

#### `::tx-error`
Event fired when there was an error processing a tx. Use this event to hook into event flow.

#### `::tx-receipt`
Event fired when receipt for a tx was loaded. No matter if tx succeeded or failed. Use this event to hook into event flow.

#### `::add-tx [tx-hash]`
Adds new transaction hash into db, sets it as `:tx.status/pending`. 

#### `::set-tx [tx-hash tx-data]`
Updates a transaction. This is called when tx receipt is loaded.

#### `::remove-tx [tx-hash]`
Removes transaction.  

#### `::clear-localstorage`
Clears transactions from localstorage.

#### `::stop`
Cleanup event fired on mount stop.

## district.ui.web3-tx.queries
DB queries provided by this module:  
*You should use them in your events, instead of trying to get this module's 
data directly with `get-in` into re-frame db.*

#### `txs [db]`
Works the same way as sub `::txs`

#### `tx [db tx-hash]`
Works the same way as sub `::tx`

#### `localstorage-disabled? [db]`
Returns true is using localstorage is disabled. 

#### `merge-tx-data [db tx-hash tx-data]`
Merges tx data into a transaction with hash `tx-hash` and returns new re-frame db.

#### `remove-tx [db tx-hash]`
Removes transaction and returns new re-frame db.

#### `merge-txs [db txs]`
Merges transactions and returns new re-frame db.

#### `assoc-opt [db key value]`
Associates an opt into this module state. For internal purposes mainly.

#### `dissoc-web3-tx [db]`
Cleans up this module from re-frame db. 

## Dependency on other district UI modules
* [district-ui-web3](https://github.com/district0x/district-ui-web3)

## Development
```bash
lein deps
# Start ganache blockchain with 1s block time
ganache-cli -p 8549 -b 1
# To run tests and rerun on changes
lein doo chrome tests
```