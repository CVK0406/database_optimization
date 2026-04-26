-- ==========================================
-- ECOMMERCE BEFORE (UNOPTIMIZED SCHEMA)
-- ==========================================

CREATE TABLE IF NOT EXISTS users (
    customer_id VARCHAR(50) PRIMARY KEY,
    customer_unique_id VARCHAR(50),
    zip_code VARCHAR(20),
    city VARCHAR(100),
    state VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS products (
    product_id VARCHAR(50) PRIMARY KEY,
    category_name VARCHAR(100),
    weight_g INTEGER
);

CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) REFERENCES users(customer_id),
    status VARCHAR(20),
    order_purchase_timestamp TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    order_id VARCHAR(50) REFERENCES orders(order_id),
    order_item_id INTEGER,
    product_id VARCHAR(50) REFERENCES products(product_id),
    price NUMERIC(10,2),
    freight_value NUMERIC(10,2),
    PRIMARY KEY (order_id, order_item_id)
);


-- ==========================================
-- PHASE 2 PROBLEM 1: INDEX OPTIMIZATION
-- ==========================================

-- Index on Foreign Key
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);

-- Index on customer_unique_id for the Purchase History Query
CREATE INDEX IF NOT EXISTS idx_users_customer_unique_id ON users(customer_unique_id);

-- Index on order_purchase_timestamp for Keyset Pagination (Phase 3)
CREATE INDEX IF NOT EXISTS idx_orders_purchase_timestamp ON orders(order_purchase_timestamp DESC);


-- ==========================================
-- PHASE 2 PROBLEM 2: ECOMMERCE AFTER (PARTITIONED SCHEMA)
-- ==========================================
-- Denormalization: Added order_date to both tables to enable partitioning by date.

CREATE TABLE IF NOT EXISTS orders_partitioned (
    order_id VARCHAR(50),
    customer_id VARCHAR(50),
    status VARCHAR(20),
    order_purchase_timestamp TIMESTAMP,
    order_date DATE, -- Denormalized for partitioning
    PRIMARY KEY (order_id, order_date)
) PARTITION BY RANGE (order_date);

CREATE TABLE IF NOT EXISTS order_items_partitioned (
    order_id VARCHAR(50),
    order_item_id INTEGER,
    product_id VARCHAR(50),
    price NUMERIC(10,2),
    freight_value NUMERIC(10,2),
    order_date DATE, -- Denormalized for partitioning
    PRIMARY KEY (order_id, order_item_id, order_date)
) PARTITION BY RANGE (order_date);


-- Create Partitions for Orders (Olist Dataset spans 2016-2018, adding future years for safety)
CREATE TABLE IF NOT EXISTS orders_2016 PARTITION OF orders_partitioned FOR VALUES FROM ('2016-01-01') TO ('2017-01-01');
CREATE TABLE IF NOT EXISTS orders_2017 PARTITION OF orders_partitioned FOR VALUES FROM ('2017-01-01') TO ('2018-01-01');
CREATE TABLE IF NOT EXISTS orders_2018 PARTITION OF orders_partitioned FOR VALUES FROM ('2018-01-01') TO ('2019-01-01');
CREATE TABLE IF NOT EXISTS orders_2019 PARTITION OF orders_partitioned FOR VALUES FROM ('2019-01-01') TO ('2020-01-01');
CREATE TABLE IF NOT EXISTS orders_2020_beyond PARTITION OF orders_partitioned FOR VALUES FROM ('2020-01-01') TO ('2030-01-01');

-- Create Partitions for Order Items
CREATE TABLE IF NOT EXISTS order_items_2016 PARTITION OF order_items_partitioned FOR VALUES FROM ('2016-01-01') TO ('2017-01-01');
CREATE TABLE IF NOT EXISTS order_items_2017 PARTITION OF order_items_partitioned FOR VALUES FROM ('2017-01-01') TO ('2018-01-01');
CREATE TABLE IF NOT EXISTS order_items_2018 PARTITION OF order_items_partitioned FOR VALUES FROM ('2018-01-01') TO ('2019-01-01');
CREATE TABLE IF NOT EXISTS order_items_2019 PARTITION OF order_items_partitioned FOR VALUES FROM ('2019-01-01') TO ('2020-01-01');
CREATE TABLE IF NOT EXISTS order_items_2020_beyond PARTITION OF order_items_partitioned FOR VALUES FROM ('2020-01-01') TO ('2030-01-01');

-- Note: In PostgreSQL 11+, Foreign Keys referencing partitioned tables are supported, but must include the partition key.
