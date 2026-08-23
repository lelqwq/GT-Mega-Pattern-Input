package com.lelqwq.gtmpi.loader;

import static com.lelqwq.gtmpi.common.api.enums.GTMPIItemList.Mega_Crafting_Input_Buffer;
import static com.lelqwq.gtmpi.common.api.enums.GTMPIItemList.Mega_Crafting_Input_Bus;
import static com.lelqwq.gtmpi.common.api.enums.GTMPIItemList.Mega_Crafting_Input_Proxy;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.lelqwq.gtmpi.GTMegaPatternInput;
import com.lelqwq.gtmpi.common.api.enums.MetaTileEntityID;
import com.lelqwq.gtmpi.machine.MTEHatchCraftingInputME;
import com.lelqwq.gtmpi.machine.MTEHatchCraftingInputSlave;

import appeng.api.AEApi;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;

/**
 * 机器加载器：在 GT preInit 末尾（{@code GregTechAPI.sAfterGTPreload}）执行。
 *
 * <p>
 * 注册三个新方块（复刻原版样板输入仓、样板槽扩容为 8×16）：
 * <ul>
 * <li>巨型样板输入总线（物品，Tier 6，对应原版 Crafting Input Bus (ME)）</li>
 * <li>巨型样板输入总成（防水/流体样板，Tier 10，对应原版 Crafting Input Buffer (ME)）</li>
 * <li>巨型样板输入镜像（Tier 11，对应原版 Crafting Input Proxy，连接本 mod 的总成）</li>
 * </ul>
 */
public final class MachineLoader {

    private MachineLoader() {}

    public static void registerMachines() {
        GTMegaPatternInput.LOG.info("开始注册 GT Mega Pattern Input 机器...");

        Mega_Crafting_Input_Bus.set(
            new MTEHatchCraftingInputME(
                MetaTileEntityID.MEGA_CRAFTING_INPUT_BUS.ID,
                "gtmpi.hatch.crafting_input.mega.bus",
                "Mega Crafting Input Bus (ME)",
                false).getStackForm(1L));

        Mega_Crafting_Input_Buffer.set(
            new MTEHatchCraftingInputME(
                MetaTileEntityID.MEGA_CRAFTING_INPUT_BUFFER.ID,
                "gtmpi.hatch.crafting_input.mega.buffer",
                "Mega Crafting Input Buffer (ME)",
                true).getStackForm(1L));

        Mega_Crafting_Input_Proxy.set(
            new MTEHatchCraftingInputSlave(
                MetaTileEntityID.MEGA_CRAFTING_INPUT_PROXY.ID,
                "gtmpi.hatch.crafting_input.mega.proxy",
                "Mega Crafting Input Proxy (ME)").getStackForm(1L));

        // AE2 接口终端白名单制：GT5U 只把原版仓类登记进 InterfaceTerminalRegistry，
        // 本 mod 的仓类需自行登记才能在接口终端中显示（镜像类无 ME 节点，与原版一致不登记）
        AEApi.instance()
            .registries()
            .interfaceTerminal()
            .register(MTEHatchCraftingInputME.class);

        GTMegaPatternInput.LOG.info(
            "机器注册完成：bus={}, buffer={}, proxy={}。",
            MetaTileEntityID.MEGA_CRAFTING_INPUT_BUS.ID,
            MetaTileEntityID.MEGA_CRAFTING_INPUT_BUFFER.ID,
            MetaTileEntityID.MEGA_CRAFTING_INPUT_PROXY.ID);
    }

