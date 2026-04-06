# 项目核心定位与任务书对应关系（答辩专用）

## 一、项目本质（一句话总结）

本项目**不是简单的二维码管理系统**，而是以**一物一码区块链溯源**为基础，叠加**实时规则引擎 + AI 异常检测**，实现**香干质量安全风险自动识别与分级处置**的全闭环监管预警系统。

## 二、与毕业设计任务书的核心对应

- **任务书题目**：基于人工智能的香干质量安全预警系统
- **核心重点**：**判险**（解决传统溯源 “只存证、不判险、不预警” 问题）
- **系统价值**：识别二维码回收复用、跨区域窜货、批量假码、异常刷单等风险

## 三、系统两层核心架构

1. **溯源层**

   保证**码真实、信息可追溯、数据不可篡改**

   - 一物一码绑定产品 / 批次 / 企业
   - 国密 SM2 签名防伪造
   - 区块链存证防篡改

   

2. **预警层**

   实现**风险可识别、可分级、可处置**

   

   - 扫码行为实时分析
   - 规则引擎 + AI 模型双通道判定
   - 红黄蓝三级预警 + 自动冻结 / 推送 / 上链

   

## 四、标准业务流程（答辩推荐讲法）

1. 企业生成溯源二维码（trace-service）
2. 二维码绑定产品、批次、企业信息，实现一物一码
3. 生成时使用**国密 SM2 签名**，防止伪造
4. 用户扫码 → 先验签 → 校验二维码状态 → 记录扫码日志（scan-service）
5. 提取风控特征：设备、IP、时间方差、位置方差
6. **规则引擎 + AI 模型**融合计算风险分（alert-service）
7. 风险分级处置：记录留痕 → 消息 / 短信 / 邮件推送 → 区块链冻结二维码

离线训练与ONNX导出脚本见 `ml-pipeline/README.md`，对应任务书“SQL生成数据→特征抽取→双通道模型训练→蒸馏→ONNX→Java推理”。

## 五、关键概念澄清

### 1. 额度管控 vs 复用预警（你已实现 vs 未实现）

- **公司发码额度控制**：业务层面，发码前限制企业可生成码数量（**未完整实现**）
- **二维码复用预警**：安全风控层面，扫码后检测是否异常扫码、跨设备、跨区域（**你已实现**）

### 2. 溯源真正含义

溯源不只是 “发了多少码”，而是向监管与消费者证明：

- 产品是谁生产、哪一批、何时生产
- 二维码是否为真、是否被篡改
- 是否存在异常扫码、窜货、复用风险

### 3. 你项目的可信基座

- `trace-service`：溯源码管理
- `block-service`：区块链存证
- `scan-service`：扫码日志采集
- `alert-service`：风险预警与处置



# 攸县香干区块链溯源系统模块分析与完善建议

## 一、整体优点（无需修改，保持原样）

1. **模块划分符合“业务域解耦”原则**：6个模块（5个核心服务+1个公共模块）精准对应之前的分库设计，每个服务只负责单一核心职责，无职责交叉，符合微服务架构规范，也贴合毕设“区块链溯源+质量安全预警”的核心需求。
2. **职责定位清晰**：每个模块都明确了“对应数据库+核心职责+业务功能”，逻辑闭环，能清晰体现“生产→扫码→预警→监管”的全业务流程，完全匹配之前梳理的业务逻辑和数据流动。
3. **突出核心亮点**：重点标注了yx-alert-service为核心服务，yx-block-service为论文亮点，贴合毕设评分重点（创新点、核心技术应用）。
4. **贴合实际业务**：模块功能与攸县香干的生产、溯源、监管场景高度契合（如小作坊备案、香干批次管理），不脱离实际，避免了“为了做模块而做模块”的问题。

## 二、各模块优化建议（小幅补充，强化严谨性）

**优化原则**：不改变原有核心功能，仅补充细节、明确关联关系、贴合“监管导向”和“技术亮点”，让模块描述更完整，更适合写入论文。

### 1. yx-trace-service溯源核心服务（补充2点，贴合预警与区块链）

