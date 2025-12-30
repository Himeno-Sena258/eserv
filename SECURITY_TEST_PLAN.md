# 安全测试计划（设计与实现）

## 目标与范围
- 验证项目的认证与授权策略：JWT签发与校验、角色（admin/user）权限、所有者（User-Customer-Order）访问控制。
- 覆盖主要安全相关路径：/api/login、/api/register、业务API下的读写权限与所有者约束。
- 运行项目全部测试用例，并统计语句覆盖率（statement coverage）与条件覆盖率（condition coverage）。

## 测试设计
- 鉴权与过滤
  - 缺失或非法令牌访问 /api/** 返回 401。
  - /api/login、/api/register 免过滤，可正常登录与注册。
  - 有效令牌可访问 API；JWT 中的用户名与角色注入到请求属性供业务层使用。
- 角色授权
  - admin 对 Customer/Order 拥有完整读写权限。
  - user 仅能读取自己的 Customer 列表与详情，Customer 写操作拒绝（403）。
  - user 仅能创建/更新/删除属于自己的 Customer 的订单，跨他人客户拒绝（403）。
- 资源所有者
  - 注册后自动创建与用户关联的 Customer（user_id）。
  - 订单与备注必须归属该用户的 Customer；跨用户访问与写入拒绝（403）。

## 实现与用例
- 已有测试用例
  - SecurityIntegrationTests：基础JWT过滤器与非API路径验证。
  - CustomerApiTests：Customer的CRUD与用户角色写操作限制。
  - OrderNoteApiTests：Order与OrderNote的增删改查流程与鉴权。
  - ProductApiTests：商品接口基本行为与鉴权。
  - OrderServiceTests：订单的基础流程与鉴权。
  - OrderAclTests：用户仅能管理自己的订单与客户的ACL规则（新增）。
- 关键安全校验点
  - JwtAuthFilter：校验并注入 currentRole/currentUsername，非API与登录/注册路径免过滤。
  - CustomerService：基于角色与用户名的读写与所有者检查。
  - OrderService：基于角色与用户名的读写与所有者检查，更新时禁止将订单迁移到他人客户。
  - RestExceptionHandler：统一返回401/403/404/400等错误响应。

## 覆盖率统计方法
- 工具：JaCoCo（Maven插件）
  - 配置：在 pom.xml 中引入 jacoco-maven-plugin（prepare-agent + report）。
  - 运行：`mvn -q -DskipITs test` 自动生成覆盖率报告。
  - 报告位置：`target/site/jacoco/jacoco.xml`（XML）与 `jacoco.csv`（汇总）。
- 指标映射
  - 语句覆盖率（statement coverage）：取 JaCoCo 的 INSTRUCTION 覆盖率。
  - 条件覆盖率（condition coverage）：取 JaCoCo 的 BRANCH 覆盖率。

## 运行步骤
1. 在Windows 11上打开PowerShell，进入项目根目录。
2. 执行：`mvn -q -DskipITs test`
3. 查看覆盖率报告：`target/site/jacoco/jacoco.csv` 或 `jacoco.xml`（HTML可浏览 `target/site/jacoco/index.html`）

## 本次执行结果（统计）
- 语句覆盖率（INSTRUCTION）：约 87.2%（covered 1840 / total 2112）
- 条件覆盖率（BRANCH）：约 65.0%（covered 91 / total 140）
- 来源：`target/site/jacoco/jacoco.csv` 汇总统计。

## 新增安全测试点与实例
- 401未授权（3例）
  - 未携带Authorization访问 /api/customers 返回401
  - 使用非Bearer前缀访问 /api/orders 返回401
  - 使用伪造无效JWT访问 /api/orders 返回401
- 登录/注册入口（3例）
  - 注册缺少用户名返回400
  - 注册缺少密码返回400
  - 登录错误密码返回401
- 用户角色对Customer写操作的403（3例）
  - user请求POST /api/customers 返回403
  - user请求PUT /api/customers/{uid} 返回403
  - user请求DELETE /api/customers/{uid} 返回403
- 客户读取的所有者约束（3例）
  - user访问GET /api/customers 仅返回自己的客户（size=1）
  - user访问GET /api/customers/{ownUid} 返回200
  - user访问GET /api/customers/{foreignUid} 返回403
- 非法UID触发400（3例）
  - GET /api/customers/invalid-uid 返回400
  - GET /api/orders/invalid-uid 返回400
  - PUT /api/orders/invalid-uid 返回400

实现代码参考：[ApiSecurityMoreTests.java](file:///e:/Code/repository/eserv(1)/eserv/src/test/java/com/eServM/eserv/security/ApiSecurityMoreTests.java)

## 失败用例与根因分析
- 本次执行：全部测试用例通过（无失败）。
- 历史问题总结（已修复）
  - SQLite 对一对一唯一约束的DDL更新报错（Cannot add a UNIQUE column）：测试环境改用 `create-drop`，并取消显式 unique 注解，避免迁移阶段对 UNIQUE 的更新。
  - 注册接口重复用户名抛500：改为服务层先查存在，控制器返回 409，以适配多用例重复注册。
  - 角色写操作在过滤器层粗粒度拒绝：下沉到服务层按资源所有者精细化检查，避免简单的“非GET一律拒绝”的策略限制正常用户的订单管理。

## 覆盖率提升建议
- 增加对异常分支的单测，覆盖更多的 400/401/403/404分支路径。
- 针对 RestExceptionHandler 的多种异常类型补充触发用例，提高BRANCH覆盖率。
- 对安全相关服务的边界情况（空字段、非法UID、不存在关联、跨用户访问）增加单测。
