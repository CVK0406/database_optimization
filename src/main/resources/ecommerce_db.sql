-- USERS table
CREATE TABLE users (
    customer_id VARCHAR(50) PRIMARY KEY,
    customer_unique_id VARCHAR(50) NOT NULL,
    zip_code VARCHAR(20),
    city VARCHAR(100),
    state VARCHAR(10)
);

-- PRODUCTS table
CREATE TABLE products (
    product_id VARCHAR(50) PRIMARY KEY,
    category_name VARCHAR(100),
    weight_g INT
);

-- ORDERS table
CREATE TABLE orders (
    order_id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    order_purchase_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_users
        FOREIGN KEY (customer_id) REFERENCES users(customer_id)
        ON DELETE CASCADE
);

-- ORDER_ITEMS table
CREATE TABLE order_items (
    order_id VARCHAR(50),
    order_item_id INT,
    product_id VARCHAR(50) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    freight_value DECIMAL(10, 2),
    PRIMARY KEY (order_id, order_item_id),
    CONSTRAINT fk_items_orders 
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_items_products 
        FOREIGN KEY (product_id) REFERENCES products(product_id)
);