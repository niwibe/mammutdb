CREATE TABLE %s (
       id uuid DEFAULT uuid_generate_v1() UNIQUE PRIMARY KEY,
       data json DEFAULT '{}'::json,
       revision bigint DEFAULT 1,
       created_at timestamp with time zone DEFAULT now(),
);


