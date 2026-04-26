import csv
import random
import os
from datetime import datetime, timedelta

def generate_mock_data():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    users_file = os.path.join(script_dir, 'users.csv')
    products_file = os.path.join(script_dir, 'products.csv')
    orders_file = os.path.join(script_dir, 'orders.csv')
    order_items_file = os.path.join(script_dir, 'order_items.csv')

    num_users = 100_000
    num_products = 32_000
    num_orders = 5_000_000

    print("--- Starting Data Generation ---")

    # 1. GENERATE USERS
    print(f"Generating {num_users} Users...")
    with open(users_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['customer_id', 'customer_unique_id', 'zip_code', 'city', 'state'])
        for i in range(1, num_users + 1):
            c_id = f"CUST_{i}"
            # customer_unique_id represents a physical person (who might have multiple customer_ids)
            cu_id = f"USER_{random.randint(1, 80000)}" 
            writer.writerow([c_id, cu_id, '12345', 'Sao Paulo', 'SP'])

    # 2. GENERATE PRODUCTS
    print(f"Generating {num_products} Products...")
    categories = ['electronics', 'furniture', 'toys', 'sports', 'beauty', 'auto', 'health', 'fashion']
    with open(products_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['product_id', 'category_name', 'weight_g'])
        for i in range(1, num_products + 1):
            p_id = f"PROD_{i}"
            writer.writerow([p_id, random.choice(categories), random.randint(100, 5000)])

    # 3. GENERATE ORDERS & ORDER_ITEMS
    print(f"Generating {num_orders} Orders and their Order Items... (This will take a minute or two)")
    start_date = datetime(2016, 1, 1)
    end_date = datetime(2018, 12, 31)
    total_seconds = int((end_date - start_date).total_seconds())
    
    statuses = ['delivered', 'shipped', 'canceled', 'processing']
    
    with open(orders_file, 'w', newline='', encoding='utf-8') as fo, \
         open(order_items_file, 'w', newline='', encoding='utf-8') as foi:
        
        o_writer = csv.writer(fo)
        oi_writer = csv.writer(foi)
        
        o_writer.writerow(['order_id', 'customer_id', 'status', 'order_purchase_timestamp'])
        oi_writer.writerow(['order_id', 'order_item_id', 'product_id', 'price', 'freight_value'])
        
        items_count = 0
        for i in range(1, num_orders + 1):
            o_id = f"ORD_{i}"
            c_id = f"CUST_{random.randint(1, num_users)}" # Guarantees FK integrity (1 to num_users)
            
            random_seconds = random.randint(0, total_seconds)
            o_date = start_date + timedelta(seconds=random_seconds)
            o_date_str = o_date.strftime("%Y-%m-%d %H:%M:%S")
            
            status = random.choices(statuses, weights=[0.90, 0.05, 0.03, 0.02])[0]
            
            o_writer.writerow([o_id, c_id, status, o_date_str])
            
            # Generate 1 to 5 items per order, heavily weighted towards 1-2 items (Averages out to ~10M+ total rows)
            num_items = random.choices([1, 2, 3, 4, 5], weights=[0.4, 0.3, 0.15, 0.1, 0.05])[0]
            for item_id in range(1, num_items + 1):
                p_id = f"PROD_{random.randint(1, num_products)}" # Guarantees FK integrity (1 to num_products)
                price = round(random.uniform(10.0, 500.0), 2)
                freight = round(random.uniform(5.0, 50.0), 2)
                oi_writer.writerow([o_id, item_id, p_id, price, freight])
                items_count += 1
                
            if i % 1_000_000 == 0:
                print(f"  ... Processed {i} / {num_orders} orders ...")
                
    print("\n--- Data Generation Complete! ---")
    print(f"Total Users:       {num_users}")
    print(f"Total Products:    {num_products}")
    print(f"Total Orders:      {num_orders}")
    print(f"Total Order Items: {items_count}")
    print("\nGenerated Files:")
    print(f"- {users_file}")
    print(f"- {products_file}")
    print(f"- {orders_file}")
    print(f"- {order_items_file}")

if __name__ == "__main__":
    generate_mock_data()
