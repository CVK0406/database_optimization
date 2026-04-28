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
-- INDEXES FOR ECOMMERCE_BEFORE (FOR SCENARIO 2: INDEX ONLY)
-- ==========================================
CREATE INDEX IF NOT EXISTS idx_users_covering ON users(customer_unique_id) INCLUDE (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_purchase_timestamp ON orders(order_purchase_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);