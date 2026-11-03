## 1.1.1 补丁（2026-11-04）

MUI2 界面也在独立服务端构建容器。MixinCraftingInputGui 必须放在 common mixins 组，不能放 client 组，否则客户端 128 样板槽、服务端 36 样板槽，点击大编号槽会导致 IndexOutOfBoundsException。SERVER 环境的 NativeMixinApplyTest 已加入原版 GUI 类；先复现旧配置失败，再验证修复通过。1.1.1 沿用本次用户授权的 GitHub 发布流程，实机测试尚待完成。

## 当前实现（2026-11-03，1.1.0）

目标为 GTNH 2.9.0 / GT5U 5.09.54.265。巨型主仓继承原版 MTEHatchCraftingInputME；巨型镜像直接注册原版 MTEHatchCraftingInputSlave。Mixin 仅按内部机器名称扩展容量和 GUI，以及隔离原版/巨型镜像链接。保留 128 槽、9 个共享槽、原有 MTE ID/偏移及三条巨型获取配方。移除了复制的加工代码和强制写 LV 说明。详细变更与存档验证清单见 RELEASE-1.1.0.md。

构建：./gradlew build --offline --no-configuration-cache；check 包含 verifyNativeReuse、verifyNativeMixinServer、verifyNativeMixinClient。可用 -PgtmpiRuntimeGtJar=<实际游戏GT.jar> 同时验证最终重混淆发布包。离线检查通过。1.1.0 根据用户本次明确指示在实机测试前提交并发布；其余版本仍遵循原有实机验证约定。

以下为旧版实现的历史记录，涉及复制类、MUI1 和 2.8.4 的说明不代表当前架构。

# GT-Mega-Pattern-Input — 项目备忘录

GTNH 2.8.4 专用 GT 附加模组：新增 **8×16（128 格）** 巨型样板输入总线/总成/镜像，解决原版样板输入仓 4×9（36 格）容量不足。modid `gtmpi`，作者 lelqwq。

## 一句话原理

不 fork GT5U：把 GT5U 5.09.51.482 的 `MTEHatchCraftingInputME`（主仓）与 `MTEHatchCraftingInputSlave`（镜像）复制到本 mod 包内，改 `MAX_PATTERN_COUNT = 8*16` + `rows()/rowSize()` + GUI 每行 16 槽 + `getGUIWidth()+142`/`getGUIHeight()+72` + 按钮与电路板槽右移 126。机器通过 `GregTechAPI.sAfterGTPreload` 队列在 GT preInit 末尾注册（`required-before:gregtech` 保证时序，参考 gtswn/GigaGramFab 模式）。

## 核心决策

- 新方块成对提供（复刻原版三件套）：巨型样板输入总线（物品，Tier 6）、巨型样板输入总成（防水/流体样板，Tier 10）、巨型样板输入镜像（Tier 11，`master` 字段随同包复制自动指向本 mod 总成）
- 多方块兼容依据（源码验证）：`addInputBus` 结构校验 = `MTEMultiBlockBase.addInputBusToMachineList` 的 `instanceof IDualInputHatch` + `supportsCraftingMEBuffer()`；自动摆放 `MTEHatchInputBus.class.isInstance` 对子类成立 → 继承 `MTEHatchInputBus` 即可手摆/自动摆放
- MTE ID：BASE **18000** + `Config.metaIdOffset`（gtmpi.cfg，默认 0）+ 相对 ID。18000 段已对照 482 全仓库唯一 MTE ID 枚举（`gregtech.api.enums.MetaTileEntityIDs`）验证空白（17000–29989 无占用）；gtswn 占 14620
- 版本依赖全部对齐 2.8.4 实机：GT5U 5.09.51.482、AE2 rv3-beta-695-GTNH、ModularUI 1.2.20、ModularUI2 2.2.18-1.7.10、GTNHLib 0.7.10、StructureLib 1.4.23；ae2fc 无 Maven 坐标 → `libs/ae2fc-1.4.120-gtnh.jar` 本地 `rfg.deobf` 编译（运行时由玩家实例自带，GT5U 本体同样硬依赖 ae2fc）
- 服务端与客户端均需安装（新方块 mod 常规要求）

## 已踩坑备忘

