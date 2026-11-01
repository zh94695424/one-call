# 一通话 ONECALL 产品需求与技术方案

> 文档状态：产品评审稿  
> 文档版本：v0.7
> 基线日期：2026-10-21
> 适用范围：微信小程序、客户 Web、自建 IVR、Java 服务端、精简运营后台

当前开通/订购与本期后台范围以 [09-business-activation-and-lean-admin.md](09-business-activation-and-lean-admin.md) 为准，统一演示套餐、状态转换和线下客服见 [10-cross-channel-rules.md](10-cross-channel-rules.md)。S1 共享规则、S2 运营后台与 S3 客户页面/IVR 展示已实现并通过本地 QA，证据见 [S3 implementation](gateflow/cross-channel-alignment-s3-implementation.md)；后续评审与验收仍由主控 gate 决定。01—08 中运营商订购、未实名禁止全部取证、旧路由及在线工单/退款/发票约束为历史稿；当前跨端交接为显式本地 fixture，不代表生产联调。

S3 独立 review 后的三项修复、针对性回归及截图增量见 [S3 fix](gateflow/cross-channel-alignment-s3-fix.md)；其结论仅为修复交接，不替代复审与验收。

2026-10-25 三端原型同步：小程序、客户 Web 与运营后台已用独立本地 call-task fixture 演示待拨任务、30 秒冻结到期、IVR 原子领取、按冻结被叫自动外呼、无任务 DTMF 确认被叫，以及录音上传/证据处理分离；Web 与运营侧均为只读诊断。该 fixture 独立于严格业务快照，仅为本地评审证据，不代表生产接口、真实线路、媒体引擎、并发或持久化已联调。

账号体系与当前原型入口见 [11-account-identity-and-workspaces.md](11-account-identity-and-workspaces.md)：自然人账户统一，微信 / 登录手机号、企业成员关系和业务号码分域；企业使用成员工作空间，运营员工账号独立。

生产目标技术选型、模块边界、部署拓扑、IVR 机房连接、数据真源和首发实施顺序见 [12-technology-stack-and-system-architecture.md](12-technology-stack-and-system-architecture.md)。该文档采用适合 5 人团队的模块化单体方案，不把当前静态原型视为生产实现。

微信小程序拨号与现有自研 IVR 声讯平台的生产对接见 [13-ivr-call-task-and-inbound-design.md](13-ivr-call-task-and-inbound-design.md)：同一主叫单一待拨任务、默认 30 秒可配置有效期、首次有效呼入原子领取并预占、按冻结被叫自动外呼、接通桥接录音（任务路径无需确认被叫号码）、通话/时长/录音上传和跨端状态均以该文档为准。

## 1. 文档目的

本目录将现有 PRD、页面截图、Logo 资产和公证处存证接口资料整理为一套统一、可追踪的产品评审基线。它用于产品、设计、研发、测试、安全、法务和外部供应商共同评审，不等同于已经完成的法律意见、正式供应商接口协议或生产上线批准。

首发目标覆盖：

- 小程序和 Web 支持开通订购；自建 IVR 支持按键开通，未完成平台注册实名认证也可直接试用并产生录音证据，之后验证认领。
- 通过取证电话、相册、现场拍摄、微信会话文件和聊天截图形成证据。
- 对固证后的通话录音进行 AI 转写和结构化要点摘要。
- 使用证据箱和“事情”组织证据；原始证据可自动对接公证处存证平台，并可获取存证验证报告、导出证据包或申请线下公证书。
- 通过个人/企业订阅套餐和加量包管理录音、存储、AI 与出证权益。
- 通过精简运营后台管理用户、内部业务开通、套餐版本、试用规则及订单查询；工单、退款、发票暂由线下人工客服支持。

## 2. 文档导航

