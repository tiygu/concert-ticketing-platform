-- H2 test database schema
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL,
    password        VARCHAR(255)    NOT NULL,
    role            VARCHAR(10)     NOT NULL DEFAULT 'USER',
    phone           VARCHAR(20)     DEFAULT NULL,
    email           VARCHAR(100)    DEFAULT NULL,
    vip_level       INT             NOT NULL DEFAULT 0,
    points          INT             NOT NULL DEFAULT 0,
    status          VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS shows (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    show_name       VARCHAR(200)    NOT NULL,
    show_time       TIMESTAMP       NOT NULL,
    venue           VARCHAR(200)    DEFAULT NULL,
    price_range     VARCHAR(100)    DEFAULT NULL,
    total_seats     INT             DEFAULT NULL,
    cover_image     VARCHAR(500)    DEFAULT NULL,
    description     TEXT            DEFAULT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'UPCOMING',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT         NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tickets (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    show_id         BIGINT          NOT NULL,
    ticket_type     VARCHAR(10)     NOT NULL,
    price_type      VARCHAR(50)     DEFAULT NULL,
    seat_number     VARCHAR(50)     DEFAULT NULL,
    price           DECIMAL(10,2)   NOT NULL,
    points_price    INT             DEFAULT NULL,
    stock           INT             NOT NULL DEFAULT 0,
    sold            INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (show_id) REFERENCES shows(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(32)     NOT NULL,
    user_id         BIGINT          NOT NULL,
    show_id         BIGINT          NOT NULL,
    ticket_id       BIGINT          NOT NULL,
    seat_number     VARCHAR(50)     DEFAULT NULL,
    amount          DECIMAL(10,2)   NOT NULL,
    ticket_type     VARCHAR(10)     DEFAULT NULL,
    pay_status      VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    order_time      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pay_time        TIMESTAMP       DEFAULT NULL,
    expire_time     TIMESTAMP       DEFAULT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (order_no),
    FOREIGN KEY (user_id)  REFERENCES users(id),
    FOREIGN KEY (show_id)  REFERENCES shows(id),
    FOREIGN KEY (ticket_id) REFERENCES tickets(id)
);

CREATE TABLE IF NOT EXISTS vip_packages (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    package_name        VARCHAR(100)    NOT NULL,
    benefits            TEXT            DEFAULT NULL,
    usage_limit         VARCHAR(200)    DEFAULT NULL,
    valid_period        VARCHAR(100)    DEFAULT NULL,
    user_level_required INT             NOT NULL DEFAULT 1,
    stock               INT             DEFAULT NULL,
    booked_count        INT             NOT NULL DEFAULT 0,
    status              VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vip_bookings (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    package_id      BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    booking_time    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    use_date        DATE            DEFAULT NULL,
    audit_status    VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    admin_reply     TEXT            DEFAULT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (package_id) REFERENCES vip_packages(id),
    FOREIGN KEY (user_id)    REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notices (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200)    NOT NULL,
    content         TEXT            DEFAULT NULL,
    publish_time    TIMESTAMP       DEFAULT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    publisher_id    BIGINT          DEFAULT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (publisher_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS reviews (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    show_id         BIGINT          NOT NULL,
    content         TEXT            DEFAULT NULL,
    rating          TINYINT         DEFAULT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (user_id)  REFERENCES users(id),
    FOREIGN KEY (show_id)  REFERENCES shows(id)
);