**对应数据库**：yx_trace_core

**核心职责**：生产端→从0到1生成香干溯源信息，为整个系统提供基础溯源数据，联动区块链存证与预警服务，保障“一码一物”可信性。

**主要业务功能**：

- 企业信息管理（生产厂家、作坊、地址）——关联yx-auth-service，仅认证通过的企业可录入信息
- 产品批次管理（香干生产批次、生产日期、配料、生产标准）——补充“生产标准”，贴合食品溯源合规要求
- 二维码生成与绑定（为每一份香干生成唯一溯源二维码，关联批次信息）——补充“关联批次”，强化“一码一物”绑定逻辑
- 二维码生命周期管理（启用、禁用、作废、冻结）——补充“冻结”，联动yx-alert-service，支持预警触发的自动冻结
- 基础溯源查询（扫二维码能看到：谁生产、何时生产、配料信息、批次编号）——补充“配料信息、批次编号”，丰富溯源内容
- 二维码验签预处理（生成二维码时同步完成SM2签名，为后续扫码验真提供支持）——补充，贴合安全要求

### 2. yx-scan-service扫码记录服务（补充2点，强化数据支撑作用）

**对应数据库**：yx_scan_anomaly、yx_geo_profile

**核心职责**：消费者/渠道端→每一次扫码都完整记录，完成数据脱敏与初步校验，为预警服务、AI模型提供高质量原始数据，是风险检测的基础。

**主要业务功能**：

- 接收前端扫码请求，完成二维码SM2验签（校验二维码真伪，关联yx-block-service存证信息）——补充，贴合安全与区块链联动
- 记录扫码日志：时间、地点、经纬度、IP、设备指纹、扫码用户手机号（脱敏存储）——补充“脱敏手机号”，贴合安全要求
- 设备画像识别（是否新设备、是否风险设备、设备操作系统/浏览器信息）——补充设备细节，丰富AI特征数据源
- 地理位置校验（是否跨区域、是否异常地点、解析所在城市/省份）——补充“解析城市/省份”，为窜货检测提供支撑
- 实时写入扫码日志表scan_log，标记“是否首次扫码”——补充，贴合AI特征提取需求
- 数据脱敏处理（手机号、IP末段打码存储）——补充，贴合安全要求
- 为后续预警与AI计算提供原始数据（同步推送扫码数据至yx-alert-service，支撑实时检测）——补充联动关系

### 3. yx-alert-service预警与AI服务（核心）（补充3点，强化核心地位）

**对应数据库**：yx_alert_engine、yx_ai_feature

**核心职责**：判断是否风险+触发预警+AI推理，是系统“存证→判险”升级的核心，联动各服务完成风险分级、预警推送与处置，面向监管部门提供风险防控能力。

**主要业务功能**：

- 定时任务：从扫码日志统计生成AI特征表reuse_pattern（提取扫码次数、时间方差、位置方差、设备数等核心特征）——补充特征细节，明确AI输入
- 规则引擎判断（硬规则：Drools引擎，支持热更新，如1h≥5次扫码、1d≥10设备、1h≥20IP等）——补充“Drools引擎、热更新”，突出技术亮点
- AI异常检测模型推理（基于PyTorch搭建的Transformer+异构图双通道网络，输入4个核心特征→输出风险分）——补充模型细节，贴合毕设技术点
- 风险等级判定（低/中/高，对应黄/橙/红警）——补充等级对应关系，贴合预警处置逻辑
- 生成预警记录、推送预警信息（仅向监管方推送：系统消息+短信+邮件，失败自动重试1次）——补充“仅向监管方推送”“重试机制”，贴合监管导向与性能要求
- 预警管理：查看、处理、闭环（支持监管人员在后台标记预警处置状态，更新预警记录）——补充“闭环”细节
- 对接后台管理页面展示风险大盘（实时展示预警数量、风险等级分布、异常二维码状态）——补充展示内容
- 联动区块链服务（严重预警时，调用yx-block-service接口，冻结二维码并完成存证）——补充联动关系，强化闭环
- 规则热更新对接（关联Nacos配置中心，规则新增/修改后≤10秒生效）——补充，贴合性能要求