| 文档 | 用途 | 主要评审人 |
| --- | --- | --- |
| [01-source-audit.md](01-source-audit.md) | 原始资料、冲突、缺口、资料优先级 | 产品、设计、法务 |
| [02-product-requirements.md](02-product-requirements.md) | 用户流程、页面状态、业务规则和文案 | 产品、设计、测试、法务 |
| [03-technical-solution.md](03-technical-solution.md) | 系统边界、状态机、接口域、安全与运维 | 架构、研发、安全、供应商 |
| [04-acceptance-roadmap.md](04-acceptance-roadmap.md) | 追踪矩阵、验收场景、阶段与上线门禁 | 项目、测试、全体评审方 |
| [05-yuzhang-notary-integration.md](05-yuzhang-notary-integration.md) | 公证处哈希存证、验证报告和线下公证书协同 | 架构、产品、法务、公证处、供应商 |
| [06-miniapp-ui-spec.md](06-miniapp-ui-spec.md) | 小程序页面集、导航、组件和视觉状态 | 产品、设计、客户端、测试 |
| [07-web-capability-hub-spec.md](07-web-capability-hub-spec.md) | 证枢 Web 端个人/企业工作台、证据与通话管理、权限和响应式规则 | 产品、设计、Web、服务端、测试 |
| [08-operations-admin-prototype-plan.md](08-operations-admin-prototype-plan.md) | 平台运营后台原型的独立 IAM、权限、路由、状态机、实施切片和 QA 计划 | 产品、运营、安全、法务、设计、Web、测试 |
| [09-business-activation-and-lean-admin.md](09-business-activation-and-lean-admin.md) | 当前业务基线：自建 IVR、三入口开通订购、免实名试用、认领和 5 人团队精简后台 | 产品、研发、运营、测试 |
| [10-cross-channel-rules.md](10-cross-channel-rules.md) | 当前共享演示规则：套餐、状态转换、快照边界与线下客服口径 | 产品、研发、运营、测试 |
| [11-account-identity-and-workspaces.md](11-account-identity-and-workspaces.md) | 账号表职责、登录身份、企业成员空间、业务号码边界和原型评审入口 | 产品、研发、安全、测试 |
| [12-technology-stack-and-system-architecture.md](12-technology-stack-and-system-architecture.md) | 生产目标技术栈、模块化单体、IVR/网络/部署拓扑、数据真源、安全、运维和实施顺序 | 架构、前后端、IVR、测试、安全、运维 |
| [13-ivr-call-task-and-inbound-design.md](13-ivr-call-task-and-inbound-design.md) | 小程序待拨任务、自研 IVR 呼入决策、30 秒失效、原子领取/预占、自动外呼桥接录音、通话/时长/录音上传和验收 | 产品、小程序、服务端、IVR、测试、安全 |
| [18-production-capacity-and-deployment-spec.md](18-production-capacity-and-deployment-spec.md) | 参数化容量、部署、恢复与证据清单 | 架构、运维、安全 |
| [19-production-acceptance-matrix.md](19-production-acceptance-matrix.md) | 生产验收矩阵、六轴覆盖与证据状态 | 架构、QA、安全、运维、产品 |

## 3. 已锁定的产品基线

