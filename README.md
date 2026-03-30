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

