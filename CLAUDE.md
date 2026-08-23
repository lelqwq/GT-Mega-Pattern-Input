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
- **已知问题**：巨型样板输入总成接入 AE 网络后在接口终端中不显示（原版可见），待查
