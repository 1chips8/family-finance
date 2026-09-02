# 家账：家庭理财系统

这是一个面向软件课程设计的本机 B/S Web 应用，完成注册/登录、创建或加入家庭、家长与普通成员权限、成员与分类管理、收支 CRUD 和家庭统计仪表盘。项目不包含预算、通知、附件、导入导出、多家庭、银行同步、支付接口或公网部署。

## 环境

- Java 17
- Maven Wrapper（Windows 使用 `backend\\mvnw.cmd`）
- Node.js 22+、npm 10+
- MySQL 8.4（8.x 可用）
- Docker Desktop：后端集成测试优先使用 MySQL Testcontainers

## 数据库与配置

先用有权限的 MySQL 账号创建数据库和本地用户：

```sql
CREATE DATABASE family_finance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'family_finance'@'localhost' IDENTIFIED BY '请替换为本机密码';
GRANT ALL PRIVILEGES ON family_finance.* TO 'family_finance'@'localhost';
FLUSH PRIVILEGES;
```

后端从环境变量读取连接信息，不把密码写入源码：

```powershell
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/family_finance?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME='family_finance'
$env:DB_PASSWORD='你的本机密码'
```

启动时 Flyway 会自动执行 `backend/src/main/resources/db/migration/` 中的迁移。不要修改已经执行过的迁移，后续变更新增版本文件。

## 启动

终端一：

```powershell
cd backend
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/family_finance?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME='family_finance'
$env:DB_PASSWORD='你的本机密码'
.\\mvnw.cmd spring-boot:run
```

终端二：

```powershell
cd frontend
npm install
npm run dev
```

浏览器打开 <http://127.0.0.1:5173>。Vite 将 `/api` 代理到 `http://127.0.0.1:8080`，浏览器使用 Session Cookie 和 CSRF Cookie。

## 测试与构建

```powershell
cd backend
.\\mvnw.cmd clean verify

cd ..\\frontend
npm run type-check
npm run test:unit
npm run build
```

后端数据库迁移测试需要 Docker Desktop 正常提供 Docker API。若当前 Windows Docker Desktop 命名管道与 Testcontainers 不兼容，可为专用测试库设置 `TEST_DB_URL`、`TEST_DB_USERNAME`、`TEST_DB_PASSWORD` 后执行迁移验证，并把原因写入阶段记录；不得使用生产库或删除其他数据库。

## Demo Profile

使用 `demo` Profile 会幂等创建演示数据；普通 Profile 不会插入演示数据：

```powershell
cd backend
$env:DB_PASSWORD='你的本机密码'
.\\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo
```

演示账号：

- 家长：`demo_parent` / `Demo1234`
- 普通成员：`demo_member` / `Demo1234`
- 未入户账号：`demo_guest` / `Demo1234`

演示家庭邀请码：`DEMO2026`。重复启动不会重复创建账号、家庭、分类和流水。若要改演示密码，设置 `DEMO_PASSWORD`，不要把真实密码提交到仓库。

## 推荐演示流程

1. 注册家长账号并创建家庭，记录邀请码。
2. 注册普通成员账号并使用邀请码加入。
3. 家长新增收入/支出自定义分类。
4. 两个账号分别记录流水；普通成员只能维护本人流水。
5. 家长查看全家流水、统计、成员和分类页面。
6. 停用分类后确认历史记录仍保留且不能用于新增；停用成员后确认历史流水仍保留。
7. 尝试停用或降级最后一名家长，确认系统拒绝。
8. 退出、重新登录，并在手机宽度完成一次新增收支。

## 常见故障

- **数据库连接失败**：确认 MySQL84 服务运行、数据库已创建、`DB_*` 环境变量在当前终端有效。
- **Flyway 报已执行迁移不一致**：不要回写旧版本；仅对专用开发库确认后重建，课程演示库先备份并核对目标。
- **401/403**：先访问 `/api/auth/csrf`，确认浏览器 Cookie 未被拦截；重新登录，普通成员不能调用家长接口。
- **前端无法访问 API**：确认后端 8080 端口可用，或修改 `frontend/vite.config.ts` 的代理目标。
- **Testcontainers 失败**：启动 Docker Desktop；若仍出现当前命名管道 400，使用专用 MySQL 测试库回退并记录未覆盖范围。

## 当前实现边界

本项目服务课程演示，不承诺生产金融系统的高可用、审计、银行对账、投资分析或公网安全运维。报告素材必须来自实际源码、测试和界面，不填写虚假的多人分工。
