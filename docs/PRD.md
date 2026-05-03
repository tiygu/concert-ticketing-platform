# PRD: 大型演唱会票务预订与VIP服务平台

**版本**: v1.0
**创建日期**: 2026-05-03
**状态**: 已确认

---

## Problem Statement

2025 年全国大型演唱会票房突破 295 亿元，观众规模近 3800 万人次。然而现有票务平台普遍面临：高并发场景下系统响应缓慢、黄牛囤票难以遏制、VIP 权益服务流程断层。消费者缺少一个集在线选座购票、订单管理、VIP 权益预约于一体的公平高效平台；主办方缺少票务库存监控和多维数据统计的运营管理工具。

## Solution

构建一套 B/S 架构的大型演唱会票务预订与 VIP 服务平台。消费者可在浏览器中浏览演出、SVG 可视化选座购票、管理订单、预约 VIP 权益；管理员通过独立后台统一管理用户、演出、订单、VIP 套餐和数据报表。系统通过 InnoDB 行级锁保障高并发购票场景下的库存一致性，通过定时任务自动释放超时未支付座位。

---

## User Stories

### 普通用户

1. As a 普通用户, I want to 注册账号并登录系统, so that 使用平台的购票和 VIP 服务功能
2. As a 普通用户, I want to 在首页浏览演出公告和最新演出列表, so that 及时了解演出资讯
3. As a 普通用户, I want to 按关键词搜索演出, so that 快速找到感兴趣的演唱会
4. As a 普通用户, I want to 查看演出详情（时间、场馆、票价档位、座位图）, so that 做出购票决策
5. As a 普通用户, I want to 在 SVG 座位图上查看座位状态（可售/已售/锁定）, so that 直观选择心仪座位
6. As a 普通用户, I want to 选择座位并提交购票订单, so that 锁定座位并进入支付
7. As a 普通用户, I want to 模拟完成支付, so that 完成购票流程
8. As a 普通用户, I want to 在个人中心查看我的订单列表和详情, so that 了解订单状态
9. As a 普通用户, I want to 取消待支付订单, so that 释放不需要的座位
10. As a 普通用户, I want to 查看和编辑个人信息, so that 维护资料的准确性
11. As a 普通用户, I want to 购票后提交演出评价（评分+内容）, so that 分享观演体验

### VIP 用户（继承普通用户全部功能）

12. As a VIP 用户, I want to 进入 VIP 权益中心浏览可选套餐, so that 了解专属服务内容
13. As a VIP 用户, I want to 提交 VIP 权益套餐预约申请, so that 享受专属服务
14. As a VIP 用户, I want to 查看我的预约审核状态, so that 跟踪预约进度
15. As a VIP 用户, I want to 使用积分兑换 VIP 票品, so that 享受积分权益

### 管理员

16. As a 管理员, I want to 登录后台管理系统, so that 访问管理功能
17. As a 管理员, I want to 查看所有注册用户列表、搜索用户, so that 了解平台用户状况
18. As a 管理员, I want to 禁用违规用户或解封账号, so that 维护平台秩序
19. As a 管理员, I want to 新增、修改、下架演出场次, so that 管理演出排期
20. As a 管理员, I want to 上传演出封面图, so that 提升展示效果
21. As a 管理员, I want to 查看所有订单列表、按状态筛选, so that 掌握售票情况
22. As a 管理员, I want to 审核订单（通过/驳回）, so that 处理异常订单
23. As a 管理员, I want to 配置 VIP 权益套餐（新增/编辑/上架/下架）, so that 管理 VIP 服务体系
24. As a 管理员, I want to 审核 VIP 用户的权益预约申请（通过/驳回）, so that 控制权益发放
25. As a 管理员, I want to 发布、编辑、删除平台公告, so that 通知用户演出排期和活动信息
26. As a 管理员, I want to 查看营收报表（时间段、总售票量、总收入）, so that 掌握经营数据
27. As a 管理员, I want to 查看库存统计（指定演出的已售/剩余座位）, so that 了解销售进度

---

## Implementation Decisions

### 架构

- **B/S 架构**：前后端分离，单仓库（backend/ + frontend/）
- **后端**：Spring Boot 2.7.x + JDK 17 + Maven，单体应用
- **前端**：Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router + Axios
- **ORM**：MyBatis-Plus
- **数据库**：MySQL 5.7+，InnoDB 引擎，utf8mb4 字符集