### 4. yx-auth-service企业认证服务（补充1点，强化联动）

**对应数据库**：yx_company_auth

**核心职责**：管住“谁能生产香干”，为溯源服务提供可信企业主体，保障溯源信息的真实性，是系统可信性的基础。

**主要业务功能**：

- 企业入驻、资料提交（企业基本信息、资质文件上传）
- 资质审核（营业执照、卫生许可、小作坊备案，由监管人员完成审核）——补充“审核主体”，贴合监管导向
- 企业信用等级管理（关联企业违规记录、预警处置结果，动态调整信用等级）——补充联动关系
- 生产权限控制（未认证不能生成二维码，联动yx-trace-service，限制未审核企业的生产溯源权限）——补充联动关系，强化权限管控
- 为溯源服务提供可信企业主体（向yx-trace-service提供企业认证状态接口）

### 5. yx-block-service区块链存证服务（补充2点，突出论文亮点）

**对应数据库**：yx_blockchain_proof

**核心职责**：关键数据上链，防止造假篡改，提升系统公信力，是论文核心创新点之一，为溯源信息、预警事件提供不可篡改的存证支撑。

**主要业务功能**：

- 二维码生成时哈希上链（联动yx-trace-service，将二维码ID、批次信息、企业信息生成哈希，写入FISCO-BCOS区块链）——补充区块链类型，贴合技术选型
- 重要预警事件存证（严重/中等预警的触发时间、处置动作、风险详情上链，实现预警可追溯）——补充存证内容
- 关键扫码行为存证（异常扫码日志、批量扫码行为上链，为监管取证提供可信依据）——补充存证内容
- 提供存证核验接口（证明数据没被改过，供扫码验真、监管复核使用，联动yx-scan-service、yx-alert-service）——补充联动关系
- 二维码冻结/解冻存证（联动yx-alert-service、yx-trace-service，将二维码冻结/解冻状态上链，保障状态不可篡改）——补充，强化闭环
- 提升系统公信力（论文亮点）——保留，突出毕设创新点

### 6. yx-common公共模块（补充2点，完善工具类）

**核心职责**：为所有服务提供公共支撑，统一规范，减少重复开发，保障系统一致性。

**主要业务功能**：

- 统一返回结果R（规范所有服务的接口返回格式，便于前端对接与异常排查）
- 全局异常处理（捕获各服务的通用异常，统一返回错误信息，提升系统稳定性）
- 工具类（日期、加密、经纬度计算、校验等，补充：SM2加密工具、区块链哈希计算工具、IP/手机号脱敏工具）——补充，贴合系统安全与区块链需求
- 公共实体、枚举（风险等级、二维码状态、预警类型、企业认证状态等）——补充枚举类型，完善规范
- 配置中心对接（Nacos配置中心公共配置，支撑规则热更新、服务注册发现）——补充，贴合微服务架构

## 三、补充完善总结（关键优化点）

1. **强化“联动关系”**：每个模块都补充了与其他模块的联动（如yx-trace-service联动yx-auth-service、yx-block-service），让整个系统的逻辑更闭环，体现“微服务协同”的特点，适合论文描述。
2. **贴合“监管导向”**：明确预警信息仅向监管方推送、企业认证由监管人员审核，呼应之前确定的“预警面向监管”的核心逻辑，让模块设计更有业务支撑。
3. **突出“毕设亮点”**：补充Drools规则引擎、FISCO-BCOS区块链、AI双通道模型、SM2加密等技术细节，强化论文的技术创新性和严谨性。
4. **完善“安全与性能”**：补充数据脱敏、规则热更新、预警重试机制等，贴合之前提出的系统要求，让模块功能更完整。

## 四、最终结论

###### 原始的模块总结已经非常扎实，上述优化均为“小幅补充、精准完善”，不改变原有设计，仅让模块描述更严谨、更贴合毕设需求和论文写作。优化后，可直接作为论文“系统模块设计”章节的核心内容，清晰体现系统架构、核心功能和技术亮点。

