(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh)]
            [mammutdb.cli :as cli]
            [clojure.test :refer [run-tests]])
  (:refer-clojure :exclude [test]))

(def system nil)

(defn init []
  (alter-var-root #'system
    (constantly (cli/system "test/testconfig.edn"))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
    (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))

(defn test
  ([]
   (refresh)
   (run-tests 'mammutdb.test-api
              'mammutdb.test-auth
              'mammutdb.test-config
              'mammutdb.test-query
              'mammutdb.test-storage
              'mammutdb.test-transaction))
  ([& namespaces]
     (apply run-tests namespaces)))