### 数据库（8 张表）

- `users` — user + admin 合并，role ENUM('ADMIN','USER') 区分
- `shows` — 演出场次信息
- `tickets` — ticket_regular + ticket_vip 合并，ticket_type ENUM('REGULAR','VIP') 区分
- `orders` — 订单，expire_time 字段支持超时释放
- `vip_packages` — VIP 权益套餐
- `vip_bookings` — VIP 预约，需管理员审核
- `notices` — 公告信息
- `reviews` — 评价信息

完整 DDL 见 `docs/schema.sql`，设计决策见 `docs/adr/0001-database-design.md`。

### API 规范

- RESTful URL，前缀 `/api/`
- 用户接口 `/api/user/*`，管理后台 `/api/admin/*`
- 统一响应体 `{ code, message, data }`
- 分页响应 `{ code, message, data: { records, total, page, pageSize } }`
- HTTP 状态码 + 业务 code 双层错误处理

### 认证与权限

- Spring Security + JWT（Access Token 2h + Refresh Token 7d）
- BCrypt 密码加密，strength=10
- Controller 层 @PreAuthorize 注解校验角色
- 前端 Axios 拦截器自动刷新 token
- CORS 白名单，开发阶段允许 localhost:5173

### 选座方案

- 前端 SVG 渲染座位图（一期用 CSS Grid 网格模拟场馆布局）
- 座位状态用颜色区分：灰=已售、绿=可售、黄=已锁定、蓝=用户选中
- 后端 tickets 表按座位粒度存储，选座时 SELECT ... FOR UPDATE 行级锁

### 支付方案

- 模拟支付：前端点击支付 → 后端直接将订单标记为 PAID
- 在 OrderService 预留 pay() 方法接口，后续可对接真实支付通道

### 订单超时释放

- 下单时设置 expire_time = NOW() + 15 分钟
- Spring @Scheduled，每 30 秒扫描超时未付订单
- 每次取 100 条，逐条在事务内释放座位 + 更新订单状态为 CANCELLED

### 异常处理

- @RestControllerAdvice 全局异常拦截
- 自定义 BizException（含业务错误码），按需继承子类异常
- DTO 入参用 javax.validation 注解校验（@NotBlank、@NotNull、@Min）
- 未知异常统一返回 HTTP 500

### 环境配置

- dev（本地 MySQL）、test（H2 内存库）、prod（生产 MySQL）三个 profile
- 文件上传：本地存储 backend/uploads/，限制 2MB，仅 jpg/png
- 日志：SLF4J + Logback，Controller 层 AOP 统一日志

### 后端模块划分

common → security → user → show → ticket → order → vip → notice → review → statistics

### 前端模块划分

auth（登录/注册）→ home（首页/演出/公告）→ user-center（订单/资料）→ vip-center（权益/预约）→ admin（后台全模块）

---

## Testing Decisions

### 测试层级

| 层级 | 框架 | 范围 | 目标 |
|------|------|------|------|
| 单元测试 | JUnit 5 + Mockito | Service 层业务逻辑 | 核心模块 80%+ |
| 集成测试 | Spring Boot Test + H2 | Mapper + Controller | 每接口至少正常+异常两条 |

### TDD 起手顺序

1. TicketService — 库存扣减、锁座、事务回滚（最核心）
2. OrderService — 支付确认、超时取消、订单状态流转
3. VipBookingService — VIP 权益预约、审核通过/驳回
4. UserService — 注册、登录、权限校验
5. ShowService — 演出 CRUD
6. NoticeService — 公告 CRUD
7. StatisticsService — 数据聚合统计

### 测试原则

- 只测外部行为，不测内部实现
- Mock 外部依赖（数据库用 H2 替代，不 Mock Mapper）
- 每个测试方法只测一个场景
- 测试方法命名：`should_预期行为_when_触发条件`

---

## Out of Scope

- 真实支付对接（支付宝/微信）
- Redis 缓存、消息队列
- 微信小程序
- 智能推荐算法
- 移动端 App
- 区块链/NFT 票务
- 微服务拆分
- CI/CD 自动化流水线
- 容器化部署（Docker/K8s）

---

## Further Notes

- 领域术语表见 `CONTEXT.md`
- 论文即需求文档，不再变更业务逻辑
- 票务工作人员角色合并入管理员，不独立开发
