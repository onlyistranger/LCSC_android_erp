# 物料列表组件清单

本文档整理当前项目中“物料列表渲染”的主要模式，以及各自对应的出现场景。

说明：

- 这里只统计“物料列表 / BOM 结果列表 / 物料候选列表”相关组件
- 不包含仓库总览卡片、仓库选择器等非物料列表组件
- 当前已经去除批量入库相关的遗留列表组件

## 基础骨架

### 统一物料卡片骨架

| 项目 | 内容 |
| --- | --- |
| 组件 | `MaterialListCard` |
| 文件 | `app/src/main/java/com/example/lcsc_android_erp/core/ui/MaterialListCard.kt` |
| 作用 | 统一承载“图片 + 名称 + 品牌/封装/类目 + 第二属性摘要”的主布局 |
| 当前复用场景 | `ManualSearchResultCard`、`SearchResultCard`、`BomBindingCandidateCard` |

补充：

- 这套骨架的布局来源于“仓库详情物料卡片”的表现方式
- 不同页面之间主要只保留底部按钮区和附加信息区的差异
- `BomSearchRowCard` 不复用它，因为 `BomSearchRowCard` 的主体是 BOM 行，不是单个物料

## 当前在用

### 1. 手动入库搜索结果卡片

| 项目 | 内容 |
| --- | --- |
| 组件 | `ManualSearchResultCard` |
| 文件 | `app/src/main/java/com/example/lcsc_android_erp/feature/inbound/InboundScreen.kt` |
| 渲染模式 | 立创商城搜索结果列表中的单个物料卡片 |
| 出现场景 | 入库页 `手动入库`，用户输入关键词后展示搜索结果 |
| 交互 | 点击“选择并入库”后进入统一的物料入库确认弹窗 |

补充：

- 当前用于显示联网搜索到的立创商城物料
- 现已收敛到与“仓库详情物料卡片”一致的主布局
- 底部保留“打印二维码 / 选择并入库”两个操作按钮

### 2. 物料搜索结果卡片

| 项目 | 内容 |
| --- | --- |
| 组件 | `SearchResultCard` |
| 文件 | `app/src/main/java/com/example/lcsc_android_erp/feature/search/SearchScreen.kt` |
| 渲染模式 | 本地库存物料结果卡片 |
| 出现场景 | 搜索页 `手动搜索`，展示库存中的匹配物料 |
| 交互 | 点击后进入物料详情或后续操作流程 |

补充：

- 该组件也复用于 BOM 搜索结果中的“已匹配本地物料”展示
- 这是当前“本地库存搜索结果”最通用的一种卡片
- 现已收敛到与“仓库详情物料卡片”一致的主布局，底部追加库位列表

### 3. BOM 行结果卡片

| 项目 | 内容 |
| --- | --- |
| 组件 | `BomSearchRowCard` |
| 文件 | `app/src/main/java/com/example/lcsc_android_erp/feature/search/SearchScreen.kt` |
| 渲染模式 | 以 BOM 行为中心的结果卡片 |
| 出现场景 | 搜索页 `BOM搜索`，展示上传 BOM 后逐行解析得到的结果 |
| 交互 | 支持查看已匹配 / 未匹配、忽略、绑定匹配、直接入库等操作 |

补充：

- 该组件不是纯“物料卡片”
- 它的主视角是 BOM 的一行记录，再附带匹配到的库存物料或立创料号信息
- 它是当前 BOM 页面中最上层的结果载体

### 4. BOM 绑定候选物料卡片

| 项目 | 内容 |
| --- | --- |
| 组件 | `BomBindingCandidateCard` |
| 文件 | `app/src/main/java/com/example/lcsc_android_erp/feature/search/SearchScreen.kt` |
| 渲染模式 | 绑定匹配弹窗中的候选物料列表项 |
| 出现场景 | 搜索页 `BOM搜索`，未匹配项点击“绑定匹配”后弹窗内显示 |
| 交互 | 点击某个候选项，将 BOM 行与该库存物料建立绑定关系 |

补充：

- 这是一个“弹窗内候选列表”样式
- 现已收敛到与“仓库详情物料卡片”一致的主布局
- 底部保留“绑定匹配”按钮

### 5. 仓库详情物料卡片

| 项目 | 内容 |
| --- | --- |
| 组件 | `LocationInventoryItemCard` |
| 文件 | `app/src/main/java/com/example/lcsc_android_erp/feature/inventory/InventoryScreen.kt` |
| 渲染模式 | 某个仓库内部的物料卡片 |
| 出现场景 | 库存管理中进入某个仓库详情页后，展示该仓库中的全部物料 |
| 交互 | 点击可打开物料管理弹窗，长按可进入多选批量操作 |

补充：

- 这是当前“仓库内物料明细”专用渲染方式
- 会显示仓库上下文更关心的信息，例如数量、第二属性、图片等
- 它也是当前其他物料卡片布局收敛时参考的基准样式

## BOM 相关组件关系

如果只看 BOM 搜索流程，当前涉及 3 个层级：

1. `BomSearchRowCard`
   负责承载 BOM 的“行”
   每一行可以处于已匹配、未匹配、忽略、直接入库等状态

2. `SearchResultCard`
   当 BOM 行已经匹配到本地库存时，用它来展示匹配到的库存物料
   这属于“BOM 行下挂的物料卡片”

3. `BomBindingCandidateCard`
   当 BOM 行未匹配，用户点击“绑定匹配”后，在弹窗里用它展示可选的库存物料
   这属于“BOM 绑定候选物料卡片”

可以理解为：

- `BomSearchRowCard` 是 BOM 页面的行级容器
- `SearchResultCard` 和 `BomBindingCandidateCard` 是 BOM 流程里的物料级卡片

## 结构总结

如果按产品语义划分，当前项目中的物料列表渲染大致可以归为 4 类：

1. 联网搜索结果类：`ManualSearchResultCard`
2. 本地库存结果类：`SearchResultCard`、`LocationInventoryItemCard`
3. BOM 结果类：`BomSearchRowCard`
4. 候选选择类：`BomBindingCandidateCard`

如果按代码实现统计：

- 当前在用主要有 5 种

## 后续建议

- 如果后续继续统一 UI，优先考虑让 `LocationInventoryItemCard` 也进一步直接复用 `MaterialListCard`
- `BomSearchRowCard` 应保持独立，因为它承载的是 BOM 行语义，不适合强行合并为普通物料卡片
