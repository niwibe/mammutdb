(ns mammutdb.test-config
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [mammutdb.config :as config]
            [cats.core :as m]))

(deftest config
  (config/setup-config! "test/testconfig.edn")

  (testing "Read config file"
    (let [conf (config/read-config "test/testconfig.edn")]
      (is (:transport conf))
      (is (:storage conf)))))
