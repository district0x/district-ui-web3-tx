(ns tests.runner
  (:require
    [cljs.spec.alpha :as s]
    [doo.runner :refer-macros [doo-tests]]
    [tests.all]))

(enable-console-print!)

(doo-tests 'tests.all)
