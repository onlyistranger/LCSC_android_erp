# LCSC Android ERP 技术规划路径

## 1. 项目目标理解

根据 [project.md](/home/bc/PRJ/LCSC_android_erp/docs/project.md)，这个 App 的核心不是电商展示，而是一个偏离线、偏仓储的小型物料管理工具，首期重点是：

1. 扫描立创二维码完成入库。
2. 根据二维码中的 `pc` 编号或用户输入的关键词，到立创商城检索器件信息。
3. 将器件基础信息、数量、库位等数据保存到本地。
4. 支持本地库存搜索。
5. 支持 CSV 导入导出。

因此技术路线应该优先保证以下几点：

- 本地离线可用。
- 结构化数据可检索、可更新、可导出。
- 后续能平滑增加批次、流水、盘点、同步等能力。
- 尽量贴合当前原生 Android Kotlin 工程，不引入过重的跨端方案。

## 2. 推荐总体技术路线

### 2.1 客户端形态

- 平台：Android 原生
- 语言：Kotlin
- UI：Jetpack Compose + Material 3
- 架构：MVVM + Repository
- 异步：Kotlin Coroutines + Flow
- 依赖注入：Hilt
- 本地数据库：Room + SQLite
- 轻量配置存储：DataStore
- 条码扫描：CameraX + ML Kit Barcode Scanning
- 网络访问：Retrofit + OkHttp
- HTML 解析兜底：Jsoup
- 序列化：kotlinx.serialization
- CSV 导入导出：Apache Commons CSV 或等价 Kotlin CSV 库

### 2.2 为什么这条路线最合适

当前仓库已经是标准 Android Kotlin + Compose 工程，继续沿用原生技术栈成本最低。这个项目的数据以“器件主数据 + 库存记录 + 入库流水 + 库位”这类结构化关系数据为主，明显更适合关系型本地存储，而不是文档型或 KV 型存储。

## 3. 本地数据库选型结论

### 3.1 结论

本地数据库建议使用：`Room + SQLite`

轻量配置建议使用：`DataStore`

### 3.2 选择 Room + SQLite 的原因

1. 数据模型天然是关系型。
器件信息、库存记录、库位、入库流水、导入批次之间存在明确关联，适合表结构和外键约束。

2. 查询能力足够强。
需要按料号、名称、封装、库位、品牌、批次做组合查询，SQLite 非常适合这类条件过滤、排序、分页和聚合统计。

3. Android 官方生态最稳。
Room 是 Android 官方推荐方案，和 Kotlin、Flow、协程、分页、迁移、测试都能自然配合。

4. 后续扩展成本低。
未来如果增加盘点、库存调整、出入库流水、低库存预警、全文搜索，SQLite 仍然能承接，不需要中途换库。

5. 导入导出更直接。
CSV 导入、本地备份、字段映射、数据迁移都更容易做，调试也更透明。

### 3.3 为什么不建议直接用其他方案

- 不建议只用 `DataStore`
DataStore 适合保存用户设置、最近搜索条件、默认仓库等轻量配置，不适合库存明细这种可筛选、可关联、可批量更新的数据。

- 不建议直接裸用 `SQLiteOpenHelper`
虽然可行，但 SQL、迁移、实体映射、线程处理都要手工维护，开发和维护成本明显高于 Room。

- 不建议首期使用 Realm/ObjectBox 一类第三方对象数据库
这类库上手可能快，但对本项目没有决定性收益，反而会增加生态依赖、迁移心智负担和后续可控性风险。

### 3.4 数据库存储建议

- `Room`
负责库存核心业务数据。

- `Proto DataStore`
负责设置项，例如默认仓库、默认搜索方式、最近导出路径、UI 偏好。

- 如后续需要更强搜索能力
优先在 SQLite 上增加索引，必要时使用 SQLite FTS5 做本地全文检索，而不是单独引入另一套搜索数据库。

## 4. 推荐分层架构

建议先保持单 `app` 模块，按包分层；功能稳定后再考虑拆模块。

### 4.1 包结构建议

