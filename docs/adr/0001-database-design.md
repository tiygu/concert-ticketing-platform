# ADR-001: 数据库表合并设计

**日期**: 2026-05-03
**状态**: 已采纳

## 背景

论文原始设计包含 10 张表：users、admins、shows、ticket_regular、ticket_vip、orders、vip_packages、vip_bookings、notices、reviews。在实际落地时发现三处可合并优化的点。

## 决策

### 1. users + admins → users（role 字段区分）

**理由**：
- 两张表字段高度重合（username、password、联系方式、创建时间）
- 合并后 Spring Security 只需对接一张表，登录逻辑统一
- 管理员和普通用户的区别仅在于权限，不需要独立存储
- role 用 ENUM('ADMIN','USER')，ADMIN 角色拥有后台管理权限

**拒绝方案**：保持两表分离。缺点：登录需要查两张表，认证逻辑冗余；管理员和用户共享字段重复存储。

### 2. ticket_regular + ticket_vip → tickets（ticket_type 区分）

**理由**：
- 两张表共用 show_id、price_type、price、stock、sold 等核心字段
- VIP 票独有字段 points_price 设为 NULLABLE，普通票该字段为 NULL
- 合并后查询"某演出所有票种"只需单表查询，无需 UNION
- 库存扣减逻辑统一，减少代码分支

**拒绝方案**：保持两表分离。缺点：查询和统计时需要 JOIN/UNION；库存管理逻辑需维护两套。

### 3. 订单表外键简化（regular_ticket_id + vip_ticket_id → ticket_id）

**理由**：
- 随选票表合并，外键自然简化为单一 ticket_id
- 订单已冗余 ticket_type 字段用于区分票种，无需通过外键判断

## 影响

- 总表数从 10 张减为 8 张
- 业务代码的 Service/Mapper 层结构更简洁
- 测试用例数量相应减少