[TOC]

## 接口清单（持续维护）

> 维护规则：后续新增/修改/删除任意接口时，必须在同一次提交中同步更新本节表格（路径、参数、作用、权限）。

> 当前版本约束（V1）：采用“一手机号一公司（企业账号与 company_id 一一绑定）”模型；企业重复提交 `POST /auth/company/apply` 视为更新同一企业申请信息，并重置为待审核。

### 1) qs-auth-service（9091）

| 方法 | 路径 | 主要参数 | 作用 | 权限 |
| --- | --- | --- | --- | --- |
| POST | `/auth/login` | `account`(可选), `username`(可选), `password`(必填, form/query) | 用户登录并签发 JWT（含 role/companyId） | 公开 |
| POST | `/auth/register/user` | Body:`username`,`phone`,`password`,`role(COMPANY/CONSUMER)`,`companyName?`,`remark?` | 普通用户/企业注册 | 公开 |
| POST | `/register/user` | 同上（兼容路径） | 同上 | 公开 |
| POST | `/auth/register/admin` | Body:`username`,`phone`,`password`,`companyName?`,`remark?` | 临时管理员注册（测试用） | 公开 |
| POST | `/register/admin` | 同上（兼容路径） | 同上 | 公开 |
| GET | `/auth/company/status/{companyId}` | Path:`companyId` | 查询企业认证状态 | 公开 |
| POST | `/auth/company/apply` | Body:`companyName`,`remark?`（`companyId`仅管理员可显式传） | 企业提交认证申请（重复提交=更新同一申请并重置待审核） | `COMPANY/ADMIN` |
| GET | `/auth/company/pending` | 无 | 查询待审核企业列表 | `ADMIN` |
| PUT | `/auth/company/review/{companyId}` | Path:`companyId` + Body:`approved`,`companyName?`,`remark?` | 审核企业认证（通过后自动触发企业主档初始化） | `ADMIN` |
| PUT | `/auth/company/review/{companyId}?approved=true/false` | Path:`companyId` + Query:`approved` | 审核兼容接口 | `ADMIN` |

### 2) qs-trace-service（9092）

| 方法 | 路径 | 主要参数 | 作用 | 权限 |
| --- | --- | --- | --- | --- |
| POST | `/trace/company/init` | Body:`companyId`,`name`,`level?`,`address?`,`contactPhone?`,`status?` | 内部幂等初始化企业主档（审核通过后自动调用） | 内部调用 |
| POST | `/trace/company/create` | Body:`companyId?`,`name`,`level?`,`address`,`contactPhone`,`status?`（企业可不传 companyId，后端取 token；管理员必须传） | 企业信息建档（company） | `ADMIN/COMPANY` |
| GET | `/trace/company/list` | 无 | 企业列表查询 | 登录可访问 |
| PUT | `/trace/company/update` | Body:`companyId?`,`name`,`address`,`contactPhone`,...（企业可不传 companyId，后端取 token；管理员必须传） | 修改企业基础信息（企业不可修改 `level/status`） | `ADMIN/COMPANY` |
| PUT | `/trace/company/governance` | Body:`companyId`,`level?`,`status?` | 修改企业治理字段（等级/启用状态） | `ADMIN` |
| DELETE | `/trace/company/delete/{companyId}` | Path:`companyId` | 删除企业信息 | `ADMIN` |
| POST | `/trace/batch/create` | Body:`companyId?`,`productionDate`,`ingredients`,`productionStandard`,`totalQuantity`,`batchId?`（企业可不传 companyId，后端取 token；管理员必须传） | 产品批次建档（product_batch） | `ADMIN/COMPANY` |
| GET | `/trace/batch/list` | Query:`companyId?` | 批次列表查询 | 登录可访问 |
| POST | `/trace/qs/generate` | Body:`batchId`,`companyId?`,`status?`,`maxAllowedScans?`（企业可不传 companyId，后端取 token；管理员必须传） | 二维码生成与绑定（qs_code） | `ADMIN/COMPANY` |
| GET | `/trace/qs/image/{fileName}` | Path:`fileName`（如 `{qsId}.png`） | 获取服务端生成的二维码图片 | 公开 |
| GET | `/trace/scan/index.html` | Query:`qsId`,`batchId`,`companyId`,`payload`,`signature` | 扫码入口页：展示溯源信息并自动上报扫码行为 | 公开 |
| GET | `/trace/query/{qsId}` | Path:`qsId` | 公开查询二维码溯源详情（扫码页调用） | 公开 |
| GET | `/trace/qs/get/{qsId}` | Path:`qsId` | 查询单个二维码溯源详情 | 登录可访问 |
| GET | `/trace/qs/list` | 无 | 二维码列表 | 登录可访问 |
| PUT | `/trace/qs/{qsId}/status` | Path:`qsId` + Body:`status` | 管理员手动变更二维码状态 | `ADMIN` |
| PUT | `/trace/qs/{qsId}/status/internal` | Path:`qsId` + Body:`status` | 系统自动处置二维码状态（预警联动） | `ADMIN/SERVICE` |

