
DROP DATABASE IF EXISTS shopsphere;
CREATE DATABASE shopsphere CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shopsphere;

CREATE TABLE users (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,           -- PBKDF2 hash, never plain text
    mobile      VARCHAR(15),
    role        VARCHAR(10)  NOT NULL DEFAULT 'CUSTOMER',   -- CUSTOMER | ADMIN
    status      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE addresses (
    address_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    address_line  VARCHAR(255) NOT NULL,
    city          VARCHAR(80)  NOT NULL,
    state         VARCHAR(80)  NOT NULL,
    pincode       VARCHAR(10)  NOT NULL,
    address_type  VARCHAR(20)  NOT NULL DEFAULT 'HOME',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE categories (
    category_id  INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL UNIQUE,
    description  VARCHAR(255),
    status       TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE products (
    product_id   INT AUTO_INCREMENT PRIMARY KEY,
    category_id  INT NOT NULL,
    name         VARCHAR(150) NOT NULL,
    brand        VARCHAR(80),
    description  TEXT,
    price        DECIMAL(10,2) NOT NULL,
    discount     DECIMAL(5,2)  NOT NULL DEFAULT 0,      -- percentage
    stock        INT NOT NULL DEFAULT 0,
    image_url    VARCHAR(255),
    status       TINYINT(1) NOT NULL DEFAULT 1,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(category_id),
    INDEX idx_product_name (name)
);

CREATE TABLE cart (
    cart_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    product_id  INT NOT NULL,
    quantity    INT NOT NULL DEFAULT 1,
    UNIQUE KEY uq_cart (user_id, product_id),
    FOREIGN KEY (user_id)    REFERENCES users(user_id)       ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE wishlist (
    wishlist_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    product_id  INT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_wishlist (user_id, product_id),
    FOREIGN KEY (user_id)    REFERENCES users(user_id)       ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE orders (
    order_id        INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL,
    address_id      INT NOT NULL,
    total_amount    DECIMAL(10,2) NOT NULL,
    discount        DECIMAL(10,2) NOT NULL DEFAULT 0,
    payment_method  VARCHAR(20) NOT NULL,
    payment_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    order_status    VARCHAR(20) NOT NULL DEFAULT 'PLACED',   -- PLACED|PACKED|SHIPPED|DELIVERED|CANCELLED
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)    REFERENCES users(user_id),
    FOREIGN KEY (address_id) REFERENCES addresses(address_id)
);

CREATE TABLE order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id      INT NOT NULL,
    product_id    INT NOT NULL,
    quantity      INT NOT NULL,
    price         DECIMAL(10,2) NOT NULL,                    -- unit price at purchase time
    FOREIGN KEY (order_id)   REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

CREATE TABLE payments (
    payment_id            INT AUTO_INCREMENT PRIMARY KEY,
    order_id              INT NOT NULL,
    payment_method        VARCHAR(20) NOT NULL,
    transaction_reference VARCHAR(60),
    amount                DECIMAL(10,2) NOT NULL,
    payment_status        VARCHAR(20) NOT NULL,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

CREATE TABLE reviews (
    review_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    product_id   INT NOT NULL,
    rating       INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_text  VARCHAR(500),
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_review (user_id, product_id),
    FOREIGN KEY (user_id)    REFERENCES users(user_id)       ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE coupons (
    coupon_id        INT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(30) NOT NULL UNIQUE,
    discount_type    VARCHAR(10) NOT NULL,                   -- PERCENT | FLAT
    discount_value   DECIMAL(10,2) NOT NULL,
    minimum_order    DECIMAL(10,2) NOT NULL DEFAULT 0,
    maximum_discount DECIMAL(10,2) NOT NULL DEFAULT 0,       -- 0 = no cap
    expiry_date      DATE,
    status           TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE coupon_usage (
    usage_id   INT AUTO_INCREMENT PRIMARY KEY,
    coupon_id  INT NOT NULL,
    user_id    INT NOT NULL,
    order_id   INT NOT NULL,
    used_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (coupon_id) REFERENCES coupons(coupon_id),
    FOREIGN KEY (user_id)   REFERENCES users(user_id),
    FOREIGN KEY (order_id)  REFERENCES orders(order_id) ON DELETE CASCADE
);

-- ------------------------- SEED DATA -------------------------
INSERT INTO users (name, email, password, mobile, role) VALUES
('Store Admin', 'admin@shopsphere.com',
 'pbkdf2$65536$OXXpbiXJYE5yaCTo6drB/g==$f6YS1Ucwgf40Ex1tFhdvHAE7EP7qnYw7lacF6SConc8=', '9999900001', 'ADMIN'),
('Demo Customer', 'demo@shopsphere.com',
 'pbkdf2$65536$wj93YTz9HcmbFUkdzeJMiA==$6PWnnhaCIToiOEb6mfCa57pv2cei+XiiNHYyyAdVPWk=', '9999900002', 'CUSTOMER');

INSERT INTO addresses (user_id, address_line, city, state, pincode, address_type) VALUES
(2, '12, Malviya Nagar', 'Jaipur', 'Rajasthan', '302017', 'HOME');

INSERT INTO categories (name, description) VALUES
('Electronics', 'Phones, audio and accessories'),
('Fashion', 'Clothing and footwear'),
('Books', 'Textbooks and novels'),
('Home & Kitchen', 'Appliances and home essentials'),
('Sports', 'Fitness and outdoor gear');

INSERT INTO products (category_id, name, brand, description, price, discount, stock, image_url) VALUES
(1, 'Wireless Earbuds Pro',    'SoundMax', 'Bluetooth 5.3 earbuds with noise cancellation and 24h battery.', 2999.00, 20, 50, 'images/placeholder.svg'),
(1, 'Smartphone X12 128GB',    'Nexa',     '6.5-inch AMOLED display, 50MP camera, 5000mAh battery.',        15999.00, 10, 25, 'images/placeholder.svg'),
(1, 'Fast Charger 33W',        'VoltEdge', 'USB-C fast charger with over-heat protection.',                   799.00, 15, 120,'images/placeholder.svg'),
(1, 'Smart Watch Active',      'Nexa',     'Heart-rate, SpO2 and sleep tracking. 7 day battery.',            3499.00, 25, 8,  'images/placeholder.svg'),
(2, 'Cotton Casual T-Shirt',   'UrbanWear','100% cotton regular-fit t-shirt.',                                 599.00, 30, 200,'images/placeholder.svg'),
(2, 'Running Shoes Flex',      'StrideOn', 'Lightweight breathable running shoes.',                           2499.00, 20, 40, 'images/placeholder.svg'),
(2, 'Denim Jacket',            'UrbanWear','Classic blue denim jacket.',                                      2199.00, 10, 15, 'images/placeholder.svg'),
(3, 'Advanced Java Programming','RTU Press','Servlets, JSP, JDBC, RMI and networking explained with examples.', 549.00,  5, 60, 'images/placeholder.svg'),
(3, 'Data Structures in Java', 'RTU Press','Core data structures with practice problems.',                     499.00,  0, 75, 'images/placeholder.svg'),
(3, 'Clean Code',              'Pearson',  'A handbook of agile software craftsmanship.',                      899.00, 15, 30, 'images/placeholder.svg'),
(4, 'Electric Kettle 1.5L',    'HomeEase', 'Stainless steel kettle with auto shut-off.',                      1099.00, 25, 70, 'images/placeholder.svg'),
(4, 'Non-stick Cookware Set',  'HomeEase', '3-piece non-stick cookware set.',                                 1999.00, 35, 35, 'images/placeholder.svg'),
(4, 'LED Desk Lamp',           'BrightUp', 'Adjustable LED lamp with 3 colour modes.',                         899.00, 10, 5,  'images/placeholder.svg'),
(5, 'Yoga Mat 6mm',            'FlexFit',  'Anti-slip eco-friendly yoga mat.',                                 699.00, 20, 90, 'images/placeholder.svg'),
(5, 'Badminton Racket Pair',   'SmashPro', 'Graphite racket set with carry cover.',                           1299.00, 15, 45, 'images/placeholder.svg'),
(5, 'Stainless Steel Bottle',  'FlexFit',  '750 ml insulated bottle, 12h cold / 8h hot.',                      549.00, 10, 0,  'images/placeholder.svg');

INSERT INTO coupons (code, discount_type, discount_value, minimum_order, maximum_discount, expiry_date) VALUES
('WELCOME10', 'PERCENT', 10, 500,  500, '2030-12-31'),
('FLAT200',   'FLAT',   200, 1500,   0, '2030-12-31'),
('EXPIRED5',  'PERCENT',  5,   0,    0, '2020-01-01');

INSERT INTO reviews (user_id, product_id, rating, review_text) VALUES
(2, 1, 5, 'Great sound and battery life!'),
(2, 8, 4, 'Very helpful for the Advanced Java syllabus.');