- **maven 5.09.51.482 dev jar ≠ git 标签 5.09.51.482 源码**：以 maven 的 `-sources.jar` 为权威（编译报错路径最准）；maven 版 `MetaTileEntity` 基类携带 MUI2 字段 `com.cleanroommc.modularui.utils.item.ItemStackHandler` → 必须同时依赖 MUI1 与 MUI2
- **同包引用不带 import**：复制 GT 类换包后，原同包的 `IDualInputHatch/IDualInputHatchWithPattern/IDualInputInventory/IDualInputInventoryWithPattern` 引用全部失效（症状是"找不到符号"+@Override 级联报错）——需手动补 `gregtech.common.tileentities.machines.*` import
- **编译期常量**：`MAX_PATTERN_COUNT = 4*9` 被内联，改它必须改源码重编译（这正是本 mod 选择复制类而不是 mixin 的原因）
- 纹理零成本：`getTexturesInactive` 使用 `OVERLAY_ME_CRAFTING_INPUT_BUFFER/BUS/SLAVE` 覆盖贴图，纹理本体在 gregtech jar 内，运行时直接可用
- **GUI 尺寸必须宽高联动**：MUI1 玩家背包由 GT 基类固定摆在 `(宽/2-81, 高-83)`，只加宽不加高会让 8 行格子盖住背包（症状：格子压住背包栏）；按钮/电路板槽 x=170/194 是原版 192 宽窗口的右栏绝对坐标，加宽后必须同步右移（170→296、194→320、电路板槽 170→296；原版 ×2 按钮本就悬出窗口右缘 18px，MUI1 不裁剪普通 widget，1:1 复刻）
- **AE2 接口终端是白名单制**：`ContainerInterfaceTerminal` 只遍历 `InterfaceTerminalRegistry.getSupportedClasses()`；GT5U 在 `AE2Compat.onPreInit()` 只登记原版仓类，复制类必须自己调 `AEApi.instance().registries().interfaceTerminal().register(本类.class)` 才能在接口终端显示（镜像类无 ME 节点，原版也不登记）
- **接口终端视口宽 174px 且无横向滚动**：终端条目按 `IInterfaceViewable.rows()/rowSize()` 排版（这是这两个方法的唯一消费方，本机 GUI 每行 16 槽由 SlotGroup 硬编码，互不影响）。16 列一行 288px 会被左右裁剪，因此 `rows()=16`、`rowSize()=8`（144px 完整显示，靠终端纵向滚动条查看）
- **tooltip 本地化不读 assets lang**：GT5U 的 `TileEntity_DESCRIPTION_<MTE-ID>_Index_NN` 翻译只从游戏根目录的 `GregTech.lang`/`GregTech_<locale>.lang` 读取（`GTPreLoad.initLocalization`，GT preInit 时加载；文件里无键时把描述数组英文写进文件作默认）。做法：本 mod 在 preInit（先于 GT，required-before 保证）用 `Configuration` 强制覆盖写入自己的 18 个键（`Property.set` 才能清掉历史残留），键按实际 MTE ID 生成（兼容 metaIdOffset）

## 构建

- `./gradlew spotlessApply` 后 `./gradlew build` → `build/libs/gt-mega-pattern-input-0.1.0.jar`
- 主类 `GTMegaPatternInput`（@Mod `required-before:gregtech;required-after:appliedenergistics2;`）；注册入口 `loader/MachineLoader`

## 工作流程

- 与 gtpoc/gtneioc 相同：用户实机测试每个版本，**实测通过前不提交**；提交信息中文 Conventional Commits
- 改动前先出影响分析报告、经用户批准再动手；沟通用中文

## 当前状态（2026-08-23）

- ⑪ v0.1.0 初版已构建（jar 内容已验证：mcmod.info/双语 lang/两个仓室类齐全）
- ⑫ v0.2.0 GUI 布局修复（getGUIHeight+72 / 按钮与电路板槽右移 126），**已实机验证通过**，已本地提交
- **待实机测试**：
  - 编码样板 → ME 网络识别；镜像闪存连接总成；流体样板（防水版）；存档重启保留
  - 与 gtpoc/gtneioc/gtswn 共存测试（ID 18000 段 + 14620 无冲突）
- ⑬ v0.3.0 接口终端修复（登记 `InterfaceTerminalRegistry` + `rows()=16`/`rowSize()=8` 适配 174px 视口），**已实机验证通过**，已本地提交
- ⑭ v0.4.0 工作台配方 ×3（总线 = LV 输入总线 + ME 接口；总成 = LV 输入总线 + LV 输入仓 + ME 二合一接口（ae2fc 流体接口方块 `ItemAndBlockHolder.INTERFACE`）；镜像 = 巨型总成 + 原版玻璃），**已实机验证通过**，已本地提交
- ⑮ v0.5.0 tooltip 中文本地化 + 等级行改 LV（构造函数 `TIER_COLORS[1]+VN[1]`），**已实机验证通过**，已本地提交
- ⑯ v1.0.0 正式发布：推送 GitHub（lelqwq/GT-Mega-Pattern-Input，main 分支）+ Release 附带 jar
