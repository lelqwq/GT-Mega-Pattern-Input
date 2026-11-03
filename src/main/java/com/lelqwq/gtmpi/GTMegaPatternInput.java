package com.lelqwq.gtmpi;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lelqwq.gtmpi.common.api.enums.GTMPIItemList;
import com.lelqwq.gtmpi.loader.MachineLoader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import gregtech.api.GregTechAPI;

/**
 * GT Mega Pattern Input —— GTNH 2.9.0 专用 GT 附加模组。
 *
 * <p>
 * 新增 8×16（128 格）巨型样板输入总线/总成/镜像，解决原版样板输入仓 4×9（36 格）容量不足的问题。
 * 不修改 GT5U 本体：机器通过 {@code GregTechAPI.sAfterGTPreload} 队列在 GT preInit 末尾注册
 * （参考 GigaGramFab / GTSimpleWirelessNetwork 的注册模式）。
 */
@Mod(
    modid = GTMegaPatternInput.MODID,
    name = GTMegaPatternInput.NAME,
    version = Tags.VERSION,
    // required-before:gregtech 确保本 mod 的 preInit 在 GT preInit 之前执行，
    // 从而能在 GT preInit 末尾遍历 sAfterGTPreload 队列之前完成机器注册任务入队
    dependencies = "required-before:gregtech;required-after:appliedenergistics2;")
public class GTMegaPatternInput {

    public static final String MODID = "gtmpi";
    public static final String NAME = "GT Mega Pattern Input";

    public static final Logger LOG = LogManager.getLogger(MODID);

    /** 创造模式物品栏：图标在机器注册完成后惰性获取。 */
    public static final CreativeTabs TAB = new CreativeTabs("gtmpi") {

        @Override
        public Item getTabIconItem() {
            net.minecraft.item.ItemStack stack = GTMPIItemList.Mega_Crafting_Input_Buffer.get(1);
            return stack != null ? stack.getItem() : net.minecraft.init.Items.bread;
        }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.load(event.getSuggestedConfigurationFile());

        GregTechAPI.sAfterGTPreload.add(MachineLoader::registerMachines);
        LOG.info("已把机器注册任务加入 GregTechAPI.sAfterGTPreload 队列（MTE ID 基准 18000）。");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 原版工作台配方：本 mod 方块（sAfterGTPreload 已注册）、AE2 方块（required-after 保证先 init）、
        // ae2fc 方块（preInit 注册）此时均已就绪
        MachineLoader.registerRecipes();
    }
}