```text
app/src/main/java/com/example/lcsc_android_erp/
  core/
    common/
    network/
    database/
    datastore/
  data/
    repository/
    remote/
    local/
    mapper/
  domain/
    model/
    usecase/
  feature/
    inbound/
    search/
    inventory/
    importexport/
    settings/
  ui/
    theme/
    component/
```

### 4.2 分层职责

- `feature`
页面、ViewModel、UI 状态管理。

- `domain`
业务模型和用例，例如扫码入库、按料号搜索、CSV 导入匹配。

- `data`
Repository 聚合本地库、远程接口、HTML 抓取等数据源。

- `core`
通用基础设施，例如数据库、网络、日志、配置。

## 5. 核心业务模型设计

### 5.1 建议的核心表

#### `component_master`

器件主数据表，保存从立创获取到的标准器件信息。

建议字段：

- `id`
- `part_number` 对应 `pc`，如 `C17710`
- `mpn`
- `name`
- `brand`
- `package_name`
- `category`
- `spec_json`
- `description`
- `source_url`
- `updated_at`

#### `storage_location`

库位表。

建议字段：

- `id`
- `code` 如 `A1`、`C13`
- `remark`
- `created_at`

#### `inventory_item`

当前库存表，表示某个器件在某个库位上的现存量。

建议字段：

- `id`
- `component_id`
- `location_id`
- `quantity`
- `last_inbound_at`
- `updated_at`

建议唯一索引：

- `component_id + location_id`

#### `inventory_txn`

库存流水表，记录每次入库、调整、导入。

建议字段：

- `id`
- `component_id`
- `location_id`
- `txn_type` 如 `INBOUND`、`ADJUST`、`IMPORT`
- `quantity_delta`
- `source_type` 如 `QRCODE`、`MANUAL`、`CSV`
- `source_ref`
- `raw_payload`
- `created_at`

#### `import_batch`

CSV 导入批次表，便于回溯导入来源和错误处理。

建议字段：

- `id`
- `file_name`
- `status`
- `total_count`
- `success_count`
- `failed_count`
- `created_at`

### 5.2 关键关系

- 一个 `component_master` 可以对应多个 `inventory_item`
- 一个 `storage_location` 可以对应多个 `inventory_item`
- 每次库存变化都写入 `inventory_txn`
- `inventory_item` 是当前态，`inventory_txn` 是历史态

这个模型后续扩展出库、盘点、回滚都比较顺。

## 6. 立创数据接入策略

### 6.1 推荐策略

采用“接口优先，HTML 解析兜底”的双层方案：

1. 优先调研立创是否存在稳定、可公开访问的搜索接口。
2. 如果没有稳定接口，再使用搜索页和详情页 HTML 解析。
3. 所有远端返回统一映射为内部 `ComponentDetail` 模型。

### 6.2 技术实现建议

- 网络层：Retrofit + OkHttp
- 如果返回 JSON：`kotlinx.serialization`
- 如果只能抓页面：`Jsoup`
- Repository 对上层屏蔽“接口/HTML”差异

### 6.3 风险提示

立创网页结构未来可能变化，因此不能把页面字段结构直接散落在 UI 层。应集中封装在 `remote` 和 `mapper` 层，保证后续只改一处。

## 7. 二维码与手动入库方案

### 7.1 扫码入库流程

1. CameraX 打开相机预览。
2. ML Kit 识别二维码。
3. 解析原始内容，如：

```text
{on:SO2507139288,pc:C17710,pm:0805W8F4700T5E,qty:100,mc:,cc:1,pdi:166537429,hp:11}
```

4. 提取 `pc`、`pm`、`qty` 等字段。
5. 根据 `pc` 拉取器件详情。
6. 用户确认数量、库位后提交入库。
7. 写入 `inventory_txn`，同步更新 `inventory_item`。

### 7.2 二维码解析建议

不要直接依赖字符串切分硬编码到页面中，建议单独提供 `QrPayloadParser`，输出稳定的数据结构：

```kotlin
data class InboundQrPayload(
    val orderNo: String?,
    val partNumber: String,
    val manufacturerPartNo: String?,
    val quantity: Int,
    val rawText: String
)
```