### 3) qs-scan-service（9093）

| 方法 | 路径 | 主要参数 | 作用 | 权限 |
| --- | --- | --- | --- | --- |
| POST | `/scan/report` | Body:`qsId`,`batchId?`,`companyId?`,`signature`,`signaturePayload`,`ip?`,`deviceFingerprint`,`browser?`,`latitude?`,`longitude?`,`expectedLatitude?`,`expectedLongitude?`,`locationSource?` | 上报扫码日志并触发验签、风控评估 | 公开 |
| GET | `/scan/logs/{qsId}` | Path:`qsId` | 按二维码查询扫码日志 | 公开 |

### 4) qs-alert-service（9094）

| 方法 | 路径 | 主要参数 | 作用 | 权限 |
| --- | --- | --- | --- | --- |
| POST | `/alert/evaluate` | Body:`qsId`,`companyId`,`scanCount1h`,`deviceCount1d`,`ipCount1h`,`timeVariance`,`locationVariance`,`newDevice?`,`riskDevice?`,`distanceKm?`,`city?`,`province?`（兼容旧字段`scanCount/deviceCount/ipCount`） | 规则引擎 + AI 双通道风险评估并生成预警/联动动作 | 登录可访问 |
| GET | `/alert/list` | 无 | 预警记录列表 | 登录可访问 |
| GET | `/alert/rules` | 无 | 查询规则配置（来自`alert_rule`） | 登录可访问 |
| PUT | `/alert/rules/{ruleId}/threshold` | Path:`ruleId` + Body:`threshold`（如`1h>=8`） | 动态修改阈值并立即热更新Drools | 登录可访问 |
| PUT | `/alert/rules/{ruleId}/status` | Path:`ruleId` + Body:`status`（0禁用/1启用） | 启停规则并立即热更新Drools | 登录可访问 |
| POST | `/alert/rules/reload` | 无 | 手动触发规则重载 | 登录可访问 |

### 5) qs-block-service（9095）

| 方法 | 路径 | 主要参数 | 作用 | 权限 |
| --- | --- | --- | --- | --- |
| POST | `/block/proof/qr` | Body:`qsId`, 其他业务字段可附带 | 保存二维码创建存证 | 登录可访问 |
| POST | `/block/proof/event` | Body:`eventId`, 其他业务字段可附带 | 保存预警事件存证 | 登录可访问 |
| POST | `/block/proof/freeze` | Body:`qsId`, 其他业务字段可附带 | 保存冻结动作存证 | 登录可访问 |
| GET | `/block/proof/verify` | Query:`businessKey`,`hash` | 校验指定业务哈希是否存在 | 登录可访问 |
| GET | `/block/proof/list/{businessKey}` | Path:`businessKey` | 查询某业务键的存证历史 | 登录可访问 |

### 6) 网关访问说明（qs-gateway-service，9090）

- 网关已配置前缀路由：`/auth/**`、`/trace/**`、`/scan/**`、`/alert/**`、`/block/**`。
- 联调优先建议走网关地址：`http://localhost:9090` + 上述路径。
- 直连端口（9091~9095）可用于服务单测与问题定位。







