# 智购商城

> 本人的毕业设计项目——一个带 AI 智能客服的电商平台。

## 项目简介

这是一个基于 **Vue 2 + Spring Boot 2.7** 的电商系统，最大的特点是集成了 **AI 智能客服**。用户可以用自然语言跟 AI 对话，AI 能帮你搜商品、加购物车、查订单，还能回答退换货政策等业务问题。

整个项目覆盖了电商的核心功能（商品浏览、购物车、下单支付、订单管理、评价），同时加入了 AI 客服和人工客服工单系统，形成完整的购物闭环。

## 核心亮点

### 1. AI 智能客服

不是那种只会回复固定话术的机器人，而是基于 **LangChain4j + DeepSeek 大模型** 构建的 AI Agent。它能理解用户意图，自主决定调用哪些工具来完成用户的请求。

**调用链路：**
```
用户输入："我想买一台笔记本电脑"
  ↓
前端通过 SSE 流式接口发送到后端
  ↓
SmartCustomerService 接收请求，创建 SSE 连接
  ↓
MallAiAssistant (LangChain4j Agent) 分析用户意图
  ↓
识别为"搜索商品"意图，自主决定调用 searchProducts 工具
  ↓
MallToolService.searchProducts() 从数据库查询笔记本类商品
  ↓
AI 将查询结果整理成自然语言回复
  ↓
通过 SSE 流式推送到前端，打字机效果展示
```

### 2. RAG 知识库问答

AI 客服背后有一个知识库（退换货政策、物流信息、支付方式等），通过向量检索技术，AI 能基于真实的业务规则来回答用户问题，而不是瞎编。

**调用链路：**
```
用户输入："怎么退换货？"
  ↓
AI 将问题通过 BGE 模型向量化
  ↓
在向量数据库（Chroma/Pinecone）中检索相似文档
  ↓
找到"退换货政策"相关文档片段
  ↓
将检索到的文档作为上下文传给大模型
  ↓
AI 基于上下文生成准确回答
  ↓
通过 SSE 流式推送到前端
```

### 3. AI 转人工客服

AI 解决不了的问题，可以一键转接人工客服。系统会自动创建工单，客服通过 WebSocket 与用户实时聊天。

### 4. 完整电商功能

- 商品浏览、搜索、分类筛选
- 购物车管理
- 下单支付（支付宝沙盒）
- 订单管理、物流追踪
- 商品评价
- 后台管理（商品、订单、数据统计）

## 技术栈

| 分类 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7、Java 17、MyBatis-Plus、MySQL、Redis |
| 前端 | Vue 2、Element UI、Vuex、ECharts |
| AI | LangChain4j、DeepSeek、BGE 向量模型、RAG |
| 其他 | WebSocket、支付宝 SDK、SSE 流式响应 |

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p < db-migration/init-database.sql
```

一条命令创建所有表并插入测试数据（35 个商品、4 个测试账号等）。

### 2. 启动后端

```bash
cd "ElectronicMallApi - idea"
# 修改 src/main/resources/application.yml 中的数据库密码和 DeepSeek API Key
mvn spring-boot:run
```

### 3. 启动前端

```bash
cd ElectronicMallVue
npm install
npm run dev
```

### 4. 登录体验

访问 **http://localhost:9193**，用以下账号登录：

| 账号 | 用户名 | 密码 |
|------|--------|------|
| 普通用户 | testuser | 123456 |
| 客服 | service001 | 123456 |
| 管理员 | admin | 123456 |

> 登录后点击右下角的 AI 客服按钮，试试跟 AI 聊天。

---

## 详细文档

- [详细配置指南](#-详细配置指南)
- [使用说明](#-使用说明)
- [AI 模块详解](#-ai-模块详解)
- [API 接口](#-api-接口)
- [常见问题](#-常见问题)

---

## 详细配置指南

### 1. 环境准备

确保以下环境已安装：

```bash
# Java 17+
java -version

# Maven
mvn -version

# Node.js 14+
node -v
npm -v

