# district-ui-web3-tx

[![Build Status](https://travis-ci.org/district0x/district-ui-web3-tx.svg?branch=master)](https://travis-ci.org/district0x/district-ui-web3-tx)

Clojurescript [mount](https://github.com/tolitius/mount) + [re-frame](https://github.com/Day8/re-frame) module for a district UI,
that helps managing [web3](https://github.com/ethereum/web3.js/) smart-contract transactions in following ways:   
* Serves as central place to fire re-frame events after all transaction related events. Other modules can then easily hook into those events and provide
additional features on top of it. Example of such module is [district-ui-web3-tx-log-core](https://github.com/district0x/district-ui-web3-tx-log-core). 
* It stores transaction data in browser's localstorage, so they're persisted between sessions.   

## Installation
Add `[district0x/district-ui-web3-tx "1.0.7"]` into your project.clj  
Include `[district.ui.web3-tx]` in your CLJS file, where you use `mount/start`

## API Overview

**Warning:** district0x modules are still in early stages, therefore API can change in a future.

- [district.ui.web3-tx](#districtuiweb3-tx)
- [district.ui.web3-tx.subs](#districtuiweb3-txsubs)
  - [::txs](#txs-sub)
  - [::tx](#tx-sub)
- [district.ui.web3-tx.events](#districtuiweb3-txevents)
  - [::send-tx](#send-tx)
  - [::watch-pending-txs](#watch-pending-txs)
  - [::tx-hash](#tx-hash)
  - [::tx-hash-error](#tx-hash-error)
  - [::tx-success](#tx-success)
  - [::tx-error](#tx-error)
  - [::tx-receipt](#tx-receipt)
  - [::tx-loaded](#tx-loaded)
  - [::add-tx](#add-tx)
  - [::set-tx](#set-tx)
  - [::remove-tx](#remove-tx)
  - [::clear-localstorage](#clear-localstorage)
- [district.ui.web3-tx.queries](#districtuiweb3-txqueries)
  - [txs](#txs)
  - [tx](#tx)
  - [localstorage-disabled?](#localstorage-disabled?)
  - [merge-tx-data](#merge-tx-data)
  - [remove-tx](#remove-tx)
  - [merge-txs](#merge-txs)
  - [assoc-opt](#assoc-opt)

## district.ui.web3-tx
This namespace contains web3-tx [mount](https://github.com/tolitius/mount) module.

You can pass following args to initiate this module: 
* `:disable-using-localstorage?` Pass true if you don't want to store transaction data in a browser's localstorage


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

#### <a name="txs-sub">`txs [filter-opts]`
Returns map of all transactions. Optionally, you can provide filter opts if you want to filter only transactions with a specific property in 
tx data. For example it can be `:status`, `:from`, `:to`.  
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

#### <a name="tx-sub">`tx [tx-hash]`
Returns transaction with transaction hash `tx-hash`

## district.ui.web3-tx.events
re-frame events provided by this module:

#### <a name="send-tx">`send-tx [opts]`
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

#### <a name="watch-pending-txs">`watch-pending-txs`
Starts watching currently pending transactions. This event is fired at mount start.

#### <a name="tx-hash">`tx-hash`
Event fired when a transaction was sent and transaction hash was obtained. Use this event to hook into event flow.  

#### <a name="tx-hash-error">`tx-hash-error`
Event fired when there was an error sending transaction. Use this event to hook into event flow.

#### <a name="tx-success">`tx-success`
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

#### <a name="tx-error">`tx-error`
Event fired when there was an error processing a tx. Use this event to hook into event flow.

#### <a name="tx-receipt">`tx-receipt`
Event fired when receipt for a tx was loaded. No matter if tx succeeded or failed. Use this event to hook into event flow.

#### <a name="tx-loaded">`tx-loaded`
After tx-receipt is fired, this module will also load a transaction (`web3.eth.getTransaction`). This event is fired when
a transaction is loaded. Use this event to hook into event flow.

#### <a name="add-tx">`add-tx [tx-hash]`
Adds new transaction hash into db, sets it as `:tx.status/pending`. 

#### <a name="set-tx">`set-tx [tx-hash tx-data]`
Updates a transaction.

#### <a name="remove-tx">`remove-tx [tx-hash]`
Removes transaction.  

#### <a name="clear-localstorage">`clear-localstorage`
Clears transactions from localstorage.

## district.ui.web3-tx.queries
DB queries provided by this module:  
*You should use them in your events, instead of trying to get this module's 
data directly with `get-in` into re-frame db.*

#### <a name="txs">`txs [db]`
Works the same way as sub `::txs`

#### <a name="tx">`tx [db tx-hash]`
Works the same way as sub `::tx`

#### <a name="localstorage-disabled?">`localstorage-disabled? [db]`
Returns true is using localstorage is disabled. 

#### <a name="merge-tx-data">`merge-tx-data [db tx-hash tx-data]`
Merges tx data into a transaction with hash `tx-hash` and returns new re-frame db.

#### <a name="remove-tx">`remove-tx [db tx-hash]`
Removes transaction and returns new re-frame db.

#### <a name="merge-txs">`merge-txs [db txs]`
Merges transactions and returns new re-frame db.

#### <a name="assoc-opt">`assoc-opt [db key value]`
Associates an opt into this module state. For internal purposes mainly.

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