| 主题 | 决策 |
| --- | --- |
| 客户端 | 原生微信小程序、客户 Web、自建 IVR |
| 服务端 | Spring Boot、PostgreSQL、Redis、对象存储、消息队列 |
| 用户范围 | IVR 号码级未实名用户可开通试用；平台身份、公证资格单独管理，未成年人政策待法务确认 |
| 账号与企业空间 | 一个自然人客户账户，通过已登记登录身份登录；企业权限来自成员关系，业务号码不自动获得登录权；运营员工账号独立 |
| 号码数量 | 个人账户手机号与独立固话合计最多三个；企业成员手机号上限与组织独立固话名额分开配置；每个号码独立验证、授权和开通 |
| 号码验证与公证 | 手机号可用微信/短信验证，公证前做三要素；独立固话使用运营商真实主叫验证，公证前按机构规则核验登记归属或合法使用权 |
| 电话接入 | 小程序为手机号创建唯一短期待拨任务并拉起系统电话拨入；默认 30 秒有效且服务端可配置，现有自研 IVR 原子领取后外呼桥接、录音并上传通话/时长/录音；用户也可直接呼入开通试用，固话由独立线路接入；企业总机、PBX、分机和坐席 PIN 暂不设计 |
| 取证入口 | 运营商电话、相册照片/视频、现场拍摄、微信会话文件、聊天截图 |
| AI | 固证后且权益充足时自动转写和摘要；结果可追溯、不可覆盖原证据 |
| 公证处存证 | 五类原始证据在内部固证后自动提交 SHA-256 和最小元数据；仅在成功取得 `recordNo` 后显示完成 |
| 存证报告 | 支持生成“公证处电子数据存证验证报告”；它不是公证书，也不直接证明基础法律事实 |
| 线下公证书 | 客户提交申请后由人工线下对接公证处；本期不建设后台工单流转，不承诺在线自动出具 |
| 免费试用 | 三入口共用首次资格；默认 300 秒，IVR 按键确认后无需实名可试用并产证；认领不重发，耗尽主动结束取证通话 |
| 套餐 | 5 分钟免费试用、个人基础/进阶、企业基础/旗舰，加录音/存储/AI/出证加量包；价格、坐席、固话名额和权益由后台版本配置 |
| 到期策略 | 既有证据只读保留，不自动删除；暂停新增取证、AI 和出证申请 |
| 运营后台 | 15 个业务路由（6 个主入口 + 9 个次级路由）、管理员/运营两类权限；用户、开通、IVR、套餐、试用、订单与操作记录；财务售后流程线下支持 |
| 部署基线 | 中国大陆境内生产部署和敏感数据保护基线 |
| 设计准绳 | 页面布局以截图为先，规则和文案以 PRD 为先，品牌统一使用方案 B SVG Logo |

## 4. 需求编号

| 前缀 | 领域 |
| --- | --- |
| `ACC` | 账户、实名和风险升级 |
| `PHN` | 号码绑定、授权和内部业务开通 |
| `EVI` | 取证、上传、固证和证据箱 |
| `AI` | 转写、摘要、版本和用户注释 |
| `SUB` | 套餐、权益、计量和支付 |
| `ENT` | 企业组织、成员、坐席、组织号码和共享权益 |
| `CRT` | 出证、公证资格和交付物 |
| `NPS` | 公证处存证、存证验证报告和平台对账 |
| `OPS` | 运营后台、人工介入和审计 |
| `NFR` | 安全、合规、性能和可用性 |

需求、技术模块和验收用例必须通过这些编号建立追踪关系。新增需求不得只写在页面稿或会议纪要中。

## 5. 资料优先级

出现冲突时按以下顺序处理：

1. 本文档集内已记录且通过评审的决策。
2. 外部能力、字段和状态以正式供应商协议/接口文档及沙箱验证结果为基准；演示样例只能证明其明确展示的结构。
3. 页面布局与交互以 `figma设计图截图/` 为基准。
4. 业务规则和页面文案以原始 PRD 为基准。
5. 品牌标志以 `logo/` 下四个方案 B SVG 为基准。
6. 无法由资料确认的内容进入阻断决策清单，不由研发自行猜测。

## 6. 评审状态定义

| 状态 | 含义 |
| --- | --- |
| 草拟 | 内容尚未完成，不作为开发输入 |
| 产品评审 | 产品范围和交互可评审，仍可能调整 |
| 技术评审 | 系统边界和关键方案可评审 |
| 法务/供应商待确认 | 依赖外部事实，不得自行关闭 |
| 已批准 | 负责人和日期已记录，可进入实现 |
| 已废弃 | 被新版本替代，仅保留追溯用途 |

当前文档集整体状态为“产品/技术评审”。在 [04-acceptance-roadmap.md](04-acceptance-roadmap.md) 的上线门禁全部关闭前，不得标记为“已批准上线”。

## PA10-S1 implementation evidence

