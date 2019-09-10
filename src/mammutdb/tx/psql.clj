;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2019 Andrey Antukh <niwi@niwi.nz>

(ns mammutdb.tx.psql
  "A postgresql backed transaction log implementation."
  (:require
   [clojure.spec.alpha :as s]
   [promesa.core :as p]
   [mammutdb.util.vertx-pgsql :as pg]
   [mammutdb.tx.proto :as pt]))

(declare impl-submit)
(declare impl-poll)
(declare ->Transactor)

;; --- Public API

(s/def ::pool pg/pool?)
(s/def ::pool-opts map?)
(s/def ::uri string?)
(s/def ::schema string?)

(s/def ::transactor-params
  (s/keys :opt [::pool ::pool-opts ::uri ::schema]))

(defn transactor
  [{:keys [::pool ::pool-opts ::uri] :as opts}]
  (s/assert ::transactor-params opts)
  (cond
    (pg/pool? pool)
    (let [tx (->Transactor pool opts false)]
      (pt/init! tx)
      tx)

    (string? uri)
    (let [pool (pg/pool uri pool-opts)
          tx   (->Transactor pool opts true)]
      (pt/init! tx)
      tx)

    :else
    (throw (ex-info "Invalid arguments" {:opts opts}))))

;; --- Impl

(deftype Consumer [pool]
  pt/TransactorConsumer
  (poll [_ opts]
    (impl-poll pool opts))

  java.io.Closeable
  (close [_]
    ))

(deftype Transactor [pool opts close-pool?]
  pt/Transactor
  (init! [_]
    ;; TODO: run in a transaction
    (let [schema (::schema opts "public")
          ops  [(str"create schema if not exists " schema)
                (str "create table if not exists " schema ".txlog ("
                     "  id bigserial PRIMARY KEY,"
                     "  created_at timestamptz DEFAULT CURRENT_TIMESTAMP,"
                     "  data jsonb"
                     ")")]]
      @(pg/atomic pool
         (p/run! #(pg/query pool %) ops))))

  (submit! [_ txdata]
    (p/do* (impl-submit pool txdata)))

  (consumer [_ opts]
    (->Consumer pool))

  java.io.Closeable
  (close [_]
    (when close-pool?
      (.close pool))))

(defn- impl-submit
  [pool ^bytes txdata]
  (let [sql "insert into public.txlog (data) values ($1::jsonb) returning id"
        sdata (String. txdata "UTF-8")]
    (p/map first (pg/query-one pool [sql sdata]))))

(defn- impl-poll
  [pool {:keys [offset batch]
         :or {offset 0 batch 10}
         :as opts}]
  (let [sql "select id, data, created_at from txlog where id >= $1 order by id limit $2"]
    (pg/query pool [sql offset batch])))