# 攸县香干区块链溯源系统 — 完整鉴权逻辑流程（答辩专用）

## 统一说明

- 技术栈：Spring Cloud Gateway + Spring Security + JWT + MyBatis-Plus
- 全局流程：**网关鉴权 → 认证授权 → 资源服务鉴权 → 业务权限校验**
- 所有流程均标注对应代码位置，方便答辩讲解

------

## 一、统一主链路（正常访问流程）

1. 前端调用登录接口：

   ```
   POST /auth/login
   ```

   

   对应代码：

   ```
   qs-auth-service/src/main/java/org/hunau/auth/controller/LoginController.java
   ```

2. 认证服务加载用户信息：

   ```
   SysUserDetailsService
   ```

   

   对应代码：

   ```
   qs-auth-service/src/main/java/org/hunau/auth/service/impl/SysUserDetailsService.java
   ```

   

   查询表：

   ```
   yx_company_auth.auth_user
   ```

3. 密码校验：`BCryptPasswordEncoder.matches`

4. 签发令牌：

   ```
   JwtUtil.generateToken
   ```

   

   对应代码：

   ```
   qs-common/src/main/java/org/hunau/common/util/JwtUtil.java
   ```

5. 后续请求统一经过

   网关过滤器

   ：

   ```
   JwtAuthGlobalFilter
   ```

   

   对应代码：

   ```
   qs-gateway-service/src/main/java/org/hunau/gateway/filter/JwtAuthGlobalFilter.java
   ```

6. 网关放行 → 进入下游业务服务

7. 下游服务过滤器：`JwtAuthFilter` 将用户信息写入 `SecurityContext`

8. `SecurityFilterChain` 完成最终权限判定

------

## 二、场景化鉴权流程（10 类核心场景）

### 场景 1：登录成功

- 触发条件：输入正确用户名 + 密码，用户状态 `status=1`

- 流程：

  1. 请求到达 `LoginController`
  2. `SysUserDetailsService` 查询 `auth_user` 表，用户存在
  3. 密码匹配成功
  4. 生成 JWT 令牌并返回

  

- 响应：`R.ok(token)`

- 后续：请求携带 `Authorization: Bearer <token>` 可访问对应权限接口

### 场景 2：用户名不存在

- 触发条件：用户名在库中无记录

- 流程：

  1. 登录请求 → `SysUserDetailsService` 查询结果为空
  2. 抛出：`UsernameNotFoundException`

  

- 响应：登录失败（全局异常处理器返回错误）

### 场景 3：密码错误

- 触发条件：用户存在，但密码不匹配

- 流程：

  1. `BCryptPasswordEncoder.matches` 验证失败

  

- 响应：`R.fail("密码错误")`

- 结果：不生成 token，请求直接结束

------

### 场景 4：不带 token 访问受保护接口

- 触发条件：请求未携带 `Authorization` 请求头

- 处理位置：**网关层（直接拦截，不到下游服务）**

- 流程：

  1. `JwtAuthGlobalFilter` 检测到无 token
  2. 接口不在白名单

  

- 响应：`401 Unauthorized`

### 场景 5：token 格式错误 / 签名非法

- 触发条件：token 被篡改、格式错误
- 处理位置：**网关层**
- 流程：`JwtUtil` 解析 token 签名失败
- 响应：`401 Unauthorized`

### 场景 6：token 过期

- 触发条件：token 过期（exp < 当前时间）
- 处理位置：**网关层**
- 流程：网关解析过期时间
- 响应：`401 Unauthorized`

------

### 场景 7：token 合法，但角色权限不足（越权访问）

- 触发条件：已登录，但角色不满足接口要求

- 处理位置：**下游业务服务层**

- 流程：

  1. 网关校验通过
  2. 接口使用 `@PreAuthorize` 做角色 / 权限控制
  3. 角色不匹配

  

- 响应：`403 Forbidden`

- 备注：项目已具备注解能力，可通过权限矩阵完善控制粒度

### 场景 8：token 合法，接口只需登录即可访问

