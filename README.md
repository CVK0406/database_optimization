# Database Optimization for a Large-Scale E-Commerce System

This project is a technical benchmark suite built to demonstrate end-to-end database optimization techniques using the real-world **Olist dataset** (Brazilian E-Commerce). By scaling the dataset up to 5+ million orders, this project deliberately exposes system bottlenecks and provides programmatic solutions ranging from the Application Layer (Java JDBC Ingestion) to the Database Layer (PostgreSQL Indexing & Partitioning).

## 🚀 Key Benchmarks & Optimizations

1. **Application Layer (Ingestion Optimization)**
   * **Problem:** Inserting millions of CSV rows using Single Inserts causes massive latency due to network round-trips.
   * **Optimization:** Implemented JDBC Batch Processing (`addBatch()`, `executeBatch()`) combined with PostgreSQL's `reWriteBatchedInserts=true` and disabled `auto-commit` to process 10,000 records per transaction.
2. **Query Performance (Power of Indexing)**
   * **Problem:** Fetching a user's purchase history via a 4-table JOIN results in a Sequential Scan taking tens of seconds.
   * **Optimization:** Created B-Tree indexes on foreign keys and composite indexes to force an Index Scan/Bitmap Heap Scan, reducing time to milliseconds.
3. **Database Architecture (Table Partitioning)**
   * **Problem:** Calculating annual revenue aggregates over a 15M-row monolithic table causes high memory usage and slow performance, even with indexing.
   * **Optimization:** Denormalized the schema (added `order_date` to `order_items`) and applied **Range Partitioning by Year** to drastically reduce the scan footprint (Partition Pruning).
4. **Communication Algorithms (Deep Pagination)**
   * **Problem:** Navigating to page 10,000 using standard `OFFSET/LIMIT` forces the database to sort and discard 500,000 rows, spiking CPU usage.
   * **Optimization:** Implemented **Keyset Pagination (Seek Method)** using indexed timestamps to maintain O(1) performance (a few milliseconds) regardless of pagination depth.

---

## 🛠️ Technology Stack
* **Language:** Java 25 (Console Benchmark Application)
* **Database:** PostgreSQL (Databases: `ecommerce_before`, `ecommerce_after`)
* **Connection Pool:** HikariCP
* **Build Tool:** Maven
* **Data Mocking:** Python (`scripts/generate_data.py`)

---

## ⚙️ Setup Instructions

### 1. Database Initialization
1. Ensure PostgreSQL is installed and running locally on port `5432`.
2. Configure your credentials in `src/main/resources/database.properties` (see below).
3. Start the Java application and select **Option 1: SETUP** from the menu. The app will automatically create both `ecommerce_before` and `ecommerce_after` databases and execute all necessary SQL table, index, and partition creation scripts.

### 2. Configure Database Credentials
Edit the `src/main/resources/database.properties` file with your local PostgreSQL credentials:
```properties
url-ecommerce-before-db=jdbc:postgresql://localhost:5432/ecommerce_before
url-ecommerce-after-db=jdbc:postgresql://localhost:5432/ecommerce_after
username=postgres
password=your_password_here
```

### 3. Generate the Dataset
Navigate to the `scripts/` directory and execute the Python script to generate the massive 16-million row CSV files.
```bash
python scripts/generate_data.py
```
This script strictly enforces Foreign Key integrity and partition-safe timestamps.

---

## ▶️ Running the Benchmark App

This project uses a Java Console Application to intentionally eliminate network, framework, and API latency, ensuring all measurements reflect **100% true database performance**.

To run the application, open your terminal (or PowerShell) and execute:

```bash
# Compile the project
mvn clean compile

# Run the interactive console menu (Note: quotes are required if using PowerShell)
mvn exec:java "-Dexec.mainClass=Main"
```

Once running, you will be presented with an interactive menu where you can provide CSV file paths for data ingestion or input dynamic parameters (like Customer IDs and Timestamps) to execute the specific Query Benchmark scenarios.