### 7.3 手动入库流程

1. 用户输入关键字或料号。
2. 请求立创搜索结果。
3. 用户选择具体器件。
4. 输入数量和库位。
5. 提交入库。

## 8. 搜索与导入导出设计

### 8.1 本地库存搜索

支持以下搜索维度：

- 料号 `C17710`
- 型号/MPN
- 名称
- 品牌
- 封装
- 库位

数据库层应优先加索引：

- `component_master.part_number`
- `component_master.mpn`
- `component_master.brand`
- `component_master.package_name`
- `storage_location.code`

### 8.2 CSV 导入

推荐导入流程：

1. 使用 Android Storage Access Framework 选择文件。
2. 解析 CSV。
3. 进行字段映射与格式校验。
4. 先写入 `import_batch` 和临时内存结果。
5. 校验通过后再事务性写入 `inventory_txn` 和 `inventory_item`。
6. 输出失败行和失败原因。

### 8.3 CSV 导出

推荐导出内容：

- 当前库存表
- 器件主数据表
- 库存流水表

导出格式建议首期统一为 UTF-8 CSV，后续再补 Excel。

## 9. 建议实施阶段

### 第一阶段：基础骨架

目标：把工程从模板项目变成可扩展业务项目。

建议完成：

- 引入 Hilt、Room、DataStore、Retrofit、OkHttp、CameraX、ML Kit
- 建立分层目录
- 定义数据库实体、DAO、Repository 接口
- 建立首页导航与基础状态管理

### 第二阶段：扫码入库闭环

目标：完成最核心的“扫码 -> 拉取器件 -> 选择库位 -> 入库”。

建议完成：

- 二维码扫描页
- 二维码解析器
- 器件详情拉取
- 入库确认页
- 写入库存和流水

### 第三阶段：手动搜索入库

目标：没有二维码时也能完成入库。

建议完成：

- 搜索页
- 搜索结果列表
- 器件选择和入库确认

### 第四阶段：本地库存搜索与管理

目标：把“能录入”升级成“能管理”。

建议完成：

- 库存列表
- 条件筛选
- 库位维度查看
- 库存详情页
- 库存调整记录

### 第五阶段：CSV 导入导出

目标：和外部表格工作流打通。

建议完成：

- CSV 模板定义
- 导入校验
- 失败报告
- 当前库存导出
- 流水导出

### 第六阶段：稳定性与可维护性增强

目标：为后续长期使用做准备。

建议完成：

- Room Migration
- 网络失败重试
- 离线缓存策略
- UI 测试与 Repository 测试
- 日志与错误上报

## 10. 关键依赖建议

建议新增以下依赖方向：

- `androidx.room`
- `androidx.datastore`
- `androidx.hilt`
- `com.google.dagger:hilt-android`
- `androidx.camera`
- `com.google.mlkit:barcode-scanning`
- `com.squareup.retrofit2`
- `com.squareup.okhttp3`
- `org.jsoup:jsoup`
- `org.jetbrains.kotlinx:kotlinx-serialization-json`

## 11. 测试建议

至少应覆盖以下测试：

- 二维码解析单元测试
- 器件搜索结果映射测试
- Room DAO 测试
- 入库流程 ViewModel 测试
- CSV 导入解析测试

重点不是追求测试数量，而是先锁住最容易出错的规则解析和库存写入逻辑。

## 12. 最终建议

这个项目建议采用以下明确路线：

- 继续使用当前 `Android + Kotlin + Compose` 原生方案
- 架构采用 `MVVM + Repository`
- 本地业务数据库采用 `Room + SQLite`
- 轻量设置存储采用 `DataStore`
- 扫码采用 `CameraX + ML Kit`
- 远程检索采用 `Retrofit + OkHttp`，必要时用 `Jsoup` 兜底解析页面

其中，本地存储数据库的结论不要摇摆，首选就是 `Room + SQLite`。这套方案最符合该项目“离线库存管理 + 结构化检索 + CSV 导入导出 + 后续可扩展流水”的核心需求。