# MySQL 和 Redis
mysql --version
redis-cli ping  # 应返回 PONG
```

### 2. 数据库初始化

```bash
mysql -u root -p < db-migration/init-database.sql
```

**初始化脚本包含：**
- 所有基础表（用户、商品、订单、购物车等）
- AI 相关表（对话历史、聊天消息）
- 工单系统表（工单、流转记录、回复）
- 评论系统表（评论、回复、图片）
- 测试数据（4 个测试用户、35 个商品、商品规格、评论等）

### 3. 后端配置

#### 3.1 进入后端目录

```bash
cd "ElectronicMallApi - idea"
```

#### 3.2 复制并编辑配置文件

```bash
cp src/main/resources/application-example.yml src/main/resources/application.yml
```

编辑 `src/main/resources/application.yml`，修改以下配置：

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_mall?serverTimezone=GMT%2b8&userSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 你的数据库密码

  # Redis 配置
  redis:
    host: localhost
    port: 6379
    password: 你的Redis密码（没有则留空）

# AI 模型配置（必须配置 DeepSeek API Key）
langchain4j:
  open-ai:
    api-key: sk-你的DeepSeekAPIKey
    base-url: https://api.deepseek.com/v1
    chat-model:
      model-name: deepseek-chat
      temperature: 0.7
      max-tokens: 2000
```

#### 3.3 获取 DeepSeek API Key