- [Implementation artifact](gateflow/pa10-sharing-application-service-s1-implementation.md): query-only PostgreSQL runtime and internal read-only `listCustomerShares`, strict creator scope, current ACTIVE context checks, sanitized eleven-field metadata and keyset paging.
- [Completion report](gateflow/pa10-sharing-application-service-s1-completion-1.md): local command/report inventories and frozen-boundary evidence; not controller acceptance or remote CI approval.
- The optional `persistence` profile uses explicit query-only credentials and fixed transaction-local `sharing_role`; default non-web startup remains database-free. Flyway is test/CI only. No public API, sharing mutation, playback authorization, policy values, schema/ACL changes or production readiness is claimed.

## 7. 核心术语

| 术语 | 定义 |
| --- | --- |
| 局端录音 | 本项目由部署于运营商机房的自建 IVR/声讯系统采集的通信侧录音，不是手机本地录音，不等同运营商提供业务系统 |
| 固证 | 对接收的原始内容计算哈希、写入可信时间证明并形成不可覆盖记录的过程 |
| 公证处存证 | 将证据哈希和经批准的最小元数据提交至公证处电子数据存证平台，并取得 `recordNo` 的外部过程 |
| 存证验证报告 | 基于 `recordNo` 生成、用于验证存证记录和数据完整性的报告；不是公证书 |
| 线下公证书 | 公证机构依申请和法定程序另行审查、受理并出具的法律文书；首发由运营协同线下办理 |
| 原始证据 | 自建 IVR 采集的原始通话录音或用户提交的原始文件；派生内容不得覆盖它 |
| 派生内容 | 预览、转码、逐字稿、摘要、缩略图等由原始证据生成的内容 |
| 事情 | 用户为同一纠纷或事实组织多份证据的集合 |
| 主叫号码 | 发起取证的手机号或独立固话；以可信接入线路传递至自建 IVR 的真实主叫匹配业务主体，用量以 IVR 有效录音事件确认 |
| 号码验证 | 微信表示用户提供经平台验证的手机号；短信表示当前可接收短信；固话主叫验证表示可从该线路完成验证呼叫，均不直接代表实名或产权一致 |
| 三要素核验 | 姓名、大陆居民身份证号和手机号的运营商实名一致性核验，仅适用于手机号公证资格 |
| 坐席 | 企业组织内可使用共享权益的成员资格，不等同于手机号、固话、分机或并发线路 |
| AI 引用 | 摘要要点所对应的说话人、逐字稿原文和录音时间范围 |
| 权益 | 套餐、活动或人工补偿赋予用户的可消费额度或能力 |

## 8. 当前上线阻断项

- 自建 IVR 对接后台的会话/确认/录音协议、服务鉴权、重试幂等与可信主叫；不再有运营商订购开通接口依赖。
- 自建 IVR 按服务端剩余秒数限时和主动挂断的实现、误差与结果事件。
- 基础线路覆盖、真实主叫标准化、固话验证、短码防重放及声讯路由能力；认领中的号码回收风险与历史证据访问规则。
- 实名二要素、人脸活体和运营商三要素供应商及数据处理协议。
- 公证处存证平台的正式 SDK/接口、签名与回调验签、生产环境、`productKey`/`stepKey`、动态字段和错误语义。
- 附件上传/补传、原文件托管和保存删除规则；资料未补齐前仅按“哈希存证”设计。
- 存证验证报告的正式样例、签章/长期验证、下载 URL 规则，以及存证编号和时间字段的权威含义。
- 线下公证书受理范围、材料、地域、时效、费用、补件、拒绝、退款、交付和证据调取 SOP。
- AI 服务部署位置、数据不用于训练的合同保证、准确率与 SLA。
- 套餐灰度初始价格与额度已形成建议配置；生产目录审批、退款规则、发票流程及未来是否支持自动续费仍需确认。
- 企业基础/旗舰版的正式价格、坐席与独立固话名额、共享额度、合同报价及组织 RBAC 仍需确认；企业总机/PBX 不作为本期阻断项，因为已明确排除。
- 文件大小上限、证据保存期限、用户删除与法定留存规则。
- 未成年人是否允许开户，以及监护人流程是否需要建设。
- 所有对外协议、隐私文本、录音提示音和司法效力表述的法务定稿。
