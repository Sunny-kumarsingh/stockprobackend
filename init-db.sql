-- StockPro Database Initialisation Script
-- Runs automatically on first MySQL container start
-- PDF §7 — MySQL (primary relational store per service)

CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS stockpro_product;
CREATE DATABASE IF NOT EXISTS stockpro_warehouse_db;
CREATE DATABASE IF NOT EXISTS stockpro_purchase_db;
CREATE DATABASE IF NOT EXISTS stockpro_supplier;
CREATE DATABASE IF NOT EXISTS stockpro_movements;
CREATE DATABASE IF NOT EXISTS stockpro_analytics_db;
CREATE DATABASE IF NOT EXISTS stockpro_alert_db;

-- Grant root access to all databases
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