1. 访问 [DeepSeek 开放平台](https://platform.deepseek.com)
2. 注册账号并登录控制台
3. 创建 API Key
4. 将 API Key 填入配置文件的 `api-key` 字段

#### 3.4 启动后端

```bash
# 方式一：Maven 直接运行（开发模式）
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package -DskipTests
java -jar target/ElectronicMallApi-0.0.1-SNAPSHOT.jar
```

启动成功后访问：`http://localhost:9191`

Swagger 文档：`http://localhost:9191/swagger-ui.html`

### 4. 前端配置

#### 4.1 进入前端目录并安装依赖

```bash
cd ElectronicMallVue
npm install
```

#### 4.2 配置后端地址

前端已配置代理指向后端（`vue.config.js`），默认配置如下：

```javascript
module.exports = {
  devServer: {
    port: 9193,  // 前端端口
    proxy: {
      '/api': {
        target: 'http://localhost:9191',  // 后端地址
        changeOrigin: true,
      }
    }
  }
}
```

> 如果后端端口不是 `9191`，需要修改 `vue.config.js` 中的 `target` 地址。

#### 4.3 启动前端

```bash
npm run dev
```

启动成功后访问：`http://localhost:9193`

---

## 使用说明

### 一、普通用户功能（testuser）

#### 1. 浏览商品
- 访问首页查看推荐商品和分类
- 点击商品卡片进入详情页查看规格、价格、评价
- 支持按分类筛选商品

#### 2. 购物车管理
- 点击"加入购物车"将商品添加到购物车
- 在购物车页面调整商品数量、删除商品
- 支持批量结算

#### 3. 下单支付
- 从购物车选择商品提交订单
- 填写收货地址和联系方式
- 选择支付宝支付（沙盒环境）
- 支付成功后查看订单状态

#### 4. 订单管理
- 查看我的订单列表（待付款、待发货、待收货、已完成）
- 查看订单详情和物流信息
- 申请退款/退货

#### 5. AI 智能客服（核心功能）
- 点击页面右下角的 AI 客服悬浮按钮
- 支持自然语言对话，例如：
  - "我想买一台笔记本电脑"
  - "帮我推荐几款手机"
  - "我的订单什么时候发货？"
  - "退换货政策是什么？"
- AI 会自动理解意图并调用工具：
  - 搜索商品
  - 加入购物车
  - 查询订单
  - 回答知识库问题
- 如果 AI 无法解决，可点击"转人工客服"创建工单

#### 6. 在线客服/工单系统
- AI 转人工后自动创建工单
- 在"我的工单"页面查看工单状态
- 与客服实时聊天（WebSocket）
- 查看工单处理进度

#### 7. 商品评价
- 收到商品后可以发表评价
- 支持文字和图片评价
- 查看其他用户的评价

---

### 二、客服功能（service001 / service002）

#### 1. 工单管理
- 登录后进入客服工作台
- 查看待处理工单列表
- 认领工单并与用户沟通
- 更新工单状态（处理中、已解决、已关闭）

#### 2. 实时聊天
- 通过 WebSocket 与用户实时对话
- 查看用户历史工单记录
- 快速回复常见问题

#### 3. 知识库查询
- 客服也可使用 AI 助手查询知识库
- 快速找到标准答案回复用户

---

### 三、管理员功能（admin）

#### 1. 商品管理
- 添加/编辑/删除商品
- 设置商品分类和规格
- 管理商品图片和描述
- 设置推荐商品

#### 2. 分类管理
- 管理商品分类树
- 设置分类图标和排序

#### 3. 订单管理
- 查看所有订单
- 处理发货（填写物流信息）
- 处理退款申请

#### 4. 用户管理
- 查看用户列表
- 管理用户状态

#### 5. 数据统计
- 查看销售数据图表
- 分析热门商品
- 查看用户活跃度

---

### 四、AI 智能客服使用技巧

#### 对话示例

| 用户输入 | AI 行为 |
|----------|---------|
| "我想买手机" | 调用 `searchProducts` 搜索手机类商品 |
| "推荐几款笔记本" | 调用 `getRecommendedProducts` 获取热销推荐 |
| "把 iPhone 加入购物车" | 调用 `addToCart` 添加商品 |
| "我的订单到哪了" | 调用 `viewOrders` 查询订单 |
| "怎么退换货" | 从知识库检索退换货政策 |
| "支持哪些支付方式" | 从知识库检索支付信息 |
| "转人工" | 调用 `transferToHuman` 创建工单 |

#### 注意事项
- AI 对话需要配置 DeepSeek API Key（必须）
- 首次启动时会加载知识库到向量数据库（需要几分钟）
- 对话历史保存在 Redis 中，支持多轮上下文
- AI 回答基于知识库，准确性高，减少幻觉

---

## AI 模块详解

### 架构概览

```
用户消息
  ↓
AiAssistantController              # 接口层
  ↓
SmartCustomerService               # 调度层（SSE 流式输出）
  ↓
MallAiAssistant (LangChain4j Agent) # Agent 层
  ↓
┌──────────────┬────────────────────────────┐
│  ReAct 循环   │  RAG 知识库   │  MCP 工具    │
│  (自主决策)   │  (向量检索)   │  (业务操作)   │
└──────────────┴──────────────┴──────────────┘
```

### 核心特性

| 特性 | 说明 |
|------|------|
| **自然语言对话** | 支持多轮对话，自动维护上下文（Redis 持久化） |
| **意图识别** | AI 自动理解用户意图（搜索、下单、查订单等） |
| **工具调用** | AI 自主决定调用哪些业务工具（@Tool 注解） |
| **知识库问答** | RAG 技术，基于真实业务规则准确回答 |
| **流式响应** | SSE 实时推送，打字机效果 |
| **转人工客服** | AI 无法解决时自动创建工单并转接 |

### MCP 工具列表

#### 商城工具（MallToolService）

| 工具 | 功能 |
|------|------|
| `searchProducts` | 搜索商品 |
| `getProductDetail` | 获取商品详情 |
| `getRecommendedProducts` | 获取热销推荐 |
| `addToCart` | 加入购物车 |
| `viewCart` | 查看购物车 |
| `createOrder` | 创建订单 |
| `viewOrders` | 查看订单列表 |
| `getOrderDetail` | 获取订单详情 |
| `analyzeOrders` | 分析订单历史 |
| `trackOrder` | 追踪订单物流 |
| `checkStock` | 查询库存 |
| `batchAddToCart` | 批量添加购物车 |

#### 客服工具（CustomerServiceSkills）

| 工具 | 功能 |
|------|------|
| `queryKnowledgeBase` | 查询知识库（RAG） |
| `checkReturnPolicy` | 查询退换货政策 |
| `checkShippingInfo` | 查询物流信息 |
| `checkPaymentMethods` | 查询支付方式 |
| `checkAfterSalesPolicy` | 查询售后政策 |
| `transferToHuman` | 转接人工客服 |
| `createTicket` | 创建客服工单 |
| `estimateDelivery` | 估算配送时间 |

### RAG 知识库

知识库文档位于 `src/main/resources/knowledge-base/` 目录，支持以下分类：

- `return_policy/` - 退换货政策
- `shipping/` - 发货物流
- `payment/` - 支付方式
- `after_sales/` - 售后服务
- `account/` - 账户相关

**工作流程：**
1. 应用启动时加载 Markdown 文档 → 分块 → BGE 模型向量化 → 存储到向量数据库
2. 用户提问时 → 向量相似度搜索 → 检索相关文档 → AI 基于知识库生成回答

---

## API 接口

### AI 相关接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ai/smart/chat` | POST | 智能客服对话（非流式） |
| `/api/ai/smart/chat/stream` | POST | 智能客服对话（SSE 流式） |
| `/api/ai/smart/knowledge` | POST | 知识库查询 |
| `/api/ai/smart/knowledge/{category}` | POST | 分类知识库查询 |
| `/api/ai/smart/transfer/human` | POST | 转人工客服 |
| `/api/ai/chat` | POST | 旧版 AI 对话 |
| `/api/ai/history` | GET | 获取对话历史 |
| `/api/ai/history/clear` | POST | 清空对话历史 |

### 业务接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/good` | GET | 商品列表 |
| `/api/good/{id}` | GET | 商品详情 |
| `/api/cart` | POST/GET | 购物车操作 |
| `/api/order` | POST/GET | 订单操作 |
| `/api/user/login` | POST | 用户登录 |
| `/api/user/register` | POST | 用户注册 |
| `/api/alipay/*` | POST/GET | 支付宝支付 |

> 所有接口基础地址：`http://localhost:9191`

---

## 常见问题

### Q1: 启动报错 "DeepSeek API Key 未配置"

**解决**：在 `application.yml` 中配置有效的 DeepSeek API Key：

```yaml
langchain4j:
  open-ai:
    api-key: sk-你的APIKey
```

### Q2: Redis 连接失败

**解决**：确保 Redis 已启动，并检查配置中的 host、port、password 是否正确。

```bash
# 启动 Redis
redis-server

# 测试连接
redis-cli ping  # 应返回 PONG
```

### Q3: 前端请求跨域

**解决**：后端已配置 CORS，如仍有问题，检查 `CorsConfig.java` 中的允许域名配置。

### Q4: AI 回答不准确或出现幻觉

**解决**：
1. 检查知识库文档是否完整（`src/main/resources/knowledge-base/`）
2. 调整 `application.yml` 中的 `temperature` 参数（越低越稳定）
3. 确保向量数据库已正确初始化

### Q5: 数据库连接失败

**解决**：
1. 确保 MySQL 已启动：`mysql.server start`（macOS）或 `systemctl start mysqld`（Linux）
2. 检查 `application.yml` 中的数据库名是否为 `db_mall`
3. 检查用户名和密码是否正确
4. 确认数据库已执行初始化脚本：`mysql -u root -p < db-migration/init-database.sql`

### Q6: 前端无法访问后端 API

**解决**：
1. 确认后端已启动：访问 http://localhost:9191 应该能看到响应
2. 检查 `vue.config.js` 中的代理配置 `target` 是否为 `http://localhost:9191`
3. 确认前端端口为 `9193`，访问 http://localhost:9193

### Q7: 支付宝支付无法使用

**解决**：
1. 支付宝沙盒配置在 `application.yml` 中已预设，默认使用沙盒环境
2. 如需使用真实支付，需要替换为正式的支付宝应用配置
3. 沙盒账号可在 [支付宝开放平台](https://open.alipay.com) 沙盒环境中获取

### Q8: 端口被占用

**解决**：
- 后端端口 `9191` 被占用：修改 `application.yml` 中的 `server.port`
- 前端端口 `9193` 被占用：修改 `vue.config.js` 中的 `devServer.port`
- 修改后记得同步更新另一端的代理配置

---

## 许可证

本项目仅供学习和研究使用。

## 作者

- **rabbiter**

---

## 联系方式

如有问题或建议，欢迎提 Issue 或联系作者。