- 触发条件：已认证用户访问普通接口
- 流程：网关通过 + 服务端认证通过
- 响应：正常业务结果 `2xx`

------

### 场景 9：企业认证状态校验（业务级鉴权，重点）

- 触发条件：企业在溯源服务生成二维码前

- 处理位置：**业务逻辑层（非登录鉴权）**

- 流程：

  1. `trace-service` 调用 `auth-service` 远程接口：`TraceExternalClient`
  2. `CompanyAuthService` 查询 `company_auth.review_status`
  3. 只有 `review_status=1`（审核通过）才允许生成溯源码

  

- 失败响应：业务异常 `企业未通过认证审核`

- 答辩亮点：区分**登录鉴权**与**业务权限鉴权**

### 场景 10：微服务间调用 + 区块链存证

- 触发条件：预警 / 溯源数据上链存证

- 流程：

  1. 业务服务调用 `block-service`
  2. 数据写入：`yx_blockchain_proof.blockchain_proof`
  3. 对应类：`ProofService`

  

- 特点：

  - 支持同一业务键多次存证，满足溯源可追溯要求
  - 当前为内部可信调用，未启用 service token（可扩展零信任安全）

  

------

## 三、答辩可用总结语

1. 本项目采用 **网关统一鉴权 + 服务细粒度授权 + 业务权限校验** 三层安全模型。
2. 所有外部请求必须经过网关校验 JWT，保证身份合法。
3. 内部服务通过 Feign 调用，并增加企业认证状态等业务鉴权，确保食品溯源数据可信。
4. 关键数据（溯源信息、预警记录、异常扫码）均上链存证，实现防篡改、可追溯。



```
package org.hunau.auth.controller;

import org.hunau.auth.details.SysUserDetails;
import org.hunau.common.R;
import org.hunau.common.util.JwtUtil;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public LoginController(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public R<String> login(@RequestParam String username,
                           @RequestParam String password) {
        SysUserDetails user = (SysUserDetails) userDetailsService.loadUserByUsername(username);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return R.fail("密码错误");
        }

        String token = JwtUtil.generateToken(username, user.getRole());
        return R.ok(token);
    }
}

/*

// 仅管理员可访问
@PreAuthorize("hasRole('ADMIN')")

// 仅企业可访问
@PreAuthorize("hasRole('COMPANY')")

// 仅消费者可访问
@PreAuthorize("hasRole('CONSUMER')")

// 管理员 + 企业均可访问
@PreAuthorize("hasAnyRole('ADMIN','COMPANY')")

 */
```



# 攸县香干区块链溯源系统 — 模块映射与开发流程（答辩专用版）

## 一、模块统一映射（必背）

为便于项目管理与答辩讲解，统一模块命名如下：

表格





| 项目模块                        | 对应代码模块       | 核心职责                     |
|-----------------------------| ------------------ | ---------------------------- |
| **yx-auth-service:9091**    | qs-auth-service    | 登录认证、企业审核、用户权限 |
| **yx-trace-service:9092**   | qs-trace-service   | 企业、批次、二维码主数据管理 |
| **yx-scan-service:9093**    | qs-scan-service    | 扫码日志、设备画像采集       |
| **yx-alert-service:9094**   | qs-alert-service   | 风险规则、预警生成、处置动作 |
| **yx-block-service:9095**   | qs-block-service   | 区块链存证、数据防篡改       |
| **qs-common**               | qs-common          | 工具类、JWT、通用返回（R）   |
| **qs-gateway-service:9090** | qs-gateway-service | 统一入口、JWT 鉴权、路由转发 |

------

## 二、业务执行顺序（从简单到复杂）

### 1. 流程链路