    /**
     * 注册原版工作台配方（FML init 阶段调用，原料均已就绪）：
     * <ul>
     * <li>巨型总线 = LV 输入总线 + ME 接口</li>
     * <li>巨型总成 = LV 输入总线 + LV 输入仓 + ME 二合一接口（ae2fc 流体接口方块）</li>
     * <li>巨型镜像 = 巨型总成 + 原版玻璃</li>
     * </ul>
     */
    public static void registerRecipes() {
        // 巨型总线 = LV 输入总线 + ME 接口
        ItemStack meInterface = AEApi.instance()
            .blocks().blockInterface.stack(1);
        if (meInterface != null) {
            GameRegistry.addShapelessRecipe(
                Mega_Crafting_Input_Bus.get(1)
                    .copy(),
                ItemList.Hatch_Input_Bus_LV.get(1),
                meInterface);
        } else {
            GTMegaPatternInput.LOG.warn("未获取到 AE2 ME 接口方块，跳过巨型总线配方。");
        }

        // 巨型总成 = LV 输入总线 + LV 输入仓 + ME 二合一接口
        ItemStack dualInterface = ItemAndBlockHolder.INTERFACE != null ? new ItemStack(ItemAndBlockHolder.INTERFACE)
            : null;
        if (dualInterface != null) {
            GameRegistry.addShapelessRecipe(
                Mega_Crafting_Input_Buffer.get(1)
                    .copy(),
                ItemList.Hatch_Input_Bus_LV.get(1),
                ItemList.Hatch_Input_LV.get(1),
                dualInterface);
        } else {
            GTMegaPatternInput.LOG.warn("未获取到 ae2fc 二合一接口方块，跳过巨型总成配方。");
        }

        // 巨型镜像 = 巨型总成 + 原版玻璃
        GameRegistry.addShapelessRecipe(
            Mega_Crafting_Input_Proxy.get(1)
                .copy(),
            Mega_Crafting_Input_Buffer.get(1),
            new ItemStack(Blocks.glass));

        GTMegaPatternInput.LOG.info("巨型样板输入三配方注册完成。");
    }

    /**
     * GT5U 的 tooltip 本地化不读 assets lang，只读游戏根目录的 GregTech(_locale).lang
     * （{@code GTPreLoad.initLocalization} 在 GT preInit 读取；本 mod required-before:gregtech，
     * 本方法先于它执行）。中文客户端把中文描述键强制写入（覆盖旧值），en_US/服务端
     * 无需写入——GT 会用描述数组的英文自动生成默认条目。
     */
    public static void registerLocalizations(File gameDir) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide()
            .isServer()) return;
        String langCode = Minecraft.getMinecraft()
            .getLanguageManager()
            .getCurrentLanguage()
            .getLanguageCode();
        if (!langCode.startsWith("zh_")) return;

        File langFile = new File(gameDir, "GregTech_" + langCode + ".lang");
        Configuration langConfig = new Configuration(langFile);
        langConfig.load();

        putDescriptions(
            langConfig,
            MetaTileEntityID.MEGA_CRAFTING_INPUT_BUS.ID,
            new String[] { "用于多方块结构的高级物品输入", "等级：§7LV", "直接从 ME 网络处理样板", "不支持包含流体的样板", "使用剪线钳右键可改变 ME 连接方式",
                "忽略其他总线或仓室的内容", "同时忽略同一总线内的其他样板" });
        putDescriptions(
            langConfig,
            MetaTileEntityID.MEGA_CRAFTING_INPUT_BUFFER.ID,
            new String[] { "用于多方块结构的高级物品输入", "等级：§7LV", "直接从 ME 网络处理样板", "支持包含流体的样板", "使用剪线钳右键可改变 ME 连接方式",
                "忽略其他总线或仓室的内容", "同时忽略同一总线内的其他样板" });
        putDescriptions(
            langConfig,
            MetaTileEntityID.MEGA_CRAFTING_INPUT_PROXY.ID,
            new String[] { "样板输入总线/总成的镜像", "等级：§7LV", "使用数据棒连接样板输入总线/总成以共享物品栏", "手持数据棒左键点击样板输入总线/总成，再右键点击本方块进行连接" });

        langConfig.save();
        GTMegaPatternInput.LOG.info("已把巨型样板输入三机器的中文 tooltip 描述写入 {}。", langFile.getName());
    }

    private static void putDescriptions(Configuration langConfig, int mteId, String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String key = String.format("TileEntity_DESCRIPTION_%05d_Index_%02d", mteId, i);
            langConfig.get("LanguageFile", key, lines[i])
                .set(lines[i]);
        }
    }
}
