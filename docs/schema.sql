-- ============================================================
-- 大型演唱会票务预订与VIP服务平台 — 数据库DDL
-- MySQL 5.7+ / InnoDB / utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS concert_ticketing
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE concert_ticketing;

-- ============================================================
-- 1. 用户表（合并原 user + admin，role 字段区分角色）
-- ============================================================
CREATE TABLE users (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    username        VARCHAR(50)     NOT NULL                 COMMENT '用户名',
    password        VARCHAR(255)    NOT NULL                 COMMENT 'BCrypt加密密码',
    role            ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN=管理员, USER=普通/VIP用户',
    phone           VARCHAR(20)     DEFAULT NULL             COMMENT '手机号',
    email           VARCHAR(100)    DEFAULT NULL             COMMENT '邮箱',
    vip_level       INT             NOT NULL DEFAULT 0       COMMENT 'VIP等级, 0=普通用户',
    points          INT             NOT NULL DEFAULT 0       COMMENT '积分余额',
    status          ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '软删除标记',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_users_username (username),
    INDEX idx_users_role (role),
    INDEX idx_users_phone (phone),
    INDEX idx_users_vip_level (vip_level),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表（含管理员）';

-- ============================================================
-- 2. 演出场次表
-- ============================================================
CREATE TABLE shows (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '演出ID',
    show_name       VARCHAR(200)    NOT NULL                 COMMENT '演出名称',
    show_time       DATETIME        NOT NULL                 COMMENT '演出时间',
    venue           VARCHAR(200)    DEFAULT NULL             COMMENT '场馆信息',
    price_range     VARCHAR(100)    DEFAULT NULL             COMMENT '票价档位概述, 如 "280-1280"',
    total_seats     INT             DEFAULT NULL             COMMENT '座位总数',
    cover_image     VARCHAR(500)    DEFAULT NULL             COMMENT '封面图URL',
    description     TEXT            DEFAULT NULL             COMMENT '演出简介',
    status          ENUM('UPCOMING','ON_SALE','SOLD_OUT','ENDED','CANCELLED')
                                    NOT NULL DEFAULT 'UPCOMING' COMMENT '演出状态',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_shows_show_time (show_time),
    INDEX idx_shows_status (status),
    INDEX idx_shows_show_name (show_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出场次表';

-- ============================================================
-- 3. 选票表（合并原 ticket_regular + ticket_vip，ticket_type 区分）
-- ============================================================
CREATE TABLE tickets (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '选票ID',
    show_id         BIGINT          NOT NULL                 COMMENT '演出ID',
    ticket_type     ENUM('REGULAR','VIP') NOT NULL           COMMENT '票种: REGULAR=普通票, VIP=VIP票',
    price_type      VARCHAR(50)     DEFAULT NULL             COMMENT '票价类型, 如"看台""内场""VIP区"',
    seat_number     VARCHAR(50)     DEFAULT NULL             COMMENT '座位编号',
    price           DECIMAL(10,2)   NOT NULL                 COMMENT '票价',
    points_price    INT             DEFAULT NULL             COMMENT 'VIP积分兑换价, 仅VIP票使用',
    stock           INT             NOT NULL DEFAULT 0       COMMENT '当前库存',
    sold            INT             NOT NULL DEFAULT 0       COMMENT '已售数量',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_tickets_show_id (show_id),
    INDEX idx_tickets_ticket_type (ticket_type),
    INDEX idx_tickets_seat_number (seat_number),
    INDEX idx_tickets_show_type (show_id, ticket_type),
    CONSTRAINT fk_tickets_shows FOREIGN KEY (show_id) REFERENCES shows(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选票表（普通票+VIP票合并）';

-- ============================================================
-- 4. 订单表（外键简化为单一 ticket_id）
-- ============================================================
CREATE TABLE orders (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '订单ID',
    order_no        VARCHAR(32)     NOT NULL                 COMMENT '业务订单号(雪花ID或时间戳生成)',
    user_id         BIGINT          NOT NULL                 COMMENT '用户ID',
    show_id         BIGINT          NOT NULL                 COMMENT '演出ID',
    ticket_id       BIGINT          NOT NULL                 COMMENT '选票ID',
    seat_number     VARCHAR(50)     DEFAULT NULL             COMMENT '座位编号(冗余,便于查询)',
    amount          DECIMAL(10,2)   NOT NULL                 COMMENT '订单金额',
    ticket_type     ENUM('REGULAR','VIP') DEFAULT NULL       COMMENT '票种(冗余,便于统计)',
    pay_status      ENUM('PENDING','PAID','COMPLETED','CANCELLED','REJECTED')
                                    NOT NULL DEFAULT 'PENDING' COMMENT '支付状态',
    order_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    pay_time        DATETIME        DEFAULT NULL             COMMENT '支付时间',
    expire_time     DATETIME        DEFAULT NULL             COMMENT '支付超时时间(超时未付自动取消)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_orders_order_no (order_no),
    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_show_id (show_id),
    INDEX idx_orders_ticket_id (ticket_id),
    INDEX idx_orders_pay_status (pay_status),
    INDEX idx_orders_expire_time (expire_time),
    INDEX idx_orders_user_pay (user_id, pay_status),
    CONSTRAINT fk_orders_users  FOREIGN KEY (user_id)  REFERENCES users(id),
    CONSTRAINT fk_orders_shows  FOREIGN KEY (show_id)  REFERENCES shows(id),
    CONSTRAINT fk_orders_tickets FOREIGN KEY (ticket_id) REFERENCES tickets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ============================================================
-- 5. VIP权益套餐表
-- ============================================================
CREATE TABLE vip_packages (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '套餐ID',
    package_name        VARCHAR(100)    NOT NULL                 COMMENT '套餐名称',
    benefits            TEXT            DEFAULT NULL             COMMENT '权益内容描述',
    usage_limit         VARCHAR(200)    DEFAULT NULL             COMMENT '使用限制说明',
    valid_period        VARCHAR(100)    DEFAULT NULL             COMMENT '有效期限说明',
    user_level_required INT             NOT NULL DEFAULT 1       COMMENT '所需VIP等级',
    stock               INT             DEFAULT NULL             COMMENT '名额上限, NULL=不限',
    booked_count        INT             NOT NULL DEFAULT 0       COMMENT '已预约数',
    status              ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT '上架状态',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_vip_packages_status (status),
    INDEX idx_vip_packages_level (user_level_required)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='VIP权益套餐表';

-- ============================================================
-- 6. VIP预约表
-- ============================================================
CREATE TABLE vip_bookings (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '预约ID',
    package_id      BIGINT          NOT NULL                 COMMENT '套餐ID',
    user_id         BIGINT          NOT NULL                 COMMENT '用户ID',
    booking_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预约时间',
    use_date        DATE            DEFAULT NULL             COMMENT '期望使用日期',
    audit_status    ENUM('PENDING','APPROVED','REJECTED')
                                    NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
    admin_reply     TEXT            DEFAULT NULL             COMMENT '管理员回复',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_vip_bookings_user (user_id),
    INDEX idx_vip_bookings_package (package_id),
    INDEX idx_vip_bookings_status (audit_status),
    CONSTRAINT fk_vip_bookings_packages FOREIGN KEY (package_id) REFERENCES vip_packages(id),
    CONSTRAINT fk_vip_bookings_users    FOREIGN KEY (user_id)    REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='VIP预约表';

-- ============================================================
-- 7. 公告信息表
-- ============================================================
CREATE TABLE notices (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '公告ID',
    title           VARCHAR(200)    NOT NULL                 COMMENT '公告标题',
    content         TEXT            DEFAULT NULL             COMMENT '公告正文',
    publish_time    DATETIME        DEFAULT NULL             COMMENT '发布时间',
    status          ENUM('PUBLISHED','DRAFT','ARCHIVED')
                                    NOT NULL DEFAULT 'DRAFT' COMMENT '展示状态',
    publisher_id    BIGINT          DEFAULT NULL             COMMENT '发布人(管理员ID)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_notices_status (status),
    INDEX idx_notices_publish_time (publish_time),
    CONSTRAINT fk_notices_users FOREIGN KEY (publisher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告信息表';

-- ============================================================
-- 8. 评价信息表
-- ============================================================
CREATE TABLE reviews (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '评价ID',
    order_id        BIGINT          NOT NULL                 COMMENT '订单ID',
    user_id         BIGINT          NOT NULL                 COMMENT '用户ID',
    show_id         BIGINT          NOT NULL                 COMMENT '演出ID(冗余,便于按演出查评价)',
    content         TEXT            DEFAULT NULL             COMMENT '评价内容',
    rating          TINYINT         DEFAULT NULL             COMMENT '评分 1-5',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    PRIMARY KEY (id),
    INDEX idx_reviews_order (order_id),
    INDEX idx_reviews_user (user_id),
    INDEX idx_reviews_show (show_id),
    CONSTRAINT fk_reviews_orders FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_reviews_users  FOREIGN KEY (user_id)  REFERENCES users(id),
    CONSTRAINT fk_reviews_shows  FOREIGN KEY (show_id)  REFERENCES shows(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价信息表';

-- ============================================================
-- 初始化：插入默认管理员账号 (密码: admin123, BCrypt加密)
-- ============================================================
INSERT INTO users (username, password, role, vip_level, points, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
        'ADMIN', 0, 0, 'ACTIVE');