1. **用户登录 / 注册**：系统身份入口；**前置**：无；**产出**：JWT 令牌
2. **企业认证审核**：决定企业是否具备生产资格；**前置**：管理员登录；**产出**：`company_auth.review_status`
3. **企业信息建档**：审核通过后系统自动初始化企业主档（`company`），企业可再完善信息；**前置**：企业认证通过
4. **产品批次建档**：创建生产批次（`product_batch`）；**前置**：企业存在且可用
5. **二维码生成与绑定**：创建溯源码（`qs_code`）并关联批次；**前置**：批次存在、企业审核通过
6. **二维码签名 / 验真**：生成 SM2 国密签名；**前置**：二维码主数据已生成
7. **二维码上链存证**：写入区块链存证（`blockchain_proof`）；**前置**：二维码生成成功
8. **扫码采集**：记录扫码日志（`scan_log`）+ 设备地理画像；**前置**：二维码可用
9. **规则 / AI 风险判定**：规则引擎 + 复用特征计算（`reuse_pattern`）；**前置**：有扫码数据
10. **生成预警记录与动作**：写入预警（`alert_record`）与执行动作（`alert_action`）；**前置**：风险命中
11. **高风险联动处置**：自动冻结二维码（`qs_code.status=frozen`）；**前置**：达到冻结策略
12. **处置上链与闭环**：处置事件存证 + 监管处理完成；**前置**：已有预警与处置动作

### 2. 一句话依赖链

**鉴权可信 -> 主数据可信 -> 行为数据充足 -> 风险识别 -> 自动处置 -> 可审计闭环**

------

## 三、代码开发四阶段（推荐顺序）

### 阶段 M1：鉴权打通（最小可用）

- **目标**：实现登录获取 Token，接口能正确拦截鉴权
- **核心表**：`yx_company_auth.auth_user`、`yx_company_auth.company_auth`
- **核心接口**：登录、注册、企业审核基础接口
- **核心配置**：网关白名单、`SecurityFilterChain`、`@PreAuthorize`角色控制
- **验收标准**：401/403 状态码正确，角色权限区分清晰

### 阶段 M2：溯源主流程（企业 -> 批次 -> 二维码）

- **目标**：实现 “一码一物” 的生成与查询
- **核心表**：`company`、`product_batch`、`qs_code`
- **核心接口**：企业建档、批次创建、二维码生成 / 查询
- **核心逻辑**：生成二维码前**强制校验**企业审核状态
- **联调**：`trace-service` -> `auth-service`
- **验收标准**：未审核企业禁止发码，已审核企业可正常发码

### 阶段 M3：扫码 + 预警 + 冻结（核心亮点）

- **目标**：实现 “扫码 -> 判险 -> 预警 -> 冻结” 自动化闭环
- **核心表**：`scan_log`、`reuse_pattern`、`alert_rule`、`alert_record`、`alert_action`
- **核心接口**：扫码上报、预警查询、二维码处置
- **联调**：`scan-service` -> `alert-service` -> `trace-service`
- **验收标准**：模拟高频扫码可触发预警，高风险自动冻结二维码

### 阶段 M4：区块链存证与答辩材料

- **目标**：关键数据上链，实现防篡改可追溯
- **核心表**：`blockchain_proof`
- **核心接口**：存证写入、存证核验查询
- **联调**：`trace/alert-service` -> `block-service`
- **验收标准**：二维码创建、预警、冻结事件均可查询存证记录

### 7) 数据库补丁执行顺序（生产/联调升级）

> 目标：在不重建库的前提下，兼容新增二维码长链接与扫码风险字段。

1. 先升级 `yx_trace_core.qs_code.qs_url` 字段长度（避免二维码生成时报 `Data too long for column 'qs_url'`）。
2. 再升级 `yx_scan_anomaly.scan_log` 扩展字段（支持设备画像/地址/距离风控）。
3. 最后重启 `qs-trace-service` 与 `qs-scan-service`（确保新字段映射生效）。

```sql
SOURCE E:/Code/project/graduation/sql/patch_qs_url_length.sql;
SOURCE E:/Code/project/graduation/sql/patch_scan_log_extensions.sql;
SOURCE E:/Code/project/graduation/sql/patch_drop_scan_log_sensitive_fields.sql;
```

校验建议：

```sql
USE yx_trace_core;
SHOW COLUMNS FROM qs_code LIKE 'qs_url';

USE yx_scan_anomaly;
SHOW COLUMNS FROM scan_log;
```
